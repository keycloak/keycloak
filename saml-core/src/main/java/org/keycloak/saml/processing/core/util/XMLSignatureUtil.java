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
package org.keycloak.saml.processing.core.util;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.Data;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.NodeSetData;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import jakarta.xml.bind.JAXBException;

import org.keycloak.common.util.PemUtils;
import org.keycloak.dom.xmlsec.w3.xmldsig.DSAKeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.KeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.RSAKeyValueType;
import org.keycloak.dom.xmlsec.w3.xmldsig.SignatureType;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.constants.WSTrustConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.SecurityActions;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.saml.common.util.SystemPropertiesUtil;
import org.keycloak.saml.common.util.TransformerUtil;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Utility for XML Signature <b>Note:</b> You can change the canonicalization method type by using the system property
 * "picketlink.xmlsig.canonicalization"
 *
 * @author Anil.Saldhana@redhat.com
 * @author alessio.soldano@jboss.com
 * @since Dec 15, 2008
 */
public class XMLSignatureUtil {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    // Set some system properties and Santuario providers. Run this block before any other class initialization.
    static {
        SystemPropertiesUtil.ensure();
        String keyInfoProp = SecurityActions.getSystemProperty("picketlink.xmlsig.includeKeyInfo", null);
        if (StringUtil.isNotNull(keyInfoProp)) {
            includeKeyInfoInSignature = Boolean.parseBoolean(keyInfoProp);
        }
    }

    ;

    private static final XMLSignatureFactory fac = getXMLSignatureFactory();

    /**
     * By default, we include the keyinfo in the signature
     */
    private static boolean includeKeyInfoInSignature = true;

    private static class KeySelectorUtilizingKeyNameHint extends KeySelector {

        private final KeyLocator locator;

        private boolean keyLocated = false;

        public KeySelectorUtilizingKeyNameHint(KeyLocator locator) {
            this.locator = locator;
        }

        @Override
        public KeySelectorResult select(KeyInfo keyInfo, KeySelector.Purpose purpose, AlgorithmMethod method, XMLCryptoContext context) throws KeySelectorException {
            try {
                final Key key = locator.getKey(keyInfo);
                this.keyLocated = key != null;
                return new KeySelectorResult() {
                    @Override public Key getKey() {
                        return key;
                    }
                };
            } catch (KeyManagementException ex) {
                throw new KeySelectorException(ex);
            }

        }

        private boolean wasKeyLocated() {
            return this.keyLocated;
        }
    }

    private static XMLSignatureFactory getXMLSignatureFactory() {
        XMLSignatureFactory xsf = null;

        try {
            xsf = XMLSignatureFactory.getInstance("DOM", "ApacheXMLDSig");
        } catch (NoSuchProviderException ex) {
            try {
                xsf = XMLSignatureFactory.getInstance("DOM");
            } catch (Exception err) {
                throw new RuntimeException(logger.couldNotCreateInstance("DOM", err));
            }
        }
        return xsf;
    }

    /**
     * Returns the element that contains the signature for the passed element.
     *
     * @param element The element to search for the signature
     * @return The signature element or null
     */
    public static Element getSignature(Element element) {
        Document doc = element.getOwnerDocument();
        NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (element.getAttributeNode(JBossSAMLConstants.ID.get()) != null) {
            // set the saml ID to be found
            element.setIdAttribute(JBossSAMLConstants.ID.get(), true);
        }
        KeySelector nullSelector = new KeySelector() {
            @Override
            public KeySelectorResult select(KeyInfo ki, KeySelector.Purpose prps, AlgorithmMethod am, XMLCryptoContext xmlcc) throws KeySelectorException {
                return () -> null;
            }
        };

        try {
            for (int i = 0; i < nl.getLength(); i++) {
                Element signatureElement = (Element) nl.item(i);
                DOMValidateContext valContext = new DOMValidateContext(nullSelector, signatureElement);
                DOMStructure structure = new DOMStructure(signatureElement);
                XMLSignature signature = fac.unmarshalXMLSignature(structure);
                for (Reference ref : (List<Reference>) signature.getSignedInfo().getReferences()) {
                    try {
                        Data data = fac.getURIDereferencer().dereference(ref, valContext);
                        if (data instanceof NodeSetData) {
                            Iterator<Node> it = ((NodeSetData) data).iterator();
                            if (it.hasNext() && element.equals(it.next())) {
                                return signatureElement;
                            }
                        }
                    } catch (URIReferenceException e) {
                        logger.trace("Invalid URI reference in signature " + ref.getURI());
                    }
                }
            }
        } catch (MarshalException e) {
            logger.trace("Error unmarshalling signature", e);
        }
        return null;
    }

