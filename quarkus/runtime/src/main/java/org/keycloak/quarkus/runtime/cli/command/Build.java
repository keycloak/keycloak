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

import java.util.Optional;

import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.Messages;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.PersistedConfigSource;

import io.quarkus.bootstrap.runner.RunnerClassLoader;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.ConfigValue;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import static org.keycloak.config.ClassLoaderOptions.QUARKUS_REMOVED_ARTIFACTS_PROPERTY;
import static org.keycloak.config.DatabaseOptions.DB;
import static org.keycloak.quarkus.runtime.Environment.getHomePath;
import static org.keycloak.quarkus.runtime.Environment.isDevProfile;

@Command(name = Build.NAME,
        header = "Creates a new and optimized server image.",
        description = {
            "%nCreates a new and optimized server image based on the configuration options passed to this command. Once created, the configuration will be persisted and read during startup without having to pass them over again.",
            "",
            "Consider running this command before running the server in production for an optimal runtime."
        },
        footerHeading = "Examples:",
        footer = "  Change the database vendor:%n%n"
                + "      $ ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} --db=postgres%n%n"
                + "  Enable a feature:%n%n"
                + "      $ ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} --features=<feature_name>%n%n"
                + "  Or alternatively, enable all tech preview features:%n%n"
                + "      $ ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} --features=preview%n%n"
                + "  Enable health endpoints:%n%n"
                + "      $ ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} --health-enabled=true%n%n"
                + "  Enable metrics endpoints:%n%n"
                + "      $ ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} --metrics-enabled=true%n%n"
                + "  Change the relative path:%n%n"
                + "      $ ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} --http-relative-path=/auth%n")
public final class Build extends AbstractCommand {

    public static final String NAME = "build";

    @CommandLine.Mixin
    HelpAllMixin helpAllMixin;

    @CommandLine.Mixin
    DryRunMixin dryRunMixin;

    @Override
    protected void runCommand() {
        checkProfileAndDb();

        // validate before setting that we're rebuilding so that runtime options are still seen
        PersistedConfigSource.getInstance().runWithDisabled(() -> {
            validateConfig();
            return null;
        });
        Environment.setRebuild();

        picocli.println("Updating the configuration and installing your custom providers, if any. Please wait.");

        try {
            configureBuildClassLoader();

            beforeReaugmentationOnWindows();
            if (!Boolean.TRUE.equals(dryRunMixin.dryRun)) {
                picocli.build();
            } else if (DryRunMixin.isDryRunBuild()) {
                PersistedConfigSource.getInstance().saveDryRunProperties();
            }

            if (!isDevProfile()) {
                picocli.println("Server configuration updated and persisted. Run the following command to review the configuration:\n");
                picocli.println("\t" + Environment.getCommand() + " show-config\n");
            }
        } catch (Throwable throwable) {
            executionError(spec.commandLine(), "Failed to update server configuration.", throwable);
        } finally {
            cleanTempResources();
        }
    }

    private static void configureBuildClassLoader() {
        // ignored artifacts must be set prior to starting re-augmentation
        Optional.ofNullable(Configuration.getNonPersistedConfigValue(QUARKUS_REMOVED_ARTIFACTS_PROPERTY))
                .map(ConfigValue::getValue)
                .ifPresent(s -> System.setProperty(QUARKUS_REMOVED_ARTIFACTS_PROPERTY, s));
    }

    @Override
    public boolean includeBuildTime() {
        return true;
    }

    private void checkProfileAndDb() {
        if (Environment.isDevProfile()) {
            String cmd = picocli.getParsedCommand().map(AbstractCommand::getName).orElse(getName());
            // we allow start-dev, and import|export|bootstrap-admin --profile=dev
            // but not start --profile=dev, nor build --profile=dev
            if (Start.NAME.equals(cmd) || Build.NAME.equals(cmd)) {
                executionError(spec.commandLine(), Messages.devProfileNotAllowedError(cmd));
            }
        } else if (Configuration.getConfigValue(DB).getConfigSourceOrdinal() == 0) {
            picocli.warn("Usage of the default value for the db option in the production profile is deprecated. Please explicitly set the db instead.");
        }
    }

    private void beforeReaugmentationOnWindows() {
        // On Windows, files generated during re-augmentation are locked and can't be re-created.
        // To workaround this behavior, we reset the internal cache of the runner classloader and force files
        // to be closed prior to re-augmenting the application
        // See KEYCLOAK-16218
        if (Environment.isWindows()) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            if (classLoader instanceof RunnerClassLoader) {
                ((RunnerClassLoader) classLoader).resetInternalCaches();
            }
        }
    }

    private void cleanTempResources() {
        if (!LaunchMode.current().isDevOrTest()) {
            // only needed for dev/testing purposes
            getHomePath().ifPresent(path -> path.resolve("quarkus-artifact.properties").toFile().delete());
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
