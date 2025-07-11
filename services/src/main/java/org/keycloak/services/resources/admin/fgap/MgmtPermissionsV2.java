/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.AdminAuth;

class MgmtPermissionsV2 extends MgmtPermissions {

    private ClientPermissionsV2 clientPermissions;

    private GroupPermissionsV2 groupPermissions;

    private RolePermissionsV2 rolePermissions;

    private UserPermissionsV2 userPermissions;

    private RealmPermissionsV2 realmPermissions;

    public MgmtPermissionsV2(KeycloakSession session, RealmModel realm) {
        super(session, realm);
    }

    public MgmtPermissionsV2(KeycloakSession session, RealmModel realm, AdminAuth auth) {
        super(session, realm, auth);
    }

    public MgmtPermissionsV2(KeycloakSession session, AdminAuth auth) {
        super(session, auth);
    }

    public MgmtPermissionsV2(KeycloakSession session, RealmModel adminsRealm, UserModel admin) {
        super(session, adminsRealm, admin);
    }

    @Override
    public ClientModel getRealmPermissionsClient() {
        return realm.getAdminPermissionsClient();
    }

    @Override
    public RealmPermissions realm() {
        if (realmPermissions != null) return realmPermissions;
        realmPermissions = new RealmPermissionsV2(this);
        return realmPermissions;
    }

    @Override
    public GroupPermissions groups() {
        if (groupPermissions != null) return groupPermissions;
        groupPermissions = new GroupPermissionsV2(session, authz, this);
        return groupPermissions;
    }

    @Override
    public RolePermissions roles() {
        if (rolePermissions != null) return rolePermissions;
        rolePermissions = new RolePermissionsV2(session, realm, authz, this);
        return rolePermissions;
    }

    @Override
    public UserPermissions users() {
        if (userPermissions != null) return userPermissions;
        userPermissions = new UserPermissionsV2(session, authz, this);
        return userPermissions;
    }

    @Override
    public ClientPermissions clients() {
        if (clientPermissions != null) return clientPermissions;
        clientPermissions = new ClientPermissionsV2(session, realm, authz, this);
        return clientPermissions;
    }
}
