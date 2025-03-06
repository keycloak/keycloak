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

import org.keycloak.authorization.AdminPermissionsSchema;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.authorization.Permission;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RolePermissionsV2 extends RolePermissions {

    public RolePermissionsV2(KeycloakSession session, RealmModel realm, AuthorizationProvider authz, MgmtPermissions root) {
        super(session, realm, authz, root);
    }

    @Override
    public boolean canMapClientScope(RoleModel role) {
        if (root.clients().canManageClientsDefault()) return true;

        if (role.getContainer() instanceof ClientModel) {
            if (root.clients().canMapClientScopeRoles((ClientModel) role.getContainer())) return true;
        }

        return hasPermission(role, MAP_ROLE_CLIENT_SCOPE_SCOPE);
    }

    @Override
    public boolean canMapComposite(RoleModel role) {
        if (canManageDefault(role)) return checkAdminRoles(role);

        if (role.getContainer() instanceof ClientModel) {
            if (root.clients().canMapCompositeRoles((ClientModel) role.getContainer())) return true;
        }

        return hasPermission(role, MAP_ROLE_COMPOSITE_SCOPE) && checkAdminRoles(role);
    }

    @Override
    public boolean canMapRole(RoleModel role) {
        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) return checkAdminRoles(role);

        if (role.getContainer() instanceof ClientModel) {
            if (root.clients().canMapRoles((ClientModel) role.getContainer())) return true;
        }

        return hasPermission(role, MAP_ROLE_SCOPE) && checkAdminRoles(role);
    }

    @Override
    public Set<String> getRolesWithPermission(String scope) {
        if (!root.isAdminSameRealm()) {
            return Collections.emptySet();
        }

        ResourceServer server = root.realmResourceServer();

        if (server == null) {
            return Collections.emptySet();
        }

        Set<String> granted = new HashSet<>();

        resourceStore.findByType(server, AdminPermissionsSchema.ROLES_RESOURCE_TYPE, resource -> {
            if (hasGrantedPermission(server, resource, scope)) {
                granted.add(resource.getName());
            }
        });

        return granted;
    }

    private boolean hasPermission(RoleModel role, String scope) {
        if (!root.isAdminSameRealm()) {
            return false;
        }
        ResourceServer server = root.realmResourceServer();
        if (server == null) return false;

        Resource resource =  resourceStore.findByName(server, role.getId(), server.getId());
        if (resource == null) {
            // check if there is permission for "all-roles". If so, load its resource and proceed with evaluation
            resource = AdminPermissionsSchema.SCHEMA.getResourceTypeResource(session, server, AdminPermissionsSchema.ROLES_RESOURCE_TYPE);

            if (authz.getStoreFactory().getPolicyStore().findByResource(server, resource).isEmpty()) {
                return false;
            }
        }

        return hasGrantedPermission(server, resource, scope);
    }

    private boolean hasGrantedPermission(ResourceServer server, Resource resource, String scope) {
        Collection<Permission> permissions = root.evaluatePermission(new ResourcePermission(resource, resource.getScopes(), server), server);
        for (Permission permission : permissions) {
            for (String s : permission.getScopes()) {
                if (scope.equals(s)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isPermissionsEnabled(RoleModel role) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public void setPermissionsEnabled(RoleModel role, boolean enable) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Map<String, String> getPermissions(RoleModel role) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy mapRolePermission(RoleModel role) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy mapCompositePermission(RoleModel role) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy mapClientScopePermission(RoleModel role) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Resource resource(RoleModel role) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public ResourceServer resourceServer(RoleModel role) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy manageUsersPolicy(ResourceServer server) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy viewUsersPolicy(ResourceServer server) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy rolePolicy(ResourceServer server, RoleModel role) {
        throw new UnsupportedOperationException("Not supported in V2");
    }
}
