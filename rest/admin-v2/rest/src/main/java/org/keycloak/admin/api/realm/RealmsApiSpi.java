package org.keycloak.admin.api.realm;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

import static org.keycloak.admin.api.AdminRootV2.isAdminApiV2Enabled;

public class RealmsApiSpi implements Spi {
    public static final String NAME = "admin-api-realms";

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

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isAdminApiV2Enabled();
    }
}
