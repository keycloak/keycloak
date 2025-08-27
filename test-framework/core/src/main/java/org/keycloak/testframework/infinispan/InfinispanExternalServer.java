package org.keycloak.testframework.infinispan;

import org.infinispan.server.test.core.InfinispanContainer;
import org.keycloak.testframework.util.JavaPropertiesUtil;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

public class InfinispanExternalServer extends InfinispanContainer implements InfinispanServer {

    private final InfinispanContainer container;
    private static final String USER = "keycloak";
    private static final String PASSWORD = "Password1!";
    private static final String HOST = "127.0.0.1";

    public InfinispanExternalServer() {
        String containerName = JavaPropertiesUtil.getContainerImageName("infinispan-server.properties", "infinispan");

        container = new InfinispanContainer(DockerImageName.parse(containerName))
            .withUser(USER)
            .withPassword(PASSWORD);

        /*// Keycloak expects Infinispan to run on fixed ports
        getExposedPorts().forEach(i -> {
            addFixedExposedPort(i, i);
        });*/
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
    public Map<String, String> serverConfig() {
        return Map.of(
                "cache-remote-host", HOST,
                "cache-remote-username", USER,
                "cache-remote-password", PASSWORD,
                "cache-remote-tls-enabled", "false",
                "spi-cache-embedded-default-site-name", "ispn",
                "spi-load-balancer-check-remote-poll-interval", "500",
                "-Dkc.cache-remote-create-caches", "true"
        );
    }
}
