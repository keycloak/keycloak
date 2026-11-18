/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.saml.processing.core.saml.v2.util;

import java.io.StringWriter;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;

import org.keycloak.common.VerificationException;
import org.keycloak.dom.saml.v2.metadata.EntitiesDescriptorType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.dom.saml.v2.metadata.SSODescriptorType;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.api.saml.v2.sig.SAML2Signature;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLMetadataWriter;
import org.keycloak.saml.processing.core.util.XMLSignatureUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Deals with SAML2 Metadata
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jan 31, 2011
 */
public class SAMLMetadataUtil {

    public static final String UTF8_BOM = "\uFEFF";

    /**
     * Get the {@link X509Certificate} from the KeyInfo
     *
     * @param keyDescriptor
     *
     * @return
     *
     * @throws org.keycloak.saml.common.exceptions.ProcessingException
     * @throws org.keycloak.saml.common.exceptions.ConfigurationException
     */
    public static X509Certificate getCertificate(KeyDescriptorType keyDescriptor) throws ConfigurationException,
            ProcessingException {
        X509Certificate cert = null;
        Element keyInfo = keyDescriptor.getKeyInfo();
        if (keyInfo != null) {
            NodeList x509DataNodes = keyInfo.getElementsByTagName("X509Data");
            if (x509DataNodes == null || x509DataNodes.getLength() == 0) {
                x509DataNodes = keyInfo.getElementsByTagNameNS(JBossSAMLURIConstants.XMLDSIG_NSURI.get(), "X509Data");
            }

            if (x509DataNodes == null || x509DataNodes.getLength() == 0) {
                x509DataNodes = keyInfo.getElementsByTagName("ds:X509Data");
            }

            if (x509DataNodes != null && x509DataNodes.getLength() > 0) {
                // Choose the first one
                Node x509DataNode = x509DataNodes.item(0);
                NodeList children = x509DataNode.getChildNodes();
                int len = children != null ? children.getLength() : 0;
                for (int i = 0; i < len; i++) {
                    Node nl = children.item(i);
                    if (nl.getNodeName().contains("X509Certificate")) {
                        Node certNode = nl.getFirstChild();
                        String certNodeValue = certNode.getNodeValue();
                        cert = XMLSignatureUtil.getX509CertificateFromKeyInfoString(certNodeValue.replaceAll("\\s", ""));
                        break;
                    }
                }
            }
        }
        return cert;
    }

    public static X509Certificate getCertificate(KeyTypes use, SSODescriptorType ssoDescriptorType) {
        if (ssoDescriptorType != null) {
            for (KeyDescriptorType keyDescriptorType : ssoDescriptorType.getKeyDescriptor()) {
                KeyTypes keyUse = keyDescriptorType.getUse();

                if (keyUse == null || (use != null && keyUse.value().equals(use.value()))) {
                    try {
                        return getCertificate(keyDescriptorType);
                    } catch (Exception e) {
                        throw new RuntimeException("Could not parse KeyDescriptor X509 certificate from metadata [" + ssoDescriptorType.getID() + "].");
                    }
                }
            }
        }

        return null;
    }

    public static EntityDescriptorType parseEntityDescriptorType(String descriptor) throws ParsingException {
        descriptor = removeUTF8BOM(descriptor);
        return getEntityDescriptorType(SAMLParser.getInstance().parse(StaxParserUtil.getXMLEventReader(descriptor)));
    }

