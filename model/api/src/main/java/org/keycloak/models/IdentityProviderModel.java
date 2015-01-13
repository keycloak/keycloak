/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.models;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>A model type representing the configuration for identity providers. It provides some common properties and also a {@link org.keycloak.models.IdentityProviderModel#config}
 * for configuration options and properties specifics to a identity provider.</p>
 *
 * @author Pedro Igor
 */
public class IdentityProviderModel {

    /**
     * <p>An user-defined identifier to unique identify an identity provider instance.</p>
     */
    private String id;

    /**
     * <p>An identifier used to reference a specific identity provider implementation. The value of this field is the same
     * across instances of the same provider implementation.</p>
     */
    private String providerId;

    /**
     * <p>An user-defined friendly name for an identity provider instance.</p>
     */
    private String name;

    private boolean enabled;

    private boolean updateProfileFirstLogin = true;

    /**
     * <p>A map containing the configuration and properties for a specific identity provider instance and implementation. The items
     * in the map are understood by the identity provider implementation.</p>
     */
    private Map<String, String> config = new HashMap<String, String>();

    public IdentityProviderModel() {
        this(null, null, null, null);
    }

    public IdentityProviderModel(String providerId, String id, String name, Map<String, String> config) {
        this.providerId = providerId;
        this.id = id;
        this.name = name;

        if (config != null) {
            this.config.putAll(config);
        }
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProviderId() {
        return this.providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isUpdateProfileFirstLogin() {
        return this.updateProfileFirstLogin;
    }

    public void setUpdateProfileFirstLogin(boolean updateProfileFirstLogin) {
        this.updateProfileFirstLogin = updateProfileFirstLogin;
    }

    public Map<String, String> getConfig() {
        return this.config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }
}
