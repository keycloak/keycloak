package org.keycloak.adapters;

import org.apache.http.client.HttpClient;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.ServiceUrlConstants;
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

    protected boolean relativeUrls;
    protected String realm;
    protected PublicKey realmKey;
    protected KeycloakUriBuilder serverBuilder;
    protected String authServerBaseUrl;
    protected String realmInfoUrl;
    protected KeycloakUriBuilder authUrl;
    protected String codeUrl;
    protected String refreshUrl;
    protected KeycloakUriBuilder logoutUrl;
    protected String accountUrl;

    protected String resourceName;
    protected boolean bearerOnly;
    protected boolean publicClient;
    protected Map<String, String> resourceCredentials = new HashMap<String, String>();
    protected HttpClient client;

    protected String scope;
    protected boolean sslRequired = true;
    protected String stateCookieName = "OAuth_Token_Request_State";
    protected boolean useResourceRoleMappings;
    protected boolean cors;
    protected int corsMaxAge = -1;
    protected String corsAllowedHeaders;
    protected String corsAllowedMethods;
    protected boolean exposeToken;
    protected volatile int notBefore;

    public KeycloakDeployment() {
    }

    public boolean isConfigured() {
        return realm != null && realmKey != null && (bearerOnly || authServerBaseUrl != null);
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

    public void setAuthServerBaseUrl(String authServerBaseUrl) {
        this.authServerBaseUrl = authServerBaseUrl;
        if (authServerBaseUrl == null) return;

        URI uri = URI.create(authServerBaseUrl);
        if (uri.getHost() == null) {
            relativeUrls = true;
            return;
        }

        relativeUrls = false;

        serverBuilder = KeycloakUriBuilder.fromUri(authServerBaseUrl);
        String login = serverBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_LOGIN_PATH).build(getRealm()).toString();
        authUrl = KeycloakUriBuilder.fromUri(login);
        refreshUrl = serverBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_REFRESH_PATH).build(getRealm()).toString();
        logoutUrl = KeycloakUriBuilder.fromUri(serverBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH).build(getRealm()).toString());
        accountUrl = serverBuilder.clone().path(ServiceUrlConstants.ACCOUNT_SERVICE_PATH).build(getRealm()).toString();
        realmInfoUrl = serverBuilder.clone().path(ServiceUrlConstants.REALM_INFO_PATH).build(getRealm()).toString();
        codeUrl = serverBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_ACCESS_CODE_PATH).build(getRealm()).toString();
    }

    public String getRealmInfoUrl() {
        return realmInfoUrl;
    }

    public KeycloakUriBuilder getAuthUrl() {
        return authUrl;
    }

    public String getCodeUrl() {
        return codeUrl;
    }

    public String getRefreshUrl() {
        return refreshUrl;
    }

    public KeycloakUriBuilder getLogoutUrl() {
        return logoutUrl;
    }

    public String getAccountUrl() {
        return accountUrl;
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

    public boolean isSslRequired() {
        return sslRequired;
    }

    public void setSslRequired(boolean sslRequired) {
        this.sslRequired = sslRequired;
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

}
