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

import static org.keycloak.quarkus.runtime.Environment.getHomePath;
import static org.keycloak.quarkus.runtime.Environment.isDevMode;
import static org.keycloak.quarkus.runtime.cli.Picocli.println;
import static org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource.getAllCliArgs;

import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.Messages;

import io.quarkus.bootstrap.runner.QuarkusEntryPoint;
import io.quarkus.bootstrap.runner.RunnerClassLoader;

import io.quarkus.runtime.configuration.ProfileManager;
import picocli.CommandLine.Command;

@Command(name = Build.NAME,
        header = "Creates a new and optimized server image.",
        description = {
            "%nCreates a new and optimized server image based on the configuration options passed to this command. Once created, the configuration will be persisted and read during startup without having to pass them over again.",
            "",
            "Some configuration options require this command to be executed in order to actually change a configuration. For instance",
            "",
            "- Change database vendor%n" +
            "- Enable/disable features%n" +
            "- Enable/Disable providers or set a default",
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
                + "  Enable metrics:%n%n"
                + "      $ ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} --metrics-enabled=true%n%n"
                + "  Change the relative path:%n%n"
                + "      $ ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} --http-relative-path=/auth%n%n"
                + "You can also use the \"--auto-build\" option when starting the server to avoid running this command every time you change a configuration:%n%n"
                + "    $ ${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} start --auto-build <OPTIONS>%n%n"
                + "By doing that you have an additional overhead when the server is starting.%n%n"
                + "Use '${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME} --help-all' to list all available options, including the start options.")
public final class Build extends AbstractCommand implements Runnable {

    public static final String NAME = "build";

    @Override
    public void run() {
        exitWithErrorIfDevProfileIsSetAndNotStartDev();

        System.setProperty("quarkus.launch.rebuild", "true");
        println(spec.commandLine(), "Updating the configuration and installing your custom providers, if any. Please wait.");

        try {
            beforeReaugmentationOnWindows();
            QuarkusEntryPoint.main();

            if (!isDevMode()) {
                println(spec.commandLine(), "Server configuration updated and persisted. Run the following command to review the configuration:\n");
                println(spec.commandLine(), "\t" + Environment.getCommand() + " show-config\n");
            }
        } catch (Throwable throwable) {
            executionError(spec.commandLine(), "Failed to update server configuration.", throwable);
        } finally {
            cleanTempResources();
        }
    }

    private void exitWithErrorIfDevProfileIsSetAndNotStartDev() {
        if (Environment.isDevProfile() && !getAllCliArgs().contains(StartDev.NAME)) {
            executionError(spec.commandLine(), Messages.devProfileNotAllowedError(NAME));
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
        if (!ProfileManager.getLaunchMode().isDevOrTest()) {
            // only needed for dev/testing purposes
            getHomePath().resolve("quarkus-artifact.properties").toFile().delete();
        }
    }
}
