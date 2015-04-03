/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.saml.processing.core.util.SignatureUtilTransferObject;
import org.keycloak.saml.processing.core.util.XMLSignatureUtil;
import org.keycloak.dom.saml.v2.protocol.RequestAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * Class that deals with SAML2 Signature
 *
 * @author Anil.Saldhana@redhat.com
 * @author alessio.soldano@jboss.com
 * @since May 26, 2009
 */
public class SAML2Signature {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    private static final String ID_ATTRIBUTE_NAME = "ID";

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
     * Sign an RequestType at the root
     *
     * @param request
     * @param keypair Key Pair
     * @param digestMethod (Example: DigestMethod.SHA1)
     * @param signatureMethod (Example: SignatureMethod.DSA_SHA1)
     *
     * @return
     *
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws XMLSignatureException
     * @throws MarshalException
     * @throws GeneralSecurityException
     */
    public Document sign(RequestAbstractType request, KeyPair keypair) throws SAXException, IOException,
            ParserConfigurationException, GeneralSecurityException, MarshalException, XMLSignatureException {
        SAML2Request saml2Request = new SAML2Request();
        Document doc = saml2Request.convert(request);
        doc.normalize();

        Node theSibling = getNextSiblingOfIssuer(doc);
        if (theSibling != null) {
            this.sibling = theSibling;
        }

        return sign(doc, request.getID(), keypair);
    }

    /**
     * Sign an ResponseType at the root
     *
     * @param response
     * @param keypair Key Pair
     * @param digestMethod (Example: DigestMethod.SHA1)
     * @param signatureMethod (Example: SignatureMethod.DSA_SHA1)
     *
     * @return
     *
     * @throws ParserConfigurationException
     * @throws XMLSignatureException
     * @throws MarshalException
     * @throws GeneralSecurityException
     */
    public Document sign(ResponseType response, KeyPair keypair) throws ParserConfigurationException, GeneralSecurityException,
            MarshalException, XMLSignatureException {
        SAML2Response saml2Request = new SAML2Response();
        Document doc = saml2Request.convert(response);
        doc.normalize();

        Node theSibling = getNextSiblingOfIssuer(doc);
        if (theSibling != null) {
            this.sibling = theSibling;
        }

        return sign(doc, response.getID(), keypair);
    }

    /**
     * Sign an Document at the root
     *
     * @param response
     * @param keyPair Key Pair
     * @param digestMethod (Example: DigestMethod.SHA1)
     * @param signatureMethod (Example: SignatureMethod.DSA_SHA1)
     *
     * @return
     *
     * @throws ParserConfigurationException
     * @throws XMLSignatureException
     * @throws MarshalException
     * @throws GeneralSecurityException
     */
    public Document sign(Document doc, String referenceID, KeyPair keyPair) throws ParserConfigurationException,
            GeneralSecurityException, MarshalException, XMLSignatureException {
        String referenceURI = "#" + referenceID;

        configureIdAttribute(doc);

        if (sibling != null) {
            SignatureUtilTransferObject dto = new SignatureUtilTransferObject();
            dto.setDocumentToBeSigned(doc);
            dto.setKeyPair(keyPair);
            dto.setDigestMethod(digestMethod);
            dto.setSignatureMethod(signatureMethod);
            dto.setReferenceURI(referenceURI);
            dto.setNextSibling(sibling);

            if (x509Certificate != null) {
                dto.setX509Certificate(x509Certificate);
            }

            return XMLSignatureUtil.sign(dto);
        }
        return XMLSignatureUtil.sign(doc, keyPair, digestMethod, signatureMethod, referenceURI);
    }

    /**
     * Sign an assertion whose id value is provided in the response type
     *
     * @param response
     * @param idValueOfAssertion
     * @param keypair
     * @param referenceURI
     *
     * @return
     *
     * @throws ParserConfigurationException
     * @throws TransformerException
     * @throws TransformerFactoryConfigurationError
     * @throws XPathException
     * @throws XMLSignatureException
     * @throws MarshalException
     * @throws GeneralSecurityException
     */
    public Document sign(ResponseType response, String idValueOfAssertion, KeyPair keypair, String referenceURI)
            throws ParserConfigurationException, XPathException, TransformerFactoryConfigurationError, TransformerException,
            GeneralSecurityException, MarshalException, XMLSignatureException {
        SAML2Response saml2Response = new SAML2Response();
        Document doc = saml2Response.convert(response);
        doc.normalize();

        Node theSibling = getNextSiblingOfIssuer(doc);
        if (theSibling != null) {
            this.sibling = theSibling;
        }

        return sign(doc, idValueOfAssertion, keypair, referenceURI);
    }

    /**
     * Sign a document
     *
     * @param doc
     * @param idValueOfAssertion
     * @param keypair
     * @param referenceURI
     *
     * @return
     *
     * @throws ParserConfigurationException
     * @throws XPathException
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerException
     * @throws GeneralSecurityException
     * @throws MarshalException
     * @throws XMLSignatureException
     */
    public Document sign(Document doc, String idValueOfAssertion, KeyPair keypair, String referenceURI)
            throws ParserConfigurationException, XPathException, TransformerFactoryConfigurationError, TransformerException,
            GeneralSecurityException, MarshalException, XMLSignatureException {
        return sign(doc, idValueOfAssertion, keypair);
    }

    /**
     * Sign a SAML Document
     *
     * @param samlDocument
     * @param keypair
     *
     * @throws org.keycloak.saml.common.exceptions.ProcessingException
     */
    public void signSAMLDocument(Document samlDocument, KeyPair keypair) throws ProcessingException {
        // Get the ID from the root
        String id = samlDocument.getDocumentElement().getAttribute(ID_ATTRIBUTE_NAME);
        try {
            sign(samlDocument, id, keypair);
        } catch (Exception e) {
            throw new ProcessingException(logger.signatureError(e));
        }
    }

    /**
     * Validate the SAML2 Document
     *
     * @param signedDocument
     * @param publicKey
     *
     * @return
     *
     * @throws ProcessingException
     */
    public boolean validate(Document signedDocument, PublicKey publicKey) throws ProcessingException {
        try {
            configureIdAttribute(signedDocument);
            return XMLSignatureUtil.validate(signedDocument, publicKey);
        } catch (MarshalException me) {
            throw new ProcessingException(logger.signatureError(me));
        } catch (XMLSignatureException xse) {
            throw new ProcessingException(logger.signatureError(xse));
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
    private void configureIdAttribute(Document document) {
        // Estabilish the IDness of the ID attribute.
        document.getDocumentElement().setIdAttribute(ID_ATTRIBUTE_NAME, true);

        NodeList nodes = document.getElementsByTagNameNS(JBossSAMLURIConstants.ASSERTION_NSURI.get(),
                JBossSAMLConstants.ASSERTION.get());

        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n instanceof Element) {
                ((Element) n).setIdAttribute(ID_ATTRIBUTE_NAME, true);
            }
        }
    }
}