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

import static org.keycloak.quarkus.runtime.Environment.isRebuildCheck;
import static org.keycloak.quarkus.runtime.Environment.isRebuilt;
import static org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand.*;
import static org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand.AUTO_BUILD_OPTION_LONG;
import static org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource.parseConfigArgs;
import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getBuildTimeProperty;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getConfig;
import static org.keycloak.quarkus.runtime.Environment.isDevMode;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getRuntimeProperty;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers.formatValue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers.isBuildTimeProperty;
import static org.keycloak.utils.StringUtil.isNotBlank;
import static picocli.CommandLine.Model.UsageMessageSpec.SECTION_KEY_COMMAND_LIST;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.keycloak.config.MultiOption;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand;
import org.keycloak.quarkus.runtime.cli.command.Build;
import org.keycloak.quarkus.runtime.cli.command.ImportRealmMixin;
import org.keycloak.quarkus.runtime.cli.command.Main;
import org.keycloak.quarkus.runtime.cli.command.Start;
import org.keycloak.quarkus.runtime.cli.command.StartDev;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.PersistedConfigSource;
import org.keycloak.quarkus.runtime.configuration.QuarkusPropertiesConfigSource;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.Environment;

import io.smallrye.config.ConfigValue;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.ArgGroupSpec;

public final class Picocli {

    public static final String ARG_PREFIX = "--";
    public static final String ARG_SHORT_PREFIX = "-";
    public static final String NO_PARAM_LABEL = "none";
    private static final String ARG_KEY_VALUE_SEPARATOR = "=";

    private Picocli() {
    }

    public static void parseAndRun(List<String> cliArgs) {
        CommandLine cmd = createCommandLine(cliArgs);

        if (Environment.isRebuildCheck()) {
            int exitCode = runReAugmentationIfNeeded(cliArgs, cmd);
            exitOnFailure(exitCode, cmd);
            return;
        }

        int exitCode = cmd.execute(cliArgs.toArray(new String[0]));
        exitOnFailure(exitCode, cmd);
    }

    private static void exitOnFailure(int exitCode, CommandLine cmd) {
        if (exitCode != cmd.getCommandSpec().exitCodeOnSuccess() && !Environment.isTestLaunchMode() || isRebuildCheck()) {
            // hard exit wanted, as build failed and no subsequent command should be executed. no quarkus involved.
            System.exit(exitCode);
        }
    }

    private static int runReAugmentationIfNeeded(List<String> cliArgs, CommandLine cmd) {
        int exitCode = 0;

        if (!isHelpCommand(cliArgs)) {
            if (cliArgs.contains(StartDev.NAME)) {
                String profile = Environment.getProfile();

                if (profile == null) {
                    // force the server image to be set with the dev profile
                    Environment.forceDevProfile();
                }
            }
            if (requiresReAugmentation(cmd)) {
                exitCode = runReAugmentation(cliArgs, cmd);
            }
        }
        return exitCode;
    }

    private static boolean isHelpCommand(List<String> cliArgs) {
        return cliArgs.contains("--help") || cliArgs.contains("-h") || cliArgs.contains("--help-all");
    }

    public static boolean requiresReAugmentation(CommandLine cmd) {
        if (hasConfigChanges()) {
            if (!ConfigArgsConfigSource.getAllCliArgs().contains(StartDev.NAME) && "dev".equals(getConfig().getOptionalValue("kc.profile", String.class).orElse(null))) {
                return false;
            }

            return true;
        }

        return hasProviderChanges();
    }

    /**
     * checks the raw cli input for possible credentials / properties which should be masked,
     * and masks them.
     * @return a list of potentially masked properties in CLI format, e.g. `--db-password=*******`
     * instead of the actual passwords value.
     */
    private static List<String> getSanitizedRuntimeCliOptions() {
        List<String> properties = new ArrayList<>();

        parseConfigArgs(new BiConsumer<String, String>() {
            @Override
            public void accept(String key, String value) {
                PropertyMapper mapper = PropertyMappers.getMapper(key);

                if (mapper != null && mapper.isBuildTime()) {
                    return;
                }

                properties.add(key + "=" + formatValue(key, value));
            }
        });

        return properties;
    }

