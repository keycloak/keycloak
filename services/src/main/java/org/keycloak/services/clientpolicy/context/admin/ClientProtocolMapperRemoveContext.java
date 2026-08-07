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

import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.resources.admin.AdminAuth;

/**
 * Fired when a protocol mapper is being removed from a client or client scope through the admin REST API.
 *
 * <p>Emitted from {@link org.keycloak.services.resources.admin.ProtocolMappersResource#delete}
 * before the mapper is removed.
 *
 * @see ClientPolicyEvent#UNREGISTER_PROTOCOL_MAPPER
 */
public class ClientProtocolMapperRemoveContext extends AbstractClientProtocolMapperContext {

    private final ProtocolMapperContainerModel protocolMapperContainer;
    private final ProtocolMapperModel existing;

    public ClientProtocolMapperRemoveContext(ProtocolMapperContainerModel protocolMapperContainer,
                                             ProtocolMapperModel existing,
                                             AdminAuth adminAuth) {
        super(adminAuth);
        this.protocolMapperContainer = protocolMapperContainer;
        this.existing = existing;
    }

    /** @return {@link ClientPolicyEvent#UNREGISTER_PROTOCOL_MAPPER} */
    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.UNREGISTER_PROTOCOL_MAPPER;
    }

    @Override
    public ProtocolMapperContainerModel getProtocolMapperContainer() {
        return protocolMapperContainer;
    }

    @Override
    public ProtocolMapperModel getExistingProtocolMapper() {
        return existing;
    }
}
