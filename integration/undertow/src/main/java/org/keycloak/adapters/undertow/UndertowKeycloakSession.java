package org.keycloak.adapters.undertow;

import org.keycloak.KeycloakAuthenticatedSession;
import org.keycloak.adapters.ResourceMetadata;
import org.keycloak.representations.AccessToken;

import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UndertowKeycloakSession extends KeycloakAuthenticatedSession {

    private transient KeycloakUndertowAccount account;

    public UndertowKeycloakSession(KeycloakUndertowAccount account) {
        super(account.getEncodedAccessToken(), account.getAccessToken(), account.getResourceMetadata());
        this.account = account;
    }

    @Override
    public AccessToken getToken() {
        checkExpiration();
        return super.getToken();
    }

    private void checkExpiration() {
        if (token.isExpired() && account != null) {
            account.refreshExpiredToken();
            this.token = account.getAccessToken();
            this.tokenString = account.getEncodedAccessToken();

        }
    }

    @Override
    public String getTokenString() {
        checkExpiration();
        return super.getTokenString();
    }

}
