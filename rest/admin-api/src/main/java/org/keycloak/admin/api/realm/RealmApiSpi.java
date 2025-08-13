package org.keycloak.admin.api.realm;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class RealmApiSpi implements Spi {
    public static final String NAME = "admin-api-realm";

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
        return RealmApi.class;
    }

    @Override
    public Class<? extends ProviderFactory<RealmApi>> getProviderFactoryClass() {
        return RealmApiFactory.class;
    }
}
