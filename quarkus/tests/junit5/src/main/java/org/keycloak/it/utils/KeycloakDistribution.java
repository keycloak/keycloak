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

    /**
     * Available after the main process exits, which may require {@link #stop()} to be called
     */
    int getExitCode();
    
    boolean supportsDebug();

    void setEnvVar(String name, String value);

    void clearEnv();
    
    void copyProvider(String groupId, String artifactId);

    void copyConfigFile(Path configFilePath);

}
