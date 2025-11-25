/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.it.cli.dist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import static io.restassured.RestAssured.given;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawKeycloakDistribution;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the Windows service lifecycle using Apache Commons Daemon (Procrun).
 */
@EnabledOnOs(value = OS.WINDOWS, disabledReason = "Windows service tests are only applicable on Windows")
@DistributionTest
@RawDistOnly(reason = "Windows service management requires raw distribution")
@Tag(DistributionTest.WIN)
public class WindowsServiceDistTest {

    private static final String TEST_SERVICE_NAME_PREFIX = "keycloak-test-";
    private static final int SERVICE_START_TIMEOUT_SECONDS = 60;
    private static final int SERVICE_STOP_TIMEOUT_SECONDS = 30;

    private Path distPath;
    private String testServiceName;
    private boolean serviceInstalled = false;
    private boolean prunsrvAvailable = false;

    @BeforeEach
    void setUp(KeycloakDistribution dist) {
        this.distPath = dist.unwrap(RawKeycloakDistribution.class).getDistPath();
        this.testServiceName = TEST_SERVICE_NAME_PREFIX + System.currentTimeMillis();

        // Check if prunsrv.exe is available in the distribution
        Path prunsrvPath = distPath.resolve("bin").resolve("prunsrv.exe");
        if (!Files.exists(prunsrvPath)) {
            String prunsrvSystemPath = findPrunsrvInSystem();
            if (prunsrvSystemPath != null) {
                try {
                    Files.copy(Path.of(prunsrvSystemPath), prunsrvPath, StandardCopyOption.REPLACE_EXISTING);
                    prunsrvAvailable = true;
                } catch (IOException e) {
                    System.err.println("Could not copy prunsrv.exe to distribution: " + e.getMessage());
                }
            }
        } else {
            prunsrvAvailable = true;
        }
    }

    @AfterEach
    void tearDown() {
        if (serviceInstalled) {
            try {
                stopService();
            } catch (Exception e) {
                System.err.println("Failed to stop service during cleanup: " + e.getMessage());
            }
            try {
                uninstallService();
            } catch (Exception e) {
                System.err.println("Failed to uninstall service during cleanup: " + e.getMessage());
            }
        }
    }

    @Test
    void testServiceLifecycle() throws Exception {
        assertPrunsrvAvailable();
        assertAdminPrivileges();

        // Test production mode service installation with Keycloak runtime options (build is not needed)
        ProcessResult installResult = runServiceScript("service-install.bat",
                "--name", testServiceName,
                "--display-name", "Keycloak Test Service",
                "--description", "Keycloak integration test service",
                "--startup", "manual",
                "--keycloak-args", "start --http-enabled=true --hostname-strict=false");

        assertEquals(0, installResult.exitCode, "Service installation failed: " + installResult.output + "\n" + installResult.errorOutput);
        assertThat(installResult.output, containsString("installed successfully"));
        serviceInstalled = true;
        assertTrue(isServiceInstalled(testServiceName), "Service should be installed");

        // Test service start
        assertTrue(startService(), "Service should start successfully");
        assertTrue(waitForKeycloakReady(), "Keycloak should be accessible after service start");
        assertEquals("RUNNING", getServiceState(testServiceName), "Service should be in RUNNING state");

        // Test service logging
        Path logDir = distPath.resolve("log");
        String stdoutLog = readServiceStdoutLog(logDir);
        assertThat("Stdout log should contain Keycloak startup message", stdoutLog, containsString("Listening on:"));

        // Test service stop
        assertTrue(stopService(), "Service should stop successfully");
        assertTrue(waitForServiceStopped(), "Service should be in STOPPED state");
        assertFalse(isKeycloakAccessible(), "Keycloak should not be accessible after service stop");

        // Test service uninstall
        ProcessResult uninstallResult = runServiceScript("service-uninstall.bat", "--name", testServiceName);
        assertEquals(0, uninstallResult.exitCode, "Service uninstallation failed: " + uninstallResult.output + "\n" + uninstallResult.errorOutput);
        assertThat(uninstallResult.output, containsString("uninstalled successfully"));
        serviceInstalled = false;
        assertFalse(isServiceInstalled(testServiceName), "Service should be uninstalled");
    }

