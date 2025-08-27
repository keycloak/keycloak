package org.keycloak.testframework.cache;

public interface InfinispanCache {

    void start();

    void stop();

    String getServerUrl();

    String getCacheName();
}
