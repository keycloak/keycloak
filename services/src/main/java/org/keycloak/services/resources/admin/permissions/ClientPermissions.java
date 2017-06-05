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
package org.keycloak.services.resources.admin.permissions;

import org.jboss.logging.Logger;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.services.ForbiddenException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages default policies for all users.
 *
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
class ClientPermissions implements ClientPermissionEvaluator, ClientPermissionManagement {
    private static final Logger logger = Logger.getLogger(ClientPermissions.class);
    protected final KeycloakSession session;
    protected final RealmModel realm;
    protected final AuthorizationProvider authz;
    protected final MgmtPermissions root;

    public ClientPermissions(KeycloakSession session, RealmModel realm, AuthorizationProvider authz, MgmtPermissions root) {
        this.session = session;
        this.realm = realm;
        this.authz = authz;
        this.root = root;
    }

    private String getResourceName(ClientModel client) {
        return "client.resource." + client.getId();
    }

    private String getManagePermissionName(ClientModel client) {
        return "manage.permission.client." + client.getId();
    }
    private String getViewPermissionName(ClientModel client) {
        return "view.permission.client." + client.getId();
    }
    private String getMapRolesPermissionName(ClientModel client) {
        return MAP_ROLES_SCOPE + ".permission.client." + client.getId();
    }
    private String getMapRolesClientScopePermissionName(ClientModel client) {
        return MAP_ROLES_CLIENT_SCOPE + ".permission.client." + client.getId();
    }
    private String getMapRolesCompositePermissionName(ClientModel client) {
        return MAP_ROLES_COMPOSITE_SCOPE + ".permission.client." + client.getId();
    }

    private void initialize(ClientModel client) {
        ResourceServer server = root.findOrCreateResourceServer(client);
        Scope manageScope = manageScope(server);
        if (manageScope == null) {
            manageScope = authz.getStoreFactory().getScopeStore().create(AdminPermissionManagement.MANAGE_SCOPE, server);
        }
        Scope viewScope = viewScope(server);
        if (viewScope == null) {
            viewScope = authz.getStoreFactory().getScopeStore().create(AdminPermissionManagement.VIEW_SCOPE, server);
        }
        Scope mapRoleScope = mapRolesScope(server);
        if (mapRoleScope == null) {
            mapRoleScope = authz.getStoreFactory().getScopeStore().create(MAP_ROLES_SCOPE, server);
        }
        Scope mapRoleClientScope = authz.getStoreFactory().getScopeStore().findByName(MAP_ROLES_CLIENT_SCOPE, server.getId());
        if (mapRoleClientScope == null) {
            mapRoleClientScope = authz.getStoreFactory().getScopeStore().create(MAP_ROLES_CLIENT_SCOPE, server);
        }
        Scope mapRoleCompositeScope = authz.getStoreFactory().getScopeStore().findByName(MAP_ROLES_COMPOSITE_SCOPE, server.getId());
        if (mapRoleCompositeScope == null) {
            mapRoleCompositeScope = authz.getStoreFactory().getScopeStore().create(MAP_ROLES_COMPOSITE_SCOPE, server);
        }

        String resourceName = getResourceName(client);
        Resource resource = authz.getStoreFactory().getResourceStore().findByName(resourceName, server.getId());
        if (resource == null) {
            resource = authz.getStoreFactory().getResourceStore().create(resourceName, server, server.getClientId());
            resource.setType("Client");
            Set<Scope> scopeset = new HashSet<>();
            scopeset.add(manageScope);
            scopeset.add(viewScope);
            scopeset.add(mapRoleScope);
            scopeset.add(mapRoleClientScope);
            scopeset.add(mapRoleCompositeScope);
            resource.updateScopes(scopeset);
        }
        String managePermissionName = getManagePermissionName(client);
        Policy managePermission = authz.getStoreFactory().getPolicyStore().findByName(managePermissionName, server.getId());
        if (managePermission == null) {
            RoleModel role = root.getRealmManagementClient().getRole(AdminRoles.MANAGE_CLIENTS);
            Policy manageClientsPolicy = root.roles().rolePolicy(server, role);
            Helper.addScopePermission(authz, server, managePermissionName, resource, manageScope, manageClientsPolicy);
        }
        String viewPermissionName = getViewPermissionName(client);
        Policy viewPermission = authz.getStoreFactory().getPolicyStore().findByName(viewPermissionName, server.getId());
        if (viewPermission == null) {
            RoleModel role = root.getRealmManagementClient().getRole(AdminRoles.VIEW_CLIENTS);
            Policy viewClientsPolicy = root.roles().rolePolicy(server, role);
            Helper.addScopePermission(authz, server, viewPermissionName, resource, viewScope, viewClientsPolicy);
        }
        String mapRolePermissionName = getMapRolesPermissionName(client);
        Policy mapRolePermission = authz.getStoreFactory().getPolicyStore().findByName(mapRolePermissionName, server.getId());
        if (mapRolePermission == null) {
            Helper.addEmptyScopePermission(authz, server, mapRolePermissionName, resource, mapRoleScope);
        }
        String mapRoleClientScopePermissionName = getMapRolesClientScopePermissionName(client);
        Policy mapRoleClientScopePermission = authz.getStoreFactory().getPolicyStore().findByName(mapRoleClientScopePermissionName, server.getId());
        if (mapRoleClientScopePermission == null) {
            Helper.addEmptyScopePermission(authz, server, mapRoleClientScopePermissionName, resource, mapRoleClientScope);
        }
        String mapRoleCompositePermissionName = getMapRolesCompositePermissionName(client);
        Policy mapRoleCompositePermission = authz.getStoreFactory().getPolicyStore().findByName(mapRoleCompositePermissionName, server.getId());
        if (mapRoleCompositePermission == null) {
            Helper.addEmptyScopePermission(authz, server, mapRoleCompositePermissionName, resource, mapRoleCompositeScope);
        }
    }

