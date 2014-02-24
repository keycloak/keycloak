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
    protected AdapterConfig adapterConfig;
    protected RealmConfiguration realmConfiguration;

    KeycloakIdentityManager(AdapterConfig adapterConfig, RealmConfiguration realmConfiguration) {
        this.adapterConfig = adapterConfig;
        this.realmConfiguration = realmConfiguration;
    }

    @Override
    public Account verify(Account account) {
        log.info("Verifying account in IdentityManager");
        KeycloakUndertowAccount keycloakAccount = (KeycloakUndertowAccount)account;
        if (!keycloakAccount.isActive(realmConfiguration, adapterConfig)) return null;
        return account;
    }

    @Override
    public Account verify(String id, Credential credential) {
        throw new IllegalStateException("Unsupported verify method");
    }

    @Override
    public Account verify(Credential credential) {
        throw new IllegalStateException("Unsupported verify method");
    }
}
