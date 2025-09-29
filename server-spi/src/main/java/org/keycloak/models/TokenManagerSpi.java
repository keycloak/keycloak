package org.keycloak.models;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class TokenManagerSpi implements Spi {
    @Override public String getName() { return "token-manager"; }
    @Override public Class<? extends Provider> getProviderClass() { return TokenManager.class; }
    @Override public Class<? extends ProviderFactory> getProviderFactoryClass() { return TokenManagerFactory.class; }
    @Override public boolean isInternal() { return false; }
}