    private void deletePolicy(String name, ClientModel client, ResourceServer server) {
        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(getViewPermissionName(client), server.getId());
        if (policy != null) {
            authz.getStoreFactory().getPolicyStore().delete(policy.getId());
        }

    }

    private void deletePermissions(ClientModel client) {
        ResourceServer server = resourceServer(client);
        if (server == null) return;
        deletePolicy(getManagePermissionName(client), client, server);
        deletePolicy(getViewPermissionName(client), client, server);
        deletePolicy(getMapRolesPermissionName(client), client, server);
        deletePolicy(getMapRolesClientScopePermissionName(client), client, server);
        deletePolicy(getMapRolesCompositePermissionName(client), client, server);
        Resource resource = authz.getStoreFactory().getResourceStore().findByName(getResourceName(client), server.getId());;
        if (resource != null) authz.getStoreFactory().getResourceStore().delete(resource.getId());
    }

    @Override
    public boolean isPermissionsEnabled(ClientModel client) {
        ResourceServer server = resourceServer(client);
        if (server == null) return false;

        return authz.getStoreFactory().getResourceStore().findByName(getResourceName(client), server.getId()) != null;
    }

    @Override
    public void setPermissionsEnabled(ClientModel client, boolean enable) {
        if (enable) {
            initialize(client);
        } else {
            deletePermissions(client);
        }
    }



    private Scope manageScope(ResourceServer server) {
        return authz.getStoreFactory().getScopeStore().findByName(AdminPermissionManagement.MANAGE_SCOPE, server.getId());
    }

    private Scope viewScope(ResourceServer server) {
        return authz.getStoreFactory().getScopeStore().findByName(AdminPermissionManagement.VIEW_SCOPE, server.getId());
    }
    private Scope mapRolesScope(ResourceServer server) {
        return authz.getStoreFactory().getScopeStore().findByName(MAP_ROLES_SCOPE, server.getId());
    }

    @Override
    public boolean canList() {
        return root.hasAnyAdminRole();
    }

