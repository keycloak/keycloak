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
import org.keycloak.authorization.common.UserModelIdentity;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.models.AdminRoles;
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
    public static final String MAP_ROLES_SCOPE="map-roles";
    public static final String IMPERSONATE_SCOPE="impersonate";
    public static final String USER_IMPERSONATED_SCOPE="user-impersonated";
    public static final String MANAGE_GROUP_MEMBERSHIP_SCOPE="manage-group-membership";
    public static final String MAP_ROLES_PERMISSION_USERS = "map-roles.permission.users";
    public static final String ADMIN_IMPERSONATING_PERMISSION = "admin-impersonating.permission.users";
    public static final String USER_IMPERSONATED_PERMISSION = "user-impersonated.permission.users";
    public static final String MANAGE_GROUP_MEMBERSHIP_PERMISSION_USERS = "manage-group-membership.permission.users";
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
        Scope mapRolesScope = root.initializeRealmScope(MAP_ROLES_SCOPE);
        Scope impersonateScope = root.initializeRealmScope(IMPERSONATE_SCOPE);
        Scope userImpersonatedScope = root.initializeRealmScope(USER_IMPERSONATED_SCOPE);
        Scope manageGroupMembershipScope = root.initializeRealmScope(MANAGE_GROUP_MEMBERSHIP_SCOPE);

        Resource usersResource = authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
        if (usersResource == null) {
            usersResource = authz.getStoreFactory().getResourceStore().create(USERS_RESOURCE, server, server.getClientId());
            Set<Scope> scopeset = new HashSet<>();
            scopeset.add(manageScope);
            scopeset.add(viewScope);
            scopeset.add(mapRolesScope);
            scopeset.add(impersonateScope);
            scopeset.add(manageGroupMembershipScope);
            scopeset.add(userImpersonatedScope);
            usersResource.updateScopes(scopeset);
        }
        Policy managePermission = authz.getStoreFactory().getPolicyStore().findByName(MANAGE_PERMISSION_USERS, server.getId());
        if (managePermission == null) {
            Helper.addEmptyScopePermission(authz, server, MANAGE_PERMISSION_USERS, usersResource, manageScope);
        }
        Policy viewPermission = authz.getStoreFactory().getPolicyStore().findByName(VIEW_PERMISSION_USERS, server.getId());
        if (viewPermission == null) {
            Helper.addEmptyScopePermission(authz, server, VIEW_PERMISSION_USERS, usersResource, viewScope);
        }
        Policy mapRolesPermission = authz.getStoreFactory().getPolicyStore().findByName(MAP_ROLES_PERMISSION_USERS, server.getId());
        if (mapRolesPermission == null) {
            Helper.addEmptyScopePermission(authz, server, MAP_ROLES_PERMISSION_USERS, usersResource, mapRolesScope);
        }
        Policy membershipPermission = authz.getStoreFactory().getPolicyStore().findByName(MANAGE_GROUP_MEMBERSHIP_PERMISSION_USERS, server.getId());
        if (membershipPermission == null) {
            Helper.addEmptyScopePermission(authz, server, MANAGE_GROUP_MEMBERSHIP_PERMISSION_USERS, usersResource, manageGroupMembershipScope);
        }
        Policy impersonatePermission = authz.getStoreFactory().getPolicyStore().findByName(ADMIN_IMPERSONATING_PERMISSION, server.getId());
        if (impersonatePermission == null) {
            Helper.addEmptyScopePermission(authz, server, ADMIN_IMPERSONATING_PERMISSION, usersResource, impersonateScope);
        }
        impersonatePermission = authz.getStoreFactory().getPolicyStore().findByName(USER_IMPERSONATED_PERMISSION, server.getId());
        if (impersonatePermission == null) {
            Helper.addEmptyScopePermission(authz, server, USER_IMPERSONATED_PERMISSION, usersResource, userImpersonatedScope);
        }
    }

    @Override
    public Map<String, String> getPermissions() {
        Map<String, String> scopes = new HashMap<>();
        scopes.put(AdminPermissionManagement.MANAGE_SCOPE, managePermission().getId());
        scopes.put(AdminPermissionManagement.VIEW_SCOPE, viewPermission().getId());
        scopes.put(MAP_ROLES_SCOPE, mapRolesPermission().getId());
        scopes.put(MANAGE_GROUP_MEMBERSHIP_SCOPE, manageGroupMembershipPermission().getId());
        scopes.put(IMPERSONATE_SCOPE, adminImpersonatingPermission().getId());
        scopes.put(USER_IMPERSONATED_SCOPE, userImpersonatedPermission().getId());
        return scopes;
    }

    @Override
    public boolean isPermissionsEnabled() {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return false;

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
        if (resource == null) return false;

        Policy policy = managePermission();

        return policy != null;
    }

    @Override
    public void setPermissionsEnabled(boolean enable) {
        if (enable) {
            initialize();
        } else {
            deletePermissionSetup();
        }
    }

    private void deletePermissionSetup() {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return;
        Policy policy = managePermission();
        if (policy != null) {
            authz.getStoreFactory().getPolicyStore().delete(policy.getId());

        }
        policy = viewPermission();
        if (policy != null) {
            authz.getStoreFactory().getPolicyStore().delete(policy.getId());

        }
        policy = mapRolesPermission();
        if (policy != null) {
            authz.getStoreFactory().getPolicyStore().delete(policy.getId());

        }
        policy = manageGroupMembershipPermission();
        if (policy != null) {
            authz.getStoreFactory().getPolicyStore().delete(policy.getId());

        }
        policy = adminImpersonatingPermission();
        if (policy != null) {
            authz.getStoreFactory().getPolicyStore().delete(policy.getId());

        }
        policy = userImpersonatedPermission();
        if (policy != null) {
            authz.getStoreFactory().getPolicyStore().delete(policy.getId());

        }
        Resource usersResource = authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
        if (usersResource == null) {
            authz.getStoreFactory().getResourceStore().delete(usersResource.getId());
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

    @Override
    public Policy manageGroupMembershipPermission() {
        ResourceServer server = root.realmResourceServer();
        return authz.getStoreFactory().getPolicyStore().findByName(MANAGE_GROUP_MEMBERSHIP_PERMISSION_USERS, server.getId());
    }

    @Override
    public Policy mapRolesPermission() {
        ResourceServer server = root.realmResourceServer();
        return authz.getStoreFactory().getPolicyStore().findByName(MAP_ROLES_PERMISSION_USERS, server.getId());
    }


    @Override
    public Policy adminImpersonatingPermission() {
        ResourceServer server = root.realmResourceServer();
        return authz.getStoreFactory().getPolicyStore().findByName(ADMIN_IMPERSONATING_PERMISSION, server.getId());
    }

    @Override
    public Policy userImpersonatedPermission() {
        ResourceServer server = root.realmResourceServer();
        return authz.getStoreFactory().getPolicyStore().findByName(USER_IMPERSONATED_PERMISSION, server.getId());
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
        if (canManageDefault()) return true;
        if (!root.isAdminSameRealm()) {
            return false;
        }

        ResourceServer server = root.realmResourceServer();
        if (server == null) return false;

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
        if (resource == null) return false;

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(MANAGE_PERMISSION_USERS, server.getId());
        if (policy == null) {
            return false;
        }

        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        // if no policies attached to permission then just do default behavior
        if (associatedPolicies == null || associatedPolicies.isEmpty()) {
            return false;
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
        return canView() || root.hasOneAdminRole(AdminRoles.QUERY_USERS);
    }

    @Override
    public void requireQuery() {
        if (!canQuery()) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canQuery(UserModel user) {
        return canView(user);
    }

    @Override
    public void requireQuery(UserModel user) {
        if (!canQuery(user)) {
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
        if (canViewDefault()) return true;
        if (!root.isAdminSameRealm()) {
            return false;
        }

        return hasViewPermission() || canManage();
    }

    private boolean hasViewPermission() {
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
        return canView() || canViewByGroup(user);
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
    public boolean canImpersonate(UserModel user) {
        if (!canImpersonate()) {
            return false;
        }

        Identity userIdentity = new UserModelIdentity(root.realm, user);
        if (!root.isAdminSameRealm()) {
            return true;
        }

        ResourceServer server = root.realmResourceServer();
        if (server == null) return true;

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
        if (resource == null) return true;

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(USER_IMPERSONATED_PERMISSION, server.getId());
        if (policy == null) {
            return true;
        }

        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        // if no policies attached to permission then just do default behavior
        if (associatedPolicies == null || associatedPolicies.isEmpty()) {
            return true;
        }

        Scope scope = root.realmScope(USER_IMPERSONATED_SCOPE);
        return root.evaluatePermission(resource, scope, server, userIdentity);

    }

    @Override
    public boolean canImpersonate() {
        if (root.hasOneAdminRole(ImpersonationConstants.IMPERSONATION_ROLE)) return true;

        if (!root.isAdminSameRealm()) {
            return false;
        }

        ResourceServer server = root.realmResourceServer();
        if (server == null) return false;

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
        if (resource == null) return false;

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(ADMIN_IMPERSONATING_PERMISSION, server.getId());
        if (policy == null) {
            return false;
        }

        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        // if no policies attached to permission then just do default behavior
        if (associatedPolicies == null || associatedPolicies.isEmpty()) {
            return false;
        }

        Scope scope = root.realmScope(IMPERSONATE_SCOPE);
        return root.evaluatePermission(resource, scope, server);
    }

    @Override
    public void requireImpersonate(UserModel user) {
        if (!canImpersonate(user)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public Map<String, Boolean> getAccess(UserModel user) {
        Map<String, Boolean> map = new HashMap<>();
        map.put("view", canView(user));
        map.put("manage", canManage(user));
        map.put("mapRoles", canMapRoles(user));
        map.put("manageGroupMembership", canManageGroupMembership(user));
        map.put("impersonate", canImpersonate(user));
        return map;
    }

    @Override
    public boolean canMapRoles(UserModel user) {
        if (canManage(user)) return true;

        if (!root.isAdminSameRealm()) {
            return false;
        }

        ResourceServer server = root.realmResourceServer();
        if (server == null) return false;

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
        if (resource == null) return false;

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(MAP_ROLES_PERMISSION_USERS, server.getId());
        if (policy == null) {
            return false;
        }

        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        // if no policies attached to permission then just do default behavior
        if (associatedPolicies == null || associatedPolicies.isEmpty()) {
            return false;
        }

        Scope scope = root.realmScope(MAP_ROLES_SCOPE);
        return root.evaluatePermission(resource, scope, server);

    }

    @Override
    public void requireMapRoles(UserModel user) {
        if (!canMapRoles(user)) {
            throw new ForbiddenException();
        }

    }


    @Override
    public boolean canManageGroupMembership(UserModel user) {
        if (canManage(user)) return true;

        if (!root.isAdminSameRealm()) {
            return false;
        }

        ResourceServer server = root.realmResourceServer();
        if (server == null) return false;

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
        if (resource == null) return false;

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(MANAGE_GROUP_MEMBERSHIP_PERMISSION_USERS, server.getId());
        if (policy == null) {
            return false;
        }

        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        // if no policies attached to permission then just do default behavior
        if (associatedPolicies == null || associatedPolicies.isEmpty()) {
            return false;
        }

        Scope scope = root.realmScope(MANAGE_GROUP_MEMBERSHIP_SCOPE);
        return root.evaluatePermission(resource, scope, server);

    }

    @Override
    public void requireManageGroupMembership(UserModel user) {
        if (!canManageGroupMembership(user)) {
            throw new ForbiddenException();
        }

    }






}