    /**
     * Use this method to not include the KeyInfo in the signature
     *
     * @param includeKeyInfoInSignature
     *
     * @since v2.0.1
     */
    public static void setIncludeKeyInfoInSignature(boolean includeKeyInfoInSignature) {
        XMLSignatureUtil.includeKeyInfoInSignature = includeKeyInfoInSignature;
    }

    /**
     * Sign a node in a document
     *
     * @param doc
     * @param nodeToBeSigned
     * @param keyPair
     * @param digestMethod
     * @param signatureMethod
     * @param referenceURI
     *
     * @return
     *
     * @throws ParserConfigurationException
     * @throws XMLSignatureException
     * @throws MarshalException
     * @throws GeneralSecurityException
     */
    public static Document sign(Document doc, Node nodeToBeSigned, String keyName, KeyPair keyPair, String digestMethod,
                                String signatureMethod, String referenceURI, X509Certificate x509Certificate,
                                String canonicalizationMethodType) throws ParserConfigurationException, GeneralSecurityException,
            MarshalException, XMLSignatureException {
        if (nodeToBeSigned == null)
            throw logger.nullArgumentError("Node to be signed");

        if (logger.isTraceEnabled()) {
            logger.trace("Document to be signed=" + DocumentUtil.asString(doc));
        }

        Node parentNode = nodeToBeSigned.getParentNode();

        // Let us create a new Document
        Document newDoc = DocumentUtil.createDocument();
        // Import the node
        Node signingNode = newDoc.importNode(nodeToBeSigned, true);
        newDoc.appendChild(signingNode);

        if (!referenceURI.isEmpty()) {
            propagateIDAttributeSetup(nodeToBeSigned, newDoc.getDocumentElement());
        }
        newDoc = sign(newDoc, keyName, keyPair, digestMethod, signatureMethod, referenceURI, x509Certificate, canonicalizationMethodType);

        // if the signed element is a SAMLv2.0 assertion we need to move the signature element to the position
        // specified in the schema (before the assertion subject element).
        if (nodeToBeSigned.getLocalName().equals("Assertion")
                && WSTrustConstants.SAML2_ASSERTION_NS.equals(nodeToBeSigned.getNamespaceURI())) {
            Node signatureNode = DocumentUtil.getElement(newDoc, new QName(WSTrustConstants.DSIG_NS, "Signature"));
            Node subjectNode = DocumentUtil.getElement(newDoc, new QName(WSTrustConstants.SAML2_ASSERTION_NS, "Subject"));
            if (signatureNode != null && subjectNode != null) {
                newDoc.getDocumentElement().removeChild(signatureNode);
                newDoc.getDocumentElement().insertBefore(signatureNode, subjectNode);
            }
        }

        // Now let us import this signed doc into the original document we got in the method call
        Node signedNode = doc.importNode(newDoc.getFirstChild(), true);

        if (!referenceURI.isEmpty()) {
            propagateIDAttributeSetup(newDoc.getDocumentElement(), (Element) signedNode);
        }

        parentNode.replaceChild(signedNode, nodeToBeSigned);
        // doc.getDocumentElement().replaceChild(signedNode, nodeToBeSigned);

        return doc;
    }

    /**
     * Sign only specified element (assumption is that it already has ID attribute set)
     *
     * @param elementToSign element to sign with set ID
     * @param nextSibling child of elementToSign, which will be used as next sibling of created signature
     * @param keyPair
     * @param digestMethod
     * @param signatureMethod
     * @param referenceURI
     *
     * @throws GeneralSecurityException
     * @throws MarshalException
     * @throws XMLSignatureException
     */
    public static void sign(Element elementToSign, Node nextSibling, String keyName, KeyPair keyPair, String digestMethod,
                            String signatureMethod, String referenceURI, String canonicalizationMethodType)
            throws GeneralSecurityException, MarshalException, XMLSignatureException {
        sign(elementToSign, nextSibling, keyName, keyPair, digestMethod, signatureMethod, referenceURI, null, canonicalizationMethodType);
    }

