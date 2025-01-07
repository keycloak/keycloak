package org.keycloak.test.framework.server;

import io.quarkus.maven.dependency.Dependency;
import org.keycloak.Keycloak;
import org.keycloak.common.Version;

import java.util.concurrent.TimeoutException;

public class EmbeddedKeycloakServer implements KeycloakServer {

    private Keycloak keycloak;

    @Override
    public void start(KeycloakServerConfigBuilder keycloakServerConfigBuilder) {
        Keycloak.Builder builder = Keycloak.builder().setVersion(Version.VERSION);

        for(Dependency dependency : keycloakServerConfigBuilder.toDependencies()) {
            builder.addDependency(dependency.getGroupId(), dependency.getArtifactId(), "");
        }

        keycloak = builder.start(keycloakServerConfigBuilder.toArgs());
    }

    @Override
    public void stop() {
        try {
            keycloak.stop();
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }

    @Override
    public String getManagementBaseUrl() {
        return "http://localhost:9000";
    }
}
