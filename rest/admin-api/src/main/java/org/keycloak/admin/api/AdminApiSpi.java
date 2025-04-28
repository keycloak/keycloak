package org.keycloak.admin.api;

import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class AdminApiSpi implements Spi {
    public static final String NAME = "admin-api";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends AdminApi> getProviderClass() {
        return AdminApi.class;
    }

    @Override
    public Class<? extends ProviderFactory<AdminApi>> getProviderFactoryClass() {
        return AdminApiFactory.class;
    }

    @Override
    public boolean isInternal() {
        return false;
    }
}
