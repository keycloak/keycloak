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

import org.keycloak.models.ClientModel;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;

/**
 * Context fired by {@link org.keycloak.services.resources.admin.ProtocolMappersResource} on
 * protocol-mapper CRUD operations through the admin REST API.
 *
 * <p>Dispatched as {@link org.keycloak.services.clientpolicy.ClientPolicyEvent#REGISTER_PROTOCOL_MAPPER},
 * {@link org.keycloak.services.clientpolicy.ClientPolicyEvent#UPDATE_PROTOCOL_MAPPER}, and
 * {@link org.keycloak.services.clientpolicy.ClientPolicyEvent#UNREGISTER_PROTOCOL_MAPPER}.
 */
public interface ClientProtocolMapperContext extends ClientPolicyContext {

    /**
     * @return the client or client scope that hosts the mapper.
     */
    default ProtocolMapperContainerModel getProtocolMapperContainer() {
        return null;
    }

    /**
     * @return the inbound mapper representation on single-item REGISTER/UPDATE flows; null on UNREGISTER and
     *         on batch REGISTER flows. Use {@link #getProposedProtocolMappers()} to inspect batch payloads.
     */
    default ProtocolMapperRepresentation getProposedProtocolMapper() {
        return null;
    }

    /**
     * @return immutable inbound mapper representations when the context carries proposed mapper payloads; null on UNREGISTER.
     */
    default List<ProtocolMapperRepresentation> getProposedProtocolMappers() {
        ProtocolMapperRepresentation proposed = getProposedProtocolMapper();
        return proposed == null ? null : List.of(proposed);
    }

    /**
     * @return the existing mapper model on UPDATE/UNREGISTER; null on REGISTER.
     */
    default ProtocolMapperModel getExistingProtocolMapper() {
        return null;
    }

    /**
     * @return the authenticated user performing this operation.
     */
    default UserModel getAuthenticatedUser() {
        return null;
    }

    /**
     * @return the authenticated client.
     */
    default ClientModel getAuthenticatedClient() {
        return null;
    }

    /**
     * @return the access token accompanying the request.
     */
    default JsonWebToken getToken() {
        return null;
    }
}
