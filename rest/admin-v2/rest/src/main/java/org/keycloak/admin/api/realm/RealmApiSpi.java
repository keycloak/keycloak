package org.keycloak.admin.api.realm;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

import static org.keycloak.admin.api.AdminRootV2.isAdminApiV2Enabled;

public class RealmApiSpi implements Spi {
    public static final String NAME = "admin-api-realm";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return RealmApi.class;
    }

    @Override
    public Class<? extends ProviderFactory<RealmApi>> getProviderFactoryClass() {
        return RealmApiFactory.class;
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