    private static int runReAugmentation(List<String> cliArgs, CommandLine cmd) {
        if(!isDevMode() && cmd != null) {
            cmd.getOut().println("Changes detected in configuration. Updating the server image.");
        }

        int exitCode = 0;

        List<String> configArgsList = new ArrayList<>(cliArgs);

        configArgsList.replaceAll(Picocli::replaceStartWithBuild);
        configArgsList.removeIf(Picocli::isRuntimeOption);

        exitCode = cmd.execute(configArgsList.toArray(new String[0]));

        if(!isDevMode() && exitCode == cmd.getCommandSpec().exitCodeOnSuccess()) {
            cmd.getOut().printf("Next time you run the server, just run:%n%n\t%s %s %s %s%n%n", Environment.getCommand(), Start.NAME, OPTIMIZED_BUILD_OPTION_LONG, String.join(" ", getSanitizedRuntimeCliOptions()));
        }

        return exitCode;
    }

    private static boolean hasProviderChanges() {
        Map<String, String> persistedProps = PersistedConfigSource.getInstance().getProperties();
        Map<String, File> deployedProviders = Environment.getProviderFiles();

        if (persistedProps.isEmpty()) {
            return !deployedProviders.isEmpty();
        }

        Set<String> providerKeys = persistedProps.keySet().stream().filter(Picocli::isProviderKey).collect(Collectors.toSet());

        if (deployedProviders.size() != providerKeys.size()) {
            return true;
        }

        for (String key : providerKeys) {
            String fileName = key.substring("kc.provider.file".length() + 1, key.lastIndexOf('.'));

            if (!deployedProviders.containsKey(fileName)) {
                return true;
            }

            File file = deployedProviders.get(fileName);
            String lastModified = persistedProps.get(key);

            if (!lastModified.equals(String.valueOf(file.lastModified()))) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasConfigChanges() {
        Optional<String> currentProfile = Optional.ofNullable(Environment.getProfile());
        Optional<String> persistedProfile = getBuildTimeProperty("kc.profile");

        if (!persistedProfile.orElse("").equals(currentProfile.orElse(""))) {
            return true;
        }

        for (String propertyName : getConfig().getPropertyNames()) {
            // only check keycloak build-time properties
            if (!isBuildTimeProperty(propertyName)) {
                continue;
            }

            ConfigValue configValue = getConfig().getConfigValue(propertyName);

            if (configValue == null || configValue.getConfigSourceName() == null) {
                continue;
            }

            // try to resolve any property set using profiles
            if (propertyName.startsWith("%")) {
                propertyName = propertyName.substring(propertyName.indexOf('.') + 1);
            }

            String persistedValue = getBuildTimeProperty(propertyName).orElse("");
            String runtimeValue = getRuntimeProperty(propertyName).orElse(null);

            if (runtimeValue == null && isNotBlank(persistedValue)) {
                PropertyMapper mapper = PropertyMappers.getMapper(propertyName);

                if (mapper != null && persistedValue.equals(mapper.getDefaultValue().map(Object::toString).orElse(null))) {
                    // same as default
                    continue;
                }

                // probably because it was unset
                return true;
            }

            // changes to a single property is enough to indicate changes to configuration
            if (!persistedValue.equals(runtimeValue)) {
                return true;
            }
        }

        //check for defined quarkus raw build properties for UserStorageProvider extensions
        if (QuarkusPropertiesConfigSource.getConfigurationFile() != null) {
            Optional<ConfigSource> quarkusPropertiesConfigSource = getConfig().getConfigSource(QuarkusPropertiesConfigSource.NAME);

            if (quarkusPropertiesConfigSource.isPresent()) {
                Map<String, String> foundQuarkusBuildProperties = findSupportedRawQuarkusBuildProperties(quarkusPropertiesConfigSource.get().getProperties().entrySet());

                //only check if buildProps are found in quarkus properties file.
                if (!foundQuarkusBuildProperties.isEmpty()) {
                    Optional<ConfigSource> persistedConfigSource = getConfig().getConfigSource(PersistedConfigSource.NAME);

                    if(persistedConfigSource.isPresent()) {
                        for(String key : foundQuarkusBuildProperties.keySet()) {
                            if (notContainsKey(persistedConfigSource.get(), key)) {
                                //if persisted cs does not contain raw quarkus key from quarkus.properties, assume build is needed as the key is new.
                                return true;
                            }
                        }

                        //if it contains the key, check if the value actually changed from the persisted one.
                        return hasAtLeastOneChangedBuildProperty(foundQuarkusBuildProperties, persistedConfigSource.get().getProperties().entrySet());
                    }
                }
            }
        }

        return false;
    }

    private static boolean hasAtLeastOneChangedBuildProperty(Map<String, String> foundQuarkusBuildProperties, Set<Map.Entry<String, String>> persistedEntries) {
        for(Map.Entry<String, String> persistedEntry : persistedEntries) {
            if (foundQuarkusBuildProperties.containsKey(persistedEntry.getKey())) {
                return isChangedValue(foundQuarkusBuildProperties, persistedEntry);
            }
        }

        return false;
    }

    private static boolean notContainsKey(ConfigSource persistedConfigSource, String key) {
        return !persistedConfigSource.getProperties().containsKey(key);
    }

    private static Map<String, String> findSupportedRawQuarkusBuildProperties(Set<Map.Entry<String, String>> entries) {
        Pattern buildTimePattern = Pattern.compile(QuarkusPropertiesConfigSource.QUARKUS_DATASOURCE_BUILDTIME_REGEX);
        Map<String, String> result = new HashMap<>();

        for(Map.Entry<String, String> entry : entries) {
            if (buildTimePattern.matcher(entry.getKey()).matches()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private static boolean isChangedValue(Map<String, String> foundQuarkusBuildProps, Map.Entry<String, String> persistedEntry) {
        return !foundQuarkusBuildProps.get(persistedEntry.getKey()).equals(persistedEntry.getValue());
    }

    private static boolean isProviderKey(String key) {
        return key.startsWith("kc.provider.file");
    }

    public static CommandLine createCommandLine(List<String> cliArgs) {
        CommandSpec spec = CommandSpec.forAnnotatedObject(new Main(), new DefaultFactory()).name(Environment.getCommand());

        for (CommandLine subCommand : spec.subcommands().values()) {
            CommandSpec subCommandSpec = subCommand.getCommandSpec();

            // help option added to any subcommand
            subCommandSpec.addOption(OptionSpec.builder(Help.OPTION_NAMES)
                    .usageHelp(true)
                    .description("This help message.")
                    .build());
        }

        addCommandOptions(cliArgs, getCurrentCommandSpec(cliArgs, spec));

        if (isRebuildCheck()) {
            // build command should be available when running re-aug
            addCommandOptions(cliArgs, spec.subcommands().get(Build.NAME).getCommandSpec());
        }

        CommandLine cmd = new CommandLine(spec);

        cmd.setExecutionExceptionHandler(new ExecutionExceptionHandler());
        cmd.setParameterExceptionHandler(new ShortErrorMessageHandler());
        cmd.setHelpFactory(new HelpFactory());
        cmd.getHelpSectionMap().put(SECTION_KEY_COMMAND_LIST, new SubCommandListRenderer());
        cmd.setErr(new PrintWriter(System.err, true));

        return cmd;
    }

    private static void addCommandOptions(List<String> cliArgs, CommandSpec command) {
        if (command != null) {
            boolean includeBuildTime = false;
            boolean includeRuntime = false;

            if (Start.NAME.equals(command.name()) || StartDev.NAME.equals(command.name())) {
                includeBuildTime = isRebuilt() || !cliArgs.contains(OPTIMIZED_BUILD_OPTION_LONG);
                includeRuntime = true;
            } else if (Build.NAME.equals(command.name())) {
                includeBuildTime = true;
                includeRuntime = isRebuildCheck();
            }

            addOptionsToCli(command, includeBuildTime, includeRuntime);
        }
    }

    private static CommandSpec getCurrentCommandSpec(List<String> cliArgs, CommandSpec spec) {
        for (String arg : cliArgs) {
            CommandLine command = spec.subcommands().get(arg);

            if (command != null) {
                return command.getCommandSpec();
            }
        }

        return null;
    }

    private static void addOptionsToCli(CommandSpec commandSpec, boolean includeBuildTime, boolean includeRuntime) {
        Map<OptionCategory, List<PropertyMapper>> mappers = new EnumMap<>(OptionCategory.class);

        if (includeRuntime) {
            mappers.putAll(PropertyMappers.getRuntimeMappers());
        }

        if (includeBuildTime) {
            for (Map.Entry<OptionCategory, List<PropertyMapper>> entry : PropertyMappers.getBuildTimeMappers()
                    .entrySet()) {
                List<PropertyMapper> result = new ArrayList<>(mappers.getOrDefault(entry.getKey(), Collections.emptyList()));

                result.addAll(entry.getValue());

                mappers.put(entry.getKey(), result);
            }
        }

        addMappedOptionsToArgGroups(commandSpec, mappers);
    }

    private static void addMappedOptionsToArgGroups(CommandSpec cSpec, Map<OptionCategory, List<PropertyMapper>> propertyMappers) {
        for(OptionCategory category : OptionCategory.values()) {
            List<PropertyMapper> mappersInCategory = propertyMappers.get(category);

            if (mappersInCategory == null) {
                //picocli raises an exception when an ArgGroup is empty, so ignore it when no mappings found for a category.
                continue;
            }

            ArgGroupSpec.Builder argGroupBuilder = ArgGroupSpec.builder()
                    .heading(category.getHeading() + ":")
                    .order(category.getOrder())
                    .validate(false);

            for (PropertyMapper mapper: mappersInCategory) {
                String name = mapper.getCliFormat();
                String description = mapper.getDescription();

                if (description == null || cSpec.optionsMap().containsKey(name) || name.endsWith(OPTION_PART_SEPARATOR)) {
                    //when key is already added or has no description, don't add.
                    continue;
                }

                OptionSpec.Builder optBuilder = OptionSpec.builder(name)
                        .description(description)
                        .paramLabel(mapper.getParamLabel())
                        .completionCandidates(new Iterable<String>() {
                            @Override
                            public Iterator<String> iterator() {
                                return mapper.getExpectedValues().iterator();
                            }
                        })
                        .parameterConsumer(PropertyMapperParameterConsumer.INSTANCE)
                        .hidden(mapper.isHidden());

                if (mapper.getDefaultValue().isPresent()) {
                    optBuilder.defaultValue(mapper.getDefaultValue().get().toString());
                }

                if (mapper.getType() != null) {
                    optBuilder.type(mapper.getType());
                    if (mapper.getOption() instanceof MultiOption) {
                        optBuilder.auxiliaryTypes(((MultiOption<?>) mapper.getOption()).getAuxiliaryType());
                    }
                } else {
                    optBuilder.type(String.class);
                }

                argGroupBuilder.addArg(optBuilder.build());
            }

            if (argGroupBuilder.args().isEmpty()) {
                continue;
            }

            cSpec.addArgGroup(argGroupBuilder.build());
        }
    }

    public static void println(CommandLine cmd, String message) {
        cmd.getOut().println(message);
    }

    public static List<String> parseArgs(String[] rawArgs) {
        if (rawArgs.length == 0) {
            return List.of();
        }

        // makes sure cli args are available to the config source
        ConfigArgsConfigSource.setCliArgs(rawArgs);
        List<String> args = new ArrayList<>(List.of(rawArgs));
        Iterator<String> iterator = args.iterator();

        while (iterator.hasNext()) {
            String arg = iterator.next();

            if (arg.startsWith("--spi") || arg.startsWith("-D")) {
                // TODO: ignore properties for providers for now, need to fetch them from the providers, otherwise CLI will complain about invalid options
                // also ignores system properties as they are set when starting the JVM
                // change this once we are able to obtain properties from providers
                iterator.remove();

                if (!arg.contains(ARG_KEY_VALUE_SEPARATOR)) {
                    String next = iterator.next();

                    if (!next.startsWith("--")) {
                        // ignore the value if the arg is using space as separator
                        iterator.remove();
                    }
                }
            }

            if (!isRebuildCheck() && (arg.startsWith(AbstractStartCommand.AUTO_BUILD_OPTION_SHORT) || arg.startsWith(AUTO_BUILD_OPTION_LONG))) {
                System.out.println(DEFAULT_WARN_MESSAGE_REPEATED_AUTO_BUILD_OPTION);
            }
        }

        return args;
    }

    private static String replaceStartWithBuild(String arg) {
        if (arg.equals(Start.NAME) || arg.equals(StartDev.NAME)) {
            return Build.NAME;
        }
        return arg;
    }

    private static boolean isRuntimeOption(String arg) {
        // remove this once auto-build option is removed
        if (AUTO_BUILD_OPTION_LONG.equals(arg) || AUTO_BUILD_OPTION_SHORT.equals(arg)) {
            return true;
        }

        return arg.startsWith(ImportRealmMixin.IMPORT_REALM);
    }
}
