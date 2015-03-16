package org.keycloak.adapters;

import org.apache.http.client.HttpClient;
import org.jboss.logging.Logger;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.enums.RelativeUrlsUsed;
import org.keycloak.enums.SslRequired;
import org.keycloak.enums.TokenStore;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.util.KeycloakUriBuilder;

import java.net.URI;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakDeployment {

    private static final Logger log = Logger.getLogger(KeycloakDeployment.class);

    protected RelativeUrlsUsed relativeUrls;
    protected String realm;
    protected volatile PublicKey realmKey;
    protected String authServerBaseUrl;
    protected String realmInfoUrl;
    protected KeycloakUriBuilder authUrl;
    protected String tokenUrl;
    protected KeycloakUriBuilder logoutUrl;
    protected String accountUrl;
    protected String registerNodeUrl;
    protected String unregisterNodeUrl;
    protected String principalAttribute = "sub";

    protected String resourceName;
    protected boolean bearerOnly;
    protected boolean enableBasicAuth;
    protected boolean publicClient;
    protected Map<String, String> resourceCredentials = new HashMap<String, String>();
    protected HttpClient client;

    protected String scope;
    protected SslRequired sslRequired = SslRequired.ALL;
    protected TokenStore tokenStore = TokenStore.SESSION;
    protected String stateCookieName = "OAuth_Token_Request_State";
    protected boolean useResourceRoleMappings;
    protected boolean cors;
    protected int corsMaxAge = -1;
    protected String corsAllowedHeaders;
    protected String corsAllowedMethods;
    protected boolean exposeToken;
    protected boolean alwaysRefreshToken;
    protected boolean registerNodeAtStartup;
    protected int registerNodePeriod;
    protected volatile int notBefore;

    public KeycloakDeployment() {
    }

    public boolean isConfigured() {
        return getRealm() != null && getRealmKey() != null && (isBearerOnly() || getAuthServerBaseUrl() != null);
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

    public PublicKey getRealmKey() {
        return realmKey;
    }

    public void setRealmKey(PublicKey realmKey) {
        this.realmKey = realmKey;
    }

    public String getAuthServerBaseUrl() {
        return authServerBaseUrl;
    }

    public void setAuthServerBaseUrl(AdapterConfig config) {
        this.authServerBaseUrl = config.getAuthServerUrl();
        if (authServerBaseUrl == null && config.getAuthServerUrlForBackendRequests() == null) return;

        URI authServerUri = null;
        if (authServerBaseUrl != null) {
            authServerUri = URI.create(authServerBaseUrl);
        }

        if (authServerUri == null || authServerUri.getHost() == null) {
            String authServerURLForBackendReqs = config.getAuthServerUrlForBackendRequests();
            if (authServerURLForBackendReqs != null) {
                relativeUrls = RelativeUrlsUsed.BROWSER_ONLY;

                KeycloakUriBuilder serverBuilder = KeycloakUriBuilder.fromUri(authServerURLForBackendReqs);
                if (serverBuilder.getHost() == null || serverBuilder.getScheme() == null) {
                    throw new IllegalStateException("Relative URL not supported for auth-server-url-for-backend-requests option. URL used: "
                            + authServerURLForBackendReqs + ", Client: " + config.getResource());
                }
                resolveNonBrowserUrls(serverBuilder);
            } else {
                relativeUrls = RelativeUrlsUsed.ALL_REQUESTS;
            }
        } else {
            // We have absolute URI in config
            relativeUrls = RelativeUrlsUsed.NEVER;
            KeycloakUriBuilder serverBuilder = KeycloakUriBuilder.fromUri(authServerBaseUrl);
            resolveBrowserUrls(serverBuilder);
            resolveNonBrowserUrls(serverBuilder);
        }
    }



    /**
     * @param authUrlBuilder absolute URI
     */
    protected void resolveBrowserUrls(KeycloakUriBuilder authUrlBuilder) {
        if (log.isDebugEnabled()) {
            log.debug("resolveBrowserUrls");
        }

        String login = authUrlBuilder.clone().path(ServiceUrlConstants.AUTH_PATH).build(getRealm()).toString();
        authUrl = KeycloakUriBuilder.fromUri(login);
    }

    /**
     * @param authUrlBuilder absolute URI
     */
    protected void resolveNonBrowserUrls(KeycloakUriBuilder authUrlBuilder) {
        if (log.isDebugEnabled()) {
            log.debug("resolveNonBrowserUrls");
        }

        tokenUrl = authUrlBuilder.clone().path(ServiceUrlConstants.TOKEN_PATH).build(getRealm()).toString();
        logoutUrl = KeycloakUriBuilder.fromUri(authUrlBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH).build(getRealm()).toString());
        accountUrl = authUrlBuilder.clone().path(ServiceUrlConstants.ACCOUNT_SERVICE_PATH).build(getRealm()).toString();
        realmInfoUrl = authUrlBuilder.clone().path(ServiceUrlConstants.REALM_INFO_PATH).build(getRealm()).toString();
        registerNodeUrl = authUrlBuilder.clone().path(ServiceUrlConstants.CLIENTS_MANAGEMENT_REGISTER_NODE_PATH).build(getRealm()).toString();
        unregisterNodeUrl = authUrlBuilder.clone().path(ServiceUrlConstants.CLIENTS_MANAGEMENT_UNREGISTER_NODE_PATH).build(getRealm()).toString();
    }

    public RelativeUrlsUsed getRelativeUrls() {
        return relativeUrls;
    }

    public String getRealmInfoUrl() {
        return realmInfoUrl;
    }

    public KeycloakUriBuilder getAuthUrl() {
        return authUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public KeycloakUriBuilder getLogoutUrl() {
        return logoutUrl;
    }

    public String getAccountUrl() {
        return accountUrl;
    }

    public String getRegisterNodeUrl() {
        return registerNodeUrl;
    }

    public String getUnregisterNodeUrl() {
        return unregisterNodeUrl;
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

    public Map<String, String> getResourceCredentials() {
        return resourceCredentials;
    }

    public void setResourceCredentials(Map<String, String> resourceCredentials) {
        this.resourceCredentials = resourceCredentials;
    }

    public HttpClient getClient() {
        return client;
    }

    public void setClient(HttpClient client) {
        this.client = client;
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

    public TokenStore getTokenStore() {
        return tokenStore;
    }

    public void setTokenStore(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
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
}