    /**
     * Sign only specified element (assumption is that it already has ID attribute set)
     *
     * @param elementToSign element to sign with set ID
     * @param nextSibling child of elementToSign, which will be used as next sibling of created signature
     * @param keyPair
     * @param digestMethod
     * @param signatureMethod
     * @param referenceURI
     * @param x509Certificate {@link X509Certificate} to be placed in SignedInfo
     *
     * @throws GeneralSecurityException
     * @throws MarshalException
     * @throws XMLSignatureException
     * @since 2.5.0
     */
    public static void sign(Element elementToSign, Node nextSibling, String keyName, KeyPair keyPair, String digestMethod,
                            String signatureMethod, String referenceURI, X509Certificate x509Certificate, String canonicalizationMethodType)
            throws GeneralSecurityException, MarshalException, XMLSignatureException {
        PrivateKey signingKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        DOMSignContext dsc = new DOMSignContext(signingKey, elementToSign, nextSibling);

        signImpl(dsc, digestMethod, signatureMethod, referenceURI, keyName, publicKey, x509Certificate, canonicalizationMethodType);
    }

    /**
     * Setup the ID attribute into <code>destElement</code> depending on the <code>isId</code> flag of an attribute of
     * <code>sourceNode</code>.
     *
     * @param sourceNode
     */
    public static void propagateIDAttributeSetup(Node sourceNode, Element destElement) {
        NamedNodeMap nnm = sourceNode.getAttributes();
        for (int i = 0; i < nnm.getLength(); i++) {
            Attr attr = (Attr) nnm.item(i);
            if (attr.isId()) {
                destElement.setIdAttribute(attr.getName(), true);
                break;
            }
        }
    }

    /**
     * Sign the root element
     *
     * @param doc
     * @param digestMethod
     * @param signatureMethod
     * @param referenceURI
     *
     * @return
     *
     * @throws GeneralSecurityException
     * @throws XMLSignatureException
     * @throws MarshalException
     */
    public static Document sign(Document doc, String keyName, KeyPair keyPair, String digestMethod, String signatureMethod, String referenceURI, String canonicalizationMethodType)
            throws GeneralSecurityException, MarshalException, XMLSignatureException {
        return sign(doc, keyName, keyPair, digestMethod, signatureMethod, referenceURI, null, canonicalizationMethodType);
    }

    /**
     * Sign the root element
     *
     * @param doc
     * @param digestMethod
     * @param signatureMethod
     * @param referenceURI
     *
     * @return
     *
     * @throws GeneralSecurityException
     * @throws XMLSignatureException
     * @throws MarshalException
     * @since 2.5.0
     */
    public static Document sign(Document doc, String keyName, KeyPair keyPair, String digestMethod, String signatureMethod, String referenceURI,
                                X509Certificate x509Certificate, String canonicalizationMethodType)
            throws GeneralSecurityException, MarshalException, XMLSignatureException {
        if (logger.isTraceEnabled()) {
            logger.trace("Document to be signed=" + DocumentUtil.asString(doc));
        }
        PrivateKey signingKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        DOMSignContext dsc = new DOMSignContext(signingKey, doc.getDocumentElement());

        signImpl(dsc, digestMethod, signatureMethod, referenceURI, keyName, publicKey, x509Certificate, canonicalizationMethodType);

        return doc;
    }

    /**
     * Sign the root element
     *
     *
     * @return
     *
     * @throws GeneralSecurityException
     * @throws XMLSignatureException
     * @throws MarshalException
     */
    public static Document sign(SignatureUtilTransferObject dto, String canonicalizationMethodType) throws GeneralSecurityException, MarshalException,
            XMLSignatureException {
        Document doc = dto.getDocumentToBeSigned();
        String keyName = dto.getKeyName();
        KeyPair keyPair = dto.getKeyPair();
        Node nextSibling = dto.getNextSibling();
        String digestMethod = dto.getDigestMethod();
        String referenceURI = dto.getReferenceURI();
        String signatureMethod = dto.getSignatureMethod();

        if (logger.isTraceEnabled()) {
            logger.trace("Document to be signed=" + DocumentUtil.asString(doc));
        }

        PrivateKey signingKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        DOMSignContext dsc = new DOMSignContext(signingKey, doc.getDocumentElement(), nextSibling);

        signImpl(dsc, digestMethod, signatureMethod, referenceURI, keyName, publicKey, dto.getX509Certificate(), canonicalizationMethodType);

        if (logger.isTraceEnabled()) {
            logger.trace("Signed document=" + DocumentUtil.asString(doc));
        }

        return doc;
    }

