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
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.config.DeprecatedMetadata;
import org.keycloak.config.Option;
import org.keycloak.config.OptionBuilder;
import org.keycloak.config.OptionCategory;
import org.keycloak.config.WildcardOptionsUtil;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.cli.ShortErrorMessageHandler;
import org.keycloak.quarkus.runtime.cli.command.AbstractCommand;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.KcEnvConfigSource;
import org.keycloak.quarkus.runtime.configuration.KeycloakConfigSourceProvider;
import org.keycloak.quarkus.runtime.configuration.NestedPropertyMappingInterceptor;
import org.keycloak.utils.StringUtil;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.ConfigValue.ConfigValueBuilder;
import io.smallrye.config.ExpressionConfigSourceInterceptor;
import io.smallrye.config.Expressions;

import static java.util.Optional.ofNullable;

import static org.keycloak.quarkus.runtime.configuration.Configuration.toCliFormat;
import static org.keycloak.quarkus.runtime.configuration.Configuration.toEnvVarFormat;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

public class PropertyMapper<T> {

    protected final Option<T> option;
    private final String to;
    private Function<AbstractCommand, Boolean> enabled;
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
    private final String from;

    private final String namedProperty;

    PropertyMapper(PropertyMapper<T> mapper, String from, String to, String mapFrom, String namedProperty, ValueMapper parentMapper) {
        this(mapper.option, to, mapper.enabled, mapper.enabledWhen, mapper.mapper, mapFrom, parentMapper,
                mapper.paramLabel, mapper.mask, mapper.validator, mapper.description, mapper.required,
                mapper.requiredWhen, from, namedProperty);
    }

    PropertyMapper(Option<T> option, String to, Function<AbstractCommand, Boolean> enabled, String enabledWhen,
                   ValueMapper mapper, String mapFrom, ValueMapper parentMapper,
                   String paramLabel, boolean mask, BiConsumer<PropertyMapper<T>, ConfigValue> validator,
                   String description, BooleanSupplier required, String requiredWhen, String from, String namedProperty) {
        this.option = option;
        this.from = from == null ? NS_KEYCLOAK_PREFIX + this.option.getKey() : from;
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
        this.namedProperty = namedProperty;
    }

