package org.keycloak.testframework.server;

import io.quarkus.maven.dependency.Dependency;

import java.net.ConnectException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RemoteKeycloakServer implements KeycloakServer {

    private boolean enableTls = false;
    private final String serverKeyStorePath;

    public RemoteKeycloakServer(Path serverKeyStorePath) {
        this.serverKeyStorePath = serverKeyStorePath == null ? null : serverKeyStorePath.toString();
    }

    @Override
    public void start(KeycloakServerConfigBuilder keycloakServerConfigBuilder) {
        enableTls = keycloakServerConfigBuilder.tlsEnabled();
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
        if (isTlsEnabled()) {
            return "https://localhost:8443";
        } else {
            return "http://localhost:8080";
        }
    }

    @Override
    public String getManagementBaseUrl() {
        if (isTlsEnabled()) {
            return "https://localhost:9000";
        } else {
            return "http://localhost:9000";
        }
    }

    @Override
    public boolean isTlsEnabled() {
        return enableTls;
    }

    private void printStartupInstructions(KeycloakServerConfigBuilder keycloakServerConfigBuilder) {
        StringBuilder sb = new StringBuilder();

        sb.append("Remote Keycloak server is not running on ")
                .append(getBaseUrl())
                .append(", please start Keycloak with:\n\n");

        sb.append(String.join(" \\\n", keycloakServerConfigBuilder.toArgs()));
        if (isTlsEnabled()) {
            sb.append(" \\\n--https-key-store-file=").append(serverKeyStorePath);
        }
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
