/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.fgap.ModelRecord.GroupModelRecord;

class GroupPermissionsV2 extends GroupPermissions {

    private final FineGrainedAdminPermissionEvaluator eval;

    GroupPermissionsV2(KeycloakSession session, AuthorizationProvider authz, MgmtPermissions root) {
        super(authz, root);
        this.eval = new FineGrainedAdminPermissionEvaluator(session, root, resourceStore, policyStore);
    }

    @Override
    public boolean canView() {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS, AdminRoles.VIEW_USERS)) {
            return true;
        }

        return eval.hasPermission(new GroupModelRecord(null), null, AdminPermissionsSchema.VIEW);
    }

    @Override
    public boolean canView(GroupModel group) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS, AdminRoles.VIEW_USERS)) {
            return true;
        }

        return eval.hasPermission(new GroupModelRecord(group), null, AdminPermissionsSchema.VIEW);
    }

    @Override
    public boolean canManage() {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return eval.hasPermission(new GroupModelRecord(null), null, AdminPermissionsSchema.MANAGE);
    }

    @Override
    public boolean canManage(GroupModel group) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return eval.hasPermission(new GroupModelRecord(group), null, AdminPermissionsSchema.MANAGE);
    }

    @Override
    public boolean canViewMembers(GroupModel group) {
        if (root.hasOneAdminRole(AdminRoles.VIEW_USERS, AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return eval.hasPermission(new GroupModelRecord(group), null, AdminPermissionsSchema.VIEW_MEMBERS);
    }

    @Override
    public boolean canManageMembers(GroupModel group) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return eval.hasPermission(new GroupModelRecord(group), null, AdminPermissionsSchema.MANAGE_MEMBERS);
    }

    @Override
    public boolean canManageMembership(GroupModel group) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return eval.hasPermission(new GroupModelRecord(group), null, AdminPermissionsSchema.MANAGE_MEMBERSHIP);
    }

    @Override
    public Set<String> getGroupIdsWithViewPermission() {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public boolean isPermissionsEnabled(GroupModel group) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public void setPermissionsEnabled(GroupModel group, boolean enable) {
       throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy viewMembersPermission(GroupModel group) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy manageMembersPermission(GroupModel group) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy manageMembershipPermission(GroupModel group) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy viewPermission(GroupModel group) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy managePermission(GroupModel group) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Resource resource(GroupModel group) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Map<String, String> getPermissions(GroupModel group) {
        throw new UnsupportedOperationException("Not supported in V2");
    }
}
