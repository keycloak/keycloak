package org.keycloak.protocol.oidc.verifier;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class TokenVerifierSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "oauth2-token-verifier";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return TokenVerifierProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return TokenVerifierProviderFactory.class;
    }
}
