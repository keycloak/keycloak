package org.keycloak.services;

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
}
