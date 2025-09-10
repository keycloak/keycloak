package org.keycloak.testframework.infinispan;

import java.util.Map;

import org.infinispan.server.test.core.InfinispanContainer;
import org.keycloak.testframework.util.JavaPropertiesUtil;

public class InfinispanExternalServer extends InfinispanContainer implements InfinispanServer {

    private static final String USER = "keycloak";
    private static final String PASSWORD = "Password1!";
    private static final String HOST = "127.0.0.1";

    static InfinispanExternalServer create() {
        String containerName = JavaPropertiesUtil.getContainerImageName("infinispan-server.properties", "infinispan");
        return new InfinispanExternalServer(containerName);
    }

    private InfinispanExternalServer(String dockerImageName) {
        super(dockerImageName);
        withUser(USER);
        withPassword(PASSWORD);
        addFixedExposedPort(11222, 11222);
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
                "spi-cache-remote-default-client-intelligence", "BASIC",
                "-Dkc.cache-remote-create-caches", "true"
        );
    }
}
