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
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.services.ForbiddenException;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
class GroupPermissions implements GroupPermissionEvaluator, GroupPermissionManagement {
    private static final Logger logger = Logger.getLogger(GroupPermissions.class);
    public static final String MAP_ROLE_SCOPE = "map-role";
    public static final String MANAGE_MEMBERS_SCOPE = "manage.members";
    public static final String VIEW_MEMBERS_SCOPE = "view.members";
    protected final KeycloakSession session;
    protected final RealmModel realm;
    protected final AuthorizationProvider authz;
    protected final MgmtPermissions root;

    public GroupPermissions(KeycloakSession session, RealmModel realm, AuthorizationProvider authz, MgmtPermissions root) {
        this.session = session;
        this.realm = realm;
        this.authz = authz;
        this.root = root;
    }

    private static String getGroupResourceName(GroupModel group) {
        return "group.resource." + getGroupSuffix(group);
    }


    public static String getManagePermissionGroup(GroupModel group) {
        return "manage.permission.group." + getGroupSuffix(group);
    }

    public static String getManageMembersPermissionGroup(GroupModel group) {
        return "manage.members.permission.group." + getGroupSuffix(group);
    }

    public static String getGroupSuffix(GroupModel group) {
        return ModelToRepresentation.buildGroupPath(group).replace('/', '.');
    }

    public static String getViewPermissionGroup(GroupModel group) {
        return "view.permission.group." + getGroupSuffix(group);
    }

    public static String getViewMembersPermissionGroup(GroupModel group) {
        return "view.members.permission.group." + getGroupSuffix(group);
    }

    private void initialize(GroupModel group) {
        root.initializeRealmResourceServer();
        root.initializeRealmDefaultScopes();
        ResourceServer server = root.realmResourceServer();
        Scope manageScope = root.realmManageScope();
        Scope viewScope = root.realmViewScope();
        Scope manageMembersScope = root.initializeRealmScope(MANAGE_MEMBERS_SCOPE);
        Scope viewMembersScope = root.initializeRealmScope(VIEW_MEMBERS_SCOPE);

        String groupResourceName = getGroupResourceName(group);
        Resource groupResource = authz.getStoreFactory().getResourceStore().findByName(groupResourceName, server.getId());
        if (groupResource == null) {
            groupResource = authz.getStoreFactory().getResourceStore().create(groupResourceName, server, server.getClientId());
            Set<Scope> scopeset = new HashSet<>();
            scopeset.add(manageScope);
            scopeset.add(viewScope);
            groupResource.updateScopes(scopeset);
        }
        String managePermissionName = getManagePermissionGroup(group);
        Policy managePermission = authz.getStoreFactory().getPolicyStore().findByName(managePermissionName, server.getId());
        if (managePermission == null) {
            Policy manageUsersPolicy = root.roles().manageUsersPolicy(server);
            Helper.addScopePermission(authz, server, managePermissionName, groupResource, manageScope, manageUsersPolicy);
        }
        String viewPermissionName = getManagePermissionGroup(group);
        Policy viewPermission = authz.getStoreFactory().getPolicyStore().findByName(viewPermissionName, server.getId());
        if (viewPermission == null) {
            Policy viewUsersPolicy = root.roles().viewUsersPolicy(server);
            Helper.addScopePermission(authz, server, viewPermissionName, groupResource, viewScope, viewUsersPolicy);
        }
        String manageMembersPermissionName = getManageMembersPermissionGroup(group);
        Policy manageMembersPermission = authz.getStoreFactory().getPolicyStore().findByName(manageMembersPermissionName, server.getId());
        if (manageMembersPermission == null) {
            Helper.addEmptyScopePermission(authz, server, manageMembersPermissionName, groupResource, manageMembersScope);
        }
        String viewMembersPermissionName = getViewMembersPermissionGroup(group);
        Policy viewMembersPermission = authz.getStoreFactory().getPolicyStore().findByName(viewMembersPermissionName, server.getId());
        if (viewMembersPermission == null) {
            Helper.addEmptyScopePermission(authz, server, viewMembersPermissionName, groupResource, viewMembersScope);
        }
    }

    @Override
    public boolean canList() {
        return root.hasOneAdminRole(AdminRoles.VIEW_USERS, AdminRoles.MANAGE_USERS);
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

        return authz.getStoreFactory().getResourceStore().findByName(getGroupResourceName(group), server.getId()) != null;
    }

    private Resource groupResource(GroupModel group) {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return null;
        String groupResourceName = getGroupResourceName(group);
        return authz.getStoreFactory().getResourceStore().findByName(groupResourceName, server.getId());
    }

    @Override
    public void setPermissionsEnabled(GroupModel group, boolean enable) {
       if (enable) {
           initialize(group);
       } else {
           deletePermissions(group);
       }
    }

