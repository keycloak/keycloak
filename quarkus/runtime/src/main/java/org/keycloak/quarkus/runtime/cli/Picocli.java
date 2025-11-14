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

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import org.keycloak.common.profile.ProfileException;
import org.keycloak.config.DeprecatedMetadata;
import org.keycloak.config.Option;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.KeycloakMain;
import org.keycloak.quarkus.runtime.Messages;
import org.keycloak.quarkus.runtime.cli.command.AbstractAutoBuildCommand;
import org.keycloak.quarkus.runtime.cli.command.AbstractCommand;
import org.keycloak.quarkus.runtime.cli.command.AbstractNonServerCommand;
import org.keycloak.quarkus.runtime.cli.command.HelpAllMixin;
import org.keycloak.quarkus.runtime.cli.command.Main;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.DisabledMappersInterceptor;
import org.keycloak.quarkus.runtime.configuration.KcUnmatchedArgumentException;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;
import org.keycloak.quarkus.runtime.configuration.PropertyMappingInterceptor;
import org.keycloak.quarkus.runtime.configuration.QuarkusPropertiesConfigSource;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import io.quarkus.bootstrap.runner.QuarkusEntryPoint;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.ConfigValue;
import io.smallrye.mutiny.tuples.Functions.TriConsumer;
import picocli.CommandLine;
import picocli.CommandLine.DuplicateOptionAnnotationsException;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Help.Ansi.Style;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.MissingParameterException;
import picocli.CommandLine.Model.ArgGroupSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.ISetter;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParseResult;

import static java.lang.String.format;

import static org.keycloak.quarkus.runtime.Environment.getProviderFiles;
import static org.keycloak.quarkus.runtime.Environment.isRebuildCheck;
import static org.keycloak.quarkus.runtime.cli.OptionRenderer.decorateDuplicitOptionName;
import static org.keycloak.quarkus.runtime.cli.command.AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG;
import static org.keycloak.quarkus.runtime.configuration.Configuration.isUserModifiable;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

import static picocli.CommandLine.Model.UsageMessageSpec.SECTION_KEY_COMMAND_LIST;

public class Picocli {

    static final String PROVIDER_TIMESTAMP_ERROR = "A provider JAR was updated since the last build, please rebuild for this to be fully utilized.";
    static final String PROVIDER_TIMESTAMP_WARNING = "A provider jar has a different timestamp than when the optimized container image was created. If you are changing provider jars after the build, you must run another build to properly account for those modifications.";
    static final String KC_PROVIDER_FILE_PREFIX = "kc.provider.file.";
    public static final String ARG_PREFIX = "--";
    public static final String ARG_SHORT_PREFIX = "-";
    public static final String NO_PARAM_LABEL = "none";

    private static class IncludeOptions {
        boolean includeRuntime;
        boolean includeBuildTime;
    }

    private final ExecutionExceptionHandler errorHandler = new ExecutionExceptionHandler();
    private Optional<AbstractCommand> parsedCommand = Optional.empty();
    private boolean warnedTimestampChanged;

    private Ansi colorMode = hasColorSupport() ? Ansi.ON : Ansi.OFF;

    public static boolean hasColorSupport() {
        return QuarkusConsole.hasColorSupport();
    }

    public Ansi getColorMode() {
        return colorMode;
    }

    private boolean isHelpRequested(ParseResult result) {
        if (result.isUsageHelpRequested()) {
            return true;
        }

        return result.subcommands().stream().anyMatch(this::isHelpRequested);
    }

