package org.keycloak.test.base;

import org.keycloak.test.framework.server.KeycloakTestServerConfig;

import java.util.Optional;

public class NoAdminUserKeycloakTestServerConfig implements KeycloakTestServerConfig {

    @Override
    public String adminUserName() {
        return null;
    }

    @Override
    public String adminUserPassword() {
        return null;
    }

}
