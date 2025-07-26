package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.keycloak.protocol.oidc.representations.MTLSEndpointAliases;

public class OPMetadataPolicy {

    @JsonProperty("federation_registration_endpoint")
    private Policy<String> federationRegistrationEndpoint;

    @JsonProperty("client_registration_types")
    private PolicyList<String> clientRegistrationTypes;

    @JsonUnwrapped
    private CommonMetadataPolicy commonMetadataPolicy;

    @JsonProperty("issuer")
    private Policy<String> issuer;

    @JsonProperty("authorization_endpoint")
    private Policy<String> authorizationEndpoint;

    @JsonProperty("token_endpoint")
    private Policy<String> tokenEndpoint;

    @JsonProperty("introspection_endpoint")
    private Policy<String> introspectionEndpoint;

    @JsonProperty("userinfo_endpoint")
    private Policy<String> userinfoEndpoint;

    @JsonProperty("end_session_endpoint")
    private Policy<String> logoutEndpoint;

    @JsonProperty("frontchannel_logout_session_supported")
    private Policy<String> frontChannelLogoutSessionSupported;

    @JsonProperty("frontchannel_logout_supported")
    private Policy<String> frontChannelLogoutSupported;

    @JsonProperty("jwks_uri")
    private Policy<String> jwksUri;

    @JsonProperty("check_session_iframe")
    private Policy<String> checkSessionIframe;

    @JsonProperty("grant_types_supported")
    private PolicyList<String> grantTypesSupported;

    @JsonProperty("acr_values_supported")
    private PolicyList<String> acrValuesSupported;

    @JsonProperty("response_types_supported")
    private PolicyList<String> responseTypesSupported;

    @JsonProperty("subject_types_supported")
    private PolicyList<String> subjectTypesSupported;

    @JsonProperty("id_token_signing_alg_values_supported")
    private PolicyList<String> idTokenSigningAlgValuesSupported;

    @JsonProperty("id_token_encryption_alg_values_supported")
    private PolicyList<String> idTokenEncryptionAlgValuesSupported;

    @JsonProperty("id_token_encryption_enc_values_supported")
    private PolicyList<String> idTokenEncryptionEncValuesSupported;

    @JsonProperty("userinfo_signing_alg_values_supported")
    private PolicyList<String> userInfoSigningAlgValuesSupported;

    @JsonProperty("userinfo_encryption_alg_values_supported")
    private PolicyList<String> userInfoEncryptionAlgValuesSupported;

    @JsonProperty("userinfo_encryption_enc_values_supported")
    private PolicyList<String> userInfoEncryptionEncValuesSupported;

    @JsonProperty("request_object_signing_alg_values_supported")
    private PolicyList<String> requestObjectSigningAlgValuesSupported;

    @JsonProperty("request_object_encryption_alg_values_supported")
    private PolicyList<String> requestObjectEncryptionAlgValuesSupported;

    @JsonProperty("request_object_encryption_enc_values_supported")
    private PolicyList<String> requestObjectEncryptionEncValuesSupported;

    @JsonProperty("response_modes_supported")
    private PolicyList<String> responseModesSupported;

    @JsonProperty("registration_endpoint")
    private Policy<String> registrationEndpoint;

    @JsonProperty("token_endpoint_auth_methods_supported")
    private PolicyList<String> tokenEndpointAuthMethodsSupported;

    @JsonProperty("token_endpoint_auth_signing_alg_values_supported")
    private PolicyList<String> tokenEndpointAuthSigningAlgValuesSupported;

    @JsonProperty("introspection_endpoint_auth_methods_supported")
    private PolicyList<String> introspectionEndpointAuthMethodsSupported;

    @JsonProperty("introspection_endpoint_auth_signing_alg_values_supported")
    private PolicyList<String> introspectionEndpointAuthSigningAlgValuesSupported;

    @JsonProperty("authorization_signing_alg_values_supported")
    private PolicyList<String> authorizationSigningAlgValuesSupported;

