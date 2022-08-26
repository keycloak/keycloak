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
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

import org.keycloak.config.Option;
import org.keycloak.config.OptionBuilder;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;

public class PropertyMapper<T> {

    static PropertyMapper IDENTITY = new PropertyMapper(
            new OptionBuilder<String>(null, String.class).build(),
            null,
            null,
            null,
            null,
            false) {
        @Override
        public ConfigValue getConfigValue(String name, ConfigSourceInterceptorContext context) {
            return context.proceed(name);
        }
    };

    private final Option<T> option;
    private final String to;
    private final BiFunction<Optional<String>, ConfigSourceInterceptorContext, Optional<String>> mapper;
    private final String mapFrom;
    private final boolean mask;
    private final String paramLabel;
    private final String envVarFormat;
    private String cliFormat;

    PropertyMapper(Option<T> option, String to, BiFunction<Optional<String>, ConfigSourceInterceptorContext, Optional<String>> mapper,
                   String mapFrom, String paramLabel, boolean mask) {
        this.option = option;
        this.to = to == null ? getFrom() : to;
        this.mapper = mapper == null ? PropertyMapper::defaultTransformer : mapper;
        this.mapFrom = mapFrom;
        this.paramLabel = paramLabel;
        this.mask = mask;
        this.cliFormat = toCliFormat(option.getKey());
        this.envVarFormat = toEnvVarFormat(getFrom());
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

        if (isRebuild() && isRunTime() && name.startsWith(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX)) {
            // during re-aug do not resolve the server runtime properties and avoid they included by quarkus in the default value config source
            return ConfigValue.builder().withName(name).build();
        }

        // try to obtain the value for the property we want to map first
        ConfigValue config = context.proceed(from);

        if (config == null) {
            if (mapFrom != null) {
                // if the property we want to map depends on another one, we use the value from the other property to call the mapper
                String parentKey = MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + mapFrom;
                ConfigValue parentValue = context.proceed(parentKey);

                if (parentValue == null) {
                    // parent value not explicitly set, try to resolve the default value set to the parent property
                    PropertyMapper parentMapper = PropertyMappers.getMapper(parentKey);

                    if (parentMapper != null && parentMapper.getDefaultValue().isPresent()) {
                        parentValue = ConfigValue.builder().withValue(parentMapper.getDefaultValue().get().toString()).build();
                    }
                }

                return transformValue(ofNullable(parentValue == null ? null : parentValue.getValue()), context);
            }

            ConfigValue defaultValue = transformValue(this.option.getDefaultValue().map(Objects::toString), context);

            if (defaultValue != null) {
                return defaultValue;
            }

            // now tries any defaults from quarkus
            ConfigValue current = context.proceed(name);

            if (current != null) {
                return transformValue(ofNullable(current.getValue()), context);
            }

            return current;
        }

        if (config.getName().equals(name)) {
            return config;
        }

        ConfigValue value = transformValue(ofNullable(config.getValue()), context);

        // we always fallback to the current value from the property we are mapping
        if (value == null) {
            return context.proceed(name);
        }

        return value;
    }

    public Option<T> getOption() { return this.option; }

    public Class<T> getType() { return this.option.getType(); }

    public String getFrom() {
        return MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + this.option.getKey();
    }

    public String getDescription() { return this.option.getDescription(); }

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

    private ConfigValue transformValue(Optional<String> value, ConfigSourceInterceptorContext context) {
        if (value == null) {
            return null;
        }

        if (mapper == null) {
            return ConfigValue.builder().withName(to).withValue(value.orElse(null)).build();
        }

        Optional<String> mappedValue = mapper.apply(value, context);

        if (mappedValue == null || mappedValue.isEmpty()) {
            return null;
        }

        return ConfigValue.builder().withName(to).withValue(mappedValue.get()).withRawValue(value.orElse(null)).build();
    }

    public static class Builder<T> {

        private final Option<T> option;
        private String to;
        private BiFunction<Optional<String>, ConfigSourceInterceptorContext, Optional<String>> mapper;
        private String mapFrom = null;
        private boolean isMasked = false;
        private String paramLabel;

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

        public PropertyMapper<T> build() {
            if (paramLabel == null && Boolean.class.equals(option.getType())) {
                paramLabel = Boolean.TRUE + "|" + Boolean.FALSE;
            }
            return new PropertyMapper<T>(option, to, mapper, mapFrom, paramLabel, isMasked);
        }
    }

    public static <T> PropertyMapper.Builder<T> fromOption(Option<T> opt) {
        return new PropertyMapper.Builder<>(opt);
    }

}
