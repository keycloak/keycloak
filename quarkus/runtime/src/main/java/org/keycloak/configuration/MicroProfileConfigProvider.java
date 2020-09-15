/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.microprofile.config.ConfigProvider;

import org.keycloak.Config;

public class MicroProfileConfigProvider implements Config.ConfigProvider {

    public static final String NS_KEYCLOAK = "kc";
    public static final String NS_KEYCLOAK_PREFIX = NS_KEYCLOAK + ".";
    public static final String NS_QUARKUS = "quarkus";
    public static final String NS_QUARKUS_PREFIX = NS_QUARKUS + ".";

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
            this.prefix = String.join(".", ArrayUtils.insert(0, scope, NS_KEYCLOAK, "spi"));
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

        private <T> T getValue(String key, Class<T> clazz, T defaultValue) {
            return config.getOptionalValue(toDashCase(prefix.concat(".").concat(key)), clazz).orElse(defaultValue);
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