    public void parseAndRun(List<String> cliArgs) {
        List<String> unrecognizedArgs = new ArrayList<>();
        CommandLine cmd = createCommandLine(unrecognizedArgs);

        String[] argArray = cliArgs.toArray(new String[0]);

        try {
            ParseResult result = cmd.parseArgs(argArray);

            var commandLineList = result.asCommandLineList();

            CommandLine cl = commandLineList.get(commandLineList.size() - 1);

            AbstractCommand currentCommand = null;
            if (cl.getCommand() instanceof AbstractCommand ac) {
                currentCommand = ac;
            }
            initConfig(cliArgs, currentCommand);

            if (!unrecognizedArgs.isEmpty()) {
                IncludeOptions options = Optional.ofNullable(currentCommand).map(c -> getIncludeOptions(cliArgs, c, c.getName())).orElse(new IncludeOptions());
                Set<OptionCategory> allowedCategories = Set.copyOf(Optional.ofNullable(currentCommand).map(AbstractCommand::getOptionCategories).orElse(List.of()));
                // TODO: further refactor this as these args should be the source for ConfigArgsConfigSource
                unrecognizedArgs.removeIf(arg -> {
                    boolean hasArg = false;
                    if (arg.contains("=")) {
                        arg = arg.substring(0, arg.indexOf("="));
                        hasArg = true;
                    }
                    PropertyMapper<?> mapper = PropertyMappers.getMapperByCliKey(arg);
                    if (mapper != null) {
                        if (!allowedCategories.contains(mapper.getCategory()) || (mapper.isBuildTime() && !options.includeBuildTime) || (mapper.isRunTime() && !options.includeRuntime)) {
                            return false;
                        }
                        if (!hasArg) {
                            addCommandOptions(cliArgs, cl);
                            throw new MissingParameterException(cl, cl.getCommandSpec().optionsMap().get(arg), null);
                        }
                        return true;
                    }
                    return false;
                });
                if (!unrecognizedArgs.isEmpty()) {
                    addCommandOptions(cliArgs, cl);
                    throw new KcUnmatchedArgumentException(cl, unrecognizedArgs);
                }
            }

            if (isHelpRequested(result)) {
                addCommandOptions(cliArgs, cl);
            }

            // ParseResult retain memory. Clear it, so it's not on the stack while the command runs
            result = null;

            // there's another ParseResult being created under the covers here.
            // to reuse the previous result either means we need to duplicate the logic in the execute method
            // or refactor the above logic so that it happens in the command logic
            // We could also reduce the memory footprint of the ParseResult, but that looks a little hackish
            int exitCode = cmd.execute(argArray);

            exit(exitCode);
        } catch (ParameterException parEx) {
            catchParameterException(parEx, cmd, argArray);
        } catch (ProfileException | PropertyException proEx) {
            usageException(proEx.getMessage(), proEx.getCause());
        }
    }

    public Optional<AbstractCommand> getParsedCommand() {
        return parsedCommand;
    }

    private void catchParameterException(ParameterException parEx, CommandLine cmd, String[] args) {
        int exitCode;
        try {
            exitCode = cmd.getParameterExceptionHandler().handleParseException(parEx, args);
        } catch (Exception e) {
            errorHandler.error(cmd.getErr(), e.getMessage(), null);
            exitCode = parEx.getCommandLine().getCommandSpec().exitCodeOnInvalidInput();
        }
        exit(exitCode);
    }

    public void usageException(String message, Throwable cause) {
        errorHandler.error(getErrWriter(), message, cause);
        exit(CommandLine.ExitCode.USAGE);
    }

    public void exit(int exitCode) {
        System.exit(exitCode);
    }

    private static boolean wasBuildEverRun() {
        return !Configuration.getRawPersistedProperties().isEmpty();
    }

