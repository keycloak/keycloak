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

import org.keycloak.quarkus.runtime.Environment;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = ServiceInstall.NAME,
        header = "Install Keycloak as a Windows service.",
        description = {
            "%nInstall Keycloak as a Windows service using Apache Commons Daemon (Procrun).",
            "",
            "This command requires prunsrv.exe to be present in the bin directory.",
            "Download it from https://downloads.apache.org/commons/daemon/binaries/windows/",
            "",
            "For production deployments, run 'kc.bat build' first to optimize the server."
        },
        footerHeading = "Examples:",
        footer = { "  Install with default settings:%n%n"
                + "      $ ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME}%n%n"
                + "  Install for production mode:%n%n"
                + "      $ ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} --kc-args=\"start --http-port=8080\"%n%n"
                + "  Install with custom service name and memory settings:%n%n"
                + "      $ ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} --name=my-keycloak --jvm-ms=512 --jvm-mx=1024%n"})
public class ServiceInstall extends AbstractCommand {

    public static final String NAME = "install";

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

    @Option(names = "--jvm",
            description = "Path to jvm.dll. Auto-detected from JAVA_HOME if not specified.")
    String jvmPath;

    @Option(names = "--kc-args",
            description = "Arguments to pass to Keycloak (e.g., \"start --http-port=8080\").",
            defaultValue = "start-dev")
    String keycloakArgs;

    @Option(names = "--jvm-args",
            description = "Additional JVM options separated by semicolons. Omit the leading dash - it will be added automatically (e.g., \"Djava.net.preferIPv4Stack=true;verbose:gc\").")
    String jvmArgs;

    @Option(names = "--jvm-ms",
            description = "Initial Java heap size in MB.")
    Integer jvmMs;

    @Option(names = "--jvm-mx",
            description = "Maximum Java heap size in MB.")
    Integer jvmMx;

    @Option(names = "--service-user",
            description = "The user account the service should run as. Defaults to LocalSystem.")
    String serviceUser;

    @Option(names = "--service-password",
            description = "The password for the service user account.")
    String servicePassword;

    @Option(names = "--classpath-jar",
            description = "Path to quarkus-run.jar if auto-detection fails.")
    String classpathJar;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected void runCommand() {
        if (!Environment.isWindows()) {
            executionError(spec.commandLine(), "Windows service management is only available on Windows.");
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

        // Find JVM path
        String jvm = resolveJvmPath();
        if (jvm == null) {
            executionError(spec.commandLine(), "Could not find jvm.dll. Please specify --jvm parameter or set JAVA_HOME.");
        }

        // Find classpath JAR
        String classPath = resolveClasspathJar(homePath);
        if (classPath == null) {
            executionError(spec.commandLine(), "Could not find quarkus-run.jar. Please specify --classpath-jar parameter.");
        }

        // Ensure log directory exists
        Path logPath = homePath.resolve("log");
        try {
            Files.createDirectories(logPath);
        } catch (IOException e) {
            executionError(spec.commandLine(), "Failed to create log directory: " + logPath, e);
        }

        picocli.println("Installing Keycloak as a Windows service '" + serviceName + "'...");
        picocli.println("Using JVM at: " + jvm);
        picocli.println("Using classpath: " + classPath);

        List<String> command = buildPrunsrvCommand(prunsrvPath, homePath, jvm, classPath, logPath);

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

    private String resolveJvmPath() {
        if (jvmPath != null && Files.exists(Path.of(jvmPath))) {
            return jvmPath;
        }

        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null) {
            javaHome = System.getProperty("java.home");
        }

        if (javaHome != null) {
            // Try standard locations for jvm.dll
            Path[] candidates = {
                Path.of(javaHome, "bin", "server", "jvm.dll"),
                Path.of(javaHome, "jre", "bin", "server", "jvm.dll"),
                Path.of(javaHome, "lib", "server", "jvm.dll")
            };

            for (Path candidate : candidates) {
                if (Files.exists(candidate)) {
                    return candidate.toString();
                }
            }
        }

        return null;
    }

    private String resolveClasspathJar(Path homePath) {
        if (classpathJar != null && Files.exists(Path.of(classpathJar))) {
            return classpathJar;
        }

        Path defaultJar = homePath.resolve("lib").resolve("quarkus-run.jar");
        if (Files.exists(defaultJar)) {
            return defaultJar.toString();
        }

        return null;
    }

    private List<String> buildPrunsrvCommand(Path prunsrvPath, Path homePath, String jvm, 
            String classPath, Path logPath) {
        List<String> cmd = new ArrayList<>();
        cmd.add(prunsrvPath.toString());
        cmd.add("install");
        cmd.add(serviceName);
        cmd.add("--DisplayName=" + displayName);
        cmd.add("--Description=" + description);
        cmd.add("--Startup=" + startupMode);
        cmd.add("--Jvm=" + jvm);

        // Build JVM options
        StringBuilder jvmOpts = new StringBuilder();
        jvmOpts.append("-Djava.awt.headless=true");
        jvmOpts.append(";-Dkc.home.dir=").append(homePath);
        jvmOpts.append(";-Dkc.config.built=true");

        if (jvmArgs != null && !jvmArgs.isEmpty()) {
            for (String arg : jvmArgs.split(";")) {
                if (!arg.isEmpty()) {
                    String trimmedArg = arg.trim();
                    // Add leading dash if not present (required due to Windows batch argument parsing limitations)
                    if (!trimmedArg.startsWith("-")) {
                        jvmOpts.append(";-").append(trimmedArg);
                    } else {
                        jvmOpts.append(";").append(trimmedArg);
                    }
                }
            }
        }

        // Add JVM options using ++JvmOptions format for proper parsing
        for (String opt : jvmOpts.toString().split(";")) {
            if (!opt.isEmpty()) {
                cmd.add("++JvmOptions=" + opt);
            }
        }

        // Add memory settings if specified
        if (jvmMs != null) {
            cmd.add("--JvmMs=" + jvmMs);
        }
        if (jvmMx != null) {
            cmd.add("--JvmMx=" + jvmMx);
        }

        cmd.add("--StartPath=" + homePath);
        cmd.add("--StartMode=jvm");
        cmd.add("--StartClass=io.quarkus.bootstrap.runner.QuarkusEntryPoint");
        cmd.add("--StartMethod=main");

        // Split keycloak args into separate --StartParams entries
        for (String arg : keycloakArgs.split("\\s+")) {
            if (!arg.isEmpty()) {
                cmd.add("--StartParams=" + arg);
            }
        }

        cmd.add("--StopMode=jvm");
        cmd.add("--StopClass=io.quarkus.bootstrap.runner.QuarkusEntryPoint");
        cmd.add("--StopMethod=main");
        cmd.add("--StopTimeout=30");
        cmd.add("--LogPath=" + logPath);
        cmd.add("--LogLevel=Info");
        cmd.add("--StdOutput=auto");
        cmd.add("--StdError=auto");
        cmd.add("--Classpath=" + classPath);

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
