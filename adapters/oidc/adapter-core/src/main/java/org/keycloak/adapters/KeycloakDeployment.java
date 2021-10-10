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

package org.keycloak.adapters;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.adapters.authentication.ClientCredentialsProvider;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.adapters.rotation.PublicKeyLocator;
import org.keycloak.common.enums.RelativeUrlsUsed;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.enums.TokenStore;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.util.JsonSerialization;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:brad.culley@spartasystems.com">Brad Culley</a>
 * @author <a href="mailto:john.ament@spartasystems.com">John D. Ament</a>
 * @version $Revision: 1 $
 */
public class KeycloakDeployment {

    private static final Logger log = Logger.getLogger(KeycloakDeployment.class);

    protected RelativeUrlsUsed relativeUrls;
    protected String realm;
    protected PublicKeyLocator publicKeyLocator;
    protected String authServerBaseUrl;
    protected String realmInfoUrl;
    protected KeycloakUriBuilder authUrl;
    protected String tokenUrl;
    protected KeycloakUriBuilder logoutUrl;
    protected String accountUrl;
    protected String registerNodeUrl;
    protected String unregisterNodeUrl;
    protected String jwksUrl;
    protected String principalAttribute = "sub";

    protected String resourceName;
    protected boolean bearerOnly;
    protected boolean autodetectBearerOnly;
    protected boolean enableBasicAuth;
    protected boolean publicClient;
    protected Map<String, Object> resourceCredentials = new HashMap<>();
    protected ClientCredentialsProvider clientAuthenticator;
    protected Callable<HttpClient> client;

    protected String scope;
    protected SslRequired sslRequired = SslRequired.ALL;
    protected int confidentialPort = -1;
    protected TokenStore tokenStore = TokenStore.SESSION;
    protected String adapterStateCookiePath = "";
    protected String stateCookieName = "OAuth_Token_Request_State";
    protected boolean useResourceRoleMappings;
    protected boolean cors;
    protected int corsMaxAge = -1;
    protected String corsAllowedHeaders;
    protected String corsAllowedMethods;
    protected String corsExposedHeaders;
    protected boolean exposeToken;
    protected boolean alwaysRefreshToken;
    protected boolean registerNodeAtStartup;
    protected int registerNodePeriod;
    protected boolean turnOffChangeSessionIdOnLogin;

    protected volatile int notBefore;
    protected int tokenMinimumTimeToLive;
    protected int minTimeBetweenJwksRequests;
    protected int publicKeyCacheTtl;
    protected Callable<PolicyEnforcer> policyEnforcer;

    // https://tools.ietf.org/html/rfc7636
    protected boolean pkce = false;
    protected boolean ignoreOAuthQueryParameter;

    protected Map<String, String> redirectRewriteRules;

    protected boolean delegateBearerErrorResponseSending = false;
    protected boolean verifyTokenAudience = false;

    public KeycloakDeployment() {
    }

