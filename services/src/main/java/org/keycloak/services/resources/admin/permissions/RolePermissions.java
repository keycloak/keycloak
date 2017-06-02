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
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.services.ForbiddenException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
class RolePermissions implements RolePermissionEvaluator, RolePermissionManagement {
    private static final Logger logger = Logger.getLogger(RolePermissions.class);
    protected final KeycloakSession session;
    protected final RealmModel realm;
    protected final AuthorizationProvider authz;
    protected final MgmtPermissions root;

    public RolePermissions(KeycloakSession session, RealmModel realm, AuthorizationProvider authz, MgmtPermissions root) {
        this.session = session;
        this.realm = realm;
        this.authz = authz;
        this.root = root;
    }

    @Override
    public boolean isPermissionsEnabled(RoleModel role) {
        return mapRolePermission(role) != null;
    }

    @Override
    public void setPermissionsEnabled(RoleModel role, boolean enable) {
       if (enable) {
           ResourceServer server = getResourceServer(role);
           if (authz.getStoreFactory().getResourceStore().findByName(getRoleResourceName(role), server.getId()) != null) {
               return;
           }
           createResource(role);
       } else {
           ResourceServer server = resourceServer(role);
           if (server == null) return;
           Resource resource = authz.getStoreFactory().getResourceStore().findByName(getRoleResourceName(role), server.getId());
           if (resource != null) authz.getStoreFactory().getResourceStore().delete(resource.getId());
           Policy policy = authz.getStoreFactory().getPolicyStore().findByName(getMapRolePermissionName(role), server.getId());
           if (policy != null) authz.getStoreFactory().getPolicyStore().delete(policy.getId());
       }
    }

    @Override
    public Map<String, String> getPermissions(RoleModel role) {
        Map<String, String> scopes = new HashMap<>();
        scopes.put(RolePermissionManagement.MAP_ROLE_SCOPE, mapRolePermission(role).getId());
        scopes.put(RolePermissionManagement.MAP_ROLE_CLIENT_SCOPE_SCOPE, mapClientScopePermission(role).getId());
        scopes.put(RolePermissionManagement.MAP_ROLE_COMPOSITE_SCOPE, mapCompositePermission(role).getId());
        return scopes;
    }

    @Override
    public Policy mapRolePermission(RoleModel role) {
        ResourceServer server = resourceServer(role);
        if (server == null) return null;

        Resource resource = authz.getStoreFactory().getResourceStore().findByName(getRoleResourceName(role), server.getId());
        if (resource == null) return null;

        return  authz.getStoreFactory().getPolicyStore().findByName(getMapRolePermissionName(role), server.getId());
    }

    @Override
    public Policy mapCompositePermission(RoleModel role) {
        ResourceServer server = resourceServer(role);
        if (server == null) return null;

        Resource resource = authz.getStoreFactory().getResourceStore().findByName(getRoleResourceName(role), server.getId());
        if (resource == null) return null;

        return  authz.getStoreFactory().getPolicyStore().findByName(getMapCompositePermissionName(role), server.getId());
    }

    @Override
    public Policy mapClientScopePermission(RoleModel role) {
        ResourceServer server = resourceServer(role);
        if (server == null) return null;

        Resource resource = authz.getStoreFactory().getResourceStore().findByName(getRoleResourceName(role), server.getId());
        if (resource == null) return null;

        return  authz.getStoreFactory().getPolicyStore().findByName(getMapClientScopePermissionName(role), server.getId());
    }

    @Override
    public Resource resource(RoleModel role) {
        ResourceStore resourceStore = authz.getStoreFactory().getResourceStore();
        ResourceServer server = resourceServer(role);
        if (server == null) return null;
        return  resourceStore.findByName(getRoleResourceName(role), server.getId());
    }

    @Override
    public ResourceServer resourceServer(RoleModel role) {
        ClientModel client = getRoleClient(role);
        return authz.getStoreFactory().getResourceServerStore().findByClient(client.getId());
    }