    @Test
    void testServiceWithDevModeAndJvmArgs() throws Exception {
        assertPrunsrvAvailable();
        assertAdminPrivileges();

        // Test development mode service installation with custom JVM args
        ProcessResult installResult = runServiceScript("service-install.bat",
                "--name", testServiceName,
                "--display-name", "Keycloak Dev Test Service",
                "--startup", "manual",
                "--keycloak-args", "start-dev",
                "--jvm-args", "-Dtest.prop1=value1;-Dtest.prop2=value2");

        assertEquals(0, installResult.exitCode, "Service installation failed: " + installResult.output + "\n" + installResult.errorOutput);
        serviceInstalled = true;

        // Verify JVM options are stored in service configuration (in Windows Registry)
        String jvmOptions = getServiceJvmOptions(testServiceName);
        assertThat("JVM options should contain test.prop1", jvmOptions, containsString("-Dtest.prop1=value1"));
        assertThat("JVM options should contain test.prop2", jvmOptions, containsString("-Dtest.prop2=value2"));

        assertTrue(startService(), "Service should start successfully with custom JVM args");
        assertTrue(waitForKeycloakReady(), "Keycloak should be accessible in dev mode");

        stopService();
        runServiceScript("service-uninstall.bat", "--name", testServiceName);
        serviceInstalled = false;
    }

    @Test
    void testJvmMemoryArgsApplied() throws Exception {
        assertPrunsrvAvailable();
        assertAdminPrivileges();

        // Test prunsrv native parameters for JVM memory settings
        ProcessResult installResult = runServiceScript("service-install.bat",
                "--name", testServiceName,
                "--startup", "manual",
                "--keycloak-args", "start --http-enabled=true --hostname-strict=false",
                "--jvm-ms", "8",
                "--jvm-mx", "16");

        assertEquals(0, installResult.exitCode, "Service installation should succeed even with low memory args");
        serviceInstalled = true;

        boolean started = startService();

        if (started) {
            assertFalse(waitForKeycloakReady(10), "Keycloak should NOT be accessible with only 16MB heap");
        }
        

        stopService();
        runServiceScript("service-uninstall.bat", "--name", testServiceName);
        serviceInstalled = false;
    }

    private boolean waitForKeycloakReady(int timeoutSeconds) {
        try {
            org.awaitility.Awaitility.await()
                    .atMost(timeoutSeconds, TimeUnit.SECONDS)
                    .pollInterval(2, TimeUnit.SECONDS)
                    .until(this::isKeycloakAccessible);
            return true;
        } catch (org.awaitility.core.ConditionTimeoutException e) {
            return false;
        }
    }

    private String readServiceStdoutLog(Path logDir) throws IOException {
        StringBuilder allLogs = new StringBuilder();
        if (Files.exists(logDir)) {
            try (var files = Files.list(logDir)) {
                files.filter(p -> p.getFileName().toString().contains("-stdout.") ||
                                  p.getFileName().toString().contains("stdout"))
                        .forEach(logFile -> {
                            try {
                                allLogs.append(Files.readString(logFile));
                            } catch (IOException e) {
                                // ignore
                            }
                        });
            }
        }
        return allLogs.toString();
    }

    private void assertPrunsrvAvailable() {
        assertTrue(prunsrvAvailable, "prunsrv.exe not available. Download from https://downloads.apache.org/commons/daemon/binaries/windows/");
    }

    private void assertAdminPrivileges() {
        try {
            ProcessBuilder pb = new ProcessBuilder("net", "session");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            // Consume output to prevent blocking
            process.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
            int exitCode = process.waitFor();
            assertEquals(0, exitCode, "Administrator privileges required to run Windows service tests. Run tests from an elevated terminal or IDE.");
        } catch (Exception e) {
            throw new AssertionError("Could not verify admin privileges: " + e.getMessage(), e);
        }
    }

