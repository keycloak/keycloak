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
 *
 */

package org.keycloak.representations.idm;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientTypeRepresentation {

    @JsonProperty("name")
    private String name;

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("config")
    private Map<String, PropertyConfig> config;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Map<String, PropertyConfig> getConfig() {
        return config;
    }

    public void setConfig(Map<String, PropertyConfig> config) {
        this.config = config;
    }

    @JsonProperty("referenced-properties")
    protected Map<String, Object> referencedProperties = new HashMap<>();

    public Map<String, Object> getReferencedProperties() {
        return referencedProperties;
    }

    public void setReferencedProperties(Map<String, Object> referencedProperties) {
        this.referencedProperties = referencedProperties;
    }

    public static class PropertyConfig {

        @JsonProperty("applicable")
        private Boolean applicable;

        @JsonProperty("read-only")
        private Boolean readOnly;

        @JsonProperty("default-value")
        private Object defaultValue;

        public Boolean getApplicable() {
            return applicable;
        }

        public void setApplicable(Boolean applicable) {
            this.applicable = applicable;
        }

        public Boolean getReadOnly() {
            return readOnly;
        }

        public void setReadOnly(Boolean readOnly) {
            this.readOnly = readOnly;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
        }
    }
}