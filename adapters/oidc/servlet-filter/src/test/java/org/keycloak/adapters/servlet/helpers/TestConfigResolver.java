package org.keycloak.adapters.servlet.helpers;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.spi.HttpFacade;

public class TestConfigResolver implements KeycloakConfigResolver {
    @Override
    public KeycloakDeployment resolve(HttpFacade.Request facade) {
        return null;
    }
}