    @Override
    public void requireList() {
        if (!canList()) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canListTemplates() {
        return root.hasAnyAdminRole();
    }

    @Override
    public void requireListTemplates() {
        if (!canListTemplates()) {
            throw new ForbiddenException();
        }
    }
    public boolean canManageClientDefault() {
        return root.hasOneAdminRole(AdminRoles.MANAGE_CLIENTS);
    }
    public boolean canViewClientDefault() {
        return root.hasOneAdminRole(AdminRoles.MANAGE_CLIENTS, AdminRoles.VIEW_CLIENTS);
    }

    @Override
    public boolean canManage() {
        return canManageClientDefault();
    }

    @Override
    public void requireManage() {
        if (!canManage()) {
            throw new ForbiddenException();
        }
    }
    @Override
    public boolean canView() {
        return canManageClientDefault() || canViewClientDefault();
    }

    @Override
    public void requireView() {
        if (!canView()) {
            throw new ForbiddenException();
        }
    }

    @Override
    public Resource resource(ClientModel client) {
        ResourceServer server = resourceServer(client);
        if (server == null) return null;
        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(getResourceName(client), server.getId());
        if (resource == null) return null;
        return resource;
    }

    @Override
    public Map<String, String> getPermissions(ClientModel client) {
        Map<String, String> scopes = new HashMap<>();
        scopes.put(MAP_ROLES_SCOPE,  mapRolesPermission(client).getId());
        scopes.put(MAP_ROLES_CLIENT_SCOPE, mapRolesClientScopePermission(client).getId());
        scopes.put(MAP_ROLES_COMPOSITE_SCOPE, mapRolesCompositePermission(client).getId());
        scopes.put(AdminPermissionManagement.VIEW_SCOPE, viewPermission(client).getId());
        scopes.put(AdminPermissionManagement.MANAGE_SCOPE, managePermission(client).getId());
        return scopes;
    }


    @Override
    public boolean canManage(ClientModel client) {
        if (!root.isAdminSameRealm()) {
            return canManage();
        }

        ResourceServer server = resourceServer(client);
        if (server == null) return canManage();

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(getResourceName(client), server.getId());
        if (resource == null) return canManage();

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(getManagePermissionName(client), server.getId());
        if (policy == null) {
            return canManage();
        }

        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        // if no policies attached to permission then just do default behavior
        if (associatedPolicies == null || associatedPolicies.isEmpty()) {
            return canManage();
        }

        Scope scope = manageScope(server);
        return root.evaluatePermission(resource, scope, server);
    }

    @Override
    public void requireManage(ClientModel client) {
        if (!canManage(client)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canView(ClientModel client) {
        if (!root.isAdminSameRealm()) {
            return canView();
        }

        ResourceServer server = resourceServer(client);
        if (server == null) return canView();

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(getResourceName(client), server.getId());
        if (resource == null) return canView();

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(getViewPermissionName(client), server.getId());
        if (policy == null) {
            return canView();
        }

        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        // if no policies attached to permission then just do default behavior
        if (associatedPolicies == null || associatedPolicies.isEmpty()) {
            return canView();
        }

        Scope scope = viewScope(server);
        return root.evaluatePermission(resource, scope, server);
    }

    @Override
    public void requireView(ClientModel client) {
        if (!canView(client)) {
            throw new ForbiddenException();
        }
    }

    // templates

    @Override
    public boolean canViewTemplates() {
        return canView();
    }

    @Override
    public boolean canManageTemplates() {
        return canManageClientDefault();
    }

    @Override
    public void requireManageTemplates() {
        if (!canManageTemplates()) {
            throw new ForbiddenException();
        }
    }
    @Override
    public void requireViewTemplates() {
        if (!canViewTemplates()) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canManage(ClientTemplateModel template) {
        return canManageClientDefault();
    }

    @Override
    public void requireManage(ClientTemplateModel template) {
        if (!canManage(template)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canView(ClientTemplateModel template) {
        return canViewClientDefault();
    }

    @Override
    public void requireView(ClientTemplateModel template) {
        if (!canView(template)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canMapRoles(ClientModel client) {
        ResourceServer server = resourceServer(client);
        if (server == null) return false;

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(getResourceName(client), server.getId());
        if (resource == null) return false;

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(getMapRolesPermissionName(client), server.getId());
        if (policy == null) {
            return false;
        }

        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        // if no policies attached to permission then just do default behavior
        if (associatedPolicies == null || associatedPolicies.isEmpty()) {
            return false;
        }

        Scope scope = mapRolesScope(server);
        return root.evaluatePermission(resource, scope, server);
    }

    @Override
    public Policy mapRolesPermission(ClientModel client) {
        ResourceServer server = resourceServer(client);
        if (server == null) return null;
        return authz.getStoreFactory().getPolicyStore().findByName(getMapRolesPermissionName(client), server.getId());
    }

    @Override
    public Policy mapRolesClientScopePermission(ClientModel client) {
        ResourceServer server = resourceServer(client);
        if (server == null) return null;
        return authz.getStoreFactory().getPolicyStore().findByName(getMapRolesClientScopePermissionName(client), server.getId());
    }

    @Override
    public Policy mapRolesCompositePermission(ClientModel client) {
        ResourceServer server = resourceServer(client);
        if (server == null) return null;
        return authz.getStoreFactory().getPolicyStore().findByName(getMapRolesCompositePermissionName(client), server.getId());
    }

    @Override
    public Policy managePermission(ClientModel client) {
        ResourceServer server = resourceServer(client);
        if (server == null) return null;
        return authz.getStoreFactory().getPolicyStore().findByName(getManagePermissionName(client), server.getId());
    }

    @Override
    public Policy viewPermission(ClientModel client) {
        ResourceServer server = resourceServer(client);
        if (server == null) return null;
        return authz.getStoreFactory().getPolicyStore().findByName(getViewPermissionName(client), server.getId());
    }

    @Override
    public ResourceServer resourceServer(ClientModel client) {
        return authz.getStoreFactory().getResourceServerStore().findByClient(client.getId());
    }

    @Override
    public boolean canMapCompositeRoles(ClientModel client) {
        ResourceServer server = resourceServer(client);
        if (server == null) return false;

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(getResourceName(client), server.getId());
        if (resource == null) return false;

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(getMapRolesCompositePermissionName(client), server.getId());
        if (policy == null) {
            return false;
        }

        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        // if no policies attached to permission then just do default behavior
        if (associatedPolicies == null || associatedPolicies.isEmpty()) {
            return false;
        }

        Scope scope = authz.getStoreFactory().getScopeStore().findByName(MAP_ROLES_COMPOSITE_SCOPE, server.getId());
        return root.evaluatePermission(resource, scope, server);
    }
    @Override
    public boolean canMapClientScopeRoles(ClientModel client) {
        ResourceServer server = resourceServer(client);
        if (server == null) return false;

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(getResourceName(client), server.getId());
        if (resource == null) return false;

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(getMapRolesClientScopePermissionName(client), server.getId());
        if (policy == null) {
            return false;
        }

        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        // if no policies attached to permission then just do default behavior
        if (associatedPolicies == null || associatedPolicies.isEmpty()) {
            return false;
        }

        Scope scope = authz.getStoreFactory().getScopeStore().findByName(MAP_ROLES_CLIENT_SCOPE, server.getId());
        return root.evaluatePermission(resource, scope, server);
    }

}
