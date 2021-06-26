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
package org.keycloak.testsuite.model;

import org.keycloak.Config.ConfigProvider;
import org.keycloak.Config.Scope;
import org.keycloak.Config.SystemPropertiesScope;
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.common.util.SystemEnvProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/**
 *
 * @author hmlnarik
 */
public class Config implements ConfigProvider {

    private final Properties systemProperties = new SystemEnvProperties();

    private final Map<String, String> defaultProperties = new HashMap<>();
    private final ThreadLocal<Map<String, String>> properties = new ThreadLocal<Map<String, String>>() {
        @Override
        protected Map<String, String> initialValue() {
            return new HashMap<>();
        }
    };
    private final BooleanSupplier useGlobalConfigurationFunc;

    public Config(BooleanSupplier useGlobalConfigurationFunc) {
        this.useGlobalConfigurationFunc = useGlobalConfigurationFunc;
    }

    void reset() {
        if (useGlobalConfigurationFunc.getAsBoolean()) {
            defaultProperties.clear();
        } else {
            properties.remove();
        }
    }

    public class SpiConfig {

        private final String prefix;

        public SpiConfig(String prefix) {
            this.prefix = prefix;
        }

        public ProviderConfig provider(String provider) {
            return new ProviderConfig(this, prefix + provider + ".");
        }

        public SpiConfig defaultProvider(String defaultProviderId) {
            return config("provider", defaultProviderId);
        }

        public SpiConfig config(String key, String value) {
            if (value == null) {
                getConfig().remove(prefix + key);
            } else {
                getConfig().put(prefix + key, value);
            }
            return this;
        }

        public SpiConfig spi(String spiName) {
            return new SpiConfig(spiName + ".");
        }
    }

    public class ProviderConfig {

        private final SpiConfig spiConfig;
        private final String prefix;

        public ProviderConfig(SpiConfig spiConfig, String prefix) {
            this.spiConfig = spiConfig;
            this.prefix = prefix;
        }

        public ProviderConfig config(String key, String value) {
            if (value == null) {
                getConfig().remove(prefix + key);
            } else {
                getConfig().put(prefix + key, value);
            }
            return this;
        }

        public ProviderConfig provider(String provider) {
            return spiConfig.provider(provider);
        }

        public SpiConfig spi(String spiName) {
            return new SpiConfig(spiName + ".");
        }

    }

    private class MapConfigScope extends SystemPropertiesScope {

        public MapConfigScope(String prefix) {
            super(prefix);
        }

        @Override
        public String get(String key, String defaultValue) {
            String v = replaceProperties(getConfig().get(prefix + key));
            if (v == null || v.isEmpty()) {
                v = System.getProperty("keycloak." + prefix + key, defaultValue);
            }
            return v != null && ! v.isEmpty() ? v : null;
        }

        @Override
        public Scope scope(String... scope) {
            StringBuilder sb = new StringBuilder();
            sb.append(prefix);
            for (String s : scope) {
                sb.append(s);
                sb.append(".");
            }
            return new MapConfigScope(sb.toString());
        }
    }

    @Override
    public String getProvider(String spiName) {
        return getConfig().get(spiName + ".provider");
    }

    public Map<String, String> getConfig() {
        return useGlobalConfigurationFunc.getAsBoolean() ? defaultProperties : properties.get();
    }

    private String replaceProperties(String value) {
        return StringPropertyReplacer.replaceProperties(value, systemProperties);
    }

    @Override
    public Scope scope(String... scope) {
        StringBuilder sb = new StringBuilder();
        for (String s : scope) {
            sb.append(s);
            sb.append(".");
        }
        return new MapConfigScope(sb.toString());
    }

    public SpiConfig spi(String spiName) {
        return new SpiConfig(spiName + ".");
    }

    @Override
    public String toString() {
        return getConfig().entrySet().stream()
          .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
          .map(e -> e.getKey() + " = " + e.getValue())
          .collect(Collectors.joining("\n    "));
    }
}
