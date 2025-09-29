package org.keycloak.protocol.oidc.spi;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderFactory;

public interface TokenManagerProviderFactory extends ProviderFactory<TokenManagerProvider> {
    @Override TokenManagerProvider create(KeycloakSession session);
}
