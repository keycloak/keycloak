/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.convert;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.utils.StringUtil;

/**
 * A typed wrapper around a {@link Map} based {@link Converter} configuration.
 */
public class ConverterConfig {

    /**
     * An empty {@link ConverterConfig}.
     */
    public static final ConverterConfig EMPTY = new ConverterConfig(Collections.emptyMap());

    public static boolean isEmpty(ConverterConfig config) {
        return EMPTY.equals(Optional.ofNullable(config).orElse(EMPTY));
    }

    /**
     * Holds the backing map for the {@link Converter} config.
     */
    private final Map<String, Object> config;

    public ConverterConfig(Map<String, Object> config) {
        this.config = config;
    }

    /**
     * Static helper to create a {@link ConverterConfig} from the given {@code map}.
     *
     * @param map
     * @return
     */
    public static ConverterConfig configFromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return EMPTY;
        }
        return new ConverterConfig(map);
    }

    public Map<String, Object> asMap() {
        return config;
    }

    public boolean containsKey(String key) {
        return config.containsKey(key);
    }

    public int size() {
        return config.size();
    }

    public boolean isEmpty() {
        return config.isEmpty();
    }

    public Object get(String key) {
        return config.get(key);
    }

    public Object getOrDefault(String key, Object defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    public String getString(String key) {
        return getStringOrDefault(key, null);
    }

    public String getStringOrDefault(String key, String defaultValue) {
        Object value = config.get(key);
        if (value instanceof String && StringUtil.isNotBlank((String) value)) {
            return (String) value;
        }
        return defaultValue;
    }

    public Boolean getBoolean(String key) {
        return getBooleanOrDefault(key, null);
    }

    public Boolean getBooleanOrDefault(String key, Boolean defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String stringValue = ((String) value).trim();
            if ("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue)) {
                return Boolean.parseBoolean(stringValue);
            }
        }
        return defaultValue;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public int hashCode() {
        return config.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Objects.equals(config, ((ConverterConfig) obj).config);
    }

    public static class Builder {

        private final Map<String, Object> config = new HashMap<>();

        public Builder config(String key, Object value) {
            config.put(key, value);
            return this;
        }

        public Builder config(Map<String, Object> values) {
            if (values != null) {
                config.putAll(values);
            }
            return this;
        }

        public ConverterConfig build() {
            return new ConverterConfig(config);
        }
    }
}
