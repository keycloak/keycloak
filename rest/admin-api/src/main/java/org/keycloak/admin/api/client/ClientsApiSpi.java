package org.keycloak.admin.api.client;

import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ClientsApiSpi implements Spi {
    public static final String NAME = "clients-api";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends ClientsApi> getProviderClass() {
        return ClientsApi.class;
    }

    @Override
    public Class<? extends ProviderFactory<ClientsApi>> getProviderFactoryClass() {
        return ClientsApiFactory.class;
    }

    @Override
    public boolean isInternal() {
        return true;
    }
}
