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

import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RolePermissionEvaluator {

    /**
     * Returns {@code true} if {@link #canView(RoleContainerModel)} returns {@code true}.
     * <p/>
     * Or if the role is a realm role, then it returns {@code true} if {@link RealmPermissionEvaluator#canViewRealm()} returns true
     * or if the caller has at least one of the {@link org.keycloak.models.AdminRoles#QUERY_USERS}, {@link org.keycloak.models.AdminRoles#QUERY_USERS},
     * {@link org.keycloak.models.AdminRoles#QUERY_CLIENTS}, {@link org.keycloak.models.AdminRoles#QUERY_REALMS}, {@link org.keycloak.models.AdminRoles#QUERY_GROUPS} roles.
     * <p/>
     */
    boolean canList(RoleContainerModel container);

    /**
     * Throws ForbiddenException if {@link #canList(RoleContainerModel)} returns {@code false}.
     */
    void requireList(RoleContainerModel container);


    /**
     * Returns {@code true} if the caller has {@link org.keycloak.models.AdminRoles#MANAGE_USERS} role and
     * {@link RolePermissions#checkAdminRoles(RoleModel)} returns {@code true}.
     * <p/>
     * Or if the role is a client role and {@link ClientPermissions#canMapRoles(ClientModel)} returns {@code true}.
     * <p/>
     * Or if the caller has permission to {@link RolePermissionManagement#MAP_ROLE_SCOPE} and {@link RolePermissions#checkAdminRoles(RoleModel)} returns {@code true}.
     * <p/>
     * For V2 only: Also if the caller has a permission to {@link RolePermissionManagement#MAP_ROLE_SCOPE} all roles.
     */
    boolean canMapRole(RoleModel role);

    /**
     * Throws ForbiddenException if {@link #canMapRole(RoleModel)} returns {@code false}.
     */
    void requireMapRole(RoleModel role);

    /**
     * If the role is a realm role, it returns {@code true} if {@link RealmPermissions#canManageRealm()} returns {@code true}.
     * <p/>
     * If the role is a client role, it returns {@code true} if {@link ClientPermissions#canConfigure(ClientModel)} returns {@code true}.
     */
    boolean canManage(RoleModel role);

    /**
     * Throws ForbiddenException if {@link #canManage(RoleModel)} returns {@code false}.
     */
    void requireManage(RoleModel role);

    /**
     * If the role is a realm role, it returns {@code true} if {@link RealmPermissions#canViewRealm()} returns {@code true}.
     * <p/>
     * If the role is a client role, it returns {@code true} if {@link ClientPermissions#canView(ClientModel)} returns {@code true}.
     */
    boolean canView(RoleModel role);

    /**
     * Throws ForbiddenException if {@link #canView(RoleModel)} returns {@code false}.
     */
    void requireView(RoleModel role);

    /**
     * Returns {@code true} if {@link ClientPermissions#canManageClientsDefault()} returns {@code true}.
     * <p/>
     * Or if the role is a client role and {@link ClientPermissions#canMapClientScopeRoles(ClientModel)} returns {@code true}.
     * <p/>
     * Or if the caller has permission to {@link RolePermissionManagement#MAP_ROLE_CLIENT_SCOPE_SCOPE}.
     * <p/>
     * For V2 only: Also if the caller has a permission to {@link RolePermissionManagement#MAP_ROLE_CLIENT_SCOPE_SCOPE} all roles.
     */
    boolean canMapClientScope(RoleModel role);

    /**
     * Throws ForbiddenException if {@link #canMapClientScope(RoleModel)} returns {@code false}.
     */
    void requireMapClientScope(RoleModel role);

    /**
     * Returns {@code true} if {@link RolePermissions#canManageDefault(RoleModel)} and {@link RolePermissions#checkAdminRoles(RoleModel)} returns {@code true}.
     * <p/>
     * Or if the role is a client role and {@link ClientPermissions#canMapCompositeRoles(ClientModel)} returns {@code true}.
     * <p/>
     * Or if the caller has permission to {@link RolePermissionManagement#MAP_ROLE_COMPOSITE_SCOPE} and {@link RolePermissions#checkAdminRoles(RoleModel)} returns {@code true}.
     * <p/>
     * For V2 only: Also if the caller has a permission to {@link RolePermissionManagement#MAP_ROLE_COMPOSITE_SCOPE} all roles.
     */
    boolean canMapComposite(RoleModel role);

    /**
     * Throws ForbiddenException if {@link #canMapComposite(RoleModel)} returns {@code false}.
     */
    void requireMapComposite(RoleModel role);

    /**
     * If the role is a realm role, it returns {@code true} if {@link RealmPermissions#canManageRealm()} returns {@code true}.
     * <p/>
     * If the role is a client role, it returns {@code true} if {@link ClientPermissions#canConfigure(ClientModel)} returns {@code true}.
     */
    boolean canManage(RoleContainerModel container);

    /**
     * Throws ForbiddenException if {@link #canManage(RoleContainerModel)} returns {@code false}.
     */
    void requireManage(RoleContainerModel container);

    /**
     * If the role is a realm role, it returns {@code true} if {@link RealmPermissions#canViewRealm()} returns {@code true}.
     * <p/>
     * If the role is a client role, it returns {@code true} if {@link ClientPermissions#canView(ClientModel)} returns {@code true}.
     */
    boolean canView(RoleContainerModel container);

    /**
     * Throws ForbiddenException if {@link #canView(RoleContainerModel)} returns {@code false}.
     */
    void requireView(RoleContainerModel container);

    /**
     * Returns the IDs of the roles that the current user can perform based on {@code scope}.
     *
     * @return Stream of IDs of roles with {@code scope} permission.
     */
    Set<String> getRoleIdsByScope(String scope);
}
