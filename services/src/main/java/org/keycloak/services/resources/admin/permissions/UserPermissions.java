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
import org.keycloak.models.GroupModel;
import org.keycloak.models.ImpersonationConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
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
class UserPermissions implements UserPermissionEvaluator, UserPermissionManagement {
    private static final Logger logger = Logger.getLogger(UserPermissions.class);
    public static final String MANAGE_PERMISSION_USERS = "manage.permission.users";
    public static final String VIEW_PERMISSION_USERS = "view.permission.users";
    public static final String USERS_RESOURCE = "Users";
    protected final KeycloakSession session;
    protected final RealmModel realm;
    protected final AuthorizationProvider authz;
    protected final MgmtPermissions root;

    public UserPermissions(KeycloakSession session, RealmModel realm, AuthorizationProvider authz, MgmtPermissions root) {
        this.session = session;
        this.realm = realm;
        this.authz = authz;
        this.root = root;
    }


    private void initialize() {
        root.initializeRealmResourceServer();
        root.initializeRealmDefaultScopes();
        ResourceServer server = root.realmResourceServer();
        Scope manageScope = root.realmManageScope();
        Scope viewScope = root.realmViewScope();

        Resource usersResource = authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
        if (usersResource == null) {
            usersResource = authz.getStoreFactory().getResourceStore().create(USERS_RESOURCE, server, server.getClientId());
            Set<Scope> scopeset = new HashSet<>();
            scopeset.add(manageScope);
            scopeset.add(viewScope);
            usersResource.updateScopes(scopeset);
        }
        Policy managePermission = authz.getStoreFactory().getPolicyStore().findByName(MANAGE_PERMISSION_USERS, server.getId());
        if (managePermission == null) {
            Policy manageUsersPolicy = root.roles().manageUsersPolicy(server);
            Helper.addScopePermission(authz, server, MANAGE_PERMISSION_USERS, usersResource, manageScope, manageUsersPolicy);
        }
        Policy viewPermission = authz.getStoreFactory().getPolicyStore().findByName(VIEW_PERMISSION_USERS, server.getId());
        if (viewPermission == null) {
            Policy viewUsersPolicy = root.roles().viewUsersPolicy(server);
            Helper.addScopePermission(authz, server, VIEW_PERMISSION_USERS, usersResource, viewScope, viewUsersPolicy);
        }
    }

    @Override
    public Map<String, String> getPermissions() {
        Map<String, String> scopes = new HashMap<>();
        scopes.put(AdminPermissionManagement.MANAGE_SCOPE, managePermission().getId());
        scopes.put(AdminPermissionManagement.VIEW_SCOPE, viewPermission().getId());
        return scopes;
    }

    @Override
    public boolean isPermissionsEnabled() {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return false;

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
        if (resource == null) return false;

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(MANAGE_PERMISSION_USERS, server.getId());

        return policy != null;
    }

    @Override
    public void setPermissionsEnabled(boolean enable) {
        ClientModel client = root.getRealmManagementClient();
        if (enable) {
            initialize();
        } else {
            ResourceServer server = authz.getStoreFactory().getResourceServerStore().findByClient(client.getId());
            if (server == null) return;
            Policy policy = authz.getStoreFactory().getPolicyStore().findByName(MANAGE_PERMISSION_USERS, server.getId());
            if (policy == null) {
                authz.getStoreFactory().getPolicyStore().delete(policy.getId());

            }
            Resource usersResource = authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
            if (usersResource == null) {
                authz.getStoreFactory().getResourceStore().delete(usersResource.getId());
            }
        }
    }

    public boolean canManageDefault() {
        return root.hasOneAdminRole(AdminRoles.MANAGE_USERS);
    }

    @Override
    public Resource resource() {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return null;

        return  authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
    }

    @Override
    public Policy managePermission() {
        ResourceServer server = root.realmResourceServer();
        return authz.getStoreFactory().getPolicyStore().findByName(MANAGE_PERMISSION_USERS, server.getId());
    }

    @Override
    public Policy viewPermission() {
        ResourceServer server = root.realmResourceServer();
        return authz.getStoreFactory().getPolicyStore().findByName(VIEW_PERMISSION_USERS, server.getId());
    }



    /**
     * Is admin allowed to manage all users?  In Authz terms, does the admin have the "manage" scope for the Users Authz resource?
     *
     * This method will follow the old default behavior (does the admin have the manage-users role) if any of these conditions
     * are met.:
     * - The admin is from the master realm managing a different realm
     * - If the Authz objects are not set up correctly for the Users resource in Authz
     * - The "manage" permission for the Users resource has an empty associatedPolicy list.
     *
     * Otherwise, it will use the Authz policy engine to resolve this answer.
     *
     * @return
     */
    @Override
    public boolean canManage() {
        if (!root.isAdminSameRealm()) {
            return canManageDefault();
        }

        ResourceServer server = root.realmResourceServer();
        if (server == null) return canManageDefault();

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
        if (resource == null) return canManageDefault();

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(MANAGE_PERMISSION_USERS, server.getId());
        if (policy == null) {
            return canManageDefault();
        }

        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        // if no policies attached to permission then just do default behavior
        if (associatedPolicies == null || associatedPolicies.isEmpty()) {
            return canManageDefault();
        }

        Scope scope = root.realmManageScope();
        return root.evaluatePermission(resource, scope, server);

    }

