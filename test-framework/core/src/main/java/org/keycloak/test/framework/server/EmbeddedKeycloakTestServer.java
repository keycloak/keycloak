package org.keycloak.test.framework.server;

import io.quarkus.maven.dependency.Dependency;
import org.keycloak.Keycloak;
import org.keycloak.common.Version;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public class EmbeddedKeycloakTestServer implements KeycloakTestServer {

    private Keycloak keycloak;

    @Override
    public void start(List<String> rawOptions, Set<Dependency> dependencies) {
        Keycloak.Builder builder = Keycloak.builder().setVersion(Version.VERSION);

        for(Dependency dependency : dependencies) {
            builder.addDependency(dependency.getGroupId(), dependency.getArtifactId(), "");
        }

        keycloak = builder.start(rawOptions);
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

}
