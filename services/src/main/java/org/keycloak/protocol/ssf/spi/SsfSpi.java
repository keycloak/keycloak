package org.keycloak.protocol.ssf.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.Spi;

/**
 * SPI for SSF (Shared Signals Framework) support.
 */
public class SsfSpi implements Spi {

    @Override
    public String getName() {
        return "ssf";
    }

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return SsfProvider.class;
    }

    @Override
    public Class<? extends SsfProviderFactory> getProviderFactoryClass() {
        return SsfProviderFactory.class;
    }
}
