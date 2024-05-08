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

package org.keycloak.services.clienttype.impl;

import org.keycloak.client.clienttype.ClientType;
import org.keycloak.models.ClientModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTypeRepresentation;
import org.keycloak.services.clienttype.client.TypeAwareClientModelDelegate;
import org.keycloak.services.clienttype.client.TypeDefaultedClientRepresentation;

import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultClientType implements ClientType {

    private final ClientTypeRepresentation clientType;

    public DefaultClientType(ClientTypeRepresentation clientType) {
        this.clientType = clientType;
    }

    @Override
    public String getName() {
        return clientType.getName();
    }

    @Override
    public boolean isApplicable(String optionName) {
        ClientTypeRepresentation.PropertyConfig cfg = clientType.getConfig().get(optionName);

        // Each property is applicable by default if not configured for the particular client type
        return (cfg != null && cfg.getApplicable() != null) ? cfg.getApplicable() : true;
    }

    @Override
    public boolean isReadOnly(String optionName) {
        ClientTypeRepresentation.PropertyConfig cfg = clientType.getConfig().get(optionName);

        // Each property is writable by default if not configured for the particular type
        return (cfg != null && cfg.getReadOnly() != null) ? cfg.getReadOnly() : false;
    }

    @Override
    public <T> T getDefaultValue(String optionName, Class<T> optionType) {
        ClientTypeRepresentation.PropertyConfig cfg = clientType.getConfig().get(optionName);

        return (cfg != null && cfg.getDefaultValue() != null) ? optionType.cast(cfg.getDefaultValue()) : null;
    }

    @Override
    public Map<String, ClientTypeRepresentation.PropertyConfig> getConfiguration() {
        return clientType.getConfig();
    }

    @Override
    public ClientModel augment(ClientModel client) {
        return new TypeAwareClientModelDelegate(this, () -> client);
    }

    @Override
    public ClientRepresentation augment(ClientRepresentation representation) {
        return new TypeDefaultedClientRepresentation(this, representation);
    }
}