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
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.utils.StringUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCAdvancedConfigWrapper {

    private final ClientModel clientModel;
    private final ClientRepresentation clientRep;

    private OIDCAdvancedConfigWrapper(ClientModel client, ClientRepresentation clientRep) {
        this.clientModel = client;
        this.clientRep = clientRep;
    }


    public static OIDCAdvancedConfigWrapper fromClientModel(ClientModel client) {
        return new OIDCAdvancedConfigWrapper(client, null);
    }

    public static OIDCAdvancedConfigWrapper fromClientRepresentation(ClientRepresentation clientRep) {
        return new OIDCAdvancedConfigWrapper(null, clientRep);
    }


    public Algorithm getUserInfoSignedResponseAlg() {
        String alg = getAttribute(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG);
        return alg==null ? null : Enum.valueOf(Algorithm.class, alg);
    }

    public void setUserInfoSignedResponseAlg(Algorithm alg) {
        String algStr = alg==null ? null : alg.toString();
        setAttribute(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, algStr);
    }

    public boolean isUserInfoSignatureRequired() {
        return getUserInfoSignedResponseAlg() != null;
    }

    public Algorithm getRequestObjectSignatureAlg() {
        String alg = getAttribute(OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG);
        return alg==null ? null : Enum.valueOf(Algorithm.class, alg);
    }

    public void setRequestObjectSignatureAlg(Algorithm alg) {
        String algStr = alg==null ? null : alg.toString();
        setAttribute(OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG, algStr);
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

    public void setLogoUri(String logoUri) {
        setAttribute(ClientModel.LOGO_URI, logoUri);
    }

    public void setPolicyUri(String policyUri) {
        setAttribute(ClientModel.POLICY_URI, policyUri);
    }

    public void setTosUri(String tosUri) {
        setAttribute(ClientModel.TOS_URI, tosUri);
    }

    private String getAttribute(String attrKey) {
        if (clientModel != null) {
            return clientModel.getAttribute(attrKey);
        } else {
            return clientRep.getAttributes()==null ? null : clientRep.getAttributes().get(attrKey);
        }
    }

    private String getAttribute(String attrKey, String defaultValue) {
        String value = getAttribute(attrKey);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    private void setAttribute(String attrKey, String attrValue) {
        if (clientModel != null) {
            if (attrValue != null) {
                clientModel.setAttribute(attrKey, attrValue);
            } else {
                clientModel.removeAttribute(attrKey);
            }
        } else {
            if (attrValue != null) {
                if (clientRep.getAttributes() == null) {
                    clientRep.setAttributes(new HashMap<>());
                }
                clientRep.getAttributes().put(attrKey, attrValue);
            } else {
                if (clientRep.getAttributes() != null) {
                    clientRep.getAttributes().put(attrKey, null);
                }
            }
        }
    }

    private List<String> getAttributeMultivalued(String attrKey) {
        String attrValue = getAttribute(attrKey);
        if (attrValue == null) return Collections.emptyList();
        return Arrays.asList(Constants.CFG_DELIMITER_PATTERN.split(attrValue));
    }

    private void setAttributeMultivalued(String attrKey, List<String> attrValues) {
        if (attrValues == null || attrValues.size() == 0) {
            // Remove attribute
            setAttribute(attrKey, null);
        } else {
            String attrValueFull = String.join(Constants.CFG_DELIMITER, attrValues);
            setAttribute(attrKey, attrValueFull);
        }
    }
}
