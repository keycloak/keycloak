/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.quarkus.runtime.configuration.mappers;

import static java.util.Optional.ofNullable;
import static org.keycloak.config.Option.WILDCARD_PLACEHOLDER_PATTERN;
import static org.keycloak.quarkus.runtime.Environment.isRebuild;
import static org.keycloak.quarkus.runtime.cli.Picocli.ARG_PREFIX;
import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR;
import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR_CHAR;
import static org.keycloak.quarkus.runtime.configuration.Configuration.toCliFormat;
import static org.keycloak.quarkus.runtime.configuration.Configuration.toEnvVarFormat;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.ConfigValue.ConfigValueBuilder;
import io.smallrye.config.ExpressionConfigSourceInterceptor;
import io.smallrye.config.Expressions;
import org.keycloak.config.DeprecatedMetadata;
import org.keycloak.config.Option;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.cli.ShortErrorMessageHandler;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.KcEnvConfigSource;
import org.keycloak.quarkus.runtime.configuration.KeycloakConfigSourceProvider;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;
import org.keycloak.utils.StringUtil;

public class PropertyMapper<T> {

    private final Option<T> option;
    private final String to;
    private BooleanSupplier enabled;
    private String enabledWhen;
    private final ValueMapper mapper;
    private final String mapFrom;
    private final ValueMapper parentMapper;
    private final boolean mask;
    private final String paramLabel;
    private final String envVarFormat;
    private final String cliFormat;
    private final BiConsumer<PropertyMapper<T>, ConfigValue> validator;
    private final String description;
    private final BooleanSupplier required;
    private final String requiredWhen;
    private Matcher fromWildcardMatcher;
    private Pattern fromWildcardPattern;
    private Pattern envVarNameWildcardPattern;
    private Matcher toWildcardMatcher;
    private Pattern toWildcardPattern;
    private Function<Set<String>, Set<String>> wildcardKeysTransformer;

    PropertyMapper(Option<T> option, String to, BooleanSupplier enabled, String enabledWhen,
                   ValueMapper mapper,
                   String mapFrom, ValueMapper parentMapper,
                   String paramLabel, boolean mask, BiConsumer<PropertyMapper<T>, ConfigValue> validator,
                   String description, BooleanSupplier required, String requiredWhen, Function<Set<String>, Set<String>> wildcardKeysTransformer) {
        this.option = option;
        this.to = to == null ? getFrom() : to;
        this.enabled = enabled;
        this.enabledWhen = enabledWhen;
        this.mapper = mapper;
        this.mapFrom = mapFrom;
        this.paramLabel = paramLabel;
        this.mask = mask;
        this.cliFormat = toCliFormat(option.getKey());
        this.required = required;
        this.requiredWhen = requiredWhen;
        this.envVarFormat = toEnvVarFormat(getFrom());
        this.validator = validator;
        this.description = description;
        this.parentMapper = parentMapper;


        // The wildcard pattern (e.g. log-level-<category>) is matching only a-z, 0-0 and dots. For env vars, dots are replaced by underscores.
        if (option.getKey() != null) {
            fromWildcardMatcher = WILDCARD_PLACEHOLDER_PATTERN.matcher(option.getKey());
            if (fromWildcardMatcher.find()) {
                // Includes handling for both "--" prefix for CLI options and "kc." prefix
                this.fromWildcardPattern = Pattern.compile("(?:" + ARG_PREFIX + "|kc\\.)" + fromWildcardMatcher.replaceFirst("([\\\\\\\\.a-zA-Z0-9]+)"));

                // Not using toEnvVarFormat because it would process the whole string incl the <...> wildcard.
                Matcher envVarMatcher = WILDCARD_PLACEHOLDER_PATTERN.matcher(option.getKey().toUpperCase().replace("-", "_"));
                this.envVarNameWildcardPattern = Pattern.compile("KC_" + envVarMatcher.replaceFirst("([_A-Z0-9]+)"));

                if (to != null) {
                    toWildcardMatcher = WILDCARD_PLACEHOLDER_PATTERN.matcher(to);
                    if (!toWildcardMatcher.find()) {
                        throw new IllegalArgumentException("Attempted to map a wildcard option to a non-wildcard option");
                    }

                    this.toWildcardPattern = Pattern.compile(toWildcardMatcher.replaceFirst("([\\\\\\\\.a-zA-Z0-9]+)"));
                }
            }

            this.wildcardKeysTransformer = wildcardKeysTransformer;
        }
    }

