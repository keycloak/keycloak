package org.keycloak.it.utils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.keycloak.quarkus.runtime.Environment.LAUNCH_MODE;

public interface KeycloakDistribution {

    void start(List<String> arguments);

    void stop();

    List<String> getOutputStream();

    List<String> getErrorStream();

    int getExitCode();

    boolean isDebug();

    boolean isManualStop();

    default String[] getCliArgs(List<String> arguments) {
        List<String> commands = new ArrayList<>();

        commands.add("./kc.sh");

        if (this.isDebug()) {
            commands.add("--debug");
        }

        if (!this.isManualStop()) {
            commands.add("-D" + LAUNCH_MODE + "=test");
        }

        commands.addAll(arguments);

        return commands.toArray(new String[0]);
    }

    default void setQuarkusProperty(String key, String value) {
        throw new RuntimeException("Not implemented");
    }

    default void setProperty(String key, String value) {
        throw new RuntimeException("Not implemented");
    }

    default void deleteQuarkusProperties() {
        throw new RuntimeException("Not implemented");
    }

    default void copyOrReplaceFileFromClasspath(String file, Path distDir) {
        throw new RuntimeException("Not implemented");
    }
}
