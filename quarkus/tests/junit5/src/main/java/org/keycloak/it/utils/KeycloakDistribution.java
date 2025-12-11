package org.keycloak.it.utils;

import java.nio.file.Path;
import java.util.List;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.quarkus.runtime.Environment;

public interface KeycloakDistribution {

    String SCRIPT_CMD = Environment.isWindows() ? "kc.bat" : "kc.sh";

    String SCRIPT_KCADM_CMD = Environment.isWindows() ? "kcadm.bat" : "kcadm.sh";

    CLIResult run(List<String> arguments);
    default CLIResult run(String... arguments) {
        return run(List.of(arguments));
    }

    void stop();

    List<String> getOutputStream();

    List<String> getErrorStream();

    int getExitCode();

    boolean isDebug();

    boolean isManualStop();

    void setRequestPort();

    void setRequestPort(int port);

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

    default void removeProperty(String name) {
        throw new RuntimeException("Not implemented");
    }

    default void setEnvVar(String name, String value) {
        throw new RuntimeException("Not implemented");
    }

    default void copyOrReplaceFile(Path file, Path targetFile) {
        throw new RuntimeException("Not implemented");
    }

    <D extends KeycloakDistribution> D unwrap(Class<D> type);

    void clearEnv();
}
