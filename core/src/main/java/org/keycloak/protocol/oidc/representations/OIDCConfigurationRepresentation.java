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

package org.keycloak.protocol.oidc.representations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */

public class OIDCConfigurationRepresentation {

    @JsonProperty("issuer")
    private String issuer;

    @JsonProperty("authorization_endpoint")
    private String authorizationEndpoint;

    @JsonProperty("token_endpoint")
    private String tokenEndpoint;

    @JsonProperty("introspection_endpoint")
    private String introspectionEndpoint;

    @JsonProperty("userinfo_endpoint")
    private String userinfoEndpoint;

    @JsonProperty("end_session_endpoint")
    private String logoutEndpoint;

    @JsonProperty("frontchannel_logout_session_supported")
    private Boolean frontChannelLogoutSessionSupported = true;

    @JsonProperty("frontchannel_logout_supported")
    private Boolean frontChannelLogoutSupported = true;

    @JsonProperty("jwks_uri")
    private String jwksUri;

    @JsonProperty("check_session_iframe")
    private String checkSessionIframe;

    @JsonProperty("grant_types_supported")
    private List<String> grantTypesSupported;

    @JsonProperty("acr_values_supported")
    private List<String> acrValuesSupported;

    @JsonProperty("response_types_supported")
    private List<String> responseTypesSupported;

    @JsonProperty("subject_types_supported")
    private List<String> subjectTypesSupported;

    @JsonProperty("prompt_values_supported")
    private List<String> promptValuesSupported;

    @JsonProperty("id_token_signing_alg_values_supported")
    private List<String> idTokenSigningAlgValuesSupported;

    @JsonProperty("id_token_encryption_alg_values_supported")
    private List<String> idTokenEncryptionAlgValuesSupported;

    @JsonProperty("id_token_encryption_enc_values_supported")
    private List<String> idTokenEncryptionEncValuesSupported;

    @JsonProperty("userinfo_signing_alg_values_supported")
    private List<String> userInfoSigningAlgValuesSupported;

    @JsonProperty("userinfo_encryption_alg_values_supported")
    private List<String> userInfoEncryptionAlgValuesSupported;

    @JsonProperty("userinfo_encryption_enc_values_supported")
    private List<String> userInfoEncryptionEncValuesSupported;

    @JsonProperty("request_object_signing_alg_values_supported")
    private List<String> requestObjectSigningAlgValuesSupported;

    @JsonProperty("request_object_encryption_alg_values_supported")
    private List<String> requestObjectEncryptionAlgValuesSupported;

    @JsonProperty("request_object_encryption_enc_values_supported")
    private List<String> requestObjectEncryptionEncValuesSupported;

    @JsonProperty("response_modes_supported")
    private List<String> responseModesSupported;

    @JsonProperty("registration_endpoint")
    private String registrationEndpoint;

    @JsonProperty("token_endpoint_auth_methods_supported")
    private List<String> tokenEndpointAuthMethodsSupported;

    @JsonProperty("token_endpoint_auth_signing_alg_values_supported")
    private List<String> tokenEndpointAuthSigningAlgValuesSupported;

    @JsonProperty("introspection_endpoint_auth_methods_supported")
    private List<String> introspectionEndpointAuthMethodsSupported;

    @JsonProperty("introspection_endpoint_auth_signing_alg_values_supported")
    private List<String> introspectionEndpointAuthSigningAlgValuesSupported;

    @JsonProperty("authorization_signing_alg_values_supported")
    private List<String> authorizationSigningAlgValuesSupported;

    @JsonProperty("authorization_encryption_alg_values_supported")
    private List<String> authorizationEncryptionAlgValuesSupported;

    @JsonProperty("authorization_encryption_enc_values_supported")
    private List<String> authorizationEncryptionEncValuesSupported;

    @JsonProperty("claims_supported")
    private List<String> claimsSupported;

