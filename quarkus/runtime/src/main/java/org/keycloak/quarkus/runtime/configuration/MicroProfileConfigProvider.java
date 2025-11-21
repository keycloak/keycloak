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

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.keycloak.Config;
import org.keycloak.Config.Scope;

import io.smallrye.config.SmallRyeConfig;
import org.eclipse.microprofile.config.ConfigValue;

import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR;
import static org.keycloak.quarkus.runtime.configuration.Configuration.toDashCase;

public class MicroProfileConfigProvider implements Config.ConfigProvider {

    public static final String NS_KEYCLOAK = "kc";
    public static final String NS_KEYCLOAK_PREFIX = NS_KEYCLOAK + ".";
    public static final String SPI_PREFIX = NS_KEYCLOAK_PREFIX + "spi" + OPTION_PART_SEPARATOR;
    public static final String NS_QUARKUS = "quarkus";
    public static final String NS_QUARKUS_PREFIX = "quarkus" + ".";

    private final SmallRyeConfig config;

    public MicroProfileConfigProvider() {
        this(Configuration.getConfig());
    }

    public MicroProfileConfigProvider(SmallRyeConfig config) {
        this.config = config;
    }

    @Override
    public String getProvider(String spi) {
        return scope(spi).get("provider");
    }

    @Override
    public String getDefaultProvider(String spi) {
        return scope(spi).get("provider-default");
    }

    @Override
    public Config.Scope scope(String... scope) {
        return new MicroProfileScope(SPI_PREFIX, scope);
    }

    public class MicroProfileScope implements Config.Scope {

        private final String prefix;
        private final String separatorPrefix;

        public MicroProfileScope(String prefix, String... scopes) {
            StringBuilder prefixBuilder = new StringBuilder(prefix);
            for (String scope : scopes) {
                prefixBuilder.append(toDashCase(scope)).append(OPTION_PART_SEPARATOR + OPTION_PART_SEPARATOR);
            }
            this.separatorPrefix = prefixBuilder.toString();
            this.prefix = separatorPrefix.replace(OPTION_PART_SEPARATOR + OPTION_PART_SEPARATOR, OPTION_PART_SEPARATOR);
        }

        @Override
        public String get(String key) {
            return getValue(key, String.class, null);
        }

        @Override
        public String get(String key, String defaultValue) {
            return getValue(key, String.class, defaultValue);
        }

        @Override
        public String[] getArray(String key) {
            return getValue(key, String[].class, null);
        }

        @Override
        public Integer getInt(String key) {
            return getValue(key, Integer.class, null);
        }

        @Override
        public Integer getInt(String key, Integer defaultValue) {
            return getValue(key, Integer.class, defaultValue);
        }

        @Override
        public Long getLong(String key) {
            return getValue(key, Long.class, null);
        }

        @Override
        public Long getLong(String key, Long defaultValue) {
            return getValue(key, Long.class, defaultValue);
        }

        @Override
        public Boolean getBoolean(String key) {
            return getValue(key, Boolean.class, null);
        }

        @Override
        public Boolean getBoolean(String key, Boolean defaultValue) {
            return getValue(key, Boolean.class, defaultValue);
        }

        @Override
        public Config.Scope scope(String... scope) {
            return new MicroProfileScope(prefix, scope);
        }

        @Override
        public Set<String> getPropertyNames() {
            return StreamSupport.stream(config.getPropertyNames().spliterator(), false)
                    .filter(key -> key.startsWith(separatorPrefix))
                    .collect(Collectors.toSet());
        }

        private <T> T getValue(String key, Class<T> clazz, T defaultValue) {
            if (NS_KEYCLOAK_PREFIX.equals(separatorPrefix)) {
                return config.getOptionalValue(separatorPrefix.concat(key), clazz).orElse(defaultValue);
            }
            String dashCase = toDashCase(key);
            String name = separatorPrefix.concat(dashCase);
            String oldName = prefix.concat(dashCase);
            ConfigValue value = config.getConfigValue(name);
            ConfigValue oldValue = config.getConfigValue(oldName);
            if (value.getValue() == null
                    || (oldValue.getValue() != null && oldValue.getSourceOrdinal() > value.getSourceOrdinal())) {
                value = oldValue;
            }
            return Optional.ofNullable(config.convert(value.getValue(), clazz)).orElse(defaultValue);
        }

        @Override
        public Scope root() {
            return new MicroProfileScope(NS_KEYCLOAK_PREFIX);
        }

    }

}
