package org.keycloak.testframework.tests.providers.single;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ProviderAndSpiSpi implements Spi {

    final String ID = "provider-and-spi-spi";

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return ID;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ProviderAndSpiProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ProviderAndSpiProviderFactory.class;
    }
}