    ConfigValue getConfigValue(ConfigSourceInterceptorContext context) {
        return getConfigValue(to, context);
    }

    ConfigValue getConfigValue(String name, ConfigSourceInterceptorContext context) {
        String from = getFrom(getMappedKey(name).orElse(null));

        if (to != null && to.endsWith(OPTION_PART_SEPARATOR)) {
            // in case mapping is based on prefixes instead of full property names
            from = name.replace(to.substring(0, to.lastIndexOf('.')), from.substring(0, from.lastIndexOf(OPTION_PART_SEPARATOR_CHAR)));
        }

        if ((isRebuild() || Environment.isRebuildCheck()) && isRunTime()) {
            // during re-aug do not resolve the server runtime properties and avoid they included by quarkus in the default value config source
            return ConfigValue.builder().withName(name).build();
        }

        // try to obtain the value for the property we want to map first
        ConfigValue config = convertValue(context.proceed(from));

        boolean parentValue = false;
        if (mapFrom != null && (config == null || config.getValue() == null)) {
            // if the property we want to map depends on another one, we use the value from the other property to call the mapper
            config = Configuration.getKcConfigValue(mapFrom);
            parentValue = true;
        }

        if (config != null && config.getValue() != null) {
            config = transformValue(name, config, context, parentValue);
        } else {
            String defaultValue = this.option.getDefaultValue().map(Option::getDefaultValueString).orElse(null);
            config = transformValue(name, new ConfigValueBuilder().withName(name)
                    .withValue(defaultValue).withRawValue(defaultValue).build(),
                    context, false);
        }

        if (config != null) {
            return config;
        }

        // now try any defaults from quarkus
        return context.proceed(name);
    }

    public Set<String> getWildcardKeys() {
        if (!hasWildcard()) {
            return Set.of();
        }

        // this is not optimal
        // TODO find an efficient way to get all values that match the wildcard
        Set<String> values = StreamSupport.stream(Configuration.getPropertyNames().spliterator(), false)
                .map(n -> getMappedKey(n, true, false, false))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        if (wildcardKeysTransformer != null) {
            return wildcardKeysTransformer.apply(values);
        }

        return values;
    }

    public Set<String> getToWithWildcards() {
        if (toWildcardMatcher == null) {
            return Set.of();
        }

        return getWildcardKeys().stream()
                .map(v -> toWildcardMatcher.replaceFirst(v))
                .collect(Collectors.toSet());
    }

    public Option<T> getOption() {
        return this.option;
    }

    public void setEnabled(BooleanSupplier enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled.getAsBoolean();
    }

    public Optional<String> getEnabledWhen() {
        return Optional.of(enabledWhen)
                .filter(StringUtil::isNotBlank)
                .map(e -> "Available only when " + e);
    }

    public void setEnabledWhen(String enabledWhen) {
        this.enabledWhen = enabledWhen;
    }

    public boolean isRequired() {
        return required.getAsBoolean();
    }

    public Optional<String> getRequiredWhen() {
        return Optional.of(requiredWhen)
                .filter(StringUtil::isNotBlank)
                .map(e -> "Required when " + e);
    }

    public Class<T> getType() {
        return this.option.getType();
    }

    public String getFrom(String wildcardKey) {
        String from = this.option.getKey();
        if (hasWildcard() && wildcardKey != null) {
            from = fromWildcardMatcher.replaceFirst(wildcardKey);
        }
        return MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + from;
    }

    public String getFrom() {
        return getFrom(null);
    }

    public String getDescription() {
        return this.description;
    }

    /**
     * If {@link #isStrictExpectedValues()} is false, custom values can be provided
     * Otherwise, only specified expected values can be used.
     *
     * @return expected values
     */
    public List<String> getExpectedValues() {
        return this.option.getExpectedValues();
    }

    public boolean isStrictExpectedValues() {
        return this.option.isStrictExpectedValues();
    }

    public Optional<T> getDefaultValue() { return this.option.getDefaultValue(); }

    public OptionCategory getCategory() {
        return this.option.getCategory();
    }

    public boolean isHidden() { return this.option.isHidden(); }

    public boolean isBuildTime() {
        return this.option.isBuildTime();
    }

    public boolean isRunTime() {
        return !this.option.isBuildTime();
    }

    public String getTo(String wildcardKey) {
        String to = this.to;
        if (hasWildcard() && wildcardKey != null) {
            to = toWildcardMatcher.replaceFirst(wildcardKey);
        }
        return to;
    }

