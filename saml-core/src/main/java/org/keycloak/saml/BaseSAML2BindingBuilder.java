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

package org.keycloak.saml;

import org.jboss.logging.Logger;

import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.sig.SAML2Signature;
import org.keycloak.saml.processing.core.saml.v2.util.DocumentUtil;
import org.keycloak.saml.processing.core.util.XMLEncryptionUtil;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;

import static org.keycloak.common.util.HtmlUtils.escapeAttribute;
import static org.keycloak.saml.common.util.StringUtil.isNotNull;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BaseSAML2BindingBuilder<T extends BaseSAML2BindingBuilder> {
    protected static final Logger logger = Logger.getLogger(BaseSAML2BindingBuilder.class);

    protected String signingKeyName;
    protected KeyPair signingKeyPair;
    protected X509Certificate signingCertificate;
    protected boolean sign;
    protected boolean signAssertions;
    protected SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RSA_SHA1;
    protected String relayState;
    protected int encryptionKeySize = 128;
    protected PublicKey encryptionPublicKey;
    protected String encryptionAlgorithm = "AES";
    protected boolean encrypt;
    protected String canonicalizationMethodType = CanonicalizationMethod.EXCLUSIVE;

    public T canonicalizationMethod(String method) {
        this.canonicalizationMethodType = method;
        return (T)this;
    }

    public T signDocument() {
        this.sign = true;
        return (T)this;
    }

    public T signAssertions() {
        this.signAssertions = true;
        return (T)this;
    }

    public T signWith(String signingKeyName, KeyPair keyPair) {
        this.signingKeyName = signingKeyName;
        this.signingKeyPair = keyPair;
        return (T)this;
    }

    public T signWith(String signingKeyName, PrivateKey privateKey, PublicKey publicKey) {
        this.signingKeyName = signingKeyName;
        this.signingKeyPair = new KeyPair(publicKey, privateKey);
        return (T)this;
    }

    public T signWith(String signingKeyName, KeyPair keyPair, X509Certificate cert) {
        this.signingKeyName = signingKeyName;
        this.signingKeyPair = keyPair;
        this.signingCertificate = cert;
        return (T)this;
    }

    public T signWith(String signingKeyName, PrivateKey privateKey, PublicKey publicKey, X509Certificate cert) {
        this.signingKeyName = signingKeyName;
        this.signingKeyPair = new KeyPair(publicKey, privateKey);
        this.signingCertificate = cert;
        return (T)this;
    }

    public T signatureAlgorithm(SignatureAlgorithm alg) {
        this.signatureAlgorithm = alg;
        return (T)this;
    }

    public T encrypt(PublicKey publicKey) {
        encrypt = true;
        encryptionPublicKey = publicKey;
        return (T)this;
    }

    public T encryptionAlgorithm(String alg) {
        this.encryptionAlgorithm = alg;
        return (T)this;
    }

    public T encryptionKeySize(int size) {
        this.encryptionKeySize = size;
        return (T)this;
    }

    public T relayState(String relayState) {
        this.relayState = relayState;
        return (T)this;
    }

    public class BasePostBindingBuilder {
        protected Document document;
        protected BaseSAML2BindingBuilder builder;

        public BasePostBindingBuilder(BaseSAML2BindingBuilder builder, Document document) throws ProcessingException {
            this.builder = builder;
            this.document = document;
            if (builder.signAssertions) {
                builder.signAssertion(document);
            }
            if (builder.encrypt) builder.encryptDocument(document);
            if (builder.sign) {
                builder.signDocument(document);
            }
        }

        public String encoded() throws ProcessingException, ConfigurationException, IOException {
            byte[] responseBytes = DocumentUtil.getDocumentAsString(document).getBytes(GeneralConstants.SAML_CHARSET);
            return PostBindingUtil.base64Encode(new String(responseBytes, GeneralConstants.SAML_CHARSET));
        }
        public Document getDocument() {
            return document;
        }
        public String getHtmlResponse(String actionUrl) throws ProcessingException, ConfigurationException, IOException {
            String str = builder.buildHtmlPostResponse(document, actionUrl, false);
            return str;
        }
        public String getHtmlRequest(String actionUrl) throws ProcessingException, ConfigurationException, IOException {
            String str = builder.buildHtmlPostResponse(document, actionUrl, true);
            return str;
        }
        public String getRelayState() {
            return relayState;
        }
    }


    public static class BaseRedirectBindingBuilder {
        protected Document document;
        protected BaseSAML2BindingBuilder builder;

        public BaseRedirectBindingBuilder(BaseSAML2BindingBuilder builder, Document document) throws ProcessingException {
            this.builder = builder;
            this.document = document;
            if (builder.encrypt) builder.encryptDocument(document);
            if (builder.signAssertions) {
                builder.signAssertion(document);
            }
        }

        public Document getDocument() {
            return document;
        }
        public URI generateURI(String redirectUri, boolean asRequest) throws ConfigurationException, ProcessingException, IOException {
            String samlParameterName = GeneralConstants.SAML_RESPONSE_KEY;

            if (asRequest) {
                samlParameterName = GeneralConstants.SAML_REQUEST_KEY;
            }

            return builder.generateRedirectUri(samlParameterName, redirectUri, document);
        }

        public URI requestURI(String actionUrl)  throws ConfigurationException, ProcessingException, IOException {
            return builder.generateRedirectUri(GeneralConstants.SAML_REQUEST_KEY, actionUrl, document);
        }
        public URI responseURI(String actionUrl)  throws ConfigurationException, ProcessingException, IOException {
            return builder.generateRedirectUri(GeneralConstants.SAML_RESPONSE_KEY, actionUrl, document);
        }
    }

    public BaseRedirectBindingBuilder redirectBinding(Document document) throws ProcessingException {
        return new BaseRedirectBindingBuilder(this, document);

    }

    public BasePostBindingBuilder postBinding(Document document) throws ProcessingException {
        return new BasePostBindingBuilder(this, document);

    }



    public String getSAMLNSPrefix(Document samlResponseDocument) {
        Node assertionElement = samlResponseDocument.getDocumentElement()
                .getElementsByTagNameNS(JBossSAMLURIConstants.ASSERTION_NSURI.get(), JBossSAMLConstants.ASSERTION.get()).item(0);

        if (assertionElement == null) {
            throw new IllegalStateException("Unable to find assertion in saml response document");
        }

        return assertionElement.getPrefix();
    }

    public void encryptDocument(Document samlDocument) throws ProcessingException {
        String samlNSPrefix = getSAMLNSPrefix(samlDocument);

        try {
            QName encryptedAssertionElementQName = new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(),
                    JBossSAMLConstants.ENCRYPTED_ASSERTION.get(), samlNSPrefix);

            byte[] secret = RandomSecret.createRandomSecret(encryptionKeySize / 8);
            SecretKey secretKey = new SecretKeySpec(secret, encryptionAlgorithm);

            // encrypt the Assertion element and replace it with a EncryptedAssertion element.
            XMLEncryptionUtil.encryptElement(new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(),
                            JBossSAMLConstants.ASSERTION.get(), samlNSPrefix), samlDocument, encryptionPublicKey,
                    secretKey, encryptionKeySize, encryptedAssertionElementQName, true);
        } catch (Exception e) {
            throw new ProcessingException("failed to encrypt", e);
        }

    }

    public void signDocument(Document samlDocument) throws ProcessingException {
        String signatureMethod = signatureAlgorithm.getXmlSignatureMethod();
        String signatureDigestMethod = signatureAlgorithm.getXmlSignatureDigestMethod();
        SAML2Signature samlSignature = new SAML2Signature();

        if (signatureMethod != null) {
            samlSignature.setSignatureMethod(signatureMethod);
        }

        if (signatureDigestMethod != null) {
            samlSignature.setDigestMethod(signatureDigestMethod);
        }

        Node nextSibling = samlSignature.getNextSiblingOfIssuer(samlDocument);

        samlSignature.setNextSibling(nextSibling);

        if (signingCertificate != null) {
            samlSignature.setX509Certificate(signingCertificate);
        }

        samlSignature.signSAMLDocument(samlDocument, signingKeyName, signingKeyPair, canonicalizationMethodType);
    }

    public void signAssertion(Document samlDocument) throws ProcessingException {
        Element originalAssertionElement = org.keycloak.saml.common.util.DocumentUtil.getChildElement(samlDocument.getDocumentElement(), new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(), JBossSAMLConstants.ASSERTION.get()));
        if (originalAssertionElement == null) return;
        Node clonedAssertionElement = originalAssertionElement.cloneNode(true);
        Document temporaryDocument;

        try {
            temporaryDocument = org.keycloak.saml.common.util.DocumentUtil.createDocument();
        } catch (ConfigurationException e) {
            throw new ProcessingException(e);
        }

        temporaryDocument.adoptNode(clonedAssertionElement);
        temporaryDocument.appendChild(clonedAssertionElement);

        signDocument(temporaryDocument);

        samlDocument.adoptNode(clonedAssertionElement);

        Element parentNode = (Element) originalAssertionElement.getParentNode();

        parentNode.replaceChild(clonedAssertionElement, originalAssertionElement);
    }

    public String buildHtmlPostResponse(Document responseDoc, String actionUrl, boolean asRequest) throws ProcessingException, ConfigurationException, IOException {
        return buildHtml(getSAMLResponse(responseDoc), actionUrl, asRequest);
    }

    public static String getSAMLResponse(Document responseDoc) throws ProcessingException, ConfigurationException, IOException {
        byte[] responseBytes = org.keycloak.saml.common.util.DocumentUtil.getDocumentAsString(responseDoc).getBytes(GeneralConstants.SAML_CHARSET);
        return PostBindingUtil.base64Encode(new String(responseBytes, GeneralConstants.SAML_CHARSET));
    }

    public String buildHtml(String samlResponse, String actionUrl, boolean asRequest) {
        StringBuilder builder = new StringBuilder();

        String key = GeneralConstants.SAML_RESPONSE_KEY;

        if (asRequest) {
            key = GeneralConstants.SAML_REQUEST_KEY;
        }

        builder.append("<HTML>")
          .append("<HEAD>")

          .append("<TITLE>Authentication Redirect</TITLE>")
          .append("</HEAD>")
          .append("<BODY Onload=\"document.forms[0].submit()\">")

          .append("<FORM METHOD=\"POST\" ACTION=\"").append(actionUrl).append("\">")
          .append("<INPUT TYPE=\"HIDDEN\" NAME=\"").append(key).append("\"").append(" VALUE=\"").append(samlResponse).append("\"/>");

        builder.append("<p>Redirecting, please wait.</p>");

        if (isNotNull(relayState)) {
            builder.append("<INPUT TYPE=\"HIDDEN\" NAME=\"RelayState\" " + "VALUE=\"").append(escapeAttribute(relayState)).append("\"/>");
        }

        builder.append("<NOSCRIPT>")
          .append("<P>JavaScript is disabled. We strongly recommend to enable it. Click the button below to continue.</P>")
          .append("<INPUT TYPE=\"SUBMIT\" VALUE=\"CONTINUE\" />")
          .append("</NOSCRIPT>")

          .append("</FORM></BODY></HTML>");

        return builder.toString();
    }

    public String base64Encoded(Document document) throws ConfigurationException, ProcessingException, IOException  {
        String documentAsString = DocumentUtil.getDocumentAsString(document);
        logger.debugv("saml document: {0}", documentAsString);
        byte[] responseBytes = documentAsString.getBytes(GeneralConstants.SAML_CHARSET);

        return RedirectBindingUtil.deflateBase64Encode(responseBytes);
    }


    public URI generateRedirectUri(String samlParameterName, String redirectUri, Document document) throws ConfigurationException, ProcessingException, IOException {
        KeycloakUriBuilder builder = KeycloakUriBuilder.fromUri(redirectUri);
        int pos = builder.getQuery() == null? 0 : builder.getQuery().length();
        builder.queryParam(samlParameterName, base64Encoded(document));
        if (relayState != null) {
            builder.queryParam("RelayState", relayState);
        }

        if (sign) {
            builder.queryParam(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY, signatureAlgorithm.getXmlSignatureMethod());
            URI uri = builder.build();
            String rawQuery = uri.getRawQuery();
            if (pos > 0) {
                // just set in the signature the added SAML parameters
                rawQuery = rawQuery.substring(pos + 1);
            }
            Signature signature = signatureAlgorithm.createSignature();
            byte[] sig = new byte[0];
            try {
                signature.initSign(signingKeyPair.getPrivate());
                signature.update(rawQuery.getBytes(GeneralConstants.SAML_CHARSET));
                sig = signature.sign();
            } catch (InvalidKeyException | SignatureException e) {
                throw new ProcessingException(e);
            }
            String encodedSig = RedirectBindingUtil.base64Encode(sig);
            builder.queryParam(GeneralConstants.SAML_SIGNATURE_REQUEST_KEY, encodedSig);
        }
        return builder.build();
    }

 }
