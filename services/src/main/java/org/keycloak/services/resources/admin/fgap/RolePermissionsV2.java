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

import java.util.Map;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.fgap.ModelRecord.RoleModelRecord;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.ROLES_RESOURCE_TYPE;

class RolePermissionsV2 extends RolePermissions {

    private final FineGrainedAdminPermissionEvaluator eval;

    RolePermissionsV2(KeycloakSession session, RealmModel realm, AuthorizationProvider authz, MgmtPermissions root) {
        super(session, realm, authz, root);
        this.eval = new FineGrainedAdminPermissionEvaluator(session, root, resourceStore, policyStore);
    }

    private boolean isRealmAdmin() {
        RealmModel masterRealm = getMasterRealm();
        UserModel admin = root.admin();
        RoleModel masterAdminRole = masterRealm.getRole(AdminRoles.ADMIN);

        if (admin.hasRole(masterAdminRole)) {
            return true;
        }

        ClientModel realmManagementClient = getRealmManagementClient();

        if (realmManagementClient != null) {
            RoleModel realmAdminRole = realmManagementClient.getRole(AdminRoles.REALM_ADMIN);
            return admin.hasRole(realmAdminRole);
        }

        return false;
    }

    @Override
    public boolean canMapRole(RoleModel role) {
        if (isRealmAdminRole(role) && !isRealmAdmin()) {
            return false;
        }

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
        if (isRealmAdminRole(role) && !isRealmAdmin()) {
            return false;
        }

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

    private boolean isRealmAdminRole(RoleModel role) {
        RoleContainerModel container = role.getContainer();
        ClientModel realmManagementClient = getRealmManagementClient();

        if (container.equals(getMasterRealm()) || container.equals(realmManagementClient)) {
            return AdminRoles.ALL_ROLES.contains(role.getName());
        }

        return false;
    }

    private ClientModel getRealmManagementClient() {
        return realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
    }

    private RealmModel getMasterRealm() {
        return root.adminsRealm().getName().equals(Config.getAdminRealm()) ?
                root.adminsRealm():
                session.realms().getRealmByName(Config.getAdminRealm());
    }
}