    public String getTo() {
        return getTo(null);
    }

    public String getParamLabel() {
        return paramLabel;
    }

    public String getCliFormat() {
        return cliFormat;
    }

    public String getEnvVarFormat() {
        return envVarFormat;
    }

    boolean isMask() {
        return mask;
    }

    public Optional<DeprecatedMetadata> getDeprecatedMetadata() {
        return option.getDeprecatedMetadata();
    }

    /**
     * An option is considered a wildcard option if its key contains a wildcard placeholder (e.g. log-level-<category>).
     * The placeholder must be denoted by the '<' and '>' characters.
     */
    public boolean hasWildcard() {
        return fromWildcardPattern != null;
    }

    /**
     * Checks if the given option name matches the wildcard pattern of this option.
     * E.g. check if "log-level-io.quarkus" matches the wildcard pattern "log-level-<category>".
     */
    public boolean matchesWildcardOptionName(String name) {
        if (!hasWildcard()) {
            return false;
        }
        return fromWildcardPattern.matcher(name).matches() || envVarNameWildcardPattern.matcher(name).matches()
                || (toWildcardPattern != null && toWildcardPattern.matcher(name).matches());
    }

    /**
     * Returns a mapped key for the given option name if a relevant mapping is available, or empty otherwise.
     * Currently, it only attempts to extract the wildcard key from the given option name.
     * E.g. for the option "log-level-<category>" and the option name "log-level-io.quarkus",
     * the wildcard value would be "io.quarkus".
     */
    private Optional<String> getMappedKey(String originalKey, boolean tryFrom, boolean tryEnvVar, boolean tryTo) {
        if (!hasWildcard()) {
            return Optional.empty();
        }

        if (tryFrom) {
            Matcher matcher = fromWildcardPattern.matcher(originalKey);
            if (matcher.matches()) {
                return Optional.of(matcher.group(1));
            }
        }

        if (tryEnvVar) {
            Matcher matcher = envVarNameWildcardPattern.matcher(originalKey);
            if (matcher.matches()) {
                String value = matcher.group(1);
                value = value.toLowerCase().replace("_", "."); // we opiniotatedly convert env var names to CLI format with dots
                return Optional.of(value);
            }
        }

        if (tryTo && toWildcardPattern != null) {
            Matcher matcher = toWildcardPattern.matcher(originalKey);
            if (matcher.matches()) {
                return Optional.of(matcher.group(1));
            }
        }

        return Optional.empty();
    }

    public Optional<String> getMappedKey(String originalKey) {
        return getMappedKey(originalKey, true, false, true);
    }

    public Optional<String> getMappedEnvVarKey(String originalKey) {
        return getMappedKey(originalKey, false, true, false);
    }

    private ConfigValue transformValue(String name, ConfigValue configValue, ConfigSourceInterceptorContext context, boolean parentValue) {
        String value = configValue.getValue();
        String mappedValue = value;

        boolean mapped = false;
        var theMapper = parentValue ? this.parentMapper : this.mapper;
        if (theMapper != null && (!name.equals(getFrom()) || parentValue)) {
            String nameForMapper = getMappedKey(name).orElse(name);
            mappedValue = theMapper.map(nameForMapper, value, context);
            mapped = true;
        }

        // defaults and values from transformers may not have been subject to expansion
        if ((mapped || configValue.getConfigSourceName() == null) && mappedValue != null && Expressions.isEnabled() && mappedValue.contains("$")) {
            mappedValue = new ExpressionConfigSourceInterceptor().getValue(
                    new ContextWrapper(context, new ConfigValueBuilder().withName(name).withValue(mappedValue).build()),
                    name).getValue();
        }

        if (value == null && mappedValue == null) {
            return null;
        }

        if (!mapped && name.equals(configValue.getName())) {
            return configValue;
        }

        // by unsetting the ordinal this will not be seen as directly modified by the user
        return configValue.from().withName(name).withValue(mappedValue).withRawValue(value).withConfigSourceOrdinal(0).build();
    }

    private ConfigValue convertValue(ConfigValue configValue) {
        if (configValue == null) {
            return null;
        }

        return configValue.withValue(ofNullable(configValue.getValue()).map(String::trim).orElse(null));
    }

    @FunctionalInterface
    public interface ValueMapper {
        String map(String name, String value, ConfigSourceInterceptorContext context);
    }

    private final class ContextWrapper implements ConfigSourceInterceptorContext {
        private final ConfigSourceInterceptorContext context;
        private final ConfigValue value;

