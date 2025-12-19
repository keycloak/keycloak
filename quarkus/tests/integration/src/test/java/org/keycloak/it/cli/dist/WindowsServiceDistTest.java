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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawKeycloakDistribution;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledOnOs(value = OS.WINDOWS, disabledReason = "Windows service tests are only applicable on Windows")
@DistributionTest
@RawDistOnly(reason = "Windows service management requires raw distribution")
@Tag(DistributionTest.WIN)
public class WindowsServiceDistTest {

    private static final String TEST_SERVICE_NAME_PREFIX = "keycloak-test-";
    private static final int SERVICE_START_TIMEOUT_SECONDS = 60;
    private static final int SERVICE_STOP_TIMEOUT_SECONDS = 30;

    private RawKeycloakDistribution rawDist;
    private Path distPath;
    private String testServiceName;
    private boolean serviceCreated = false;
    private boolean prunsrvAvailable = false;

    @BeforeEach
    void setUp(KeycloakDistribution dist) {
        this.rawDist = dist.unwrap(RawKeycloakDistribution.class);
        this.distPath = rawDist.getDistPath();
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
        if (serviceCreated) {
            try {
                stopService();
            } catch (Exception e) {
                System.err.println("Failed to stop service during cleanup: " + e.getMessage());
            }
            try {
                deleteService();
            } catch (Exception e) {
                System.err.println("Failed to delete service during cleanup: " + e.getMessage());
            }
        }
    }

    @Test
    void testServiceLifecycle() throws Exception {
        assertPrunsrvAvailable();
        assertAdminPrivileges();

        String customDisplayName = "Keycloak Test Service " + testServiceName;
        String customDescription = "Keycloak integration test service";

        rawDist.setProperty("http-enabled", "true");
        rawDist.setProperty("hostname-strict", "false");
        rawDist.setProperty("log", "console,file");
        rawDist.setProperty("log-file", distPath.resolve("log").resolve("keycloak.log").toString());

        // Install the service with custom name and display name
        CLIResult installResult = rawDist.run("tools", "windows-service", "install",
                "--name=" + testServiceName,
                "--display-name=" + customDisplayName,
                "--description=" + customDescription,
                "--startup=manual");

        assertEquals(0, installResult.exitCode(), "Service installation failed: " + installResult.getOutput());
        assertThat(installResult.getOutput(), containsString("installed successfully"));
        serviceCreated = true;
        assertTrue(isServiceCreated(testServiceName), "Service should be installed");

        // Verify the display name in service configuration
        String serviceInfo = getServiceInfo(testServiceName);
        assertThat("Service info should contain custom display name", serviceInfo, containsString(customDisplayName));

        // Test service start
        assertTrue(startService(), "Service should start successfully");
        assertTrue(waitForKeycloakReady(), "Keycloak should be accessible after service start");
        assertEquals("RUNNING", getServiceState(testServiceName), "Service should be in RUNNING state");

        // Verify log file was created and contains startup message
        Path logFile = distPath.resolve("log").resolve("keycloak.log");
        assertTrue(waitForLogFile(logFile), "Log file should be created");
        String logContent = Files.readString(logFile);
        assertThat("Log should contain Keycloak startup message", logContent, containsString("Listening on:"));

        // Test service stop
        assertTrue(stopService(), "Service should stop successfully");
        assertTrue(waitForServiceStopped(), "Service should be in STOPPED state");
        assertFalse(isKeycloakAccessible(), "Keycloak should not be accessible after service stop");

        // Test service uninstall
        CLIResult uninstallResult = rawDist.run("tools", "windows-service", "uninstall", "--name=" + testServiceName);
        assertEquals(0, uninstallResult.exitCode(), "Service uninstallation failed: " + uninstallResult.getOutput());
        assertThat(uninstallResult.getOutput(), containsString("uninstalled successfully"));
        serviceCreated = false;
        assertFalse(isServiceCreated(testServiceName), "Service should be uninstalled");
    }

    private boolean waitForLogFile(Path logFile) {
        try {
            org.awaitility.Awaitility.await()
                    .atMost(SERVICE_START_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .pollInterval(2, TimeUnit.SECONDS)
                    .until(() -> Files.exists(logFile) && Files.size(logFile) > 0);
            return true;
        } catch (org.awaitility.core.ConditionTimeoutException e) {
            return false;
        }
    }

    private boolean waitForKeycloakReady() {
        try {
            org.awaitility.Awaitility.await()
                    .atMost(SERVICE_START_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .pollInterval(2, TimeUnit.SECONDS)
                    .until(this::isKeycloakAccessible);
            return true;
        } catch (org.awaitility.core.ConditionTimeoutException e) {
            return false;
        }
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

    private void deleteService() {
        rawDist.run("tools", "windows-service", "uninstall", "--name=" + testServiceName);
    }

    private boolean isServiceCreated(String serviceName) {
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

    private String getServiceInfo(String serviceName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sc", "qc", serviceName);
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
}
