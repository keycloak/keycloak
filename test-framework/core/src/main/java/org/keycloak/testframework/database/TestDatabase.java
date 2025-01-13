package org.keycloak.testframework.database;

import java.util.Map;

public interface TestDatabase {

    void start();

    void stop();

    Map<String, String> serverConfig();

}