    @Override
    public void requireManage() {
        if (!canManage()) {
            throw new ForbiddenException();
        }
    }


    /**
     * Does current admin have manage permissions for this particular user?
     *
     * @param user
     * @return
     */
    @Override
    public boolean canManage(UserModel user) {
        return canManage() || canManageByGroup(user);
    }

    @Override
    public void requireManage(UserModel user) {
        if (!canManage(user)) {
            throw new ForbiddenException();
        }
    }

    private interface EvaluateGroup {
        boolean evaluate(GroupModel group);
    }

    private boolean evaluateGroups(UserModel user, EvaluateGroup eval) {
        for (GroupModel group : user.getGroups()) {
            if (eval.evaluate(group)) return true;
        }
        return false;
    }

    private boolean evaluateHierarchy(UserModel user, EvaluateGroup eval) {
        Set<GroupModel> visited = new HashSet<>();
        for (GroupModel group : user.getGroups()) {
            if (evaluateHierarchy(eval, group, visited)) return true;
        }
        return false;
    }

    private boolean evaluateHierarchy(EvaluateGroup eval, GroupModel group, Set<GroupModel> visited) {
        if (visited.contains(group)) return false;
        if (eval.evaluate(group)) {
            return true;
        }
        visited.add(group);
        if (group.getParent() == null) return false;
        return evaluateHierarchy(eval, group.getParent(), visited);
    }

    private boolean canManageByGroup(UserModel user) {
        /* no inheritance
        return evaluateGroups(user,
                (group) -> root.groups().canViewMembers(group)
        );
        */

        /* inheritance
        */
        return evaluateHierarchy(user, (group) -> root.groups().canManageMembers(group));

    }
    private boolean canViewByGroup(UserModel user) {
        /* no inheritance
        return evaluateGroups(user,
                (group) -> root.groups().canViewMembers(group)
        );
        */

        /* inheritance
        */
        return evaluateHierarchy(user, (group) -> root.groups().canViewMembers(group));
    }

    public boolean canViewDefault() {
        return root.hasOneAdminRole(AdminRoles.MANAGE_USERS, AdminRoles.VIEW_USERS);
    }

    @Override
    public boolean canQuery() {
        return canViewDefault();
    }

    @Override
    public void requireQuery() {
        if (!canQuery()) {
            throw new ForbiddenException();
        }
    }



    /**
     * Is admin allowed to view all users?  In Authz terms, does the admin have the "view" scope for the Users Authz resource?
     *
     * This method will follow the old default behavior (does the admin have the view-users role) if any of these conditions
     * are met.:
     * - The admin is from the master realm managing a different realm
     * - If the Authz objects are not set up correctly for the Users resource in Authz
     * - The "view" permission for the Users resource has an empty associatedPolicy list.
     *
     * Otherwise, it will use the Authz policy engine to resolve this answer.
     *
     * @return
     */
    @Override
    public boolean canView() {
        if (!root.isAdminSameRealm()) {
            return canViewDefault();
        }

        ResourceServer server = root.realmResourceServer();
        if (server == null) return canViewDefault();

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
        if (resource == null) return canViewDefault();

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(VIEW_PERMISSION_USERS, server.getId());
        if (policy == null) {
            return canViewDefault();
        }

        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        // if no policies attached to permission then just do default behavior
        if (associatedPolicies == null || associatedPolicies.isEmpty()) {
            return canViewDefault();
        }

        Scope scope = root.realmViewScope();
        return root.evaluatePermission(resource, scope, server);
    }

    /**
     * Does current admin have view permissions for this particular user?
     *
     * Evaluates in this order. If any true, return true:
     * - canViewUsers
     * - canManageUsers
     *
     *
     * @param user
     * @return
     */
    @Override
    public boolean canView(UserModel user) {
        return canView() || canManage() || canViewByGroup(user) || canManageByGroup(user);
    }

    @Override
    public void requireView(UserModel user) {
        if (!canView(user)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public void requireView() {
        if (!(canView() || canManage())) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canImpersonate(UserModel user) {
        return root.hasOneAdminRole(ImpersonationConstants.IMPERSONATION_ROLE);
    }

    @Override
    public void requireImpersonate(UserModel user) {
        if (!canImpersonate(user)) {
            throw new ForbiddenException();
        }
    }





}
