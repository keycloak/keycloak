package org.keycloak.device;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class DeviceRepresentationSpi implements Spi {

    public static final String NAME = "deviceRepresentation";
    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return DeviceRepresentationProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return DeviceRepresentationProviderFactory.class;
    }

}
