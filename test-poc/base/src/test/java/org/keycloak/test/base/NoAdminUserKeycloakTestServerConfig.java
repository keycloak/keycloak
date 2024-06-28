package org.keycloak.test.base;

import org.keycloak.test.framework.server.KeycloakTestServerConfig;

import java.util.Optional;

public class NoAdminUserKeycloakTestServerConfig implements KeycloakTestServerConfig {

    @Override
    public Optional<String> adminUserName() {
        return Optional.empty();
    }

    @Override
    public Optional<String> adminUserPassword() {
        return Optional.empty();
    }

}
