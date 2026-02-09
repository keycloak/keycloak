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

import org.keycloak.config.ExportOptions;
import org.keycloak.config.Option;
import org.keycloak.config.OptionBuilder;
import org.keycloak.config.OptionCategory;
import org.keycloak.exportimport.UsersExportStrategy;
import org.keycloak.quarkus.runtime.cli.Picocli;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.cli.command.Export;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

import static org.keycloak.exportimport.ExportImportConfig.PROVIDER;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalValue;
import static org.keycloak.quarkus.runtime.configuration.Configuration.isBlank;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public final class ExportPropertyMappers implements PropertyMapperGrouping {
    private static final String EXPORTER_PROPERTY = "kc.spi-export--exporter";
    private static final String SINGLE_FILE = "singleFile";
    private static final String DIR = "dir";

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        return List.of(
                fromOption(EXPORTER_PLACEHOLDER)
                        .to(EXPORTER_PROPERTY)
                        .transformer(ExportPropertyMappers::transformExporter)
                        .paramLabel("file")
                        .build(),
                fromOption(ExportOptions.FILE)
                        .to("kc.spi-export--single-file--file")
                        .paramLabel("file")
                        .isEnabled(c -> c instanceof Export)
                        .build(),
                fromOption(ExportOptions.DIR)
                        .to("kc.spi-export--dir--dir")
                        .paramLabel("dir")
                        .isEnabled(c -> c instanceof Export)
                        .build(),
                fromOption(ExportOptions.REALM)
                        .to("kc.spi-export--single-file--realm-name")
                        .isEnabled(ExportPropertyMappers::isSingleFileProvider)
                        .paramLabel("realm")
                        .build(),
                fromOption(ExportOptions.REALM)
                        .to("kc.spi-export--dir--realm-name")
                        .isEnabled(ExportPropertyMappers::isDirProvider)
                        .paramLabel("realm")
                        .build(),
                fromOption(ExportOptions.USERS)
                        .to("kc.spi-export--dir--users-export-strategy")
                        .addValidator(ExportPropertyMappers::validateUsersUsage)
                        .paramLabel("strategy")
                        .build(),
                fromOption(ExportOptions.USERS_PER_FILE)
                        .to("kc.spi-export--dir--users-per-file")
                        .isEnabled(ExportPropertyMappers::isDirProvider)
                        .paramLabel("number")
                        .build()
        );
    }

    private static void validateUsersUsage(PropertyMapper<?> mapper, ConfigValue value) {
        if (!isBlank(ExportOptions.FILE) && isBlank(ExportOptions.DIR)) {
            var sameFileIsSpecified = UsersExportStrategy.SAME_FILE.toString().toLowerCase().equals(value.getValue());

            if (!sameFileIsSpecified) {
                throw new PropertyException("Property '--users' can be used only when exporting to a directory, or value set to 'same_file' when exporting to a file.");
            }
        }
    }

    @Override
    public void validateConfig(Picocli picocli) {
        if (picocli.getParsedCommand().orElse(null) instanceof Export && getOptionalValue(EXPORTER_PROPERTY).isEmpty() && System.getProperty(PROVIDER) == null) {
            throw new PropertyException("Must specify either --dir or --file options.");
        }
    }

    private static final Option<String> EXPORTER_PLACEHOLDER = new OptionBuilder<>("exporter", String.class)
            .category(OptionCategory.EXPORT)
            .description("Placeholder for determining export mode")
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
        return Configuration.getOptionalValue(EXPORTER_PROPERTY)
                .filter(provider::equals)
                .isPresent();
    }

    private static String transformExporter(String option, ConfigSourceInterceptorContext context) {
        ConfigValue exporter = context.proceed(EXPORTER_PROPERTY);
        if (exporter != null) {
            return exporter.getValue();
        }

        var file = Configuration.getOptionalValue("kc.spi-export--single-file--file").map(f -> SINGLE_FILE);
        var dir = Configuration.getOptionalValue("kc.spi-export--dir--dir")
                .or(() -> Configuration.getOptionalValue("kc.dir"))
                .map(f -> DIR);

        // Only one option can be specified
        boolean xor = file.isPresent() ^ dir.isPresent();

        return xor ? file.or(() -> dir).get() : null;
    }

}