    /**
     * Validate a signed document with the given public key. All elements that contain a Signature are checked,
     * this way both assertions and the containing document are verified when signed.
     *
     * @param signedDoc
     * @param locator
     *
     * @return
     *
     * @throws MarshalException
     * @throws XMLSignatureException
     */
    @SuppressWarnings("unchecked")
    public static boolean validate(Document signedDoc, final KeyLocator locator) throws MarshalException, XMLSignatureException {
        if (signedDoc == null)
            throw logger.nullArgumentError("Signed Document");

        propagateIDAttributeSetup(signedDoc.getDocumentElement(), signedDoc.getDocumentElement());

        NodeList nl = signedDoc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");

        if (nl == null || nl.getLength() == 0) {
            logger.debug("Cannot find Signature element");
            return false;
        }

        if (locator == null)
            throw logger.nullValueError("Public Key");

        HashSet<Node> signedNodes = new HashSet<>();

        for (int i = 0; i < nl.getLength(); i++) {
            Node signatureNode = nl.item(i);
            if (!validateSingleNode(signatureNode, locator, signedNodes)) {
                return false;
            }
        }

        if (signedNodes.contains(signedDoc.getDocumentElement())) {
            logger.trace("All signatures are OK and root document is signed");
            return true;
        }

        NodeList assertions = signedDoc.getElementsByTagNameNS(JBossSAMLURIConstants.ASSERTION_NSURI.get(), JBossSAMLConstants.ASSERTION.get());

        if (assertions.getLength() > 0) {
            // if document is not fully signed check if all the assertions are signed
            for (int i = 0; i < assertions.getLength(); i++) {
                if (!signedNodes.contains(assertions.item(i))) {
                    logger.debug("SAML Response document may contain malicious assertions. Signature validation will fail.");
                    // there are unsigned assertions mixed with signed ones
                    return false;
                }
            }
            logger.trace("Document not signed but all assertions are signed OK");
            return true;
        }

        return false;
    }

    public static boolean validateSingleNode(Node signatureNode, final KeyLocator locator) throws MarshalException, XMLSignatureException {
        return validateSingleNode(signatureNode, locator, new HashSet<>());
    }

    public static boolean validateSingleNode(Node signatureNode, final KeyLocator locator, Set<Node> signedNodes) throws MarshalException, XMLSignatureException {
        KeySelectorUtilizingKeyNameHint sel = new KeySelectorUtilizingKeyNameHint(locator);
        try {
            if (validateUsingKeySelector(signatureNode, sel, signedNodes)) {
                return true;
            }
            if (sel.wasKeyLocated()) {
                return false;
            }
        } catch (XMLSignatureException ex) { // pass through MarshalException
            logger.debug("Verification failed: " + ex);
            logger.trace(ex);
        }

        logger.trace("Could not validate signature using ds:KeyInfo/ds:KeyName hint.");

        logger.trace("Trying hard to validate XML signature using all available keys.");

        for (Key key : locator) {
            try {
                if (validateUsingKeySelector(signatureNode, KeySelector.singletonKeySelector(key), signedNodes)) {
                    return true;
                }
            } catch (XMLSignatureException ex) { // pass through MarshalException
                logger.debug("Verification failed: " + ex);
                logger.trace(ex);
            }
        }

        return false;
    }

    private static boolean validateUsingKeySelector(Node signatureNode, KeySelector validationKeySelector, Set<Node> signedNodes) throws XMLSignatureException, MarshalException {
        DOMValidateContext valContext = new DOMValidateContext(validationKeySelector, signatureNode);
        XMLSignature signature = fac.unmarshalXMLSignature(valContext);
        boolean coreValidity = signature.validate(valContext);

        if (coreValidity) {
            for (Reference ref : (List<Reference>) signature.getSignedInfo().getReferences()) {
                try {
                    Data data = fac.getURIDereferencer().dereference(ref, valContext);
                    if (data instanceof NodeSetData) {
                        Iterator<Node> it = ((NodeSetData) data).iterator();
                        if (it.hasNext()) {
                            signedNodes.add(it.next()); // add the first referenced object as signed element
                        }
                    }
                } catch (URIReferenceException e) {
                    // ignored as signature was ok so reference can be obtained
                }
            }
        } else {
            if (logger.isTraceEnabled()) {
                boolean sv = signature.getSignatureValue().validate(valContext);
                logger.trace("Signature validation status: " + sv);

                List<Reference> references = signature.getSignedInfo().getReferences();
                for (Reference ref : references) {
                    logger.trace("[Ref id=" + ref.getId() + ":uri=" + ref.getURI() + "]validity status:" + ref.validate(valContext));
                }
            }
        }

        return coreValidity;
    }

