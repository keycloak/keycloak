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

import java.util.List;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ProtocolMapperPolicyValidator;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Enforces a whitelist of allowed protocol mapper types during client create/update
 * and Admin REST API protocol mapper operations.
 */
public class AllowedProtocolMappersExecutor implements ClientPolicyExecutorProvider<AllowedProtocolMappersExecutor.Configuration> {

    private AllowedProtocolMappersExecutor.Configuration configuration;

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        List<String> allowedMapperProviders = getAllowedMapperProviders();
        if (allowedMapperProviders == null || allowedMapperProviders.isEmpty()) {
            return;
        }

        switch (context.getEvent()) {
            case REGISTER:
            case UPDATE:
                ClientCRUDContext clientCrudContext = (ClientCRUDContext) context;
                ClientRepresentation proposedClient = clientCrudContext.getProposedClientRepresentation();
                ProtocolMapperPolicyValidator.validateProtocolMappers(
                        proposedClient.getProtocolMappers(),
                        allowedMapperProviders,
                        clientCrudContext.getTargetClient());
                break;
            default:
                return;
        }
    }

    @Override
    public void setupConfiguration(AllowedProtocolMappersExecutor.Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<AllowedProtocolMappersExecutor.Configuration> getExecutorConfigurationClass() {
        return AllowedProtocolMappersExecutor.Configuration.class;
    }

    @Override
    public String getProviderId() {
        return AllowedProtocolMappersExecutorFactory.PROVIDER_ID;
    }

    private List<String> getAllowedMapperProviders() {
        if (configuration.getAllowedProtocolMapperTypes() != null) {
            return configuration.getAllowedProtocolMapperTypes();
        }
        Object value = configuration.getConfigAsMap().get(AllowedProtocolMappersExecutorFactory.ALLOWED_PROTOCOL_MAPPER_TYPES);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return null;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {

        @JsonProperty(AllowedProtocolMappersExecutorFactory.ALLOWED_PROTOCOL_MAPPER_TYPES)
        protected List<String> allowedProtocolMapperTypes;

        public List<String> getAllowedProtocolMapperTypes() {
            return allowedProtocolMapperTypes;
        }

        public void setAllowedProtocolMapperTypes(List<String> allowedProtocolMapperTypes) {
            this.allowedProtocolMapperTypes = allowedProtocolMapperTypes;
        }
    }
}
