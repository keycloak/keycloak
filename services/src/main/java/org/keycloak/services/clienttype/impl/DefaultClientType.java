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

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.client.clienttype.ClientType;
import org.keycloak.client.clienttype.ClientTypeException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTypeRepresentation;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultClientType implements ClientType {

    private static final Logger logger = Logger.getLogger(DefaultClientType.class);

    private final KeycloakSession session;
    private final ClientTypeRepresentation clientType;

    private final Map<String, PropertyDescriptor> clientRepresentationProperties;

    public DefaultClientType(KeycloakSession session, ClientTypeRepresentation clientType, Map<String, PropertyDescriptor> clientRepresentationProperties) {
        this.session = session;
        this.clientType = clientType;
        this.clientRepresentationProperties = clientRepresentationProperties;
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
    public void onCreate(ClientRepresentation createdClient) throws ClientTypeException {
        // Create empty client augmented with the applicable default client type values.
        ClientRepresentation defaultClientRep = augmentClient(new ClientRepresentation());

        validateClientRequest(createdClient, defaultClientRep);

        augmentClient(createdClient);
    }

    @Override
    public void onUpdate(ClientModel currentClient, ClientRepresentation newClient) throws ClientTypeException {
        ClientRepresentation currentRep = ModelToRepresentation.toRepresentation(currentClient, session);
        validateClientRequest(newClient, currentRep);
    }

    protected void validateClientRequest(ClientRepresentation newClient, ClientRepresentation currentClient) throws ClientTypeException {
        List<String> validationErrors = clientType.getConfig().entrySet().stream()
                .filter(property -> clientPropertyHasInvalidChangeRequested(currentClient, newClient, property.getKey(), property.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (validationErrors.size() > 0) {
            throw new ClientTypeException(
                    "Cannot change property of client as it is not allowed by the specified client type.",
                    validationErrors.toArray());
        }
    }

    protected ClientRepresentation augmentClient(ClientRepresentation client) {
        clientType.getConfig().entrySet()
                .forEach(property -> setClientProperty(client, property.getKey(), property.getValue()));
        return client;
    }

    private boolean clientPropertyHasInvalidChangeRequested(
            ClientRepresentation oldClient,
            ClientRepresentation newClient,
            String propertyName,
            ClientTypeRepresentation.PropertyConfig propertyConfig) {
        Object newClientProperty = getClientProperty(newClient, propertyName);
        Object oldClientProperty = getClientProperty(oldClient, propertyName);

        return (
                    // Validate that non-applicable client properties were not changed.
                    !propertyConfig.getApplicable() &&
                    !Objects.isNull(newClientProperty) &&
                    !Objects.equals(oldClientProperty, newClientProperty)
                ) || (
                    // Validate that applicable read-only client properties were not changed.
                    propertyConfig.getApplicable() &&
                    propertyConfig.getReadOnly() &&
                    !Objects.isNull(newClientProperty) &&
                    !Objects.equals(oldClientProperty, newClientProperty)
                );
    }

    private void setClientProperty(ClientRepresentation client,
                               String propertyName,
                               ClientTypeRepresentation.PropertyConfig propertyConfig) {

        if (!propertyConfig.getApplicable() || propertyConfig.getDefaultValue() == null) {
            return;
        }

        if (clientRepresentationProperties.containsKey(propertyName)) {
            // Java property on client representation
            Method setter = clientRepresentationProperties.get(propertyName).getWriteMethod();
            try {
                setter.invoke(client, propertyConfig.getDefaultValue());
            } catch (Exception e) {
                logger.warnf("Cannot set property '%s' on client with value '%s'. Check configuration of the client type '%s'", propertyName, propertyConfig.getDefaultValue(), clientType.getName());
                throw new ClientTypeException("Cannot set property on client", e);
            }
        } else {
            // Client attribute
            if (client.getAttributes() == null) {
                client.setAttributes(new HashMap<>());
            }
            client.getAttributes().put(propertyName, propertyConfig.getDefaultValue().toString());
        }
    }

    private Object getClientProperty(ClientRepresentation client, String propertyName) {
        PropertyDescriptor propertyDescriptor = clientRepresentationProperties.get(propertyName);

        if (propertyDescriptor != null) {
            // Java property
            Method getter = propertyDescriptor.getReadMethod();
            try {
                return getter.invoke(client);
            } catch (Exception e) {
                logger.warnf("Cannot read property '%s' on client '%s'. Client type is '%s'", propertyName, client.getClientId(), clientType.getName());
                throw new ClientTypeException("Cannot read property of client", e);
            }
        } else {
            // Attribute
            return client.getAttributes() == null ? null : client.getAttributes().get(propertyName);
        }
    }
}