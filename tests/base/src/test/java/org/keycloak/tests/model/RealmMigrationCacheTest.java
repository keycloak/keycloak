/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.model;

import org.keycloak.migration.ModelVersion;
import org.keycloak.migration.migrators.RealmMigration;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.infinispan.RealmAdapter;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.annotations.TestOnServer;

import org.junit.jupiter.api.Assertions;

@KeycloakIntegrationTest
public class RealmMigrationCacheTest {

    private static final String REALM_NAME = "realm-migration-cache";
    private static final String CLIENT_ID = "realm-migration-cache-app";

    @InjectRealm(config = RealmMigrationCacheRealmConfig.class)
    ManagedRealm managedRealm;

    @TestOnServer
    public void testManagedModelsNotReusedByRealmMigration(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), s -> {
            RealmModel primedRealm = s.realms().getRealmByName(REALM_NAME);
            Assertions.assertTrue(primedRealm instanceof RealmAdapter);

            ClientModel primedClient = primedRealm.getClientByClientId(CLIENT_ID);
            Assertions.assertNotNull(primedClient);

            RoleModel primedRole = primedRealm.getDefaultRole();
            Assertions.assertNotNull(primedRole);

            String roleName = primedRole.getName();

            RecordingRealmMigration migration = new RecordingRealmMigration(primedRealm.getId(), primedClient.getId(), primedRole.getId());
            migration.migrate(s);

            Assertions.assertNotNull(migration.realm);
            Assertions.assertNotNull(migration.client);
            Assertions.assertNotNull(migration.role);

            Assertions.assertNotSame(primedRealm, migration.realm);
            Assertions.assertNotSame(primedClient, migration.client);
            Assertions.assertNotSame(primedRole, migration.role);

            Assertions.assertEquals(REALM_NAME, migration.realm.getName());
            Assertions.assertEquals(CLIENT_ID, migration.client.getClientId());
            Assertions.assertEquals(roleName, migration.role.getName());
        });
    }

    public static class RecordingRealmMigration extends RealmMigration {

        private final String realmId;
        private final String clientId;
        private final String roleId;

        RealmModel realm;
        ClientModel client;
        RoleModel role;

        public RecordingRealmMigration(String realmId, String clientId, String roleId) {
            this.realmId = realmId;
            this.clientId = clientId;
            this.roleId = roleId;
        }

        @Override
        public ModelVersion getVersion() {
            return new ModelVersion("999.0.0");
        }

        @Override
        public void migrateRealm(KeycloakSession session, RealmModel realm) {
            if (!realmId.equals(realm.getId())) {
                return;
            }
            this.realm = realm;
            this.client = session.clients().getClientById(realm, clientId);
            this.role = session.roles().getRoleById(realm, roleId);
        }
    }

    public static class RealmMigrationCacheRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name(REALM_NAME).clients(ClientBuilder.create(CLIENT_ID));
        }
    }

}
