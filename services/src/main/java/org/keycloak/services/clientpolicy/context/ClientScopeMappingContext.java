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
package org.keycloak.services.clientpolicy.context;

import java.util.List;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ScopeContainerModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;

/**
 * Context fired by {@link org.keycloak.services.resources.admin.ScopeMappedResource} and
 * {@link org.keycloak.services.resources.admin.ScopeMappedClientResource} on scope-mapping
 * add/remove operations through the admin REST API.
 *
 * <p>Dispatched as {@link org.keycloak.services.clientpolicy.ClientPolicyEvent#REGISTER_SCOPE_MAPPING}
 * and {@link org.keycloak.services.clientpolicy.ClientPolicyEvent#UNREGISTER_SCOPE_MAPPING}.
 */
public interface ClientScopeMappingContext extends ClientPolicyContext {

    /**
     * @return the scope container (client or client scope) receiving or losing the mapping.
     */
    default ScopeContainerModel getScopeContainer() {
        return null;
    }

    /**
     * @return the client whose roles are being mapped; non-null for client-role mappings, null for realm-role.
     */
    default ClientModel getRoleContainerClient() {
        return null;
    }

    /**
     * @return the roles being added or removed.
     */
    default List<RoleRepresentation> getRoles() {
        return null;
    }

    /**
     * @return the authenticated user.
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
     * @return the access token.
     */
    default JsonWebToken getToken() {
        return null;
    }
}
