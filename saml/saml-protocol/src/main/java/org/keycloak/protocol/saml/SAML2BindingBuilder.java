package org.keycloak.protocol.saml;

import org.picketlink.common.constants.GeneralConstants;
import org.picketlink.common.constants.JBossSAMLConstants;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.common.util.DocumentUtil;
import org.picketlink.identity.federation.api.saml.v2.sig.SAML2Signature;
import org.picketlink.identity.federation.core.util.XMLEncryptionUtil;
import org.picketlink.identity.federation.core.wstrust.WSTrustUtil;
import org.picketlink.identity.federation.web.util.PostBindingUtil;
import org.picketlink.identity.federation.web.util.RedirectBindingUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;

import static org.picketlink.common.util.StringUtil.isNotNull;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SAML2BindingBuilder<T extends SAML2BindingBuilder> {
    protected KeyPair signingKeyPair;
    protected X509Certificate signingCertificate;
    protected boolean sign;
    protected boolean signAssertions;
    protected SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RSA_SHA1;
    protected String relayState;
    protected String destination;
    protected String responseIssuer;
    protected int encryptionKeySize = 128;
    protected PublicKey encryptionPublicKey;
    protected String encryptionAlgorithm = "AES";
    protected boolean encrypt;

    public T signDocument() {
        this.sign = true;
        return (T)this;
    }

    public T signAssertions() {
        this.signAssertions = true;
        return (T)this;
    }

    public T signWith(KeyPair keyPair) {
        this.signingKeyPair = keyPair;
        return (T)this;
    }

    public T signWith(PrivateKey privateKey, PublicKey publicKey) {
        this.signingKeyPair = new KeyPair(publicKey, privateKey);
        return (T)this;
    }

    public T signWith(KeyPair keyPair, X509Certificate cert) {
        this.signingKeyPair = keyPair;
        this.signingCertificate = cert;
        return (T)this;
    }

    public T signWith(PrivateKey privateKey, PublicKey publicKey, X509Certificate cert) {
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

    public T destination(String destination) {
        this.destination = destination;
        return (T)this;
    }

    public T responseIssuer(String issuer) {
        this.responseIssuer = issuer;
        return (T)this;
    }

    public T relayState(String relayState) {
        this.relayState = relayState;
        return (T)this;
    }

    public class PostBindingBuilder {
        protected Document document;

        public PostBindingBuilder(Document document) throws ProcessingException {
            this.document = document;
            if (signAssertions) {
                signAssertion(document);
            }
            if (sign) {
                signDocument(document);
            }
        }

        public String encoded() throws ProcessingException, ConfigurationException, IOException {
            byte[] responseBytes = org.picketlink.identity.federation.core.saml.v2.util.DocumentUtil.getDocumentAsString(document).getBytes("UTF-8");
            return PostBindingUtil.base64Encode(new String(responseBytes));
        }
        public Document getDocument() {
            return document;
        }

        public String htmlResponse() throws ProcessingException, ConfigurationException, IOException {
            return buildHtml(encoded());

        }
        public Response response() throws ConfigurationException, ProcessingException, IOException {
            return buildResponse(document);
        }
    }


    public class RedirectBindingBuilder {
        protected Document document;

        public RedirectBindingBuilder(Document document) throws ProcessingException {
            this.document = document;
            if (signAssertions) {
                signAssertion(document);
            }
        }

        public Document getDocument() {
            return document;
        }
        public URI responseUri() throws ConfigurationException, ProcessingException, IOException {
            return generateRedirectUri("SAMLResponse", document);
        }
        public Response response() throws ProcessingException, ConfigurationException, IOException {
            URI uri = responseUri();

            CacheControl cacheControl = new CacheControl();
            cacheControl.setNoCache(true);
            return Response.status(302).location(uri)
                    .header("Pragma", "no-cache")
                    .header("Cache-Control", "no-cache, no-store").build();
        }

    }



    private String getSAMLNSPrefix(Document samlResponseDocument) {
        Node assertionElement = samlResponseDocument.getDocumentElement()
                .getElementsByTagNameNS(JBossSAMLURIConstants.ASSERTION_NSURI.get(), JBossSAMLConstants.ASSERTION.get()).item(0);

        if (assertionElement == null) {
            throw new IllegalStateException("Unable to find assertion in saml response document");
        }

        return assertionElement.getPrefix();
    }

    protected void encryptDocument(Document samlDocument) throws ProcessingException {
        String samlNSPrefix = getSAMLNSPrefix(samlDocument);

        try {
            QName encryptedAssertionElementQName = new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(),
                    JBossSAMLConstants.ENCRYPTED_ASSERTION.get(), samlNSPrefix);

            byte[] secret = WSTrustUtil.createRandomSecret(encryptionKeySize / 8);
            SecretKey secretKey = new SecretKeySpec(secret, encryptionAlgorithm);

            // encrypt the Assertion element and replace it with a EncryptedAssertion element.
            XMLEncryptionUtil.encryptElement(new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(),
                            JBossSAMLConstants.ASSERTION.get(), samlNSPrefix), samlDocument, encryptionPublicKey,
                    secretKey, encryptionKeySize, encryptedAssertionElementQName, true);
        } catch (Exception e) {
            throw new ProcessingException("failed to encrypt", e);
        }

    }

    protected void signDocument(Document samlDocument) throws ProcessingException {
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

        samlSignature.signSAMLDocument(samlDocument, signingKeyPair);
    }

    protected void signAssertion(Document samlDocument) throws ProcessingException {
        Element originalAssertionElement = DocumentUtil.getChildElement(samlDocument.getDocumentElement(), new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(), JBossSAMLConstants.ASSERTION.get()));
        if (originalAssertionElement == null) return;
        Node clonedAssertionElement = originalAssertionElement.cloneNode(true);
        Document temporaryDocument;

        try {
            temporaryDocument = DocumentUtil.createDocument();
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


    protected Response buildResponse(Document responseDoc) throws ProcessingException, ConfigurationException, IOException {
        String str = buildHtmlPostResponse(responseDoc);

        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        return Response.ok(str, MediaType.TEXT_HTML_TYPE)
                       .header("Pragma", "no-cache")
                       .header("Cache-Control", "no-cache, no-store").build();
    }

    protected String buildHtmlPostResponse(Document responseDoc) throws ProcessingException, ConfigurationException, IOException {
        byte[] responseBytes = DocumentUtil.getDocumentAsString(responseDoc).getBytes("UTF-8");
        String samlResponse = PostBindingUtil.base64Encode(new String(responseBytes));

        return buildHtml(samlResponse);
    }

    protected String buildHtml(String samlResponse) {
        if (destination == null) {
            throw SALM2LoginResponseBuilder.logger.nullValueError("Destination is null");
        }

        StringBuilder builder = new StringBuilder();

        String key = GeneralConstants.SAML_RESPONSE_KEY;
        builder.append("<HTML>");
        builder.append("<HEAD>");

        builder.append("<TITLE>HTTP Post Binding Response (Response)</TITLE>");
        builder.append("</HEAD>");
        builder.append("<BODY Onload=\"document.forms[0].submit()\">");

        builder.append("<FORM METHOD=\"POST\" ACTION=\"" + destination + "\">");
        builder.append("<INPUT TYPE=\"HIDDEN\" NAME=\"" + key + "\"" + " VALUE=\"" + samlResponse + "\"/>");

        if (isNotNull(relayState)) {
            builder.append("<INPUT TYPE=\"HIDDEN\" NAME=\"RelayState\" " + "VALUE=\"" + relayState + "\"/>");
        }

        builder.append("<NOSCRIPT>");
        builder.append("<P>JavaScript is disabled. We strongly recommend to enable it. Click the button below to continue.</P>");
        builder.append("<INPUT TYPE=\"SUBMIT\" VALUE=\"CONTINUE\" />");
        builder.append("</NOSCRIPT>");

        builder.append("</FORM></BODY></HTML>");

        return builder.toString();
    }

    protected String base64Encoded(Document document) throws ConfigurationException, ProcessingException, IOException  {
        byte[] responseBytes = org.picketlink.identity.federation.core.saml.v2.util.DocumentUtil.getDocumentAsString(document).getBytes("UTF-8");

        return RedirectBindingUtil.deflateBase64URLEncode(responseBytes);
    }


    protected URI generateRedirectUri(String samlParameterName, Document document) throws ConfigurationException, ProcessingException, IOException {
        UriBuilder builder = UriBuilder.fromUri(destination)
                .replaceQuery(null)
                .queryParam(samlParameterName, base64Encoded(document));
        if (relayState != null) {
            builder.queryParam("RelayState", relayState);
        }

        if (sign) {
            builder.queryParam(GeneralConstants.SAML_SIG_ALG_REQUEST_KEY, signatureAlgorithm.getJavaSignatureAlgorithm());
            URI uri = builder.build();
            String rawQuery = uri.getRawQuery();
            Signature signature = signatureAlgorithm.createSignature();
            byte[] sig = new byte[0];
            try {
                signature.initSign(signingKeyPair.getPrivate());
                signature.update(rawQuery.getBytes("UTF-8"));
                sig = signature.sign();
            } catch (Exception e) {
                throw new ProcessingException(e);
            }
            String encodedSig = RedirectBindingUtil.base64URLEncode(sig);
            builder.queryParam(GeneralConstants.SAML_SIGNATURE_REQUEST_KEY, encodedSig);
        }
        return builder.build();
    }


}
