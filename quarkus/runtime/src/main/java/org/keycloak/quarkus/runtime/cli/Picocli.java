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

package org.keycloak.quarkus.runtime.cli;

import static java.util.Arrays.asList;
import static org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand.AUTO_BUILD_OPTION;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getConfig;
import static org.keycloak.quarkus.runtime.configuration.PropertyMappers.isBuildTimeProperty;
import static org.keycloak.quarkus.runtime.Environment.isDevMode;
import static picocli.CommandLine.Model.UsageMessageSpec.SECTION_KEY_COMMAND_LIST;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.FileSystemException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.quarkus.runtime.cli.command.Build;
import org.keycloak.quarkus.runtime.cli.command.Main;
import org.keycloak.quarkus.runtime.cli.command.Start;
import org.keycloak.quarkus.runtime.cli.command.StartDev;
import org.keycloak.common.Profile;
import org.keycloak.quarkus.runtime.configuration.KeycloakConfigSourceProvider;
import org.keycloak.quarkus.runtime.configuration.Messages;
import org.keycloak.quarkus.runtime.configuration.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.PropertyMappers;
import org.keycloak.platform.Platform;
import org.keycloak.quarkus.runtime.InitializationException;
import org.keycloak.quarkus.runtime.integration.QuarkusPlatform;
import org.keycloak.quarkus.runtime.Environment;

import io.smallrye.config.ConfigValue;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.UnmatchedArgumentException;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.Model.OptionSpec;

public final class Picocli {

    private static final Logger logger = Logger.getLogger(Picocli.class);

    private static final String ARG_SEPARATOR = ";;";
    public static final String ARG_PREFIX = "--";
    public static final char ARG_KEY_VALUE_SEPARATOR = '=';
    public static final Pattern ARG_SPLIT = Pattern.compile(";;");
    public static final Pattern ARG_KEY_VALUE_SPLIT = Pattern.compile("=");

    private Picocli() {
    }

