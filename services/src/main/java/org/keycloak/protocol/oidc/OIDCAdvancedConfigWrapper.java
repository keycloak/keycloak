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

package org.keycloak.protocol.oidc;

import static org.keycloak.protocol.oidc.OIDCConfigAttributes.USE_LOWER_CASE_IN_TOKEN_RESPONSE;

import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.models.ClientModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCAdvancedConfigWrapper extends AbstractClientConfigWrapper {

    private OIDCAdvancedConfigWrapper(ClientModel client, ClientRepresentation clientRep) {
        super(client,clientRep);
    }

    public static OIDCAdvancedConfigWrapper fromClientModel(ClientModel client) {
        return new OIDCAdvancedConfigWrapper(client, null);
    }

    public static OIDCAdvancedConfigWrapper fromClientRepresentation(ClientRepresentation clientRep) {
        return new OIDCAdvancedConfigWrapper(null, clientRep);
    }


    public String getUserInfoSignedResponseAlg() {
        return getAttribute(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG);
    }

    public void setUserInfoSignedResponseAlg(String algorithm) {
        setAttribute(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, algorithm);
    }

    public boolean isUserInfoSignatureRequired() {
        return getUserInfoSignedResponseAlg() != null;
    }

    public void setUserInfoEncryptedResponseAlg(String algorithm) {
        setAttribute(OIDCConfigAttributes.USER_INFO_ENCRYPTED_RESPONSE_ALG, algorithm);
    }

    public String getUserInfoEncryptedResponseAlg() {
        return getAttribute(OIDCConfigAttributes.USER_INFO_ENCRYPTED_RESPONSE_ALG);
    }

    public String getUserInfoEncryptedResponseEnc() {
        return getAttribute(OIDCConfigAttributes.USER_INFO_ENCRYPTED_RESPONSE_ENC);
    }

    public void setUserInfoEncryptedResponseEnc(String algorithm) {
        setAttribute(OIDCConfigAttributes.USER_INFO_ENCRYPTED_RESPONSE_ENC, algorithm);
    }

    public boolean isUserInfoEncryptionRequired() {
        return getUserInfoEncryptedResponseAlg() != null;
    }

    public String getRequestObjectSignatureAlg() {
        return getAttribute(OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG);
    }

    public void setRequestObjectSignatureAlg(String algorithm) {
        setAttribute(OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG, algorithm);
    }

    public void setRequestObjectEncryptionAlg(String algorithm) {
        setAttribute(OIDCConfigAttributes.REQUEST_OBJECT_ENCRYPTION_ALG, algorithm);
    }

    public String getRequestObjectEncryptionAlg() {
        return getAttribute(OIDCConfigAttributes.REQUEST_OBJECT_ENCRYPTION_ALG);
    }

    public String getRequestObjectEncryptionEnc() {
        return getAttribute(OIDCConfigAttributes.REQUEST_OBJECT_ENCRYPTION_ENC);
    }

    public void setRequestObjectEncryptionEnc(String algorithm) {
        setAttribute(OIDCConfigAttributes.REQUEST_OBJECT_ENCRYPTION_ENC, algorithm);
    }

    public String getRequestObjectRequired() {
        return getAttribute(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED);
    }
    
    public void setRequestObjectRequired(String requestObjectRequired) {
        setAttribute(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED, requestObjectRequired);
    }

    public List<String> getRequestUris() {
        return getAttributeMultivalued(OIDCConfigAttributes.REQUEST_URIS);
    }

    public void setRequestUris(List<String> requestUris) {
        setAttributeMultivalued(OIDCConfigAttributes.REQUEST_URIS, requestUris);
    }

    public boolean isUseJwksUrl() {
        String useJwksUrl = getAttribute(OIDCConfigAttributes.USE_JWKS_URL);
        return Boolean.parseBoolean(useJwksUrl);
    }

    public void setUseJwksUrl(boolean useJwksUrl) {
        String val = String.valueOf(useJwksUrl);
        setAttribute(OIDCConfigAttributes.USE_JWKS_URL, val);
    }

    public String getJwksUrl() {
        return getAttribute(OIDCConfigAttributes.JWKS_URL);
    }

    public void setJwksUrl(String jwksUrl) {
        setAttribute(OIDCConfigAttributes.JWKS_URL, jwksUrl);
    }

    public boolean isUseJwksString() {
        String useJwksString = getAttribute(OIDCConfigAttributes.USE_JWKS_STRING);
        return Boolean.parseBoolean(useJwksString);
    }

    public void setUseJwksString(boolean useJwksString) {
        String val = String.valueOf(useJwksString);
        setAttribute(OIDCConfigAttributes.USE_JWKS_STRING, val);
    }

    public String getJwksString() {
        return getAttribute(OIDCConfigAttributes.JWKS_STRING);
    }

    public void setJwksString(String jwksString) {
        setAttribute(OIDCConfigAttributes.JWKS_STRING, jwksString);
    }

    public boolean isExcludeSessionStateFromAuthResponse() {
        String excludeSessionStateFromAuthResponse = getAttribute(OIDCConfigAttributes.EXCLUDE_SESSION_STATE_FROM_AUTH_RESPONSE);
        return Boolean.parseBoolean(excludeSessionStateFromAuthResponse);
    }

    public void setExcludeSessionStateFromAuthResponse(boolean excludeSessionStateFromAuthResponse) {
        String val = String.valueOf(excludeSessionStateFromAuthResponse);
        setAttribute(OIDCConfigAttributes.EXCLUDE_SESSION_STATE_FROM_AUTH_RESPONSE, val);
    }

    // KEYCLOAK-6771 Certificate Bound Token
    // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-6.5
    public boolean isUseMtlsHokToken() {
        String useUtlsHokToken = getAttribute(OIDCConfigAttributes.USE_MTLS_HOK_TOKEN);
        return Boolean.parseBoolean(useUtlsHokToken);
    }

    public void setUseMtlsHoKToken(boolean useUtlsHokToken) {
        String val = String.valueOf(useUtlsHokToken);
        setAttribute(OIDCConfigAttributes.USE_MTLS_HOK_TOKEN, val);
    }

    public boolean isUseRefreshToken() {
        String useRefreshToken = getAttribute(OIDCConfigAttributes.USE_REFRESH_TOKEN, "true");
        return Boolean.parseBoolean(useRefreshToken);
    }

    public void setUseRefreshToken(boolean useRefreshToken) {
        String val = String.valueOf(useRefreshToken);
        setAttribute(OIDCConfigAttributes.USE_REFRESH_TOKEN, val);
    }

    public boolean isUseLowerCaseInTokenResponse() {
        return Boolean.parseBoolean(getAttribute(USE_LOWER_CASE_IN_TOKEN_RESPONSE, "false"));
    }

    public void setUseLowerCaseInTokenResponse(boolean useRefreshToken) {
        setAttribute(USE_LOWER_CASE_IN_TOKEN_RESPONSE, String.valueOf(useRefreshToken));
    }

    /**
     * If true, then Client Credentials Grant generates refresh token and creates user session. This is not per specs, so it is false by default
     * For the details @see https://tools.ietf.org/html/rfc6749#section-4.4.3
     */
    public boolean isUseRefreshTokenForClientCredentialsGrant() {
        String val = getAttribute(OIDCConfigAttributes.USE_REFRESH_TOKEN_FOR_CLIENT_CREDENTIALS_GRANT, "false");
        return Boolean.parseBoolean(val);
    }

    public void setUseRefreshTokenForClientCredentialsGrant(boolean enable) {
        String val =  String.valueOf(enable);
        setAttribute(OIDCConfigAttributes.USE_REFRESH_TOKEN_FOR_CLIENT_CREDENTIALS_GRANT, val);
    }

    public String getTlsClientAuthSubjectDn() {
        return getAttribute(X509ClientAuthenticator.ATTR_SUBJECT_DN);
     }

    public void setTlsClientAuthSubjectDn(String tls_client_auth_subject_dn) {
        setAttribute(X509ClientAuthenticator.ATTR_SUBJECT_DN, tls_client_auth_subject_dn);
    }

    public boolean getAllowRegexPatternComparison() {
        String attrVal = getAttribute(X509ClientAuthenticator.ATTR_ALLOW_REGEX_PATTERN_COMPARISON);
        // Allow Regex Pattern Comparison by default due the backwards compatibility
        return attrVal == null || Boolean.parseBoolean(attrVal);
    }

    public void setAllowRegexPatternComparison(boolean allowRegexPatternComparison) {
        setAttribute(X509ClientAuthenticator.ATTR_ALLOW_REGEX_PATTERN_COMPARISON, String.valueOf(allowRegexPatternComparison));
    }

    public String getPkceCodeChallengeMethod() {
        return getAttribute(OIDCConfigAttributes.PKCE_CODE_CHALLENGE_METHOD);
    }

    public void setPkceCodeChallengeMethod(String codeChallengeMethodName) {
        setAttribute(OIDCConfigAttributes.PKCE_CODE_CHALLENGE_METHOD, codeChallengeMethodName);
    }

    public String getIdTokenSignedResponseAlg() {
        return getAttribute(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG);
    }
    public void setIdTokenSignedResponseAlg(String algName) {
        setAttribute(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG, algName);
    }

    public String getIdTokenEncryptedResponseAlg() {
        return getAttribute(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ALG);
    }

    public void setIdTokenEncryptedResponseAlg(String algName) {
        setAttribute(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ALG, algName);
    }

    public String getIdTokenEncryptedResponseEnc() {
        return getAttribute(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ENC);
    }

    public void setIdTokenEncryptedResponseEnc(String encName) {
        setAttribute(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ENC, encName);
    }

    public String getAuthorizationSignedResponseAlg() {
        return getAttribute(OIDCConfigAttributes.AUTHORIZATION_SIGNED_RESPONSE_ALG);
    }
    public void setAuthorizationSignedResponseAlg(String algName) {
        setAttribute(OIDCConfigAttributes.AUTHORIZATION_SIGNED_RESPONSE_ALG, algName);
    }

    public String getAuthorizationEncryptedResponseAlg() {
        return getAttribute(OIDCConfigAttributes.AUTHORIZATION_ENCRYPTED_RESPONSE_ALG);
    }

    public void setAuthorizationEncryptedResponseAlg(String algName) {
        setAttribute(OIDCConfigAttributes.AUTHORIZATION_ENCRYPTED_RESPONSE_ALG, algName);
    }

    public String getAuthorizationEncryptedResponseEnc() {
        return getAttribute(OIDCConfigAttributes.AUTHORIZATION_ENCRYPTED_RESPONSE_ENC);
    }

    public void setAuthorizationEncryptedResponseEnc(String encName) {
        setAttribute(OIDCConfigAttributes.AUTHORIZATION_ENCRYPTED_RESPONSE_ENC, encName);
    }

    public String getTokenEndpointAuthSigningAlg() {
        return getAttribute(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG);
    }

    public void setTokenEndpointAuthSigningAlg(String algName) {
        setAttribute(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, algName);
    }

    public String getBackchannelLogoutUrl() {
        return getAttribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL);
    }

    public void setBackchannelLogoutUrl(String backchannelLogoutUrl) {
        setAttribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, backchannelLogoutUrl);
    }

    public boolean isBackchannelLogoutSessionRequired() {
        String backchannelLogoutSessionRequired = getAttribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_SESSION_REQUIRED);
        return Boolean.parseBoolean(backchannelLogoutSessionRequired);
    }

    public void setBackchannelLogoutSessionRequired(boolean backchannelLogoutSessionRequired) {
        String val = String.valueOf(backchannelLogoutSessionRequired);
        setAttribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_SESSION_REQUIRED, val);
    }

    public boolean getBackchannelLogoutRevokeOfflineTokens() {
        String backchannelLogoutRevokeOfflineTokens = getAttribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_REVOKE_OFFLINE_TOKENS);
        return Boolean.parseBoolean(backchannelLogoutRevokeOfflineTokens);
    }

    public void setBackchannelLogoutRevokeOfflineTokens(boolean backchannelLogoutRevokeOfflineTokens) {
        String val = String.valueOf(backchannelLogoutRevokeOfflineTokens);
        setAttribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_REVOKE_OFFLINE_TOKENS, val);
    }

    public void setFrontChannelLogoutUrl(String frontChannelLogoutUrl) {
        if (clientRep != null) {
            clientRep.setFrontchannelLogout(StringUtil.isNotBlank(frontChannelLogoutUrl));
        }
        if (clientModel != null) {
            clientModel.setFrontchannelLogout(StringUtil.isNotBlank(frontChannelLogoutUrl));
        }
        setAttribute(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, frontChannelLogoutUrl);
    }

    public boolean isFrontChannelLogoutEnabled() {
        return clientModel != null && clientModel.isFrontchannelLogout() && StringUtil.isNotBlank(getFrontChannelLogoutUrl());
    }

    public String getFrontChannelLogoutUrl() {
        return getAttribute(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI);
    }

    public boolean isFrontChannelLogoutSessionRequired() {
        String frontChannelLogoutSessionRequired = getAttribute(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED);
        // Include session by default for backwards compatibility
        return frontChannelLogoutSessionRequired == null ? true : Boolean.parseBoolean(frontChannelLogoutSessionRequired);
    }

    public void setFrontChannelLogoutSessionRequired(boolean frontChannelLogoutSessionRequired) {
        String val = String.valueOf(frontChannelLogoutSessionRequired);
        setAttribute(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED, val);
    }

    public void setLogoUri(String logoUri) {
        setAttribute(ClientModel.LOGO_URI, logoUri);
    }

    public void setPolicyUri(String policyUri) {
        setAttribute(ClientModel.POLICY_URI, policyUri);
    }

    public void setTosUri(String tosUri) {
        setAttribute(ClientModel.TOS_URI, tosUri);
    }

    public List<String> getPostLogoutRedirectUris() {
        List<String> postLogoutRedirectUris = getAttributeMultivalued(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS);
        if(postLogoutRedirectUris == null || postLogoutRedirectUris.isEmpty()) {
            return null;
        }
        else if (postLogoutRedirectUris.get(0).equals("+")) {
            if(clientModel != null) {
                return new ArrayList(clientModel.getRedirectUris());
            }
            else if(clientRep != null) {
                return clientRep.getRedirectUris();
            }
            return null;
        }
        else {
            return postLogoutRedirectUris;
        }
    }

    public void setPostLogoutRedirectUris(List<String> postLogoutRedirectUris) {
        setAttributeMultivalued(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, postLogoutRedirectUris);
    }

}
