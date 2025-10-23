package org.keycloak.admin.api;

import org.keycloak.common.Profile;
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

    @Override
    public boolean isEnabled() {
        return Profile.isFeatureEnabled(Profile.Feature.CLIENT_ADMIN_API_V2); // There's currently only Client API for the new Admin API v2
    }
}