    @JsonProperty("claim_types_supported")
    private List<String> claimTypesSupported;

    @JsonProperty("claims_parameter_supported")
    private Boolean claimsParameterSupported;

    @JsonProperty("scopes_supported")
    private List<String> scopesSupported;

    @JsonProperty("request_parameter_supported")
    private Boolean requestParameterSupported;

    @JsonProperty("request_uri_parameter_supported")
    private Boolean requestUriParameterSupported;

    @JsonProperty("require_request_uri_registration")
    private Boolean requireRequestUriRegistration;

    // KEYCLOAK-7451 OAuth Authorization Server Metadata for Proof Key for Code Exchange
    @JsonProperty("code_challenge_methods_supported")
    private List<String> codeChallengeMethodsSupported;

    // KEYCLOAK-6771 Certificate Bound Token
    // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-6.2
    @JsonProperty("tls_client_certificate_bound_access_tokens")
    private Boolean tlsClientCertificateBoundAccessTokens;

    @JsonProperty("dpop_signing_alg_values_supported")
    private List<String> dpopSigningAlgValuesSupported;

    @JsonProperty("revocation_endpoint")
    private String revocationEndpoint;

    @JsonProperty("revocation_endpoint_auth_methods_supported")
    private List<String> revocationEndpointAuthMethodsSupported;

    @JsonProperty("revocation_endpoint_auth_signing_alg_values_supported")
    private List<String> revocationEndpointAuthSigningAlgValuesSupported;

    @JsonProperty("backchannel_logout_supported")
    private Boolean backchannelLogoutSupported;

    @JsonProperty("backchannel_logout_session_supported")
    private Boolean backchannelLogoutSessionSupported;

    @JsonProperty("device_authorization_endpoint")
    private String deviceAuthorizationEndpoint;

    @JsonProperty("backchannel_token_delivery_modes_supported")
    private List<String> backchannelTokenDeliveryModesSupported;

    @JsonProperty("backchannel_authentication_endpoint")
    private String backchannelAuthenticationEndpoint;

    @JsonProperty("backchannel_authentication_request_signing_alg_values_supported")
    private List<String> backchannelAuthenticationRequestSigningAlgValuesSupported;

    @JsonProperty("require_pushed_authorization_requests")
    private Boolean requirePushedAuthorizationRequests;

    @JsonProperty("pushed_authorization_request_endpoint")
    private String pushedAuthorizationRequestEndpoint;

    @JsonProperty("mtls_endpoint_aliases")
    private MTLSEndpointAliases mtlsEndpointAliases;

    @JsonProperty("authorization_response_iss_parameter_supported")
    private Boolean authorizationResponseIssParameterSupported;

    @JsonProperty("authorization_details_types_supported")
    private List<String> authorizationDetailsTypesSupported;

    protected Map<String, Object> otherClaims = new HashMap<String, Object>();

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getIntrospectionEndpoint() {
        return this.introspectionEndpoint;
    }

    public void setIntrospectionEndpoint(String introspectionEndpoint) {
        this.introspectionEndpoint = introspectionEndpoint;
    }

    public String getUserinfoEndpoint() {
        return userinfoEndpoint;
    }

