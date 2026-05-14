package org.keycloak.it.utils;

import java.nio.file.Path;
import java.util.List;

import org.keycloak.quarkus.runtime.Environment;

public interface KeycloakDistribution {

    String SCRIPT_CMD = Environment.isWindows() ? "kc.bat" : "kc.sh";

    String SCRIPT_KCADM_CMD = Environment.isWindows() ? "kcadm.bat" : "kcadm.sh";

    /**
     * Run the kc command and immediately return without waiting for the process
     */
    void runKc(List<String> arguments);
    /**
     * Run the kc command and immediately return without waiting for the process
     */
    default void runKc(String... arguments) {
        runKc(List.of(arguments));
    }
    
    void waitFor(boolean ready, long timeoutMillis);
    
    void stop();
    
    int getMappedPort(int port);

    List<String> getOutputStream();

    List<String> getErrorStream();

    int getExitCode();
    
    boolean supportsDebug();

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

    void clearEnv();
}
