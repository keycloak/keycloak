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

import static java.util.Optional.ofNullable;
import static java.util.stream.StreamSupport.stream;
import static org.keycloak.quarkus.runtime.Environment.isRebuildCheck;
import static org.keycloak.quarkus.runtime.Environment.isRebuilt;
import static org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand.OPTIMIZED_BUILD_OPTION_LONG;
import static org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource.parseConfigArgs;
import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getBuildTimeProperty;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getConfig;
import static org.keycloak.quarkus.runtime.Environment.isDevMode;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getCurrentBuiltTimeProperty;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getRawPersistedProperty;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getRuntimeProperty;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;
import org.keycloak.config.DeprecatedMetadata;
import org.keycloak.config.MultiOption;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.cli.command.AbstractCommand;
import org.keycloak.quarkus.runtime.cli.command.Build;
import org.keycloak.quarkus.runtime.cli.command.ImportRealmMixin;
import org.keycloak.quarkus.runtime.cli.command.Main;
import org.keycloak.quarkus.runtime.cli.command.ShowConfig;
import org.keycloak.quarkus.runtime.cli.command.StartDev;
import org.keycloak.quarkus.runtime.cli.command.Tools;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.PersistedConfigSource;
import org.keycloak.quarkus.runtime.configuration.PropertyMappingInterceptor;
import org.keycloak.quarkus.runtime.configuration.QuarkusPropertiesConfigSource;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.Environment;

import io.smallrye.config.ConfigValue;

import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.ArgGroupSpec;

public final class Picocli {

    public static final String ARG_PREFIX = "--";
    public static final String ARG_SHORT_PREFIX = "-";
    public static final String NO_PARAM_LABEL = "none";
    private static final String ARG_KEY_VALUE_SEPARATOR = "=";

    private static class IncludeOptions {
        boolean includeRuntime;
        boolean includeBuildTime;
    }

    private Picocli() {
    }

    public static void parseAndRun(List<String> cliArgs) {
        CommandLine cmd = createCommandLine(cliArgs);

        String[] argArray = cliArgs.toArray(new String[0]);
        if (Environment.isRebuildCheck()) {
            int exitCode = 0;
            try {
                // process the cli args first to init the config file and perform validation
                cmd.parseArgs(argArray);
                exitCode = runReAugmentationIfNeeded(cliArgs, cmd);
            } catch (ParameterException ex) {
                try {
                    exitCode = cmd.getParameterExceptionHandler().handleParseException(ex, argArray);
                } catch (Exception e) {
                    ExecutionExceptionHandler errorHandler = new ExecutionExceptionHandler();
                    errorHandler.error(cmd.getErr(), e.getMessage(), null);
                    exitCode = ex.getCommandLine().getCommandSpec().exitCodeOnInvalidInput();
                }
            }
            exitOnFailure(exitCode, cmd);
            return;
        }

        int exitCode = cmd.execute(argArray);
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

        CommandLine currentCommandSpec = getCurrentCommandSpec(cliArgs, cmd.getCommandSpec());

        if (currentCommandSpec == null) {
            return exitCode; // possible if using --version or the user made a mistake
        }

        String currentCommandName = currentCommandSpec.getCommandName();

        if (shouldSkipRebuild(cliArgs, currentCommandName)) {
            return exitCode;
        }

        if (currentCommandName.equals(StartDev.NAME)) {
            String profile = Environment.getProfile();

            if (profile == null) {
                // force the server image to be set with the dev profile
                Environment.forceDevProfile();
            }
        }
        if (requiresReAugmentation(currentCommandSpec)) {
            exitCode = runReAugmentation(cliArgs, cmd);
        }

        return exitCode;
    }

    private static boolean shouldSkipRebuild(List<String> cliArgs, String currentCommandName) {
        return cliArgs.contains("--help")
                || cliArgs.contains("-h")
                || cliArgs.contains("--help-all")
                || currentCommandName.equals(ShowConfig.NAME)
                || currentCommandName.equals(Tools.NAME);
    }

