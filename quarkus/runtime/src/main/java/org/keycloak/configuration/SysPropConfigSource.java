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

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * The only reason for this config source is to keep the Keycloak specific properties when configuring the server so that
 * they are read again when running the server after the configuration.
 */
public class SysPropConfigSource implements ConfigSource {

    private final Map<String, String> properties = new TreeMap<>();

    public SysPropConfigSource() {
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX)) {
                properties.put(key, entry.getValue().toString());
            }
        }
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    public String getValue(final String propertyName) {
        return System.getProperty(propertyName);
    }

    public String getName() {
        return "KcSysPropConfigSource";
    }

    public int getOrdinal() {
        return 400;
    }
}