    /**
     * Is admin allowed to map this role?
     *
     * @param role
     * @return
     */
    @Override
    public boolean canMapRole(RoleModel role) {
        if (!root.isAdminSameRealm()) {
            return root.users().canManage();
        }
        if (role.getContainer() instanceof ClientModel) {
            if (root.clients().canMapRoles((ClientModel)role.getContainer())) return true;
        }
        if (!isPermissionsEnabled(role)){
            return root.users().canManage();
        }

        ResourceServer resourceServer = getResourceServer(role);
        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(getMapRolePermissionName(role), resourceServer.getId());
        if (policy.getAssociatedPolicies().isEmpty()) {
            return root.users().canManage(); // if no policies applied, just do default
        }

        Resource roleResource = resource(role);
        Scope mapRoleScope = getMapRoleScope(resourceServer);
        return root.evaluatePermission(roleResource, mapRoleScope, resourceServer);
    }

    @Override
    public void requireMapRole(RoleModel role) {
        if (!canMapRole(role)) {
            throw new ForbiddenException();
        }

    }

    @Override
    public boolean canList(RoleContainerModel container) {
        return root.hasAnyAdminRole();
    }

    @Override
    public void requireList(RoleContainerModel container) {
        if (!canList(container)) {
            throw new ForbiddenException();
        }

    }

    @Override
    public boolean canManage(RoleContainerModel container) {
        if (container instanceof RealmModel) {
            return root.realm().canManageRealm();
        } else {
            return root.clients().canManage((ClientModel)container);
        }
    }

