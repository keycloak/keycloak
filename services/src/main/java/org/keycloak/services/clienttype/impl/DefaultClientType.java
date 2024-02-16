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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import org.jboss.logging.Logger;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTypeRepresentation;
import org.keycloak.client.clienttype.ClientType;
import org.keycloak.client.clienttype.ClientTypeException;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultClientType implements ClientType {

    private static final Logger logger = Logger.getLogger(DefaultClientType.class);

    // Will be used as reference in JSON. Probably just temporary solution
    private static final String REFERENCE_PREFIX = "ref::";

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
        for (Map.Entry<String, ClientTypeRepresentation.PropertyConfig> property : clientType.getConfig().entrySet()) {
            ClientTypeRepresentation.PropertyConfig propertyConfig = property.getValue();
            if (!propertyConfig.getApplicable()) continue;
            if (propertyConfig.getDefaultValue() != null) {
                if (clientRepresentationProperties.containsKey(property.getKey())) {
                    // Java property on client representation
                    try {
                        PropertyDescriptor propertyDescriptor = clientRepresentationProperties.get(property.getKey());
                        Method setter = propertyDescriptor.getWriteMethod();
                        Object defaultVal = propertyConfig.getDefaultValue();
                        if (defaultVal instanceof String && defaultVal.toString().startsWith(REFERENCE_PREFIX)) {
                            // TODO:client-types re-verify or remove support for "ref::" entirely from the codebase
                            throw new UnsupportedOperationException("Not supported to use ref:: references");
                            // Reference. We need to found referred value and call the setter with it
//                            String referredPropertyName = defaultVal.toString().substring(REFERENCE_PREFIX.length());
//                            Object referredPropertyVal = clientType.getReferencedProperties().get(referredPropertyName);
//                            if (referredPropertyVal == null) {
//                                logger.warnf("Reference '%s' not found used in property '%s' of client type '%s'", defaultVal.toString(), property.getKey(), clientType.getName());
//                                throw new ClientTypeException("Cannot set property on client");
//                            }
//
//                            // Generic collections
//                            Type genericType = setter.getGenericParameterTypes()[0];
//                            JavaType jacksonType = JsonSerialization.mapper.constructType(genericType);
//                            Object converted = JsonSerialization.mapper.convertValue(referredPropertyVal, jacksonType);
//
//                            setter.invoke(createdClient, converted);
                        }  else {
                            Type genericType = setter.getGenericParameterTypes()[0];

                            Object converted;
                            if (!defaultVal.getClass().equals(genericType)) {
                                JavaType jacksonType = JsonSerialization.mapper.constructType(genericType);
                                converted = JsonSerialization.mapper.convertValue(defaultVal, jacksonType);
                            } else {
                                converted = defaultVal;
                            }

                            setter.invoke(createdClient, converted);
                        }
                    } catch (Exception e) {
                        logger.warnf("Cannot set property '%s' on client with value '%s'. Check configuration of the client type '%s'", property.getKey(), propertyConfig.getDefaultValue(), clientType.getName());
                        throw new ClientTypeException("Cannot set property on client", e);
                    }
                } else {
                    // Client attribute
                    if (createdClient.getAttributes() == null) {
                        createdClient.setAttributes(new HashMap<>());
                    }
                    createdClient.getAttributes().put(property.getKey(), propertyConfig.getDefaultValue().toString());
                }
            }
        }
    }

    @Override
    public void onUpdate(ClientModel currentClient, ClientRepresentation newClient) throws ClientTypeException{
        ClientRepresentation oldClient = ModelToRepresentation.toRepresentation(currentClient, session);
        for (Map.Entry<String, ClientTypeRepresentation.PropertyConfig> property : clientType.getConfig().entrySet()) {
            String propertyName = property.getKey();
            ClientTypeRepresentation.PropertyConfig propertyConfig = property.getValue();

            Object oldVal = getClientProperty(oldClient, propertyName);
            Object newVal = getClientProperty(newClient, propertyName);

            // Validate that read-only client properties were not changed. Also validate that non-applicable properties were not changed.
            if (!propertyConfig.getApplicable() || propertyConfig.getReadOnly()) {
                if (!ObjectUtil.isEqualOrBothNull(oldVal, newVal)) {
                    logger.warnf("Cannot change property '%s' of client '%s' . Old value '%s', New value '%s'", propertyName, currentClient.getClientId(), oldVal, newVal);
                    throw new ClientTypeException("Cannot change property of client as it is not allowed");
                }
            }
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