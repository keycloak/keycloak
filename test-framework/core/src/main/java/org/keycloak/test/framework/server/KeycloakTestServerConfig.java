package org.keycloak.test.framework.server;

import io.quarkus.maven.dependency.Dependency;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public interface KeycloakTestServerConfig {

    default Map<String, String> options() {
        return Collections.emptyMap();
    }

    default Set<String> features() {
        return Collections.emptySet();
    }

    default boolean enableSysLog() { return false; }

    default Set<Dependency> dependencies() {
        return Collections.emptySet();
    }

}
