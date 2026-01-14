package org.keycloak.testframework.util;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.keycloak.quarkus.runtime.Environment;

public class ProcessUtils {

    public static ProcessHandle waitForDescendent(Process process) {
        long timeout = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < timeout) {
            Optional<ProcessHandle> descendent = process.descendants().findFirst();
            if (descendent.isPresent()) {
                return descendent.get();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Descendent process not started within timeout");
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