    /**
     * Parses a SAML metadata document that is required to be signed with one of the keys of the key locator.
     *
     * <p>The signature that is validated is the enveloped signature of the root element of the document. If
     * the root element is an <code>EntitiesDescriptor</code> that is not signed with one of the keys, the
     * enveloped signature of its first <code>EntityDescriptor</code> is validated instead, as that is the
     * entity returned by {@link #parseEntityDescriptorType(String)}. The returned object is always created
     * from the element that is covered by the validated signature.</p>
     *
     * <p>The signature needs to follow the signature profile of the SAML metadata specification: a single
     * reference to the signed element and no transforms other than the enveloped signature and the exclusive
     * canonicalization. The keys contained in the document are never trusted.</p>
     *
     * @param descriptor The metadata document
     * @param keyLocator The keys that are trusted to sign the metadata
     * @return The entity descriptor
     * @throws ParsingException If the document cannot be parsed
     * @throws VerificationException If the document is not signed or the signature cannot be validated with the keys
     */
    public static EntityDescriptorType parseEntityDescriptorType(String descriptor, KeyLocator keyLocator)
            throws ParsingException, VerificationException {
        Element root;
        try {
            root = DocumentUtil.getDocument(removeUTF8BOM(descriptor)).getDocumentElement();
        } catch (ConfigurationException | ProcessingException e) {
            throw new ParsingException(e);
        }

        boolean signed = false;
        for (Element element : new Element[] { root, getFirstEntityDescriptorElement(root) }) {
            Element signature = element == null
                    ? null
                    : DocumentUtil.getDirectChildElement(element, XMLSignature.XMLNS, JBossSAMLConstants.SIGNATURE.get());
            if (signature != null) {
                signed = true;
                if (isSignatureValid(element, signature, keyLocator)) {
                    // only the element that is covered by the signature is parsed
                    return getEntityDescriptorType(SAMLParser.getInstance().parse(element));
                }
            }
        }

        throw new VerificationException(signed
                ? "The signature of the SAML metadata is not valid for the trusted keys"
                : "The SAML metadata is not signed");
    }

