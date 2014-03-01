package org.keycloak.adapters.undertow;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import org.jboss.logging.Logger;
import org.keycloak.adapters.config.RealmConfiguration;
import org.keycloak.representations.adapters.config.AdapterConfig;

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
        if (!keycloakAccount.isActive(realmConfiguration, adapterConfig)) {
            log.info("account.isActive() returned false, returning null");
            return null;
        }
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