    private static boolean requiresReAugmentation(CommandLine cmdCommand) {
        if (hasConfigChanges(cmdCommand)) {
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
            checkChangesInBuildOptionsDuringAutoBuild();
        }

        int exitCode;

        List<String> configArgsList = new ArrayList<>(cliArgs);

        configArgsList.replaceAll(arg -> replaceCommandWithBuild(getCurrentCommandSpec(cliArgs, cmd.getCommandSpec()).getCommandName(), arg));
        configArgsList.removeIf(Picocli::isRuntimeOption);

        exitCode = cmd.execute(configArgsList.toArray(new String[0]));

        if(!isDevMode() && exitCode == cmd.getCommandSpec().exitCodeOnSuccess()) {
            cmd.getOut().printf("Next time you run the server, just run:%n%n\t%s %s %s %s%n%n", Environment.getCommand(), getCurrentCommandSpec(cliArgs, cmd.getCommandSpec()).getCommandName(), OPTIMIZED_BUILD_OPTION_LONG, String.join(" ", getSanitizedRuntimeCliOptions()));
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

    /**
     * Additional validation and handling of deprecated options
     *
     * @param cliArgs
     * @param abstractCommand
     */
    public static void validateConfig(List<String> cliArgs, AbstractCommand abstractCommand, PrintWriter out) {
        IncludeOptions options = getIncludeOptions(cliArgs, abstractCommand, abstractCommand.getName());

        if (!options.includeBuildTime && !options.includeRuntime) {
            return;
        }

        try {
            PropertyMappingInterceptor.disable(); // we don't want the mapped / transformed properties, we want what the user effectively supplied
            List<String> ignoredBuildTime = new ArrayList<>();
            List<String> ignoredRunTime = new ArrayList<>();
            Set<String> deprecatedInUse = new HashSet<>();
            for (OptionCategory category : abstractCommand.getOptionCategories()) {
                List<PropertyMapper> mappers = new ArrayList<>();
                Optional.ofNullable(PropertyMappers.getRuntimeMappers().get(category)).ifPresent(mappers::addAll);
                Optional.ofNullable(PropertyMappers.getBuildTimeMappers().get(category)).ifPresent(mappers::addAll);
                for (PropertyMapper<?> mapper : mappers) {
                    ConfigValue configValue = Configuration.getConfigValue(mapper.getFrom());

                    // don't consider missing or anything below standard env properties
                    if (configValue.getValue() == null || configValue.getConfigSourceOrdinal() < 300) {
                        continue;
                    }

                    if (mapper.isBuildTime() && !options.includeBuildTime) {
                        ignoredBuildTime.add(mapper.getFrom());
                        continue;
                    }
                    if (mapper.isRunTime() && !options.includeRuntime) {
                        ignoredRunTime.add(mapper.getFrom());
                        continue;
                    }

                    mapper.validate(configValue);

                    mapper.getDeprecatedMetadata().ifPresent(metadata -> {
                        handleDeprecated(deprecatedInUse, mapper, metadata);
                    });
                }
            }

            Logger logger = Logger.getLogger(Picocli.class); // logger can't be instantiated in a class field

            if (!ignoredBuildTime.isEmpty()) {
                outputIgnoredProperties(ignoredBuildTime, true, logger);
            } else if (!ignoredRunTime.isEmpty()) {
                outputIgnoredProperties(ignoredRunTime, false, logger);
            }

            if (!deprecatedInUse.isEmpty()) {
                logger.warn("The following used options are DEPRECATED and will be removed in a future release:\n" + String.join("\n", deprecatedInUse));
            }
        } finally {
            PropertyMappingInterceptor.enable();
        }
    }

    private static void handleDeprecated(Set<String> deprecatedInUse, PropertyMapper<?> mapper,
            DeprecatedMetadata metadata) {
        String optionName = mapper.getFrom();
        if (optionName.startsWith(NS_KEYCLOAK_PREFIX)) {
            optionName = optionName.substring(NS_KEYCLOAK_PREFIX.length());
        }

        StringBuilder sb = new StringBuilder("\t- ");
        sb.append(optionName);
        if (metadata.getNote() != null || !metadata.getNewOptionsKeys().isEmpty()) {
            sb.append(":");
        }
        if (metadata.getNote() != null) {
            sb.append(" ");
            sb.append(metadata.getNote());
            if (!metadata.getNote().endsWith(".")) {
                sb.append(".");
            }
        }
        if (!metadata.getNewOptionsKeys().isEmpty()) {
            sb.append(" Use ");
            sb.append(String.join(", ", metadata.getNewOptionsKeys()));
            sb.append(".");
        }
        deprecatedInUse.add(sb.toString());
    }

    private static void outputIgnoredProperties(List<String> properties, boolean build, Logger logger) {
        logger.warn(String.format("The following %s time non-cli options were found, but will be ignored during %s time: %s\n",
                build ? "build" : "run", build ? "run" : "build",
                properties.stream().collect(Collectors.joining(", "))));
    }

    private static boolean hasConfigChanges(CommandLine cmdCommand) {
        Optional<String> currentProfile = ofNullable(Environment.getProfile());
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

            // compare only the relevant options for this command, as not all options might be set for this command
            if (cmdCommand.getCommand() instanceof AbstractCommand) {
                AbstractCommand abstractCommand = cmdCommand.getCommand();
                PropertyMapper mapper = PropertyMappers.getMapper(propertyName);
                if (mapper != null) {
                    if (!abstractCommand.getOptionCategories().contains(mapper.getCategory())) {
                        continue;
                    }
                }
            }

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
            addCommandOptions(cliArgs, spec.subcommands().get(Build.NAME));
        }

        CommandLine cmd = new CommandLine(spec);

        cmd.setExecutionExceptionHandler(new ExecutionExceptionHandler());
        cmd.setParameterExceptionHandler(new ShortErrorMessageHandler());
        cmd.setHelpFactory(new HelpFactory());
        cmd.getHelpSectionMap().put(SECTION_KEY_COMMAND_LIST, new SubCommandListRenderer());
        cmd.setErr(new PrintWriter(System.err, true));

        return cmd;
    }

    private static IncludeOptions getIncludeOptions(List<String> cliArgs, AbstractCommand abstractCommand, String commandName) {
        IncludeOptions result = new IncludeOptions();
        if (abstractCommand == null) {
            return result;
        }
        result.includeRuntime = abstractCommand.includeRuntime();
        result.includeBuildTime = abstractCommand.includeBuildTime();

        if (!result.includeBuildTime && !result.includeRuntime) {
            return result;
        } else if (result.includeRuntime && !result.includeBuildTime && !ShowConfig.NAME.equals(commandName)) {
            result.includeBuildTime = isRebuilt() || !cliArgs.contains(OPTIMIZED_BUILD_OPTION_LONG);
        } else if (result.includeBuildTime && !result.includeRuntime) {
            result.includeRuntime = isRebuildCheck();
        }
        return result;
    }

    private static void addCommandOptions(List<String> cliArgs, CommandLine command) {
        if (command != null && command.getCommand() instanceof AbstractCommand) {
            IncludeOptions options = getIncludeOptions(cliArgs, command.getCommand(), command.getCommandName());

            if (!options.includeBuildTime && !options.includeRuntime) {
                return;
            }

            addOptionsToCli(command, options.includeBuildTime, options.includeRuntime);
        }
    }

    private static CommandLine getCurrentCommandSpec(List<String> cliArgs, CommandSpec spec) {
        for (String arg : cliArgs) {
            CommandLine command = spec.subcommands().get(arg);

            if (command != null) {
                return command;
            }
        }

        return null;
    }

    private static void addOptionsToCli(CommandLine commandLine, boolean includeBuildTime, boolean includeRuntime) {
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

        addMappedOptionsToArgGroups(commandLine, mappers);
    }

    private static void addMappedOptionsToArgGroups(CommandLine commandLine, Map<OptionCategory, List<PropertyMapper>> propertyMappers) {
        CommandSpec cSpec = commandLine.getCommandSpec();
        for(OptionCategory category : ((AbstractCommand) commandLine.getCommand()).getOptionCategories()) {
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
                        .description(getDecoratedOptionDescription(mapper))
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

    private static String getDecoratedOptionDescription(PropertyMapper<?> mapper) {
        StringBuilder transformedDesc = new StringBuilder(mapper.getDescription());

        if (mapper.getType() != Boolean.class && !mapper.getExpectedValues().isEmpty()) {
            transformedDesc.append(" Possible values are: " + String.join(", ", mapper.getExpectedValues()) + ".");
        }

        mapper.getDefaultValue().map(d -> " Default: " + d + ".").ifPresent(transformedDesc::append);

        mapper.getDeprecatedMetadata().ifPresent(deprecatedMetadata -> {
            List<String> deprecatedDetails = new ArrayList<>();
            String note = deprecatedMetadata.getNote();
            if (note != null) {
                if (!note.endsWith(".")) {
                    note += ".";
                }
                deprecatedDetails.add(note);
            }
            if (!deprecatedMetadata.getNewOptionsKeys().isEmpty()) {
                String s = deprecatedMetadata.getNewOptionsKeys().size() > 1 ? "s" : "";
                deprecatedDetails.add("Use the following option" + s + " instead: " + String.join(", ", deprecatedMetadata.getNewOptionsKeys()) + ".");
            }

            transformedDesc.insert(0, "@|bold DEPRECATED.|@ ");
            if (!deprecatedDetails.isEmpty()) {
                transformedDesc
                        .append(" @|bold ")
                        .append(String.join(" ", deprecatedDetails))
                        .append("|@");
            }
        });

        return transformedDesc.toString();
    }

    public static void println(CommandLine cmd, String message) {
        cmd.getOut().println(message);
    }

    public static List<String> parseArgs(String[] rawArgs) throws PropertyException {
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
                    if (!iterator.hasNext()) {
                        if (arg.startsWith("--spi")) {
                            throw new PropertyException(String.format("spi argument %s requires a value.", arg));
                        }
                        return args;
                    }
                    String next = iterator.next();

                    if (!next.startsWith("--")) {
                        // ignore the value if the arg is using space as separator
                        iterator.remove();
                    }
                }
            }
        }

        return args;
    }

