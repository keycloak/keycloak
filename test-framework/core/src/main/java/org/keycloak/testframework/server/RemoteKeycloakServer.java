package org.keycloak.testframework.server;

import java.net.ConnectException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;

import io.quarkus.maven.dependency.Dependency;

public class RemoteKeycloakServer implements KeycloakServer {

    private boolean enableTls = false;

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
        } catch (SSLException ignored) {
            // if the kc server is running with https, it is not this class' responsibility to check the certificate
            // we're just checking that keycloak is running
            return true;
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
