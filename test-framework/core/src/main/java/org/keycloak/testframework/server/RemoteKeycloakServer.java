package org.keycloak.testframework.server;

import io.quarkus.maven.dependency.Dependency;

import java.net.ConnectException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RemoteKeycloakServer implements KeycloakServer {

    @Override
    public void start(KeycloakServerConfigBuilder keycloakServerConfigBuilder) {
        if (!verifyRunningKeycloak()) {
            printStartupInstructions(keycloakServerConfigBuilder);
            waitForStartup();
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }

    @Override
    public String getManagementBaseUrl() {
        return "http://localhost:9000";
    }

    private void printStartupInstructions(KeycloakServerConfigBuilder keycloakServerConfigBuilder) {
        StringBuilder sb = new StringBuilder();

        sb.append("Remote Keycloak server is not running on ")
                .append(getBaseUrl())
                .append(", please start Keycloak with:\n\n");

        sb.append(String.join(" \\\n", keycloakServerConfigBuilder.toArgs()));
        sb.append("\n\n");

        Set<Dependency> dependencies = keycloakServerConfigBuilder.toDependencies();
        if (!dependencies.isEmpty()) {
            sb.append("Requested providers:\n");
            for (Dependency d : dependencies) {
                sb.append("- ");
                sb.append(d.getGroupId());
                sb.append(":");
                sb.append(d.getArtifactId());
                sb.append("\n");
            }
        }
        Set<Path> configFiles = keycloakServerConfigBuilder.toConfigFiles();
        if (!configFiles.isEmpty()) {
            sb.append("Copy following config files to your conf directory:\n");
            for (Path c : configFiles) {
                sb.append(c.toAbsolutePath());
                sb.append("\n");
            }
        }
        System.out.println(sb);
    }

    private boolean verifyRunningKeycloak() {
        try {
            new URL(getBaseUrl()).openConnection().connect();
            return true;
        } catch (ConnectException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForStartup() {
        long waitUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        while (!verifyRunningKeycloak() && System.currentTimeMillis() < waitUntil) {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            } catch (InterruptedException e) {
                return;
            }
        }
    }

}
