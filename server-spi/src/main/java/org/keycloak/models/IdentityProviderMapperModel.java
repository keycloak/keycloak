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

package org.keycloak.models;

import com.fasterxml.jackson.core.type.TypeReference;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Specifies a mapping from broker login to user data.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class IdentityProviderMapperModel implements Serializable {
    public static final String SYNC_MODE = "syncMode";

    private static final TypeReference<List<StringPair>> MAP_TYPE_REPRESENTATION = new TypeReference<List<StringPair>>() {
    };

    protected String id;
    protected String name;
    protected String identityProviderAlias;
    protected String identityProviderMapper;
    protected Map<String, String> config;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentityProviderAlias() {
        return identityProviderAlias;
    }

    public void setIdentityProviderAlias(String identityProviderAlias) {
        this.identityProviderAlias = identityProviderAlias;
    }

    public String getIdentityProviderMapper() {
        return identityProviderMapper;
    }

    public void setIdentityProviderMapper(String identityProviderMapper) {
        this.identityProviderMapper = identityProviderMapper;
    }

    public IdentityProviderMapperSyncMode getSyncMode() {
        return IdentityProviderMapperSyncMode.valueOf(getConfig().getOrDefault(SYNC_MODE, "LEGACY"));
    }

    public void setSyncMode(IdentityProviderMapperSyncMode syncMode) {
        getConfig().put(SYNC_MODE, syncMode.toString());
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public Map<String, String> getConfigMap(String configKey) {
        String configMap = config.get(configKey);

        try {
            List<StringPair> map = JsonSerialization.readValue(configMap, MAP_TYPE_REPRESENTATION);
            return map.stream().collect(Collectors.toMap(StringPair::getKey, StringPair::getValue));
        } catch (IOException e) {
            throw new RuntimeException("Could not deserialize json: " + configMap, e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdentityProviderMapperModel that = (IdentityProviderMapperModel) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    static class StringPair {
        private String key;
        private String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
