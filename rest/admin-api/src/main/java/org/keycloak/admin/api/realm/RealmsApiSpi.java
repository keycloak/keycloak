package org.keycloak.admin.api.realm;

import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class RealmsApiSpi implements Spi {
    public static final String NAME = "realms-api";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends RealmsApi> getProviderClass() {
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
}
