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

import org.keycloak.common.enums.SslRequired;
import org.keycloak.dom.saml.v2.protocol.AuthnContextComparisonType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.saml.SamlPrincipalType;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.XmlKeyInfoKeyNameTransformer;
import org.keycloak.utils.StringUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Pedro Igor
 */
public class SAMLIdentityProviderConfig extends IdentityProviderModel {

    public static final XmlKeyInfoKeyNameTransformer DEFAULT_XML_KEY_INFO_KEY_NAME_TRANSFORMER = XmlKeyInfoKeyNameTransformer.NONE;

    public static final String ENTITY_ID = "entityId";
    public static final String IDP_ENTITY_ID = "idpEntityId";
    public static final String ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO = "addExtensionsElementWithKeyInfo";
    public static final String BACKCHANNEL_SUPPORTED = "backchannelSupported";
    public static final String ENCRYPTION_PUBLIC_KEY = "encryptionPublicKey";
    public static final String FORCE_AUTHN = "forceAuthn";
    public static final String NAME_ID_POLICY_FORMAT = "nameIDPolicyFormat";
    public static final String POST_BINDING_AUTHN_REQUEST = "postBindingAuthnRequest";
    public static final String POST_BINDING_LOGOUT = "postBindingLogout";
    public static final String POST_BINDING_RESPONSE = "postBindingResponse";
    public static final String ARTIFACT_BINDING_RESPONSE = "artifactBindingResponse";
    public static final String SIGNATURE_ALGORITHM = "signatureAlgorithm";
    public static final String ENCRYPTION_ALGORITHM = "encryptionAlgorithm";
    public static final String SIGNING_CERTIFICATE_KEY = "signingCertificate";
    public static final String SINGLE_LOGOUT_SERVICE_URL = "singleLogoutServiceUrl";
    public static final String SINGLE_SIGN_ON_SERVICE_URL = "singleSignOnServiceUrl";
    public static final String ARTIFACT_RESOLUTION_SERVICE_URL = "artifactResolutionServiceUrl";
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
    public static final String ALLOW_CREATE = "allowCreate";
    public static final String ATTRIBUTE_CONSUMING_SERVICE_INDEX = "attributeConsumingServiceIndex";
    public static final String ATTRIBUTE_CONSUMING_SERVICE_NAME = "attributeConsumingServiceName";
    public static final String ATTRIBUTE_CONSUMING_SERVICE_METADATA = "attributeConsumingServiceMetadata";
    public static final String USE_METADATA_DESCRIPTOR_URL = "useMetadataDescriptorUrl";
    public static final String ASSERTION_CONSUMING_SERVICE_INDEX = "assertionConsumingServiceIndex";
    public static final String ARTIFACT_RESOLUTION = "artifactResolution";
    public static final String ARTIFACT_RESOLUTION_ENDPOINT = "artifactResolutionEndpoint";
    public static final String SIGN_ARTIFACT_RESOLUTION_REQUEST = "signArtifactResolutionRequest";
    public static final String ARTIFACT_RESOLUTION_SOAP = "artifactResolutionSOAP";
    public static final String ARTIFACT_RESOLUTION_WITH_XML_HEADER = "artifactResolutionWithXmlHeader";
    public static final String ARTIFACT_RESOLUTION_SERVICE_METADATA = "includeArtifactResolutionServiceMetadata";
    public static final String CHAR_SET = "charSet";
    public static final String METADATA_VALID_UNTIL_UNIT = "metadataValidUntilUnit";
    public static final String METADATA_VALID_UNTIL_PERIOD = "metadataValidUntilPeriod";
    public static final String ARTIFACT_RESOLUTION_MUTUAL_TLS = "mutualTls";
    public static final String IGNORE_SAML_ADVICE_NODES = "ignoreSamlAdviceNodes";
    public static final String AUTHN_REQUEST_SCOPING = "scoping";
    public static final String LINKED_PROVIDERS = "linkedProviders";
    public static final String SERVICE_NAME = "serviceName";
    public static final String FRIENDLY_NAME = "friendlyName";
    public static final String ATTRIBUTE_NAME = "attributeName";
    public static final String ATTRIBUTE_VALUE = "attributeValue";

