package org.keycloak.admin.api;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class AdminApiSpi implements Spi {
    public static final String PROVIDER_ID = "admin-api-root";

    @Override
    public String getName() {
        return PROVIDER_ID;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return AdminApi.class;
    }

    @Override
    public Class<? extends ProviderFactory<AdminApi>> getProviderFactoryClass() {
        return AdminApiFactory.class;
    }

    @Override
    public boolean isInternal() {
        return true;
    }
}
