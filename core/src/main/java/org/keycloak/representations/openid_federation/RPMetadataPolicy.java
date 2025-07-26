package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.keycloak.jose.jwk.JSONWebKeySet;

public class RPMetadataPolicy {

    @JsonProperty("client_registration_types")
    private PolicyList<String> clientRegistrationTypes;

    @JsonUnwrapped
    private CommonMetadataPolicy commonMetadataPolicy;

    @JsonProperty("redirect_uris")
    private PolicyList<String> redirectUris;

    @JsonProperty("token_endpoint_auth_method")
    private Policy<String> tokenEndpointAuthMethod;

    @JsonProperty("token_endpoint_auth_signing_alg")
    private Policy<String> tokenEndpointAuthSigningAlg;

    @JsonProperty("grant_types")
    private PolicyList<String> grantTypes;

    @JsonProperty("response_types")
    private PolicyList<String> responseTypes;

    @JsonProperty("application_type")
    private Policy<String> applicationType;

    @JsonProperty("client_name")
    private Policy<String> clientName;

    @JsonProperty("client_uri")
    private Policy<String> clientUri;

    @JsonProperty("client_id")
    private Policy<String> clientId;

    @JsonProperty("client_secret")
    private Policy<String> clientSecret;

    @JsonProperty("logo_uri")
    private Policy<String> logoUri;

    @JsonProperty("scope")
    private Policy<String> scope;

    @JsonProperty("contacts")
    private PolicyList<String> contacts;

    @JsonProperty("tos_uri")
    private Policy<String> tosUri;

    @JsonProperty("policy_uri")
    private Policy<String> policyUri;

    @JsonProperty("jwks_uri")
    private Policy<String> jwksUri;

    private Policy<JSONWebKeySet> jwks;

    @JsonProperty("sector_identifier_uri")
    private Policy<String> sectorIdentifierUri;

    @JsonProperty("subject_type")
    private Policy<String> subjectType;

    @JsonProperty("id_token_signed_response_alg")
    private Policy<String> idTokenSignedResponseAlg;

    @JsonProperty("id_token_encrypted_response_alg")
    private Policy<String> idTokenEncryptedResponseAlg;

    @JsonProperty("id_token_encrypted_response_enc")
    private Policy<String> idTokenEncryptedResponseEnc;

    @JsonProperty("userinfo_signed_response_alg")
    private Policy<String> userinfoSignedResponseAlg;

    @JsonProperty("userinfo_encrypted_response_alg")
    private Policy<String> userinfoEncryptedResponseAlg;

    @JsonProperty("userinfo_encrypted_response_enc")
    private Policy<String> userinfoEncryptedResponseEnc;

    @JsonProperty("request_object_signing_alg")
    private Policy<String> requestObjectSigningAlg;

    @JsonProperty("request_object_encryption_alg")
    private Policy<String> requestObjectEncryptionAlg;

    @JsonProperty("request_object_encryption_enc")
    private Policy<String> requestObjectEncryptionEnc;

    @JsonProperty("default_max_age")
    private Policy<Integer> defaultMaxAge;

    @JsonProperty("require_auth_time")
    private Policy<Boolean> requireAuthTime;

    @JsonProperty("default_acr_values")
    private PolicyList<String> defaultAcrValues;

    @JsonProperty("initiate_login_uri")
    private Policy<String> initiateLoginUri;

    @JsonProperty("request_uris")
    private PolicyList<String> requestUris;

    // KEYCLOAK-6771 Certificate Bound Token
    // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-6.5
    @JsonProperty("tls_client_certificate_bound_access_tokens")
    private Policy<Boolean> tlsClientCertificateBoundAccessTokens;

    @JsonProperty("tls_client_auth_subject_dn")
    private Policy<String> tlsClientAuthSubjectDn;

    // OIDC Session Management
    @JsonProperty("post_logout_redirect_uris")
    private PolicyList<String> postLogoutRedirectUris;

    // Not sure from which specs this comes
    @JsonProperty("software_id")
    private Policy<String> softwareId;

    @JsonProperty("software_version")
    private Policy<String> softwareVersion;

    // OIDC Dynamic Client Registration Response
    @JsonProperty("client_id_issued_at")
    private Policy<Integer> clientIdIssuedAt;

    @JsonProperty("client_secret_expires_at")
    private Policy<Integer> clientSecretExpiresAt;

    @JsonProperty("registration_client_uri")
    private Policy<String> registrationClientUri;

