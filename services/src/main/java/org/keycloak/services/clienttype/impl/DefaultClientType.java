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
import org.keycloak.representations.idm.ClientTypeRepresentation;
import org.keycloak.services.clienttype.client.TypeAwareClientModelDelegate;

import java.util.Optional;
import java.util.Set;

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
        // Each property is applicable by default if not configured for the particular client type
        return getConfiguration(optionName)
                .map(ClientTypeRepresentation.PropertyConfig::getApplicable)
                .orElse(true);
    }

    @Override
    public <T> T getTypeValue(String optionName, Class<T> optionType) {

        return getConfiguration(optionName)
                .map(ClientTypeRepresentation.PropertyConfig::getValue)
                .map(optionType::cast).orElse(null);
    }

    @Override
    public Set<String> getOptionNames() {
        return clientType.getConfig().keySet();
    }

    @Override
    public ClientModel augment(ClientModel client) {
        return new TypeAwareClientModelDelegate(this, () -> client);
    }

    private Optional<ClientTypeRepresentation.PropertyConfig> getConfiguration(String optionName) {
        return Optional.ofNullable(clientType.getConfig().get(optionName));
    }
}