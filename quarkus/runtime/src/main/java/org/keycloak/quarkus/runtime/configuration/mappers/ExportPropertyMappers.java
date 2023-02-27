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
import org.keycloak.config.ExportOptions;

import java.util.Optional;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class ExportPropertyMappers {

    private ExportPropertyMappers() {
    }

    public static PropertyMapper<?>[] getMappers() {
        return new PropertyMapper[] {
                fromOption(ExportOptions.FILE)
                        .to("kc.spi-export-exporter")
                        .transformer(ExportPropertyMappers::transformExporter)
                        .paramLabel("file")
                        .build(),
                fromOption(ExportOptions.FILE)
                        .to("kc.spi-export-single-file-file")
                        .paramLabel("file")
                        .build(),
                fromOption(ExportOptions.DIR)
                        .to("kc.spi-export-dir-dir")
                        .paramLabel("dir")
                        .build(),
                fromOption(ExportOptions.REALM)
                        .to("kc.spi-export-single-file-realm-name")
                        .paramLabel("realm")
                        .build(),
                fromOption(ExportOptions.REALM)
                        .to("kc.spi-export-dir-realm-name")
                        .paramLabel("realm")
                        .build(),
                fromOption(ExportOptions.USERS)
                        .to("kc.spi-export-dir-users-export-strategy")
                        .paramLabel("strategy")
                        .build(),
                fromOption(ExportOptions.USERS_PER_FILE)
                        .to("kc.spi-export-dir-users-per-file")
                        .paramLabel("number")
                        .build()
        };
    }

    private static Optional<String> transformExporter(Optional<String> option, ConfigSourceInterceptorContext context) {
        if (option.isPresent()) {
            return Optional.of("singleFile");
        }
        ConfigValue dirConfigValue = context.proceed("kc.spi-export-dir-dir");
        if (dirConfigValue != null && dirConfigValue.getValue() != null) {
            return Optional.of("dir");
        }
        return Optional.empty();
    }

}
