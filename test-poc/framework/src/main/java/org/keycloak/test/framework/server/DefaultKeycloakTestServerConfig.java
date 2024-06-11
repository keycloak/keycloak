package org.keycloak.test.framework.server;

import java.util.Optional;

public class DefaultKeycloakTestServerConfig implements KeycloakTestServerConfig {
    @Override
    public Optional<String> adminUserName() { return Optional.of("admin"); }

    @Override
    public Optional<String> adminUserPassword() { return Optional.of("admin"); }
}
