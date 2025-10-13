package org.keycloak.services;

import org.keycloak.common.Profile;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class KeycloakServicesSpi implements Spi {
    public static final String NAME = "keycloak-services";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends KeycloakServices> getProviderClass() {
        return KeycloakServices.class;
    }

    @Override
    public Class<? extends ProviderFactory<KeycloakServices>> getProviderFactoryClass() {
        return KeycloakServicesFactory.class;
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Currently used only by Client Admin API v2
        return Profile.isFeatureEnabled(Profile.Feature.CLIENT_ADMIN_API_V2);
    }
}
