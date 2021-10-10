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
package org.keycloak.configuration;

import static org.keycloak.util.Environment.getBuiltTimeProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

public class PropertyMapper {

    static PropertyMapper create(String fromProperty, String toProperty, String description) {
        return MAPPERS.computeIfAbsent(toProperty, s -> new PropertyMapper(fromProperty, s, null, null, description));
    }

    static PropertyMapper createWithDefault(String fromProperty, String toProperty, String defaultValue, String description) {
        return MAPPERS.computeIfAbsent(toProperty, s -> new PropertyMapper(fromProperty, s, defaultValue, null, description));
    }

    static PropertyMapper createWithDefault(String fromProperty, String toProperty, Supplier<String> defaultValue, String description) {
        return MAPPERS.computeIfAbsent(toProperty, s -> new PropertyMapper(fromProperty, s, defaultValue.get(), null, description));
    }

    static PropertyMapper createWithDefault(String fromProperty, String toProperty, String defaultValue, BiFunction<String, ConfigSourceInterceptorContext, String> transformer, String description) {
        return MAPPERS.computeIfAbsent(toProperty, s -> new PropertyMapper(fromProperty, s, defaultValue, transformer, description));
    }

    static PropertyMapper create(String fromProperty, String toProperty, BiFunction<String, ConfigSourceInterceptorContext, String> transformer, String description) {
        return MAPPERS.computeIfAbsent(toProperty, s -> new PropertyMapper(fromProperty, s, null, transformer, null, description));
    }

    static PropertyMapper create(String fromProperty, String toProperty, String description, boolean mask) {
        return MAPPERS.computeIfAbsent(toProperty, s -> new PropertyMapper(fromProperty, s, null, null, null, false, description, mask));
    }

    static PropertyMapper create(String fromProperty, String mapFrom, String toProperty, BiFunction<String, ConfigSourceInterceptorContext, String> transformer, String description) {
        return MAPPERS.computeIfAbsent(toProperty, s -> new PropertyMapper(fromProperty, s, null, transformer, mapFrom, description));
    }

    static PropertyMapper createBuildTimeProperty(String fromProperty, String toProperty, BiFunction<String, ConfigSourceInterceptorContext, String> transformer, String description) {
        return MAPPERS.computeIfAbsent(toProperty, s -> new PropertyMapper(fromProperty, s, null, transformer, null, true, description, false));
    }

    static PropertyMapper createBuildTimeProperty(String fromProperty, String toProperty, String description) {
        return MAPPERS.computeIfAbsent(toProperty, s -> new PropertyMapper(fromProperty, s, null, null, null, true, description, false));
    }

    static Map<String, PropertyMapper> MAPPERS = new HashMap<>();

    static PropertyMapper IDENTITY = new PropertyMapper(null, null, null, null, null) {
        @Override
        public ConfigValue getOrDefault(String name, ConfigSourceInterceptorContext context, ConfigValue current) {
            return current;
        }
    };

    private static String defaultTransformer(String value, ConfigSourceInterceptorContext context) {
        return value;
    }

    private final String to;
    private final String from;
    private final String defaultValue;
    private final BiFunction<String, ConfigSourceInterceptorContext, String> mapper;
    private final String mapFrom;
    private final boolean buildTime;
    private String description;
    private boolean mask;

    PropertyMapper(String from, String to, String defaultValue, BiFunction<String, ConfigSourceInterceptorContext, String> mapper, String description) {
        this(from, to, defaultValue, mapper, null, description);
    }

    PropertyMapper(String from, String to, String defaultValue, BiFunction<String, ConfigSourceInterceptorContext, String> mapper, String mapFrom, String description) {
        this(from, to, defaultValue, mapper, mapFrom, false, description, false);
    }
    
    PropertyMapper(String from, String to, String defaultValue, BiFunction<String, ConfigSourceInterceptorContext, String> mapper, String mapFrom, boolean buildTime, String description, boolean mask) {
        this.from = MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + from;
        this.to = to;
        this.defaultValue = defaultValue;
        if (mapper == null) {
            this.mapper = PropertyMapper::defaultTransformer;
        } else {
            this.mapper = mapper;
        }
        this.mapFrom = mapFrom;
        this.buildTime = buildTime;
        this.description = description;
        this.mask = mask;
    }

    ConfigValue getOrDefault(ConfigSourceInterceptorContext context, ConfigValue current) {
        return getOrDefault(null, context, current);        
    }

    ConfigValue getOrDefault(String name, ConfigSourceInterceptorContext context, ConfigValue current) {
        // try to obtain the value for the property we want to map
        ConfigValue config = context.proceed(from);

        if (config == null) {
            if (mapFrom != null) {
                // if the property we want to map depends on another one, we use the value from the other property to call the mapper
                String parentKey = MicroProfileConfigProvider.NS_KEYCLOAK + "." + mapFrom;
                ConfigValue parentValue = context.proceed(parentKey);

                if (parentValue != null) {
                    ConfigValue value = transformValue(parentValue.getValue(), context);

                    if (value != null) {
                        return value;
                    }
                }
            }

            // if not defined, return the current value from the property as a default if the property is not explicitly set
            if (defaultValue == null
                    || (current != null && !current.getConfigSourceName().equalsIgnoreCase("default values"))) {
                return current;
            }

            if (mapper != null) {
                return transformValue(defaultValue, context);
            }
            
            return ConfigValue.builder().withName(to).withValue(defaultValue).build();
        }

        if (mapFrom != null) {
            return config;
        }

        ConfigValue value = transformValue(config.getValue(), context);

        // we always fallback to the current value from the property we are mapping
        if (value == null) {
            return current;
        }

        return value;
    }

    public String getFrom() {
        return from;
    }

    public String getDescription() {
        return description;
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

    boolean isBuildTime() {
        return buildTime;
    }


    boolean isMask() {
        return mask;
    }

    String getTo() {
        return to;
    }
}
