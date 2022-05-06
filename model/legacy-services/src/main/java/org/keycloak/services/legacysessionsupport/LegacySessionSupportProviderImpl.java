package org.keycloak.services.legacysessionsupport;

import org.keycloak.credential.UserCredentialStoreManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.LegacySessionSupportProvider;

/**
 * @author Alexander Schwartz
 */
public class LegacySessionSupportProviderImpl implements LegacySessionSupportProvider {

    private final KeycloakSession session;

    public LegacySessionSupportProviderImpl(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {

    }

    @Override
    @Deprecated
    public UserCredentialManager userCredentialManager() {
        // UserCacheSession calls session.userCredentialManager().onCache(), therefore can't trigger a warning here at the moment.
        return new UserCredentialStoreManager(session);
    }

}
