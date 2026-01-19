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
import java.util.Map.Entry;
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
import org.keycloak.quarkus.runtime.cli.command.Build;
import org.keycloak.quarkus.runtime.cli.command.Main;
import org.keycloak.quarkus.runtime.cli.command.ShowConfig;
import org.keycloak.quarkus.runtime.cli.command.Tools;
import org.keycloak.quarkus.runtime.cli.command.WindowsService;
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

    private record IncludeOptions(boolean includeRuntime, boolean includeBuildTime, boolean allowUnrecognized) {
    }

    private final ExecutionExceptionHandler errorHandler = new ExecutionExceptionHandler();
    private Optional<AbstractCommand> parsedCommand = Optional.empty();
    private boolean warnedTimestampChanged;

    private Ansi colorMode = hasColorSupport() ? Ansi.ON : Ansi.OFF;
    private IncludeOptions options;
    private Set<String> duplicatedOptionsNames = new HashSet<String>();

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

            AbstractCommand currentCommand;
            if (cl.getCommand() instanceof AbstractCommand ac) {
                currentCommand = ac;
            } else {
                currentCommand = null;
            }

            // any unrecognized args can now be normalized to our property mapper based argument expectations
            Map<String, String> normalizedArgs = new LinkedHashMap<String, String>();
            List<String> unknown = new ArrayList<String>();
            ConfigArgsConfigSource.parseConfigArgs(unrecognizedArgs, (k, v) -> {
                if (normalizedArgs.put(k, v) != null) {
                    duplicatedOptionsNames.add(k);
                }
            }, unknown::add);
            unrecognizedArgs = null;

            ConfigArgsConfigSource.setCliArgs(normalizedArgs.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new));

            initConfig(currentCommand);

            // now that the property mappers are properly initalized further refine the args
            if (options.allowUnrecognized) {
                normalizedArgs.keySet().removeIf(arg -> PropertyMappers.getMapperByCliKey(arg) != null || arg.startsWith(ConfigArgsConfigSource.SPI_OPTION_PREFIX));
            }
            unknown.forEach(arg -> {
                if (PropertyMappers.getMapperByCliKey(arg) != null) {
                    addCommandOptions(cl, currentCommand);
                    throw new MissingParameterException(cl, cl.getCommandSpec().optionsMap().get(arg), null);
                } else if (arg.startsWith(ConfigArgsConfigSource.SPI_OPTION_PREFIX)) {
                    throw new PropertyException(format("spi argument %s requires a value.", arg));
                }
            });
            unknown.addAll(normalizedArgs.keySet());
            if (!unknown.isEmpty()) {
                addCommandOptions(cl, currentCommand);
                throw new KcUnmatchedArgumentException(cl, unknown);
            }

            if (isHelpRequested(result)) {
                addCommandOptions(cl, currentCommand);
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
     */
    public void validateConfig() {
        AbstractCommand abstractCommand = this.getParsedCommand().orElseThrow();
        if (abstractCommand.isOptimized() && !wasBuildEverRun()) {
            throw new PropertyException(Messages.optimizedUsedForFirstStartup());
        }
        warnOnDuplicatedOptionsInCli();

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
            final Set<String> unnecessary = new LinkedHashSet<>();
            final LinkedHashMap<String, String> secondClassOptions = new LinkedHashMap<>();

            final Set<PropertyMapper<?>> disabledMappers = new HashSet<>();
            if (options.includeBuildTime) {
                disabledMappers.addAll(PropertyMappers.getDisabledBuildTimeMappers().values());
            }
            if (options.includeRuntime) {
                disabledMappers.addAll(PropertyMappers.getDisabledRuntimeMappers().values());
            }

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
                var forKey = mapper.forKey(name);
                if (!name.equals(forKey.getFrom())) {
                    ConfigValue value = getUnmappedValue(name);
                    if (value.getValue() != null && isUserModifiable(value)) {
                        secondClassOptions.put(name, forKey.getFrom());
                    }
                }
                if (!mapper.hasWildcard()) {
                    return; // non-wildcard options will be validated in the next pass
                }
                validateProperty(abstractCommand, options, ignoredRunTime, disabledBuildTime, disabledRunTime,
                        deprecatedInUse, missingOption, disabledMappers.contains(mapper), forKey, unnecessary);
            });

            // second pass validate any property mapper not seen in the first pass
            // - this will catch required values, anything missing from the property names, or disabled
            for (PropertyMapper<?> mapper : PropertyMappers.getMappers()) {
                if (!mapper.hasWildcard()) {
                    validateProperty(abstractCommand, options, ignoredRunTime, disabledBuildTime, disabledRunTime,
                            deprecatedInUse, missingOption, disabledMappers.contains(mapper), mapper, unnecessary);
                }
            }

            PropertyMappers.getPropertyMapperGroupings().forEach(g -> g.validateConfig(this));

            // third pass check for disabled mappers
            for (PropertyMapper<?> mapper : disabledMappers) {
                if (!mapper.hasWildcard()) {
                    validateProperty(abstractCommand, options, ignoredRunTime, disabledBuildTime, disabledRunTime,
                            deprecatedInUse, missingOption, disabledMappers.contains(mapper), mapper, unnecessary);
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
            if (!unnecessary.isEmpty()) {
                info("The following options were specified, but are typically not relevant for this command: " + String.join("\n", unnecessary));
            }
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
            boolean disabled, PropertyMapper<?> mapper, final Set<String> unnecessary) {
        if (mapper.isBuildTime() && !options.includeBuildTime) {
            return; // no need to validate as we've already checked for changes in the build time state
        }
        boolean ignoreRuntime = mapper.isRunTime() && !options.includeRuntime;

        ConfigValue configValue = getUnmappedValue(mapper.getFrom());
        String configValueStr = configValue.getValue();

        if (configValueStr == null) {
            if (!ignoreRuntime && mapper.isRequired()) {
                handleRequired(missingOption, mapper);
            }
            return;
        }

        if (!isUserModifiable(configValue)) {
            return;
        }

        if (disabled) {
            // add an error message if no enabled propertymapper, and it's not a cli value
            // as some cli options may be directly on the command and not backed by a property mapper
            // - if they are disabled that should have already been handled as an unrecognized arg
            if (PropertyMappers.getMapper(mapper.getFrom()) == null
                    && !PropertyMapper.isCliOption(configValue)) {
                handleDisabled(mapper.isRunTime() ? disabledRunTime : disabledBuildTime, mapper);
            }
            return;
        }

        if (ignoreRuntime) {
            ignoredRunTime.add(mapper.getFrom());
            return;
        }

        mapper.validate(configValue);

        mapper.getDeprecatedMetadata().ifPresent(metadata -> handleDeprecated(deprecatedInUse, mapper, configValueStr, metadata));

        if (mapper.isRunTime() && PropertyMapper.isCliOption(configValue) && abstractCommand.isHiddenCategory(mapper.getCategory())) {
            unnecessary.add(mapper.getCliFormat());
        }
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

        removePlatformSpecificCommands(cmd);

        return cmd;
    }

    /**
     * Removes platform-specific commands on non-applicable platforms
     */
    private void removePlatformSpecificCommands(CommandLine cmd) {
        if (getCommandMode() == CommandMode.UNIX) {
            CommandLine toolsCmd = cmd.getSubcommands().get(Tools.NAME);
            if (toolsCmd != null) {
                CommandLine windowsServiceCmd = toolsCmd.getSubcommands().get(WindowsService.NAME);
                if (windowsServiceCmd != null) {
                    toolsCmd.getCommandSpec().removeSubcommand(WindowsService.NAME);
                }
            }
        }
    }

    enum CommandMode {
        ALL,
        WIN,
        UNIX
    }

    protected CommandMode getCommandMode() {
        // not an official option, just a way for integration tests to produce the same output regardless of OS
        return Optional.ofNullable(System.getenv("KEYCLOAK_COMMAND_MODE")).map(CommandMode::valueOf)
                .orElse(Environment.isWindows() ? CommandMode.WIN : CommandMode.UNIX);
    }

    public PrintWriter getErrWriter() {
        return new PrintWriter(System.err, true);
    }

    public PrintWriter getOutWriter() {
        return new PrintWriter(System.out, true);
    }

    private IncludeOptions getIncludeOptions(AbstractCommand abstractCommand) {
        if (abstractCommand == null) {
            return new IncludeOptions(false, false, false);
        }
        boolean autoBuild = abstractCommand instanceof AbstractAutoBuildCommand;
        boolean includeBuildTime = abstractCommand instanceof Build || (autoBuild && !abstractCommand.isOptimized());
        return new IncludeOptions(autoBuild, includeBuildTime, autoBuild || includeBuildTime || abstractCommand instanceof ShowConfig);
    }

    private void addCommandOptions(CommandLine command, AbstractCommand ac) {
        if (!options.includeBuildTime && !options.includeRuntime) {
            return;
        }
        final Map<OptionCategory, List<PropertyMapper<?>>> mappers = new EnumMap<>(OptionCategory.class);

        PropertyMappers.getRuntimeMappers().entrySet().forEach(e -> mappers.put(e.getKey(), new ArrayList<>(e.getValue())));
        PropertyMappers.getBuildTimeMappers().entrySet().forEach(e -> mappers.computeIfAbsent(e.getKey(), category -> new ArrayList<>()).addAll(e.getValue()));

        addMappedOptionsToArgGroups(command, mappers, ac, options);
    }

    private static void addMappedOptionsToArgGroups(CommandLine commandLine, Map<OptionCategory, List<PropertyMapper<?>>> propertyMappers, AbstractCommand ac, IncludeOptions options) {
        CommandSpec cSpec = commandLine.getCommandSpec();
        for (Entry<OptionCategory, List<PropertyMapper<?>>> entry : propertyMappers.entrySet()) {
            Set<String> names = new HashSet<String>();
            OptionCategory category = entry.getKey();

            ArgGroupSpec.Builder argGroupBuilder = ArgGroupSpec.builder()
                    .heading(category.getHeading() + ":")
                    .order(category.getOrder())
                    .validate(false);

            for (PropertyMapper<?> mapper : entry.getValue()) {
                String name = mapper.getCliFormat();

                boolean hidden = mapper.isHidden() || ac.isHiddenCategory(mapper.getCategory())
                        || (!options.includeBuildTime && mapper.isBuildTime())
                        || (!options.includeRuntime && mapper.isRunTime());

                if (hidden && ac.isHelpAll()) {
                    continue; // doesn't need defined
                }

                if (cSpec.optionsMap().containsKey(name)) {
                    continue; // command is dominant
                }

                if (ac.isHelpAll() && !names.add(name)) {
                    continue; // we sometimes duplicate mappers within the same command
                }

                OptionSpec.Builder optBuilder = OptionSpec.builder(name)
                        .description(getDecoratedOptionDescription(mapper))
                        .completionCandidates(() -> mapper.getExpectedValues().iterator())
                        .hidden(hidden);

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
        Environment.setRebuild();
        QuarkusEntryPoint.main();
    }

    public void initConfig(AbstractCommand command) {
        if (Configuration.isInitialized()) {
            throw new IllegalStateException("Config should not be initialized until profile is determined");
        }
        this.parsedCommand = Optional.ofNullable(command);
        options = getIncludeOptions(command);

        Environment.setRebuildCheck(!Environment.isRebuilt() && command instanceof AbstractAutoBuildCommand
                && !command.isOptimized());

        String profile = Optional.ofNullable(org.keycloak.common.util.Environment.getProfile())
                .or(() -> parsedCommand.map(AbstractCommand::getInitProfile)).orElse(Environment.PROD_PROFILE_VALUE);

        Environment.setProfile(profile);
        if (parsedCommand.filter(AbstractCommand::isHelpAll).isEmpty()) {
            parsedCommand.ifPresent(PropertyMappers::sanitizeDisabledMappers);
        }
    }

    // Show warning about duplicated options in CLI
    public void warnOnDuplicatedOptionsInCli() {
        if (!duplicatedOptionsNames.isEmpty()) {
            warn("Duplicated options present in CLI: %s".formatted(String.join(", ", duplicatedOptionsNames)));
        }
    }

}
