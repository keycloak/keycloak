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
package org.keycloak.broker.oidc;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oauth2.OAuth2WellKnownProviderFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCWellKnownProviderFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.services.util.DPoPUtil;
import org.keycloak.utils.KeycloakSessionUtil;
import org.keycloak.utils.StringUtil;

import static org.keycloak.common.util.UriUtils.checkUrl;

/**
 * @author Pedro Igor
 */
public class OAuth2IdentityProviderConfig extends IdentityProviderModel {

    public static final String PKCE_ENABLED = "pkceEnabled";
    public static final String PKCE_METHOD = "pkceMethod";
    public static final String TOKEN_ENDPOINT_URL = "tokenUrl";
    public static final String TOKEN_INTROSPECTION_URL = "tokenIntrospectionUrl";

    public static final String JWT_X509_HEADERS_ENABLED = "jwtX509HeadersEnabled";

    public static final String REQUIRES_SHORT_STATE_PARAMETER = "requiresShortStateParameter";
    public static final String PAR_ENABLED = "pushedAuthorizationRequestsEnabled";
    public static final String PAR_URL = "pushedAuthorizationRequestUrl";
    public static final String PAR_REQUIRED = "pushedAuthorizationRequestsRequired";
    public static final String REQUEST_OBJECT_ENABLED = "requestObjectEnabled";
    public static final String REQUEST_OBJECT_SIGNING_ALG = "requestObjectSigningAlg";
    public static final String DPOP_ENABLED = "dpopEnabled";
    public static final String DPOP_SIGNING_ALG = "dpopSigningAlg";
    public static final String AUTHORIZATION_RESPONSE_ISSUER_REQUIRED = "authorizationResponseIssuerRequired";
    public static final String VALIDATE_DISCOVERY_ISSUER = "validateDiscoveryIssuer";

    private static final String OPENID_CONFIGURATION = "/.well-known/" + OIDCWellKnownProviderFactory.PROVIDER_ID;
    private static final String OAUTH_AUTHORIZATION_SERVER = "/.well-known/" + OAuth2WellKnownProviderFactory.PROVIDER_ID;

    public OAuth2IdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public OAuth2IdentityProviderConfig() {
        super();
    }

    public String getAuthorizationUrl() {
        return getConfig().get("authorizationUrl");
    }

    public void setAuthorizationUrl(String authorizationUrl) {
        getConfig().put("authorizationUrl", authorizationUrl);
    }

    public String getTokenUrl() {
        return getConfig().get(TOKEN_ENDPOINT_URL);
    }

    public void setTokenUrl(String tokenUrl) {
        getConfig().put(TOKEN_ENDPOINT_URL, tokenUrl);
    }

    public String getUserInfoUrl() {
        return getConfig().get("userInfoUrl");
    }

    public void setUserInfoUrl(String userInfoUrl) {
        getConfig().put("userInfoUrl", userInfoUrl);
    }

    public String getTokenIntrospectionUrl() {
        return getConfig().get(TOKEN_INTROSPECTION_URL);
    }

    public void setTokenIntrospectionUrl(String introspectionEndpointUrl) {
        getConfig().put(TOKEN_INTROSPECTION_URL, introspectionEndpointUrl);
    }

    public String getClientId() {
        return getConfig().get("clientId");
    }

    public void setClientId(String clientId) {
        getConfig().put("clientId", clientId);
    }

