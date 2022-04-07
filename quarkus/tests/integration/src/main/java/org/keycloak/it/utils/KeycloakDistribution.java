package org.keycloak.it.utils;

import org.keycloak.quarkus.runtime.Environment;
import java.nio.file.Path;
import java.util.List;

public interface KeycloakDistribution {

    String SCRIPT_CMD = Environment.isWindows() ? "kc.bat" : "kc.sh";
    String SCRIPT_CMD_INVOKABLE = Environment.isWindows() ? SCRIPT_CMD : "./"+SCRIPT_CMD;

    void start(List<String> arguments);

    void stop();

    List<String> getOutputStream();

    List<String> getErrorStream();

    int getExitCode();

    boolean isDebug();

    boolean isManualStop();

    default String[] getCliArgs(List<String> arguments) {
        throw new RuntimeException("Not implemented");
    }

    default void setManualStop(boolean manualStop) {
        throw new RuntimeException("Not implemented");
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
