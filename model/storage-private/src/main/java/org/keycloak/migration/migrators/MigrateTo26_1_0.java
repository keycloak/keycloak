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

package org.keycloak.migration.migrators;

import java.lang.invoke.MethodHandles;
import org.jboss.logging.Logger;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 *
 * @author rmartinc
 */
public class MigrateTo26_1_0 implements Migration {
public static final ModelVersion VERSION = new ModelVersion("26.1.0");

    private static final Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void migrate(KeycloakSession session) {
        session.realms().getRealmsStream().forEach(realm -> migrateRealm(session, realm));
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        migrateRealm(session, realm);
    }

    private void migrateRealm(KeycloakSession session, RealmModel realm) {
        // add the new service_account scope to the realm
        MigrationProvider migrationProvider = session.getProvider(MigrationProvider.class);
        migrationProvider.addOIDCServiceAccountClientScope(realm);

        session.authenticationSessions().migrate(VERSION.toString());
    }
}
