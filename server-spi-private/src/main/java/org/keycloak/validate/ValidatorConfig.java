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
import java.util.HashMap;
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

    public Map<String, Object> asMap(){
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
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    public Integer getInt(String key) {
        return getIntOrDefault(key, null);
    }

    public Integer getIntOrDefault(String key, Integer defaultValue) {
        Object value = config.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return new Integer((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return defaultValue;
    }

    public Long getLong(String key) {
        return getLongOrDefault(key, null);
    }

    public Long getLongOrDefault(String key, Long defaultValue) {
        Object value = config.get(key);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return new Long((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return defaultValue;
    }

    public Double getDouble(String key) {
        return getDoubleOrDefault(key, null);
    }

    public Double getDoubleOrDefault(String key, Double defaultValue) {
        Object value = config.get(key);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return defaultValue;
    }

    public Boolean getBoolean(String key) {
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

    public Set<String> getStringSet(String key) {
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

    /**
     * Get regex Pattern from the configuration. String can be used and it is compiled into Pattern.
     * 
     * @param key to get
     * @return Pattern or null
     */
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

    public static ValidatorConfigBuilder builder() {
        return new ValidatorConfigBuilder();
    }

    public static class ValidatorConfigBuilder {

        private Map<String, Object> config = new HashMap<>();

        public ValidatorConfig build() {
            return ValidatorConfig.configFromMap(this.config);
        }

        public ValidatorConfigBuilder config(String name, Object value) {
            config.put(name, value);
            return this;
        }

        /**
         * Add all configurations from map
         */
        public ValidatorConfigBuilder config(Map<String, Object> values) {
            if(values!=null) {
                config.putAll(values);
            }
            return this;
        }
        
        /**
         * Add all configurations from other config
         */
        public ValidatorConfigBuilder config(ValidatorConfig values) {
            if(values != null && values.config != null) {
                config.putAll(values.config);
            }
            return this;
        }
    }

    @Override
    public String toString() {
        return "ValidatorConfig{" + "config=" + config + '}';
    }
}
