/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.services.ForbiddenException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
class GroupPermissions implements GroupPermissionEvaluator, GroupPermissionManagement {

    private static final String MANAGE_MEMBERSHIP_SCOPE = "manage-membership";
    private static final String MANAGE_MEMBERS_SCOPE = "manage-members";
    private static final String VIEW_MEMBERS_SCOPE = "view-members";
    private static final String RESOURCE_NAME_PREFIX = "group.resource.";

    private final AuthorizationProvider authz;
    private final MgmtPermissions root;
    private final ResourceStore resourceStore;
    private final PolicyStore policyStore;

    GroupPermissions(AuthorizationProvider authz, MgmtPermissions root) {
        this.authz = authz;
        this.root = root;
        if (Profile.isFeatureEnabled(Profile.Feature.AUTHORIZATION)) {
            resourceStore = authz.getStoreFactory().getResourceStore();
            policyStore = authz.getStoreFactory().getPolicyStore();
        } else {
            resourceStore = null;
            policyStore = null;
        }
    }

    private static String getGroupResourceName(GroupModel group) {
        return RESOURCE_NAME_PREFIX + group.getId();
    }


    private static String getManagePermissionGroup(GroupModel group) {
        return "manage.permission.group." + group.getId();
    }

    private static String getManageMembersPermissionGroup(GroupModel group) {
        return "manage.members.permission.group." + group.getId();
    }

    private static String getManageMembershipPermissionGroup(GroupModel group) {
        return "manage.membership.permission.group." + group.getId();
    }

    private static String getViewPermissionGroup(GroupModel group) {
        return "view.permission.group." + group.getId();
    }

    private static String getViewMembersPermissionGroup(GroupModel group) {
        return "view.members.permission.group." + group.getId();
    }

    private void initialize(GroupModel group) {
        root.initializeRealmResourceServer();
        root.initializeRealmDefaultScopes();
        ResourceServer server = root.realmResourceServer();
        Scope manageScope = root.realmManageScope();
        Scope viewScope = root.realmViewScope();
        Scope manageMembersScope = root.initializeRealmScope(MANAGE_MEMBERS_SCOPE);
        Scope viewMembersScope = root.initializeRealmScope(VIEW_MEMBERS_SCOPE);
        Scope manageMembershipScope = root.initializeRealmScope(MANAGE_MEMBERSHIP_SCOPE);

        String groupResourceName = getGroupResourceName(group);
        Resource groupResource = resourceStore.findByName(server, groupResourceName);
        if (groupResource == null) {
            groupResource = resourceStore.create(server, groupResourceName, server.getClientId());
            Set<Scope> scopeset = new HashSet<>();
            scopeset.add(manageScope);
            scopeset.add(viewScope);
            scopeset.add(viewMembersScope);
            scopeset.add(manageMembershipScope);
            scopeset.add(manageMembersScope);
            groupResource.updateScopes(scopeset);
            groupResource.setType("Group");
        }
        String managePermissionName = getManagePermissionGroup(group);
        Policy managePermission = policyStore.findByName(server, managePermissionName);
        if (managePermission == null) {
            Helper.addEmptyScopePermission(authz, server, managePermissionName, groupResource, manageScope);
        }
        String viewPermissionName = getViewPermissionGroup(group);
        Policy viewPermission = policyStore.findByName(server, viewPermissionName);
        if (viewPermission == null) {
            Helper.addEmptyScopePermission(authz, server, viewPermissionName, groupResource, viewScope);
        }
        String manageMembersPermissionName = getManageMembersPermissionGroup(group);
        Policy manageMembersPermission = policyStore.findByName(server, manageMembersPermissionName);
        if (manageMembersPermission == null) {
            Helper.addEmptyScopePermission(authz, server, manageMembersPermissionName, groupResource, manageMembersScope);
        }
        String viewMembersPermissionName = getViewMembersPermissionGroup(group);
        Policy viewMembersPermission = policyStore.findByName(server, viewMembersPermissionName);
        if (viewMembersPermission == null) {
            Helper.addEmptyScopePermission(authz, server, viewMembersPermissionName, groupResource, viewMembersScope);
        }
        String manageMembershipPermissionName = getManageMembershipPermissionGroup(group);
        Policy manageMembershipPermission = policyStore.findByName(server, manageMembershipPermissionName);
        if (manageMembershipPermission == null) {
            Helper.addEmptyScopePermission(authz, server, manageMembershipPermissionName, groupResource, manageMembershipScope);
        }

    }

