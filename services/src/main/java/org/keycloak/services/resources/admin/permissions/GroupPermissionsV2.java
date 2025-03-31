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
package org.keycloak.services.resources.admin.permissions;

import static org.keycloak.authorization.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.keycloak.authorization.AdminPermissionsSchema;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.ResourceWrapper;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.models.AdminRoles;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;

class GroupPermissionsV2 extends GroupPermissions {

    private final KeycloakSession session;

    GroupPermissionsV2(KeycloakSession session, AuthorizationProvider authz, MgmtPermissions root) {
        super(authz, root);
        this.session = session;
    }

    @Override
    public boolean canView() {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS, AdminRoles.VIEW_USERS)) {
            return true;
        }

        return hasPermission(null, AdminPermissionsSchema.VIEW);
    }

    @Override
    public boolean canView(GroupModel group) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS, AdminRoles.VIEW_USERS)) {
            return true;
        }

        return hasPermission(group.getId(), AdminPermissionsSchema.VIEW);
    }

    @Override
    public boolean canManage() {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return hasPermission(null, AdminPermissionsSchema.MANAGE);
    }

    @Override
    public boolean canManage(GroupModel group) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return hasPermission(group.getId(), AdminPermissionsSchema.MANAGE);
    }

    @Override
    public boolean canViewMembers(GroupModel group) {
        if (root.hasOneAdminRole(AdminRoles.VIEW_USERS, AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return hasPermission(group.getId(), AdminPermissionsSchema.VIEW_MEMBERS);
    }

    @Override
    public boolean canManageMembers(GroupModel group) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return hasPermission(group.getId(), AdminPermissionsSchema.MANAGE_MEMBERS);
    }

    @Override
    public boolean canManageMembership(GroupModel group) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        return hasPermission(group.getId(), AdminPermissionsSchema.MANAGE_MEMBERSHIP);
    }

    @Override
    public Set<String> getGroupIdsWithViewPermission() {
        if (root.hasOneAdminRole(AdminRoles.VIEW_USERS, AdminRoles.MANAGE_USERS)) {
            return Collections.emptySet();
        }

        if (!root.isAdminSameRealm()) {
            return Collections.emptySet();
        }

        ResourceServer server = root.realmResourceServer();

        if (server == null) {
            return Collections.emptySet();
        }

        Set<String> granted = new HashSet<>();

        policyStore.findByResourceType(server, GROUPS_RESOURCE_TYPE).stream()
                .flatMap((Function<Policy, Stream<Resource>>) policy -> policy.getResources().stream())
                .forEach(gr -> {
            if (hasPermission(gr.getName(), AdminPermissionsSchema.VIEW_MEMBERS, AdminPermissionsSchema.MANAGE_MEMBERS)) {
                granted.add(gr.getName());
            }
        });

        return granted;
    }

    private boolean hasPermission(String groupId, String... scopes) {
        return hasPermission(groupId, null, scopes);
    }

    private boolean hasPermission(String groupId, EvaluationContext context, String... scopes) {
        if (!root.isAdminSameRealm()) {
            return false;
        }

        ResourceServer server = root.realmResourceServer();

        if (server == null) {
            return false;
        }

        String resourceType = GROUPS_RESOURCE_TYPE;
        Resource resourceTypeResource = AdminPermissionsSchema.SCHEMA.getResourceTypeResource(session, server, resourceType);
        Resource resource = groupId == null ? resourceTypeResource : resourceStore.findByName(server, groupId);

        if (groupId != null && resource == null) {
            resource = new ResourceWrapper(groupId, groupId, new HashSet<>(resourceTypeResource.getScopes()), server);
        }

        Collection<Permission> permissions = (context == null) ?
                root.evaluatePermission(new ResourcePermission(resourceType, resource, resource.getScopes(), server), server) :
                root.evaluatePermission(new ResourcePermission(resourceType, resource, resource.getScopes(), server), server, context);

        List<String> expectedScopes = List.of(scopes);

        for (Permission permission : permissions) {
            if (permission.getResourceId().equals(resource.getId())) {
                for (String scope : permission.getScopes()) {
                    if (expectedScopes.contains(scope)) {
                        return true;
                    }
                }
            }
        }

        return false;
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
