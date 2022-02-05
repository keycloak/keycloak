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

import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR;
import static org.keycloak.quarkus.runtime.configuration.Configuration.toEnvVarFormat;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.microprofile.config.ConfigProvider;

import org.keycloak.Config;
import org.keycloak.quarkus.runtime.cli.Picocli;

public class MicroProfileConfigProvider implements Config.ConfigProvider {

    public static final String NS_KEYCLOAK = "kc";
    public static final String NS_KEYCLOAK_PREFIX = NS_KEYCLOAK + ".";
    public static final String NS_QUARKUS = "quarkus";
    public static final String NS_QUARKUS_PREFIX = "quarkus" + ".";

    private final org.eclipse.microprofile.config.Config config;

    public MicroProfileConfigProvider() {
        this(ConfigProvider.getConfig());
    }

    public MicroProfileConfigProvider(org.eclipse.microprofile.config.Config config) {
        this.config = config;
    }

    @Override
    public String getProvider(String spi) {
        return scope(spi).get("provider");
    }

    @Override
    public Config.Scope scope(String... scope) {
        return new MicroProfileScope(scope);
    }

    public class MicroProfileScope implements Config.Scope {

        private final String[] scope;
        private final String prefix;

        public MicroProfileScope(String... scope) {
            this.scope = scope;
            this.prefix = NS_KEYCLOAK_PREFIX + String.join(OPTION_PART_SEPARATOR, ArrayUtils.insert(0, scope, "spi"));
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
            return new MicroProfileScope(ArrayUtils.addAll(this.scope, scope));
        }

        @Override
        public Set<String> getPropertyNames() {
            return StreamSupport.stream(config.getPropertyNames().spliterator(), false)
                    .filter(new Predicate<String>() {
                        @Override
                        public boolean test(String key) {
                            return key.startsWith(prefix) || key.startsWith(toEnvVarFormat(prefix));
                        }
                    })
                    .collect(Collectors.toSet());
        }

        private <T> T getValue(String key, Class<T> clazz, T defaultValue) {
            return config.getOptionalValue(toDashCase(prefix.concat(OPTION_PART_SEPARATOR).concat(key)), clazz).orElse(defaultValue);
        }
    }

    private static String toDashCase(String s) {

        StringBuilder sb = new StringBuilder(s.length());
        boolean l = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
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

}
