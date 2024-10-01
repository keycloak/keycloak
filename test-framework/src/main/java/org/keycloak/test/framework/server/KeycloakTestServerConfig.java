package org.keycloak.test.framework.server;

import org.keycloak.it.TestProvider;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.List;

public interface KeycloakTestServerConfig {

    default Map<String, String> options() {
        return Collections.emptyMap();
    }

    default Set<String> features() {
        return Collections.emptySet();
    }

    default boolean enableSysLog() { return false; }

    default List<Class<? extends TestProvider>> customProviders() {
        return Collections.emptyList();
    }
}