    @Override
    public boolean canList() {
        return canView() || root.hasOneAdminRole(AdminRoles.VIEW_USERS, AdminRoles.MANAGE_USERS, AdminRoles.QUERY_GROUPS);
    }

    @Override
    public void requireList() {
        if (!canList()) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean isPermissionsEnabled(GroupModel group) {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return false;

        return resourceStore.findByName(server, getGroupResourceName(group)) != null;
    }

    @Override
    public void setPermissionsEnabled(GroupModel group, boolean enable) {
       if (enable) {
           initialize(group);
       } else {
           deletePermissions(group);
       }
    }

    @Override
    public Policy viewMembersPermission(GroupModel group) {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return null;
        return policyStore.findByName(server, getViewMembersPermissionGroup(group));
    }

    @Override
    public Policy manageMembersPermission(GroupModel group) {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return null;
        return policyStore.findByName(server, getManageMembersPermissionGroup(group));
    }

    @Override
    public Policy manageMembershipPermission(GroupModel group) {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return null;
        return policyStore.findByName(server, getManageMembershipPermissionGroup(group));
    }

    @Override
    public Policy viewPermission(GroupModel group) {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return null;
        return policyStore.findByName(server, getViewPermissionGroup(group));
    }

    @Override
    public Policy managePermission(GroupModel group) {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return null;
        return policyStore.findByName(server, getManagePermissionGroup(group));
    }

    @Override
    public Resource resource(GroupModel group) {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return null;
        Resource resource =  resourceStore.findByName(server, getGroupResourceName(group));
        if (resource == null) return null;
        return resource;
    }

    @Override
    public Map<String, String> getPermissions(GroupModel group) {
        initialize(group);
        Map<String, String> scopes = new LinkedHashMap<>();
        scopes.put(AdminPermissionManagement.VIEW_SCOPE, viewPermission(group).getId());
        scopes.put(AdminPermissionManagement.MANAGE_SCOPE, managePermission(group).getId());
        scopes.put(VIEW_MEMBERS_SCOPE, viewMembersPermission(group).getId());
        scopes.put(MANAGE_MEMBERS_SCOPE, manageMembersPermission(group).getId());
        scopes.put(MANAGE_MEMBERSHIP_SCOPE, manageMembershipPermission(group).getId());
        return scopes;
    }

    @Override
    public boolean canManage(GroupModel group) {
        if (canManage()) {
            return true;
        }

        if (!root.isAdminSameRealm()) {
            return false;
        }

        return hasPermission(group, MgmtPermissions.MANAGE_SCOPE);
    }

    @Override
    public void requireManage(GroupModel group) {
        if (!canManage(group)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canView(GroupModel group) {
        if (canView() || canManage()) {
            return true;
        }

        if (!root.isAdminSameRealm()) {
            return false;
        }

        return hasPermission(group, MgmtPermissions.VIEW_SCOPE, MgmtPermissions.MANAGE_SCOPE);
    }

    @Override
    public void requireView(GroupModel group) {
        if (!canView(group)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canManage() {
        return root.users().canManageDefault();
    }

    @Override
    public void requireManage() {
        if (!canManage()) {
            throw new ForbiddenException();
        }
    }
    @Override
    public boolean canView() {
        return root.users().canViewDefault();
    }

    @Override
    public void requireView() {
        if (!canView()) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean getGroupsWithViewPermission(GroupModel group) {
        if (root.users().canView() || root.users().canManage()) {
            return true;
        }

        if (!root.isAdminSameRealm()) {
            return false;
        }

        ResourceServer server = root.realmResourceServer();
        if (server == null) return false;

        return hasPermission(group, VIEW_MEMBERS_SCOPE, MANAGE_MEMBERS_SCOPE);
    }

    @Override
    public Set<String> getGroupsWithViewPermission() {
        if (root.users().canView() || root.users().canManage()) return Collections.emptySet();

        if (!root.isAdminSameRealm()) {
            return Collections.emptySet();
        }

        ResourceServer server = root.realmResourceServer();

        if (server == null) {
            return Collections.emptySet();
        }

        Set<String> granted = new HashSet<>();

        resourceStore.findByType(server, "Group", resource -> {
            if (hasPermission(resource, null, VIEW_MEMBERS_SCOPE, MANAGE_MEMBERS_SCOPE)) {
                granted.add(resource.getName().substring(RESOURCE_NAME_PREFIX.length()));
            }
        });

        return granted;
    }

    @Override
    public void requireViewMembers(GroupModel group) {
        if (!getGroupsWithViewPermission(group)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public boolean canManageMembers(GroupModel group) {
        if (root.users().canManage()) return true;

        if (!root.isAdminSameRealm()) {
            return false;
        }

        ResourceServer server = root.realmResourceServer();
        if (server == null) return false;

        return hasPermission(group, MANAGE_MEMBERS_SCOPE);
    }

    @Override
    public boolean canManageMembership(GroupModel group) {
        if (canManage(group)) return true;

        if (!root.isAdminSameRealm()) {
            return false;
        }

        return hasPermission(group, MANAGE_MEMBERSHIP_SCOPE);
    }

    @Override
    public void requireManageMembership(GroupModel group) {
        if (!canManageMembership(group)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public void requireManageMembers(GroupModel group) {
        if (!canManageMembers(group)) {
            throw new ForbiddenException();
        }
    }

    @Override
    public Map<String, Boolean> getAccess(GroupModel group) {
        Map<String, Boolean> map = new HashMap<>();
        map.put("view", canView(group));
        map.put("manage", canManage(group));
        map.put("manageMembership", canManageMembership(group));
        return map;
    }

    private boolean hasPermission(GroupModel group, String... scopes) {
        return hasPermission(group, null, scopes);
    }

    private boolean hasPermission(GroupModel group, EvaluationContext context, String... scopes) {
        ResourceServer server = root.realmResourceServer();

        if (server == null) {
            return false;
        }

        Resource resource =  resourceStore.findByName(server, getGroupResourceName(group));

        if (resource == null) {
            return false;
        }

        return hasPermission(resource, context, scopes);
    }

    private boolean hasPermission(Resource resource, EvaluationContext context, String... scopes) {
        ResourceServer server = root.realmResourceServer();
        Collection<Permission> permissions;

        if (context == null) {
            permissions = root.evaluatePermission(new ResourcePermission(resource, resource.getScopes(), server), server);
        } else {
            permissions = root.evaluatePermission(new ResourcePermission(resource, resource.getScopes(), server), server, context);
        }

        List<String> expectedScopes = Arrays.asList(scopes);


        for (Permission permission : permissions) {
            for (String scope : permission.getScopes()) {
                if (expectedScopes.contains(scope)) {
                    return true;
                }
            }
        }

        return false;
    }

    private Resource groupResource(GroupModel group) {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return null;
        String groupResourceName = getGroupResourceName(group);
        return resourceStore.findByName(server, groupResourceName);
    }

    private void deletePermissions(GroupModel group) {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return;

        RealmModel realm = server.getRealm();

        Policy managePermission = managePermission(group);
        if (managePermission != null) {
            policyStore.delete(realm, managePermission.getId());
        }
        Policy viewPermission = viewPermission(group);
        if (viewPermission != null) {
            policyStore.delete(realm, viewPermission.getId());
        }
        Policy manageMembersPermission = manageMembersPermission(group);
        if (manageMembersPermission != null) {
            policyStore.delete(realm, manageMembersPermission.getId());
        }
        Policy viewMembersPermission = viewMembersPermission(group);
        if (viewMembersPermission != null) {
            policyStore.delete(realm, viewMembersPermission.getId());
        }
        Policy manageMembershipPermission = manageMembershipPermission(group);
        if (manageMembershipPermission != null) {
            policyStore.delete(realm, manageMembershipPermission.getId());
        }
        Resource resource = groupResource(group);
        if (resource != null) resourceStore.delete(realm, resource.getId());
    }
}