    /**
     * Returns the element of the first entity of an <code>EntitiesDescriptor</code>, which is the entity
     * used by {@link #getEntityDescriptorType(Object)}, if that entity is an <code>EntityDescriptor</code>.
     */
    private static Element getFirstEntityDescriptorElement(Element root) {
        if (JBossSAMLConstants.from(root) != JBossSAMLConstants.ENTITIES_DESCRIPTOR) {
            return null;
        }
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
            JBossSAMLConstants name = JBossSAMLConstants.from(child);
            if (name == JBossSAMLConstants.ENTITY_DESCRIPTOR) {
                return (Element) child;
            } else if (name == JBossSAMLConstants.ENTITIES_DESCRIPTOR) {
                return null;
            }
        }
        return null;
    }

    /**
     * Checks that the signature is the enveloped signature of the element, as defined in the signature
     * profile of the SAML metadata specification (section 3.1), and that it is valid for one of the keys.
     */
    private static boolean isSignatureValid(Element element, Element signature, KeyLocator keyLocator) {
        NodeList references = signature.getElementsByTagNameNS(XMLSignature.XMLNS, "Reference");
        if (references.getLength() != 1) {
            return false;
        }

        // the reference is the ID of the element or, only for the root element, the whole document
        Element reference = (Element) references.item(0);
        String id = element.getAttribute(JBossSAMLConstants.ID.get());
        String uri = reference.getAttribute("URI");
        boolean wholeDocument = reference.hasAttribute("URI") && uri.isEmpty()
                && element == element.getOwnerDocument().getDocumentElement();
        if (!wholeDocument && (id.isEmpty() || !uri.equals("#" + id))) {
            return false;
        }

        // any other transform could leave parts of the element out of the signature
        NodeList transforms = signature.getElementsByTagNameNS(XMLSignature.XMLNS, "Transform");
        for (int i = 0; i < transforms.getLength(); i++) {
            String algorithm = ((Element) transforms.item(i)).getAttribute("Algorithm");
            if (!Transform.ENVELOPED.equals(algorithm)
                    && !CanonicalizationMethod.EXCLUSIVE.equals(algorithm)
                    && !CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS.equals(algorithm)) {
                return false;
            }
        }

        if (!id.isEmpty()) {
            SAML2Signature.configureIdAttribute(element);
        }

        try {
            // the element is one of the signed nodes only if the reference is resolved to the element itself
            Set<Node> signedNodes = new HashSet<>();
            return XMLSignatureUtil.validateSingleNode(signature, keyLocator, signedNodes) && signedNodes.contains(element);
        } catch (MarshalException | XMLSignatureException e) {
            return false;
        }
    }

    private static EntityDescriptorType getEntityDescriptorType(Object parsedObject) {
        EntityDescriptorType entityType;

        if (EntitiesDescriptorType.class.isInstance(parsedObject)) {
            entityType = (EntityDescriptorType) ((EntitiesDescriptorType) parsedObject).getEntityDescriptor().get(0);
        } else {
            entityType = (EntityDescriptorType) parsedObject;
        }

        return entityType;
    }

    public static IDPSSODescriptorType locateIDPSSODescriptorType(EntityDescriptorType entityType) {
        return locateSSODescriptorType(entityType, SAMLMetadataUtil::getIDPSSODescriptorType);
    }

    public static SPSSODescriptorType locateSPSSODescriptorType(EntityDescriptorType entityType) {
        return locateSSODescriptorType(entityType, SAMLMetadataUtil::getSPSSODescriptorType);
    }

    private static IDPSSODescriptorType getIDPSSODescriptorType(EntityDescriptorType.EDTDescriptorChoiceType type) {
        return type.getIdpDescriptor();
    }

    private static SPSSODescriptorType getSPSSODescriptorType(EntityDescriptorType.EDTDescriptorChoiceType type) {
        return type.getSpDescriptor();
    }

    private static <T> T locateSSODescriptorType(EntityDescriptorType entityType,
            Function<EntityDescriptorType.EDTDescriptorChoiceType, T> getter) {
        List<EntityDescriptorType.EDTChoiceType> choiceType = entityType.getChoiceType();
        T descriptor = null;
        if (!choiceType.isEmpty()) {

            //Metadata documents can contain multiple Descriptors (See ADFS metadata documents) such as RoleDescriptor, SPSSODescriptor, IDPSSODescriptor.
            //So we need to loop through to find the correct Descriptor.
            for (EntityDescriptorType.EDTChoiceType edtChoiceType : entityType.getChoiceType()) {
                List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = edtChoiceType.getDescriptors();

                if (!descriptors.isEmpty() && getter.apply(descriptors.get(0)) != null) {
                    descriptor = getter.apply(descriptors.get(0));
                }
            }
        }
        return descriptor;
    }

    public static String removeUTF8BOM(String s) {
        if (s.startsWith(UTF8_BOM)) {
            s = s.substring(1);
        }
        return s;
    }

    public static String writeEntityDescriptorType(EntityDescriptorType type) throws ProcessingException {
        final StringWriter sw = new StringWriter();
        final SAMLMetadataWriter writer = new SAMLMetadataWriter(StaxUtil.getXMLStreamWriter(sw));
        writer.writeEntityDescriptor(type);
        return sw.toString();
    }

    public static String signEntityDescriptorType(EntityDescriptorType type, SignatureAlgorithm sigAlg,
            String kid, X509Certificate certificate, KeyPair keyPair) throws ProcessingException, ConfigurationException, ParsingException {
        if (type.getID() == null) {
            type.setID(IDGenerator.create("ID_"));
        }

        // write descriptor to XML
        final String descriptor = writeEntityDescriptorType(type);

        // create the document from the XML
        final Document metadataDocument = DocumentUtil.getDocument(descriptor);
        final SAML2Signature signatureHelper = new SAML2Signature();
        signatureHelper.setSignatureMethod(sigAlg.getXmlSignatureMethod());
        signatureHelper.setDigestMethod(sigAlg.getXmlSignatureDigestMethod());
        signatureHelper.setX509Certificate(certificate);

        final Node nextSibling = metadataDocument.getDocumentElement().getFirstChild();
        signatureHelper.setNextSibling(nextSibling);

        // sign the document
        signatureHelper.signSAMLDocument(metadataDocument, kid, keyPair, CanonicalizationMethod.EXCLUSIVE);

        return DocumentUtil.getDocumentAsString(metadataDocument);
    }
}
