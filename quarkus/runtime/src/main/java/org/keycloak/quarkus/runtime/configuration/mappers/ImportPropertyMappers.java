/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.keycloak.config.ImportOptions;
import org.keycloak.exportimport.Strategy;
import picocli.CommandLine;

import java.util.Optional;

import static org.keycloak.exportimport.ExportImportConfig.PROVIDER;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class ImportPropertyMappers {

    private ImportPropertyMappers() {
    }

    public static PropertyMapper<?>[] getMappers() {
        return new PropertyMapper[] {
                fromOption(ImportOptions.FILE)
                        .to("kc.spi-import-importer")
                        .transformer(ImportPropertyMappers::transformImporter)
                        .paramLabel("file")
                        .build(),
                fromOption(ImportOptions.FILE)
                        .to("kc.spi-import-single-file-file")
                        .paramLabel("file")
                        .build(),
                fromOption(ImportOptions.DIR)
                        .to("kc.spi-import-dir-dir")
                        .paramLabel("dir")
                        .build(),
                fromOption(ImportOptions.OVERRIDE)
                        .to("kc.spi-import-single-file-strategy")
                        .transformer(ImportPropertyMappers::transformOverride)
                        .build(),
                fromOption(ImportOptions.OVERRIDE)
                        .to("kc.spi-import-dir-strategy")
                        .transformer(ImportPropertyMappers::transformOverride)
                        .build(),
        };
    }

    private static Optional<String> transformOverride(Optional<String> option, ConfigSourceInterceptorContext context) {
        if (option.isPresent() && Boolean.parseBoolean(option.get())) {
            return Optional.of(Strategy.OVERWRITE_EXISTING.name());
        } else {
            return Optional.of(Strategy.IGNORE_EXISTING.name());
        }
    }

    private static Optional<String> transformImporter(Optional<String> option, ConfigSourceInterceptorContext context) {
        ConfigValue importer = context.proceed("kc.spi-import-importer");
        if (importer != null) {
            return Optional.of(importer.getValue());
        }
        if (option.isPresent()) {
            return Optional.of("singleFile");
        }
        ConfigValue dirConfigValue = context.proceed("kc.spi-import-dir-dir");
        if (dirConfigValue != null && dirConfigValue.getValue() != null) {
            return Optional.of("dir");
        }
        ConfigValue dirValue = context.proceed("kc.dir");
        if (dirConfigValue != null && dirValue.getValue() != null) {
            return Optional.of("dir");
        }
        if (System.getProperty(PROVIDER) == null) {
            throw new CommandLine.PicocliException("Must specify either --dir or --file options.");
        }
        return Optional.empty();
    }

}
