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

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.client.clienttype.ClientType;
import org.keycloak.models.ClientModel;
import org.keycloak.representations.idm.ClientTypeRepresentation;
import org.keycloak.services.clienttype.client.TypeAwareClientModelDelegate;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultClientType implements ClientType {

    private final ClientTypeRepresentation clientType;
    private final ClientType parentClientType;

    public DefaultClientType(ClientTypeRepresentation clientType, ClientType parentClientType) {
        this.clientType = clientType;
        this.parentClientType = parentClientType;
    }

    @Override
    public String getName() {
        return clientType.getName();
    }

    @Override
    public boolean isApplicable(String optionName) {
        ClientTypeRepresentation.PropertyConfig propertyConfig = clientType.getConfig().get(optionName);
        if (propertyConfig != null) {
            return propertyConfig.getApplicable();
        }

        if (parentClientType != null) {
            return parentClientType.isApplicable(optionName);
        }

        return true;
    }

    @Override
    public <T> T getTypeValue(String optionName, Class<T> optionType) {
        ClientTypeRepresentation.PropertyConfig propertyConfig = clientType.getConfig().get(optionName);
        if (propertyConfig != null) {
            return optionType.cast(propertyConfig.getValue());
        } else if (parentClientType != null) {
            return parentClientType.getTypeValue(optionName, optionType);
        }
        return null;
    }

    @Override
    public Set<String> getOptionNames() {
        Stream<String> optionNames = clientType.getConfig().keySet().stream();
        if (parentClientType != null) {
            optionNames = Stream.concat(optionNames, parentClientType.getOptionNames().stream());
        }
        return optionNames.collect(Collectors.toSet());
    }

    @Override
    public ClientModel augment(ClientModel client) {
        return new TypeAwareClientModelDelegate(this, () -> client);
    }
}