        private ContextWrapper(ConfigSourceInterceptorContext context, ConfigValue value) {
            this.context = context;
            this.value = value;
        }

        @Override
        public ConfigValue restart(String name) {
            return context.restart(name);
        }

        @Override
        public ConfigValue proceed(String name) {
            if (name.equals(value.getName())) {
                return value;
            }
            return context.proceed(name);
        }

        @Override
        public Iterator<String> iterateNames() {
            return context.iterateNames();
        }
    }

    public static class Builder<T> {

        private final Option<T> option;
        private String to;
        private ValueMapper mapper;
        private String mapFrom = null;
        private ValueMapper parentMapper;
        private boolean isMasked = false;
        private BooleanSupplier isEnabled = () -> true;
        private String enabledWhen = "";
        private String paramLabel;
        private BiConsumer<PropertyMapper<T>, ConfigValue> validator = (mapper, value) -> mapper.validateValues(value, mapper::validateExpectedValues);
        private String description;
        private BooleanSupplier isRequired = () -> false;
        private String requiredWhen = "";
        private Function<Set<String>, Set<String>> wildcardKeysTransformer;

        public Builder(Option<T> option) {
            this.option = option;
            this.description = this.option.getDescription();
        }

        public Builder<T> to(String to) {
            this.to = to;
            return this;
        }

        /**
         * NOTE: This transformer will not apply to the mapFrom value. When using
         * {@link #mapFrom} you generally need a transformer specifically for the parent
         * value, see {@link #mapFrom(Option, ValueMapper)}
         * <p>
         * The value passed into the transformer may be null if the property has no value set, and no default
         */
        public Builder<T> transformer(ValueMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder<T> transformer(BiFunction<String, ConfigSourceInterceptorContext, String> mapper) {
            return transformer((name, value, context) -> mapper.apply(value, context));
        }

        public Builder<T> paramLabel(String label) {
            this.paramLabel = label;
            return this;
        }

        public Builder<T> mapFrom(Option<?> mapFrom) {
            this.mapFrom = mapFrom.getKey();
            return this;
        }

        public Builder<T> mapFrom(Option<?> mapFrom, ValueMapper parentMapper) {
            this.mapFrom = mapFrom.getKey();
            this.parentMapper = parentMapper;
            return this;
        }

        public Builder<T> mapFrom(Option<?> mapFrom, BiFunction<String, ConfigSourceInterceptorContext, String> parentMapper) {
            return mapFrom(mapFrom, (name, value, context) -> parentMapper.apply(value, context));
        }

        public Builder<T> isMasked(boolean isMasked) {
            this.isMasked = isMasked;
            return this;
        }

        public Builder<T> isEnabled(BooleanSupplier isEnabled, String enabledWhen) {
            this.isEnabled = isEnabled;
            this.enabledWhen=enabledWhen;
            return this;
        }

        public Builder<T> isEnabled(BooleanSupplier isEnabled) {
            this.isEnabled = isEnabled;
            return this;
        }

        /**
         * Sets this option as required when the {@link BooleanSupplier} returns {@code true}.
         * <p>
         * The {@code enableWhen} parameter is a message to show with the error message.
         * <p>
         * This check is only run in runtime mode.
         */
        public Builder<T> isRequired(BooleanSupplier isRequired, String requiredWhen) {
            this.requiredWhen = Objects.requireNonNull(requiredWhen);
            assert !requiredWhen.endsWith(".");
            return isRequired(isRequired);
        }

        /**
         * Sets this option as required when the {@link BooleanSupplier} returns {@code true}.
         * <p>
         * This check is only run in runtime mode.
         */
        public Builder<T> isRequired(BooleanSupplier isRequired) {
            this.isRequired = Objects.requireNonNull(isRequired);
            return this;
        }

        /**
         * Set the validator, overwriting the current one.
         */
        public Builder<T> validator(Consumer<String> validator) {
            this.validator = (mapper, value) -> mapper.validateValues(value,
                    (c, v) -> validator.accept(v));
            if (!Objects.equals(this.description, this.option.getDescription())) {
                throw new AssertionError("Overwriting the validator will cause the description modification from addValidateEnabled to be incorrect.");
            }
            return this;
        }

        public Builder<T> addValidator(BiConsumer<PropertyMapper<T>, ConfigValue> validator) {
            var current = this.validator;
            this.validator = (mapper, value) -> {
                Stream.of(current, validator).map(v -> {
                    try {
                        v.accept(mapper, value);
                        return Optional.<PropertyException>empty();
                    } catch (PropertyException e) {
                        return Optional.of(e);
                    }
                }).flatMap(Optional::stream)
                        .reduce((e1, e2) -> new PropertyException(String.format("%s.\n%s", e1.getMessage(), e2.getMessage())))
                        .ifPresent(e -> {
                            throw e;
                        });
            };
            return this;
        }

        /**
         * Similar to {@link #enabledWhen}, but uses the condition as a validator that is added to the current one. This allows the option
         * to appear in help.
         * @return
         */
        public Builder<T> addValidateEnabled(BooleanSupplier isEnabled, String enabledWhen) {
            this.addValidator((mapper, value) -> {
                if (!isEnabled.getAsBoolean()) {
                    throw new PropertyException(mapper.getOption().getKey() + " available only when " + enabledWhen);
                }
            });
            this.description = String.format("%s Available only when %s.", this.description, enabledWhen);
            return this;
        }

        public Builder<T> wildcardKeysTransformer(Function<Set<String>, Set<String>> wildcardValuesTransformer) {
            this.wildcardKeysTransformer = wildcardValuesTransformer;
            return this;
        }

        public PropertyMapper<T> build() {
            if (paramLabel == null && Boolean.class.equals(option.getType())) {
                paramLabel = Boolean.TRUE + "|" + Boolean.FALSE;
            }
            return new PropertyMapper<>(option, to, isEnabled, enabledWhen, mapper, mapFrom, parentMapper, paramLabel, isMasked, validator, description, isRequired, requiredWhen, wildcardKeysTransformer);
        }
    }

