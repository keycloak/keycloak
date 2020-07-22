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
package org.keycloak.broker.saml;

import static org.keycloak.common.util.UriUtils.checkUrl;

import java.io.IOException;
import java.util.List;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.dom.saml.v2.protocol.AuthnContextComparisonType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.saml.SamlPrincipalType;
import org.keycloak.saml.common.util.XmlKeyInfoKeyNameTransformer;
import org.keycloak.util.JsonSerialization;

/**
 * @author Pedro Igor
 */
public class SAMLIdentityProviderConfig extends IdentityProviderModel {

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
    public static final String ENABLED_FROM_METADATA  = "enabledFromMetadata";
    public static final String AUTHN_CONTEXT_COMPARISON_TYPE = "authnContextComparisonType";
    public static final String AUTHN_CONTEXT_CLASS_REFS = "authnContextClassRefs";
    public static final String AUTHN_CONTEXT_DECL_REFS = "authnContextDeclRefs";   
    public static final String SIGN_SP_METADATA = "signSpMetadata"; 
    // description extensions
    public static final String MDRPI_REGISTRATION_AUTHORITY = "mdrpiRegistrationAuthority";
    public static final String MDRPI_REGISTRATION_POLICY = "mdrpiRegistrationPolicy";
    // UIinfo fields
    public static final String MDUI_DISPLAY_NAME = "mduiDisplayName";
    public static final String MDUI_DESCRIPTION = "mduiDescription";
    public static final String MDUI_INFORMATION_URL = "mduiInformationURL";
    public static final String MDUI_PRIVACY_STATEMENT_URL = "mduiPrivacyStatementURL";
    public static final String MDUI_LOGO = "mduiLogo";
    public static final String MDUI_LOGO_HEIGHT = "mduiLogoHeight";
    public static final String MDUI_LOGO_WIDTH = "mduiLogoWidth";
    // organization- contact person
    public static final String MD_ORGANIZATION_NAME = "mdOrganizationName";
    public static final String MD_ORGANIZATION_DISPLAY_NAME = "mdOrganizationDisplayName";
    public static final String MD_ORGANIZATION_URL = "mdOrganizationURL";
    public static final String MD_CONTACT_TYPE = "mdContactType";
    public static final String MD_CONTACT_COMPANY = "mdContactCompany";
    public static final String MD_CONTACT_SURNAME = "mdContactSurname";
    public static final String MD_CONTACT_GIVEN_NAME = "mdContactGivenName";
    public static final String MD_CONTACT_EMAIL_ADDRESS = "mdContactEmailAddress";
    public static final String MD_CONTACT_TELEPHONE_NUMBER = "mdContactTelephoneNumber";
   
    public SAMLIdentityProviderConfig() {
    }