    @JsonProperty("registration_access_token")
    private Policy<String> registrationAccessToken;



    @JsonProperty("backchannel_token_delivery_mode")
    private Policy<String> backchannelTokenDeliveryMode;

    @JsonProperty("backchannel_client_notification_endpoint")
    private Policy<String> backchannelClientNotificationEndpoint;

    @JsonProperty("backchannel_authentication_request_signing_alg")
    private Policy<String> backchannelAuthenticationRequestSigningAlg;

    @JsonProperty("authorization_signed_response_alg")
    private Policy<String> authorizationSignedResponseAlg;

    @JsonProperty("authorization_encrypted_response_alg")
    private Policy<String> authorizationEncryptedResponseAlg;

    @JsonProperty("authorization_encrypted_response_enc")
    private Policy<String> authorizationEncryptedResponseEnc;

    @JsonProperty("require_pushed_authorization_requests")
    private Policy<Boolean> requirePushedAuthorizationRequests;

    @JsonProperty("frontchannel_logout_uri")
    private Policy<String> frontchannelLogoutUri;

    @JsonProperty("frontchannel_logout_session_required")
    private Policy<Boolean> frontchannelLogoutSessionRequired;

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

    public PolicyList<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(PolicyList<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public Policy<String> getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    public void setTokenEndpointAuthMethod(Policy<String> tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    public Policy<String> getTokenEndpointAuthSigningAlg() {
        return tokenEndpointAuthSigningAlg;
    }

    public void setTokenEndpointAuthSigningAlg(Policy<String> tokenEndpointAuthSigningAlg) {
        this.tokenEndpointAuthSigningAlg = tokenEndpointAuthSigningAlg;
    }

    public PolicyList<String> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(PolicyList<String> grantTypes) {
        this.grantTypes = grantTypes;
    }

    public PolicyList<String> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(PolicyList<String> responseTypes) {
        this.responseTypes = responseTypes;
    }

    public Policy<String> getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(Policy<String> applicationType) {
        this.applicationType = applicationType;
    }

    public Policy<String> getClientName() {
        return clientName;
    }

    public void setClientName(Policy<String> clientName) {
        this.clientName = clientName;
    }

    public Policy<String> getClientUri() {
        return clientUri;
    }

    public void setClientUri(Policy<String> clientUri) {
        this.clientUri = clientUri;
    }

    public Policy<String> getClientId() {
        return clientId;
    }

    public void setClientId(Policy<String> clientId) {
        this.clientId = clientId;
    }

    public Policy<String> getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(Policy<String> clientSecret) {
        this.clientSecret = clientSecret;
    }

    public Policy<String> getLogoUri() {
        return logoUri;
    }

    public void setLogoUri(Policy<String> logoUri) {
        this.logoUri = logoUri;
    }

    public Policy<String> getScope() {
        return scope;
    }

    public void setScope(Policy<String> scope) {
        this.scope = scope;
    }

    public PolicyList<String> getContacts() {
        return contacts;
    }

    public void setContacts(PolicyList<String> contacts) {
        this.contacts = contacts;
    }

    public Policy<String> getTosUri() {
        return tosUri;
    }

    public void setTosUri(Policy<String> tosUri) {
        this.tosUri = tosUri;
    }

    public Policy<String> getPolicyUri() {
        return policyUri;
    }

    public void setPolicyUri(Policy<String> policyUri) {
        this.policyUri = policyUri;
    }

    public Policy<String> getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(Policy<String> jwksUri) {
        this.jwksUri = jwksUri;
    }

    public Policy<JSONWebKeySet> getJwks() {
        return jwks;
    }

    public void setJwks(Policy<JSONWebKeySet> jwks) {
        this.jwks = jwks;
    }

    public Policy<String> getSectorIdentifierUri() {
        return sectorIdentifierUri;
    }

    public void setSectorIdentifierUri(Policy<String> sectorIdentifierUri) {
        this.sectorIdentifierUri = sectorIdentifierUri;
    }

    public Policy<String> getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(Policy<String> subjectType) {
        this.subjectType = subjectType;
    }

    public Policy<String> getIdTokenSignedResponseAlg() {
        return idTokenSignedResponseAlg;
    }

    public void setIdTokenSignedResponseAlg(Policy<String> idTokenSignedResponseAlg) {
        this.idTokenSignedResponseAlg = idTokenSignedResponseAlg;
    }

    public Policy<String> getIdTokenEncryptedResponseAlg() {
        return idTokenEncryptedResponseAlg;
    }

    public void setIdTokenEncryptedResponseAlg(Policy<String> idTokenEncryptedResponseAlg) {
        this.idTokenEncryptedResponseAlg = idTokenEncryptedResponseAlg;
    }

    public Policy<String> getIdTokenEncryptedResponseEnc() {
        return idTokenEncryptedResponseEnc;
    }

    public void setIdTokenEncryptedResponseEnc(Policy<String> idTokenEncryptedResponseEnc) {
        this.idTokenEncryptedResponseEnc = idTokenEncryptedResponseEnc;
    }

    public Policy<String> getUserinfoSignedResponseAlg() {
        return userinfoSignedResponseAlg;
    }

    public void setUserinfoSignedResponseAlg(Policy<String> userinfoSignedResponseAlg) {
        this.userinfoSignedResponseAlg = userinfoSignedResponseAlg;
    }

    public Policy<String> getUserinfoEncryptedResponseAlg() {
        return userinfoEncryptedResponseAlg;
    }

    public void setUserinfoEncryptedResponseAlg(Policy<String> userinfoEncryptedResponseAlg) {
        this.userinfoEncryptedResponseAlg = userinfoEncryptedResponseAlg;
    }

    public Policy<String> getUserinfoEncryptedResponseEnc() {
        return userinfoEncryptedResponseEnc;
    }

    public void setUserinfoEncryptedResponseEnc(Policy<String> userinfoEncryptedResponseEnc) {
        this.userinfoEncryptedResponseEnc = userinfoEncryptedResponseEnc;
    }

    public Policy<String> getRequestObjectSigningAlg() {
        return requestObjectSigningAlg;
    }

    public void setRequestObjectSigningAlg(Policy<String> requestObjectSigningAlg) {
        this.requestObjectSigningAlg = requestObjectSigningAlg;
    }

    public Policy<String> getRequestObjectEncryptionAlg() {
        return requestObjectEncryptionAlg;
    }

    public void setRequestObjectEncryptionAlg(Policy<String> requestObjectEncryptionAlg) {
        this.requestObjectEncryptionAlg = requestObjectEncryptionAlg;
    }

    public Policy<String> getRequestObjectEncryptionEnc() {
        return requestObjectEncryptionEnc;
    }

    public void setRequestObjectEncryptionEnc(Policy<String> requestObjectEncryptionEnc) {
        this.requestObjectEncryptionEnc = requestObjectEncryptionEnc;
    }

    public Policy<Integer> getDefaultMaxAge() {
        return defaultMaxAge;
    }

    public void setDefaultMaxAge(Policy<Integer> defaultMaxAge) {
        this.defaultMaxAge = defaultMaxAge;
    }

    public Policy<Boolean> getRequireAuthTime() {
        return requireAuthTime;
    }

    public void setRequireAuthTime(Policy<Boolean> requireAuthTime) {
        this.requireAuthTime = requireAuthTime;
    }

    public PolicyList<String> getDefaultAcrValues() {
        return defaultAcrValues;
    }

    public void setDefaultAcrValues(PolicyList<String> defaultAcrValues) {
        this.defaultAcrValues = defaultAcrValues;
    }

    public Policy<String> getInitiateLoginUri() {
        return initiateLoginUri;
    }

    public void setInitiateLoginUri(Policy<String> initiateLoginUri) {
        this.initiateLoginUri = initiateLoginUri;
    }

    public PolicyList<String> getRequestUris() {
        return requestUris;
    }

    public void setRequestUris(PolicyList<String> requestUris) {
        this.requestUris = requestUris;
    }

    public Policy<Boolean> getTlsClientCertificateBoundAccessTokens() {
        return tlsClientCertificateBoundAccessTokens;
    }

    public void setTlsClientCertificateBoundAccessTokens(Policy<Boolean> tlsClientCertificateBoundAccessTokens) {
        this.tlsClientCertificateBoundAccessTokens = tlsClientCertificateBoundAccessTokens;
    }

    public Policy<String> getTlsClientAuthSubjectDn() {
        return tlsClientAuthSubjectDn;
    }

    public void setTlsClientAuthSubjectDn(Policy<String> tlsClientAuthSubjectDn) {
        this.tlsClientAuthSubjectDn = tlsClientAuthSubjectDn;
    }

    public PolicyList<String> getPostLogoutRedirectUris() {
        return postLogoutRedirectUris;
    }

    public void setPostLogoutRedirectUris(PolicyList<String> postLogoutRedirectUris) {
        this.postLogoutRedirectUris = postLogoutRedirectUris;
    }

    public Policy<String> getSoftwareId() {
        return softwareId;
    }

    public void setSoftwareId(Policy<String> softwareId) {
        this.softwareId = softwareId;
    }

    public Policy<String> getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(Policy<String> softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public Policy<Integer> getClientIdIssuedAt() {
        return clientIdIssuedAt;
    }

    public void setClientIdIssuedAt(Policy<Integer> clientIdIssuedAt) {
        this.clientIdIssuedAt = clientIdIssuedAt;
    }

    public Policy<Integer> getClientSecretExpiresAt() {
        return clientSecretExpiresAt;
    }

    public void setClientSecretExpiresAt(Policy<Integer> clientSecretExpiresAt) {
        this.clientSecretExpiresAt = clientSecretExpiresAt;
    }

    public Policy<String> getRegistrationClientUri() {
        return registrationClientUri;
    }

    public void setRegistrationClientUri(Policy<String> registrationClientUri) {
        this.registrationClientUri = registrationClientUri;
    }

    public Policy<String> getRegistrationAccessToken() {
        return registrationAccessToken;
    }

    public void setRegistrationAccessToken(Policy<String> registrationAccessToken) {
        this.registrationAccessToken = registrationAccessToken;
    }

    public Policy<String> getBackchannelTokenDeliveryMode() {
        return backchannelTokenDeliveryMode;
    }

    public void setBackchannelTokenDeliveryMode(Policy<String> backchannelTokenDeliveryMode) {
        this.backchannelTokenDeliveryMode = backchannelTokenDeliveryMode;
    }

    public Policy<String> getBackchannelClientNotificationEndpoint() {
        return backchannelClientNotificationEndpoint;
    }

    public void setBackchannelClientNotificationEndpoint(Policy<String> backchannelClientNotificationEndpoint) {
        this.backchannelClientNotificationEndpoint = backchannelClientNotificationEndpoint;
    }

    public Policy<String> getBackchannelAuthenticationRequestSigningAlg() {
        return backchannelAuthenticationRequestSigningAlg;
    }

    public void setBackchannelAuthenticationRequestSigningAlg(Policy<String> backchannelAuthenticationRequestSigningAlg) {
        this.backchannelAuthenticationRequestSigningAlg = backchannelAuthenticationRequestSigningAlg;
    }

    public Policy<String> getAuthorizationSignedResponseAlg() {
        return authorizationSignedResponseAlg;
    }

    public void setAuthorizationSignedResponseAlg(Policy<String> authorizationSignedResponseAlg) {
        this.authorizationSignedResponseAlg = authorizationSignedResponseAlg;
    }

    public Policy<String> getAuthorizationEncryptedResponseAlg() {
        return authorizationEncryptedResponseAlg;
    }

    public void setAuthorizationEncryptedResponseAlg(Policy<String> authorizationEncryptedResponseAlg) {
        this.authorizationEncryptedResponseAlg = authorizationEncryptedResponseAlg;
    }

    public Policy<String> getAuthorizationEncryptedResponseEnc() {
        return authorizationEncryptedResponseEnc;
    }

    public void setAuthorizationEncryptedResponseEnc(Policy<String> authorizationEncryptedResponseEnc) {
        this.authorizationEncryptedResponseEnc = authorizationEncryptedResponseEnc;
    }

    public Policy<Boolean> getRequirePushedAuthorizationRequests() {
        return requirePushedAuthorizationRequests;
    }

    public void setRequirePushedAuthorizationRequests(Policy<Boolean> requirePushedAuthorizationRequests) {
        this.requirePushedAuthorizationRequests = requirePushedAuthorizationRequests;
    }

    public Policy<String> getFrontchannelLogoutUri() {
        return frontchannelLogoutUri;
    }

    public void setFrontchannelLogoutUri(Policy<String> frontchannelLogoutUri) {
        this.frontchannelLogoutUri = frontchannelLogoutUri;
    }

    public Policy<Boolean> getFrontchannelLogoutSessionRequired() {
        return frontchannelLogoutSessionRequired;
    }

    public void setFrontchannelLogoutSessionRequired(Policy<Boolean> frontchannelLogoutSessionRequired) {
        this.frontchannelLogoutSessionRequired = frontchannelLogoutSessionRequired;
    }
}
