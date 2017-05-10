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
import org.keycloak.Config;
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
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.util.Permissions;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleMgmtPermissions {
    private static final Logger logger = Logger.getLogger(RoleMgmtPermissions.class);
    public static final String MAP_ROLE_SCOPE = "map-role";
    protected final KeycloakSession session;
    protected final RealmModel realm;
    protected final AuthorizationProvider authz;
    protected final MgmtPermissions root;

    public RoleMgmtPermissions(KeycloakSession session, RealmModel realm, AuthorizationProvider authz, MgmtPermissions root) {
        this.session = session;
        this.realm = realm;
        this.authz = authz;
        this.root = root;
    }

    public boolean isPermissionsEnabled(RoleModel role) {
        return mapRolePermission(role) != null;
    }

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
           Policy policy = authz.getStoreFactory().getPolicyStore().findByName(getMapRoleScopePermissionName(role), server.getId());
           if (policy != null) authz.getStoreFactory().getPolicyStore().delete(policy.getId());
       }
    }

    public Policy mapRolePermission(RoleModel role) {
        ResourceServer server = resourceServer(role);
        if (server == null) return null;

        Resource resource = authz.getStoreFactory().getResourceStore().findByName(getRoleResourceName(role), server.getId());
        if (resource == null) return null;

        Policy policy =  authz.getStoreFactory().getPolicyStore().findByName(getMapRoleScopePermissionName(role), server.getId());
        return authz.getStoreFactory().getPolicyStore().findById(policy.getId(), server.getId());
    }

    public Resource resource(RoleModel role) {
        ResourceStore resourceStore = authz.getStoreFactory().getResourceStore();
        ResourceServer server = resourceServer(role);
        if (server == null) return null;
        Resource resource =  resourceStore.findByName(getRoleResourceName(role), server.getId());
        if (resource == null) return null;
        return resourceStore.findById(resource.getId(), server.getId());
    }

    public ResourceServer resourceServer(RoleModel role) {
        ClientModel client = getRoleClient(role);
        return authz.getStoreFactory().getResourceServerStore().findByClient(client.getId());
    }

    /**
     * Is admin allowed to map this role?  In Authz terms, does the user have the "map-role" scope for the role's Authz resource?
     *
     * This method is hardcoded to return TRUE if any of these conditions are met:
     * - The admin is from the master realm managing a different realm
     * - If the Authz objects are not set up correctly for this role (resource server, resource, permission)
     * - If the role's mapRole permission does not have a policy associated with it.
     *
     * Otherwise, it will use the Authz policy engine to resolve this answer.
     *
     * @param role
     * @return
     */
    public boolean canMapRole(RoleModel role) {
        if (!root.isAdminSameRealm()) {
            return true;
        }
        if (!isPermissionsEnabled(role)) return true;  // no authz permissions set up so just allow it.

        ResourceServer resourceServer = getResourceServer(role);
        Policy policy = authz.getStoreFactory().getPolicyStore().findByName(getMapRoleScopePermissionName(role), resourceServer.getId());
        if (policy.getAssociatedPolicies().isEmpty()) {
            return true; // if no policies applied, just ignore
        }

        RealmModel oldRealm = session.getContext().getRealm();
        try {
            session.getContext().setRealm(realm);
            Identity identity = root.identity();

            EvaluationContext context = new DefaultEvaluationContext(identity, session);
            DecisionResult decisionCollector = new DecisionResult();
            Resource roleResource = resource(role);
            Scope mapRoleScope = getMapRoleScope(resourceServer);

            List<ResourcePermission> permissions = Permissions.permission(resourceServer, roleResource, mapRoleScope);
            PermissionEvaluator from = authz.evaluators().from(permissions, context);
            from.evaluate(decisionCollector);
            if (!decisionCollector.completed()) {
                logger.error("Failed to run map role policy check", decisionCollector.getError());
                return false;
            }
            return decisionCollector.getResults().get(0).getEffect() == Decision.Effect.PERMIT;
        } finally {
            session.getContext().setRealm(oldRealm);
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

    public Policy manageUsersPolicy(ResourceServer server) {
        RoleModel role = root.getRealmManagementClient().getRole(AdminRoles.MANAGE_USERS);
        return rolePolicy(server, role);
    }

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


    private Resource createResource(RoleModel role) {
        ResourceServer server = getResourceServer(role);
        Resource resource =  authz.getStoreFactory().getResourceStore().create(getRoleResourceName(role), server, server.getClientId());
        resource.setType("Role");
        Scope mapRoleScope = getMapRoleScope(server);
        Helper.addEmptyScopePermission(authz, server, getMapRoleScopePermissionName(role), resource, mapRoleScope);
        return resource;
    }

    private String getMapRoleScopePermissionName(RoleModel role) {
        return MAP_ROLE_SCOPE + ".permission." + role.getName();
    }

    private ResourceServer getResourceServer(RoleModel role) {
        ClientModel client = getRoleClient(role);
        return root.findOrCreateResourceServer(client);
    }

    private static String getRoleResourceName(RoleModel role) {
        return "role.resource." + role.getName();
    }


}
