/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.quarkus.runtime.Environment;

import picocli.CommandLine;

import static org.keycloak.quarkus.runtime.cli.Picocli.NO_PARAM_LABEL;

public final class ImportRealmMixin {

    public static final String IMPORT_REALM = "--import-realm";

    @CommandLine.Option(names = IMPORT_REALM,
            description = "Import realms during startup by reading any realm configuration file from the 'data/import' directory.",
            paramLabel = NO_PARAM_LABEL,
            arity = "0")
    public void setImportRealm(boolean importRealm) {
        Environment.getHomePath().map(p -> p.resolve("data").resolve("import").toFile()).filter(File::exists)
                .map(File::getAbsolutePath).ifPresent(ExportImportConfig::setDir);
    }
}
