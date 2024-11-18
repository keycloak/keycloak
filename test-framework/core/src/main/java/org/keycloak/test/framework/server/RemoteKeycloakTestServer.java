package org.keycloak.test.framework.server;

import io.quarkus.maven.dependency.Dependency;
import org.jboss.logging.Logger;

import java.util.Set;

public class RemoteKeycloakTestServer implements KeycloakTestServer {

    private static final Logger LOGGER = Logger.getLogger(RemoteKeycloakTestServer.class);

    @Override
    public void start(CommandBuilder commandBuilder, Set<Dependency> dependencies) {
        StringBuilder sb = new StringBuilder();
        sb.append("Requested server configuration:");
        sb.append("\n");
        sb.append("Startup command and options:\n\n");
        sb.append(String.join(" \\\n", commandBuilder.toArgs()));
        sb.append("\n\n");
        if (!dependencies.isEmpty()) {
            sb.append("Dependencies:\n\n");
            for (Dependency d : dependencies) {
                sb.append("- ");
                sb.append(d.getGroupId());
                sb.append(":");
                sb.append(d.getArtifactId());
                sb.append("\n");
            }
        }

        LOGGER.infov(sb.toString());
    }

    @Override
    public void stop() {
    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }

}
