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
package org.keycloak.validate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A typed wrapper around a {@link Map} based {@link Validator} configuration.
 */
public class ValidatorConfig {

    /**
     * An empty {@link ValidatorConfig}.
     */
    public static final ValidatorConfig EMPTY = new ValidatorConfig(Collections.emptyMap());

    /**
     * Holds the backing map for the {@link Validator} config.
     */
    private final Map<String, Object> config;

    /**
     * Creates a new {@link ValidatorConfig} from the given {@code map}.
     *
     * @param config
     */
    public ValidatorConfig(Map<String, Object> config) {
        this.config = config;
    }

    /**
     * Static helper to create a {@link ValidatorConfig} from the given {@code map}.
     *
     * @param map
     * @return
     */
    public static ValidatorConfig configFromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return EMPTY;
        }
        return new ValidatorConfig(map);
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
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    public Integer getIntOrDefault(String key) {
        return getIntOrDefault(key, null);
    }

    public Integer getIntOrDefault(String key, Integer defaultValue) {
        Object value = config.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        return defaultValue;
    }

    public Double getDoubleOrDefault(String key) {
        return getDoubleOrDefault(key, null);
    }

    public Double getDoubleOrDefault(String key, Double defaultValue) {
        Object value = config.get(key);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        return defaultValue;
    }

    public Boolean getBooleanOrDefault(String key) {
        return getBooleanOrDefault(key, null);
    }

    public Boolean getBooleanOrDefault(String key, Boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    public Set<String> getStringSetOrDefault(String key) {
        return getStringSetOrDefault(key, null);
    }

    public Set<String> getStringSetOrDefault(String key, Set<String> defaultValue) {
        Object value = config.get(key);
        if (value instanceof Set) {
            return (Set<String>) value;
        }
        return defaultValue;
    }

    public List<String> getStringListOrDefault(String key) {
        return getStringListOrDefault(key, null);
    }

    public List<String> getStringListOrDefault(String key, List<String> defaultValue) {
        Object value = config.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return defaultValue;
    }

    public Pattern getPattern(String key) {
        return getPatternOrDefault(key, null);
    }

    public Pattern getPatternOrDefault(String key, Pattern defaultValue) {
        Object value = config.get(key);
        if (value instanceof Pattern) {
            return (Pattern) value;
        } else if (value instanceof String) {
            return Pattern.compile((String) value);
        }
        return defaultValue;
    }

    @Override
    public String toString() {
        return "ValidatorConfig{" +
                "config=" + config +
                '}';
    }
}
