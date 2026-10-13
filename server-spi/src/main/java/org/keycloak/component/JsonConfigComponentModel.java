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

package org.keycloak.component;

import org.keycloak.provider.Provider;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Component model backed by JSON configuration. Useful for providers, which rely on JSON configuration rather than on ComponentModel, which is directly
 * persisted as entity in the DB (store).
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JsonConfigComponentModel extends ComponentModel {

    private final String providerType;
    private final String providerId;
    private final String componentId;
    private final JsonNode configNode;

    /**
     * @param providerType
     * @param realmId
     * @param providerId
     * @param configNode JSON configuration of this provider. For example if node corresponds to JSON like "{\"foo\":\"bar\"}", then
     *                   component configuration is supposed to have one configuration option "foo" with value "bar"
     */
    public JsonConfigComponentModel(Class<? extends Provider> providerType, String realmId, String providerId, JsonNode configNode) {
        checkNotNull(providerType, "providerType must be not null");
        checkNotNull(realmId, "realmId must be not null");
        checkNotNull(providerId, "providerId must be not null");
        checkNotNull(configNode, "configNode must be not null for provider " + providerId);
        this.providerType = providerType.getName();
        this.providerId = providerId;
        this.configNode = configNode;

        // We don't have realm model ID of the component, so componentId based on the realmId, providerType, providerId and hashCode of configurations.
        this.componentId = realmId + "::" + providerType + "::" + this.providerId + "::" + configNode.hashCode();
    }

    private void checkNotNull(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }


    @Override
    public String getProviderType() {
        return providerType;
    }

    @Override
    public String getProviderId() {
        return providerId;
    }

    @Override
    public String getName() {
        return componentId + "-config";
    }

    @Override
    public String getId() {
        return componentId;
    }

    @Override
    public boolean get(String key, boolean defaultValue) {
        JsonNode sub = configNode.get(key);
        return sub == null ? defaultValue : sub.asBoolean();
    }

    @Override
    public long get(String key, long defaultValue) {
        JsonNode sub = configNode.get(key);
        return sub == null ? defaultValue : sub.asLong();
    }

    @Override
    public int get(String key, int defaultValue) {
        JsonNode sub = configNode.get(key);
        return sub == null ? defaultValue : sub.asInt();
    }

    @Override
    public String get(String key, String defaultValue) {
        JsonNode sub = configNode.get(key);
        return sub == null ? defaultValue : sub.asText();
    }

    @Override
    public String get(String key) {
        return get(key, null);
    }

}
