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

package org.keycloak.quarkus.runtime.configuration;

import static org.keycloak.quarkus.runtime.Environment.getProfileOrDefault;
import static org.keycloak.quarkus.runtime.cli.Picocli.ARG_PREFIX;

import java.util.Optional;
import java.util.Properties;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.keycloak.config.Option;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

/**
 * The entry point for accessing the server configuration
 */
public final class Configuration {

    public static final char OPTION_PART_SEPARATOR_CHAR = '-';
    public static final String OPTION_PART_SEPARATOR = String.valueOf(OPTION_PART_SEPARATOR_CHAR);
    private static final String KC_OPTIMIZED = NS_KEYCLOAK_PREFIX + "optimized";

    private Configuration() {

    }

    public static boolean isTrue(Option<Boolean> option) {
        return getOptionalBooleanValue(NS_KEYCLOAK_PREFIX + option.getKey()).orElse(false);
    }

    public static boolean isTrue(String propertyName) {
        return getOptionalBooleanValue(propertyName).orElse(false);
    }

    public static boolean contains(Option<?> option, String value) {
        return getOptionalValue(NS_KEYCLOAK_PREFIX + option.getKey())
                .filter(f -> f.contains(value))
                .isPresent();
    }

    public static boolean equals(Option<?> option, String value) {
        return getOptionalValue(NS_KEYCLOAK_PREFIX + option.getKey())
                .filter(f -> f.equals(value))
                .isPresent();
    }

    public static synchronized SmallRyeConfig getConfig() {
        return (SmallRyeConfig) ConfigProviderResolver.instance().getConfig();
    }

    public static Optional<String> getBuildTimeProperty(String name) {
        Optional<String> value = getRawPersistedProperty(name);

        if (value.isEmpty()) {
            PropertyMapper<?> mapper = PropertyMappers.getMapper(name);

            if (mapper != null) {
                value = getRawPersistedProperty(mapper.getFrom());

                if (value.isEmpty() && mapper.getTo() != null) {
                    value = getRawPersistedProperty(mapper.getTo());
                }
            }
        }

        if (value.isEmpty()) {
            String profile = org.keycloak.common.util.Environment.getProfile();

            if (profile == null) {
                profile = getConfig().getRawValue(org.keycloak.common.util.Environment.PROFILE);
            }

            value = getRawPersistedProperty("%" + profile + "." + name);
        }

        return value;
    }

    public static Optional<String> getRawPersistedProperty(String name) {
        return Optional.ofNullable(PersistedConfigSource.getInstance().getValue(name));
    }

    public static String getRawValue(String propertyName) {
        return getConfig().getRawValue(propertyName);
    }

    public static Iterable<String> getPropertyNames() {
        return getPropertyNames(false);
    }

    public static Iterable<String> getPropertyNames(boolean onlyPersisted) {
        if (onlyPersisted) {
            return PersistedConfigSource.getInstance().getPropertyNames();
        }

        return getConfig().getPropertyNames();
    }

    public static ConfigValue getConfigValue(String propertyName) {
        return getConfig().getConfigValue(propertyName);
    }

    public static ConfigValue getKcConfigValue(String propertyName) {
        return getConfigValue(NS_KEYCLOAK_PREFIX.concat(propertyName));
    }

    public static Optional<String> getOptionalValue(String name) {
        return getConfig().getOptionalValue(name, String.class);
    }

    public static Optional<String> getOptionalKcValue(String propertyName) {
        return getOptionalValue(NS_KEYCLOAK_PREFIX.concat(propertyName));
    }

    public static Optional<Boolean> getOptionalBooleanKcValue(String propertyName) {
        return getOptionalValue(NS_KEYCLOAK_PREFIX.concat(propertyName)).map(Boolean::parseBoolean);
    }

    public static Optional<Boolean> getOptionalBooleanValue(String name) {
        return getOptionalValue(name).map(Boolean::parseBoolean);
    }

    public static String getMappedPropertyName(String key) {
        PropertyMapper<?> mapper = PropertyMappers.getMapper(key);

        if (mapper == null) {
            return key;
        }

        // we also need to make sure the target property is available when defined such as when defining alias for provider config (no spi-prefix).
        return mapper.getTo() == null ? mapper.getFrom() : mapper.getTo();
    }

    public static Optional<String> getRuntimeProperty(String name) {
        for (ConfigSource configSource : getConfig().getConfigSources()) {
            if (PersistedConfigSource.NAME.equals(configSource.getName())) {
                continue;
            }

            String value = getValue(configSource, name);

            if (value == null) {
                value = getValue(configSource, getMappedPropertyName(name));
            }

            if (value != null) {
                return Optional.of(value);
            }
        }

        return Optional.empty();
    }

    public static String toEnvVarFormat(String key) {
        return replaceNonAlphanumericByUnderscores(key).toUpperCase();
    }

    public static String toCliFormat(String key) {
        return ARG_PREFIX + key;
    }

    public static String toDashCase(String key) {
        StringBuilder sb = new StringBuilder(key.length());
        boolean l = false;

        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (l && Character.isUpperCase(c)) {
                sb.append('-');
                c = Character.toLowerCase(c);
                l = false;
            } else {
                l = Character.isLowerCase(c);
            }
            sb.append(c);
        }

        return sb.toString();
    }

    public static String replaceNonAlphanumericByUnderscores(String name) {
        int length = name.length();
        StringBuilder sb = new StringBuilder(length);

        for(int i = 0; i < length; ++i) {
            char c = name.charAt(i);
            if (('a' > c || c > 'z') && ('A' > c || c > 'Z') && ('0' > c || c > '9')) {
                sb.append('_');
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    private static String getValue(ConfigSource configSource, String name) {
        String value = configSource.getValue("%".concat(getProfileOrDefault("prod").concat(".").concat(name)));

        if (value == null) {
            value = configSource.getValue(name);
        }

        return value;
    }

    public static boolean isOptimized() {
        return Configuration.getRawPersistedProperty(KC_OPTIMIZED).isPresent();
    }

    public static void markAsOptimized(Properties properties) {
        properties.put(Configuration.KC_OPTIMIZED, Boolean.TRUE.toString());
    }

    public static ConfigValue getCurrentBuiltTimeProperty(String name) {
        return PersistedConfigSource.getInstance().runWithDisabled(() -> getConfigValue(name));
    }
}
