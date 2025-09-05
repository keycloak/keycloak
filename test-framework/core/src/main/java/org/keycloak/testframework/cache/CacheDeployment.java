package org.keycloak.testframework.cache;

public interface CacheDeployment {

    void start();

    void stop();

    String getServerUrl();
}
