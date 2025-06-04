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
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Predicate;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.compatibility.CompatibilityMetadataProvider;
import org.keycloak.config.ConfigProviderFactory;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import picocli.CommandLine;

public abstract class AbstractUpdatesCommand extends AbstractCommand implements Runnable {

    private static final int FEATURE_DISABLED_EXIT_CODE = 4;

    @CommandLine.Mixin
    HelpAllMixin helpAllMixin;

    @CommandLine.Mixin
    OptimizedMixin optimizedMixin;

    @Override
    public List<OptionCategory> getOptionCategories() {
        return super.getOptionCategories().stream()
                .filter(Predicate.not(OptionCategory.EXPORT::equals))
                .filter(Predicate.not(OptionCategory.IMPORT::equals))
                .toList();
    }

    @Override
    public void run() {
        Environment.updateProfile(true);
        if (!Profile.isAnyVersionOfFeatureEnabled(Profile.Feature.ROLLING_UPDATES_V1)) {
            printFeatureDisabled();
            picocli.exit(FEATURE_DISABLED_EXIT_CODE);
            return;
        }
        loadConfiguration();
        printPreviewWarning();
        validateConfig();
        var exitCode = executeAction();
        picocli.exit(exitCode);
    }

    abstract int executeAction();

    static void validateFileIsNotDirectory(File file, String option) {
        if (file.isDirectory()) {
            throw new PropertyException("Incorrect argument %s. Path '%s' is not a valid file.".formatted(option, file.getAbsolutePath()));
        }
    }

    void printOut(String message) {
        var cmd = getCommandLine();
        if (cmd.isPresent()) {
            cmd.get().getOut().println(message);
        } else {
            System.out.println(message);
        }
    }

    void printError(String message) {
        var cmd = getCommandLine();
        if (cmd.isPresent()) {
            var colorScheme = cmd.get().getColorScheme();
            cmd.get().getErr().println(colorScheme.errorText(message));
        } else {
            System.err.println(message);
        }
    }

    private void printPreviewWarning() {
        if (Profile.isFeatureEnabled(Profile.Feature.ROLLING_UPDATES_V2) && (Profile.Feature.ROLLING_UPDATES_V2.getType() == Profile.Feature.Type.PREVIEW || Profile.Feature.ROLLING_UPDATES_V2.getType() == Profile.Feature.Type.EXPERIMENTAL)) {
            printError("Warning! This command is '" + Profile.Feature.ROLLING_UPDATES_V2.getType() + "' and is not recommended for use in production. It may change or be removed at a future release.");
        }
    }

    void printFeatureDisabled() {
        printError("Unable to use this command. None of the versions of the feature '" + Profile.Feature.ROLLING_UPDATES_V1.getUnversionedKey() + "' is enabled.");
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
        // Initialize config
        var configProvider = ServiceLoader.load(ConfigProviderFactory.class)
                .stream()
                .findFirst()
                .map(ServiceLoader.Provider::get)
                .flatMap(ConfigProviderFactory::create)
                .orElseThrow(() -> new RuntimeException("Failed to load Keycloak Configuration"));
        Config.init(configProvider);
    }

}
