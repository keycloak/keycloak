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

import java.io.File;
import java.io.IOException;

import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.compatibility.CompatibilityResult;
import org.keycloak.quarkus.runtime.compatibility.ServerInfo;
import org.keycloak.util.JsonSerialization;
import picocli.CommandLine;

@CommandLine.Command(
        name = UpdateCompatibilityCheck.NAME,
        description = "Checks if the metadata is compatible with the current configuration."

)
public class UpdateCompatibilityCheck extends AbstractUpdatesCommand {

    public static final String NAME = "check";
    private static final String INPUT_OPTION_NAME = "--input";


    @CommandLine.Option(names = {INPUT_OPTION_NAME}, paramLabel = "FILE",
            description = "The file path to read the metadata.")
    String inputFile;

    @Override
    public void run() {
        validateInputFile();
        var info = readServerInfo();
        var result = compatibilityManager.isCompatible(info);
        writeComplete("read");
        writeErrorsIfPresent(result);
        picocli.exit(result.exitCode());
    }

    @Override
    public String getName() {
        return NAME;
    }

    private void validateInputFile() {
        validateOptionIsPresent(inputFile, INPUT_OPTION_NAME);
        var file = new File(inputFile);
        if (!file.exists()) {
            throw new PropertyException("Incorrect argument %s. Path '%s' not found".formatted(INPUT_OPTION_NAME, file.getAbsolutePath()));
        }
        validateFileIsNotDirectory(file, INPUT_OPTION_NAME);
    }

    private ServerInfo readServerInfo() {
        var file = new File(inputFile);
        try {
            return JsonSerialization.mapper.readValue(file, ServerInfo.class);
        } catch (IOException e) {
            throw new PropertyException("Unable to read file '%s'".formatted(file.getAbsolutePath()), e);
        }
    }

    private void writeErrorsIfPresent(CompatibilityResult result) {
        var cmd = getCommandLine();
        if (cmd.isPresent()) {
            var colorScheme = cmd.get().getColorScheme();
            var writer = cmd.get().getErr();
            result.errorMessage().map(colorScheme::errorText).ifPresent(writer::println);
        } else {
            result.errorMessage().ifPresent(System.err::println);
        }
    }
}
