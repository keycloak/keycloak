package org.keycloak.jose.jws;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.TokenManager;
import org.keycloak.models.TokenManagerFactory;

/**
 * Default factory that creates the built-in DefaultTokenManager.
 */
public class DefaultTokenManagerFactory implements TokenManagerFactory {

    @Override
    public TokenManager create(KeycloakSession session) {
        return new DefaultTokenManager(session);
    }

    @Override
    public void init(Config.Scope config) {}

    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    @Override
    public void close() {}

    @Override
    public String getId() {
        return "default-token-manager";
    }
}
