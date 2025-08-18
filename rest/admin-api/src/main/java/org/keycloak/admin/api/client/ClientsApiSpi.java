package org.keycloak.admin.api.client;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ClientsApiSpi implements Spi {
    public static final String NAME = "admin-api-clients";

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ClientsApi.class;
    }

    @Override
    public Class<? extends ProviderFactory<ClientsApi>> getProviderFactoryClass() {
        return ClientsApiFactory.class;
    }
}
