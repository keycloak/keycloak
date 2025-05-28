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

package org.keycloak.services.resources.admin.fgap;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.ROLES_RESOURCE_TYPE;
import static org.keycloak.models.utils.KeycloakModelUtils.getMasterRealmAdminManagementClientId;
import static org.keycloak.services.managers.RealmManager.isAdministrationRealm;

import java.util.Map;
import java.util.Set;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.services.resources.admin.fgap.ModelRecord.RoleModelRecord;

class RolePermissionsV2 extends RolePermissions {

    private static final Logger logger = Logger.getLogger(RolePermissionsV2.class);
    private final FineGrainedAdminPermissionEvaluator eval;

    RolePermissionsV2(KeycloakSession session, RealmModel realm, AuthorizationProvider authz, MgmtPermissions root) {
        super(session, realm, authz, root);
        this.eval = new FineGrainedAdminPermissionEvaluator(session, root, resourceStore, policyStore);
    }

    @Override
    public boolean canMapRole(RoleModel role) {
        if (!canMapAdminRole(role)) return false;

        if (root.hasOneAdminRole(AdminRoles.MANAGE_USERS)) {
            return true;
        }

        if (role.getContainer() instanceof ClientModel clientModel) {
            if (root.clients().canMapRoles(clientModel)) {
                return true;
            }
        }

        return eval.hasPermission(new RoleModelRecord(role), null, AdminPermissionsSchema.MAP_ROLE);
    }

    @Override
    public boolean canMapComposite(RoleModel role) {
        if (!canMapAdminRole(role)) return false;

        if (role.getContainer() instanceof ClientModel clientModel) {
            if (root.hasOneAdminRole(AdminRoles.MANAGE_CLIENTS)) {
                return true;
            }
            if (root.clients().canMapCompositeRoles(clientModel)) {
                return true;
            }
        } else {
            if (root.hasOneAdminRole(AdminRoles.MANAGE_REALM)) {
                return true;
            }
        }

        return eval.hasPermission(new RoleModelRecord(role), null, AdminPermissionsSchema.MAP_ROLE_COMPOSITE);
    }

    @Override
    public boolean canMapClientScope(RoleModel role) {
        if (role.getContainer() instanceof ClientModel clientModel) {
            if (root.hasOneAdminRole(AdminRoles.MANAGE_CLIENTS)) {
                return true;
            }
            if (root.clients().canMapClientScopeRoles(clientModel)) {
                return true;
            }
        } else {
            if (root.hasOneAdminRole(AdminRoles.MANAGE_REALM)) {
                return true;
            }
        }

        return eval.hasPermission(new RoleModelRecord(role), null, AdminPermissionsSchema.MAP_ROLE_CLIENT_SCOPE);
    }

    @Override
    public Set<String> getRoleIdsByScope(String scope) {
        return eval.getIdsByScope(ROLES_RESOURCE_TYPE, scope);
    }

    boolean canMapAdminRole(RoleModel role) {
        if (!AdminRoles.ALL_ROLES.contains(role.getName())) {
            return true;
        }

        if (root.admin().hasRole(role)) return true;

        if (!role.isClientRole()) { //realm role
            // if realm name is master realm, than we know this is a admin role ("admin" or "create-realm") in master realm.
            return isAdministrationRealm((RealmModel) role.getContainer()) ? adminConflictMessage(role) : true;
        } else {
            // management client in master realm
            ClientModel client = (ClientModel)role.getContainer();
            if (isAdministrationRealm(client.getRealm()) && client.getClientId().equals(getMasterRealmAdminManagementClientId(client.getRealm().getName()))) {
                return adminConflictMessage(role);
            }

            switch (role.getName()) {
                case AdminRoles.REALM_ADMIN:
                    // check to see if we have masterRealm.admin role.  Otherwise abort
                    if (root.adminsRealm() == null || !root.adminsRealm().getName().equals(Config.getAdminRealm())) {
                        return adminConflictMessage(role);
                    }

                    RealmModel masterRealm = root.adminsRealm();
                    RoleModel adminRole = masterRealm.getRole(AdminRoles.ADMIN);
                    return root.admin().hasRole(adminRole) ? true : adminConflictMessage(role);

                case AdminRoles.QUERY_CLIENTS:
                case AdminRoles.QUERY_GROUPS:
                case AdminRoles.QUERY_REALMS:
                case AdminRoles.QUERY_USERS:
                    return true;

                case AdminRoles.MANAGE_CLIENTS:
                case AdminRoles.CREATE_CLIENT:
                    return root.clients().canManage() ? true : adminConflictMessage(role);
                case AdminRoles.VIEW_CLIENTS:
                    return root.clients().canView() ? true : adminConflictMessage(role);

                case AdminRoles.MANAGE_USERS:
                    return root.users().canManage() ? true : adminConflictMessage(role);
                case AdminRoles.VIEW_USERS:
                    return root.users().canView() ? true : adminConflictMessage(role);
                case AdminRoles.IMPERSONATION:
                    return root.users().canImpersonate() ? true : adminConflictMessage(role);

                case AdminRoles.MANAGE_REALM:
                    return root.realm().canManageRealm() ? true : adminConflictMessage(role);
                case AdminRoles.VIEW_REALM:
                    return root.realm().canViewRealm() ? true : adminConflictMessage(role);
                case AdminRoles.MANAGE_AUTHORIZATION:
                    return root.realm().canManageAuthorization(getResourceServer(role)) ? true : adminConflictMessage(role);
                case AdminRoles.VIEW_AUTHORIZATION:
                    return root.realm().canViewAuthorization(getResourceServer(role)) ? true : adminConflictMessage(role);
                case AdminRoles.MANAGE_EVENTS:
                    return root.realm().canManageEvents() ? true : adminConflictMessage(role);
                case AdminRoles.VIEW_EVENTS:
                    return root.realm().canViewEvents() ? true : adminConflictMessage(role);
                case AdminRoles.MANAGE_IDENTITY_PROVIDERS:
                    return root.realm().canManageIdentityProviders() ? true : adminConflictMessage(role);
                case AdminRoles.VIEW_IDENTITY_PROVIDERS:
                    return root.realm().canViewIdentityProviders() ? true : adminConflictMessage(role);

                default:
                    return adminConflictMessage(role);
            }
        }
    }

    private boolean adminConflictMessage(RoleModel role) {
        logger.debugf("Trying to assign admin privileges of role: %s but admin doesn't have same privilege", role.getName());
        return false;
    }

    private ResourceServer getResourceServer(RoleModel role) {
        ResourceServer resourceServer = null;
        if (role.isClientRole()) {
            RoleContainerModel container = role.getContainer();
            resourceServer = session.getProvider(AuthorizationProvider.class).getStoreFactory().getResourceServerStore().findById(container.getId());
        }
        return resourceServer;
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
