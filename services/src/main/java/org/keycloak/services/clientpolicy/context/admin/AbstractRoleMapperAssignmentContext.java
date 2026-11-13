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
import org.keycloak.models.RoleMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.resources.admin.AdminAuth;

/**
 * Internal scaffolding for AdminAuth wiring shared by all role-mapping context classes.
 *
 * @see RoleMapperAssignmentContext
 * @see RoleMapperAssignmentRegisterContext
 * @see RoleMapperAssignmentRemoveContext
 */
abstract class AbstractRoleMapperAssignmentContext implements RoleMapperAssignmentContext {

    protected final RoleMapperModel roleMapper;
    protected final ClientModel roleContainerClient;
    protected final List<RoleRepresentation> roles;
    protected final AdminAuth adminAuth;

    AbstractRoleMapperAssignmentContext(RoleMapperModel roleMapper,
                                        ClientModel roleContainerClient,
                                        List<RoleRepresentation> roles,
                                        AdminAuth adminAuth) {
        this.roleMapper = roleMapper;
        this.roleContainerClient = roleContainerClient;
        this.roles = roles == null ? null : List.copyOf(roles);
        this.adminAuth = adminAuth;
    }

    @Override
    public RoleMapperModel getRoleMapper() {
        return roleMapper;
    }

    @Override
    public ClientModel getRoleContainerClient() {
        return roleContainerClient;
    }

    @Override
    public List<RoleRepresentation> getRoles() {
        return roles;
    }

    @Override
    public ClientModel getAuthenticatedClient() {
        return adminAuth.getClient();
    }

    @Override
    public UserModel getAuthenticatedUser() {
        return adminAuth.getUser();
    }

    @Override
    public JsonWebToken getToken() {
        return adminAuth.getToken();
    }
}