    /**
     * This is the heart of the property mapping logic. In the first step, we need to find the value of the property and then transform it into a form of our needs.
     * <p>
     *
     * <b>1. Find value</b>
     * <p>
     * In preference order we are looking for:
     * <pre>
     *  [ {@link #from} ] ---> [ {@link #mapFrom} ] ---> [ {@link #getDefaultValue()} ] ---> [ {@link #to} ]
     * (explicit)     (derived)           (fallback)         (fallback)
     * </pre>
     * <p>
     *
     * <b>2. Transform found value</b>
     * <p>
     * If we found a value for the attribute name, it needs to be transformed via {@link #transformValue} method. How to transform it?
     * <ul>
     *   <li>If the name matches {@link #from} or we using the {@link #mapFrom} value, then apply the {@link PropertyMapper.Builder#transformer} or the {@link PropertyMapper.Builder#mapFrom(Option, ValueMapper)}
     *   <li>If the value contains an expression, expand it using SmallRye logic
     *   <li>Finally the returned {@link ConfigValue} is made to match what was requested - with the name, value, rawValue, and ordinal set appropriately.
     * </ul>
     */
    ConfigValue getConfigValue(String name, ConfigSourceInterceptorContext context) {
        String from = getFrom();

        // try to obtain the value for the property we want to map first
        // we don't want the NestedPropertyMappingInterceptor to restart the chain here, so we force a proceed
        // this ensures that mapFrom transformers, and regular transformers are applied exclusively - not chained
        ConfigValue config = convertValue(NestedPropertyMappingInterceptor.proceed(context, from));

        boolean parentValue = false;
        if (mapFrom != null && (config == null || config.getValue() == null)) {
            // if the property we want to map depends on another one, we use the value from the other property to call the mapper
            // not getting the value directly from SmallRye Config to avoid the risk of infinite recursion when Config is initializing
            String mapFromWithPrefix = NS_KEYCLOAK_PREFIX + mapFrom;
            config = context.restart(mapFromWithPrefix);
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

        if (config != null || name.equals(from)) {
            return config;
        }

        // now try any defaults from quarkus
        return context.proceed(name);
    }

    public Option<T> getOption() {
        return this.option;
    }

    public void setEnabled(Function<AbstractCommand, Boolean> enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled(AbstractCommand command) {
        return enabled.apply(command);
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

    public String getFrom() {
        return from;
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

    public boolean isHidden() {
        return this.option.isHidden() || this.getDescription() == null;
    }

    public boolean isBuildTime() {
        return this.option.isBuildTime();
    }

    public boolean isRunTime() {
        return !this.option.isBuildTime();
    }

    public String getTo() {
        return to;
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

    ValueMapper getParentMapper() {
        return parentMapper;
    }

    ValueMapper getMapper() {
        return mapper;
    }

    public Optional<DeprecatedMetadata> getDeprecatedMetadata() {
        return option.getDeprecatedMetadata();
    }

    /**
     * An option is considered a wildcard option if its key contains a wildcard placeholder (e.g. log-level-<category>).
     * The placeholder must be denoted by the '<' and '>' characters.
     */
    public boolean hasWildcard() {
        return false;
    }

    public Optional<String> getNamedProperty() {
        return Optional.ofNullable(namedProperty);
    }

    private ConfigValue transformValue(String name, ConfigValue configValue, ConfigSourceInterceptorContext context, boolean parentValue) {
        String value = configValue.getValue();
        String mappedValue = value;

        boolean mapped = false;
        // fall back to the transformer when no mapper is explicitly specified in .mapFrom()
        var theMapper = parentValue && parentMapper != null ? this.parentMapper : this.mapper;
        // since our mapping logic assumes fully resolved values, we cannot reliably map if Expressions are disabled
        if (Expressions.isEnabled() && theMapper != null && (!name.equals(getFrom()) || parentValue)) {
            mappedValue = theMapper.map(getNamedProperty().orElse(null), value, context);
            mapped = true;
        }

        // defaults and values from transformers may not have been subject to expansion
        if ((mapped || configValue.getConfigSourceName() == null) && mappedValue != null && Expressions.isEnabled() && mappedValue.contains("$")) {
            mappedValue = new ExpressionConfigSourceInterceptor().getValue(
                    new ContextWrapper(context, new ConfigValueBuilder().withName(name).withValue(mappedValue).build()),
                    name).getValue();
        }

        if (mappedValue == null) {
            return null;
        }

        if (!mapped && name.equals(configValue.getName())) {
            return configValue;
        }

        // by unsetting the configsource name this will not be seen as directly modified by the user
        return configValue.from().withName(name).withValue(mappedValue).withRawValue(value).withConfigSourceName(null).build();
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

    private static final class ContextWrapper implements ConfigSourceInterceptorContext {
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
        private Function<AbstractCommand, Boolean> enabled = ignored -> true;
        private String enabledWhen = "";
        private String paramLabel;
        private BiConsumer<PropertyMapper<T>, ConfigValue> validator = (mapper, value) -> mapper.validateValues(value, mapper::validateExpectedValues);
        private String description;
        private BooleanSupplier isRequired = () -> false;
        private String requiredWhen = "";
        private BiFunction<String, Set<String>, Set<String>> wildcardKeysTransformer;
        private ValueMapper wildcardMapFrom;

        public Builder(Option<T> option) {
            this.option = option;
            this.description = this.option.getDescription();
        }

        public Builder<T> to(String to) {
            this.to = to;
            return this;
        }

        /**
         * When using {@link #mapFrom} you generally need a transformer specifically for the parent
         * value, see {@link #mapFrom(Option, BiFunction)}
         * <p>
         * The value passed into the transformer may be null if the property has no value set, and no default
         */
        public Builder<T> transformer(BiFunction<String, ConfigSourceInterceptorContext, String> mapper) {
            return transformer((name, value, context) -> mapper.apply(value, context));
        }

        /**
         * When using {@link #mapFrom} you generally need a transformer specifically for the parent
         * value, see {@link #mapFrom(Option, BiFunction)}
         * <p>
         * The value passed into the transformer may be null if the property has no value set, and no default
         */
        public Builder<T> transformer(ValueMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder<T> paramLabel(String label) {
            this.paramLabel = label;
            return this;
        }

        public Builder<T> removeMapFrom() {
            this.mapFrom = null;
            return this;
        }

        public Builder<T> mapFrom(Option<?> mapFrom) {
            this.mapFrom = mapFrom.getKey();
            return this;
        }

        public Builder<T> mapFrom(Option<?> mapFrom, BiFunction<String, ConfigSourceInterceptorContext, String> parentMapper) {
            return mapFrom(mapFrom, (name, value, context) -> parentMapper.apply(value, context));
        }

        public Builder<T> mapFrom(Option<?> mapFrom, ValueMapper parentMapper) {
            this.mapFrom = mapFrom.getKey();
            this.parentMapper = parentMapper;
            return this;
        }

        public Builder<T> isMasked(boolean isMasked) {
            this.isMasked = isMasked;
            return this;
        }

        public Builder<T> isEnabled(BooleanSupplier isEnabled, String enabledWhen) {
            this.enabled = ignored -> isEnabled.getAsBoolean();
            this.enabledWhen=enabledWhen;
            return this;
        }

        public Builder<T> isEnabled(BooleanSupplier isEnabled) {
            return isEnabled(isEnabled, "");
        }

        public Builder<T> isEnabled(Function<AbstractCommand, Boolean> enabled) {
            this.enabled = enabled;
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

        public Builder<T> wildcardKeysTransformer(BiFunction<String, Set<String>, Set<String>> wildcardValuesTransformer) {
            this.wildcardKeysTransformer = wildcardValuesTransformer;
            return this;
        }

        public Builder<T> wildcardMapFrom(Option<?> mapFrom, ValueMapper function) {
            wildcardMapFrom(mapFrom.getKey(), function);
            return this;
        }

        public Builder<T> wildcardMapFrom(String mapFrom, ValueMapper function) {
            this.mapFrom = mapFrom;
            this.wildcardMapFrom = function;
            return this;
        }

        /**
         * Validates wildcard keys.
         * You can validate whether an allowed key is provided as the wildcard key.
         * <p>
         * f.e. check whether existing feature is referenced
         * <pre>
         * kc.feature-enabled-<feature>:v1
         * → (key, value) -> is key a feature? if not, fail
         *
         * @param validator validator with parameters (wildcardKey, value)
         */
        public Builder<T> wildcardKeysValidator(BiConsumer<String, String> validator) {
            addValidator((mapper, configValue) -> {
                var wildcardMapper = (WildcardPropertyMapper<?>) mapper;
                var key = wildcardMapper.extractWildcardValue(configValue.getName()).orElseThrow(() -> new PropertyException("Cannot determine wildcard key."));
                validator.accept(key, configValue.getValue());
            });
            return this;
        }

        public PropertyMapper<T> build() {
            if (paramLabel == null && Boolean.class.equals(option.getType())) {
                paramLabel = Boolean.TRUE + "|" + Boolean.FALSE;
            }
            if (option.getKey().contains(WildcardOptionsUtil.WILDCARD_START)) {
                return new WildcardPropertyMapper<>(option, to, enabled, enabledWhen, mapper, mapFrom, parentMapper, paramLabel, isMasked, validator, description, isRequired, requiredWhen, wildcardKeysTransformer, wildcardMapFrom);
            }
            if (wildcardKeysTransformer != null || wildcardMapFrom != null) {
                throw new AssertionError("Wildcard operations not expected with non-wildcard mapper");
            }
            return new PropertyMapper<>(option, to, enabled, enabledWhen, mapper, mapFrom, parentMapper, paramLabel, isMasked, validator, description, isRequired, requiredWhen, null, null);
        }
    }

    public static <T> PropertyMapper.Builder<T> fromOption(Option<T> opt) {
        return new PropertyMapper.Builder<>(opt);
    }

    /**
     * Create a property mapper from a feature.
     * The mapper maps to external properties the state of the feature.
     * <p>
     * If the feature is enabled, it returns {@code true}. Otherwise {@code null}.
     */
    public static PropertyMapper.Builder<Boolean> fromFeature(Profile.Feature feature) {
        final var option = new OptionBuilder<>(feature.getKey() + "-hidden-mapper", Boolean.class)
                .buildTime(true)
                .hidden()
                .build();
        return new Builder<>(option)
                .isEnabled(() -> Profile.isFeatureEnabled(feature))
                .transformer((v, ctx) -> Boolean.TRUE.toString()); // we know the feature is enabled due to .isEnabled()
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
                if (option.getComponentType() != String.class && option.getExpectedValues().isEmpty()) {
                    if (v.isEmpty()) {
                        throw new PropertyException("Invalid empty value for option %s".formatted(getOptionAndSourceMessage(configValue)));
                    }
                    try {
                        Configuration.getConfig().convert(v, option.getComponentType());
                    } catch (Exception e) {
                        // strip the smallrye code if possible
                        String message = e.getMessage();
                        Pattern p = Pattern.compile("SRCFG\\d+: (.*)$");
                        Matcher m = p.matcher(message);
                        if (m.find()) {
                            message = m.group(1);
                        }
                        throw new PropertyException("Invalid value for option %s: %s".formatted(getOptionAndSourceMessage(configValue), message));
                    }
                }

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

    /**
     * Returns a new PropertyMapper tailored for the given key.
     * This is currently useful in {@link WildcardPropertyMapper} where "to" and "from" fields need to include a specific
     * wildcard key.
     */
    public PropertyMapper<?> forKey(String key) {
        return this;
    }

    public boolean hasConnectedOptions() {
        return !option.getConnectedOptions().isEmpty();
    }

    String getMapFrom() {
        return mapFrom;
    }

}