    /**
     * Additional validation and handling of deprecated options
     *
     * @param cliArgs
     * @param abstractCommand
     */
    public void validateConfig(List<String> cliArgs, AbstractCommand abstractCommand) {
        if (cliArgs.contains(OPTIMIZED_BUILD_OPTION_LONG) && !wasBuildEverRun()) {
            throw new PropertyException(Messages.optimizedUsedForFirstStartup());
        }
        warnOnDuplicatedOptionsInCli();

        IncludeOptions options = getIncludeOptions(cliArgs, abstractCommand, abstractCommand.getName());

        if (!options.includeBuildTime && !options.includeRuntime) {
            return;
        }

        if (!options.includeBuildTime) {
            validateBuildtime();
        }

        final boolean disabledMappersInterceptorEnabled = DisabledMappersInterceptor.isEnabled(); // return to the state before the disable
        try {
            DisabledMappersInterceptor.disable(); // we want all properties, even disabled ones

            final List<String> ignoredRunTime = new ArrayList<>();
            final Set<String> disabledBuildTime = new LinkedHashSet<>();
            final Set<String> disabledRunTime = new LinkedHashSet<>();
            final Set<String> deprecatedInUse = new LinkedHashSet<>();
            final Set<String> missingOption = new LinkedHashSet<>();
            final Set<String> ambiguousSpi = new LinkedHashSet<>();
            final LinkedHashMap<String, String> secondClassOptions = new LinkedHashMap<>();

            final Set<PropertyMapper<?>> disabledMappers = new HashSet<>();
            if (options.includeBuildTime) {
                disabledMappers.addAll(PropertyMappers.getDisabledBuildTimeMappers().values());
            }
            if (options.includeRuntime) {
                disabledMappers.addAll(PropertyMappers.getDisabledRuntimeMappers().values());
            }

            var categories = new HashSet<>(abstractCommand.getOptionCategories());

            // first validate the advertised property names
            // - this allows for efficient resolution of wildcard values and checking spi options
            Configuration.getPropertyNames().forEach(name -> {
                if (!name.startsWith(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX)) {
                    return; // there are canonical mappings to kc. values - no need to consider alternative forms
                }
                if (!options.includeRuntime) {
                    checkRuntimeSpiOptions(name, ignoredRunTime);
                }
                if (PropertyMappers.isMaybeSpiBuildTimeProperty(name)) {
                    ambiguousSpi.add(name);
                }
                PropertyMapper<?> mapper = PropertyMappers.getMapper(name);
                if (mapper == null) {
                    return; // TODO: need to look for disabled Wildcard mappers
                }
                String from = mapper.forKey(name).getFrom();
                if (!name.equals(from)) {
                    ConfigValue value = getUnmappedValue(name);
                    if (value.getValue() != null && isUserModifiable(value)) {
                        secondClassOptions.put(name, from);
                    }
                }
                if (!mapper.hasWildcard()) {
                    return; // non-wildcard options will be validated in the next pass
                }
                if (!categories.contains(mapper.getCategory())) {
                    return; // not of interest to this command
                    // TODO: due to picking values up from the env and auto-builds, this probably isn't correct
                    // - the same issue exists with the second pass
                }
                validateProperty(abstractCommand, options, ignoredRunTime, disabledBuildTime, disabledRunTime,
                        deprecatedInUse, missingOption, disabledMappers, mapper, from);
            });

            // second pass validate any property mapper not seen in the first pass
            // - this will catch required values, anything missing from the property names
            List<PropertyMapper<?>> mappers = new ArrayList<>();
            for (OptionCategory category : categories) {
                Optional.ofNullable(PropertyMappers.getRuntimeMappers().get(category)).ifPresent(mappers::addAll);
                Optional.ofNullable(PropertyMappers.getBuildTimeMappers().get(category)).ifPresent(mappers::addAll);
            }

            for (PropertyMapper<?> mapper : mappers) {
                if (!mapper.hasWildcard()) {
                    validateProperty(abstractCommand, options, ignoredRunTime, disabledBuildTime, disabledRunTime,
                            deprecatedInUse, missingOption, disabledMappers, mapper, mapper.getFrom());
                }
            }

            PropertyMappers.getPropertyMapperGroupings().forEach(g -> g.validateConfig(this));

            // third pass check for disabled mappers
            for (PropertyMapper<?> mapper : disabledMappers) {
                if (!mapper.hasWildcard()) {
                    validateProperty(abstractCommand, options, ignoredRunTime, disabledBuildTime, disabledRunTime,
                            deprecatedInUse, missingOption, disabledMappers, mapper, mapper.getFrom());
                }
            }

            if (!missingOption.isEmpty()) {
                throw new PropertyException("The following options are required: \n%s".formatted(String.join("\n", missingOption)));
            }
            if (!ignoredRunTime.isEmpty()) {
                info(format("The following run time options were found, but will be ignored during build time: %s\n",
                        String.join(", ", ignoredRunTime)));
            }

            if (!disabledBuildTime.isEmpty()) {
                outputDisabledProperties(disabledBuildTime, true);
            } else if (!disabledRunTime.isEmpty()) {
                outputDisabledProperties(disabledRunTime, false);
            }

            if (!deprecatedInUse.isEmpty()) {
                warn("The following used options or option values are DEPRECATED and will be removed or their behaviour changed in a future release:\n" + String.join("\n", deprecatedInUse) + "\nConsult the Release Notes for details.");
            }
            if (!ambiguousSpi.isEmpty()) {
                warn("The following SPI options are using the legacy format and are not being treated as build time options. Please use the new format with the appropriate -- separators to resolve this ambiguity: " + String.join("\n", ambiguousSpi));
            }
            secondClassOptions.forEach((key, firstClass) -> {
                warn("Please use the first-class option `%s` instead of `%s`".formatted(firstClass, key));
            });
        } finally {
            DisabledMappersInterceptor.enable(disabledMappersInterceptorEnabled);
        }
    }

