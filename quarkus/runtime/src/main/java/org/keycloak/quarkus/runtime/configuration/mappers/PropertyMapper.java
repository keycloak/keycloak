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
import static org.keycloak.quarkus.runtime.Environment.isRebuild;
import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR;
import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR_CHAR;
import static org.keycloak.quarkus.runtime.configuration.Configuration.toCliFormat;
import static org.keycloak.quarkus.runtime.configuration.Configuration.toEnvVarFormat;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

import org.keycloak.config.DeprecatedMetadata;
import org.keycloak.config.Option;
import org.keycloak.config.OptionBuilder;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.cli.PropertyMapperParameterConsumer;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.KcEnvConfigSource;
import org.keycloak.quarkus.runtime.configuration.KeycloakConfigSourceProvider;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;
import org.keycloak.utils.StringUtil;

public class PropertyMapper<T> {

    static PropertyMapper<?> IDENTITY = new PropertyMapper<>(
            new OptionBuilder<>(null, String.class).build(),
            null,
            () -> false,
            "",
            null,
            null,
            null,
            false,
            null) {
        @Override
        public ConfigValue getConfigValue(String name, ConfigSourceInterceptorContext context) {
            return context.proceed(name);
        }
    };

    private final Option<T> option;
    private final String to;
    private BooleanSupplier enabled;
    private String enabledWhen;
    private final BiFunction<Optional<String>, ConfigSourceInterceptorContext, Optional<String>> mapper;
    private final String mapFrom;
    private final boolean mask;
    private final String paramLabel;
    private final String envVarFormat;
    private final String cliFormat;
    private final BiConsumer<PropertyMapper<T>, ConfigValue> validator;

    PropertyMapper(Option<T> option, String to, BooleanSupplier enabled, String enabledWhen,
                   BiFunction<Optional<String>, ConfigSourceInterceptorContext, Optional<String>> mapper,
                   String mapFrom, String paramLabel, boolean mask, BiConsumer<PropertyMapper<T>, ConfigValue> validator) {
        this.option = option;
        this.to = to == null ? getFrom() : to;
        this.enabled = enabled;
        this.enabledWhen = enabledWhen;
        this.mapper = mapper == null ? PropertyMapper::defaultTransformer : mapper;
        this.mapFrom = mapFrom;
        this.paramLabel = paramLabel;
        this.mask = mask;
        this.cliFormat = toCliFormat(option.getKey());
        this.envVarFormat = toEnvVarFormat(getFrom());
        this.validator = validator;
    }

    private static Optional<String> defaultTransformer(Optional<String> value, ConfigSourceInterceptorContext context) {
        return value;
    }

    ConfigValue getConfigValue(ConfigSourceInterceptorContext context) {
        return getConfigValue(to, context);
    }

