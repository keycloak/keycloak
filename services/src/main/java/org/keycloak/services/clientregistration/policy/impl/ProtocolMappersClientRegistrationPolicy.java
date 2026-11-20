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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.clientregistration.ClientRegistrationContext;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;

import org.jboss.logging.Logger;

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
        List<ProtocolMapperRepresentation> protocolMappers = context.getClient().getProtocolMappers();
        if (protocolMappers == null) {
            return;
        }

        // On create every mapper is new — compute changed types against an empty existing set.
        Set<String> changedTypes = ProtocolMapperUtils.getChangedOrDeletedMapperTypes(Stream.empty(), protocolMappers);
        checkMapperTypesAllowed(changedTypes);
    }

    @Override
    public void beforeUpdate(ClientRegistrationContext context, ClientModel clientModel) throws ClientRegistrationPolicyException {
        List<ProtocolMapperRepresentation> protocolMappers = context.getClient().getProtocolMappers();
        if (protocolMappers == null) {
            return;
        }

        // On update only check mapper types that are actually new, changed, or deleted — identical
        // pre-existing mappers are allowed through, consistent with the admin REST ClientResource.update path.
        Set<String> changedTypes = ProtocolMapperUtils.getChangedOrDeletedMapperTypes(
                clientModel.getProtocolMappersStream(), protocolMappers);
        checkMapperTypesAllowed(changedTypes);
    }

    private void checkMapperTypesAllowed(Set<String> mapperTypes) throws ClientRegistrationPolicyException {
        if (mapperTypes.isEmpty()) {
            return;
        }
        Set<String> allowedMapperProviders = getAllowedMapperProviders();
        for (String mapperType : mapperTypes) {
            if (!allowedMapperProviders.contains(mapperType)) {
                ServicesLogger.LOGGER.clientRegistrationMapperNotAllowed(mapperType, mapperType);
                throw new ClientRegistrationPolicyException("ProtocolMapper type not allowed");
            }
        }
    }

	   // Remove builtin mappers of unsupported types too
	   @Override
	   public void afterRegister(ClientRegistrationContext context, ClientModel clientModel) {
        // Remove mappers of unsupported type, which were added "automatically"
        Set<String> allowedMapperProviders = getAllowedMapperProviders();
        clientModel.getProtocolMappersStream()
                .filter(mapper -> !allowedMapperProviders.contains(mapper.getProtocolMapper()))
                .peek(mapperToRemove -> logger.debugf("Removing builtin mapper '%s' of type '%s' as type is not permitted",
                        mapperToRemove.getName(), mapperToRemove.getProtocolMapper()))
                .toList()
                .forEach(clientModel::removeProtocolMapper);
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

    private Set<String> getAllowedMapperProviders() {
        RealmModel realm = session.getContext().getRealm();
        String allowedForAdminsAttr = realm != null ? realm.getAttribute(Constants.ALLOWED_PROTOCOL_MAPPER_TYPES_FOR_CLIENT_ADMINISTRATORS) : null;
        if (allowedForAdminsAttr == null || allowedForAdminsAttr.isBlank()) {
            return Collections.emptySet();
        }

        Set<String> allowedForAdmins = Arrays.stream(allowedForAdminsAttr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        // Allowed mappers are intersection of the policy configuration and realm attribute. Mapper must be included in both places to be effectively allowed
        Set<String> allowedInConfig = new HashSet<>(componentModel.getConfig().getOrDefault(ProtocolMappersClientRegistrationPolicyFactory.ALLOWED_PROTOCOL_MAPPER_TYPES, Collections.emptyList()));
        allowedInConfig.retainAll(allowedForAdmins);
        return allowedInConfig;
    }

}
