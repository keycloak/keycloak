/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * The only reason for this config source is to keep the Keycloak specific properties when configuring the server so that
 * they are read again when running the server after the configuration.
 */
public class SysPropConfigSource implements ConfigSource {

    // Key is name of the property converted to the internal (dot) format like "kc.foo.bar.baz". Value is the real name of the corresponding system property
    // like for example "kc.foo-bar-baz" .
    private static Map<String, String> CANONICAL_FORMAT_TO_SYSPROP = new ConcurrentHashMap<>();
    static {
        // Pre-initialize the map now.
        getPropertiesInternal();
    }

    public Map<String, String> getProperties() {
        return getPropertiesInternal();
    }

    private static Map<String, String> getPropertiesInternal() {
        Map<String, String> output = new TreeMap<>();
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX)) {
                String keyInInternalFormat = PropertyMappers.canonicalFormat(key);
                CANONICAL_FORMAT_TO_SYSPROP.putIfAbsent(keyInInternalFormat, key);
                output.put(keyInInternalFormat, entry.getValue().toString());
            }
        }
        return output;
    }

    public String getValue(final String propertyName) {
        String sysPropertyName = CANONICAL_FORMAT_TO_SYSPROP.getOrDefault(propertyName, propertyName);
        return System.getProperty(sysPropertyName);
    }

    public String getName() {
        return "System properties";
    }

    public int getOrdinal() {
        return 400;
    }
}
