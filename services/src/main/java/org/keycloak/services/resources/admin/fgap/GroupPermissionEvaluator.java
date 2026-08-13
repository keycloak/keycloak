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
import org.keycloak.models.GroupModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface GroupPermissionEvaluator {

    /**
     * Returns {@code true} if the caller has at least one of {@link AdminRoles#QUERY_GROUPS},
     * {@link AdminRoles#MANAGE_USERS} or {@link AdminRoles#VIEW_USERS} roles.
     * <p/>
     * For V2 only: Also if it has a permission to {@link AdminPermissionsSchema#VIEW} or
     * {@link AdminPermissionsSchema#MANAGE} groups.
     */
    boolean canList();

    /**
     * Throws ForbiddenException if {@link #canList()} returns {@code false}.
     */
    void requireList();

    /**
     * Returns {@code true} if the caller has {@link AdminRoles#MANAGE_USERS} role.
     * <p/>
     * Or if it has a permission to {@link AdminPermissionsSchema#MANAGE} the group.
     */
    boolean canManage(GroupModel group);

    /**
     * Throws ForbiddenException if {@link #canManage(GroupModel)} returns {@code false}.
     */
    void requireManage(GroupModel group);

    /**
     * Returns {@code true} if the caller has one of {@link AdminRoles#MANAGE_USERS} or
     * {@link AdminRoles#VIEW_USERS} roles.
     * <p/>
     * Or if it has a permission to {@link AdminPermissionsSchema#VIEW} the group.
     */
    boolean canView(GroupModel group);

    /**
     * Throws ForbiddenException if {@link #canView(GroupModel)} returns {@code false}.
     */
    void requireView(GroupModel group);

    /**
     * Returns {@code true} if the caller has {@link AdminRoles#MANAGE_USERS} role.
     * <p/>
     * For V2 only: Also if it has permission to {@link AdminPermissionsSchema#MANAGE} groups.
     */
    boolean canManage();

    /**
     * Throws ForbiddenException if {@link #canManage()} returns {@code false}.
     */
    void requireManage();

    /**
     * Returns {@code true} if the caller has one of {@link AdminRoles#MANAGE_USERS} or 
     * {@link AdminRoles#VIEW_USERS} roles.
     * <p/>
     * Or if it has a permission to {@link AdminPermissionsSchema#VIEW} groups.
     */
    boolean canView();

    /**
     * Throws ForbiddenException if {@link #canView()} returns {@code false}.
     */
    void requireView();

    /**
     * Throws ForbiddenException if {@link #canViewMembers(GroupModel)} returns {@code false}.
     */
    void requireViewMembers(GroupModel group);

    /**
     * Returns {@code true} if the caller has {@link AdminRoles#MANAGE_USERS} role.
     * <p/>
     * Or if it has a permission to {@link AdminPermissionsSchema#MANAGE_MEMBERS} of the group.
     */
    boolean canManageMembers(GroupModel group);

    /**
     * Returns {@code true} if the caller has {@link AdminRoles#MANAGE_USERS} role.
     * <p/>
     * Or if it has a permission to {@link AdminPermissionsSchema#MANAGE_MEMBERSHIP} of the group.
     */
    boolean canManageMembership(GroupModel group);

    /**
     * Returns {@code true} if the caller has one of {@link AdminRoles#MANAGE_USERS} or 
     * {@link AdminRoles#VIEW_USERS} roles.
     * <p/>
     * Or if it has a permission to {@link AdminPermissionsSchema#VIEW_MEMBERS} of the group.
     */
    boolean canViewMembers(GroupModel group);

    /**
     * Throws ForbiddenException if {@link #canManageMembership(GroupModel)} returns {@code false}.
     */
    void requireManageMembership(GroupModel group);

    /**
     * Throws ForbiddenException if {@link #canManageMembership(GroupModel)} returns {@code false}.
     */
    void requireManageMembers(GroupModel group);

    /**
     * Returns Map with information what access the caller for the provided group has.
     */
    Map<String, Boolean> getAccess(GroupModel group);

    /**
     * If {@link UserPermissionEvaluator#canView()} evaluates to {@code true}, returns empty set.
     * 
     * @return Stream of IDs of groups with view permission.
     */
    Set<String> getGroupIdsWithViewPermission();
}
