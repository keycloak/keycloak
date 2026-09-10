package org.keycloak.ssf.transmitter;

import org.keycloak.provider.Provider;
import org.keycloak.provider.Spi;

public class SsfTransmitterSpi implements Spi {

    @Override
    public String getName() {
        return "ssf-transmitter";
    }

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return SsfTransmitterProvider.class;
    }

    @Override
    public Class<? extends SsfTransmitterProviderFactory> getProviderFactoryClass() {
        return SsfTransmitterProviderFactory.class;
    }
}
