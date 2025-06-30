/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import static org.keycloak.exportimport.ExportImportConfig.ACTION_IMPORT;

import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.configuration.mappers.ImportPropertyMappers;
import picocli.CommandLine.Command;

import java.util.List;
import java.util.stream.Collectors;

@Command(name = Import.NAME,
        header = "Import data from a directory or a file.",
        description = "%nImport data from a directory or a file.")
public final class Import extends AbstractExportImportCommand implements Runnable {

    public static final String NAME = "import";

    public Import() {
        super(ACTION_IMPORT);
    }

    @Override
    public List<OptionCategory> getOptionCategories() {
        return super.getOptionCategories().stream().filter(optionCategory ->
                optionCategory != OptionCategory.EXPORT).collect(Collectors.toList());
    }

    @Override
    public void validateConfig() {
        ImportPropertyMappers.validateConfig();
        super.validateConfig();
    }

    @Override
    public String getName() {
        return NAME;
    }

}
