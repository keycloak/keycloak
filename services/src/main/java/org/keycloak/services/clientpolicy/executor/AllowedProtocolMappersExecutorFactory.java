/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.executor;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;

/**
 * Factory for the allowed-protocol-mappers client policy executor.
 */
public class AllowedProtocolMappersExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "allowed-protocol-mappers";

    public static final String ALLOWED_PROTOCOL_MAPPER_TYPES = "allowed-protocol-mapper-types";

    private final List<ProviderConfigProperty> configProperties = new LinkedList<>();
    private KeycloakSessionFactory sessionFactory;

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new AllowedProtocolMappersExecutor();
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        this.sessionFactory = factory;

        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(ALLOWED_PROTOCOL_MAPPER_TYPES);
        property.setLabel("allowed-protocol-mappers.label");
        property.setHelpText("allowed-protocol-mappers.tooltip");
        property.setType(ProviderConfigProperty.MULTIVALUED_LIST_TYPE);
        property.setOptions(getProtocolMapperFactoryIds());
        configProperties.add(property);
    }

    private List<String> getProtocolMapperFactoryIds() {
        return sessionFactory.getProviderFactoriesStream(ProtocolMapper.class)
                .map(ProviderFactory::getId)
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "When present, it allows to specify whitelist of protocol mapper types, which will be allowed when creating or updating clients and protocol mappers";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }
}
