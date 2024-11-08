package org.keycloak.test.framework.database;

import io.quarkus.maven.dependency.Dependency;

import java.util.Map;

public interface TestDatabase {

    void start();

    void stop();

    Map<String, String> serverConfig();

    default Dependency jdbcDriver() {
        return null;
    }

}
