/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds the configuration for a required action.
 */
public class RequiredActionConfigModel implements Serializable {

    protected String id;

    protected String providerId;

    protected String alias;

    protected Map<String, String> config = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean containsConfigKey(String key) {
        return config != null && config.containsKey(key);
    }

    public String getConfigValue(String key) {
        return getConfigValue(key, null);
    }

    public String getConfigValue(String key, String defaultValue) {
        if (config == null) {
            return defaultValue;
        }
        String value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