    private void deletePermissions(GroupModel group) {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return;
        Policy managePermission = managePermissionGroup(group);
        if (managePermission != null) {
            authz.getStoreFactory().getPolicyStore().delete(managePermission.getId());
        }
        Policy viewPermission = viewPermissionGroup(group);
        if (viewPermission != null) {
            authz.getStoreFactory().getPolicyStore().delete(viewPermission.getId());
        }
        Policy manageMembersPermission = manageMembersPermission(group);
        if (manageMembersPermission != null) {
            authz.getStoreFactory().getPolicyStore().delete(manageMembersPermission.getId());
        }
        Policy viewMembersPermission = viewMembersPermission(group);
        if (manageMembersPermission == null) {
            authz.getStoreFactory().getPolicyStore().delete(viewMembersPermission.getId());
        }
        Resource resource = groupResource(group);
        if (resource != null) authz.getStoreFactory().getResourceStore().delete(resource.getId());
    }

    @Override
    public Policy viewMembersPermission(GroupModel group) {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return null;
        String viewMembersPermissionName = getViewMembersPermissionGroup(group);
        return authz.getStoreFactory().getPolicyStore().findByName(viewMembersPermissionName, server.getId());
    }

    @Override
    public Policy manageMembersPermission(GroupModel group) {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return null;
        String manageMembersPermissionName = getManageMembersPermissionGroup(group);
        return authz.getStoreFactory().getPolicyStore().findByName(manageMembersPermissionName, server.getId());
    }

    @Override
    public Policy viewPermissionGroup(GroupModel group) {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return null;
        String viewPermissionName = getViewPermissionGroup(group);
        return authz.getStoreFactory().getPolicyStore().findByName(viewPermissionName, server.getId());
    }

    @Override
    public Policy managePermissionGroup(GroupModel group) {
        ResourceServer server = root.realmResourceServer();
        if (server == null) return null;
        String managePermissionName = getManagePermissionGroup(group);
        return authz.getStoreFactory().getPolicyStore().findByName(managePermissionName, server.getId());
    }


    @Override
    public boolean canManage(GroupModel group) {
        if (!root.isAdminSameRealm()) {
            return canManage();
        }

        ResourceServer server = root.realmResourceServer();
        if (server == null) return canManage();

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(getGroupResourceName(group), server.getId());
        if (resource == null) return canManage();

        Policy policy = managePermissionGroup(group);
        if (policy == null) {
            return canManage();
        }

        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        // if no policies attached to permission then just do default behavior
        if (associatedPolicies == null || associatedPolicies.isEmpty()) {
            return canManage();
        }

        Scope scope = root.realmManageScope();
        return root.evaluatePermission(resource, scope, server);
    }

    @Override
    public void requireManage(GroupModel group) {
        if (!canManage(group)) {
            throw new ForbiddenException();
        }
    }
    @Override
    public boolean canView(GroupModel group) {
        if (!root.isAdminSameRealm()) {
            return canView();
        }

        ResourceServer server = root.realmResourceServer();
        if (server == null) return canView();

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(getGroupResourceName(group), server.getId());
        if (resource == null) return canView();

        Policy policy = viewPermissionGroup(group);
        if (policy == null) {
            return canView();
        }

        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        // if no policies attached to permission then abort
        if (associatedPolicies == null || associatedPolicies.isEmpty()) {
            return canView();
        }

        Scope scope = root.realmViewScope();
        return root.evaluatePermission(resource, scope, server);
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
    public boolean canViewMembers(GroupModel group) {
        return canViewMembersEvaluation(group) || canManageMembers(group);
    }

    private boolean canViewMembersEvaluation(GroupModel group) {
        if (!root.isAdminSameRealm()) {
            return root.users().canView();
        }

        ResourceServer server = root.realmResourceServer();
        if (server == null) return root.users().canView();

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(getGroupResourceName(group), server.getId());
        if (resource == null) return root.users().canView();

        Policy policy = viewMembersPermission(group);
        if (policy == null) {
            return root.users().canView();
        }

        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        // if no policies attached to permission then just do default behavior
        if (associatedPolicies == null || associatedPolicies.isEmpty()) {
            return root.users().canView();
        }

        Scope scope = authz.getStoreFactory().getScopeStore().findByName(VIEW_MEMBERS_SCOPE, server.getId());

        return root.evaluatePermission(resource, scope, server);
    }


    @Override
    public void requireViewMembers(GroupModel group) {
        if (!canViewMembers(group)) {
            throw new ForbiddenException();
        }
    }


    @Override
    public boolean canManageMembers(GroupModel group) {
        if (!root.isAdminSameRealm()) {
            return root.users().canManage();
        }

        ResourceServer server = root.realmResourceServer();
        if (server == null) return root.users().canManage();

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(getGroupResourceName(group), server.getId());
        if (resource == null) return root.users().canManage();

        Policy policy = manageMembersPermission(group);
        if (policy == null) {
            return root.users().canManage();
        }

        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        // if no policies attached to permission then just do default behavior
        if (associatedPolicies == null || associatedPolicies.isEmpty()) {
            return root.users().canManage();
        }

        Scope scope = authz.getStoreFactory().getScopeStore().findByName(MANAGE_MEMBERS_SCOPE, server.getId());
        return root.evaluatePermission(resource, scope, server);
    }

    @Override
    public void requireManageMembers(GroupModel group) {
        if (!canManageMembers(group)) {
            throw new ForbiddenException();
        }
    }



}
