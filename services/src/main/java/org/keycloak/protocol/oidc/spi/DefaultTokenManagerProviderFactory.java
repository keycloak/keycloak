package org.keycloak.protocol.oidc.spi;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oidc.TokenManager;
import org.jboss.logging.Logger;

public class DefaultTokenManagerProviderFactory implements TokenManagerProviderFactory {
    private static final Logger logger = Logger.getLogger(DefaultTokenManagerProviderFactory.class);

    @Override public TokenManagerProvider create(KeycloakSession session) {
        return new TokenManagerProvider() {
            private final TokenManager delegate = new org.keycloak.protocol.oidc.TokenManager();
            @Override public TokenManager get() { return delegate; }
        };
    }
    @Override public void init(Config.Scope config) {}
    @Override public void postInit(KeycloakSessionFactory factory) {}
    @Override public void close() {}
    @Override public String getId() { return "default"; }
}