    public SAMLIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }

    public String getEntityId() {
        return getConfig().get(ENTITY_ID);
    }

    public void setEntityId(String entityId) {
        getConfig().put(ENTITY_ID, entityId);
    }

    public String getSingleSignOnServiceUrl() {
        return getConfig().get(SINGLE_SIGN_ON_SERVICE_URL);
    }

    public void setSingleSignOnServiceUrl(String singleSignOnServiceUrl) {
        getConfig().put(SINGLE_SIGN_ON_SERVICE_URL, singleSignOnServiceUrl);
    }

    public String getSingleLogoutServiceUrl() {
        return getConfig().get(SINGLE_LOGOUT_SERVICE_URL);
    }

    public void setSingleLogoutServiceUrl(String singleLogoutServiceUrl) {
        getConfig().put(SINGLE_LOGOUT_SERVICE_URL, singleLogoutServiceUrl);
    }

    public boolean isValidateSignature() {
        return Boolean.valueOf(getConfig().get(VALIDATE_SIGNATURE));
    }

    public void setValidateSignature(boolean validateSignature) {
        getConfig().put(VALIDATE_SIGNATURE, String.valueOf(validateSignature));
    }

    public boolean isForceAuthn() {
        return Boolean.valueOf(getConfig().get(FORCE_AUTHN));
    }

    public void setForceAuthn(boolean forceAuthn) {
        getConfig().put(FORCE_AUTHN, String.valueOf(forceAuthn));
    }

    /**
     * @deprecated Prefer {@link #getSigningCertificates()}}
     * @param signingCertificate
     */
    public String getSigningCertificate() {
        return getConfig().get(SIGNING_CERTIFICATE_KEY);
    }

    /**
     * @deprecated Prefer {@link #addSigningCertificate(String)}}
     * @param signingCertificate
     */
    public void setSigningCertificate(String signingCertificate) {
        getConfig().put(SIGNING_CERTIFICATE_KEY, signingCertificate);
    }

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

    public String[] getSigningCertificates() {
        String crt = getConfig().get(SIGNING_CERTIFICATE_KEY);
        if (crt == null || crt.isEmpty()) {
            return new String[] { };
        }
        // Note that "," is not coding character per PEM format specification:
        // see https://tools.ietf.org/html/rfc1421, section 4.3.2.4 Step 4: Printable Encoding
        return crt.split(",");
    }

    public String getNameIDPolicyFormat() {
        return getConfig().get(NAME_ID_POLICY_FORMAT);
    }

    public void setNameIDPolicyFormat(String nameIDPolicyFormat) {
        getConfig().put(NAME_ID_POLICY_FORMAT, nameIDPolicyFormat);
    }

    public boolean isWantAuthnRequestsSigned() {
        return Boolean.valueOf(getConfig().get(WANT_AUTHN_REQUESTS_SIGNED));
    }

    public void setWantAuthnRequestsSigned(boolean wantAuthnRequestsSigned) {
        getConfig().put(WANT_AUTHN_REQUESTS_SIGNED, String.valueOf(wantAuthnRequestsSigned));
    }

    public boolean isWantAssertionsSigned() {
        return Boolean.valueOf(getConfig().get(WANT_ASSERTIONS_SIGNED));
    }

    public void setWantAssertionsSigned(boolean wantAssertionsSigned) {
        getConfig().put(WANT_ASSERTIONS_SIGNED, String.valueOf(wantAssertionsSigned));
    }

    public boolean isWantAssertionsEncrypted() {
        return Boolean.valueOf(getConfig().get(WANT_ASSERTIONS_ENCRYPTED));
    }

    public void setWantAssertionsEncrypted(boolean wantAssertionsEncrypted) {
        getConfig().put(WANT_ASSERTIONS_ENCRYPTED, String.valueOf(wantAssertionsEncrypted));
    }

    public boolean isAddExtensionsElementWithKeyInfo() {
        return Boolean.valueOf(getConfig().get(ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO));
    }

    public void setAddExtensionsElementWithKeyInfo(boolean addExtensionsElementWithKeyInfo) {
        getConfig().put(ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO, String.valueOf(addExtensionsElementWithKeyInfo));
    }

    public String getSignatureAlgorithm() {
        return getConfig().get(SIGNATURE_ALGORITHM);
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        getConfig().put(SIGNATURE_ALGORITHM, signatureAlgorithm);
    }

    public String getEncryptionPublicKey() {
        return getConfig().get(ENCRYPTION_PUBLIC_KEY);
    }

    public void setEncryptionPublicKey(String encryptionPublicKey) {
        getConfig().put(ENCRYPTION_PUBLIC_KEY, encryptionPublicKey);
    }

    public boolean isPostBindingAuthnRequest() {
        return Boolean.valueOf(getConfig().get(POST_BINDING_AUTHN_REQUEST));
    }

    public void setPostBindingAuthnRequest(boolean postBindingAuthnRequest) {
        getConfig().put(POST_BINDING_AUTHN_REQUEST, String.valueOf(postBindingAuthnRequest));
    }

    public boolean isPostBindingResponse() {
        return Boolean.valueOf(getConfig().get(POST_BINDING_RESPONSE));
    }

    public void setPostBindingResponse(boolean postBindingResponse) {
        getConfig().put(POST_BINDING_RESPONSE, String.valueOf(postBindingResponse));
    }

    public boolean isPostBindingLogout() {
        String postBindingLogout = getConfig().get(POST_BINDING_LOGOUT);
        if (postBindingLogout == null) {
            // To maintain unchanged behavior when adding this field, we set the inital value to equal that
            // of the binding for the response:
            return isPostBindingResponse();
        }
        return Boolean.valueOf(postBindingLogout);
    }

    public void setPostBindingLogout(boolean postBindingLogout) {
        getConfig().put(POST_BINDING_LOGOUT, String.valueOf(postBindingLogout));
    }

    public boolean isBackchannelSupported() {
        return Boolean.valueOf(getConfig().get(BACKCHANNEL_SUPPORTED));
    }

    public void setBackchannelSupported(boolean backchannel) {
        getConfig().put(BACKCHANNEL_SUPPORTED, String.valueOf(backchannel));
    }

    /**
     * Always returns non-{@code null} result.
     * @return Configured ransformer of {@link #DEFAULT_XML_KEY_INFO_KEY_NAME_TRANSFORMER} if not set.
     */
    public XmlKeyInfoKeyNameTransformer getXmlSigKeyInfoKeyNameTransformer() {
        return XmlKeyInfoKeyNameTransformer.from(getConfig().get(XML_SIG_KEY_INFO_KEY_NAME_TRANSFORMER), DEFAULT_XML_KEY_INFO_KEY_NAME_TRANSFORMER);
    }

    public void setXmlSigKeyInfoKeyNameTransformer(XmlKeyInfoKeyNameTransformer xmlSigKeyInfoKeyNameTransformer) {
        getConfig().put(XML_SIG_KEY_INFO_KEY_NAME_TRANSFORMER,
          xmlSigKeyInfoKeyNameTransformer == null
            ? null
            : xmlSigKeyInfoKeyNameTransformer.name());
    }

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

    public void setAllowedClockSkew(int allowedClockSkew) {
        if (allowedClockSkew < 0) {
            getConfig().remove(ALLOWED_CLOCK_SKEW);
        } else {
            getConfig().put(ALLOWED_CLOCK_SKEW, String.valueOf(allowedClockSkew));
        }
    }

    public SamlPrincipalType getPrincipalType() {
        return SamlPrincipalType.from(getConfig().get(PRINCIPAL_TYPE), SamlPrincipalType.SUBJECT);
    }

    public void setPrincipalType(SamlPrincipalType principalType) {
        getConfig().put(PRINCIPAL_TYPE,
            principalType == null
                ? null
                : principalType.name());
    }

    public String getPrincipalAttribute() {
        return getConfig().get(PRINCIPAL_ATTRIBUTE);
    }

    public void setPrincipalAttribute(String principalAttribute) {
        getConfig().put(PRINCIPAL_ATTRIBUTE, principalAttribute);
    }

    public boolean isEnabledFromMetadata() {
        return Boolean.valueOf(getConfig().get(ENABLED_FROM_METADATA ));
    }

    public void setEnabledFromMetadata(boolean enabled) {
        getConfig().put(ENABLED_FROM_METADATA , String.valueOf(enabled));
    }

    public AuthnContextComparisonType getAuthnContextComparisonType() {
        return AuthnContextComparisonType.fromValue(getConfig().getOrDefault(AUTHN_CONTEXT_COMPARISON_TYPE, AuthnContextComparisonType.EXACT.value()));
    }

    public void setAuthnContextComparisonType(AuthnContextComparisonType authnContextComparisonType) {
        getConfig().put(AUTHN_CONTEXT_COMPARISON_TYPE, authnContextComparisonType.value());
    }

    public String getAuthnContextClassRefs() {
        return getConfig().get(AUTHN_CONTEXT_CLASS_REFS);
    }

    public void setAuthnContextClassRefs(String authnContextClassRefs) {
        getConfig().put(AUTHN_CONTEXT_CLASS_REFS, authnContextClassRefs);
    }

    public String getAuthnContextDeclRefs() {
        return getConfig().get(AUTHN_CONTEXT_DECL_REFS);
    }

    public void setAuthnContextDeclRefs(String authnContextDeclRefs) {
        getConfig().put(AUTHN_CONTEXT_DECL_REFS, authnContextDeclRefs);
    }

    public boolean isSignSpMetadata() {
        return Boolean.valueOf(getConfig().get(SIGN_SP_METADATA));
    }

    public void setSignSpMetadata(boolean signSpMetadata) {
        getConfig().put(SIGN_SP_METADATA, String.valueOf(signSpMetadata));
    }

    public String getΜdrpiRegistrationAuthority() {
        return getConfig().get(MDRPI_REGISTRATION_AUTHORITY);
    }

    public void setΜdrpiRegistrationAuthority(String mdrpiRegistrationAuthority) {
        getConfig().put(MDRPI_REGISTRATION_AUTHORITY, mdrpiRegistrationAuthority);
    }

    public String getΜdrpiRegistrationPolicy() {
        return getConfig().get(MDRPI_REGISTRATION_POLICY);
    }

    public void setΜdrpiRegistrationPolicy(String mdrpiRegistrationPolicy) {
        getConfig().put(MDRPI_REGISTRATION_POLICY, mdrpiRegistrationPolicy);
    }

    public String getConfigMduiDisplayName() {
        return getConfig().get(MDUI_DISPLAY_NAME);
    }

    public void setConfigMduiDisplayName(String mduiDisplayName) {
        getConfig().put(MDUI_DISPLAY_NAME, mduiDisplayName);
    }

    public String getMduiDescription() {
        return getConfig().get(MDUI_DESCRIPTION);
    }

    public void setMduiDescription(String mduiDescription) {
        getConfig().put(MDUI_DESCRIPTION, mduiDescription);
    }

    public String getMduiInformationURL() {
        return getConfig().get(MDUI_INFORMATION_URL);
    }

    public void setMduiInformationURL(String mduiInformationURL) {
        getConfig().put(MDUI_INFORMATION_URL, mduiInformationURL);
    }

    public String getMduiPrivacyStatementURL() {
        return getConfig().get(MDUI_PRIVACY_STATEMENT_URL);
    }

    public void setMduiPrivacyStatementURL(String mduiPrivacyStatementURL) {
        getConfig().put(MDUI_PRIVACY_STATEMENT_URL, mduiPrivacyStatementURL);
    }

    public String getMduiLogo() {
        return getConfig().get(MDUI_LOGO);
    }

    public void setMduiLogo(String mduiLogo) {
        getConfig().put(MDUI_LOGO, mduiLogo);
    }

    public Integer getMduiLogoHeight() {
        return getConfig().get(MDUI_LOGO_HEIGHT) == null ? null : Integer.valueOf(getConfig().get(MDUI_LOGO_HEIGHT));
    }

    public void setMduiLogoHeight(Integer mduiLogoHeight) {
        getConfig().put(MDUI_LOGO_HEIGHT, String.valueOf(mduiLogoHeight));
    }

    public Integer getMduiLogoWidth() {
        return getConfig().get(MDUI_LOGO_WIDTH) == null ? null : Integer.valueOf(getConfig().get(MDUI_LOGO_WIDTH));
    }

    public void setMduiLogoWidth(Integer mduiLogoWidth) {
        getConfig().put(MDUI_LOGO_WIDTH, String.valueOf(mduiLogoWidth));
    }

    public String getMdOrganizationName() {
        return getConfig().get(MD_ORGANIZATION_NAME);
    }

    public void setMdOrganizationName(String mdOrganizationName) {
        getConfig().put(MD_ORGANIZATION_NAME, mdOrganizationName);
    }

    public String getMdOrganizationDisplayName() {
        return getConfig().get(MD_ORGANIZATION_DISPLAY_NAME);
    }

    public void setMdOrganizationDisplayName(String mdOrganizationDisplayName) {
        getConfig().put(MD_ORGANIZATION_DISPLAY_NAME, mdOrganizationDisplayName);
    }

    public String getMdOrganizationURL() {
        return getConfig().get(MD_ORGANIZATION_URL);
    }

    public void setMdOrganizationURL(String mdOrganizationURL) {
        getConfig().put(MD_ORGANIZATION_URL, mdOrganizationURL);
    }

    public String getMdContactType() {
        return getConfig().get(MD_CONTACT_TYPE);
    }

    public void setMdContactType(String mdContactType) {
        getConfig().put(MD_CONTACT_TYPE, mdContactType);
    }

    public String getMdContactCompany() {
        return getConfig().get(MD_CONTACT_COMPANY);
    }

    public void setMdContactCompany(String mdContactCompany) {
        getConfig().put(MD_CONTACT_COMPANY, mdContactCompany);
    }

    public String getMdContactGivenName() {
        return getConfig().get(MD_CONTACT_GIVEN_NAME);
    }

    public void setMdContactGivenName(String mdContactGivenName) {
        getConfig().put(MD_CONTACT_GIVEN_NAME, mdContactGivenName);
    }

    public String getMdContactSurname() {
        return getConfig().get(MD_CONTACT_SURNAME);
    }

    public void setMdContactSurname(String mdContactSurname) {
        getConfig().put(MD_CONTACT_SURNAME, mdContactSurname);
    }

    public String getMdContactEmailAddress() {
        return getConfig().get(MD_CONTACT_EMAIL_ADDRESS);
    }

    public void setMdContactEmailAddress(List<String> mdContactEmailAddress) {
        try {
            getConfig().put(MD_CONTACT_EMAIL_ADDRESS, JsonSerialization.writeValueAsString(mdContactEmailAddress));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String getMdContactTelephoneNumber() {
        return getConfig().get(MD_CONTACT_TELEPHONE_NUMBER);
    }

    public void setMdContactTelephoneNumber(List<String> mdContactTelephoneNumber) {
        try {
            getConfig().put(MD_CONTACT_TELEPHONE_NUMBER, JsonSerialization.writeValueAsString(mdContactTelephoneNumber));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void validate(RealmModel realm) {
        SslRequired sslRequired = realm.getSslRequired();

        checkUrl(sslRequired, getSingleLogoutServiceUrl(), SINGLE_LOGOUT_SERVICE_URL);
        checkUrl(sslRequired, getSingleSignOnServiceUrl(), SINGLE_SIGN_ON_SERVICE_URL);
        checkUrl(SslRequired.NONE, getΜdrpiRegistrationAuthority(), MDRPI_REGISTRATION_AUTHORITY);
        checkUrl(SslRequired.NONE, getΜdrpiRegistrationPolicy(), MDRPI_REGISTRATION_POLICY);
        checkUrl(SslRequired.NONE, getMduiInformationURL(), MDUI_INFORMATION_URL);
        checkUrl(SslRequired.NONE, getMduiPrivacyStatementURL(), MDUI_PRIVACY_STATEMENT_URL);
        checkUrl(SslRequired.NONE, getMdOrganizationURL(), MD_ORGANIZATION_URL);

    }
}
