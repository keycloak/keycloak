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

import org.keycloak.Config;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrateTo1_6_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("1.6.0");

    public ModelVersion getVersion() {
        return VERSION;
    }

    public void migrate(KeycloakSession session) {
        MigrationProvider provider = session.getProvider(MigrationProvider.class);

        ProtocolMapperModel localeMapper = provider.getBuiltinMappers("openid-connect").get("locale");

        if (localeMapper == null) {
            throw new RuntimeException("Can't find default locale mapper");
        }

        session.realms().getRealmsStream().forEach(realm -> migrateRealm(session, localeMapper, realm));
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        MigrationProvider provider = session.getProvider(MigrationProvider.class);
        ProtocolMapperModel localeMapper = provider.getBuiltinMappers("openid-connect").get("locale");

        if (localeMapper == null) {
            throw new RuntimeException("Can't find default locale mapper");
        }
        migrateRealm(session, localeMapper, realm);


    }

    protected void migrateRealm(KeycloakSession session, ProtocolMapperModel localeMapper, RealmModel realm) {
        realm.setOfflineSessionIdleTimeout(Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT);

        if (realm.getRole(Constants.OFFLINE_ACCESS_ROLE) == null) {
            KeycloakModelUtils.setupOfflineRole(realm);
            RoleModel role = realm.getRole(Constants.OFFLINE_ACCESS_ROLE);

            // Bulk grant of offline_access role to all users
            session.users().grantToAllUsers(realm, role);
        }

        ClientModel adminConsoleClient = realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
        if ((adminConsoleClient != null) && !localeMapperAdded(adminConsoleClient)) {
            adminConsoleClient.addProtocolMapper(localeMapper);
        }

        ClientModel client = realm.getMasterAdminClient();
        if (client.getRole(AdminRoles.CREATE_CLIENT) == null) {
            RoleModel role = client.addRole(AdminRoles.CREATE_CLIENT);
            role.setDescription("${role_" + AdminRoles.CREATE_CLIENT + "}");

            client.getRealm().getRole(AdminRoles.ADMIN).addCompositeRole(role);
        }

        if (!realm.getName().equals(Config.getAdminRealm())) {
            client = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
            if (client.getRole(AdminRoles.CREATE_CLIENT) == null) {
                RoleModel role = client.addRole(AdminRoles.CREATE_CLIENT);
                role.setDescription("${role_" + AdminRoles.CREATE_CLIENT + "}");
                client.getRole(AdminRoles.REALM_ADMIN).addCompositeRole(role);
            }
        }
    }

    private boolean localeMapperAdded(ClientModel adminConsoleClient) {
        return adminConsoleClient.getProtocolMapperByName("openid-connect", "locale") != null;
    }

}