    public SAMLIdentityProviderConfig() {
        super();
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

    public String getIdpEntityId() {
        return getConfig().get(IDP_ENTITY_ID);
    }

    public void setIdpEntityId(String idpEntityId) {
        getConfig().put(IDP_ENTITY_ID, idpEntityId);
    }

    public String getSingleSignOnServiceUrl() {
        return getConfig().get(SINGLE_SIGN_ON_SERVICE_URL);
    }

    public void setSingleSignOnServiceUrl(String singleSignOnServiceUrl) {
        getConfig().put(SINGLE_SIGN_ON_SERVICE_URL, singleSignOnServiceUrl);
    }

    public String getArtifactResolutionServiceUrl() {
        return getConfig().get(ARTIFACT_RESOLUTION_SERVICE_URL);
    }

    public void setArtifactResolutionServiceUrl(String artifactResolutionServiceUrl) {
        getConfig().put(ARTIFACT_RESOLUTION_SERVICE_URL, artifactResolutionServiceUrl);
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

    public Integer getAssertionConsumingServiceIndex() {
        return getConfig().get(ASSERTION_CONSUMING_SERVICE_INDEX) != null ? Integer.parseInt(getConfig().get(ASSERTION_CONSUMING_SERVICE_INDEX)) : null;
    }

    public void setAssertionConsumingServiceIndex(int assertionConsumingServiceIndex) {
        getConfig().put(ASSERTION_CONSUMING_SERVICE_INDEX, String.valueOf(assertionConsumingServiceIndex));
    }


    public void setAttributeConsumingServiceMetadata(String attributeConsumingServiceName) {
        getConfig().put(ATTRIBUTE_CONSUMING_SERVICE_METADATA, attributeConsumingServiceName);
    }

    public String getAttributeConsumingService() {
        return getConfig().get(ATTRIBUTE_CONSUMING_SERVICE_METADATA);
    }


    public boolean isArtifactResolutionWithXmlHeader() {
        return Boolean.parseBoolean(getConfig().get(ARTIFACT_RESOLUTION_WITH_XML_HEADER));
    }

    public void setArtifactResolutionWithXmlHeader(boolean artifactResolutionWithXmlHeader) {
        getConfig().put(ARTIFACT_RESOLUTION_WITH_XML_HEADER, String.valueOf(artifactResolutionWithXmlHeader));
    }

    public boolean isArtifactResolution() {
        return Boolean.parseBoolean(getConfig().get(ARTIFACT_RESOLUTION));
    }

    public void setArtifactResolution(boolean artifactResolution) {
        getConfig().put(ARTIFACT_RESOLUTION, String.valueOf(artifactResolution));
    }

    public boolean isIncludeArtifactResolutionServiceMetadata() {
        return Boolean.parseBoolean(getConfig().get(ARTIFACT_RESOLUTION_SERVICE_METADATA));
    }

    public void setIncludeArtifactResolutionServiceMetadata(boolean includeArtifactResolutionServiceMetadata) {
        getConfig().put(ARTIFACT_RESOLUTION_SERVICE_METADATA, String.valueOf(includeArtifactResolutionServiceMetadata));
    }

    public boolean isArtifactResolutionSOAP() {
        return Boolean.parseBoolean(getConfig().get(ARTIFACT_RESOLUTION_SOAP));
    }

    public void setArtifactResolutionSOAP(boolean artifactResolutionSOAP) {
        getConfig().put(ARTIFACT_RESOLUTION_SOAP, String.valueOf(artifactResolutionSOAP));
    }

    public String getArtifactResolutionEndpoint() {
        return getConfig().get(ARTIFACT_RESOLUTION_ENDPOINT);
    }

    public void setArtifactResolutionEndpoint(String artifactResolutionEndpoint) {
        getConfig().put(ARTIFACT_RESOLUTION_ENDPOINT, artifactResolutionEndpoint);
    }

    public boolean isSignArtifactResolutionRequest() {
        return Boolean.valueOf(getConfig().get(SIGN_ARTIFACT_RESOLUTION_REQUEST));
    }

    public void setSignArtifactResolutionRequest(boolean signArtifactResolutionRequest) {
        getConfig().put(SIGN_ARTIFACT_RESOLUTION_REQUEST, String.valueOf(signArtifactResolutionRequest));
    }

    public Charset getCharSet() {
        return getConfig().get(CHAR_SET) != null ? Charset.forName(getConfig().get(CHAR_SET)) : StandardCharsets.UTF_8;
    }

    public void setCharSet(String charset) {
        getConfig().put(CHAR_SET, charset);
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

    public String getEncryptionAlgorithm() {
        return getConfig().get(ENCRYPTION_ALGORITHM);
    }

    public void setEncryptionAlgorithm(String encryptionAlgorithm) {
        getConfig().put(ENCRYPTION_ALGORITHM, encryptionAlgorithm);
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

    public boolean isArtifactBindingResponse() {
        return Boolean.valueOf(getConfig().get(ARTIFACT_BINDING_RESPONSE));
    }

    public void setArtifactBindingResponse(boolean backchannel) {
        getConfig().put(ARTIFACT_BINDING_RESPONSE, String.valueOf(backchannel));
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

    public Integer getMetadataValidUntilUnit() {
        return getConfig().get(METADATA_VALID_UNTIL_UNIT) != null ? Integer.parseInt(getConfig().get(METADATA_VALID_UNTIL_UNIT)) : null;
    }

    public void setMetadataValidUntilUnit(Integer unit) {
        getConfig().put(METADATA_VALID_UNTIL_UNIT, String.valueOf(unit));
    }

    public Integer getMetadataValidUntilPeriod() {
        return getConfig().get(METADATA_VALID_UNTIL_PERIOD) != null ? Integer.parseInt(getConfig().get(METADATA_VALID_UNTIL_PERIOD)) : null;
    }

    public void setMetadataValidUntilPeriod(Integer period) {
        getConfig().put(METADATA_VALID_UNTIL_UNIT, String.valueOf(period));
    }

    public boolean isMutualTLS() {
        return Boolean.parseBoolean(getConfig().get(ARTIFACT_RESOLUTION_MUTUAL_TLS));
    }

    public void setMutualTls(boolean mutualTls) {
        getConfig().put(ARTIFACT_RESOLUTION_MUTUAL_TLS, String.valueOf(mutualTls));
    }

    public void setIgnoreSamlAdviceNodes(boolean ignoreSamlAdviceNodes) {
        getConfig().put(IGNORE_SAML_ADVICE_NODES, String.valueOf(ignoreSamlAdviceNodes));
    }

    public boolean isIgnoreSamlAdviceNodes() {
        return Boolean.parseBoolean(getConfig().get(IGNORE_SAML_ADVICE_NODES));
    }


    public String getScoping() {
        return this.getConfig().get(AUTHN_REQUEST_SCOPING);
    }

    public void setScoping(String authnContextClassRefs) {
        this.getConfig().put(AUTHN_REQUEST_SCOPING, authnContextClassRefs);
    }

    public List<String> getLinkedProviders() {
        String linkedProviders = this.getConfig().get(LINKED_PROVIDERS);
        if (linkedProviders == null || linkedProviders.isEmpty())
            return new LinkedList<>();

        try {
            return List.of(linkedProviders);
        } catch (Exception e) {
            return new LinkedList<>();
        }
    }

    public void setLinkedProviders(String linkedProviders) {
        this.getConfig().put(LINKED_PROVIDERS, linkedProviders);
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

    public boolean isAllowCreate() {
        return Boolean.valueOf(getConfig().get(ALLOW_CREATE));
    }

    public void setAllowCreated(boolean allowCreate) {
        getConfig().put(ALLOW_CREATE, String.valueOf(allowCreate));
    }

    public Integer getAttributeConsumingServiceIndex() {
        Integer result = null;
        String strAttributeConsumingServiceIndex = getConfig().get(ATTRIBUTE_CONSUMING_SERVICE_INDEX);
        if (strAttributeConsumingServiceIndex != null && !strAttributeConsumingServiceIndex.isEmpty()) {
            try {
                result = Integer.parseInt(strAttributeConsumingServiceIndex);
                if (result < 0) {
                    result = null;
                }
            } catch (NumberFormatException e) {
                // ignore it and use null
            }
        }
        return result;
    }

    public void setAttributeConsumingServiceIndex(Integer attributeConsumingServiceIndex) {
        if (attributeConsumingServiceIndex == null || attributeConsumingServiceIndex < 0) {
            getConfig().remove(ATTRIBUTE_CONSUMING_SERVICE_INDEX);
        } else {
            getConfig().put(ATTRIBUTE_CONSUMING_SERVICE_INDEX, String.valueOf(attributeConsumingServiceIndex));
        }
    }

    public void setAttributeConsumingServiceName(String attributeConsumingServiceName) {
        getConfig().put(ATTRIBUTE_CONSUMING_SERVICE_NAME, attributeConsumingServiceName);
    }

    public String getAttributeConsumingServiceName() {
        return getConfig().get(ATTRIBUTE_CONSUMING_SERVICE_NAME);
    }

    public void setUseMetadataDescriptorUrl(Boolean useDescriptorUrl) {
        if (useDescriptorUrl == null || !useDescriptorUrl) {
            getConfig().remove(USE_METADATA_DESCRIPTOR_URL);
        } else {
            getConfig().put(USE_METADATA_DESCRIPTOR_URL, Boolean.TRUE.toString());
        }
    }

    public boolean isUseMetadataDescriptorUrl() {
        return Boolean.parseBoolean(getConfig().get(USE_METADATA_DESCRIPTOR_URL));
    }

    @Override
    public void validate(RealmModel realm) {
        SslRequired sslRequired = realm.getSslRequired();

        checkUrl(sslRequired, getSingleLogoutServiceUrl(), SINGLE_LOGOUT_SERVICE_URL);
        checkUrl(sslRequired, getSingleSignOnServiceUrl(), SINGLE_SIGN_ON_SERVICE_URL);
        if (StringUtil.isNotBlank(getMetadataDescriptorUrl())) {
            checkUrl(sslRequired, getMetadataDescriptorUrl(), METADATA_DESCRIPTOR_URL);
        }
        if (isUseMetadataDescriptorUrl()) {
            if (StringUtil.isBlank(getMetadataDescriptorUrl())) {
                throw new IllegalArgumentException(USE_METADATA_DESCRIPTOR_URL + " needs a non-empty URL for " + METADATA_DESCRIPTOR_URL);
            }
        }
        if (StringUtil.isNotBlank(getArtifactResolutionServiceUrl())) {
            checkUrl(sslRequired, getArtifactResolutionServiceUrl(), ARTIFACT_RESOLUTION_SERVICE_URL);
        }
        //transient name id format is not accepted together with principaltype SubjectnameId
        if (JBossSAMLURIConstants.NAMEID_FORMAT_TRANSIENT.get().equals(getNameIDPolicyFormat()) && SamlPrincipalType.SUBJECT == getPrincipalType())
            throw new IllegalArgumentException("Can not have Transient NameID Policy Format together with SUBJECT Principal Type");

    }
}