    /**
     * Marshall a SignatureType to output stream
     *
     * @param signature
     * @param os
     *
     * @throws SAXException
     * @throws JAXBException
     */
    public static void marshall(SignatureType signature, OutputStream os) throws JAXBException, SAXException {
        throw logger.notImplementedYet("NYI");
        /*
         * JAXBElement<SignatureType> jsig = objectFactory.createSignature(signature); Marshaller marshaller =
         * JAXBUtil.getValidatingMarshaller(pkgName, schemaLocation); marshaller.marshal(jsig, os);
         */
    }

    /**
     * Marshall the signed document to an output stream
     *
     * @param signedDocument
     * @param os
     *
     * @throws TransformerException
     */
    public static void marshall(Document signedDocument, OutputStream os) throws TransformerException {
        TransformerFactory tf = TransformerUtil.getTransformerFactory();
        Transformer trans = tf.newTransformer();
        trans.transform(DocumentUtil.getXMLSource(signedDocument), new StreamResult(os));
    }

    /**
     * Given the X509Certificate in the keyinfo element, get a {@link X509Certificate}
     *
     * @param certificateString
     *
     * @return
     *
     * @throws org.keycloak.saml.common.exceptions.ProcessingException
     */
    public static X509Certificate getX509CertificateFromKeyInfoString(String certificateString) throws ProcessingException {
        X509Certificate cert = null;
        StringBuilder builder = new StringBuilder();
        builder.append(PemUtils.BEGIN_CERT + "\n").append(certificateString).append("\n" + PemUtils.END_CERT);

        String derFormattedString = builder.toString();

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bais = new ByteArrayInputStream(derFormattedString.getBytes(GeneralConstants.SAML_CHARSET));

            while (bais.available() > 0) {
                cert = (X509Certificate) cf.generateCertificate(bais);
            }
        } catch (java.security.cert.CertificateException e) {
            throw logger.processingError(e);
        }
        return cert;
    }

    /**
     * Given a dsig:DSAKeyValue element, return {@link DSAKeyValueType}
     *
     * @param element
     *
     * @return
     *
     * @throws ProcessingException
     */
    public static DSAKeyValueType getDSAKeyValue(Element element) throws ParsingException {
        DSAKeyValueType dsa = new DSAKeyValueType();
        NodeList nl = element.getChildNodes();
        int length = nl.getLength();

        for (int i = 0; i < length; i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element childElement = (Element) node;
                String tag = childElement.getLocalName();

                byte[] text = childElement.getTextContent().getBytes(GeneralConstants.SAML_CHARSET);

                if (WSTrustConstants.XMLDSig.P.equals(tag)) {
                    dsa.setP(text);
                } else if (WSTrustConstants.XMLDSig.Q.equals(tag)) {
                    dsa.setQ(text);
                } else if (WSTrustConstants.XMLDSig.G.equals(tag)) {
                    dsa.setG(text);
                } else if (WSTrustConstants.XMLDSig.Y.equals(tag)) {
                    dsa.setY(text);
                } else if (WSTrustConstants.XMLDSig.SEED.equals(tag)) {
                    dsa.setSeed(text);
                } else if (WSTrustConstants.XMLDSig.PGEN_COUNTER.equals(tag)) {
                    dsa.setPgenCounter(text);
                }
            }
        }

        return dsa;
    }

    /**
     * Given a dsig:DSAKeyValue element, return {@link DSAKeyValueType}
     *
     * @param element
     *
     * @return
     *
     * @throws ProcessingException
     */
    public static RSAKeyValueType getRSAKeyValue(Element element) throws ParsingException {
        RSAKeyValueType rsa = new RSAKeyValueType();
        NodeList nl = element.getChildNodes();
        int length = nl.getLength();

        for (int i = 0; i < length; i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element childElement = (Element) node;
                String tag = childElement.getLocalName();

                byte[] text = childElement.getTextContent().getBytes(GeneralConstants.SAML_CHARSET);

                if (WSTrustConstants.XMLDSig.MODULUS.equals(tag)) {
                    rsa.setModulus(text);
                } else if (WSTrustConstants.XMLDSig.EXPONENT.equals(tag)) {
                    rsa.setExponent(text);
                }
            }
        }

        return rsa;
    }

    /**
     * <p>
     * Creates a {@code KeyValueType} that wraps the specified public key. This method supports DSA and RSA keys.
     * </p>
     *
     * @param key the {@code PublicKey} that will be represented as a {@code KeyValueType}.
     *
     * @return the constructed {@code KeyValueType} or {@code null} if the specified key is neither a DSA nor a RSA
     *         key.
     */
    public static KeyValueType createKeyValue(PublicKey key) {
        if (key instanceof RSAPublicKey) {
            RSAPublicKey pubKey = (RSAPublicKey) key;
            byte[] modulus = pubKey.getModulus().toByteArray();
            byte[] exponent = pubKey.getPublicExponent().toByteArray();

            RSAKeyValueType rsaKeyValue = new RSAKeyValueType();
            rsaKeyValue.setModulus(Base64.getEncoder().encodeToString(modulus).getBytes(GeneralConstants.SAML_CHARSET));
            rsaKeyValue.setExponent(Base64.getEncoder().encodeToString(exponent).getBytes(GeneralConstants.SAML_CHARSET));
            return rsaKeyValue;
        } else if (key instanceof DSAPublicKey) {
            DSAPublicKey pubKey = (DSAPublicKey) key;
            byte[] P = pubKey.getParams().getP().toByteArray();
            byte[] Q = pubKey.getParams().getQ().toByteArray();
            byte[] G = pubKey.getParams().getG().toByteArray();
            byte[] Y = pubKey.getY().toByteArray();

            DSAKeyValueType dsaKeyValue = new DSAKeyValueType();
            dsaKeyValue.setP(Base64.getEncoder().encodeToString(P).getBytes(GeneralConstants.SAML_CHARSET));
            dsaKeyValue.setQ(Base64.getEncoder().encodeToString(Q).getBytes(GeneralConstants.SAML_CHARSET));
            dsaKeyValue.setG(Base64.getEncoder().encodeToString(G).getBytes(GeneralConstants.SAML_CHARSET));
            dsaKeyValue.setY(Base64.getEncoder().encodeToString(Y).getBytes(GeneralConstants.SAML_CHARSET));
            return dsaKeyValue;
        }
        throw logger.unsupportedType(key.toString());
    }

    private static void signImpl(DOMSignContext dsc, String digestMethod, String signatureMethod, String referenceURI, String keyName, PublicKey publicKey,
                                 X509Certificate x509Certificate, String canonicalizationMethodType)
            throws GeneralSecurityException, MarshalException, XMLSignatureException {
        dsc.setDefaultNamespacePrefix("dsig");

        DigestMethod digestMethodObj = fac.newDigestMethod(digestMethod, null);
        Transform transform1 = fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null);
        Transform transform2 = fac.newTransform("http://www.w3.org/2001/10/xml-exc-c14n#", (TransformParameterSpec) null);

        List<Transform> transformList = new ArrayList<>();
        transformList.add(transform1);
        transformList.add(transform2);

        Reference ref = fac.newReference(referenceURI, digestMethodObj, transformList, null, null);

        CanonicalizationMethod canonicalizationMethod = fac.newCanonicalizationMethod(canonicalizationMethodType,
                (C14NMethodParameterSpec) null);

        List<Reference> referenceList = Collections.singletonList(ref);
        SignatureMethod signatureMethodObj = fac.newSignatureMethod(signatureMethod, null);
        SignedInfo si = fac.newSignedInfo(canonicalizationMethod, signatureMethodObj, referenceList);

        KeyInfo ki;
        if (includeKeyInfoInSignature) {
            ki = createKeyInfo(keyName, publicKey, x509Certificate);
        } else {
            ki = createKeyInfo(keyName, null, null);
        }
        XMLSignature signature = fac.newXMLSignature(si, ki);

        signature.sign(dsc);
    }

    public static KeyInfo createKeyInfo(String keyName, PublicKey publicKey, X509Certificate x509Certificate) throws KeyException {
        KeyInfoFactory keyInfoFactory = fac.getKeyInfoFactory();

        List<XMLStructure> items = new LinkedList<>();

        if (keyName != null) {
            items.add(keyInfoFactory.newKeyName(keyName));
        }

        if (x509Certificate != null) {
            items.add(keyInfoFactory.newX509Data(Collections.singletonList(x509Certificate)));
        } else if (publicKey != null) {
            items.add(keyInfoFactory.newKeyValue(publicKey));
        }

        return keyInfoFactory.newKeyInfo(items);
    }

    public static KeyInfo createKeyInfo(Element keyInfo) throws MarshalException {
        KeyInfoFactory keyInfoFactory = fac.getKeyInfoFactory();
        return keyInfoFactory.unmarshalKeyInfo(new DOMStructure(keyInfo));
    }
}
