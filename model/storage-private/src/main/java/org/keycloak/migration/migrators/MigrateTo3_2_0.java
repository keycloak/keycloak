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
package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.RealmRepresentation;

public class MigrateTo3_2_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("3.2.0");

    @Override
    public void migrate(KeycloakSession session) {
        session.realms().getRealmsStream().forEach(realm -> migrateRealm(session, realm));
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        migrateRealm(session, realm);
    }

    protected void migrateRealm(KeycloakSession session, RealmModel realm) {
        PasswordPolicy.Builder builder = realm.getPasswordPolicy().toBuilder();
        if (!builder.contains(PasswordPolicy.HASH_ALGORITHM_ID) && "20000".equals(builder.get(PasswordPolicy.HASH_ITERATIONS_ID))) {
            realm.setPasswordPolicy(builder.remove(PasswordPolicy.HASH_ITERATIONS_ID).build(session));
        }

        if (realm.getDockerAuthenticationFlow() == null) {
            DefaultAuthenticationFlows.dockerAuthenticationFlow(realm);
        }

        ClientModel realmAccess = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
        if (realmAccess != null) {
            addRoles(realmAccess);
        }
        ClientModel masterAdminClient = realm.getMasterAdminClient();
        if (masterAdminClient != null) {
            addRoles(masterAdminClient);

        }
    }

    public void addRoles(ClientModel realmAccess) {
        RoleModel queryClients = realmAccess.getRole(AdminRoles.QUERY_CLIENTS);
        if (queryClients == null) {
            queryClients = realmAccess.addRole(AdminRoles.QUERY_CLIENTS);
            RoleModel viewClients = realmAccess.getRole(AdminRoles.VIEW_CLIENTS);
            if (viewClients != null) {
                viewClients.addCompositeRole(queryClients);
            }
        }
        RoleModel queryUsers = realmAccess.getRole(AdminRoles.QUERY_USERS);
        if (queryUsers == null) queryUsers = realmAccess.addRole(AdminRoles.QUERY_USERS);
        RoleModel queryGroups = realmAccess.getRole(AdminRoles.QUERY_GROUPS);
        if (queryGroups == null) queryGroups = realmAccess.addRole(AdminRoles.QUERY_GROUPS);

        RoleModel viewUsers = realmAccess.getRole(AdminRoles.VIEW_USERS);
        if (viewUsers != null) {
            if (!viewUsers.hasRole(queryUsers)) viewUsers.addCompositeRole(queryUsers);
            if (!viewUsers.hasRole(queryGroups)) viewUsers.addCompositeRole(queryGroups);
        }
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}
