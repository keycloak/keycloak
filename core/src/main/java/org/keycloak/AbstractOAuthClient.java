package org.keycloak;

import org.keycloak.enums.RelativeUrlsUsed;
import org.keycloak.util.KeycloakUriBuilder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AbstractOAuthClient {
    private static final String OAUTH_TOKEN_REQUEST_STATE = "OAuth_Token_Request_State";
    private final AtomicLong counter = new AtomicLong();

    protected String clientId;
    protected Map<String, String> credentials;
    protected String authUrl;
    protected String tokenUrl;
    protected RelativeUrlsUsed relativeUrlsUsed;
    protected String scope;
    protected String stateCookieName = OAUTH_TOKEN_REQUEST_STATE;
    protected String stateCookiePath;
    protected boolean isSecure;
    protected boolean publicClient;
    protected String getStateCode() {
        return counter.getAndIncrement() + "/" + UUID.randomUUID().toString();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Map<String, String> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, String> credentials) {
        this.credentials = credentials;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getStateCookieName() {
        return stateCookieName;
    }

    public void setStateCookieName(String stateCookieName) {
        this.stateCookieName = stateCookieName;
    }

    public String getStateCookiePath() {
        return stateCookiePath;
    }

    public void setStateCookiePath(String stateCookiePath) {
        this.stateCookiePath = stateCookiePath;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public void setPublicClient(boolean publicClient) {
        this.publicClient = publicClient;
    }

    public RelativeUrlsUsed getRelativeUrlsUsed() {
        return relativeUrlsUsed;
    }

    public void setRelativeUrlsUsed(RelativeUrlsUsed relativeUrlsUsed) {
        this.relativeUrlsUsed = relativeUrlsUsed;
    }

    protected String stripOauthParametersFromRedirect(String uri) {
        KeycloakUriBuilder builder = KeycloakUriBuilder.fromUri(uri)
                .replaceQueryParam(OAuth2Constants.CODE, null)
                .replaceQueryParam(OAuth2Constants.STATE, null);
        return builder.build().toString();
    }

}
