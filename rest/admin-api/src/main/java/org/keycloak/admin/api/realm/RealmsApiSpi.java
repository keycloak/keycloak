package org.keycloak.admin.api.realm;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class RealmsApiSpi implements Spi {
    public static final String NAME = "admin-api-realms";

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
        return RealmsApi.class;
    }

    @Override
    public Class<? extends ProviderFactory<RealmsApi>> getProviderFactoryClass() {
        return RealmsApiFactory.class;
    }
}
