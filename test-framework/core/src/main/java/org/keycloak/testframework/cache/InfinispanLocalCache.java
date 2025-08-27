package org.keycloak.testframework.cache;

public class InfinispanLocalCache implements InfinispanCache{

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Override
    public String getServerUrl() {
        return null;
    }

    @Override
    public String getCacheName() {
        return "local";
    }
}
