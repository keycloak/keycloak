package org.keycloak.adapters.springboot;

import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.representations.adapters.config.AdapterConfig;

public class KeycloakSpringBootConfigResolver implements org.keycloak.adapters.KeycloakConfigResolver {

    private KeycloakDeployment keycloakDeployment;

    private static AdapterConfig adapterConfig;

    @Override
    public KeycloakDeployment resolve(OIDCHttpFacade.Request request) {
        if (keycloakDeployment != null) {
            return keycloakDeployment;
        }

        keycloakDeployment = KeycloakDeploymentBuilder.build(KeycloakSpringBootConfigResolver.adapterConfig);

        return keycloakDeployment;
    }

    static void setAdapterConfig(AdapterConfig adapterConfig) {
        KeycloakSpringBootConfigResolver.adapterConfig = adapterConfig;
    }
}