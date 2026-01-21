package org.keycloak.testframework.server;

import java.util.Optional;
import java.util.concurrent.TimeoutException;

import org.keycloak.Keycloak;
import org.keycloak.common.Version;
import org.keycloak.it.utils.Maven;

import io.quarkus.maven.dependency.Dependency;
import org.eclipse.aether.artifact.Artifact;

public class EmbeddedKeycloakServer implements KeycloakServer {

    private Keycloak keycloak;
    private boolean tlsEnabled = false;

    @Override
    public void start(KeycloakServerConfigBuilder keycloakServerConfigBuilder, boolean tlsEnabled) {
        Keycloak.Builder builder = Keycloak.builder().setVersion(Version.VERSION);
        this.tlsEnabled = tlsEnabled;

        for(Dependency dependency : keycloakServerConfigBuilder.toDependencies()) {
            var version = Optional.ofNullable(Maven.getArtifact(dependency.getGroupId(), dependency.getArtifactId()))
                    .map(Artifact::getVersion)
                    .orElse("");
            builder.addDependency(dependency.getGroupId(), dependency.getArtifactId(), version);
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
        if (tlsEnabled) {
            return "https://localhost:8443";
        } else {
            return "http://localhost:8080";
        }
    }

    @Override
    public String getManagementBaseUrl() {
        if (tlsEnabled) {
            return "https://localhost:9001";
        } else {
            return "http://localhost:9001";
        }
    }
}
