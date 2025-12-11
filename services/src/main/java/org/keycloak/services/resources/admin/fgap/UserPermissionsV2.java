/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.ForbiddenException;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.common.DefaultEvaluationContext;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.identity.UserModelIdentity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.fgap.ModelRecord.UserModelRecord;

class UserPermissionsV2 extends UserPermissions {

    private final FineGrainedAdminPermissionEvaluator eval;

    UserPermissionsV2(KeycloakSession session, AuthorizationProvider authz, MgmtPermissionsV2 root) {
        super(session, authz, root);
        this.eval = new FineGrainedAdminPermissionEvaluator(session, root, resourceStore, policyStore);
    }

    @Override
    public boolean canQuery() {
        return root.hasOneAdminRole(AdminRoles.QUERY_USERS) || canView();
    }

    @Override
    public void requireQuery() {
        if (!canQuery()) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canView(UserModel user) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS, AdminRoles.VIEW_USERS)) {
            return true;
        }

        return eval.hasPermission(new UserModelRecord(user), null, AdminPermissionsSchema.VIEW);
    }

    @Override
    public boolean canView() {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS, AdminRoles.VIEW_USERS)) {
            return true;
        }

        return eval.hasPermission(new UserModelRecord(null), null, AdminPermissionsSchema.VIEW);
    }

    @Override
    public void requireView(UserModel user) {
        if (!canView(user)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public void requireView() {
        if (!(canView())) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canManage(UserModel user) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return eval.hasPermission(new UserModelRecord(user), null, AdminPermissionsSchema.MANAGE);
    }

    @Override
    public boolean canManage() {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return eval.hasPermission(new UserModelRecord(null), null, AdminPermissionsSchema.MANAGE);
    }

    @Override
    public void requireManage(UserModel user) {
        if (!canManage(user)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public void requireManage() {
        if (!canManage()) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canImpersonate(UserModel user, ClientModel requester) {
        if (root.hasOneAdminRole(AdminRoles.IMPERSONATION)) {
            return true;
        }

        DefaultEvaluationContext context = requester == null ? null :
                new DefaultEvaluationContext(new UserModelIdentity(root.realm, user), Map.of("kc.client.id", List.of(requester.getClientId())), session);

        return eval.hasPermission(new UserModelRecord(user), context, AdminPermissionsSchema.IMPERSONATE);
    }

    @Override
    public boolean canMapRoles(UserModel user) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return eval.hasPermission(new UserModelRecord(user), null, AdminPermissionsSchema.MAP_ROLES);
    }

    @Override
    public boolean canManageGroupMembership(UserModel user) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return eval.hasPermission(new UserModelRecord(user), null, AdminPermissionsSchema.MANAGE_GROUP_MEMBERSHIP);
    }

    @Override
    public boolean canResetPassword(UserModel user) {
        // admin roles has the precedence over permissions
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return eval.hasPermission(new UserModelRecord(user), null, AdminPermissionsSchema.RESET_PASSWORD,
                () -> eval.hasPermission(new UserModelRecord(user), null, AdminPermissionsSchema.MANAGE));
    }

    @Override
    public void requireResetPassword(UserModel user) {
        if (!canResetPassword(user)) {
            throw new ForbiddenException();
        }
    }

    // todo this method should be removed and replaced by canImpersonate(user, client); once V1 is removed
    @Override
    public boolean canClientImpersonate(ClientModel client, UserModel user) {
        return canImpersonate(user, client);
    }

    @Override
    public boolean isImpersonatable(UserModel user, ClientModel requester) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public boolean isImpersonatable(UserModel user) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public boolean isPermissionsEnabled() {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public void setPermissionsEnabled(boolean enable) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Map<String, String> getPermissions() {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Resource resource() {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy managePermission() {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy viewPermission() {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy manageGroupMembershipPermission() {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy mapRolesPermission() {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy adminImpersonatingPermission() {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy userImpersonatedPermission() {
        throw new UnsupportedOperationException("Not supported in V2");
    }
}
