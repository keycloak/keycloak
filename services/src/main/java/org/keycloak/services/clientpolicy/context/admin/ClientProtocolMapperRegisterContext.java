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
package org.keycloak.services.clientpolicy.context.admin;

import java.util.List;

import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.resources.admin.AdminAuth;

/**
 * Fired when protocol mappers are being created on a client or client scope through the admin REST API.
 *
 * <p>Emitted from {@link org.keycloak.services.resources.admin.ProtocolMappersResource#createMapper(java.util.List)}
 * before the mapper is persisted. An executor may inspect {@link #getProposedProtocolMappers()} and throw
 * {@link org.keycloak.services.clientpolicy.ClientPolicyException} to reject the request.
 *
 * @see ClientPolicyEvent#REGISTER_PROTOCOL_MAPPER
 */
public class ClientProtocolMapperRegisterContext extends AbstractClientProtocolMapperContext {

    private final ProtocolMapperContainerModel protocolMapperContainer;
    private final List<ProtocolMapperRepresentation> proposed;

    public ClientProtocolMapperRegisterContext(ProtocolMapperContainerModel protocolMapperContainer,
                                               ProtocolMapperRepresentation proposed,
                                               AdminAuth adminAuth) {
        this(protocolMapperContainer, proposed == null ? null : List.of(proposed), adminAuth);
    }

    public ClientProtocolMapperRegisterContext(ProtocolMapperContainerModel protocolMapperContainer,
                                               List<ProtocolMapperRepresentation> proposed,
                                               AdminAuth adminAuth) {
        super(adminAuth);
        this.protocolMapperContainer = protocolMapperContainer;
        this.proposed = proposed == null ? null : List.copyOf(proposed);
    }

    /** @return {@link ClientPolicyEvent#REGISTER_PROTOCOL_MAPPER} */
    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.REGISTER_PROTOCOL_MAPPER;
    }

    @Override
    public ProtocolMapperContainerModel getProtocolMapperContainer() {
        return protocolMapperContainer;
    }

    @Override
    public ProtocolMapperRepresentation getProposedProtocolMapper() {
        return proposed == null || proposed.size() != 1 ? null : proposed.get(0);
    }

    @Override
    public List<ProtocolMapperRepresentation> getProposedProtocolMappers() {
        return proposed;
    }
}