    @Override
    public void requireManage(RoleContainerModel container) {
        if (!canManage(container)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canView(RoleContainerModel container) {
        if (container instanceof RealmModel) {
            return root.realm().canViewRealm();
        } else {
            return root.clients().canView((ClientModel)container);
        }
    }

    @Override
    public void requireView(RoleContainerModel container) {
        if (!canView(container)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canMapComposite(RoleModel role) {
        if (!root.isAdminSameRealm()) {
            return canManage(role);
        }
        if (role.getContainer() instanceof ClientModel) {
            if (root.clients().canMapCompositeRoles((ClientModel)role.getContainer())) return true;
        }
        if (!isPermissionsEnabled(role)){
            return canManage(role);
        }

        ResourceServer resourceServer = getResourceServer(role);
        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(getMapCompositePermissionName(role), resourceServer.getId());
        if (policy.getAssociatedPolicies().isEmpty()) {
            return canManage(role);
        }

        Resource roleResource = resource(role);
        Scope scope = getMapCompositeScope(resourceServer);
        return root.evaluatePermission(roleResource, scope, resourceServer);
    }

    @Override
    public void requireMapComposite(RoleModel role) {
        if (!canMapComposite(role)) {
            throw new ForbiddenException();
        }

    }


    @Override
    public boolean canMapClientScope(RoleModel role) {
        if (!root.isAdminSameRealm()) {
            return root.clients().canManage();
        }
        if (role.getContainer() instanceof ClientModel) {
            if (root.clients().canMapClientScopeRoles((ClientModel)role.getContainer())) return true;
        }
        if (!isPermissionsEnabled(role)){
            return root.clients().canManage();
        }

        ResourceServer resourceServer = getResourceServer(role);
        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(getMapClientScopePermissionName(role), resourceServer.getId());
        if (policy.getAssociatedPolicies().isEmpty()) {
            return root.clients().canManage();
        }

        Resource roleResource = resource(role);
        Scope scope = getMapClientScope(resourceServer);
        return root.evaluatePermission(roleResource, scope, resourceServer);
    }

    @Override
    public void requireMapClientScope(RoleModel role) {
        if (!canMapClientScope(role)) {
            throw new ForbiddenException();
        }
    }


    @Override
    public boolean canManage(RoleModel role) {
        if (role.getContainer() instanceof RealmModel) {
            return root.realm().canManageRealm();
        } else if (role.getContainer() instanceof ClientModel) {
            ClientModel client = (ClientModel)role.getContainer();
            return root.clients().canManage(client);
        }
        return false;
    }

    @Override
    public void requireManage(RoleModel role) {
        if (!canManage(role)) {
            throw new ForbiddenException();
        }

    }

    @Override
    public boolean canView(RoleModel role) {
        if (role.getContainer() instanceof RealmModel) {
            return root.realm().canViewRealm();
        } else if (role.getContainer() instanceof ClientModel) {
            ClientModel client = (ClientModel)role.getContainer();
            return root.clients().canView(client);
        }
        return false;
    }

    @Override
    public void requireView(RoleModel role) {
        if (!canView(role)) {
            throw new ForbiddenException();
        }

    }

    private ClientModel getRoleClient(RoleModel role) {
        ClientModel client = null;
        if (role.getContainer() instanceof ClientModel) {
            client = (ClientModel)role.getContainer();
        } else {
            client = root.getRealmManagementClient();
        }
        return client;
    }

    @Override
    public Policy manageUsersPolicy(ResourceServer server) {
        RoleModel role = root.getRealmManagementClient().getRole(AdminRoles.MANAGE_USERS);
        return rolePolicy(server, role);
    }

    @Override
    public Policy viewUsersPolicy(ResourceServer server) {
        RoleModel role = root.getRealmManagementClient().getRole(AdminRoles.VIEW_USERS);
        return rolePolicy(server, role);
    }

    @Override
    public Policy rolePolicy(ResourceServer server, RoleModel role) {
        String policyName = Helper.getRolePolicyName(role);
        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(policyName, server.getId());
        if (policy != null) return policy;
        return Helper.createRolePolicy(authz, server, role, policyName);
    }

    private Scope getMapRoleScope(ResourceServer server) {
        Scope scope = authz.getStoreFactory().getScopeStore().findByName(MAP_ROLE_SCOPE, server.getId());
        if (scope == null) {
            scope = authz.getStoreFactory().getScopeStore().create(MAP_ROLE_SCOPE, server);
        }
        return scope;
    }

    private Scope getMapClientScope(ResourceServer server) {
        Scope scope = authz.getStoreFactory().getScopeStore().findByName(MAP_ROLE_CLIENT_SCOPE_SCOPE, server.getId());
        if (scope == null) {
            scope = authz.getStoreFactory().getScopeStore().create(MAP_ROLE_CLIENT_SCOPE_SCOPE, server);
        }
        return scope;
    }

    private Scope getMapCompositeScope(ResourceServer server) {
        Scope scope = authz.getStoreFactory().getScopeStore().findByName(MAP_ROLE_COMPOSITE_SCOPE, server.getId());
        if (scope == null) {
            scope = authz.getStoreFactory().getScopeStore().create(MAP_ROLE_COMPOSITE_SCOPE, server);
        }
        return scope;
    }


    private Resource createResource(RoleModel role) {
        ResourceServer server = getResourceServer(role);
        Resource resource =  authz.getStoreFactory().getResourceStore().create(getRoleResourceName(role), server, server.getClientId());
        resource.setType("Role");
        Scope mapRoleScope = getMapRoleScope(server);
        Policy policy = manageUsersPolicy(server);
        Policy mapRolePermission = Helper.addScopePermission(authz, server, getMapRolePermissionName(role), resource, mapRoleScope, policy);
        mapRolePermission.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);

        Scope mapClientScope = getMapClientScope(server);
        RoleModel mngClients = root.getRealmManagementClient().getRole(AdminRoles.MANAGE_CLIENTS);
        Policy mngClientsPolicy = rolePolicy(server, mngClients);
        Policy mapClientScopePermission = Helper.addScopePermission(authz, server, getMapClientScopePermissionName(role), resource, mapClientScope, mngClientsPolicy);
        mapClientScopePermission.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);

        Scope mapCompositeScope = getMapCompositeScope(server);
        if (role.getContainer() instanceof RealmModel) {
            RoleModel mngRealm = root.getRealmManagementClient().getRole(AdminRoles.MANAGE_REALM);
            policy = rolePolicy(server, mngRealm);
        } else {
            policy = mngClientsPolicy;

        }
        Policy mapCompositePermission = Helper.addScopePermission(authz, server, getMapCompositePermissionName(role), resource, mapCompositeScope, policy);
        mapCompositePermission.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        return resource;
    }

    private String getMapRolePermissionName(RoleModel role) {
        return MAP_ROLE_SCOPE + ".permission." + role.getName();
    }

    private String getMapClientScopePermissionName(RoleModel role) {
        return MAP_ROLE_CLIENT_SCOPE_SCOPE + ".permission." + role.getName();
    }

    private String getMapCompositePermissionName(RoleModel role) {
        return MAP_ROLE_COMPOSITE_SCOPE + ".permission." + role.getName();
    }

    private ResourceServer getResourceServer(RoleModel role) {
        ClientModel client = getRoleClient(role);
        return root.findOrCreateResourceServer(client);
    }

    private static String getRoleResourceName(RoleModel role) {
        return "role.resource." + role.getName();
    }


}