    public void setUserinfoEndpoint(String userinfoEndpoint) {
        this.userinfoEndpoint = userinfoEndpoint;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String getCheckSessionIframe() {
        return checkSessionIframe;
    }

    public void setCheckSessionIframe(String checkSessionIframe) {
        this.checkSessionIframe = checkSessionIframe;
    }

    public String getLogoutEndpoint() {
        return logoutEndpoint;
    }

    public void setLogoutEndpoint(String logoutEndpoint) {
        this.logoutEndpoint = logoutEndpoint;
    }

    public List<String> getGrantTypesSupported() {
        return grantTypesSupported;
    }

    public void setGrantTypesSupported(List<String> grantTypesSupported) {
        this.grantTypesSupported = grantTypesSupported;
    }

    public List<String> getAcrValuesSupported() {
        return acrValuesSupported;
    }

    public void setAcrValuesSupported(List<String> acrValuesSupported) {
        this.acrValuesSupported = acrValuesSupported;
    }

    public List<String> getResponseTypesSupported() {
        return responseTypesSupported;
    }

    public void setResponseTypesSupported(List<String> responseTypesSupported) {
        this.responseTypesSupported = responseTypesSupported;
    }

    public List<String> getSubjectTypesSupported() {
        return subjectTypesSupported;
    }

    public void setSubjectTypesSupported(List<String> subjectTypesSupported) {
        this.subjectTypesSupported = subjectTypesSupported;
    }

    public List<String> getIdTokenSigningAlgValuesSupported() {
        return idTokenSigningAlgValuesSupported;
    }

    public void setIdTokenSigningAlgValuesSupported(List<String> idTokenSigningAlgValuesSupported) {
        this.idTokenSigningAlgValuesSupported = idTokenSigningAlgValuesSupported;
    }

    public List<String> getIdTokenEncryptionAlgValuesSupported() {
        return idTokenEncryptionAlgValuesSupported;
    }

    public void setIdTokenEncryptionAlgValuesSupported(List<String> idTokenEncryptionAlgValuesSupported) {
        this.idTokenEncryptionAlgValuesSupported = idTokenEncryptionAlgValuesSupported;
    }

    public List<String> getIdTokenEncryptionEncValuesSupported() {
        return idTokenEncryptionEncValuesSupported;
    }

    public void setIdTokenEncryptionEncValuesSupported(List<String> idTokenEncryptionEncValuesSupported) {
        this.idTokenEncryptionEncValuesSupported = idTokenEncryptionEncValuesSupported;
    }

    public List<String> getUserInfoSigningAlgValuesSupported() {
        return userInfoSigningAlgValuesSupported;
    }

    public void setUserInfoSigningAlgValuesSupported(List<String> userInfoSigningAlgValuesSupported) {
        this.userInfoSigningAlgValuesSupported = userInfoSigningAlgValuesSupported;
    }

    public List<String> getUserInfoEncryptionAlgValuesSupported() {
        return userInfoEncryptionAlgValuesSupported;
    }

    public void setUserInfoEncryptionAlgValuesSupported(List<String> userInfoEncryptionAlgValuesSupported) {
        this.userInfoEncryptionAlgValuesSupported = userInfoEncryptionAlgValuesSupported;
    }

    public List<String> getUserInfoEncryptionEncValuesSupported() {
        return userInfoEncryptionEncValuesSupported;
    }

    public void setUserInfoEncryptionEncValuesSupported(List<String> userInfoEncryptionEncValuesSupported) {
        this.userInfoEncryptionEncValuesSupported = userInfoEncryptionEncValuesSupported;
    }

    public List<String> getRequestObjectSigningAlgValuesSupported() {
        return requestObjectSigningAlgValuesSupported;
    }

    public void setRequestObjectSigningAlgValuesSupported(List<String> requestObjectSigningAlgValuesSupported) {
        this.requestObjectSigningAlgValuesSupported = requestObjectSigningAlgValuesSupported;
    }

    public List<String> getRequestObjectEncryptionAlgValuesSupported() {
        return requestObjectEncryptionAlgValuesSupported;
    }

    public void setRequestObjectEncryptionAlgValuesSupported(List<String> requestObjectEncryptionAlgValuesSupported) {
        this.requestObjectEncryptionAlgValuesSupported = requestObjectEncryptionAlgValuesSupported;
    }

    public List<String> getRequestObjectEncryptionEncValuesSupported() {
        return requestObjectEncryptionEncValuesSupported;
    }

    public void setRequestObjectEncryptionEncValuesSupported(List<String> requestObjectEncryptionEncValuesSupported) {
        this.requestObjectEncryptionEncValuesSupported = requestObjectEncryptionEncValuesSupported;
    }

    public List<String> getResponseModesSupported() {
        return responseModesSupported;
    }

    public void setResponseModesSupported(List<String> responseModesSupported) {
        this.responseModesSupported = responseModesSupported;
    }

    public String getRegistrationEndpoint() {
        return registrationEndpoint;
    }

    public void setRegistrationEndpoint(String registrationEndpoint) {
        this.registrationEndpoint = registrationEndpoint;
    }

    public List<String> getTokenEndpointAuthMethodsSupported() {
        return tokenEndpointAuthMethodsSupported;
    }

    public void setTokenEndpointAuthMethodsSupported(List<String> tokenEndpointAuthMethodsSupported) {
        this.tokenEndpointAuthMethodsSupported = tokenEndpointAuthMethodsSupported;
    }

    public List<String> getTokenEndpointAuthSigningAlgValuesSupported() {
        return tokenEndpointAuthSigningAlgValuesSupported;
    }

    public void setTokenEndpointAuthSigningAlgValuesSupported(List<String> tokenEndpointAuthSigningAlgValuesSupported) {
        this.tokenEndpointAuthSigningAlgValuesSupported = tokenEndpointAuthSigningAlgValuesSupported;
    }

    public List<String> getIntrospectionEndpointAuthMethodsSupported() {
        return introspectionEndpointAuthMethodsSupported;
    }

    public void setIntrospectionEndpointAuthMethodsSupported(List<String> introspectionEndpointAuthMethodsSupported) {
        this.introspectionEndpointAuthMethodsSupported = introspectionEndpointAuthMethodsSupported;
    }

    public List<String> getIntrospectionEndpointAuthSigningAlgValuesSupported() {
        return introspectionEndpointAuthSigningAlgValuesSupported;
    }

    public void setIntrospectionEndpointAuthSigningAlgValuesSupported(
        List<String> introspectionEndpointAuthSigningAlgValuesSupported) {
        this.introspectionEndpointAuthSigningAlgValuesSupported = introspectionEndpointAuthSigningAlgValuesSupported;
    }

    public List<String> getClaimsSupported() {
        return claimsSupported;
    }

    public void setClaimsSupported(List<String> claimsSupported) {
        this.claimsSupported = claimsSupported;
    }

    public List<String> getClaimTypesSupported() {
        return claimTypesSupported;
    }

    public void setClaimTypesSupported(List<String> claimTypesSupported) {
        this.claimTypesSupported = claimTypesSupported;
    }

    public Boolean getClaimsParameterSupported() {
        return claimsParameterSupported;
    }

    public void setClaimsParameterSupported(Boolean claimsParameterSupported) {
        this.claimsParameterSupported = claimsParameterSupported;
    }

    public List<String> getScopesSupported() {
        return scopesSupported;
    }

    public void setScopesSupported(List<String> scopesSupported) {
        this.scopesSupported = scopesSupported;
    }

    public Boolean getRequestParameterSupported() {
        return requestParameterSupported;
    }

    public void setRequestParameterSupported(Boolean requestParameterSupported) {
        this.requestParameterSupported = requestParameterSupported;
    }

    public Boolean getRequestUriParameterSupported() {
        return requestUriParameterSupported;
    }

    public void setRequestUriParameterSupported(Boolean requestUriParameterSupported) {
        this.requestUriParameterSupported = requestUriParameterSupported;
    }

    public Boolean getRequireRequestUriRegistration() {
        return requireRequestUriRegistration;
    }

    public void setRequireRequestUriRegistration(Boolean requireRequestUriRegistration) {
        this.requireRequestUriRegistration = requireRequestUriRegistration;
    }

    // KEYCLOAK-7451 OAuth Authorization Server Metadata for Proof Key for Code Exchange
    public List<String> getCodeChallengeMethodsSupported() {
        return codeChallengeMethodsSupported;
    }

    public void setCodeChallengeMethodsSupported(List<String> codeChallengeMethodsSupported) {
        this.codeChallengeMethodsSupported = codeChallengeMethodsSupported;
    }

    // KEYCLOAK-6771 Certificate Bound Token
    // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-6.2
    public Boolean getTlsClientCertificateBoundAccessTokens() {
        return tlsClientCertificateBoundAccessTokens;
    }

    public void setTlsClientCertificateBoundAccessTokens(Boolean tlsClientCertificateBoundAccessTokens) {
        this.tlsClientCertificateBoundAccessTokens = tlsClientCertificateBoundAccessTokens;
    }

    public List<String> getDpopSigningAlgValuesSupported() {
        return dpopSigningAlgValuesSupported;
    }

    public void setDpopSigningAlgValuesSupported(List<String> dpopSigningAlgValuesSupported) {
        this.dpopSigningAlgValuesSupported = dpopSigningAlgValuesSupported;
    }

    public String getRevocationEndpoint() {
        return revocationEndpoint;
    }

    public void setRevocationEndpoint(String revocationEndpoint) {
        this.revocationEndpoint = revocationEndpoint;
    }

    public List<String> getRevocationEndpointAuthMethodsSupported() {
        return revocationEndpointAuthMethodsSupported;
    }

    public void setRevocationEndpointAuthMethodsSupported(List<String> revocationEndpointAuthMethodsSupported) {
        this.revocationEndpointAuthMethodsSupported = revocationEndpointAuthMethodsSupported;
    }

    public List<String> getRevocationEndpointAuthSigningAlgValuesSupported() {
        return revocationEndpointAuthSigningAlgValuesSupported;
    }

    public void setRevocationEndpointAuthSigningAlgValuesSupported(List<String> revocationEndpointAuthSigningAlgValuesSupported) {
        this.revocationEndpointAuthSigningAlgValuesSupported = revocationEndpointAuthSigningAlgValuesSupported;
    }

    public Boolean getBackchannelLogoutSupported() {
        return backchannelLogoutSupported;
    }

    public Boolean getBackchannelLogoutSessionSupported() {
        return backchannelLogoutSessionSupported;
    }

    public void setBackchannelLogoutSessionSupported(Boolean backchannelLogoutSessionSupported) {
        this.backchannelLogoutSessionSupported = backchannelLogoutSessionSupported;
    }

    public void setBackchannelLogoutSupported(Boolean backchannelLogoutSupported) {
        this.backchannelLogoutSupported = backchannelLogoutSupported;
    }

    public List<String> getBackchannelTokenDeliveryModesSupported() {
        return backchannelTokenDeliveryModesSupported;
    }

    public void setBackchannelTokenDeliveryModesSupported(List<String> backchannelTokenDeliveryModesSupported) {
        this.backchannelTokenDeliveryModesSupported = backchannelTokenDeliveryModesSupported;
    }

    public String getBackchannelAuthenticationEndpoint() {
        return backchannelAuthenticationEndpoint;
    }

    public void setBackchannelAuthenticationEndpoint(String backchannelAuthenticationEndpoint) {
        this.backchannelAuthenticationEndpoint = backchannelAuthenticationEndpoint;
    }

    public List<String> getBackchannelAuthenticationRequestSigningAlgValuesSupported() {
        return backchannelAuthenticationRequestSigningAlgValuesSupported;
    }

    public void setBackchannelAuthenticationRequestSigningAlgValuesSupported(List<String> backchannelAuthenticationRequestSigningAlgValuesSupported) {
        this.backchannelAuthenticationRequestSigningAlgValuesSupported = backchannelAuthenticationRequestSigningAlgValuesSupported;
    }

    public String getPushedAuthorizationRequestEndpoint() {
        return pushedAuthorizationRequestEndpoint;
    }

    public void setPushedAuthorizationRequestEndpoint(String pushedAuthorizationRequestEndpoint) {
        this.pushedAuthorizationRequestEndpoint = pushedAuthorizationRequestEndpoint;
    }

    public Boolean getRequirePushedAuthorizationRequests() {
        return requirePushedAuthorizationRequests;
    }

    public void setRequirePushedAuthorizationRequests(Boolean requirePushedAuthorizationRequests) {
        this.requirePushedAuthorizationRequests = requirePushedAuthorizationRequests;
    }

    public MTLSEndpointAliases getMtlsEndpointAliases() {
        return mtlsEndpointAliases;
    }

    public void setMtlsEndpointAliases(MTLSEndpointAliases mtlsEndpointAliases) {
        this.mtlsEndpointAliases = mtlsEndpointAliases;
    }

    @JsonAnyGetter
    public Map<String, Object> getOtherClaims() {
        return otherClaims;
    }

    @JsonAnySetter
    public void setOtherClaims(String name, Object value) {
        otherClaims.put(name, value);
    }

    public void setDeviceAuthorizationEndpoint(String deviceAuthorizationEndpoint) {
        this.deviceAuthorizationEndpoint = deviceAuthorizationEndpoint;
    }

    public String getDeviceAuthorizationEndpoint() {
        return deviceAuthorizationEndpoint;
    }

    public List<String> getAuthorizationSigningAlgValuesSupported() {
        return authorizationSigningAlgValuesSupported;
    }

    public void setAuthorizationSigningAlgValuesSupported(List<String> authorizationSigningAlgValuesSupported) {
        this.authorizationSigningAlgValuesSupported = authorizationSigningAlgValuesSupported;
    }

    public List<String> getAuthorizationEncryptionAlgValuesSupported() {
        return authorizationEncryptionAlgValuesSupported;
    }

    public void setAuthorizationEncryptionAlgValuesSupported(List<String> authorizationEncryptionAlgValuesSupported) {
        this.authorizationEncryptionAlgValuesSupported = authorizationEncryptionAlgValuesSupported;
    }

    public List<String> getAuthorizationEncryptionEncValuesSupported() {
        return authorizationEncryptionEncValuesSupported;
    }

    public void setAuthorizationEncryptionEncValuesSupported(List<String> authorizationEncryptionEncValuesSupported) {
        this.authorizationEncryptionEncValuesSupported = authorizationEncryptionEncValuesSupported;
    }

    public Boolean getFrontChannelLogoutSessionSupported() {
        return frontChannelLogoutSessionSupported;
    }

    public void setFrontChannelLogoutSessionSupported(Boolean frontChannelLogoutSessionSupported) {
        this.frontChannelLogoutSessionSupported = frontChannelLogoutSessionSupported;
    }

    public Boolean getFrontChannelLogoutSupported() {
        return frontChannelLogoutSupported;
    }

    public void setFrontChannelLogoutSupported(Boolean frontChannelLogoutSupported) {
        this.frontChannelLogoutSupported = frontChannelLogoutSupported;
    }

    public Boolean getAuthorizationResponseIssParameterSupported() {
        return authorizationResponseIssParameterSupported;
    }

    public void setAuthorizationResponseIssParameterSupported(Boolean authorizationResponseIssParameterSupported) {
        this.authorizationResponseIssParameterSupported = authorizationResponseIssParameterSupported;
    }

    public List<String> getPromptValuesSupported() {
        return promptValuesSupported;
    }

    public void setPromptValuesSupported(List<String> promptValuesSupported) {
        this.promptValuesSupported = promptValuesSupported;
    }

    public List<String> getAuthorizationDetailsTypesSupported() {
        return authorizationDetailsTypesSupported;
    }

    public void setAuthorizationDetailsTypesSupported(List<String> authorizationDetailsTypesSupported) {
        this.authorizationDetailsTypesSupported = authorizationDetailsTypesSupported;
    }
}
