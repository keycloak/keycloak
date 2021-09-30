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

package org.keycloak.cli;

import static org.keycloak.cli.MainCommand.BUILD_COMMAND;
import static org.keycloak.cli.MainCommand.START_COMMAND;
import static org.keycloak.cli.MainCommand.isStartDevCommand;
import static org.keycloak.cli.Picocli.createCommandLine;
import static org.keycloak.cli.Picocli.error;
import static org.keycloak.cli.Picocli.getCliArgs;
import static org.keycloak.cli.Picocli.parseConfigArgs;
import static org.keycloak.configuration.Configuration.getConfig;
import static org.keycloak.configuration.PropertyMappers.isBuildTimeProperty;
import static org.keycloak.util.Environment.getProfileOrDefault;
import static org.keycloak.util.Environment.isDevMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import io.quarkus.runtime.Quarkus;
import org.keycloak.common.Version;

import io.quarkus.runtime.annotations.QuarkusMain;

import org.keycloak.configuration.KeycloakConfigSourceProvider;
import org.keycloak.util.Environment;

import picocli.CommandLine;

/**
 * <p>The main entry point, responsible for initialize and run the CLI as well as start the server.
 * 
 * <p>For optimal startup of the server, the server should be configured first by running the {@link MainCommand#reAugment(Boolean)} 
 * command so that subsequent server starts, without any args, do not need to build the CLI, which is costly.
 */
@QuarkusMain(name = "keycloak")
public class KeycloakMain {

    public static void main(String[] args) {
        System.setProperty("kc.version", Version.VERSION_KEYCLOAK);
        List<String> cliArgs = new ArrayList<>(Arrays.asList(args));
        System.setProperty(Environment.CLI_ARGS, parseConfigArgs(cliArgs));

        if (cliArgs.isEmpty()) {
            // no arguments, just start the server
            start(cliArgs, new PrintWriter(System.err));
            if (!isDevMode()) {
                System.exit(CommandLine.ExitCode.OK);
            }
            return;
        }

        // parse arguments and execute any of the configured commands
        parseAndRun(cliArgs);
    }

    static void start(CommandLine cmd) {
        start(getCliArgs(cmd), cmd.getErr());
    }

    private static void start(List<String> cliArgs, PrintWriter errorWriter) {
        try {
            Quarkus.run(null, (integer, cause) -> {
                if (cause != null) {
                    error(cliArgs, errorWriter,
                            String.format("Failed to start server using profile (%s)", getProfileOrDefault("none")),
                            cause.getCause());
                }
            });
        } catch (Throwable cause) {
            error(cliArgs, errorWriter,
                    String.format("Unexpected error when starting the server using profile (%s)", getProfileOrDefault("none")),
                    cause.getCause());
        }

        Quarkus.waitForExit();
    }
    
    private static void parseAndRun(List<String> cliArgs) {
        CommandLine cmd = createCommandLine();

        try {
            CommandLine.ParseResult result = cmd.parseArgs(cliArgs.toArray(new String[0]));

            if (result.hasSubcommand()) {
                if (isStartDevCommand(result.subcommand().commandSpec())) {
                    String profile = Environment.getProfile();

                    if (profile == null) {
                        // force the server image to be set with the dev profile
                        Environment.forceDevProfile();
                    }

                    runReAugmentationIfNeeded(cliArgs, cmd);
                }
            } else if ((!result.isUsageHelpRequested() && !result.isVersionHelpRequested())) {
                // if no command was set, the start command becomes the default
                cliArgs.add(0, START_COMMAND);
            }
        } catch (CommandLine.UnmatchedArgumentException e) {
            // if no command was set but options were provided, the start command becomes the default
            if (!cmd.getParseResult().hasSubcommand() && cliArgs.get(0).startsWith("--")) {
                cliArgs.add(0, "start");
            } else {
                cmd.getErr().println(e.getMessage());
                System.exit(cmd.getCommandSpec().exitCodeOnInvalidInput());
            }
        } catch (Exception e) {
            cmd.getErr().println(e.getMessage());
            System.exit(cmd.getCommandSpec().exitCodeOnExecutionException());
        }

        int exitCode = cmd.execute(cliArgs.toArray(new String[0]));

        if (!isDevMode()) {
            System.exit(exitCode);
        }
    }

    private static void runReAugmentationIfNeeded(List<String> cliArgs, CommandLine cmd) {
        if (Boolean.getBoolean("kc.dev.rebuild")) {
            if (requiresReAugmentation(cliArgs)) {
                runReAugmentation(cliArgs, cmd);
            }
            System.exit(cmd.getCommandSpec().exitCodeOnSuccess());
        }
    }

    private static boolean requiresReAugmentation(List<String> cliArgs) {
        if (hasConfigChanges()) {
            System.out.printf("Changes detected in configuration. Updating the server image.\n");

            List<String> suggestedArgs = cliArgs.subList(1, cliArgs.size());

            suggestedArgs.removeAll(Arrays.asList("--verbose", "--help"));

            System.out.printf("For an optional runtime and bypass this step, please run the '" + BUILD_COMMAND + "' command prior to starting the server:\n\n\t%s config %s\n",
                    Environment.getCommand(),
                    String.join(" ", suggestedArgs) + "\n");

            return true;
        }

        return hasProviderChanges();
    }

    private static void runReAugmentation(List<String> cliArgs, CommandLine cmd) {
        List<String> configArgsList = new ArrayList<>(cliArgs);

        if (!configArgsList.get(0).startsWith("--")) {
            configArgsList.remove(0);
        }

        configArgsList.add(0, BUILD_COMMAND);

        cmd.execute(configArgsList.toArray(new String[0]));

        System.out.printf("Next time you run the server, just run:\n\n\t%s\n\n", Environment.getCommand());
    }

    public static boolean hasProviderChanges() {
        File propertiesFile = KeycloakConfigSourceProvider.getPersistedConfigFile().toFile();
        File[] providerFiles = Environment.getProviderFiles();

        if (!propertiesFile.exists()) {
            return providerFiles.length > 0;
        }

        Properties properties = new Properties();

        try (InputStream is = new FileInputStream(propertiesFile)) {
            properties.load(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load persisted properties", e);
        }

        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("kc.provider.file")) {
                if (providerFiles.length == 0) {
                    return true;
                }

                String fileName = key.substring("kc.provider.file".length() + 1, key.lastIndexOf('.'));
                String lastModified = properties.getProperty(key);

                for (File file : providerFiles) {
                    if (file.getName().equals(fileName) && !lastModified.equals(String.valueOf(file.lastModified()))) {
                        return true;
                    }
                }

                return false;
            }
        }

        return providerFiles.length > 0;
    }

    public static boolean hasConfigChanges() {
        for (String propertyName : getConfig().getPropertyNames()) {
            // only check keycloak build-time properties
            if (!isBuildTimeProperty(propertyName)) {
                continue;
            }

            // try to resolve any property set using profiles
            if (propertyName.startsWith("%")) {
                propertyName = propertyName.substring(propertyName.indexOf('.') + 1);
            }

            String currentValue = Environment.getBuiltTimeProperty(propertyName).orElse(null);
            String newValue = getConfig().getConfigValue(propertyName).getValue();

            if (newValue != null && !newValue.equalsIgnoreCase(currentValue)) {
                // changes to a single property are enough to indicate changes to configuration
                return true;
            }
        }

        return false;
    }

}