    ConfigValue getConfigValue(String name, ConfigSourceInterceptorContext context) {
        String from = getFrom();

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

        if (config == null || config.getValue() == null) {
            if (mapFrom != null) {
                // if the property we want to map depends on another one, we use the value from the other property to call the mapper
                String parentKey = MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + mapFrom;
                ConfigValue parentValue = convertValue(context.proceed(parentKey));

                if (parentValue == null) {
                    // parent value not explicitly set, try to resolve the default value set to the parent property
                    PropertyMapper<?> parentMapper = PropertyMappers.getMapper(parentKey);

                    if (parentMapper != null && parentMapper.getDefaultValue().isPresent()) {
                        parentValue = ConfigValue.builder().withValue(Option.getDefaultValueString(parentMapper.getDefaultValue().get())).build();
                    }
                }

                return transformValue(name, ofNullable(parentValue == null ? null : parentValue.getValue()), context, null);
            }

            ConfigValue defaultValue = transformValue(name, this.option.getDefaultValue().map(Option::getDefaultValueString), context, null);

            if (defaultValue != null) {
                return defaultValue;
            }

            // now tries any defaults from quarkus
            ConfigValue current = context.proceed(name);

            if (current != null) {
                return transformValue(name, ofNullable(current.getValue()), context, current.getConfigSourceName());
            }

            return current;
        }

        ConfigValue transformedValue = transformValue(name, ofNullable(config.getValue()), context, config.getConfigSourceName());

        // we always fallback to the current value from the property we are mapping
        if (transformedValue == null) {
            return context.proceed(name);
        }

        return transformedValue;
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

    public Class<T> getType() {
        return this.option.getType();
    }

    public String getFrom() {
        return MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + this.option.getKey();
    }

    public String getDescription() {
        return this.option.getDescription();
    }

    public List<String> getExpectedValues() {
        return this.option.getExpectedValues();
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

    public Optional<DeprecatedMetadata> getDeprecatedMetadata() {
        return option.getDeprecatedMetadata();
    }

    private ConfigValue transformValue(String name, Optional<String> value, ConfigSourceInterceptorContext context, String configSourceName) {
        if (value == null) {
            return null;
        }

        if (mapper == null || (mapFrom == null && name.equals(getFrom()))) {
            // no mapper set or requesting a property that does not depend on other property, just return the value from the config source
            return ConfigValue.builder()
                    .withName(name)
                    .withValue(value.orElse(null))
                    .withConfigSourceName(configSourceName)
                    .build();
        }

        Optional<String> mappedValue = mapper.apply(value, context);

        if (mappedValue == null || mappedValue.isEmpty()) {
            return null;
        }

        return ConfigValue.builder()
                .withName(name)
                .withValue(mappedValue.get())
                .withRawValue(value.orElse(null))
                .withConfigSourceName(configSourceName)
                .build();
    }

    private ConfigValue convertValue(ConfigValue configValue) {
        if (configValue == null) {
            return null;
        }

        return configValue.withValue(ofNullable(configValue.getValue()).map(String::trim).orElse(null));
    }

    public static class Builder<T> {

        private final Option<T> option;
        private String to;
        private BiFunction<Optional<String>, ConfigSourceInterceptorContext, Optional<String>> mapper;
        private String mapFrom = null;
        private boolean isMasked = false;
        private BooleanSupplier isEnabled = () -> true;
        private String enabledWhen = "";
        private String paramLabel;
        private BiConsumer<PropertyMapper<T>, ConfigValue> validator = (mapper, value) -> mapper.validateExpectedValues(value, mapper::validateSingleValue);

        public Builder(Option<T> option) {
            this.option = option;
        }

        public Builder<T> to(String to) {
            this.to = to;
            return this;
        }

        public Builder<T> transformer(BiFunction<Optional<String>, ConfigSourceInterceptorContext, Optional<String>> mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder<T> paramLabel(String label) {
            this.paramLabel = label;
            return this;
        }

        public Builder<T> mapFrom(String mapFrom) {
            this.mapFrom = mapFrom;
            return this;
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

        public Builder<T> validator(BiConsumer<PropertyMapper<T>, ConfigValue> validator) {
            this.validator = validator;
            return this;
        }

        public PropertyMapper<T> build() {
            if (paramLabel == null && Boolean.class.equals(option.getType())) {
                paramLabel = Boolean.TRUE + "|" + Boolean.FALSE;
            }
            return new PropertyMapper<T>(option, to, isEnabled, enabledWhen, mapper, mapFrom, paramLabel, isMasked, validator);
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

    public void validateExpectedValues(ConfigValue configValue, BiConsumer<ConfigValue, String> singleValidator) {
        String value = configValue.getValue();

        boolean multiValued = getOption().getType() == java.util.List.class;

        String[] values = multiValued ? value.split(",") : new String[] { value };
        for (String v : values) {
            if (multiValued && !v.trim().equals(v)) {
                throw new PropertyException("Invalid value for multivalued option " + getOptionAndSourceMessage(configValue)
                        + ": list value '" + v + "' should not have leading nor trailing whitespace");
            }
            singleValidator.accept(configValue, v);
        }
    }

    public static boolean isCliOption(ConfigValue configValue) {
        return Optional.ofNullable(configValue.getConfigSourceName()).filter(name -> name.contains(ConfigArgsConfigSource.NAME)).isPresent();
    }

    public static boolean isEnvOption(ConfigValue configValue) {
        return Optional.ofNullable(configValue.getConfigSourceName()).filter(name -> name.contains(KcEnvConfigSource.NAME)).isPresent();
    }

    void validateSingleValue(ConfigValue configValue, String v) {
        List<String> expectedValues = getExpectedValues();
        if (!expectedValues.isEmpty() && !expectedValues.contains(v)) {
            throw new PropertyException(
                    String.format("Invalid value for option %s: %s.%s", getOptionAndSourceMessage(configValue), v,
                            PropertyMapperParameterConsumer.getExpectedValuesMessage(expectedValues, expectedValues)));
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
