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

import static org.keycloak.quarkus.runtime.cli.Picocli.error;
import static org.keycloak.quarkus.runtime.cli.Picocli.println;

import org.keycloak.quarkus.runtime.Environment;

import io.quarkus.bootstrap.runner.QuarkusEntryPoint;
import io.quarkus.bootstrap.runner.RunnerClassLoader;
import picocli.CommandLine;

@CommandLine.Command(name = Build.NAME,
        header = "Creates a new and optimized server image based on the configuration options passed to this command.",
        description = {
            "Creates a new and optimized server image based on the configuration options passed to this command. Once created, configuration will be read from the server image and the server can be started without passing the same options again.",
            "",
            "Some configuration options require this command to be executed in order to actually change a configuration. For instance, the database vendor."
        },
        mixinStandardHelpOptions = true,
        usageHelpAutoWidth = true,
        optionListHeading = "%nOptions%n",
        parameterListHeading = "Available Commands%n")
public final class Build extends AbstractCommand implements Runnable {

    public static final String NAME = "build";

    @Override
    public void run() {
        System.setProperty("quarkus.launch.rebuild", "true");
        println(spec.commandLine(), "Updating the configuration and installing your custom providers, if any. Please wait.");

        try {
            beforeReaugmentationOnWindows();
            QuarkusEntryPoint.main();
            println(spec.commandLine(), "Server configuration updated and persisted. Run the following command to review the configuration:\n");
            println(spec.commandLine(), "\t" + Environment.getCommand() + " show-config\n");
        } catch (Throwable throwable) {
            error(spec.commandLine(), "Failed to update server configuration.", throwable);
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
                RunnerClassLoader.class.cast(classLoader).resetInternalCaches();
            }
        }
    }
}
