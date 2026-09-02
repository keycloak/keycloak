/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.common.util.StringPropertyReplacer.PropertyResolver;
import org.keycloak.common.util.SystemEnvProperties;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Config {

    private static ConfigProvider configProvider = new SystemPropertiesConfigProvider();

    public static void init(ConfigProvider configProvider) {
        Config.configProvider = configProvider;
        StringPropertyReplacer.setDefaultPropertyResolver(new PropertyResolver() {
            SystemEnvProperties systemVariables = new SystemEnvProperties(Config.getAllowedSystemVariables());

            @Override
            public String resolve(String property) {
                return systemVariables.getProperty(property);
            }
        });
    }

    public static String getAdminRealm() {
        return configProvider.scope("admin").get("realm", "master");
    }

    public static String getProvider(String spi) {
        String provider = configProvider.getProvider(spi);
        if (provider == null || provider.trim().equals("")) {
            return null;
        } else {
            return provider;
        }
    }

    public static String getDefaultProvider(String spi) {
        String provider = configProvider.getDefaultProvider(spi);
        if (provider == null || provider.trim().equals("")) {
            return null;
        } else {
            return provider;
        }
    }

    public static Scope scope(String... scope) {
         return configProvider.scope(scope);
    }

    private static Set<String> getAllowedSystemVariables() {
        Scope adminScope = configProvider.scope("admin");

        if (adminScope == null) {
            return Collections.emptySet();
        }

        String[] allowedSystemVariables = adminScope.getArray("allowed-system-variables");

        if (allowedSystemVariables == null) {
            return Collections.emptySet();
        }

        return new HashSet<>(Arrays.asList(allowedSystemVariables));
    }

    public static interface ConfigProvider {

        String getProvider(String spi);

        String getDefaultProvider(String spi);

        Scope scope(String... scope);

    }

    public static class SystemPropertiesConfigProvider implements ConfigProvider {

        @Override
        public String getProvider(String spi) {
            return System.getProperties().getProperty("keycloak." + spi + ".provider");
        }

        @Override
        public String getDefaultProvider(String spi) {
            return System.getProperties().getProperty("keycloak." + spi + ".provider.default");
        }

        @Override
        public Scope scope(String... scope) {
            StringBuilder sb = new StringBuilder();
            sb.append("keycloak.");
            for (String s : scope) {
                sb.append(s);
                sb.append(".");
            }
            return new SystemPropertiesScope(sb.toString());
        }

    }

    public static class SystemPropertiesScope extends AbstractScope {

        protected String prefix;

        public SystemPropertiesScope(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String get(String key) {
            String v = System.getProperty(prefix + key, null);
            return v != null && !v.isEmpty() ? v : null;
        }

        @Override
        public Scope scope(String... scope) {
            StringBuilder sb = new StringBuilder();
            sb.append(prefix + ".");
            for (String s : scope) {
                sb.append(s);
                sb.append(".");
            }
            return new SystemPropertiesScope(sb.toString());
        }

        @Override
        public Set<String> getPropertyNames() {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public Scope root() {
            return new SystemPropertiesScope("keycloak.");
        }

    }

    /**
     * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
     */
    public static interface Scope {

        String get(String key);

        String get(String key, String defaultValue);

        String[] getArray(String key);

        default Integer getInt(String key) {
            return getInt(key, null);
        }

        Integer getInt(String key, Integer defaultValue);

        default Long getLong(String key) {
            return getLong(key, null);
        }

        Long getLong(String key, Long defaultValue);

        default Boolean getBoolean(String key) {
            return getBoolean(key, null);
        }

        Boolean getBoolean(String key, Boolean defaultValue);

        Scope scope(String... scope);

        /**
         * @deprecated since 26.3.0, to be removed
         *
         * <br>Was introduced for testing purposes and was not fully / correctly implements
         * across Scope implementations
         */
        @Deprecated
        Set<String> getPropertyNames();

        /**
         * Root {@link Scope} for global options. The key format should match exactly what
         * is expected to appear in the main configuration file - e.g. metrics-enabled, db, etc.
         *
         * @return a {@link Scope} with access to global configuration properties.
         */
        Scope root();
    }

    public static abstract class AbstractScope implements Scope {

        @Override
        public String get(String key, String defaultValue) {
            return getValue(key, Function.identity(), String.class, defaultValue);
        }

        @Override
        public Integer getInt(String key, Integer defaultValue) {
            return getValue(key, Integer::valueOf, Integer.class, defaultValue);
        }

        @Override
        public String[] getArray(String key) {
            return getValue(key, s -> s.split("\\s*,\\s*"), String[].class, null);
        }

        @Override
        public Long getLong(String key, Long defaultValue) {
            return getValue(key, Long::valueOf, Long.class, defaultValue);
        }

        @Override
        public Boolean getBoolean(String key, Boolean defaultValue) {
            return getValue(key, Boolean::valueOf, Boolean.class, defaultValue);
        }

        protected <T> T getValue(String key, Function<String, T> conversion, Class<T> type, T defaultValue) {
            return Optional.ofNullable(get(key)).map(conversion).orElse(defaultValue);
        }
    }
}