    public static <T> PropertyMapper.Builder<T> fromOption(Option<T> opt) {
        return new PropertyMapper.Builder<>(opt);
    }

    public void validate(ConfigValue value) {
        if (validator != null) {
            validator.accept(this, value);
        }
    }

    public boolean isList() {
        return getOption().getType() == java.util.List.class;
    }

    public void validateValues(ConfigValue configValue, BiConsumer<ConfigValue, String> singleValidator) {
        String value = configValue.getValue();

        boolean multiValued = isList();
        StringBuilder result = new StringBuilder();

        String[] values = multiValued ? value.split(",") : new String[] { value };
        for (String v : values) {
            if (multiValued && !v.trim().equals(v)) {
                if (!result.isEmpty()) {
                    result.append(".\n");
                }
                result.append("Invalid value for multivalued option ")
                        .append(getOptionAndSourceMessage(configValue))
                        .append(": list value '")
                        .append(v)
                        .append("' should not have leading nor trailing whitespace");
                continue;
            }
            try {
                singleValidator.accept(configValue, v);
            } catch (PropertyException e) {
                if (!result.isEmpty()) {
                    result.append(".\n");
                }
                result.append(e.getMessage());
            }
        }

        if (!result.isEmpty()) {
            throw new PropertyException(result.toString());
        }
    }

    public static boolean isCliOption(ConfigValue configValue) {
        return Optional.ofNullable(configValue.getConfigSourceName()).filter(name -> name.contains(ConfigArgsConfigSource.NAME)).isPresent();
    }

    public static boolean isEnvOption(ConfigValue configValue) {
        return Optional.ofNullable(configValue.getConfigSourceName()).filter(name -> name.contains(KcEnvConfigSource.NAME)).isPresent();
    }

    void validateExpectedValues(ConfigValue configValue, String v) {
        List<String> expectedValues = getExpectedValues();
        if (!expectedValues.isEmpty() && getOption().isStrictExpectedValues() && !expectedValues.contains(v)
                && (!getOption().isCaseInsensitiveExpectedValues()
                        || !expectedValues.stream().anyMatch(v::equalsIgnoreCase))) {
            throw new PropertyException(
                    String.format("Invalid value for option %s: %s.%s", getOptionAndSourceMessage(configValue), v,
                            ShortErrorMessageHandler.getExpectedValuesMessage(expectedValues, getOption().isCaseInsensitiveExpectedValues())));
        }
    }

    String getOptionAndSourceMessage(ConfigValue configValue) {
        if (isCliOption(configValue)) {
            return String.format("'%s'", this.getCliFormat());
        }
        if (isEnvOption(configValue)) {
            return String.format("'%s'", this.getEnvVarFormat());
        }
        return String.format("'%s' in %s", getFrom(),
                KeycloakConfigSourceProvider.getConfigSourceDisplayName(configValue.getConfigSourceName()));
    }

}