    private void validateBuildtime() {
        final List<String> ignoredBuildTime = new ArrayList<>();
        // check for provider changes, or overrides of existing persisted options
        // we have to ignore things like the profile properties because the commands set them at runtime
        checkChangesInBuildOptions((key, oldValue, newValue) -> {
            if (key.startsWith(KC_PROVIDER_FILE_PREFIX)) {
                boolean changed = false;
                if (newValue == null || oldValue == null) {
                    changed = true;
                } else if (!warnedTimestampChanged && timestampChanged(oldValue, newValue)) {
                    if (Configuration.getOptionalBooleanKcValue("run-in-container").orElse(false)) {
                        warnedTimestampChanged = true;
                        warn(PROVIDER_TIMESTAMP_WARNING);
                    } else {
                        changed = true;
                    }
                }
                if (changed) {
                    throw new PropertyException(PROVIDER_TIMESTAMP_ERROR);
                }
            } else if (newValue != null && !isIgnoredPersistedOption(key)
                    && isUserModifiable(Configuration.getConfigValue(key))
                    // let quarkus handle this - it's unsupported for direct usage in keycloak
                    && !key.startsWith(MicroProfileConfigProvider.NS_QUARKUS_PREFIX)) {
                ignoredBuildTime.add(key);
            }
        });

        if (!ignoredBuildTime.isEmpty()) {
            throw new PropertyException(format("The following build time options have values that differ from what is persisted - the new values will NOT be used until another build is run: %s\n",
                    String.join(", ", ignoredBuildTime)));
        }
    }

    static boolean timestampChanged(String oldValue, String newValue) {
        long longNewValue = Long.valueOf(newValue);
        long longOldValue = Long.valueOf(oldValue);
        // docker commonly truncates to the second at runtime, so we'll allow that special case
        return ((longNewValue / 1000) * 1000) != longNewValue || ((longOldValue / 1000) * 1000) != longNewValue;
    }

    private ConfigValue getUnmappedValue(String key) {
        PropertyMappingInterceptor.disable();
        try {
            return Configuration.getConfigValue(key);
        } finally {
            PropertyMappingInterceptor.enable();
        }
    }

    private void validateProperty(AbstractCommand abstractCommand, IncludeOptions options,
            final List<String> ignoredRunTime, final Set<String> disabledBuildTime, final Set<String> disabledRunTime,
            final Set<String> deprecatedInUse, final Set<String> missingOption,
            final Set<PropertyMapper<?>> disabledMappers, PropertyMapper<?> mapper, String from) {
        if (mapper.isBuildTime() && !options.includeBuildTime) {
            return; // no need to validate as we've already checked for changes in the build time state
        }

        ConfigValue configValue = getUnmappedValue(from);
        String configValueStr = configValue.getValue();

        // don't consider missing or anything below standard env properties
        if (configValueStr != null && !isUserModifiable(configValue)) {
            return;
        }

        if (disabledMappers.contains(mapper)) {
            // add an error message if there's a value, no enabled propertymapper, and it's not a cli value
            // as some cli options may be directly on the command and not
            // backed by a property mapper - if they are disabled that should have already been handled as
            // an unrecognized arg
            if (configValueStr != null && PropertyMappers.getMapper(from) == null
                    && !PropertyMapper.isCliOption(configValue)) {
                handleDisabled(mapper.isRunTime() ? disabledRunTime : disabledBuildTime, mapper);
            }
            return;
        }

        if (mapper.isRunTime() && !options.includeRuntime) {
            if (configValueStr != null) {
                ignoredRunTime.add(mapper.getFrom());
            }
            return;
        }

        if (configValueStr == null) {
            if (mapper.isRequired()) {
                handleRequired(missingOption, mapper);
            }
            return;
        }

        mapper.validate(configValue);

        mapper.getDeprecatedMetadata().ifPresent(metadata -> handleDeprecated(deprecatedInUse, mapper, configValueStr, metadata));
    }

    private static void checkRuntimeSpiOptions(String key, final List<String> ignoredRunTime) {
        if (!key.startsWith(PropertyMappers.KC_SPI_PREFIX)) {
            return;
        }
        boolean buildTimeOption = PropertyMappers.isSpiBuildTimeProperty(key);

        if (!buildTimeOption) {
            ConfigValue configValue = Configuration.getConfigValue(key);
            String configValueStr = configValue.getValue();

            // don't consider missing or anything below standard env properties
            if (configValueStr != null && isUserModifiable(configValue)) {
                ignoredRunTime.add(key);
            }
        }
    }

