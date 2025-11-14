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
import java.util.Map;

import org.keycloak.compatibility.CompatibilityResult;
import org.keycloak.compatibility.Util;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import picocli.CommandLine;

@CommandLine.Command(
        name = UpdateCompatibilityCheck.NAME,
        description = "Checks if the metadata is compatible with the current configuration. A zero exit code means a rolling update is possible between old and the current metadata."
)
public class UpdateCompatibilityCheck extends AbstractUpdatesCommand {

    public static final String NAME = "check";
    public static final String INPUT_OPTION_NAME = "--file";
    public static final TypeReference<Map<String, Map<String, String>>> METADATA_TYPE_REF = new TypeReference<>() {
    };


    @CommandLine.Option(names = {INPUT_OPTION_NAME}, paramLabel = "FILE",
            description = "The file path to read the metadata.")
    String inputFile;

    @Override
    int executeAction() {
        var info = readServerInfo();
        var providers = loadAllProviders();
        var idIterator = Util.mergeKeySet(info, providers)
                .sorted()
                .iterator();

        while (idIterator.hasNext()) {
            var id = idIterator.next();
            var provider = providers.get(id);
            if (provider == null) {
                picocli.error("[%s] Provider not found. Rolling Update is not available.".formatted(id));
                return CompatibilityResult.ExitCode.RECREATE.value();
            }

            var result = provider.isCompatible(Map.copyOf(info.getOrDefault(id, Map.of())));
            result.endMessage().ifPresent(picocli.getOutWriter()::println);

            if (Util.isNotCompatible(result)) {
                result.errorMessage().ifPresent(picocli::error);
                return result.exitCode();
            }
        }

        picocli.getOutWriter().println("[OK] Rolling Update is available.");
        return CompatibilityResult.ExitCode.ROLLING.value();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean includeRuntime() {
        return true;
    }

    @Override
    protected void validateConfig() {
        super.validateConfig();
        validateFileParameter();
    }

    private void validateFileParameter() {
        if (inputFile == null || inputFile.isBlank()) {
            throw new PropertyException("Missing required argument: " + INPUT_OPTION_NAME);
        }
        var file = new File(inputFile);
        if (!file.exists()) {
            throw new PropertyException("Incorrect argument %s. Path '%s' not found".formatted(INPUT_OPTION_NAME, file.getAbsolutePath()));
        }
        validateFileIsNotDirectory(file, INPUT_OPTION_NAME);
    }

    private Map<String, Map<String, String>> readServerInfo() {
        var file = new File(inputFile);
        try {
            return JsonSerialization.mapper.readValue(file, METADATA_TYPE_REF);
        } catch (IOException e) {
            throw new PropertyException("Unable to read file '%s'".formatted(file.getAbsolutePath()), e);
        }
    }
}
