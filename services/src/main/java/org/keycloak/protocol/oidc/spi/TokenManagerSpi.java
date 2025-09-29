package org.keycloak.protocol.oidc.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class TokenManagerSpi implements Spi {
    @Override public boolean isInternal() { return false; }
    @Override public String getName() { return "oidc-token-manager"; }
    @Override public Class<? extends Provider> getProviderClass() { return TokenManagerProvider.class; }
    @Override public Class<? extends ProviderFactory> getProviderFactoryClass() { return TokenManagerProviderFactory.class; }
}
