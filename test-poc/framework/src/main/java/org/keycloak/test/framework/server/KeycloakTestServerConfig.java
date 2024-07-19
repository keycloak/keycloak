package org.keycloak.test.framework.server;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface KeycloakTestServerConfig {

    default Map<String, String> options() {
        return Collections.emptyMap();
    }

    default Set<String> features() {
        return Collections.emptySet();
    }

    default String adminUserName() {
        return "admin";
    }

    default String adminUserPassword() {
        return "admin";
    }

}
