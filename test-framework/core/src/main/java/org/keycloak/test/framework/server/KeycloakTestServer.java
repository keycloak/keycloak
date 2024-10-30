package org.keycloak.test.framework.server;

import io.quarkus.maven.dependency.Dependency;

import java.util.List;
import java.util.Set;

public interface KeycloakTestServer {

    void start(List<String> rawOptions, Set<Dependency> dependencies);

    void stop();

    String getBaseUrl();

}
