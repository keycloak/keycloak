package org.keycloak.testframework.util;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.keycloak.quarkus.runtime.Environment;

public class ProcessUtils {

    public static long getKeycloakPid(Process keycloakProcess) {
        List<ProcessHandle> descendants = keycloakProcess.descendants().toList();
        if (descendants.isEmpty()) {
            // When re-augmentation happens `exec` is used to re-start Keycloak. In this case java has the same pid as kc.sh
            return keycloakProcess.pid();
        } else if (descendants.size() == 1) {
            // When re-augmentation does not happen `exec` is not used to start Keycloak. In this case java has a different pid to kc.sh
            return descendants.get(0).pid();
        } else {
            throw new RuntimeException("Started process has multiple descendants");
        }
    }

    public static boolean killProcess(String pid) {
        try {
            if (!Environment.isWindows()) {
                ProcessBuilder pb = new ProcessBuilder("kill", "--timeout", "10000", "TERM", "--timeout", "10000", "KILL", pid);
                Process process = pb.start();
                process.waitFor(10, TimeUnit.SECONDS);
                return process.exitValue() == 0;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public static void killRunningProcess(Process process) {
        killRunningProcess(process, false);
    }

    public static void killRunningProcess(Process process, boolean force) {
        try {
            if (Environment.isWindows()) {
                CompletableFuture<?> allProcesses = CompletableFuture.completedFuture(null);
                Iterator<ProcessHandle> itr = process.descendants().iterator();
                while (itr.hasNext()) {
                    ProcessHandle ph = itr.next();
                    if (force) {
                        ph.destroyForcibly();
                    } else {
                        ph.destroy();
                    }
                    allProcesses = CompletableFuture.allOf(allProcesses, ph.onExit());
                }
                allProcesses.get(10, TimeUnit.SECONDS);
            }

            if (force) {
                process.destroyForcibly();
            } else {
                process.destroy();
            }
            process.waitFor(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            if (!force) {
                killRunningProcess(process, true);
            } else {
                throw new RuntimeException("Failed to stop Keycloak process");
            }
        }
    }

}
