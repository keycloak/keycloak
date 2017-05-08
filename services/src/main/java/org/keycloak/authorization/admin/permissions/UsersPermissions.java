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
package org.keycloak.authorization.admin.permissions;

import org.jboss.logging.Logger;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision;
import org.keycloak.authorization.common.DefaultEvaluationContext;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.permission.evaluator.PermissionEvaluator;
import org.keycloak.authorization.policy.evaluation.DecisionResult;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.authorization.util.Permissions;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.RealmAuth;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages default policies for all users.
 *
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UsersPermissions {
    private static final Logger logger = Logger.getLogger(UsersPermissions.class);
    public static final String MANAGE_PERMISSION_USERS = "manage.permission.users";
    public static final String USERS_RESOURCE = "Users";
    protected final KeycloakSession session;
    protected final RealmModel realm;
    protected final AuthorizationProvider authz;
    protected final MgmtPermissions root;

    public UsersPermissions(KeycloakSession session, RealmModel realm, AuthorizationProvider authz, MgmtPermissions root) {
        this.session = session;
        this.realm = realm;
        this.authz = authz;
        this.root = root;
    }

    private void initialize() {
        ClientModel client = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
        ResourceServer server = root.findOrCreateResourceServer(client);
        Scope manageScope = authz.getStoreFactory().getScopeStore().findByName(MgmtPermissions.MANAGE_SCOPE, server.getId());
        if (manageScope == null) {
            manageScope = authz.getStoreFactory().getScopeStore().create(MgmtPermissions.MANAGE_SCOPE, server);

        }
        Resource usersResource = authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
        if (usersResource == null) {
            usersResource = authz.getStoreFactory().getResourceStore().create(USERS_RESOURCE, server, server.getClientId());
        }
        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(MANAGE_PERMISSION_USERS, server.getId());
        if (policy == null) {
            Set<Scope> scopeset = new HashSet<>();
            scopeset.add(manageScope);
            usersResource.updateScopes(scopeset);
            Policy manageUsersPolicy = root.roles().manageUsersPolicy(server);
            Helper.addScopePermission(authz, server, MANAGE_PERMISSION_USERS, usersResource, manageScope, manageUsersPolicy);
        }
    }

    public boolean isPermissionsEnabled() {
        ResourceServer server = resourceServer();
        if (server == null) return false;

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
        if (resource == null) return false;

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(MANAGE_PERMISSION_USERS, server.getId());

        return policy != null;
    }

    public void setPermissionsEnabled(boolean enable) {
        ClientModel client = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
        if (enable) {
            initialize();
        } else {
            ResourceServer server = authz.getStoreFactory().getResourceServerStore().findByClient(client.getId());
            if (server == null) return;
            Resource usersResource = authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
            if (usersResource == null) {
                authz.getStoreFactory().getResourceStore().delete(usersResource.getId());
            }
            Policy policy = authz.getStoreFactory().getPolicyStore().findByName(MANAGE_PERMISSION_USERS, server.getId());
            if (policy == null) {
                authz.getStoreFactory().getPolicyStore().delete(policy.getId());

            }
        }
    }

    private Resource getUsersResource(ResourceServer server) {
        Resource usersResource = authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
        if (usersResource == null) {
            usersResource = authz.getStoreFactory().getResourceStore().create(USERS_RESOURCE, server, server.getClientId());
        }
        return usersResource;
    }

    private Scope getManageScope(ResourceServer server) {
        Scope manageScope = authz.getStoreFactory().getScopeStore().findByName(MgmtPermissions.MANAGE_SCOPE, server.getId());
        if (manageScope == null) {
            manageScope = authz.getStoreFactory().getScopeStore().create(MgmtPermissions.MANAGE_SCOPE, server);

        }
        return manageScope;
    }

    private ResourceServer getRealmManagementResourceServer() {
        ClientModel client = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
        return root.findOrCreateResourceServer(client);
    }

    private boolean canManageDefault(UserModel admin) {
        RealmAuth auth = root.getRealmAuth();
        if (auth != null) {
            auth.init(RealmAuth.Resource.USER);
            return auth.hasManage();
        } else {
            ClientModel client = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
            RoleModel manageUsers = client.getRole(AdminRoles.MANAGE_USERS);
            return admin.hasRole(manageUsers);
        }

    }

    public boolean canManage() {
        if (root.getRealmAuth() == null) {
            throw new NullPointerException("Realm auth null");
        }
        return canManage(root.getRealmAuth().getAuth().getUser());
    }

    public Resource resource() {
        ResourceServer server = resourceServer();
        if (server == null) return null;

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
        if (resource == null) return null;
        return  authz.getStoreFactory().getResourceStore().findById(resource.getId(), server.getId());

    }

    /**
     * Is admin allowed to manage users?  In Authz terms, does the admin have the "map-role" scope for the role's Authz resource?
     *
     * This method will follow the old default behavior (does the admin have the manage-users role) if any of these conditions
     * are met.:
     * - The admin is from the master realm managing a different realm
     * - If the Authz objects are not set up correctly for the Users resource in Authz
     * - The "manage" permission for the Users resource has an empty associatedPolicy list.
     *
     * Otherwise, it will use the Authz policy engine to resolve this answer.
     *
     * @param admin
     * @return
     */
    public boolean canManage(UserModel admin) {
        if (!root.isAdminSameRealm()) {
            return canManageDefault(admin);
        }

        ResourceServer server = resourceServer();
        if (server == null) return canManageDefault(admin);

        Resource resource =  authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, server.getId());
        if (resource == null) return canManageDefault(admin);

        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(MANAGE_PERMISSION_USERS, server.getId());
        if (policy == null) {
            return canManageDefault(admin);
        }

        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        // if no policies attached to permission then just do default behavior
        if (associatedPolicies == null || associatedPolicies.isEmpty()) {
            return canManageDefault(admin);
        }

        RealmModel oldRealm = session.getContext().getRealm();
        try {
            session.getContext().setRealm(realm);
            Identity identity = root.identity();
            EvaluationContext context = new DefaultEvaluationContext(identity, session);
            DecisionResult decisionCollector = new DecisionResult();
            ResourceServer resourceServer = getRealmManagementResourceServer();
            Resource roleResource = authz.getStoreFactory().getResourceStore().findByName(USERS_RESOURCE, resourceServer.getId());
            Scope manageScope = getManageScope(resourceServer);

            List<ResourcePermission> permissions = Permissions.permission(resourceServer, roleResource, manageScope);
            PermissionEvaluator from = authz.evaluators().from(permissions, context);
            from.evaluate(decisionCollector);
            if (!decisionCollector.completed()) {
                logger.error("Failed to run Users manage check", decisionCollector.getError());
                return false;
            }
            return decisionCollector.getResults().get(0).getEffect() == Decision.Effect.PERMIT;
        } finally {
            session.getContext().setRealm(oldRealm);
        }

    }

    public ResourceServer resourceServer() {
        ClientModel client = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
        return authz.getStoreFactory().getResourceServerStore().findByClient(client.getId());
    }

    public Policy managePermission() {
        ResourceServer server = resourceServer();
        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(MANAGE_PERMISSION_USERS, server.getId());
        // have to do this because findByName returns a Jpa Entity todo change when fixed
        return authz.getStoreFactory().getPolicyStore().findById(policy.getId(), server.getId());

    }


}
