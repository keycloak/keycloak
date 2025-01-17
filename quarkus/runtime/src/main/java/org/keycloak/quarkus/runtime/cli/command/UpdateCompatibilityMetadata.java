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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.compatibility.ServerInfo;
import org.keycloak.util.JsonSerialization;
import picocli.CommandLine;

@CommandLine.Command(
        name = UpdateCompatibilityMetadata.NAME,
        description = "Stores the metadata necessary to determine if a configuration is compatible."
)
public class UpdateCompatibilityMetadata extends AbstractUpdatesCommand {

    public static final String NAME = "metadata";
    public static final String OUTPUT_OPTION_NAME = "--file";

    @CommandLine.Option(names = {OUTPUT_OPTION_NAME}, paramLabel = "FILE",
            description = "The file path to store the metadata. It is stored in the JSON format.")
    String outputFile;

    @Override
    public void run() {
        printPreviewWarning();
        validateOutputFile();
        var info = compatibilityManager.current();
        printToConsole(info);
        writeToFile(info);
    }

    @Override
    public String getName() {
        return NAME;
    }

    private void validateOutputFile() {
        if (noOutputFileSet()) {
            return;
        }
        var file = new File(outputFile);
        if (file.getParentFile() != null && !file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new PropertyException("Incorrect argument %s. Unable to create parent directory: %s".formatted(OUTPUT_OPTION_NAME, file.getParentFile().getAbsolutePath()));
        }
        validateFileIsNotDirectory(file, OUTPUT_OPTION_NAME);
    }

    private void printToConsole(ServerInfo info) {
        try {
            var json = JsonSerialization.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(info);
            printOut("Metadata:%n%s".formatted(json));
        } catch (JsonProcessingException e) {
            throw new PropertyException("Unable to create JSON representation of the metadata", e);
        }
    }

    private void writeToFile(ServerInfo info) {
        if (noOutputFileSet()) {
            return;
        }
        var file = new File(outputFile);
        try {
            JsonSerialization.mapper.writeValue(file, info);
        } catch (IOException e) {
            throw new PropertyException("Unable to write file '%s'".formatted(file.getAbsolutePath()), e);
        }
    }

    private boolean noOutputFileSet() {
        return outputFile == null || outputFile.isBlank();
    }
}
