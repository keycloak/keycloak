package org.keycloak.protocol.saml;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.saml.SignatureAlgorithm;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlClientTemplate {
    protected ClientTemplateModel clientTemplate;

    public SamlClientTemplate(ClientTemplateModel template) {
        this.clientTemplate = template;
    }

    public String getId() {
        return clientTemplate.getId();
    }

//

    public String getCanonicalizationMethod() {
        return clientTemplate.getAttribute(SamlConfigAttributes.SAML_CANONICALIZATION_METHOD_ATTRIBUTE);
    }

    public void setCanonicalizationMethod(String value) {
        clientTemplate.setAttribute(SamlConfigAttributes.SAML_CANONICALIZATION_METHOD_ATTRIBUTE, value);
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        String alg = null;
        alg = clientTemplate.getAttribute(SamlConfigAttributes.SAML_CANONICALIZATION_METHOD_ATTRIBUTE);
        if (alg != null) {
            SignatureAlgorithm algorithm = SignatureAlgorithm.valueOf(alg);
            if (algorithm != null)
                return algorithm;
        }
        return SignatureAlgorithm.RSA_SHA256;
    }

    public void setSignatureAlgorithm(SignatureAlgorithm algorithm) {
        clientTemplate.setAttribute(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM, algorithm.name());
    }

    public String getNameIDFormat() {
        return clientTemplate.getAttributes().get(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE);
    }
    public void setNameIDFormat(String format) {
        clientTemplate.setAttribute(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, format);
    }

    public boolean includeAuthnStatement() {
        return "true".equals(clientTemplate.getAttribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT));
    }

    public void setIncludeAuthnStatement(boolean val) {
        clientTemplate.setAttribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, Boolean.toString(val));
    }

    public boolean forceNameIDFormat() {
        return "true".equals(clientTemplate.getAttribute(SamlConfigAttributes.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE));

    }
    public void setForceNameIDFormat(boolean val) {
        clientTemplate.setAttribute(SamlConfigAttributes.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE, Boolean.toString(val));
    }

    public boolean requiresRealmSignature() {
        return "true".equals(clientTemplate.getAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE));
    }

    public void setRequiresRealmSignature(boolean val) {
        clientTemplate.setAttribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, Boolean.toString(val));

    }

    public boolean forcePostBinding() {
        return "true".equals(clientTemplate.getAttribute(SamlConfigAttributes.SAML_FORCE_POST_BINDING));
    }

    public void setForcePostBinding(boolean val) {
        clientTemplate.setAttribute(SamlConfigAttributes.SAML_FORCE_POST_BINDING, Boolean.toString(val));

    }
    public boolean requiresAssertionSignature() {
        return "true".equals(clientTemplate.getAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE));
    }

    public void setRequiresAssertionSignature(boolean val) {
        clientTemplate.setAttribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE   , Boolean.toString(val));

    }
    public boolean requiresEncryption() {
        return "true".equals(clientTemplate.getAttribute(SamlConfigAttributes.SAML_ENCRYPT));
    }


    public void setRequiresEncryption(boolean val) {
        clientTemplate.setAttribute(SamlConfigAttributes.SAML_ENCRYPT, Boolean.toString(val));

    }

    public boolean requiresClientSignature() {
        return "true".equals(clientTemplate.getAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE));
    }

    public void setRequiresClientSignature(boolean val) {
        clientTemplate.setAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE   , Boolean.toString(val));

    }

    public String getClientSigningCertificate() {
        return clientTemplate.getAttribute(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE);
    }

    public void setClientSigningCertificate(String val) {
        clientTemplate.setAttribute(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE, val);

    }

    public String getClientSigningPrivateKey() {
        return clientTemplate.getAttribute(SamlConfigAttributes.SAML_SIGNING_PRIVATE_KEY);
    }

    public void setClientSigningPrivateKey(String val) {
        clientTemplate.setAttribute(SamlConfigAttributes.SAML_SIGNING_PRIVATE_KEY, val);

    }
}
