package org.keycloak.protocol.oidc.refresh;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class RefreshTokenSpi implements Spi {

    public static final String SPI_NAME = "refresh-token";

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return SPI_NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return RefreshTokenProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return RefreshTokenProviderFactory.class;
    }
}
