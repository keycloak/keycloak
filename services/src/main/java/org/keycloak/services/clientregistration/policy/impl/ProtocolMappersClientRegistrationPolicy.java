/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientregistration.policy.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.clientregistration.ClientRegistrationContext;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ProtocolMappersClientRegistrationPolicy implements ClientRegistrationPolicy {

    private static final Logger logger = Logger.getLogger(ProtocolMappersClientRegistrationPolicy.class);

    private final KeycloakSession session;
    private final ComponentModel componentModel;

    public ProtocolMappersClientRegistrationPolicy(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }

    @Override
    public void beforeRegister(ClientRegistrationContext context) throws ClientRegistrationPolicyException {
        testMappers(context);
    }

    protected void testMappers(ClientRegistrationContext context) throws ClientRegistrationPolicyException {
        List<ProtocolMapperRepresentation> protocolMappers = context.getClient().getProtocolMappers();
        if (protocolMappers == null) {
            return;
        }

        List<String> allowedMapperProviders = getAllowedMapperProviders();

        for (ProtocolMapperRepresentation mapper : protocolMappers) {
            String mapperType = mapper.getProtocolMapper();

            if (!allowedMapperProviders.contains(mapperType)) {
                ServicesLogger.LOGGER.clientRegistrationMapperNotAllowed(mapper.getName(), mapperType);
                throw new ClientRegistrationPolicyException("ProtocolMapper type not allowed");
            }
        }
    }

    // Remove builtin mappers of unsupported types too
    @Override
    public void afterRegister(ClientRegistrationContext context, ClientModel clientModel) {
        // Remove mappers of unsupported type, which were added "automatically"
        List<String> allowedMapperProviders = getAllowedMapperProviders();
        clientModel.getProtocolMappersStream()
                .filter(mapper -> !allowedMapperProviders.contains(mapper.getProtocolMapper()))
                .peek(mapperToRemove -> logger.debugf("Removing builtin mapper '%s' of type '%s' as type is not permitted",
                        mapperToRemove.getName(), mapperToRemove.getProtocolMapper()))
                .collect(Collectors.toList())
                .forEach(clientModel::removeProtocolMapper);
    }

    // We don't take already existing protocolMappers into consideration for now
    @Override
    public void beforeUpdate(ClientRegistrationContext context, ClientModel clientModel) throws ClientRegistrationPolicyException {
        testMappers(context);
    }

    @Override
    public void afterUpdate(ClientRegistrationContext context, ClientModel clientModel) {
    }

    @Override
    public void beforeView(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {

    }

    @Override
    public void beforeDelete(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {

    }

    private List<String> getAllowedMapperProviders() {
        return componentModel.getConfig().getList(ProtocolMappersClientRegistrationPolicyFactory.ALLOWED_PROTOCOL_MAPPER_TYPES);
    }

}
