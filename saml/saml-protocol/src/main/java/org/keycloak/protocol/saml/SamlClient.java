package org.keycloak.protocol.saml;

import org.keycloak.models.ClientModel;
import org.keycloak.saml.SignatureAlgorithm;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlClient {
    public static final String SAML_SIGNING_PRIVATE_KEY = "saml.signing.private.key";
    protected ClientModel client;

    public SamlClient(ClientModel client) {
        this.client = client;
    }

    public String getCanonicalizationMethod() {
        return client.getAttribute(SamlProtocol.SAML_CANONICALIZATION_METHOD_ATTRIBUTE);
    }

    public void setCanonicalizationMethod(String value) {
        client.setAttribute(SamlProtocol.SAML_CANONICALIZATION_METHOD_ATTRIBUTE, value);
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        String alg = client.getAttribute(SamlProtocol.SAML_SIGNATURE_ALGORITHM);
        if (alg != null) {
            SignatureAlgorithm algorithm = SignatureAlgorithm.valueOf(alg);
            if (algorithm != null)
                return algorithm;
        }
        return SignatureAlgorithm.RSA_SHA256;
    }

    public void setSignatureAlgorithm(SignatureAlgorithm algorithm) {
        client.setAttribute(SamlProtocol.SAML_SIGNATURE_ALGORITHM, algorithm.name());
    }

    public String getNameIDFormat() {
        return client.getAttributes().get(SamlProtocol.SAML_NAME_ID_FORMAT_ATTRIBUTE);
    }
    public void setNameIDFormat(String format) {
        client.setAttribute(SamlProtocol.SAML_NAME_ID_FORMAT_ATTRIBUTE, format);
    }

    public boolean includeAuthnStatement() {
        return "true".equals(client.getAttribute(SamlProtocol.SAML_AUTHNSTATEMENT));
    }

    public void setIncludeAuthnStatement(boolean val) {
        client.setAttribute(SamlProtocol.SAML_AUTHNSTATEMENT, Boolean.toString(val));
    }

    public boolean forceNameIDFormat() {
        return "true".equals(client.getAttribute(SamlProtocol.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE));

    }
    public void setForceNameIDFormat(boolean val) {
        client.setAttribute(SamlProtocol.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE, Boolean.toString(val));
    }

    public boolean requiresRealmSignature(ClientModel client) {
        return "true".equals(client.getAttribute(SamlProtocol.SAML_SERVER_SIGNATURE));
    }

    public void setRequiresRealmSignature(boolean val) {
        client.setAttribute(SamlProtocol.SAML_SERVER_SIGNATURE, Boolean.toString(val));

    }

    public boolean forcePostBinding(ClientModel client) {
        return "true".equals(client.getAttribute(SamlProtocol.SAML_FORCE_POST_BINDING));
    }

    public void setForcePostBinding(boolean val) {
        client.setAttribute(SamlProtocol.SAML_FORCE_POST_BINDING, Boolean.toString(val));

    }
    public boolean samlAssertionSignature(ClientModel client) {
        return "true".equals(client.getAttribute(SamlProtocol.SAML_ASSERTION_SIGNATURE));
    }

    public void setAssertionSignature(boolean val) {
        client.setAttribute(SamlProtocol.SAML_ASSERTION_SIGNATURE   , Boolean.toString(val));

    }
    public boolean requiresEncryption(ClientModel client) {
        return "true".equals(client.getAttribute(SamlProtocol.SAML_ENCRYPT));
    }


    public void setRequiresEncryption(boolean val) {
        client.setAttribute(SamlProtocol.SAML_ENCRYPT, Boolean.toString(val));

    }

    public boolean requiresClientSignature(ClientModel client) {
        return "true".equals(client.getAttribute(SamlProtocol.SAML_CLIENT_SIGNATURE_ATTRIBUTE));
    }

    public void setRequiresClientSignature(boolean val) {
        client.setAttribute(SamlProtocol.SAML_CLIENT_SIGNATURE_ATTRIBUTE   , Boolean.toString(val));

    }

    public String getClientSigningCertificate() {
        return client.getAttribute(SamlProtocol.SAML_SIGNING_CERTIFICATE_ATTRIBUTE);
    }

    public void setClientSigningCertificate(String val) {
        client.setAttribute(SamlProtocol.SAML_SIGNING_CERTIFICATE_ATTRIBUTE, val);

    }

    public String getClientSigningPrivateKey() {
        return client.getAttribute(SAML_SIGNING_PRIVATE_KEY);
    }

    public void setClientSigningPrivateKey(String val) {
        client.setAttribute(SAML_SIGNING_PRIVATE_KEY, val);

    }



}
