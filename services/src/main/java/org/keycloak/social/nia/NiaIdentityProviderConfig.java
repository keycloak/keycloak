package org.keycloak.social.nia;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.common.enums.SslRequired;
import static org.keycloak.common.util.UriUtils.checkUrl;
import org.keycloak.dom.saml.v2.protocol.AuthnContextComparisonType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.saml.SamlPrincipalType;
import org.keycloak.saml.common.util.XmlKeyInfoKeyNameTransformer;

public class NiaIdentityProviderConfig extends SAMLIdentityProviderConfig {

    public static final XmlKeyInfoKeyNameTransformer DEFAULT_XML_KEY_INFO_KEY_NAME_TRANSFORMER = XmlKeyInfoKeyNameTransformer.NONE;

    public static final String ENTITY_ID = "entityId";
    public static final String ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO = "addExtensionsElementWithKeyInfo";
    public static final String BACKCHANNEL_SUPPORTED = "backchannelSupported";
    public static final String ENCRYPTION_PUBLIC_KEY = "encryptionPublicKey";
    public static final String FORCE_AUTHN = "forceAuthn";
    public static final String NAME_ID_POLICY_FORMAT = "nameIDPolicyFormat";
    public static final String POST_BINDING_AUTHN_REQUEST = "postBindingAuthnRequest";
    public static final String POST_BINDING_LOGOUT = "postBindingLogout";
    public static final String POST_BINDING_RESPONSE = "postBindingResponse";
    public static final String SIGNATURE_ALGORITHM = "signatureAlgorithm";
    public static final String SIGNING_CERTIFICATE_KEY = "signingCertificate";
    public static final String SINGLE_LOGOUT_SERVICE_URL = "singleLogoutServiceUrl";
    public static final String SINGLE_SIGN_ON_SERVICE_URL = "singleSignOnServiceUrl";
    public static final String VALIDATE_SIGNATURE = "validateSignature";
    public static final String PRINCIPAL_TYPE = "principalType";
    public static final String PRINCIPAL_ATTRIBUTE = "principalAttribute";
    public static final String WANT_ASSERTIONS_ENCRYPTED = "wantAssertionsEncrypted";
    public static final String WANT_ASSERTIONS_SIGNED = "wantAssertionsSigned";
    public static final String WANT_AUTHN_REQUESTS_SIGNED = "wantAuthnRequestsSigned";
    public static final String XML_SIG_KEY_INFO_KEY_NAME_TRANSFORMER = "xmlSigKeyInfoKeyNameTransformer";
    public static final String ENABLED_FROM_METADATA = "enabledFromMetadata";
    public static final String AUTHN_CONTEXT_COMPARISON_TYPE = "authnContextComparisonType";
    public static final String AUTHN_CONTEXT_CLASS_REFS = "authnContextClassRefs";
    public static final String AUTHN_CONTEXT_DECL_REFS = "authnContextDeclRefs";
    public static final String SIGN_SP_METADATA = "signSpMetadata";

    NiaIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
        initialize();
    }

    NiaIdentityProviderConfig() {
        super();
        initialize();
    }

    private void initialize() {
        this.setSignSpMetadata(true);
        this.setPostBindingAuthnRequest(true);
        this.setPostBindingResponse(true);
        this.setSingleSignOnServiceUrl("https://tnia.eidentita.cz/FPSTS/saml2/basic");

    }

    @Override
    public String getEntityId() {
        return getConfig().get(ENTITY_ID);
    }

    @Override
    public void setEntityId(String entityId) {
        getConfig().put(ENTITY_ID, entityId);
    }

    @Override
    public String getSingleSignOnServiceUrl() {
        return getConfig().get(SINGLE_SIGN_ON_SERVICE_URL);
    }

    @Override
    public void setSingleSignOnServiceUrl(String singleSignOnServiceUrl) {
        getConfig().put(SINGLE_SIGN_ON_SERVICE_URL, singleSignOnServiceUrl);
    }

    @Override
    public String getSingleLogoutServiceUrl() {
        return getConfig().get(SINGLE_LOGOUT_SERVICE_URL);
    }

    @Override
    public void setSingleLogoutServiceUrl(String singleLogoutServiceUrl) {
        getConfig().put(SINGLE_LOGOUT_SERVICE_URL, singleLogoutServiceUrl);
    }

    @Override
    public boolean isValidateSignature() {
        return Boolean.valueOf(getConfig().get(VALIDATE_SIGNATURE));
    }

    @Override
    public void setValidateSignature(boolean validateSignature) {
        getConfig().put(VALIDATE_SIGNATURE, String.valueOf(validateSignature));
    }

    @Override
    public boolean isForceAuthn() {
        return Boolean.valueOf(getConfig().get(FORCE_AUTHN));
    }

    @Override
    public void setForceAuthn(boolean forceAuthn) {
        getConfig().put(FORCE_AUTHN, String.valueOf(forceAuthn));
    }

    /**
     * @deprecated Prefer {@link #getSigningCertificates()}}
     * @param signingCertificate
     */
    @Override
    public String getSigningCertificate() {
        return getConfig().get(SIGNING_CERTIFICATE_KEY);
    }

    /**
     * @deprecated Prefer {@link #addSigningCertificate(String)}}
     * @param signingCertificate
     */
    @Override
    public void setSigningCertificate(String signingCertificate) {
        getConfig().put(SIGNING_CERTIFICATE_KEY, signingCertificate);
    }

    @Override
    public void addSigningCertificate(String signingCertificate) {
        String crt = getConfig().get(SIGNING_CERTIFICATE_KEY);
        if (crt == null || crt.isEmpty()) {
            getConfig().put(SIGNING_CERTIFICATE_KEY, signingCertificate);
        } else {
            // Note that "," is not coding character per PEM format specification:
            // see https://tools.ietf.org/html/rfc1421, section 4.3.2.4 Step 4: Printable Encoding
            getConfig().put(SIGNING_CERTIFICATE_KEY, crt + "," + signingCertificate);
        }
    }

    @Override
    public String[] getSigningCertificates() {
        String crt = getConfig().get(SIGNING_CERTIFICATE_KEY);
        if (crt == null || crt.isEmpty()) {
            return new String[]{};
        }
        // Note that "," is not coding character per PEM format specification:
        // see https://tools.ietf.org/html/rfc1421, section 4.3.2.4 Step 4: Printable Encoding
        return crt.split(",");
    }

    @Override
    public String getNameIDPolicyFormat() {
        return getConfig().get(NAME_ID_POLICY_FORMAT);
    }

    @Override
    public void setNameIDPolicyFormat(String nameIDPolicyFormat) {
        getConfig().put(NAME_ID_POLICY_FORMAT, nameIDPolicyFormat);
    }

    @Override
    public boolean isWantAuthnRequestsSigned() {
        return Boolean.valueOf(getConfig().get(WANT_AUTHN_REQUESTS_SIGNED));
    }

    @Override
    public void setWantAuthnRequestsSigned(boolean wantAuthnRequestsSigned) {
        getConfig().put(WANT_AUTHN_REQUESTS_SIGNED, String.valueOf(wantAuthnRequestsSigned));
    }

    @Override
    public boolean isWantAssertionsSigned() {
        return Boolean.valueOf(getConfig().get(WANT_ASSERTIONS_SIGNED));
    }

    @Override
    public void setWantAssertionsSigned(boolean wantAssertionsSigned) {
        getConfig().put(WANT_ASSERTIONS_SIGNED, String.valueOf(wantAssertionsSigned));
    }

    @Override
    public boolean isWantAssertionsEncrypted() {
        return Boolean.valueOf(getConfig().get(WANT_ASSERTIONS_ENCRYPTED));
    }

    @Override
    public void setWantAssertionsEncrypted(boolean wantAssertionsEncrypted) {
        getConfig().put(WANT_ASSERTIONS_ENCRYPTED, String.valueOf(wantAssertionsEncrypted));
    }

    @Override
    public boolean isAddExtensionsElementWithKeyInfo() {
        return Boolean.valueOf(getConfig().get(ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO));
    }

    @Override
    public void setAddExtensionsElementWithKeyInfo(boolean addExtensionsElementWithKeyInfo) {
        getConfig().put(ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO, String.valueOf(addExtensionsElementWithKeyInfo));
    }

    @Override
    public String getSignatureAlgorithm() {
        return getConfig().get(SIGNATURE_ALGORITHM);
    }

    @Override
    public void setSignatureAlgorithm(String signatureAlgorithm) {
        getConfig().put(SIGNATURE_ALGORITHM, signatureAlgorithm);
    }

    @Override
    public String getEncryptionPublicKey() {
        return getConfig().get(ENCRYPTION_PUBLIC_KEY);
    }

    @Override
    public void setEncryptionPublicKey(String encryptionPublicKey) {
        getConfig().put(ENCRYPTION_PUBLIC_KEY, encryptionPublicKey);
    }

    @Override
    public boolean isPostBindingAuthnRequest() {
        return Boolean.valueOf(getConfig().get(POST_BINDING_AUTHN_REQUEST));
    }

    @Override
    public void setPostBindingAuthnRequest(boolean postBindingAuthnRequest) {
        getConfig().put(POST_BINDING_AUTHN_REQUEST, String.valueOf(postBindingAuthnRequest));
    }

    @Override
    public boolean isPostBindingResponse() {
        return Boolean.valueOf(getConfig().get(POST_BINDING_RESPONSE));
    }

    @Override
    public void setPostBindingResponse(boolean postBindingResponse) {
        getConfig().put(POST_BINDING_RESPONSE, String.valueOf(postBindingResponse));
    }

    @Override
    public boolean isPostBindingLogout() {
        String postBindingLogout = getConfig().get(POST_BINDING_LOGOUT);
        if (postBindingLogout == null) {
            // To maintain unchanged behavior when adding this field, we set the inital value to equal that
            // of the binding for the response:
            return isPostBindingResponse();
        }
        return Boolean.valueOf(postBindingLogout);
    }

    @Override
    public void setPostBindingLogout(boolean postBindingLogout) {
        getConfig().put(POST_BINDING_LOGOUT, String.valueOf(postBindingLogout));
    }

    @Override
    public boolean isBackchannelSupported() {
        return Boolean.valueOf(getConfig().get(BACKCHANNEL_SUPPORTED));
    }

    @Override
    public void setBackchannelSupported(boolean backchannel) {
        getConfig().put(BACKCHANNEL_SUPPORTED, String.valueOf(backchannel));
    }

    /**
     * Always returns non-{@code null} result.
     *
     * @return Configured ransformer of
     * {@link #DEFAULT_XML_KEY_INFO_KEY_NAME_TRANSFORMER} if not set.
     */
    @Override
    public XmlKeyInfoKeyNameTransformer getXmlSigKeyInfoKeyNameTransformer() {
        return XmlKeyInfoKeyNameTransformer.from(getConfig().get(XML_SIG_KEY_INFO_KEY_NAME_TRANSFORMER), DEFAULT_XML_KEY_INFO_KEY_NAME_TRANSFORMER);
    }

    @Override
    public void setXmlSigKeyInfoKeyNameTransformer(XmlKeyInfoKeyNameTransformer xmlSigKeyInfoKeyNameTransformer) {
        getConfig().put(XML_SIG_KEY_INFO_KEY_NAME_TRANSFORMER,
                xmlSigKeyInfoKeyNameTransformer == null
                        ? null
                        : xmlSigKeyInfoKeyNameTransformer.name());
    }

    @Override
    public int getAllowedClockSkew() {
        int result = 0;
        String allowedClockSkew = getConfig().get(ALLOWED_CLOCK_SKEW);
        if (allowedClockSkew != null && !allowedClockSkew.isEmpty()) {
            try {
                result = Integer.parseInt(allowedClockSkew);
                if (result < 0) {
                    result = 0;
                }
            } catch (NumberFormatException e) {
                // ignore it and use 0
            }
        }
        return result;
    }

    @Override
    public void setAllowedClockSkew(int allowedClockSkew) {
        if (allowedClockSkew < 0) {
            getConfig().remove(ALLOWED_CLOCK_SKEW);
        } else {
            getConfig().put(ALLOWED_CLOCK_SKEW, String.valueOf(allowedClockSkew));
        }
    }

    @Override
    public SamlPrincipalType getPrincipalType() {
        return SamlPrincipalType.from(getConfig().get(PRINCIPAL_TYPE), SamlPrincipalType.SUBJECT);
    }

    @Override
    public void setPrincipalType(SamlPrincipalType principalType) {
        getConfig().put(PRINCIPAL_TYPE,
                principalType == null
                        ? null
                        : principalType.name());
    }

    @Override
    public String getPrincipalAttribute() {
        return getConfig().get(PRINCIPAL_ATTRIBUTE);
    }

    @Override
    public void setPrincipalAttribute(String principalAttribute) {
        getConfig().put(PRINCIPAL_ATTRIBUTE, principalAttribute);
    }

    @Override
    public boolean isEnabledFromMetadata() {
        return Boolean.valueOf(getConfig().get(ENABLED_FROM_METADATA));
    }

    @Override
    public void setEnabledFromMetadata(boolean enabled) {
        getConfig().put(ENABLED_FROM_METADATA, String.valueOf(enabled));
    }

    @Override
    public AuthnContextComparisonType getAuthnContextComparisonType() {
        return AuthnContextComparisonType.fromValue(getConfig().getOrDefault(AUTHN_CONTEXT_COMPARISON_TYPE, AuthnContextComparisonType.EXACT.value()));
    }

    @Override
    public void setAuthnContextComparisonType(AuthnContextComparisonType authnContextComparisonType) {
        getConfig().put(AUTHN_CONTEXT_COMPARISON_TYPE, authnContextComparisonType.value());
    }

    @Override
    public String getAuthnContextClassRefs() {
        return getConfig().get(AUTHN_CONTEXT_CLASS_REFS);
    }

    @Override
    public void setAuthnContextClassRefs(String authnContextClassRefs) {
        getConfig().put(AUTHN_CONTEXT_CLASS_REFS, authnContextClassRefs);
    }

    @Override
    public String getAuthnContextDeclRefs() {
        return getConfig().get(AUTHN_CONTEXT_DECL_REFS);
    }

    @Override
    public void setAuthnContextDeclRefs(String authnContextDeclRefs) {
        getConfig().put(AUTHN_CONTEXT_DECL_REFS, authnContextDeclRefs);
    }

    @Override
    public boolean isSignSpMetadata() {
        return Boolean.valueOf(getConfig().get(SIGN_SP_METADATA));
    }

    @Override
    public void setSignSpMetadata(boolean signSpMetadata) {
        getConfig().put(SIGN_SP_METADATA, String.valueOf(signSpMetadata));
    }

    @Override
    public void validate(RealmModel realm) {
        SslRequired sslRequired = realm.getSslRequired();

        checkUrl(sslRequired, getSingleLogoutServiceUrl(), SINGLE_LOGOUT_SERVICE_URL);
        checkUrl(sslRequired, getSingleSignOnServiceUrl(), SINGLE_SIGN_ON_SERVICE_URL);
    }
}
