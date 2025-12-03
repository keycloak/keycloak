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

@Command(name = ServiceDelete.NAME,
        header = "Delete Keycloak Windows service.",
        description = {
                "%nRemove Keycloak Windows service created with 'kc.bat service create'.",
                "",
                "This command requires prunsrv.exe to be present in the bin directory."
        },
        footerHeading = "Examples:",
        footer = { "  Delete with default service name:%n%n"
                + "      $ ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME}%n%n"
                + "  Delete a custom-named service:%n%n"
                + "      $ ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} --name=my-keycloak%n"})
public class ServiceDelete extends AbstractCommand {

    public static final String NAME = "delete";

    private static final String DEFAULT_SERVICE_NAME = "keycloak";

    @Option(names = "--name",
            description = "The name of the Windows service to delete.",
            defaultValue = DEFAULT_SERVICE_NAME)
    String serviceName;

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

        Path homePath = Environment.getHomePath().orElseThrow(() ->
                new CommandLine.ExecutionException(spec.commandLine(),
                        "Could not determine Keycloak home directory"));

        Path prunsrvPath = homePath.resolve("bin").resolve("prunsrv.exe");
        if (!Files.exists(prunsrvPath)) {
            picocli.println("Looking for prunsrv.exe in: " + prunsrvPath);
            picocli.println("Download from https://downloads.apache.org/commons/daemon/binaries/windows/");
            executionError(spec.commandLine(), "Apache Commons Daemon (Procrun) executable not found at " + prunsrvPath);
        }

        picocli.println("Deleting Keycloak service '" + serviceName + "'...");

        List<String> command = new ArrayList<>();
        command.add(prunsrvPath.toString());
        command.add("delete");
        command.add(serviceName);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                picocli.println("Service '" + serviceName + "' deleted successfully.");
            } else {
                executionError(spec.commandLine(),
                        "Failed to delete service '" + serviceName + "'. Exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            executionError(spec.commandLine(), "Failed to execute prunsrv: " + e.getMessage(), e);
        }
    }
}
