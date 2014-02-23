package org.keycloak.adapters.undertow;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.util.StatusCodes;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.adapters.ResourceMetadata;
import org.keycloak.adapters.TokenGrantRequest;
import org.keycloak.adapters.config.RealmConfiguration;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.adapters.config.AdapterConfig;

import java.io.IOException;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
class KeycloakIdentityManager implements IdentityManager {
    protected static Logger log = Logger.getLogger(KeycloakIdentityManager.class);

    @Override
    public Account verify(Account account) {
        log.info("Verifying account in IdentityManager");
        KeycloakUndertowAccount keycloakAccount = (KeycloakUndertowAccount)account;
        if (keycloakAccount.getAccessToken().isActive()) {
            log.info("account is still active.  Time left: " + (keycloakAccount.getAccessToken().getExpiration() - (System.currentTimeMillis()/1000)) );
            return account;
        }
        keycloakAccount.refreshExpiredToken();
        if (!keycloakAccount.getAccessToken().isActive()) return null;
        return account;
    }

    @Override
    public Account verify(String id, Credential credential) {
        KeycloakServletExtension.log.warn("Shouldn't call verify!!!");
        throw new IllegalStateException("Not allowed");
    }

    @Override
    public Account verify(Credential credential) {
        KeycloakServletExtension.log.warn("Shouldn't call verify!!!");
        throw new IllegalStateException("Not allowed");
    }
}
