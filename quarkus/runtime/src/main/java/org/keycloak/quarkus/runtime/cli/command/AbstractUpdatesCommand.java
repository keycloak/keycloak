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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.compatibility.CompatibilityMetadataProvider;
import org.keycloak.config.ConfigProviderFactory;
import org.keycloak.quarkus.runtime.cli.PropertyException;

import picocli.CommandLine;

public abstract class AbstractUpdatesCommand extends AbstractAutoBuildCommand {

    @CommandLine.Mixin
    OptimizedMixin optimizedMixin = new OptimizedMixin();

    @Override
    public boolean shouldStart() {
        return false;
    }

    @Override
    protected Optional<Integer> callCommand() {
        return super.callCommand().or(() -> {
            if (!Profile.isAnyVersionOfFeatureEnabled(Profile.Feature.ROLLING_UPDATES_V1)) {
                printFeatureDisabled();
                return Optional.of(FEATURE_DISABLED_EXIT_CODE);
            }
            loadConfiguration();
            printPreviewWarning();
            validateConfig();
            return Optional.of(executeAction());
        });
    }

    abstract int executeAction();

    static void validateFileIsNotDirectory(File file, String option) {
        if (file.isDirectory()) {
            throw new PropertyException("Incorrect argument %s. Path '%s' is not a valid file.".formatted(option, file.getAbsolutePath()));
        }
    }

    private void printPreviewWarning() {
        if (Profile.isFeatureEnabled(Profile.Feature.ROLLING_UPDATES_V2) && (Profile.Feature.ROLLING_UPDATES_V2.getType() == Profile.Feature.Type.PREVIEW || Profile.Feature.ROLLING_UPDATES_V2.getType() == Profile.Feature.Type.EXPERIMENTAL)) {
            picocli.error("Warning! This command is '" + Profile.Feature.ROLLING_UPDATES_V2.getType() + "' and is not recommended for use in production. It may change or be removed at a future release.");
        }
    }

    void printFeatureDisabled() {
        picocli.error("Unable to use this command. None of the versions of the feature '" + Profile.Feature.ROLLING_UPDATES_V1.getUnversionedKey() + "' is enabled.");
    }

    static Map<String, CompatibilityMetadataProvider> loadAllProviders() {
        Map<String, CompatibilityMetadataProvider> providers = new HashMap<>();
        for (var p : ServiceLoader.load(CompatibilityMetadataProvider.class)) {
            providers.merge(p.getId(), p, (existing, current) -> {
                if (existing.priority() == current.priority()) {
                    throw new IllegalArgumentException("Unable to handle two providers with the same id (%s) and priority.".formatted(existing.getId()));
                }
                // If a user wants to replace default providers with their own.
                return existing.priority() < current.priority() ?
                        current :
                        existing;
            });
        }
        return providers;
    }

    private static void loadConfiguration() {
        // Initialize config without directly referencing MicroProfileConfigProvider
        // as that currently causing classloading issue during command creation
        var configProvider = ServiceLoader.load(ConfigProviderFactory.class)
                .stream()
                .findFirst()
                .map(ServiceLoader.Provider::get)
                .flatMap(ConfigProviderFactory::create)
                .orElseThrow(() -> new RuntimeException("Failed to load Keycloak Configuration"));
        Config.init(configProvider);
    }

}
