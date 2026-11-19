package org.keycloak.testframework.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StartupDiagnosticsTest {

    @Test
    public void reportsOutputAndThreadDumpOfRunningProcess() throws Exception {
        String java = new File(System.getProperty("java.home"), "bin/java").getPath();
        Process process = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), Sleeper.class.getName()).start();
        try {
            // Wait until the JVM is up and running main, so it can be attached to
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            Assertions.assertEquals("ready", reader.readLine());

            String report = StartupDiagnostics.collect(process, List.of("a line of output"), "an error");

            Assertions.assertTrue(report.contains("Process alive: true"), report);
            Assertions.assertTrue(report.contains("a line of output"), report);
            Assertions.assertTrue(report.contains("an error"), report);
            Assertions.assertTrue(report.contains("Thread dump of " + process.pid()), report);
            Assertions.assertTrue(report.contains(Sleeper.class.getName() + ".main"), report);
        } finally {
            process.destroyForcibly();
        }
    }

    @Test
    public void reportsExitCodeOfTerminatedProcess() throws Exception {
        String java = new File(System.getProperty("java.home"), "bin/java").getPath();
        Process process = new ProcessBuilder(java, "-version").redirectErrorStream(true).start();
        process.waitFor();

        String report = StartupDiagnostics.collect(process, List.of(), "");

        Assertions.assertTrue(report.contains("Process alive: false"), report);
        Assertions.assertTrue(report.contains("Exit code: 0"), report);
        Assertions.assertFalse(report.contains("Thread dump"), report);
    }

    public static class Sleeper {
        public static void main(String[] args) throws Exception {
            System.out.println("ready");
            System.out.flush();
            Thread.sleep(60_000);
        }
    }

}
