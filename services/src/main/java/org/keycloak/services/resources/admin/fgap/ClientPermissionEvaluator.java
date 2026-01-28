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
package org.keycloak.services.resources.admin.fgap;

import java.util.Map;
import java.util.Set;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientPermissionEvaluator {
    boolean isPermissionsEnabled(ClientModel client);

    void setPermissionsEnabled(ClientModel client, boolean enable);

    /**
     * Throws ForbiddenException if {@link #canListClientScopes()} returns {@code false}.
     */
    void requireListClientScopes();

    /**
     * Returns {@code true} if the caller has {@link org.keycloak.models.AdminRoles#MANAGE_CLIENTS} role.
     * <p/>
     * For V2 only: Also if it has permission to {@link AdminPermissionsSchema#MANAGE}.
     */
    boolean canManage();

    /**
     * Throws ForbiddenException if {@link #canManage()} returns {@code false}.
     */
    void requireManage();

    /**
     * Returns {@code true} if the caller has {@link org.keycloak.models.AdminRoles#MANAGE_CLIENTS} role.
     * <p/>
     * For V2 only: Also if it has permission to {@link AdminPermissionsSchema#MANAGE}.
     */
    boolean canManageClientScopes();

    /**
     * Throws ForbiddenException if {@link #canManageClientScopes()} returns {@code false}.
     */
    void requireManageClientScopes();

    /**
     * Returns {@code true} if the caller has at least one of the {@link org.keycloak.models.AdminRoles#MANAGE_CLIENTS} or {@link org.keycloak.models.AdminRoles#VIEW_CLIENTS} roles.
     * <p/>
     * For V2 only: Also if it has permission to {@link AdminPermissionsSchema#VIEW}.
     */
    boolean canView();

    /**
     * Returns {@code true} if {@link #canView()} returns {@code true}.
     * <p/>
     * Or if the caller has at least one of the {@link AdminRoles#QUERY_CLIENTS} role.
     * <p/>
     * V1: or {@link AdminRoles#QUERY_USERS} roles.
     */
    boolean canList();

    /**
     * Returns {@code true} if {@link #canView()} returns {@code true}.
     */
    boolean canViewClientScopes();

    /**
     * Throws ForbiddenException if {@link #canList()} returns {@code false}.
     */
    void requireList();

    /**
     * Returns {@code true} if {@link #canView()} returns {@code true}.
     * <p/>
     * Or if the caller has {@link AdminRoles#QUERY_CLIENTS} role.
     */
    boolean canListClientScopes();

    /**
     * Returns {@code true} if {@link #canView()} returns {@code true}.
     */
    void requireView();

    /**
     * Returns {@code true} if {@link #canViewClientScopes()} returns {@code true}.
     */
    void requireViewClientScopes();

    /**
     * Returns {@code true} if the caller has {@link org.keycloak.models.AdminRoles#MANAGE_CLIENTS} role.
     * <p/>
     * Or if the caller has a permission to {@link AdminPermissionManagement#MANAGE_SCOPE} the client.
     * <p/>
     * For V2 only: Also if the caller has a permission to {@link AdminPermissionsSchema#MANAGE} all clients.
     */
    boolean canManage(ClientModel client);

    /**
     * Returns {@code true} if {@link #canManage(ClientModel)} returns {@code true}.
     * <p/>
     * Or if the caller has a permission to {@link ClientPermissionManagement#CONFIGURE_SCOPE} the client.
     * <p/>
     * For V2 only: the call is redirected to {@code canManage(ClientModel)}.
     */
    boolean canConfigure(ClientModel client);

    /**
     * Throws ForbiddenException if {@link #canConfigure(ClientModel)} returns {@code false}.
     * <p/>
     * For V2 only: the call is redirected to {@code requireManage(ClientModel)}.
     */
    void requireConfigure(ClientModel client);

    /**
     * Throws ForbiddenException if {@link #canManage(ClientModel)}  returns {@code false}.
     */
    void requireManage(ClientModel client);

    /**
     * Returns {@code true} if {@link #canView()} or {@link #canConfigure(ClientModel)} returns {@code true}.
     * <p/>
     * Or if the caller has a permission to {@link AdminPermissionManagement#VIEW_SCOPE} the client.
     * <p/>
     * For V2 only: Also if the caller has a permission to {@link AdminPermissionsSchema#VIEW} all clients.
     */
    boolean canView(ClientModel client);

    /**
     * Throws ForbiddenException if {@link #canView(ClientModel)}  returns {@code false}.
     */
    void requireView(ClientModel client);

    /**
     * Returns {@code true} if the caller has {@link org.keycloak.models.AdminRoles#MANAGE_CLIENTS} role.
     * <p/>
     * For V2 only: Also if it has permission to {@link AdminPermissionsSchema#MANAGE}.
     */
    boolean canManage(ClientScopeModel clientScope);

    /**
     * Throws ForbiddenException if {@link #canManage(ClientScopeModel)} returns {@code false}.
     */
    void requireManage(ClientScopeModel clientScope);

    /**
     * Returns {@code true} if the caller has at least one of the {@link org.keycloak.models.AdminRoles#VIEW_CLIENTS} or {@link org.keycloak.models.AdminRoles#MANAGE_CLIENTS} roles.
     * <p/>
     * For V2 only: Also if it has permission to {@link AdminPermissionsSchema#VIEW}.
     */
    boolean canView(ClientScopeModel clientScope);

    /**
     * Throws ForbiddenException if {@link #canView(ClientScopeModel)} returns {@code false}.
     */
    void requireView(ClientScopeModel clientScope);

    /**
     * Returns {@code true} if the caller has a permission to {@link ClientPermissionManagement#MAP_ROLES_SCOPE} for the client.
     * <p/>
     * For V2 only: Also if the caller has a permission to {@link AdminPermissionsSchema#MAP_ROLES} for all clients.
     */
    boolean canMapRoles(ClientModel client);

    /**
     * Returns {@code true} if the caller has a permission to {@link ClientPermissionManagement#MAP_ROLES_COMPOSITE_SCOPE} for the client.
     * <p/>
     * For V2 only: Also if the caller has a permission to {@link AdminPermissionsSchema#MAP_ROLES_COMPOSITE} for all clients.
     */
    boolean canMapCompositeRoles(ClientModel client);

    /**
     * Returns {@code true} if the caller has a permission to {@link ClientPermissionManagement#MAP_ROLES_CLIENT_SCOPE} for the client.
     * <p/>
     * For V2 only: Also if the caller has a permission to {@link AdminPermissionsSchema#MAP_ROLES_CLIENT_SCOPE} for all clients.
     */
    boolean canMapClientScopeRoles(ClientModel client);

    Map<String, Boolean> getAccess(ClientModel client);

    /**
     * Returns the IDs of the clients that the current user can perform based on {@code scope}.
     *
     * @return Stream of IDs of clients with {@code scope} permission.
     */
    Set<String> getClientIdsByScope(String scope);
}