    private static String replaceCommandWithBuild(String commandName, String arg) {
        if (arg.equals(commandName)) {
            return Build.NAME;
        }
        return arg;
    }

    private static boolean isRuntimeOption(String arg) {
        return arg.startsWith(ImportRealmMixin.IMPORT_REALM);
    }

    private static void checkChangesInBuildOptionsDuringAutoBuild() {
        if (Configuration.isOptimized()) {
            List<PropertyMapper> buildOptions = stream(Configuration.getPropertyNames(true).spliterator(), false)
                    .sorted()
                    .map(PropertyMappers::getMapper)
                    .filter(Objects::nonNull).collect(Collectors.toList());

            if (buildOptions.isEmpty()) {
                return;
            }

            StringBuilder options = new StringBuilder();

            for (PropertyMapper mapper : buildOptions) {
                String newValue = ofNullable(getCurrentBuiltTimeProperty(mapper.getFrom()))
                        .map(ConfigValue::getValue)
                        .orElse("<unset>");
                String currentValue = getRawPersistedProperty(mapper.getFrom()).get();

                if (newValue.equals(currentValue)) {
                    continue;
                }

                String name = mapper.getOption().getKey();

                options.append("\n\t- ")
                    .append(name).append("=").append(currentValue)
                    .append(" > ")
                    .append(name).append("=").append(newValue);
            }

            if (options.length() > 0) {
                System.out.println(
                        Ansi.AUTO.string(
                                new StringBuilder("@|bold,red ")
                                        .append("The previous optimized build will be overridden with the following build options:")
                                        .append(options)
                                        .append("\nTo avoid that, run the 'build' command again and then start the optimized server instance using the '--optimized' flag.")
                                        .append("|@").toString()
                        )
                );
            }
        }
    }
}