    public static void parseAndRun(List<String> cliArgs) {
        CommandLine cmd = createCommandLine(cliArgs);

        try {
            ParseResult result = cmd.parseArgs(cliArgs.toArray(new String[0]));

            if (!result.hasSubcommand() && !result.isUsageHelpRequested() && !result.isVersionHelpRequested()) {
                // if no command was set, the start command becomes the default
                cliArgs.add(0, Start.NAME);
            }
        } catch (UnmatchedArgumentException e) {
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

        runReAugmentationIfNeeded(cliArgs, cmd);

        int exitCode = cmd.execute(cliArgs.toArray(new String[0]));

        if (isDevMode()) {
            // do not exit if running in dev mode, otherwise quarkus dev mode will exit when running from IDE
            return;
        }

        System.exit(exitCode);
    }

    private static void runReAugmentationIfNeeded(List<String> cliArgs, CommandLine cmd) {
        if (cliArgs.contains(AUTO_BUILD_OPTION)) {
            if (requiresReAugmentation(cmd)) {
                runReAugmentation(cliArgs, cmd);
            }

            if (Boolean.getBoolean("kc.config.rebuild-and-exit")) {
                System.exit(cmd.getCommandSpec().exitCodeOnSuccess());
            }
        }
    }

    private static boolean requiresReAugmentation(CommandLine cmd) {
        if (hasConfigChanges()) {
            cmd.getOut().println("Changes detected in configuration. Updating the server image.");
            cmd.getOut().printf("For an optional runtime and bypass this step, please run the '%s' command prior to starting the server:%n%n\t%s %s %s%n",
                    Build.NAME,
                    Environment.getCommand(),
                    Build.NAME,
                    String.join(" ", asList(ARG_SPLIT.split(Environment.getConfigArgs()))) + "\n");

            return true;
        }

        return hasProviderChanges();
    }

    private static void runReAugmentation(List<String> cliArgs, CommandLine cmd) {
        if (StartDev.NAME.equals(cliArgs.get(0))) {
            String profile = Environment.getProfile();

            if (profile == null) {
                // force the server image to be set with the dev profile
                Environment.forceDevProfile();
            }
        }

        List<String> configArgsList = new ArrayList<>(cliArgs);

        if (!configArgsList.get(0).startsWith("--")) {
            configArgsList.remove(0);
        }

        configArgsList.remove("--auto-build");
        configArgsList.add(0, Build.NAME);

        cmd.execute(configArgsList.toArray(new String[0]));

        cmd.getOut().printf("Next time you run the server, just run:%n%n\t%s%n%n", Environment.getCommand());
    }

    private static boolean hasProviderChanges() {
        File propertiesFile = KeycloakConfigSourceProvider.getPersistedConfigFile().toFile();
        Map<String, File> deployedProviders = Environment.getProviderFiles();

        if (!propertiesFile.exists()) {
            return !deployedProviders.isEmpty();
        }

        Properties properties = new Properties();

        try (InputStream is = new FileInputStream(propertiesFile)) {
            properties.load(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load persisted properties", e);
        }

        Set<String> providerKeys = properties.stringPropertyNames().stream().filter(Picocli::isProviderKey).collect(
                Collectors.toSet());

        if (deployedProviders.size() != providerKeys.size()) {
            return true;
        }

        for (String key : providerKeys) {
            String fileName = key.substring("kc.provider.file".length() + 1, key.lastIndexOf('.'));

            if (!deployedProviders.containsKey(fileName)) {
                return true;
            }

            File file = deployedProviders.get(fileName);
            String lastModified = properties.getProperty(key);

            if (!lastModified.equals(String.valueOf(file.lastModified()))) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasConfigChanges() {
        Optional<String> currentProfile = Optional.ofNullable(Environment.getProfile());
        Optional<String> persistedProfile = Environment.getBuiltTimeProperty("kc.profile");

        if (!persistedProfile.orElse("").equals(currentProfile.orElse(""))) {
            return true;
        }

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

    private static boolean isProviderKey(String key) {
        return key.startsWith("kc.provider.file");
    }

    private static CommandLine createCommandLine(List<String> cliArgs) {
        CommandSpec spec = CommandSpec.forAnnotatedObject(new Main())
                .name(Environment.getCommand());

        spec.usageMessage().width(100);

        boolean addBuildOptionsToStartCommand = cliArgs.contains(AUTO_BUILD_OPTION);

        addOption(spec, Start.NAME, addBuildOptionsToStartCommand);
        addOption(spec, StartDev.NAME, true);
        addOption(spec, Build.NAME, true);

        for (Profile.Feature feature : Profile.Feature.values()) {
            addOption(spec.subcommands().get(Build.NAME).getCommandSpec(), "--features-" + feature.name().toLowerCase(),
                    "Enables the " + feature.name() + " feature. Set enabled to enable the feature or disabled otherwise.", null);
        }
        
        CommandLine cmd = new CommandLine(spec);

        cmd.getHelpSectionMap().put(SECTION_KEY_COMMAND_LIST, new SubCommandListRenderer());
        cmd.setExecutionExceptionHandler(new ExecutionExceptionHandler());

        return cmd;
    }

    public static String parseConfigArgs(List<String> argsList) {
        StringBuilder options = new StringBuilder();
        Iterator<String> iterator = argsList.iterator();
        boolean expectValue = false;
        List<String> ignoredArgs = asList("--verbose", "-v", "--help", "-h", AUTO_BUILD_OPTION);

        while (iterator.hasNext()) {
            String key = iterator.next();

            // TODO: ignore properties for providers for now, need to fetch them from the providers, otherwise CLI will complain about invalid options
            // change this once we are able to obtain properties from providers
            if (key.startsWith("--spi")) {
                iterator.remove();
            }

            if (ignoredArgs.contains(key)) {
                continue;
            }

            if (key.startsWith(ARG_PREFIX)) {
                if (options.length() > 0) {
                    options.append(ARG_SEPARATOR);
                }

                options.append(key);

                if (key.indexOf(ARG_KEY_VALUE_SEPARATOR) == -1) {
                    // values can be set using spaces (e.g.: --option <value>)
                    expectValue = true;
                }
            } else if (expectValue) {
                options.append(ARG_KEY_VALUE_SEPARATOR).append(key);
                expectValue = false;
            }
        }

        return options.toString();
    }

    private static void addOption(CommandSpec spec, String command, boolean includeBuildTime) {
        CommandSpec commandSpec = spec.subcommands().get(command).getCommandSpec();
        List<PropertyMapper> mappers = new ArrayList<>(PropertyMappers.getRuntimeMappers());

        if (includeBuildTime) {
            mappers.addAll(PropertyMappers.getBuiltTimeMappers());
        }

        for (PropertyMapper mapper : mappers) {
            String name = ARG_PREFIX + PropertyMappers.toCLIFormat(mapper.getFrom()).substring(3);
            String description = mapper.getDescription();

            if (description == null || commandSpec.optionsMap().containsKey(name)) {
                continue;
            }

            addOption(commandSpec, name, description, mapper);
        }

        addOption(commandSpec, "--features", "Enables a group of features. Possible values are: "
                + String.join(",", Arrays.stream(Profile.Type.values()).map(
                type -> type.name().toLowerCase()).toArray((IntFunction<CharSequence[]>) String[]::new)), null);
    }

    private static void addOption(CommandSpec commandSpec, String name, String description, PropertyMapper mapper) {
        OptionSpec.Builder builder = OptionSpec.builder(name)
                .description(description)
                .paramLabel(name.substring(2))
                .type(String.class);

        if (mapper != null) {
            builder.completionCandidates(mapper.getExpectedValues());
        }

        commandSpec.addOption(builder.build());
    }

    public static List<String> getCliArgs(CommandLine cmd) {
        ParseResult parseResult = cmd.getParseResult();

        if (parseResult == null) {
            return Collections.emptyList();
        }

        return parseResult.expandedArgs();
    }

    public static void error(List<String> cliArgs, PrintWriter errorWriter, String message, Throwable throwable) {
        logError(errorWriter, "ERROR: " + message);

        if (throwable != null) {
            boolean verbose = cliArgs.contains("--verbose") || cliArgs.contains("-v");

            if (throwable instanceof InitializationException) {
                InitializationException initializationException = (InitializationException) throwable;
                if (initializationException.getSuppressed() == null || initializationException.getSuppressed().length == 0) {
                    dumpException(errorWriter, initializationException, verbose);
                } else if (initializationException.getSuppressed().length == 1) {
                    dumpException(errorWriter, initializationException.getSuppressed()[0], verbose);
                } else {
                    logError(errorWriter, "ERROR: Multiple configuration errors during startup");
                    int counter = 0;
                    for (Throwable inner : initializationException.getSuppressed()) {
                        counter++;
                        logError(errorWriter, "ERROR " + counter);
                        dumpException(errorWriter, inner, verbose);
                    }
                }
            } else {
                dumpException(errorWriter, throwable, verbose);
            }

            if (!verbose) {
                logError(errorWriter, "For more details run the same command passing the '--verbose' option. Also you can use '--help' to see the details about the usage of the particular command.");
            }
        }

        System.exit(1);
    }

    public static void error(CommandLine cmd, String message, Throwable throwable) {
        error(getCliArgs(cmd), cmd.getErr(), message, throwable);
    }

    public static void error(CommandLine cmd, String message) {
        error(getCliArgs(cmd), cmd.getErr(), message, null);
    }

    public static void println(CommandLine cmd, String message) {
        cmd.getOut().println(message);
    }

    private static void dumpException(PrintWriter errorWriter, Throwable cause, boolean verbose) {
        if (verbose) {
            logError(errorWriter, "ERROR: Details:", cause);
        } else {
            do {
                if (cause.getMessage() != null) {
                    logError(errorWriter, String.format("ERROR: %s", cause.getMessage()));
                }
                printErrorHints(errorWriter, cause);
            } while ((cause = cause.getCause())!= null);
        }
        printErrorHints(errorWriter, cause);
    }

    private static void printErrorHints(PrintWriter errorWriter, Throwable cause) {
        if (cause instanceof FileSystemException) {
            FileSystemException fse = (FileSystemException) cause;
            ConfigValue httpsCertFile = getConfig().getConfigValue("kc.https.certificate.file");

            if (fse.getFile().equals(Optional.ofNullable(httpsCertFile.getValue()).orElse(null))) {
                logError(errorWriter, Messages.httpsConfigurationNotSet().getMessage());
            }
        }
    }

    private static void logError(PrintWriter errorWriter, String errorMessage) {
        logError(errorWriter, errorMessage, null);
    }

    // The "cause" can be null
    private static void logError(PrintWriter errorWriter, String errorMessage, Throwable cause) {
        QuarkusPlatform platform = (QuarkusPlatform) Platform.getPlatform();
        if (platform.isStarted()) {
            // Can delegate to proper logger once the platform is started
            if (cause == null) {
                logger.error(errorMessage);
            } else {
                logger.error(errorMessage, cause);
            }
        } else {
            if (cause == null) {
                errorWriter.println(errorMessage);
            } else {
                errorWriter.println(errorMessage);
                cause.printStackTrace();
            }
        }
    }
}
