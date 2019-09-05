/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import org.jboss.logging.Logger;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * Implements the migration necessary for version 6.0.0.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class MigrateTo6_0_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("6.0.0");

    private static final Logger LOG = Logger.getLogger(MigrateTo6_0_0.class);

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void migrate(KeycloakSession session) {
        session.realms().getRealms().stream().forEach(r -> {
            migrateRealm(session, r, false);
        });
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        migrateRealm(session, realm, true);
    }

    protected void migrateRealm(KeycloakSession session, RealmModel realm, boolean jsn) {
        MigrationProvider migrationProvider = session.getProvider(MigrationProvider.class);

        // create 'microprofile-jwt' optional client scope in the realm.
        ClientScopeModel mpJWTScope = migrationProvider.addOIDCMicroprofileJWTClientScope(realm);

        LOG.debugf("Added '%s' optional client scope", mpJWTScope.getName());

        // assign 'microprofile-jwt' optional client scope to all the OIDC clients.
        for (ClientModel client : realm.getClients()) {
            if ((client.getProtocol() == null || "openid-connect".equals(client.getProtocol())) && (!client.isBearerOnly())) {
                client.addClientScope(mpJWTScope, false);
            }
        }

        LOG.debugf("Client scope '%s' assigned to all the clients", mpJWTScope.getName());
    }
}
