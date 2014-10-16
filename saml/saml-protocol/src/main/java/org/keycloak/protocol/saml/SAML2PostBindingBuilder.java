package org.keycloak.protocol.saml;

import org.picketlink.common.constants.GeneralConstants;
import org.picketlink.common.constants.JBossSAMLConstants;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.common.util.DocumentUtil;
import org.picketlink.identity.federation.core.util.XMLEncryptionUtil;
import org.picketlink.identity.federation.core.wstrust.WSTrustUtil;
import org.picketlink.identity.federation.web.util.PostBindingUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import static org.picketlink.common.util.StringUtil.isNotNull;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SAML2PostBindingBuilder<T extends SAML2PostBindingBuilder> {
    protected KeyPair signingKeyPair;
    protected X509Certificate signingCertificate;
    protected boolean signed;
    protected String signatureDigestMethod;
    protected String signatureMethod;
    protected String relayState;
    protected String destination;
    protected String responseIssuer;
    protected int encryptionKeySize = 128;
    protected PublicKey encryptionPublicKey;
    protected String encryptionAlgorithm = "AES";
    protected boolean encrypt;

    public T sign(KeyPair keyPair) {
        this.signingKeyPair = keyPair;
        this.signed = true;
        return (T)this;
    }

    public T sign(PrivateKey privateKey, PublicKey publicKey) {
        this.signingKeyPair = new KeyPair(publicKey, privateKey);
        this.signed = true;
        return (T)this;
    }

    public T sign(KeyPair keyPair, X509Certificate cert) {
        this.signingKeyPair = keyPair;
        this.signingCertificate = cert;
        this.signed = true;
        return (T)this;
    }

    public T sign(PrivateKey privateKey, PublicKey publicKey, X509Certificate cert) {
        this.signingKeyPair = new KeyPair(publicKey, privateKey);
        this.signingCertificate = cert;
        this.signed = true;
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

    public T signatureDigestMethod(String method) {
        this.signatureDigestMethod = method;
        return (T)this;
    }

    public T signatureMethod(String method) {
        this.signatureMethod = method;
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

            byte[] secret = WSTrustUtil.createRandomSecret(128 / 8);
            SecretKey secretKey = new SecretKeySpec(secret, encryptionAlgorithm);

            // encrypt the Assertion element and replace it with a EncryptedAssertion element.
            XMLEncryptionUtil.encryptElement(new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(),
                            JBossSAMLConstants.ASSERTION.get(), samlNSPrefix), samlDocument, encryptionPublicKey,
                    secretKey, encryptionKeySize, encryptedAssertionElementQName, true);
        } catch (Exception e) {
            throw new ProcessingException("failed to encrypt", e);
        }

    }

    protected void encryptAndSign(Document samlDocument) throws ProcessingException {
        if (encrypt) {
            encryptDocument(samlDocument);
            signDocument(samlDocument);
            return;
        }
        if (signed) {
            signDocument(samlDocument);
            return;
        }
    }

    protected void signDocument(Document samlDocument) throws ProcessingException {
        SamlProtocolUtils.signDocument(samlDocument, signingKeyPair, signatureMethod, signatureDigestMethod, signingCertificate);
    }

    protected Response buildResponse(Document responseDoc) throws ProcessingException, ConfigurationException, IOException {
        byte[] responseBytes = DocumentUtil.getDocumentAsString(responseDoc).getBytes("UTF-8");
        String samlResponse = PostBindingUtil.base64Encode(new String(responseBytes));

        if (destination == null) {
            throw SALM2PostBindingLoginResponseBuilder.logger.nullValueError("Destination is null");
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

        String str = builder.toString();

        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        return Response.ok(str, MediaType.TEXT_HTML_TYPE)
                       .header("Pragma", "no-cache")
                       .header("Cache-Control", "no-cache, no-store").build();
    }

}