    @JsonProperty("authorization_encryption_alg_values_supported")
    private PolicyList<String> authorizationEncryptionAlgValuesSupported;

    @JsonProperty("authorization_encryption_enc_values_supported")
    private PolicyList<String> authorizationEncryptionEncValuesSupported;

    @JsonProperty("claims_supported")
    private PolicyList<String> claimsSupported;

    @JsonProperty("claim_types_supported")
    private PolicyList<String> claimTypesSupported;

    @JsonProperty("claims_parameter_supported")
    private Policy<Boolean> claimsParameterSupported;

    @JsonProperty("scopes_supported")
    private PolicyList<String> scopesSupported;

    @JsonProperty("request_parameter_supported")
    private Policy<Boolean> requestParameterSupported;

    @JsonProperty("request_uri_parameter_supported")
    private Policy<Boolean> requestUriParameterSupported;

    @JsonProperty("code_challenge_methods_supported")
    private PolicyList<String> codeChallengeMethodsSupported;

    @JsonProperty("tls_client_certificate_bound_access_tokens")
    private Policy<Boolean> tlsClientCertificateBoundAccessTokens;

    @JsonProperty("revocation_endpoint")
    private Policy<String> revocationEndpoint;

    @JsonProperty("revocation_endpoint_auth_methods_supported")
    private PolicyList<String> revocationEndpointAuthMethodsSupported;

    @JsonProperty("revocation_endpoint_auth_signing_alg_values_supported")
    private PolicyList<String> revocationEndpointAuthSigningAlgValuesSupported;

    @JsonProperty("backchannel_logout_supported")
    private Policy<Boolean> backchannelLogoutSupported;

    @JsonProperty("backchannel_logout_session_supported")
    private Policy<Boolean> backchannelLogoutSessionSupported;

    @JsonProperty("device_authorization_endpoint")
    private Policy<String> deviceAuthorizationEndpoint;

    @JsonProperty("backchannel_token_delivery_modes_supported")
    private PolicyList<String> backchannelTokenDeliveryModesSupported;

    @JsonProperty("backchannel_authentication_endpoint")
    private Policy<String> backchannelAuthenticationEndpoint;

    @JsonProperty("backchannel_authentication_request_signing_alg_values_supported")
    private PolicyList<String> backchannelAuthenticationRequestSigningAlgValuesSupported;

    @JsonProperty("require_pushed_authorization_requests")
    private Policy<Boolean> requirePushedAuthorizationRequests;

    @JsonProperty("pushed_authorization_request_endpoint")
    private Policy<String> pushedAuthorizationRequestEndpoint;

    @JsonProperty("mtls_endpoint_aliases")
    private PolicyList<MTLSEndpointAliases> mtlsEndpointAliases;


    public Policy<String> getFederationRegistrationEndpoint() {
        return federationRegistrationEndpoint;
    }

    public void setFederationRegistrationEndpoint(Policy<String> federationRegistrationEndpoint) {
        this.federationRegistrationEndpoint = federationRegistrationEndpoint;
    }

    public PolicyList<String> getClientRegistrationTypes() {
        return clientRegistrationTypes;
    }

    public void setClientRegistrationTypes(PolicyList<String> clientRegistrationTypes) {
        this.clientRegistrationTypes = clientRegistrationTypes;
    }

    public CommonMetadataPolicy getCommonMetadataPolicy() {
        return commonMetadataPolicy;
    }

    public void setCommonMetadataPolicy(CommonMetadataPolicy commonMetadataPolicy) {
        this.commonMetadataPolicy = commonMetadataPolicy;
    }

    public Policy<String> getIssuer() {
        return issuer;
    }

    public void setIssuer(Policy<String> issuer) {
        this.issuer = issuer;
    }

