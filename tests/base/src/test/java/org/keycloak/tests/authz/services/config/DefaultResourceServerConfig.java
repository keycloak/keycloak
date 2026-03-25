package org.keycloak.tests.authz.services.config;

import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;

public class DefaultResourceServerConfig implements ClientConfig {

    @Override
    public ClientConfigBuilder configure(ClientConfigBuilder client) {
        return client
                .clientId(KeycloakModelUtils.generateId())
                .secret("secret")
                .authorizationServicesEnabled(true)
                .directAccessGrantsEnabled(true);
    }
}
