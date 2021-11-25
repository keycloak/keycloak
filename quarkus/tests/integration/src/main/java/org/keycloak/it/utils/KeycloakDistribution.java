package org.keycloak.it.utils;

import org.eclipse.aether.artifact.Artifact;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.keycloak.quarkus.runtime.Environment.LAUNCH_MODE;

public interface KeycloakDistribution {

    void start(List<String> arguments);

    void stop();

    List<String> getOutputStream();

    List<String> getErrorStream();

    int getExitCode();

    boolean getDebug();

    boolean getManualStop();

    default String[] getCliArgs(List<String> arguments) {
        List<String> commands = new ArrayList<>();

        commands.add("./kc.sh");

        if (this.getDebug()) {
            commands.add("--debug");
        }

        if (!this.getManualStop()) {
            commands.add("-D" + LAUNCH_MODE + "=test");
        }

        commands.addAll(arguments);

        return commands.toArray(new String[0]);
    }
}