    public Policy<String> getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(Policy<String> authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public Policy<String> getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(Policy<String> tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public Policy<String> getIntrospectionEndpoint() {
        return introspectionEndpoint;
    }

    public void setIntrospectionEndpoint(Policy<String> introspectionEndpoint) {
        this.introspectionEndpoint = introspectionEndpoint;
    }

    public Policy<String> getUserinfoEndpoint() {
        return userinfoEndpoint;
    }

    public void setUserinfoEndpoint(Policy<String> userinfoEndpoint) {
        this.userinfoEndpoint = userinfoEndpoint;
    }

    public Policy<String> getLogoutEndpoint() {
        return logoutEndpoint;
    }

    public void setLogoutEndpoint(Policy<String> logoutEndpoint) {
        this.logoutEndpoint = logoutEndpoint;
    }

    public Policy<String> getFrontChannelLogoutSessionSupported() {
        return frontChannelLogoutSessionSupported;
    }

    public void setFrontChannelLogoutSessionSupported(Policy<String> frontChannelLogoutSessionSupported) {
        this.frontChannelLogoutSessionSupported = frontChannelLogoutSessionSupported;
    }

    public Policy<String> getFrontChannelLogoutSupported() {
        return frontChannelLogoutSupported;
    }

    public void setFrontChannelLogoutSupported(Policy<String> frontChannelLogoutSupported) {
        this.frontChannelLogoutSupported = frontChannelLogoutSupported;
    }

    public Policy<String> getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(Policy<String> jwksUri) {
        this.jwksUri = jwksUri;
    }

    public Policy<String> getCheckSessionIframe() {
        return checkSessionIframe;
    }

    public void setCheckSessionIframe(Policy<String> checkSessionIframe) {
        this.checkSessionIframe = checkSessionIframe;
    }

    public PolicyList<String> getGrantTypesSupported() {
        return grantTypesSupported;
    }

    public void setGrantTypesSupported(PolicyList<String> grantTypesSupported) {
        this.grantTypesSupported = grantTypesSupported;
    }

    public PolicyList<String> getAcrValuesSupported() {
        return acrValuesSupported;
    }

    public void setAcrValuesSupported(PolicyList<String> acrValuesSupported) {
        this.acrValuesSupported = acrValuesSupported;
    }

    public PolicyList<String> getResponseTypesSupported() {
        return responseTypesSupported;
    }

    public void setResponseTypesSupported(PolicyList<String> responseTypesSupported) {
        this.responseTypesSupported = responseTypesSupported;
    }

    public PolicyList<String> getSubjectTypesSupported() {
        return subjectTypesSupported;
    }

    public void setSubjectTypesSupported(PolicyList<String> subjectTypesSupported) {
        this.subjectTypesSupported = subjectTypesSupported;
    }

    public PolicyList<String> getIdTokenSigningAlgValuesSupported() {
        return idTokenSigningAlgValuesSupported;
    }

    public void setIdTokenSigningAlgValuesSupported(PolicyList<String> idTokenSigningAlgValuesSupported) {
        this.idTokenSigningAlgValuesSupported = idTokenSigningAlgValuesSupported;
    }

    public PolicyList<String> getIdTokenEncryptionAlgValuesSupported() {
        return idTokenEncryptionAlgValuesSupported;
    }

    public void setIdTokenEncryptionAlgValuesSupported(PolicyList<String> idTokenEncryptionAlgValuesSupported) {
        this.idTokenEncryptionAlgValuesSupported = idTokenEncryptionAlgValuesSupported;
    }

    public PolicyList<String> getIdTokenEncryptionEncValuesSupported() {
        return idTokenEncryptionEncValuesSupported;
    }

    public void setIdTokenEncryptionEncValuesSupported(PolicyList<String> idTokenEncryptionEncValuesSupported) {
        this.idTokenEncryptionEncValuesSupported = idTokenEncryptionEncValuesSupported;
    }

    public PolicyList<String> getUserInfoSigningAlgValuesSupported() {
        return userInfoSigningAlgValuesSupported;
    }

    public void setUserInfoSigningAlgValuesSupported(PolicyList<String> userInfoSigningAlgValuesSupported) {
        this.userInfoSigningAlgValuesSupported = userInfoSigningAlgValuesSupported;
    }

    public PolicyList<String> getUserInfoEncryptionAlgValuesSupported() {
        return userInfoEncryptionAlgValuesSupported;
    }

    public void setUserInfoEncryptionAlgValuesSupported(PolicyList<String> userInfoEncryptionAlgValuesSupported) {
        this.userInfoEncryptionAlgValuesSupported = userInfoEncryptionAlgValuesSupported;
    }

    public PolicyList<String> getUserInfoEncryptionEncValuesSupported() {
        return userInfoEncryptionEncValuesSupported;
    }

    public void setUserInfoEncryptionEncValuesSupported(PolicyList<String> userInfoEncryptionEncValuesSupported) {
        this.userInfoEncryptionEncValuesSupported = userInfoEncryptionEncValuesSupported;
    }

    public PolicyList<String> getRequestObjectSigningAlgValuesSupported() {
        return requestObjectSigningAlgValuesSupported;
    }

    public void setRequestObjectSigningAlgValuesSupported(PolicyList<String> requestObjectSigningAlgValuesSupported) {
        this.requestObjectSigningAlgValuesSupported = requestObjectSigningAlgValuesSupported;
    }

    public PolicyList<String> getRequestObjectEncryptionAlgValuesSupported() {
        return requestObjectEncryptionAlgValuesSupported;
    }

    public void setRequestObjectEncryptionAlgValuesSupported(PolicyList<String> requestObjectEncryptionAlgValuesSupported) {
        this.requestObjectEncryptionAlgValuesSupported = requestObjectEncryptionAlgValuesSupported;
    }

    public PolicyList<String> getRequestObjectEncryptionEncValuesSupported() {
        return requestObjectEncryptionEncValuesSupported;
    }

    public void setRequestObjectEncryptionEncValuesSupported(PolicyList<String> requestObjectEncryptionEncValuesSupported) {
        this.requestObjectEncryptionEncValuesSupported = requestObjectEncryptionEncValuesSupported;
    }

    public PolicyList<String> getResponseModesSupported() {
        return responseModesSupported;
    }

    public void setResponseModesSupported(PolicyList<String> responseModesSupported) {
        this.responseModesSupported = responseModesSupported;
    }

    public Policy<String> getRegistrationEndpoint() {
        return registrationEndpoint;
    }

    public void setRegistrationEndpoint(Policy<String> registrationEndpoint) {
        this.registrationEndpoint = registrationEndpoint;
    }

    public PolicyList<String> getTokenEndpointAuthMethodsSupported() {
        return tokenEndpointAuthMethodsSupported;
    }

    public void setTokenEndpointAuthMethodsSupported(PolicyList<String> tokenEndpointAuthMethodsSupported) {
        this.tokenEndpointAuthMethodsSupported = tokenEndpointAuthMethodsSupported;
    }

    public PolicyList<String> getTokenEndpointAuthSigningAlgValuesSupported() {
        return tokenEndpointAuthSigningAlgValuesSupported;
    }

    public void setTokenEndpointAuthSigningAlgValuesSupported(PolicyList<String> tokenEndpointAuthSigningAlgValuesSupported) {
        this.tokenEndpointAuthSigningAlgValuesSupported = tokenEndpointAuthSigningAlgValuesSupported;
    }

    public PolicyList<String> getIntrospectionEndpointAuthMethodsSupported() {
        return introspectionEndpointAuthMethodsSupported;
    }

    public void setIntrospectionEndpointAuthMethodsSupported(PolicyList<String> introspectionEndpointAuthMethodsSupported) {
        this.introspectionEndpointAuthMethodsSupported = introspectionEndpointAuthMethodsSupported;
    }

    public PolicyList<String> getIntrospectionEndpointAuthSigningAlgValuesSupported() {
        return introspectionEndpointAuthSigningAlgValuesSupported;
    }

    public void setIntrospectionEndpointAuthSigningAlgValuesSupported(PolicyList<String> introspectionEndpointAuthSigningAlgValuesSupported) {
        this.introspectionEndpointAuthSigningAlgValuesSupported = introspectionEndpointAuthSigningAlgValuesSupported;
    }

    public PolicyList<String> getAuthorizationSigningAlgValuesSupported() {
        return authorizationSigningAlgValuesSupported;
    }

    public void setAuthorizationSigningAlgValuesSupported(PolicyList<String> authorizationSigningAlgValuesSupported) {
        this.authorizationSigningAlgValuesSupported = authorizationSigningAlgValuesSupported;
    }

    public PolicyList<String> getAuthorizationEncryptionAlgValuesSupported() {
        return authorizationEncryptionAlgValuesSupported;
    }

    public void setAuthorizationEncryptionAlgValuesSupported(PolicyList<String> authorizationEncryptionAlgValuesSupported) {
        this.authorizationEncryptionAlgValuesSupported = authorizationEncryptionAlgValuesSupported;
    }

    public PolicyList<String> getAuthorizationEncryptionEncValuesSupported() {
        return authorizationEncryptionEncValuesSupported;
    }

    public void setAuthorizationEncryptionEncValuesSupported(PolicyList<String> authorizationEncryptionEncValuesSupported) {
        this.authorizationEncryptionEncValuesSupported = authorizationEncryptionEncValuesSupported;
    }

    public PolicyList<String> getClaimsSupported() {
        return claimsSupported;
    }

    public void setClaimsSupported(PolicyList<String> claimsSupported) {
        this.claimsSupported = claimsSupported;
    }

    public PolicyList<String> getClaimTypesSupported() {
        return claimTypesSupported;
    }

    public void setClaimTypesSupported(PolicyList<String> claimTypesSupported) {
        this.claimTypesSupported = claimTypesSupported;
    }

    public Policy<Boolean> getClaimsParameterSupported() {
        return claimsParameterSupported;
    }

    public void setClaimsParameterSupported(Policy<Boolean> claimsParameterSupported) {
        this.claimsParameterSupported = claimsParameterSupported;
    }

    public PolicyList<String> getScopesSupported() {
        return scopesSupported;
    }

    public void setScopesSupported(PolicyList<String> scopesSupported) {
        this.scopesSupported = scopesSupported;
    }

    public Policy<Boolean> getRequestParameterSupported() {
        return requestParameterSupported;
    }

    public void setRequestParameterSupported(Policy<Boolean> requestParameterSupported) {
        this.requestParameterSupported = requestParameterSupported;
    }

    public Policy<Boolean> getRequestUriParameterSupported() {
        return requestUriParameterSupported;
    }

    public void setRequestUriParameterSupported(Policy<Boolean> requestUriParameterSupported) {
        this.requestUriParameterSupported = requestUriParameterSupported;
    }

    public PolicyList<String> getCodeChallengeMethodsSupported() {
        return codeChallengeMethodsSupported;
    }

    public void setCodeChallengeMethodsSupported(PolicyList<String> codeChallengeMethodsSupported) {
        this.codeChallengeMethodsSupported = codeChallengeMethodsSupported;
    }

    public Policy<Boolean> getTlsClientCertificateBoundAccessTokens() {
        return tlsClientCertificateBoundAccessTokens;
    }

    public void setTlsClientCertificateBoundAccessTokens(Policy<Boolean> tlsClientCertificateBoundAccessTokens) {
        this.tlsClientCertificateBoundAccessTokens = tlsClientCertificateBoundAccessTokens;
    }

    public Policy<String> getRevocationEndpoint() {
        return revocationEndpoint;
    }

    public void setRevocationEndpoint(Policy<String> revocationEndpoint) {
        this.revocationEndpoint = revocationEndpoint;
    }

    public PolicyList<String> getRevocationEndpointAuthMethodsSupported() {
        return revocationEndpointAuthMethodsSupported;
    }

    public void setRevocationEndpointAuthMethodsSupported(PolicyList<String> revocationEndpointAuthMethodsSupported) {
        this.revocationEndpointAuthMethodsSupported = revocationEndpointAuthMethodsSupported;
    }

    public PolicyList<String> getRevocationEndpointAuthSigningAlgValuesSupported() {
        return revocationEndpointAuthSigningAlgValuesSupported;
    }

    public void setRevocationEndpointAuthSigningAlgValuesSupported(PolicyList<String> revocationEndpointAuthSigningAlgValuesSupported) {
        this.revocationEndpointAuthSigningAlgValuesSupported = revocationEndpointAuthSigningAlgValuesSupported;
    }

    public Policy<Boolean> getBackchannelLogoutSupported() {
        return backchannelLogoutSupported;
    }

    public void setBackchannelLogoutSupported(Policy<Boolean> backchannelLogoutSupported) {
        this.backchannelLogoutSupported = backchannelLogoutSupported;
    }

    public Policy<Boolean> getBackchannelLogoutSessionSupported() {
        return backchannelLogoutSessionSupported;
    }

    public void setBackchannelLogoutSessionSupported(Policy<Boolean> backchannelLogoutSessionSupported) {
        this.backchannelLogoutSessionSupported = backchannelLogoutSessionSupported;
    }

    public Policy<String> getDeviceAuthorizationEndpoint() {
        return deviceAuthorizationEndpoint;
    }

    public void setDeviceAuthorizationEndpoint(Policy<String> deviceAuthorizationEndpoint) {
        this.deviceAuthorizationEndpoint = deviceAuthorizationEndpoint;
    }

    public PolicyList<String> getBackchannelTokenDeliveryModesSupported() {
        return backchannelTokenDeliveryModesSupported;
    }

    public void setBackchannelTokenDeliveryModesSupported(PolicyList<String> backchannelTokenDeliveryModesSupported) {
        this.backchannelTokenDeliveryModesSupported = backchannelTokenDeliveryModesSupported;
    }

    public Policy<String> getBackchannelAuthenticationEndpoint() {
        return backchannelAuthenticationEndpoint;
    }

    public void setBackchannelAuthenticationEndpoint(Policy<String> backchannelAuthenticationEndpoint) {
        this.backchannelAuthenticationEndpoint = backchannelAuthenticationEndpoint;
    }

    public PolicyList<String> getBackchannelAuthenticationRequestSigningAlgValuesSupported() {
        return backchannelAuthenticationRequestSigningAlgValuesSupported;
    }

    public void setBackchannelAuthenticationRequestSigningAlgValuesSupported(PolicyList<String> backchannelAuthenticationRequestSigningAlgValuesSupported) {
        this.backchannelAuthenticationRequestSigningAlgValuesSupported = backchannelAuthenticationRequestSigningAlgValuesSupported;
    }

    public Policy<Boolean> getRequirePushedAuthorizationRequests() {
        return requirePushedAuthorizationRequests;
    }

    public void setRequirePushedAuthorizationRequests(Policy<Boolean> requirePushedAuthorizationRequests) {
        this.requirePushedAuthorizationRequests = requirePushedAuthorizationRequests;
    }

    public Policy<String> getPushedAuthorizationRequestEndpoint() {
        return pushedAuthorizationRequestEndpoint;
    }

    public void setPushedAuthorizationRequestEndpoint(Policy<String> pushedAuthorizationRequestEndpoint) {
        this.pushedAuthorizationRequestEndpoint = pushedAuthorizationRequestEndpoint;
    }

    public PolicyList<MTLSEndpointAliases> getMtlsEndpointAliases() {
        return mtlsEndpointAliases;
    }

    public void setMtlsEndpointAliases(PolicyList<MTLSEndpointAliases> mtlsEndpointAliases) {
        this.mtlsEndpointAliases = mtlsEndpointAliases;
    }
}