    private static void handleDeprecated(Set<String> deprecatedInUse, PropertyMapper<?> mapper, String configValue,
            DeprecatedMetadata metadata) {
        Set<String> deprecatedValuesInUse = new HashSet<>();
        if (!metadata.getDeprecatedValues().isEmpty()) {
            deprecatedValuesInUse.addAll(Arrays.asList(configValue.split(",")));
            deprecatedValuesInUse.retainAll(metadata.getDeprecatedValues());

            if (deprecatedValuesInUse.isEmpty()) {
                return; // no deprecated values are used, don't emit any warning
            }
        }

        String optionName = mapper.getFrom();
        if (optionName.startsWith(NS_KEYCLOAK_PREFIX)) {
            optionName = optionName.substring(NS_KEYCLOAK_PREFIX.length());
        }

        StringBuilder sb = new StringBuilder("\t- ");
        sb.append(optionName);

        if (!deprecatedValuesInUse.isEmpty()) {
            sb.append("=").append(String.join(",", deprecatedValuesInUse));
        }

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

    private static void handleDisabled(Set<String> disabledInUse, PropertyMapper<?> mapper) {
        handleMessage(disabledInUse, mapper, PropertyMapper::getEnabledWhen);
    }

    private static void handleRequired(Set<String> requiredOptions, PropertyMapper<?> mapper) {
        handleMessage(requiredOptions, mapper, PropertyMapper::getRequiredWhen);
    }

    private static void handleMessage(Set<String> messages, PropertyMapper<?> mapper, Function<PropertyMapper<?>, Optional<String>> retrieveMessage) {
        var optionName = mapper.getOption().getKey();
        final StringBuilder sb = new StringBuilder("\t- ");
        sb.append(optionName);
        retrieveMessage.apply(mapper).ifPresent(msg -> sb.append(": ").append(msg).append("."));
        messages.add(sb.toString());
    }

    public void info(String text) {
        ColorScheme defaultColorScheme = picocli.CommandLine.Help.defaultColorScheme(colorMode);
        getOutWriter().println(defaultColorScheme.apply("INFO: ", Arrays.asList(Style.fg_green, Style.bold)) + text);
    }

    public void error(String text) {
        ColorScheme defaultColorScheme = picocli.CommandLine.Help.defaultColorScheme(colorMode);
        getErrWriter().println(defaultColorScheme.apply(text, Arrays.asList(Style.fg_red, Style.bold)));
    }

    public void warn(String text) {
        ColorScheme defaultColorScheme = picocli.CommandLine.Help.defaultColorScheme(colorMode);
        getOutWriter().println(defaultColorScheme.apply("WARNING: ", Arrays.asList(Style.fg_yellow, Style.bold)) + text);
    }

    private void outputDisabledProperties(Set<String> properties, boolean build) {
        warn(format("The following used %s time options are UNAVAILABLE and will be ignored during %s time:\n %s",
                build ? "build" : "run", build ? "run" : "build",
                String.join("\n", properties)));
    }

    public static Properties getNonPersistedBuildTimeOptions() {
        Properties properties = new Properties();
        // TODO: could get only non-persistent property names
        Configuration.getPropertyNames().forEach(name -> {
            boolean quarkus = false;
            PropertyMapper<?> mapper = PropertyMappers.getMapper(name);
            if (mapper != null) {
                if (!mapper.isBuildTime()) {
                    return;
                }
                name = mapper.forKey(name).getFrom();
                if (properties.containsKey(name)) {
                    return;
                }
            } else if (name.startsWith(MicroProfileConfigProvider.NS_QUARKUS)) {
                // TODO: this is not correct - we are including runtime properties here, but at least they
                // are already coming from a file
                quarkus = true;
            } else if (!PropertyMappers.isSpiBuildTimeProperty(name)) {
                return;
            }
            ConfigValue value = Configuration.getNonPersistedConfigValue(name);
            if (value.getValue() == null || value.getConfigSourceName() == null
                    || (quarkus && !value.getConfigSourceName().contains(QuarkusPropertiesConfigSource.NAME))) {
                // only persist build options resolved from config sources and not default values
                // instead we'll persist the profile (if set) because that may influence the defaults
                return;
            }
            // since we're persisting all quarkus values, this may leak some runtime information - we don't want
            // to capture expanded expressions that may be referencing environment variables
            String stringValue = value.getValue();
            if (quarkus && value.getRawValue() != null) {
                stringValue = value.getRawValue();
            }
            properties.put(name, stringValue);
        });

        // the following should be ignored when output the optimized check message
        // they are either not set by the user, or not properly initialized

        for (File jar : getProviderFiles().values()) {
            properties.put(String.format(KC_PROVIDER_FILE_PREFIX + "%s.last-modified", jar.getName()), String.valueOf(jar.lastModified()));
        }

        if (!Environment.isRebuildCheck()) {
            // not auto-build (e.g.: start without optimized option) but a regular build to create an optimized server image
            Configuration.markAsOptimized(properties);
        }

        String profile = org.keycloak.common.util.Environment.getProfile();
        if (profile != null) {
            properties.put(org.keycloak.common.util.Environment.PROFILE, profile);
            properties.put(LaunchMode.current().getProfileKey(), profile);
        }

        return properties;
    }

    private void updateSpecHelpAndUnmatched(CommandSpec spec, List<String> unrecognizedArgs) {
        try {
            spec.addOption(OptionSpec.builder(Help.OPTION_NAMES)
                    .usageHelp(true)
                    .description("This help message.")
                    .build());
        } catch (DuplicateOptionAnnotationsException e) {
            // Completion is inheriting mixinStandardHelpOptions = true
        }

        spec.addUnmatchedArgsBinding(CommandLine.Model.UnmatchedArgsBinding.forStringArrayConsumer(new ISetter() {
            @Override
            public <T> T set(T value) {
                if (value != null) {
                    unrecognizedArgs.addAll(Arrays.asList((String[]) value));
                }
                return null; // doesn't matter
            }
        }));

        spec.subcommands().values().forEach(c -> updateSpecHelpAndUnmatched(c.getCommandSpec(), unrecognizedArgs));
    }

    CommandLine createCommandLine(List<String> unrecognizedArgs) {
        CommandSpec spec = CommandSpec.forAnnotatedObject(new Main(), new IFactory() {
            @Override
            public <K> K create(Class<K> cls) throws Exception {
                K result = CommandLine.defaultFactory().create(cls);
                if (result instanceof AbstractCommand ac) {
                    ac.setPicocli(Picocli.this);
                }
                return result;
            }
        }).name(Environment.getCommand());
        updateSpecHelpAndUnmatched(spec, unrecognizedArgs);

        CommandLine cmd = new CommandLine(spec);
        cmd.setExpandAtFiles(false);
        cmd.setPosixClusteredShortOptionsAllowed(false);
        cmd.setExecutionExceptionHandler(this.errorHandler);
        cmd.setParameterExceptionHandler(new ShortErrorMessageHandler());
        cmd.setHelpFactory(new HelpFactory());
        cmd.getHelpSectionMap().put(SECTION_KEY_COMMAND_LIST, new SubCommandListRenderer());
        cmd.setErr(getErrWriter());
        cmd.setOut(getOutWriter());
        return cmd;
    }

    public PrintWriter getErrWriter() {
        return new PrintWriter(System.err, true);
    }

    public PrintWriter getOutWriter() {
        return new PrintWriter(System.out, true);
    }

    private IncludeOptions getIncludeOptions(List<String> cliArgs, AbstractCommand abstractCommand, String commandName) {
        IncludeOptions result = new IncludeOptions();
        if (abstractCommand == null) {
            return result;
        }
        result.includeRuntime = abstractCommand.includeRuntime();
        result.includeBuildTime = abstractCommand.includeBuildTime();

        if (!result.includeBuildTime && !result.includeRuntime) {
            return result;
        } else if (result.includeRuntime && !result.includeBuildTime) {
            result.includeBuildTime = !cliArgs.contains(OPTIMIZED_BUILD_OPTION_LONG);
        } else if (result.includeBuildTime && !result.includeRuntime) {
            result.includeRuntime = isRebuildCheck();
        }
        return result;
    }

    private void addCommandOptions(List<String> cliArgs, CommandLine command) {
        if (command != null && command.getCommand() instanceof AbstractCommand) {
            IncludeOptions options = getIncludeOptions(cliArgs, command.getCommand(), command.getCommandName());

            if (!options.includeBuildTime && !options.includeRuntime) {
                return;
            }

            addOptionsToCli(command, options);
        }
    }

    private void addOptionsToCli(CommandLine commandLine, IncludeOptions includeOptions) {
        final Map<OptionCategory, List<PropertyMapper<?>>> mappers = new EnumMap<>(OptionCategory.class);

        // Since we can't run sanitizeDisabledMappers sooner, PropertyMappers.getRuntime|BuildTimeMappers() at this point
        // contain both enabled and disabled mappers. Actual filtering is done later (help command, validations etc.).
        if (includeOptions.includeRuntime) {
            mappers.putAll(PropertyMappers.getRuntimeMappers());
        }

        if (includeOptions.includeBuildTime) {
            combinePropertyMappers(mappers, PropertyMappers.getBuildTimeMappers());
        }

        addMappedOptionsToArgGroups(commandLine, mappers);
    }

    private static <T extends Map<OptionCategory, List<PropertyMapper<?>>>> void combinePropertyMappers(T origMappers, T additionalMappers) {
        for (var entry : additionalMappers.entrySet()) {
            final List<PropertyMapper<?>> result = origMappers.getOrDefault(entry.getKey(), new ArrayList<>());
            result.addAll(entry.getValue());
            origMappers.put(entry.getKey(), result);
        }
    }

    private static void addMappedOptionsToArgGroups(CommandLine commandLine, Map<OptionCategory, List<PropertyMapper<?>>> propertyMappers) {
        CommandSpec cSpec = commandLine.getCommandSpec();
        for (OptionCategory category : ((AbstractCommand) commandLine.getCommand()).getOptionCategories()) {
            List<PropertyMapper<?>> mappersInCategory = propertyMappers.get(category);

            if (mappersInCategory == null) {
                //picocli raises an exception when an ArgGroup is empty, so ignore it when no mappings found for a category.
                continue;
            }

            ArgGroupSpec.Builder argGroupBuilder = ArgGroupSpec.builder()
                    .heading(category.getHeading() + ":")
                    .order(category.getOrder())
                    .validate(false);

            final Set<String> alreadyPresentArgs = new HashSet<>();

            for (PropertyMapper<?> mapper : mappersInCategory) {
                String name = mapper.getCliFormat();
                // Picocli doesn't allow to have multiple options with the same name. We need this in help-all which also prints
                // currently disabled options which might have a duplicate among enabled options. This is to register the disabled
                // options with a unique name in Picocli. To keep it simple, it adds just a suffix to the options, i.e. there cannot
                // be more that 1 disabled option with a unique name.
                if (cSpec.optionsMap().containsKey(name)) {
                    name = decorateDuplicitOptionName(name);
                }

                if (cSpec.optionsMap().containsKey(name) || alreadyPresentArgs.contains(name)) {
                    //when key is already added, don't add.
                    continue;
                }

                OptionSpec.Builder optBuilder = OptionSpec.builder(name)
                        .description(getDecoratedOptionDescription(mapper))
                        .completionCandidates(() -> mapper.getExpectedValues().iterator())
                        .hidden(mapper.isHidden());

                if (mapper.getParamLabel() != null) {
                    optBuilder.paramLabel(mapper.getParamLabel());
                }

                if (mapper.getDefaultValue().isPresent()) {
                    optBuilder.defaultValue(Option.getDefaultValueString(mapper.getDefaultValue().get()));
                }

                optBuilder.arity("1"); // everything requires a value to match configargs parsing
                if (mapper.getType() != null) {
                    optBuilder.type(mapper.getType());
                    if (mapper.isList()) {
                        // make picocli aware of the only list convention we allow
                        optBuilder.splitRegex(",");
                    } else if (mapper.getType().isEnum()) {
                        // prevent the auto-conversion that picocli does
                        // we validate the expected values later
                        optBuilder.type(String.class);
                    }
                } else {
                    optBuilder.type(String.class);
                }

                alreadyPresentArgs.add(name);

                argGroupBuilder.addArg(optBuilder.build());
            }

            if (argGroupBuilder.args().isEmpty()) {
                continue;
            }

            cSpec.addArgGroup(argGroupBuilder.build());
        }
    }

    private static String getDecoratedOptionDescription(PropertyMapper<?> mapper) {
        StringBuilder transformedDesc = new StringBuilder(Optional.ofNullable(mapper.getDescription()).orElse(""));

        if (mapper.getType() != Boolean.class && !mapper.getExpectedValues().isEmpty()) {
            List<String> decoratedExpectedValues = mapper.getExpectedValues().stream().map(value -> {
                if (mapper.getDeprecatedMetadata().filter(metadata -> metadata.getDeprecatedValues().contains(value)).isPresent()) {
                    return value + " (deprecated)";
                }
                return value;
            }).toList();

            var isStrictExpectedValues = mapper.getOption().isStrictExpectedValues();
            var isCaseInsensitiveExpectedValues = mapper.getOption().isCaseInsensitiveExpectedValues();
            var printableValues = String.join(", ", decoratedExpectedValues) + (!isStrictExpectedValues ? ", or a custom one" : "");

            transformedDesc.append(String.format(" Possible values are%s: %s.",
                    isCaseInsensitiveExpectedValues ? " (case insensitive)" : "",
                    printableValues)
            );
        }

        mapper.getDefaultValue()
                .map(d -> Option.getDefaultValueString(d).replaceAll("%", "%%")) // escape formats
                .map(d -> " Default: " + d + ".")
                .ifPresent(transformedDesc::append);

        mapper.getEnabledWhen().map(e -> format(" %s.", e)).ifPresent(transformedDesc::append);
        mapper.getRequiredWhen().map(e -> format(" %s.", e)).ifPresent(transformedDesc::append);

        // only fully deprecated options, not just deprecated values
        mapper.getDeprecatedMetadata()
                .filter(deprecatedMetadata -> deprecatedMetadata.getDeprecatedValues().isEmpty())
                .ifPresent(deprecatedMetadata -> {
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

    public void println(String message) {
        getOutWriter().println(message);
    }

    public static List<String> parseArgs(String[] rawArgs) throws PropertyException {
        if (rawArgs.length == 0) {
            return List.of();
        }

        // makes sure cli args are available to the config source
        ConfigArgsConfigSource.setCliArgs(rawArgs);

        // TODO: ignore properties for providers for now, need to fetch them from the providers, otherwise CLI will complain about invalid options
        // also ignores system properties as they are set when starting the JVM
        // change this once we are able to obtain properties from providers
        List<String> args = new ArrayList<>();
        ConfigArgsConfigSource.parseConfigArgs(List.of(rawArgs), (arg, value) -> {
            if (!arg.startsWith(ConfigArgsConfigSource.SPI_OPTION_PREFIX) && !arg.startsWith("-D")) {
                args.add(arg + "=" + value);
            }
        }, arg -> {
            if (arg.startsWith(ConfigArgsConfigSource.SPI_OPTION_PREFIX)) {
                throw new PropertyException(format("spi argument %s requires a value.", arg));
            }
            if (!arg.startsWith("-D")) {
                args.add(arg);
            }
        });
        return args;
    }

    public void checkChangesInBuildOptionsDuringAutoBuild(PrintWriter out) {
        StringBuilder options = new StringBuilder();

        checkChangesInBuildOptions((key, oldValue, newValue) -> optionChanged(options, key, oldValue, newValue));

        if (options.isEmpty()) {
            return;
        }
        out.println(
                colorMode.string(
                        new StringBuilder("@|bold,red ")
                                .append("The previous optimized build will be overridden with the following build options:")
                                .append(options)
                                .append("\nTo avoid that, run the 'build' command again and then start the optimized server instance using the '--optimized' flag.")
                                .append("|@").toString()
                )
        );
    }

    private static void checkChangesInBuildOptions(TriConsumer<String, String, String> valueChanged) {
        var current = getNonPersistedBuildTimeOptions();
        var persisted = Configuration.getRawPersistedProperties();

        // TODO: order is not well defined here

        current.forEach((key, value) -> {
            String persistedValue = persisted.get(key);
            if (!value.equals(persistedValue)) {
                valueChanged.accept((String)key, persistedValue, (String)value);
            }
        });

        persisted.forEach((key, value) -> {
            if (current.get(key) == null) {
                valueChanged.accept(key, value, null);
            }
        });
    }

    private static void optionChanged(StringBuilder options, String key, String oldValue, String newValue) {
        // the assumption here is that no build time options need mask handling
        boolean isIgnored = !key.startsWith(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX)
                || key.startsWith(KC_PROVIDER_FILE_PREFIX) || isIgnoredPersistedOption(key);
        if (!isIgnored) {
            key = key.substring(3);
            options.append("\n\t- ").append(key).append("=")
                    .append(Optional.ofNullable(oldValue).orElse("<unset>")).append(" > ")
                    .append(key).append("=")
                    .append(Optional.ofNullable(newValue).orElse("<unset>"));
        }
    }

    private static boolean isIgnoredPersistedOption(String key) {
        return key.equals(Configuration.KC_OPTIMIZED) || key.equals(org.keycloak.common.util.Environment.PROFILE)
                || key.equals(LaunchMode.current().getProfileKey());
    }

    public void start() {
        KeycloakMain.start(this, (AbstractNonServerCommand) this.getParsedCommand()
                .filter(AbstractNonServerCommand.class::isInstance).orElse(null), this.errorHandler);
    }

    public void build() throws Throwable {
        QuarkusEntryPoint.main();
    }

    public void initConfig(List<String> cliArgs, AbstractCommand command) {
        if (Configuration.isInitialized()) {
            throw new IllegalStateException("Config should not be initialized until profile is determined");
        }
        this.parsedCommand = Optional.ofNullable(command);

        if (!Environment.isRebuilt() && command instanceof AbstractAutoBuildCommand
                && !cliArgs.contains(OPTIMIZED_BUILD_OPTION_LONG)) {
            Environment.setRebuildCheck(true);
        }

        String profile = Optional.ofNullable(org.keycloak.common.util.Environment.getProfile())
                .or(() -> parsedCommand.map(AbstractCommand::getInitProfile)).orElse(Environment.PROD_PROFILE_VALUE);

        Environment.setProfile(profile);
        if (!cliArgs.contains(HelpAllMixin.HELP_ALL_OPTION)) {
            parsedCommand.ifPresent(PropertyMappers::sanitizeDisabledMappers);
        }
    }

    // Show warning about duplicated options in CLI
    public void warnOnDuplicatedOptionsInCli() {
        var duplicatedOptionsNames = ConfigArgsConfigSource.getDuplicatedArgNames();
        if (!duplicatedOptionsNames.isEmpty()) {
            warn("Duplicated options present in CLI: %s".formatted(String.join(", ", duplicatedOptionsNames)));
            ConfigArgsConfigSource.clearDuplicatedArgNames();
        }
    }

}
