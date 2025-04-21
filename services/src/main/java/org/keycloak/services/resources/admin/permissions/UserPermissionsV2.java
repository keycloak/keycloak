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
package org.keycloak.services.resources.admin.permissions;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.ForbiddenException;
import org.keycloak.authorization.AdminPermissionsSchema;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.common.DefaultEvaluationContext;
import org.keycloak.authorization.identity.UserModelIdentity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.ResourceWrapper;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ImpersonationConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.authorization.Permission;

class UserPermissionsV2 extends UserPermissions {

    UserPermissionsV2(KeycloakSession session, AuthorizationProvider authz, MgmtPermissionsV2 root) {
        super(session, authz, root);
    }

    @Override
    public boolean canView(UserModel user) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS, AdminRoles.VIEW_USERS)) {
            return true;
        }

        return hasPermission(user, null, AdminPermissionsSchema.VIEW);
    }

    @Override
    public boolean canView() {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS, AdminRoles.VIEW_USERS)) {
            return true;
        }

        return hasPermission((UserModel) null, null, AdminPermissionsSchema.VIEW);
    }

    @Override
    public boolean canManage(UserModel user) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return hasPermission(user, null, AdminPermissionsSchema.MANAGE);
    }

    @Override
    public boolean canManage() {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        if (!root.isAdminSameRealm()) {
            return false;
        }

        return hasPermission((UserModel) null, null, AdminPermissionsSchema.MANAGE);
    }

    @Override
    public void requireManage() {
        if (!canManage()) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canImpersonate(UserModel user, ClientModel requester) {
        if (root.hasOneAdminRole(ImpersonationConstants.IMPERSONATION_ROLE)) {
            return true;
        }

        DefaultEvaluationContext context = requester == null ? null :
                new DefaultEvaluationContext(new UserModelIdentity(root.realm, user), Map.of("kc.client.id", List.of(requester.getClientId())), session);

        return hasPermission(user, context, AdminPermissionsSchema.IMPERSONATE);
    }

    @Override
    public boolean canMapRoles(UserModel user) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return hasPermission(user, null, AdminPermissionsSchema.MAP_ROLES);
    }

    @Override
    public boolean canManageGroupMembership(UserModel user) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return hasPermission(user, null, AdminPermissionsSchema.MANAGE_GROUP_MEMBERSHIP);
    }

    private boolean hasPermission(UserModel user, EvaluationContext context, String scope) {
        if (!root.isAdminSameRealm()) {
            return false;
        }

        ResourceServer server = root.realmResourceServer();

        if (server == null) {
            return false;
        }

        String resourceType = AdminPermissionsSchema.USERS_RESOURCE_TYPE;
        Resource resourceTypeResource = AdminPermissionsSchema.SCHEMA.getResourceTypeResource(session, server, resourceType);
        Resource resource = user == null ? resourceTypeResource : resourceStore.findByName(server, user.getId());

        if (user != null && resource == null) {
            resource = new ResourceWrapper(user.getId(), user.getId(), new HashSet<>(resourceTypeResource.getScopes()), server);
        }

        Collection<Permission> permissions = (context == null) ?
                root.evaluatePermission(new ResourcePermission(resourceType, resource, resource.getScopes(), server), server) :
                root.evaluatePermission(new ResourcePermission(resourceType, resource, resource.getScopes(), server), server, context);

        for (Permission permission : permissions) {
            if (permission.getResourceId().equals(resource.getId())) {
                if (permission.getScopes().contains(scope)) {
                    return true;
                }
            }
        }

        return false;
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
