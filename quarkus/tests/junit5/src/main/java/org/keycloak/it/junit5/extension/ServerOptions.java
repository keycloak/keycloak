/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.it.junit5.extension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.keycloak.quarkus.runtime.cli.command.Export;
import org.keycloak.quarkus.runtime.cli.command.Import;
import org.keycloak.quarkus.runtime.cli.command.ShowConfig;

import static org.keycloak.quarkus.runtime.cli.command.AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG;

final class ServerOptions extends ArrayList<String> {

    private static final Predicate<String> IGNORED_ARGUMENTS = ((Predicate<String>) s -> false)
            .or(OPTIMIZED_BUILD_OPTION_LONG::equals)
            .or(Export.NAME::equals)
            .or(Import.NAME::equals)
            .or("--help"::equals)
            .or("--help-all"::equals)
            .or("-h"::equals)
            .or(ShowConfig.NAME::equals);

    private boolean isBuildPhase = false;

    ServerOptions(Storage storageConfig, WithDatabase withDatabase, List<String> rawOptions) {
        if (rawOptions.isEmpty()) {
            return;
        }

        this.isBuildPhase = rawOptions.contains("build");

        for (Map.Entry<String, Predicate<String>> entry : getDefaultOptions(storageConfig, withDatabase).entrySet()) {
            if (contains(entry.getKey())) {
                continue;
            }

            if (!rawOptions.stream().anyMatch(entry.getValue())) {
                add(entry.getKey());
            }
        }

        addAll(0, rawOptions);
    }

    private Map<String, Predicate<String>> getDefaultOptions(Storage storageConfig, WithDatabase withDatabase) {
        Map<String, Predicate<String>> defaultOptions = new HashMap<>();

        if (!isBuildPhase) {
            defaultOptions.put("--cache=local", ignoreCacheLocal(storageConfig));
        }

        return defaultOptions;
    }

    private Predicate<String> ignoreCacheLocal(Storage storageConfig) {
        return new Predicate<String>() {
            @Override
            public boolean test(String arg) {
                return arg.contains("--cache") || storageConfig == null || !storageConfig.defaultLocalCache();
            }
        }.or(IGNORED_ARGUMENTS);
    }

}