    public String getClientAuthMethod() {
        return getConfig().getOrDefault("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_POST);
    }

    public void setClientAuthMethod(String clientAuth) {
        getConfig().put("clientAuthMethod", clientAuth);
    }

    public String getClientSecret() {
        return getConfig().get("clientSecret");
    }

    public void setClientSecret(String clientSecret) {
        getConfig().put("clientSecret", clientSecret);
    }

    public String getDefaultScope() {
        return getConfig().get("defaultScope");
    }

    public void setDefaultScope(String defaultScope) {
        getConfig().put("defaultScope", defaultScope);
    }
    
    public boolean isJWTAuthentication() {
        if (getClientAuthMethod().equals(OIDCLoginProtocol.CLIENT_SECRET_JWT)
                || getClientAuthMethod().equals(OIDCLoginProtocol.PRIVATE_KEY_JWT)) {
            return true;
        }
        return false;
    }

    public boolean isBasicAuthentication(){
        return getClientAuthMethod().equals(OIDCLoginProtocol.CLIENT_SECRET_BASIC);
    }

    public boolean isBasicAuthenticationUnencoded(){
        return getClientAuthMethod().equals(OIDCLoginProtocol.CLIENT_SECRET_BASIC_UNENCODED);
    }

    public boolean isUiLocales() {
        return Boolean.valueOf(getConfig().get("uiLocales"));
    }

    public void setUiLocales(boolean uiLocales) {
        getConfig().put("uiLocales", String.valueOf(uiLocales));
    }

    public String getPrompt() {
        return getConfig().get("prompt");
    }

    public boolean isRequiresShortStateParameter() {
        return Boolean.parseBoolean(getConfig().get(REQUIRES_SHORT_STATE_PARAMETER));
    }

    public void setRequiresShortStateParameter(boolean requiresShortStateParameter) {
        getConfig().put(REQUIRES_SHORT_STATE_PARAMETER, String.valueOf(requiresShortStateParameter));
    }

    public String getForwardParameters() {
        return getConfig().get("forwardParameters");
    }

    public void setForwardParameters(String forwardParameters) {
       getConfig().put("forwardParameters", forwardParameters);
    }

    public boolean isPkceEnabled() {
        return Boolean.parseBoolean(getConfig().getOrDefault(PKCE_ENABLED, "false"));
    }

    public void setPkceEnabled(boolean enabled) {
        getConfig().put(PKCE_ENABLED, String.valueOf(enabled));
    }

    public String getPkceMethod() {
        return getConfig().get(PKCE_METHOD);
    }

    public String setPkceMethod(String method) {
        return getConfig().put(PKCE_METHOD, method);
    }

    public String getClientAssertionSigningAlg() {
        return getConfig().get("clientAssertionSigningAlg");
    }
    
    public void setClientAssertionSigningAlg(String signingAlg) {
        getConfig().put("clientAssertionSigningAlg", signingAlg);
    }

    public String getClientAssertionAudience() {
        return getConfig().get("clientAssertionAudience");
    }

    public void setClientAssertionAudience(String audience) {
        getConfig().put("clientAssertionAudience", audience);
    }


    public boolean isJwtX509HeadersEnabled() {
        if (getClientAuthMethod().equals(OIDCLoginProtocol.PRIVATE_KEY_JWT)
            && Boolean.parseBoolean(getConfig().getOrDefault(JWT_X509_HEADERS_ENABLED, "false"))) {
            return true;
        }
        return false;
    }

    public void setJwtX509HeadersEnabled(boolean enabled) {
        getConfig().put(JWT_X509_HEADERS_ENABLED, String.valueOf(enabled));
    }

    public String getUserIDClaim() {
        return getConfig().getOrDefault("userIDClaim", IDToken.SUBJECT);
    }

    public String getUserNameClaim() {
        return getConfig().getOrDefault("userNameClaim", IDToken.PREFERRED_USERNAME);
    }

    public String getFullNameClaim() {
        return getConfig().getOrDefault("fullNameClaim", IDToken.NAME);
    }

    public String getGivenNameClaim() {
        return getConfig().getOrDefault("givenNameClaim", IDToken.GIVEN_NAME);
    }

    public String getFamilyNameClaim() {
        return getConfig().getOrDefault("familyNameClaim", IDToken.FAMILY_NAME);
    }

    public String getEmailClaim() {
        return getConfig().getOrDefault("emailClaim", IDToken.EMAIL);
    }

    public boolean isPushedAuthorizationRequestsEnabled() {
        return Boolean.parseBoolean(getConfig().getOrDefault(PAR_ENABLED, "false"));
    }

    public void setPushedAuthorizationRequestsEnabled(boolean enabled) {
        getConfig().put(PAR_ENABLED, String.valueOf(enabled));
    }

    /**
     * @return whether the identity provider metadata requires pushed authorization requests
     */
    public boolean isPushedAuthorizationRequestsRequired() {
        return Boolean.parseBoolean(getConfig().getOrDefault(PAR_REQUIRED, "false"));
    }

    public void setPushedAuthorizationRequestsRequired(boolean required) {
        getConfig().put(PAR_REQUIRED, String.valueOf(required));
    }

    public String getPushedAuthorizationRequestUrl() {
        return getConfig().get(PAR_URL);
    }

    public void setPushedAuthorizationRequestUrl(String url) {
        getConfig().put(PAR_URL, url);
    }

    public boolean isRequestObjectEnabled() {
        return Boolean.parseBoolean(getConfig().getOrDefault(REQUEST_OBJECT_ENABLED, "false"));
    }

    public void setRequestObjectEnabled(boolean enabled) {
        getConfig().put(REQUEST_OBJECT_ENABLED, String.valueOf(enabled));
    }

    public String getRequestObjectSigningAlg() {
        return getConfig().getOrDefault(REQUEST_OBJECT_SIGNING_ALG, Algorithm.PS256);
    }

    public void setRequestObjectSigningAlg(String signingAlg) {
        getConfig().put(REQUEST_OBJECT_SIGNING_ALG, signingAlg);
    }

    public boolean isDPoPEnabled() {
        return Boolean.parseBoolean(getConfig().getOrDefault(DPOP_ENABLED, "false"));
    }

    public void setDPoPEnabled(boolean enabled) {
        getConfig().put(DPOP_ENABLED, String.valueOf(enabled));
    }

    public String getDPoPSigningAlg() {
        return getConfig().getOrDefault(DPOP_SIGNING_ALG, Algorithm.ES256);
    }

    public void setDPoPSigningAlg(String signingAlg) {
        getConfig().put(DPOP_SIGNING_ALG, signingAlg);
    }

    public boolean isAuthorizationResponseIssuerRequired() {
        return Boolean.parseBoolean(getConfig().getOrDefault(AUTHORIZATION_RESPONSE_ISSUER_REQUIRED, "false"));
    }

    public void setAuthorizationResponseIssuerRequired(boolean required) {
        getConfig().put(AUTHORIZATION_RESPONSE_ISSUER_REQUIRED, String.valueOf(required));
    }

    public boolean isValidateDiscoveryIssuer() {
        return Boolean.parseBoolean(getConfig().getOrDefault(VALIDATE_DISCOVERY_ISSUER, "false"));
    }

    public void setValidateDiscoveryIssuer(boolean validate) {
        getConfig().put(VALIDATE_DISCOVERY_ISSUER, String.valueOf(validate));
    }

    public String getIssuer() {
        return getConfig().get(ISSUER);
    }

    public void setIssuer(String issuer) {
        getConfig().put(ISSUER, issuer);
    }

    /**
     * Imports the PAR and DPoP settings and the {@code iss} support from the identity provider metadata.
     */
    public void importMetadata(KeycloakSession session, OIDCConfigurationRepresentation rep) {
        if (rep.getPushedAuthorizationRequestEndpoint() != null) {
            setPushedAuthorizationRequestUrl(rep.getPushedAuthorizationRequestEndpoint());
        }
        if (Boolean.TRUE.equals(rep.getRequirePushedAuthorizationRequests())) {
            setPushedAuthorizationRequestsRequired(true);
            setPushedAuthorizationRequestsEnabled(true);
        }
        if (Boolean.TRUE.equals(rep.getAuthorizationResponseIssParameterSupported())) {
            setAuthorizationResponseIssuerRequired(true);
        }
        if (rep.getDpopSigningAlgValuesSupported() != null) {
            // the first algorithm both sides support with ES256 and PS256 preferred
            List<String> supported = DPoPUtil.getDPoPSupportedAlgorithms(session);
            Stream.concat(Stream.of(Algorithm.ES256, Algorithm.PS256), rep.getDpopSigningAlgValuesSupported().stream())
                    .filter(rep.getDpopSigningAlgValuesSupported()::contains)
                    .filter(supported::contains)
                    .findFirst()
                    .ifPresent(this::setDPoPSigningAlg);
        }
    }

    @Override
    public void validate(RealmModel realm) {
        SslRequired sslRequired = realm.getSslRequired();

        checkUrl(sslRequired, getAuthorizationUrl(), "authorization_url");
        checkUrl(sslRequired, getTokenUrl(), "token_url");
        checkUrl(sslRequired, getUserInfoUrl(), "userinfo_url");
        checkUrl(sslRequired, getTokenIntrospectionUrl(), "tokenIntrospection_url");

        if (isPushedAuthorizationRequestsRequired() && !isPushedAuthorizationRequestsEnabled()) {
            throw new IllegalArgumentException("The identity provider only accepts pushed authorization requests");
        }

        if (isPushedAuthorizationRequestsEnabled()) {
            if (StringUtil.isBlank(getPushedAuthorizationRequestUrl())) {
                throw new IllegalArgumentException("The pushed authorization request endpoint is required when pushed authorization requests are enabled");
            }
            checkUrl(sslRequired, getPushedAuthorizationRequestUrl(), "pushed_authorization_request_url");
        }

        if (isDPoPEnabled()) {
            KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
            if (!DPoPUtil.getDPoPSupportedAlgorithms(session).contains(getDPoPSigningAlg())) {
                throw new IllegalArgumentException("DPoP signing algorithm not supported: " + getDPoPSigningAlg());
            }
        }

        if (isRequestObjectEnabled() && StringUtil.isBlank(getIssuer())) {
            throw new IllegalArgumentException("The issuer is required as the audience of the request object");
        }

        if (isAuthorizationResponseIssuerRequired() && StringUtil.isBlank(getIssuer())) {
            throw new IllegalArgumentException("The issuer is required to validate the issuer of the authorization response");
        }

        if (isValidateDiscoveryIssuer()) {
            if (StringUtil.isBlank(getMetadataDescriptorUrl())) {
                throw new IllegalArgumentException("The discovery endpoint is required to validate its issuer");
            }
            if (StringUtil.isBlank(getIssuer())) {
                throw new IllegalArgumentException("The issuer is required to validate it against the discovery endpoint");
            }
            // https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfigurationValidation
            // https://datatracker.ietf.org/doc/html/rfc8414#section-3.3
            // the discovery endpoint drops the trailing slash of the issuer
            if (!StringUtil.removeSuffix(getIssuer(), "/").equals(issuerOf(getMetadataDescriptorUrl()))) {
                throw new IllegalArgumentException("The issuer does not match the discovery endpoint");
            }
        }

        if (isPkceEnabled()) {
            String pkceMethod = getPkceMethod();
            if (!Arrays.asList(OAuth2Constants.PKCE_METHOD_PLAIN, OAuth2Constants.PKCE_METHOD_S256).contains(pkceMethod)) {
                throw new IllegalArgumentException("PKCE Method not supported: " + pkceMethod);
            }
        }
    }

    /**
     * @return the issuer of an OpenID Connect Discovery or RFC 8414 metadata URL or {@code null}
     */
    private static String issuerOf(String discoveryUrl) {
        // the two differ when the issuer has a path
        // https://datatracker.ietf.org/doc/html/rfc8414#section-5
        // OpenID Connect Discovery: https://idp.example.com/realms/test/.well-known/openid-configuration
        if (discoveryUrl.endsWith(OPENID_CONFIGURATION)) {
            return discoveryUrl.substring(0, discoveryUrl.length() - OPENID_CONFIGURATION.length());
        }
        // RFC 8414: https://idp.example.com/.well-known/oauth-authorization-server/realms/test
        URI uri = URI.create(discoveryUrl);
        String path = uri.getRawPath();
        if (path != null && path.startsWith(OAUTH_AUTHORIZATION_SERVER) && uri.getRawQuery() == null && uri.getRawFragment() == null) {
            String issuer = uri.getScheme() + "://" + uri.getRawAuthority();
            return StringUtil.removeSuffix(issuer + path.substring(OAUTH_AUTHORIZATION_SERVER.length()), "/");
        }
        return null;
    }

    /**
     * Config keys that determine where/how a client secret is sent. Includes {@code baseUrl},
     * which some social providers (for example GitHub/OpenShift) use to derive the token
     * endpoint at runtime. Provider-specific configs may override to add keys such as
     * {@code tenantId} or {@code sandbox}.
     */
    protected String[] getClientSecretDestinationConfigKeys() {
        return new String[] {
                TOKEN_ENDPOINT_URL,
                TOKEN_INTROSPECTION_URL,
                PAR_URL,
                "clientId",
                "baseUrl"
        };
    }

    /**
     * Reuse a masked {@code clientSecret} only when fields that determine where/how the secret
     * is sent are unchanged. Otherwise a delegated IdP manager could rebind the stored secret
     * to an attacker-controlled token endpoint.
     */
    @Override
    public boolean canReuseMaskedClientSecret(IdentityProviderModel other) {
        Map<String, String> thisConfig = getConfig() != null ? getConfig() : Map.of();
        Map<String, String> otherConfig = other.getConfig() != null ? other.getConfig() : Map.of();

        for (String key : getClientSecretDestinationConfigKeys()) {
            if (isClientSecretDestinationChanged(key, otherConfig.get(key), thisConfig.get(key))) {
                return false;
            }
        }
        return Objects.equals(
                clientAuthMethod(thisConfig.get("clientAuthMethod")),
                clientAuthMethod(otherConfig.get("clientAuthMethod")));
    }

    /**
     * An absent or empty {@code clientAuthMethod} behaves as {@code client_secret_post} at runtime
     * (see {@code AbstractOAuth2IdentityProvider#authenticateTokenRequest}).
     */
    private static String clientAuthMethod(String value) {
        return value == null || value.isEmpty() ? OIDCLoginProtocol.CLIENT_SECRET_POST : value;
    }

    /**
     * Decides whether a destination config value changed in a way that affects where/how the
     * client secret is sent. By default {@code null} and {@code ""} are treated as equivalent, as
     * UI/API clients may send {@code ""} for omitted optional fields; any other difference is a
     * change, matching the raw values used at runtime. Sub-classes may override to apply
     * key-specific semantics such as boolean or default-value equivalence.
     *
     * @param key the config key being compared
     * @param stored the currently stored value, possibly {@code null}
     * @param updated the submitted value, possibly {@code null}
     * @return {@code true} if the values represent different destinations
     */
    protected boolean isClientSecretDestinationChanged(String key, String stored, String updated) {
        return !Objects.equals(normalize(stored), normalize(updated));
    }

    private static String normalize(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
