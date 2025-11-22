package org.keycloak.services.error;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ErrorResponseHandlerSpi implements Spi {

    @Override
    public boolean isInternal() {
        return false; // Custom SPI, not internal Keycloak SPI
    }

    @Override
    public String getName() {
        return "errorResponseHandler"; // Unique SPI name
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Provider> getProviderClass() {
        return ErrorResponseHandlerProvider.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ErrorResponseHandlerProviderFactory.class;
    }
}
