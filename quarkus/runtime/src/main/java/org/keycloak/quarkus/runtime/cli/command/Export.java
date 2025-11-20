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

import java.util.EnumSet;

import org.keycloak.config.OptionCategory;
import org.keycloak.exportimport.ExportImportConfig;

import picocli.CommandLine.Command;

import static org.keycloak.exportimport.ExportImportConfig.ACTION_EXPORT;

@Command(name = Export.NAME,
        header = "Export data from realms to a file or directory.",
        description = "%nExport data from realms to a file or directory.")
public final class Export extends AbstractNonServerCommand {

    public static final String NAME = "export";

    @Override
    protected void doBeforeRun() {
        System.setProperty(ExportImportConfig.ACTION, ACTION_EXPORT);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected EnumSet<OptionCategory> excludedCategories() {
        return EnumSet.of(OptionCategory.IMPORT);
    }

}
