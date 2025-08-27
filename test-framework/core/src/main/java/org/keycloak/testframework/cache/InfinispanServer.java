package org.keycloak.testframework.cache;

import org.infinispan.server.test.core.InfinispanContainer;
import org.testcontainers.utility.DockerImageName;

public class InfinispanServer implements CacheDeployment {

    private final InfinispanContainer container;
    public static final String USER = "keycloak";
    public static final String PASSWORD = "Password1!";
    public static final String HOST = "127.0.0.1";
    private static final int PORT = 11222;

    public InfinispanServer() {
        container = new InfinispanContainer(DockerImageName.parse(CacheProperties.getContainerImageName()))
        .withUser(USER)
        .withPassword(PASSWORD);
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
        return container.getHost() + ":" + PORT;
    }
}
