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

package org.keycloak.services.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.Config;
import org.keycloak.common.util.StringPropertyReplacer;

import java.util.Properties;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JsonConfigProvider implements Config.ConfigProvider {

    private Properties properties;

    private JsonNode config;

    public JsonConfigProvider(JsonNode config, Properties properties) {
        this.config = config;
        this.properties = properties;
    }

    @Override
    public String getProvider(String spi) {
        JsonNode n = getNode(config, spi, "provider");
        return n != null ? replaceProperties(n.textValue()) : null;
    }

    @Override
    public Config.Scope scope(String... path) {
        return new JsonScope(getNode(config, path));
    }

    private static JsonNode getNode(JsonNode root, String... path) {
        if (root == null) {
            return null;
        }
        JsonNode n = root;
        for (String p : path) {
            n = n.get(p);
            if (n == null) {
                return null;
            }
        }
        return n;
    }

    private String replaceProperties(String value) {
        return StringPropertyReplacer.replaceProperties(value, properties);
    }

    public class JsonScope implements Config.Scope {

        private JsonNode config;

        public JsonScope(JsonNode config) {
            this.config = config;
        }

        @Override
        public String get(String key) {
            return get(key, null);
        }

        @Override
        public String get(String key, String defaultValue) {
            if (config == null) {
                return defaultValue;
            }
            JsonNode n = config.get(key);
            if (n == null) {
                return defaultValue;
            }
            return replaceProperties(n.textValue());
        }

        @Override
        public String[] getArray(String key) {
            if (config == null) {
                return null;
            }

            JsonNode n = config.get(key);
            if (n == null) {
                return null;
            } else if (n.isArray()) {
                String[] a = new String[n.size()];
                for (int i = 0; i < a.length; i++) {
                    a[i] = replaceProperties(n.get(i).textValue());
                }
                return a;
            } else {
               return new String[] { replaceProperties(n.textValue()) };
            }
        }

        @Override
        public Integer getInt(String key) {
            return getInt(key, null);
        }

        @Override
        public Integer getInt(String key, Integer defaultValue) {
            if (config == null) {
                return defaultValue;
            }
            JsonNode n = config.get(key);
            if (n == null) {
                return defaultValue;
            }
            if (n.isTextual()) {
                return Integer.parseInt(replaceProperties(n.textValue()));
            } else {
                return n.intValue();
            }
        }

        @Override
        public Long getLong(String key) {
            return getLong(key, null);
        }

        @Override
        public Long getLong(String key, Long defaultValue) {
            if (config == null) {
                return defaultValue;
            }
            JsonNode n = config.get(key);
            if (n == null) {
                return defaultValue;
            }
            if (n.isTextual()) {
                return Long.parseLong(replaceProperties(n.textValue()));
            } else {
                return n.longValue();
            }
        }

        @Override
        public Boolean getBoolean(String key) {
            return getBoolean(key, null);
        }

        @Override
        public Boolean getBoolean(String key, Boolean defaultValue) {
            if (config == null) {
                return defaultValue;
            }
            JsonNode n = config.get(key);
            if (n == null) {
                return defaultValue;
            }
            if (n.isTextual()) {
                return Boolean.parseBoolean(replaceProperties(n.textValue()));
            } else {
                return n.booleanValue();
            }
        }

        @Override
        public Config.Scope scope(String... path) {
            return new JsonScope(getNode(config, path));
        }

    }

}