    public boolean isConfigured() {
        return getRealm() != null && getPublicKeyLocator() != null && (isBearerOnly() || getAuthServerBaseUrl() != null);
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public PublicKeyLocator getPublicKeyLocator() {
        return publicKeyLocator;
    }

    public void setPublicKeyLocator(PublicKeyLocator publicKeyLocator) {
        this.publicKeyLocator = publicKeyLocator;
    }

    public String getAuthServerBaseUrl() {
        return authServerBaseUrl;
    }

    public void setAuthServerBaseUrl(AdapterConfig config) {
        this.authServerBaseUrl = config.getAuthServerUrl();
        if (authServerBaseUrl == null) return;

        authServerBaseUrl = KeycloakUriBuilder.fromUri(authServerBaseUrl).build().toString();

        authUrl = null;
        realmInfoUrl = null;
        tokenUrl = null;
        logoutUrl = null;
        accountUrl = null;
        registerNodeUrl = null;
        unregisterNodeUrl = null;
        jwksUrl = null;

        URI authServerUri = URI.create(authServerBaseUrl);

        if (authServerUri.getHost() == null) {
            relativeUrls = RelativeUrlsUsed.ALWAYS;
        } else {
            // We have absolute URI in config
            relativeUrls = RelativeUrlsUsed.NEVER;
        }
    }

    /**
     * URLs are loaded lazily when used. This allows adapter to be deployed prior to Keycloak server starting, and will
     * also allow the adapter to retry loading config for each request until the Keycloak server is ready.
     *
     * In the future we may want to support reloading config at a configurable interval.
     */
    protected void resolveUrls() {
        if (realmInfoUrl == null) {
            synchronized (this) {
                if (realmInfoUrl == null) {
                    KeycloakUriBuilder authUrlBuilder = KeycloakUriBuilder
                        .fromUri(authServerBaseUrl);

                    String discoveryUrl = authUrlBuilder.clone()
                        .path(ServiceUrlConstants.DISCOVERY_URL).build(getRealm()).toString();
                    try {
                        log.debugv("Resolving URLs from {0}", discoveryUrl);

                        OIDCConfigurationRepresentation config = getOidcConfiguration(discoveryUrl);

                        authUrl = KeycloakUriBuilder.fromUri(config.getAuthorizationEndpoint());
                        realmInfoUrl = config.getIssuer();

                        tokenUrl = config.getTokenEndpoint();
                        logoutUrl = KeycloakUriBuilder.fromUri(config.getLogoutEndpoint());
                        accountUrl = KeycloakUriBuilder.fromUri(config.getIssuer()).path("/account")
                            .build().toString();
                        registerNodeUrl = authUrlBuilder.clone()
                            .path(ServiceUrlConstants.CLIENTS_MANAGEMENT_REGISTER_NODE_PATH)
                            .build(getRealm()).toString();
                        unregisterNodeUrl = authUrlBuilder.clone()
                            .path(ServiceUrlConstants.CLIENTS_MANAGEMENT_UNREGISTER_NODE_PATH)
                            .build(getRealm()).toString();
                        jwksUrl = config.getJwksUri();

                        log.infov("Loaded URLs from {0}", discoveryUrl);
                    } catch (Exception e) {
                        log.warnv(e, "Failed to load URLs from {0}", discoveryUrl);
                    }
                }
            }
        }
    }

    protected void resolveUrls(KeycloakUriBuilder authUrlBuilder) {
        if (log.isDebugEnabled()) {
            log.debug("resolveUrls");
        }

        String login = authUrlBuilder.clone().path(ServiceUrlConstants.AUTH_PATH).build(getRealm()).toString();
        authUrl = KeycloakUriBuilder.fromUri(login);
        realmInfoUrl = authUrlBuilder.clone().path(ServiceUrlConstants.REALM_INFO_PATH).build(getRealm()).toString();

        tokenUrl = authUrlBuilder.clone().path(ServiceUrlConstants.TOKEN_PATH).build(getRealm()).toString();
        logoutUrl = KeycloakUriBuilder.fromUri(authUrlBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH).build(getRealm()).toString());
        accountUrl = authUrlBuilder.clone().path(ServiceUrlConstants.ACCOUNT_SERVICE_PATH).build(getRealm()).toString();
        registerNodeUrl = authUrlBuilder.clone().path(ServiceUrlConstants.CLIENTS_MANAGEMENT_REGISTER_NODE_PATH).build(getRealm()).toString();
        unregisterNodeUrl = authUrlBuilder.clone().path(ServiceUrlConstants.CLIENTS_MANAGEMENT_UNREGISTER_NODE_PATH).build(getRealm()).toString();
        jwksUrl = authUrlBuilder.clone().path(ServiceUrlConstants.JWKS_URL).build(getRealm()).toString();
    }

    protected OIDCConfigurationRepresentation getOidcConfiguration(String discoveryUrl) throws Exception {
        HttpGet request = new HttpGet(discoveryUrl);
        request.addHeader("accept", "application/json");

        try {
            HttpResponse response = getClient().execute(request);
            if (response.getStatusLine().getStatusCode() != 200) {
                EntityUtils.consumeQuietly(response.getEntity());
                throw new Exception(response.getStatusLine().getReasonPhrase());
            }
            return JsonSerialization.readValue(response.getEntity().getContent(), OIDCConfigurationRepresentation.class);
        } finally {
            request.releaseConnection();
        }
    }

    public RelativeUrlsUsed getRelativeUrls() {
        return relativeUrls;
    }

    public String getRealmInfoUrl() {
        resolveUrls();
        return realmInfoUrl;
    }