    private String findPrunsrvInSystem() {
        List<String> possiblePaths = new ArrayList<>();
        
        String prunsrvHome = System.getenv("PRUNSRV_HOME");
        if (prunsrvHome != null) {
            possiblePaths.add(prunsrvHome + "\\prunsrv.exe");
        }
        
        String commonsDaemonHome = System.getenv("COMMONS_DAEMON_HOME");
        if (commonsDaemonHome != null) {
            possiblePaths.add(commonsDaemonHome + "\\prunsrv.exe");
        }
        
        possiblePaths.add("C:\\Program Files\\Apache\\commons-daemon\\prunsrv.exe");

        return possiblePaths.stream()
                .filter(path -> Files.exists(Path.of(path)))
                .findFirst()
                .orElse(null);
    }

    private ProcessResult runServiceScript(String scriptName, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("cmd.exe");
        command.add("/c");
        command.add(distPath.resolve("bin").resolve(scriptName).toString());
        for (String arg : args) {
            // Wrap args containing = or ; in quotes to preserve them as single arguments
            if (arg.contains("=") || arg.contains(";")) {
                command.add("\"" + arg + "\"");
            } else {
                command.add(arg);
            }
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(distPath.resolve("bin").toFile());
        pb.environment().put("JAVA_HOME", System.getProperty("java.home"));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Read all output (stdout + stderr merged)
        String output = new String(process.getInputStream().readAllBytes());

        boolean finished = process.waitFor(120, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new ProcessResult(-1, output, "Process timed out after 120 seconds");
        }

        return new ProcessResult(process.exitValue(), output, "");
    }

    private boolean startService() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("net", "start", testServiceName);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("Service start failed with exit code " + exitCode + ": " + output);
        }
        return exitCode == 0;
    }

    private boolean stopService() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("net", "stop", testServiceName);
        Process process = pb.start();
        process.waitFor(SERVICE_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return true;
    }

    private void uninstallService() throws IOException, InterruptedException {
        runServiceScript("service-uninstall.bat", "--name", testServiceName);
    }

    private boolean isServiceInstalled(String serviceName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sc", "query", serviceName);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String getServiceState(String serviceName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sc", "query", serviceName);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("STATE")) {
                        if (line.contains("RUNNING")) return "RUNNING";
                        if (line.contains("STOPPED")) return "STOPPED";
                        if (line.contains("PAUSED")) return "PAUSED";
                        if (line.contains("START_PENDING")) return "START_PENDING";
                        if (line.contains("STOP_PENDING")) return "STOP_PENDING";
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // ignore
        }
        return "UNKNOWN";
    }

    private String getServiceJvmOptions(String serviceName) {
        try {
            // On 64-bit Windows, procrun uses the 32-bit registry view (Wow6432Node)
            ProcessBuilder pb = new ProcessBuilder("reg", "query",
                    "HKLM\\SOFTWARE\\WOW6432Node\\Apache Software Foundation\\ProcRun 2.0\\" + serviceName + "\\Parameters\\Java",
                    "/v", "Options");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();
            return output;
        } catch (Exception e) {
            return "";
        }
    }

    private boolean waitForServiceStopped() {
        try {
            org.awaitility.Awaitility.await()
                    .atMost(SERVICE_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .until(() -> "STOPPED".equals(getServiceState(testServiceName)));
            return true;
        } catch (org.awaitility.core.ConditionTimeoutException e) {
            return false;
        }
    }

    private boolean waitForKeycloakReady() {
        return waitForKeycloakReady(SERVICE_START_TIMEOUT_SECONDS);
    }

    private boolean isKeycloakAccessible() {
        try {
            return given()
                    .when()
                    .get("http://localhost:8080/realms/master/")
                    .getStatusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private record ProcessResult(int exitCode, String output, String errorOutput) {
    }
}
