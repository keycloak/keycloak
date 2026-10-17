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
package org.keycloak.services.resources.admin.fgap;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.OrganizationModel;

public interface OrganizationPermissionEvaluator {

    boolean canManage();

    boolean canManage(OrganizationModel organization);

    void requireManage();

    void requireManage(OrganizationModel organization);

    boolean canView();

    boolean canView(OrganizationModel organization);

    void requireView();

    void requireView(OrganizationModel organization);

    boolean canQuery();

    void requireQuery();

    /**
     * Returns {@code true} if the caller has one of {@link AdminRoles#MANAGE_USERS} or
     * {@link AdminRoles#VIEW_USERS} roles.
     * <p/>
     * Or if it has a permission to {@link AdminPermissionsSchema#VIEW_MEMBERS} of the organization.
     */
    boolean canViewMembers(OrganizationModel organization);

    /**
     * Throws ForbiddenException if {@link #canViewMembers(OrganizationModel)} returns {@code false}.
     */
    void requireViewMembers(OrganizationModel organization);

    /**
     * Returns {@code true} if the caller has {@link AdminRoles#MANAGE_USERS} role.
     * <p/>
     * Or if it has a permission to {@link AdminPermissionsSchema#MANAGE_MEMBERS} of the organization.
     */
    boolean canManageMembers(OrganizationModel organization);

    /**
     * Throws ForbiddenException if {@link #canManageMembers(OrganizationModel)} returns {@code false}.
     */
    void requireManageMembers(OrganizationModel organization);
}
