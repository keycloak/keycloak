package org.keycloak.tests.authz.services.config;

import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;

public class DefaultResourceServerConfig implements ClientConfig {

    @Override
    public ClientBuilder configure(ClientBuilder client) {
        return client
                .clientId(KeycloakModelUtils.generateId())
                .secret("secret")
                .authorizationServicesEnabled(true)
                .directAccessGrantsEnabled(true);
    }
}
