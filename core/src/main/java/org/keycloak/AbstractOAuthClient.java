package org.keycloak;

import org.keycloak.util.KeycloakUriBuilder;

import java.security.KeyStore;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AbstractOAuthClient {
    public static final String OAUTH_TOKEN_REQUEST_STATE = "OAuth_Token_Request_State";
    protected String clientId;
    protected String password;
    protected KeyStore truststore;
    protected String authUrl;
    protected String codeUrl;
    protected String scope;
    protected String stateCookieName = OAUTH_TOKEN_REQUEST_STATE;
    protected String stateCookiePath;
    protected boolean isSecure;
    protected final AtomicLong counter = new AtomicLong();

    protected String getStateCode() {
        return counter.getAndIncrement() + "/" + UUID.randomUUID().toString();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public KeyStore getTruststore() {
        return truststore;
    }

    public void setTruststore(KeyStore truststore) {
        this.truststore = truststore;
    }

    public String getAuthUrl() {
        return authUrl;
    }



    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getCodeUrl() {
        return codeUrl;
    }

    public void setCodeUrl(String codeUrl) {
        this.codeUrl = codeUrl;
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

    protected String stripOauthParametersFromRedirect(String uri) {
        KeycloakUriBuilder builder = KeycloakUriBuilder.fromUri(uri)
                .replaceQueryParam("code", null)
                .replaceQueryParam("state", null);
        return builder.build().toString();
    }

}
