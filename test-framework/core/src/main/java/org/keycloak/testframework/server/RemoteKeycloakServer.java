package org.keycloak.testframework.server;

import java.net.ConnectException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.net.ssl.SSLException;

import org.keycloak.it.utils.Maven;
import org.keycloak.testframework.config.Config;

import io.quarkus.maven.dependency.Dependency;

import static java.lang.System.out;

public class RemoteKeycloakServer implements KeycloakServer {

    private boolean enableTls = false;

    private String kcwCommand;

    @Override
    public void start(KeycloakServerConfigBuilder keycloakServerConfigBuilder) {
        enableTls = keycloakServerConfigBuilder.tlsEnabled();
        kcwCommand = Config.getValueTypeConfig(KeycloakServer.class, "kcw", null, String.class);
        if (!verifyRunningKeycloak()) {
            if (kcwCommand != null) {
                printStartupInstructionsKcw(keycloakServerConfigBuilder);
            } else {
                printStartupInstructionsManual(keycloakServerConfigBuilder);
            }
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

    private void printStartupInstructionsManual(KeycloakServerConfigBuilder config) {
        out.println("Remote Keycloak server is not running on " + getBaseUrl() + ", please start Keycloak with:");
        out.println();
        out.println(String.join(" \\\n", config.toArgs()));
        out.println();

        Set<Dependency> dependencies = config.toDependencies();
        if (!dependencies.isEmpty()) {
            out.println("Requested providers:");
            for (Dependency d : dependencies) {
                out.println("* " + d.getGroupId() + ":" + d.getArtifactId());
            }
            out.println();
        }

        Set<Path> configFiles = config.toConfigFiles();
        if (!configFiles.isEmpty()) {
            out.println("Config files:");
            for (Path c : configFiles) {
                out.print("* " + c.toAbsolutePath());
            }
            out.println();
        }
    }

    private void printStartupInstructionsKcw(KeycloakServerConfigBuilder config) {
        out.println("Remote Keycloak server is not running on " + getBaseUrl() + ", please start Keycloak with:");
        out.println();

        Set<Dependency> dependencies = config.toDependencies();
        if (!dependencies.isEmpty()) {
            String dependencyPaths = dependencies.stream().map(d -> Maven.resolveArtifact(d.getGroupId(), d.getArtifactId()).toString()).collect(Collectors.joining(","));
            out.println("KCW_PROVIDERS=" + dependencyPaths + " \\");
        }

        Set<Path> configFiles = config.toConfigFiles();
        if (!configFiles.isEmpty()) {
            String configPaths =  configFiles.stream().map(p -> p.toAbsolutePath().toString()).collect(Collectors.joining(","));
            out.println("KCW_CONFIGS=" + configPaths + " \\");
        }

        out.println("kcw " + kcwCommand + " " + String.join(" \\\n", config.toArgs()));
        out.println();
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
