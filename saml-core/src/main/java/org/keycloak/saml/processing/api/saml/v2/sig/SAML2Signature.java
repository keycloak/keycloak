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
package org.keycloak.saml.processing.api.saml.v2.sig;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.util.SignatureUtilTransferObject;
import org.keycloak.saml.processing.core.util.XMLSignatureUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.parsers.ParserConfigurationException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import org.keycloak.rotation.KeyLocator;

/**
 * Class that deals with SAML2 Signature
 *
 * @author Anil.Saldhana@redhat.com
 * @author alessio.soldano@jboss.com
 * @since May 26, 2009
 */
public class SAML2Signature {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    private String signatureMethod = SignatureMethod.RSA_SHA1;

    private String digestMethod = DigestMethod.SHA1;

    private Node sibling;

    /**
     * Set the X509Certificate if X509Data is needed in signed info
     */
    private X509Certificate x509Certificate;

    public String getSignatureMethod() {
        return signatureMethod;
    }

    public void setSignatureMethod(String signatureMethod) {
        this.signatureMethod = signatureMethod;
    }

    public String getDigestMethod() {
        return digestMethod;
    }

    public void setDigestMethod(String digestMethod) {
        this.digestMethod = digestMethod;
    }

    public void setNextSibling(Node sibling) {
        this.sibling = sibling;
    }

    /**
     * Set to false, if you do not want to include keyinfo in the signature
     *
     * @param val
     *
     * @since v2.0.1
     */
    public void setSignatureIncludeKeyInfo(boolean val) {
        if (!val) {
            XMLSignatureUtil.setIncludeKeyInfoInSignature(false);
        }
    }

    /**
     * Set the {@link X509Certificate} if you desire
     * to have the SignedInfo have X509 Data
     *
     * This method needs to be called before any of the sign methods.
     *
     * @param x509Certificate
     *
     * @since v2.5.0
     */
    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    /**
     * Sign an Document at the root
     *
     * @param keyPair Key Pair
     *
     * @return
     *
     * @throws ParserConfigurationException
     * @throws XMLSignatureException
     * @throws MarshalException
     * @throws GeneralSecurityException
     */
    public Document sign(Document doc, String referenceID, String keyName, KeyPair keyPair, String canonicalizationMethodType) throws ParserConfigurationException,
            GeneralSecurityException, MarshalException, XMLSignatureException {
        String referenceURI = "#" + referenceID;

        configureIdAttribute(doc);

        if (sibling != null) {
            SignatureUtilTransferObject dto = new SignatureUtilTransferObject();
            dto.setDocumentToBeSigned(doc);
            dto.setKeyName(keyName);
            dto.setKeyPair(keyPair);
            dto.setDigestMethod(digestMethod);
            dto.setSignatureMethod(signatureMethod);
            dto.setReferenceURI(referenceURI);
            dto.setNextSibling(sibling);

            if (x509Certificate != null) {
                dto.setX509Certificate(x509Certificate);
            }

            return XMLSignatureUtil.sign(dto, canonicalizationMethodType);
        }
        return XMLSignatureUtil.sign(doc, keyName, keyPair, digestMethod, signatureMethod, referenceURI, canonicalizationMethodType);
    }

    /**
     * Sign a SAML Document
     *
     * @param samlDocument
     * @param keypair
     *
     * @throws org.keycloak.saml.common.exceptions.ProcessingException
     */
    public void signSAMLDocument(Document samlDocument, String keyName, KeyPair keypair, String canonicalizationMethodType) throws ProcessingException {
        // Get the ID from the root
        String id = samlDocument.getDocumentElement().getAttribute(JBossSAMLConstants.ID.get());
        try {
            sign(samlDocument, id, keyName, keypair, canonicalizationMethodType);
        } catch (ParserConfigurationException | GeneralSecurityException | MarshalException | XMLSignatureException e) {
            throw new ProcessingException(logger.signatureError(e));
        }
    }

    /**
     * Validate the SAML2 Document
     *
     * @param signedDocument
     * @param keyLocator
     *
     * @return
     *
     * @throws ProcessingException
     */
    public boolean validate(Document signedDocument, KeyLocator keyLocator) throws ProcessingException {
        try {
            configureIdAttribute(signedDocument);
            return XMLSignatureUtil.validate(signedDocument, keyLocator);
        } catch (MarshalException | XMLSignatureException me) {
            throw new ProcessingException(logger.signatureError(me));
        }
    }

    /**
     * Given a {@link Document}, find the {@link Node} which is the sibling of the Issuer element
     *
     * @param doc
     *
     * @return
     */
    public Node getNextSiblingOfIssuer(Document doc) {
        // Find the sibling of Issuer
        NodeList nl = doc.getElementsByTagNameNS(JBossSAMLURIConstants.ASSERTION_NSURI.get(), JBossSAMLConstants.ISSUER.get());
        if (nl.getLength() > 0) {
            Node issuer = nl.item(0);

            return issuer.getNextSibling();
        }
        return null;
    }

    /**
     * <p>
     * Sets the IDness of the ID attribute. Santuario 1.5.1 does not assumes IDness based on attribute names anymore.
     * This
     * method should be called before signing/validating a saml document.
     * </p>
     *
     * @param document SAML document to have its ID attribute configured.
     */
    public static void configureIdAttribute(Document document) {
        // Estabilish the IDness of the ID attribute.
        configureIdAttribute(document.getDocumentElement());

        NodeList nodes = document.getElementsByTagNameNS(JBossSAMLURIConstants.ASSERTION_NSURI.get(),
                JBossSAMLConstants.ASSERTION.get());

        for (int i = 0; i < nodes.getLength(); i++) {
            configureIdAttribute((Element) nodes.item(i));
        }
    }
    
    public static void configureIdAttribute(Element element) {
        element.setIdAttribute(JBossSAMLConstants.ID.get(), true);
    }

}