package org.keycloak.testframework.cache;

import org.keycloak.it.junit5.extension.InfinispanContainer;

public class InfinispanExternalCache implements InfinispanCache {

    private final InfinispanContainer container;

    public InfinispanExternalCache() {
        container = new InfinispanContainer();
    }

    @Override
    public void start() {
        container.start();
    }

    @Override
    public void stop() {
        container.stop();
    }

    @Override
    public String getServerUrl() {
        return container.getHost() + ":" + container.getPort();
    }

    @Override
    public String getCacheName() {
        return "ispn";
    }
}
