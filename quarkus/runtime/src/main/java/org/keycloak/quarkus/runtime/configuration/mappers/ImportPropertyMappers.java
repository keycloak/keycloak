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

import java.util.List;

import org.keycloak.config.ImportOptions;
import org.keycloak.config.Option;
import org.keycloak.config.OptionBuilder;
import org.keycloak.config.OptionCategory;
import org.keycloak.exportimport.Strategy;
import org.keycloak.quarkus.runtime.cli.Picocli;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.cli.command.Import;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

import static org.keycloak.exportimport.ExportImportConfig.PROVIDER;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalValue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public final class ImportPropertyMappers implements PropertyMapperGrouping {
    private static final String IMPORTER_PROPERTY = "kc.spi-import--importer";
    private static final String SINGLE_FILE = "singleFile";
    private static final String DIR = "dir";

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        return List.of(
                fromOption(IMPORTER_PLACEHOLDER)
                        .to(IMPORTER_PROPERTY)
                        .transformer(ImportPropertyMappers::transformImporter)
                        .paramLabel("file")
                        .build(),
                fromOption(ImportOptions.FILE)
                        .to("kc.spi-import--single-file--file")
                        .paramLabel("file")
                        .isEnabled(c -> c instanceof Import)
                        .build(),
                fromOption(ImportOptions.DIR)
                        .to("kc.spi-import--dir--dir")
                        .paramLabel("dir")
                        .isEnabled(c -> c instanceof Import)
                        .build(),
                fromOption(ImportOptions.OVERRIDE)
                        .to("kc.spi-import--single-file--strategy")
                        .transformer(ImportPropertyMappers::transformOverride)
                        .isEnabled(ImportPropertyMappers::isSingleFileProvider)
                        .build(),
                fromOption(ImportOptions.OVERRIDE)
                        .to("kc.spi-import--dir--strategy")
                        .transformer(ImportPropertyMappers::transformOverride)
                        .isEnabled(ImportPropertyMappers::isDirProvider)
                        .build()
        );
    }

    @Override
    public void validateConfig(Picocli picocli) {
        if (picocli.getParsedCommand().orElse(null) instanceof Import && getOptionalValue(IMPORTER_PROPERTY).isEmpty() && System.getProperty(PROVIDER) == null) {
            throw new PropertyException("Must specify either --dir or --file options.");
        }
    }

    private static final Option<String> IMPORTER_PLACEHOLDER = new OptionBuilder<>("importer", String.class)
            .category(OptionCategory.IMPORT)
            .description("Placeholder for determining import mode")
            .buildTime(false)
            .hidden()
            .build();

    private static boolean isSingleFileProvider() {
        return isProvider(SINGLE_FILE);
    }

    private static boolean isDirProvider() {
        return !isSingleFileProvider();
    }

    private static boolean isProvider(String provider) {
        return getOptionalValue(IMPORTER_PROPERTY)
                .filter(provider::equals)
                .isPresent();
    }

    private static String transformOverride(String option, ConfigSourceInterceptorContext context) {
        if (Boolean.parseBoolean(option)) {
            return Strategy.OVERWRITE_EXISTING.name();
        } else {
            return Strategy.IGNORE_EXISTING.name();
        }
    }

    private static String transformImporter(String option, ConfigSourceInterceptorContext context) {
        ConfigValue importer = context.proceed(IMPORTER_PROPERTY);
        if (importer != null) {
            return importer.getValue();
        }

        var file = getOptionalValue("kc.spi-import--single-file--file").map(f -> SINGLE_FILE);
        var dir = getOptionalValue("kc.spi-import--dir--dir")
                .or(() -> getOptionalValue("kc.dir"))
                .map(f -> DIR);

        // Only one option can be specified
        boolean xor = file.isPresent() ^ dir.isPresent();

        return xor ? file.or(() -> dir).get() : null;
    }

}
