package org.keycloak.admin.api.client;

import org.keycloak.common.Profile;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ClientApiSpi implements Spi {
    public static final String NAME = "admin-api-client";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ClientApi.class;
    }

    @Override
    public Class<? extends ProviderFactory<ClientApi>> getProviderFactoryClass() {
        return ClientApiFactory.class;
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Profile.isFeatureEnabled(Profile.Feature.CLIENT_ADMIN_API_V2);
    }
}