    public KeycloakUriBuilder getAuthUrl() {
        resolveUrls();
        return authUrl;
    }

    public String getTokenUrl() {
        resolveUrls();
        return tokenUrl;
    }

    public KeycloakUriBuilder getLogoutUrl() {
        resolveUrls();
        return logoutUrl;
    }

    public String getAccountUrl() {
        resolveUrls();
        return accountUrl;
    }

    public String getRegisterNodeUrl() {
        resolveUrls();
        return registerNodeUrl;
    }

    public String getUnregisterNodeUrl() {
        resolveUrls();
        return unregisterNodeUrl;
    }

    public String getJwksUrl() {
        resolveUrls();
        return jwksUrl;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public boolean isBearerOnly() {
        return bearerOnly;
    }

    public void setBearerOnly(boolean bearerOnly) {
        this.bearerOnly = bearerOnly;
    }

    public boolean isAutodetectBearerOnly() {
        return autodetectBearerOnly;
    }

    public void setAutodetectBearerOnly(boolean autodetectBearerOnly) {
        this.autodetectBearerOnly = autodetectBearerOnly;
    }

    public boolean isEnableBasicAuth() {
        return enableBasicAuth;
    }

    public void setEnableBasicAuth(boolean enableBasicAuth) {
        this.enableBasicAuth = enableBasicAuth;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public void setPublicClient(boolean publicClient) {
        this.publicClient = publicClient;
    }

    public Map<String, Object> getResourceCredentials() {
        return resourceCredentials;
    }

    public void setResourceCredentials(Map<String, Object> resourceCredentials) {
        this.resourceCredentials = resourceCredentials;
    }

    public ClientCredentialsProvider getClientAuthenticator() {
        return clientAuthenticator;
    }

    public void setClientAuthenticator(ClientCredentialsProvider clientAuthenticator) {
        this.clientAuthenticator = clientAuthenticator;
    }

    public HttpClient getClient() {
        try {
            return client.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setClient(final HttpClient client) {
        this.client = new Callable<HttpClient>() {
            @Override
            public HttpClient call() {
                return client;
            }
        };
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public SslRequired getSslRequired() {
        return sslRequired;
    }

    public void setSslRequired(SslRequired sslRequired) {
        this.sslRequired = sslRequired;
    }

    public boolean isSSLEnabled() {
        if (SslRequired.NONE == sslRequired) {
            return false;
        }
        return true;
    }

    public int getConfidentialPort() {
        return confidentialPort;
    }

    public void setConfidentialPort(int confidentialPort) {
        this.confidentialPort = confidentialPort;
    }

    public TokenStore getTokenStore() {
        return tokenStore;
    }

    public void setTokenStore(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public String getAdapterStateCookiePath() {
        return adapterStateCookiePath;
    }

    public void setAdapterStateCookiePath(String adapterStateCookiePath) {
        this.adapterStateCookiePath = adapterStateCookiePath;
    }

    public String getStateCookieName() {
        return stateCookieName;
    }

    public void setStateCookieName(String stateCookieName) {
        this.stateCookieName = stateCookieName;
    }

    public boolean isUseResourceRoleMappings() {
        return useResourceRoleMappings;
    }

    public void setUseResourceRoleMappings(boolean useResourceRoleMappings) {
        this.useResourceRoleMappings = useResourceRoleMappings;
    }

    public boolean isCors() {
        return cors;
    }

    public void setCors(boolean cors) {
        this.cors = cors;
    }

    public int getCorsMaxAge() {
        return corsMaxAge;
    }

    public void setCorsMaxAge(int corsMaxAge) {
        this.corsMaxAge = corsMaxAge;
    }

    public String getCorsAllowedHeaders() {
        return corsAllowedHeaders;
    }

    public void setCorsAllowedHeaders(String corsAllowedHeaders) {
        this.corsAllowedHeaders = corsAllowedHeaders;
    }

    public String getCorsAllowedMethods() {
        return corsAllowedMethods;
    }

    public void setCorsAllowedMethods(String corsAllowedMethods) {
        this.corsAllowedMethods = corsAllowedMethods;
    }

    public String getCorsExposedHeaders() {
        return corsExposedHeaders;
    }

    public void setCorsExposedHeaders(String corsExposedHeaders) {
        this.corsExposedHeaders = corsExposedHeaders;
    }

    public boolean isExposeToken() {
        return exposeToken;
    }

    public void setExposeToken(boolean exposeToken) {
        this.exposeToken = exposeToken;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
    }

    public void updateNotBefore(int notBefore) {
        this.notBefore = notBefore;
        getPublicKeyLocator().reset(this);
    }

    public boolean isAlwaysRefreshToken() {
        return alwaysRefreshToken;
    }

    public void setAlwaysRefreshToken(boolean alwaysRefreshToken) {
        this.alwaysRefreshToken = alwaysRefreshToken;
    }

    public boolean isRegisterNodeAtStartup() {
        return registerNodeAtStartup;
    }

    public void setRegisterNodeAtStartup(boolean registerNodeAtStartup) {
        this.registerNodeAtStartup = registerNodeAtStartup;
    }

    public int getRegisterNodePeriod() {
        return registerNodePeriod;
    }

    public void setRegisterNodePeriod(int registerNodePeriod) {
        this.registerNodePeriod = registerNodePeriod;
    }

    public String getPrincipalAttribute() {
        return principalAttribute;
    }

    public void setPrincipalAttribute(String principalAttribute) {
        this.principalAttribute = principalAttribute;
    }

    public boolean isTurnOffChangeSessionIdOnLogin() {
        return turnOffChangeSessionIdOnLogin;
    }

    public void setTurnOffChangeSessionIdOnLogin(boolean turnOffChangeSessionIdOnLogin) {
        this.turnOffChangeSessionIdOnLogin = turnOffChangeSessionIdOnLogin;
    }

    public int getTokenMinimumTimeToLive() {
        return tokenMinimumTimeToLive;
    }

    public void setTokenMinimumTimeToLive(final int tokenMinimumTimeToLive) {
        this.tokenMinimumTimeToLive = tokenMinimumTimeToLive;
    }

    public int getMinTimeBetweenJwksRequests() {
        return minTimeBetweenJwksRequests;
    }

    public void setMinTimeBetweenJwksRequests(int minTimeBetweenJwksRequests) {
        this.minTimeBetweenJwksRequests = minTimeBetweenJwksRequests;
    }

    public int getPublicKeyCacheTtl() {
        return publicKeyCacheTtl;
    }

    public void setPublicKeyCacheTtl(int publicKeyCacheTtl) {
        this.publicKeyCacheTtl = publicKeyCacheTtl;
    }

    public void setPolicyEnforcer(Callable<PolicyEnforcer> policyEnforcer) {
        this.policyEnforcer = policyEnforcer;
    }

    public PolicyEnforcer getPolicyEnforcer() {
        if (policyEnforcer == null) {
            return null;
        }
        try {
            return policyEnforcer.call();
        } catch (Exception cause) {
            throw new RuntimeException("Failed to obtain policy enforcer", cause);
        }
    }

    // https://tools.ietf.org/html/rfc7636
    public boolean isPkce() {
        return pkce;
    }

    public void setPkce(boolean pkce) {
        this.pkce = pkce;
    }

    public void setIgnoreOAuthQueryParameter(boolean ignoreOAuthQueryParameter) {
        this.ignoreOAuthQueryParameter = ignoreOAuthQueryParameter;
    }

    public boolean isOAuthQueryParameterEnabled() {
        return !this.ignoreOAuthQueryParameter;
    }

    public Map<String, String> getRedirectRewriteRules() {
        return redirectRewriteRules;
    }

    public void setRewriteRedirectRules(Map<String, String> redirectRewriteRules) {
        this.redirectRewriteRules = redirectRewriteRules;
    }

    public boolean isDelegateBearerErrorResponseSending() {
        return delegateBearerErrorResponseSending;
    }

    public void setDelegateBearerErrorResponseSending(boolean delegateBearerErrorResponseSending) {
        this.delegateBearerErrorResponseSending = delegateBearerErrorResponseSending;
    }

    public boolean isVerifyTokenAudience() {
        return verifyTokenAudience;
    }

    public void setVerifyTokenAudience(boolean verifyTokenAudience) {
        this.verifyTokenAudience = verifyTokenAudience;
    }

    public void setClient(Callable<HttpClient> callable) {
        client = callable;
    }
}
