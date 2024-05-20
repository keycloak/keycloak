package org.keycloak.test.framework.server.smallrye_config;

import io.smallrye.config.ConfigMapping;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ConfigMapping(prefix = "keycloak")
public interface KeycloakTestServerConfigMapping {

    Optional<String> server();

    Optional<Set<String>> features();

    Map<String, String> options();
}
