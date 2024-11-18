package org.keycloak.test.framework.server;

import io.quarkus.maven.dependency.Dependency;

import java.net.ConnectException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RemoteKeycloakTestServer implements KeycloakTestServer {

    @Override
    public void start(CommandBuilder commandBuilder, Set<Dependency> dependencies) {
        if (!verifyRunningKeycloak()) {
            printStartupInstructions(commandBuilder, dependencies);
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

    private void printStartupInstructions(CommandBuilder commandBuilder, Set<Dependency> dependencies) {
        StringBuilder sb = new StringBuilder();

        sb.append("Remote Keycloak server is not running on " + getBaseUrl() + ", please start Keycloak with:\n\n");

        sb.append(String.join(" \\\n", commandBuilder.toArgs()));
        sb.append("\n\n");
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
