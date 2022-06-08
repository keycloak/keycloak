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

import static org.keycloak.quarkus.runtime.Environment.isRebuild;
import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR;
import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR_CHAR;
import static org.keycloak.quarkus.runtime.configuration.Configuration.toCliFormat;
import static org.keycloak.quarkus.runtime.configuration.Configuration.toEnvVarFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

import org.keycloak.config.Option;
import org.keycloak.config.OptionBuilder;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;

public class PropertyMapper<T> {

    static PropertyMapper IDENTITY = new PropertyMapper(String.class, null, null, Optional.empty(), null, null,
            false,null, null, false,Collections.emptyList(),null, true) {
        @Override
        public ConfigValue getConfigValue(String name, ConfigSourceInterceptorContext context) {
            return context.proceed(name);
        }
    };

    private final Option<T> option;
    private final String to;
    private final BiFunction<String, ConfigSourceInterceptorContext, String> mapper;
    private final String mapFrom;
    private final boolean mask;
    private final List<T> expectedValues;
    private final String paramLabel;
    private final String envVarFormat;
    private String cliFormat;

    // Backward compatible constructor
    PropertyMapper(Class<T> type, String from, String to, Optional<T> defaultValue, BiFunction<String, ConfigSourceInterceptorContext, String> mapper,
            String mapFrom, boolean buildTime, String description, String paramLabel, boolean mask, List<T> expectedValues,
            OptionCategory category, boolean hidden) {
        Set<Option.Runtime> runtimes = new HashSet<>();
        if (!hidden) {
            runtimes.add(Option.Runtime.QUARKUS);
        }
        this.option = new OptionBuilder<T>(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + from, type)
                .buildTime(buildTime)
                .category(category != null ? category : OptionCategory.GENERAL)
                .defaultValue(defaultValue)
                .description(description)
                .expectedValues(expectedValues)
                .runtimes(runtimes)
                .build();
        this.to = to == null ? option.getKey() : to;
        this.mapper = mapper == null ? PropertyMapper::defaultTransformer : mapper;
        this.mapFrom = mapFrom;
        this.paramLabel = paramLabel;
        this.mask = mask;
        this.expectedValues = expectedValues == null ? Collections.emptyList() : expectedValues;
        this.cliFormat = toCliFormat(from);
        this.envVarFormat = toEnvVarFormat(option.getKey());
    }

    public static PropertyMapper.Builder builder(String fromProp, String toProp) {
        return new PropertyMapper.Builder(fromProp, toProp);
    }

    public static PropertyMapper.Builder builder(OptionCategory category) {
        return new PropertyMapper.Builder(category);
    }

    private static String defaultTransformer(String value, ConfigSourceInterceptorContext context) {
        return value;
    }

    ConfigValue getConfigValue(ConfigSourceInterceptorContext context) {
        return getConfigValue(to, context);
    }

    ConfigValue getConfigValue(String name, ConfigSourceInterceptorContext context) {
        String from = this.option.getKey();

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

                if (parentValue != null) {
                    return transformValue(parentValue.getValue(), context);
                }
            }

            if (this.option.getDefaultValue().isPresent()) {
                return transformValue(this.option.getDefaultValue().get().toString(), context);
            }

            // now tries any defaults from quarkus
            ConfigValue current = context.proceed(name);

            if (current != null) {
                return transformValue(current.getValue(), context);
            }

            return current;
        }

        if (config.getName().equals(name)) {
            return config;
        }

        ConfigValue value = transformValue(config.getValue(), context);

        // we always fallback to the current value from the property we are mapping
        if (value == null) {
            return context.proceed(name);
        }

        return value;
    }

    public Class<T> getType() { return this.option.getType(); }

    public String getFrom() {
        return this.option.getKey();
    }

    public String getDescription() { return this.option.getDescription(); }

    public List<T> getExpectedValues() {
        return expectedValues;
    }

    public Optional<T> getDefaultValue() {return this.option.getDefaultValue(); }

    public OptionCategory getCategory() {
        return this.option.getCategory();
    }

    public boolean isHidden() {
        return !this.option.getSupportedRuntimes().contains(Option.Runtime.QUARKUS);
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

    private ConfigValue transformValue(String value, ConfigSourceInterceptorContext context) {
        if (value == null) {
            return null;
        }

        if (mapper == null) {
            return ConfigValue.builder().withName(to).withValue(value).build();
        }

        String mappedValue = mapper.apply(value, context);

        if (mappedValue != null) {
            return ConfigValue.builder().withName(to).withValue(mappedValue).build();
        }

        return null;
    }

    public static class Builder<T> {

        private Class<T> type;
        private String from;
        private String to;
        private T defaultValue;
        private BiFunction<String, ConfigSourceInterceptorContext, String> mapper;
        private String description;
        private String mapFrom = null;
        private List<T> expectedValues = new ArrayList<>();
        private boolean isBuildTimeProperty = false;
        private boolean isMasked = false;
        private OptionCategory category = OptionCategory.GENERAL;
        private String paramLabel;
        private boolean hidden;

        public Builder(OptionCategory category) {
            this.category = category;
        }

        public Builder(String fromProp, String toProp) {
            this.from = fromProp;
            this.to = toProp;
        }

        public Builder<T> from(String from) {
            this.from = from;
            return this;
        }

        public Builder<T> to(String to) {
            this.to = to;
            return this;
        }


        public Builder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder<T> transformer(BiFunction<String, ConfigSourceInterceptorContext, String> mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder<T> description(String description) {
            this.description = description;
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

        public Builder<T> expectedValues(List<T> expectedValues) {
            this.expectedValues = new ArrayList<>(expectedValues);
            return this;
        }

        public Builder expectedValues(T... expectedValues) {
            this.expectedValues = new ArrayList<>(Arrays.asList(expectedValues));
            return this;
        }

        public Builder<T> isBuildTimeProperty(boolean isBuildTime) {
            this.isBuildTimeProperty = isBuildTime;
            return this;
        }

        public Builder<T> isMasked(boolean isMasked) {
            this.isMasked = isMasked;
            return this;
        }

        public Builder<T> category(OptionCategory category) {
            this.category = category;
            return this;
        }

        public Builder<T> type(Class<T> type) {
            if (Boolean.class.equals(type)) {
                expectedValues((T) Boolean.TRUE.toString(), (T) Boolean.FALSE.toString());
                paramLabel(defaultValue == null ? "true|false" : defaultValue.toString());
                defaultValue(defaultValue == null ? (T) Boolean.FALSE : defaultValue);
            }
            this.type = type;
            return this;
        }

        public Builder<T> hidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }

        public PropertyMapper<T> build() {
            return new PropertyMapper<T>(type, from, to, Optional.ofNullable(defaultValue), mapper, mapFrom, isBuildTimeProperty, description, paramLabel,
                    isMasked, expectedValues, category, hidden);
        }
    }

    public static <T> PropertyMapper.Builder<T> fromOption(Option<T> opt) {
        Builder<T> builder = PropertyMapper.builder(opt.getCategory())
                .type(opt.getType())
                .from(opt.getKey())
                .hidden(!opt.getSupportedRuntimes().contains(Option.Runtime.QUARKUS))
                .description(opt.getDescription())
                .isBuildTimeProperty(opt.isBuildTime())
                .expectedValues(opt.getExpectedValues());
        if (opt.getDefaultValue().isPresent()) {
            builder.defaultValue(opt.getDefaultValue().get());
        }
        return builder;
    }
}
