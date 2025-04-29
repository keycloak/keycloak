package org.keycloak.services.client;

import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ClientServiceSpi implements Spi {
    public static final String NAME = "client-service";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends ClientService> getProviderClass() {
        return ClientService.class;
    }

    @Override
    public Class<? extends ProviderFactory<ClientService>> getProviderFactoryClass() {
        return ClientServiceFactory.class;
    }

    @Override
    public boolean isInternal() {
        return true;
    }
}
