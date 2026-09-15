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

package org.keycloak.quarkus.runtime.cli.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.keycloak.config.LoggingOptions;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = WindowsServiceInstall.NAME,
        header = "Install Keycloak as a Windows service.",
        description = {
            "%nInstall a Windows service that runs Keycloak using 'kc.bat start'.",
            "",
            "This command requires prunsrv.exe to be present in the bin directory.",
            "Download it from https://downloads.apache.org/commons/daemon/binaries/windows/",
            "",
            "The service runs in exe mode, executing kc.bat as an external process.",
            "This means all environment variables and configuration files are respected.",
            "",
            "For faster startup, run 'kc.bat build' before installing the service.",
            "Without a pre-build, the first service start will be slower as it builds."
        },
        footerHeading = "Examples:",
        footer = { "  Install with default settings:%n%n"
                + "      $ ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME}%n%n"
                + "  Install with custom service name:%n%n"
                + "      $ ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} --name=my-keycloak%n%n"
                + "  Install with dependencies on other services:%n%n"
                + "      $ ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} --depends-on=\"postgresql-x64-15;Tcpip\"%n"})
public class WindowsServiceInstall extends AbstractCommand {

    public static final String NAME = "install";

    public static final String SERVICE_PASSWORD_ENV = "KC_SERVICE_PASSWORD";

    private static final String DEFAULT_SERVICE_NAME = "keycloak";
    private static final String DEFAULT_DISPLAY_NAME = "Keycloak Server";
    private static final String DEFAULT_DESCRIPTION = "Keycloak Identity and Access Management";

    @Option(names = "--name",
            description = "The name of the Windows service.",
            defaultValue = DEFAULT_SERVICE_NAME)
    String serviceName;

    @Option(names = "--display-name",
            description = "The display name of the Windows service.",
            defaultValue = DEFAULT_DISPLAY_NAME)
    String displayName;

    @Option(names = "--description",
            description = "The description of the Windows service.",
            defaultValue = DEFAULT_DESCRIPTION)
    String description;

    @Option(names = "--startup",
            description = "Service startup mode: auto, manual.",
            defaultValue = "auto")
    String startupMode;

    @Option(names = "--service-user",
            description = "The user account the service should run as. Defaults to LocalSystem.")
    String serviceUser;

    @Option(names = "--service-password",
            description = "The password for the service user account. Can also be set via the " + SERVICE_PASSWORD_ENV + " environment variable.")
    String servicePassword;

    @Option(names = "--stop-timeout",
            description = "Timeout in seconds to wait for service to stop gracefully.",
            defaultValue = "30")
    Integer stopTimeout;

    @Option(names = "--depends-on",
            description = "Services that must start before this service. Separate multiple services with semicolons (e.g., \"postgresql-x64-15;Tcpip\").")
    String dependsOn;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isHelpAll() {
        return false;
    }

    @Override
    protected void runCommand() {
        if (!Environment.isWindows()) {
            executionError(spec.commandLine(), "Windows service management is only available on Windows.");
        }

        // Check for password from environment variable if not provided via command line
        if (servicePassword == null || servicePassword.isEmpty()) {
            servicePassword = System.getenv(SERVICE_PASSWORD_ENV);
        }

        Path homePath = Environment.getHomePath().orElseThrow(() -> 
            new CommandLine.ExecutionException(spec.commandLine(), 
                "Could not determine Keycloak home directory"));

        Path prunsrvPath = homePath.resolve("bin").resolve("prunsrv.exe");
        if (!Files.exists(prunsrvPath)) {
            picocli.println("Looking for prunsrv.exe in: " + prunsrvPath);
            picocli.println("Download from https://downloads.apache.org/commons/daemon/binaries/windows/");
            executionError(spec.commandLine(), "Apache Commons Daemon (Procrun) executable not found at " + prunsrvPath);
        }

        Path kcBatPath = homePath.resolve("bin").resolve("kc.bat");
        if (!Files.exists(kcBatPath)) {
            executionError(spec.commandLine(), "kc.bat not found at " + kcBatPath);
        }

        // If a custom log file location is set, the service wrapper logs are stored in the same directory
        Path logPath;
        Optional<String> logFileOption = Configuration.getOptionalKcValue(LoggingOptions.LOG_FILE);
        if (logFileOption.isPresent()) {
            Path logFile = Path.of(logFileOption.get());
            if (!logFile.isAbsolute()) {
                logFile = homePath.resolve(logFile);
            }
            logPath = logFile.getParent();
        } else {
            logPath = homePath.resolve("data").resolve("log");
        }

        try {
            Files.createDirectories(logPath);
        } catch (IOException e) {
            executionError(spec.commandLine(), "Failed to create log directory: " + logPath, e);
        }

        picocli.println("Creating Keycloak Windows service '" + serviceName + "'...");
        picocli.println("Service will run: " + kcBatPath + " start");

        List<String> command = buildPrunsrvCommand(prunsrvPath, homePath, kcBatPath, logPath);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                picocli.println("Service '" + serviceName + "' installed successfully.");
                if (serviceUser == null) {
                    picocli.println("Service is configured to run as Local System account.");
                }
                picocli.println("");
                picocli.println("To start the service, run as Administrator:");
                picocli.println("   net start " + serviceName);
            } else {
                executionError(spec.commandLine(), 
                    "Failed to install service '" + serviceName + "'. Exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            executionError(spec.commandLine(), "Failed to execute prunsrv: " + e.getMessage(), e);
        }
    }

    private List<String> buildPrunsrvCommand(Path prunsrvPath, Path homePath, Path kcBatPath, Path logPath) {
        List<String> cmd = new ArrayList<>();
        cmd.add(prunsrvPath.toString());
        cmd.add("install");
        cmd.add(serviceName);
        cmd.add("--DisplayName=" + displayName);
        cmd.add("--Description=" + description);
        cmd.add("--Startup=" + startupMode);

        // Use exe mode to run kc.bat directly
        cmd.add("--StartMode=exe");
        cmd.add("--StartPath=" + homePath);
        cmd.add("--StartImage=" + kcBatPath);
        cmd.add("--StartParams=start");

        cmd.add("--StopMode=exe");
        cmd.add("--StopPath=" + homePath);
        cmd.add("--StopImage=" + kcBatPath);
        cmd.add("--StopParams=stop");
        cmd.add("--StopTimeout=" + stopTimeout);

        // Add service dependencies if specified
        if (dependsOn != null && !dependsOn.isEmpty()) {
            cmd.add("++DependsOn=" + dependsOn);
        }

        cmd.add("--LogPath=" + logPath);
        cmd.add("--LogLevel=Info");

        // Configure service account
        if (serviceUser != null && !serviceUser.isEmpty()) {
            picocli.println("Configuring service to run as user: " + serviceUser);
            cmd.add("--ServiceUser=" + serviceUser);
            if (servicePassword != null && !servicePassword.isEmpty()) {
                cmd.add("--ServicePassword=" + servicePassword);
            }
        } else {
            picocli.println("Configuring service to run as Local System account");
            cmd.add("--ServiceUser=LocalSystem");
        }

        return cmd;
    }
}
