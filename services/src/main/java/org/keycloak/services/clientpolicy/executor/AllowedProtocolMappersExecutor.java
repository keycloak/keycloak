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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.keycloak.OAuthErrorException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.admin.ClientProtocolMapperContext;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Checks protocol mapper types and configurations changed through the Admin REST API.
 */
public class AllowedProtocolMappersExecutor implements ClientPolicyExecutorProvider<AllowedProtocolMappersExecutor.Configuration> {

    private Configuration configuration = new Configuration();

    @Override
    public void setupConfiguration(Configuration config) {
        configuration = config == null ? new Configuration() : config;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (!(context instanceof ClientProtocolMapperContext mapperContext) || mapperContext.getTargetClient() == null) {
            return;
        }

        List<ProtocolMapperRepresentation> proposedMappers = mapperContext.getProposedProtocolMappers();
        if (proposedMappers == null) {
            return;
        }

        Set<String> allowedMapperTypes = new HashSet<>(configuration.getAllowedProtocolMapperTypes());
        ProtocolMapperModel existingMapper = mapperContext.getExistingProtocolMapper();
        if (existingMapper == null) {
            ProtocolMapperRepresentation rejectedMapper = proposedMappers.stream()
                    .filter(mapper -> !allowedMapperTypes.contains(mapper.getProtocolMapper()))
                    .findFirst().orElse(null);
            if (rejectedMapper != null) {
                throw notAllowed(rejectedMapper.getProtocolMapper());
            }
            return;
        }

        ProtocolMapperRepresentation proposedMapper = mapperContext.getProposedProtocolMapper();
        if (proposedMapper == null || allowedMapperTypes.contains(proposedMapper.getProtocolMapper())) {
            return;
        }

        if (!Objects.equals(proposedMapper.getProtocolMapper(), existingMapper.getProtocolMapper())
                || !Objects.equals(proposedMapper.getConfig(), existingMapper.getConfig())) {
            throw notAllowed(proposedMapper.getProtocolMapper());
        }
    }

    private ClientPolicyException notAllowed(String mapperType) {
        return new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Protocol mapper type '" + mapperType + "' is not allowed by client policy");
    }

    @Override
    public String getProviderId() {
        return AllowedProtocolMappersExecutorFactory.PROVIDER_ID;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {

        @JsonProperty(AllowedProtocolMappersExecutorFactory.ALLOWED_PROTOCOL_MAPPER_TYPES)
        protected List<String> allowedProtocolMapperTypes;

        public List<String> getAllowedProtocolMapperTypes() {
            return allowedProtocolMapperTypes == null ? Collections.emptyList() : allowedProtocolMapperTypes;
        }

        public void setAllowedProtocolMapperTypes(List<String> allowedProtocolMapperTypes) {
            this.allowedProtocolMapperTypes = allowedProtocolMapperTypes;
        }
    }
}
