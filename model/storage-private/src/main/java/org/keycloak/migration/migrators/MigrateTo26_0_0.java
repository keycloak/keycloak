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


import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionProvider;

public class MigrateTo26_0_0 extends RealmMigration {

    public static final ModelVersion VERSION = new ModelVersion("26.0.0");

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void migrate(KeycloakSession session) {
        // migrate jboss-marshalling to infinispan protostream - do this only on upgrade, not on import
        UserSessionProvider userSessions = session.sessions();
        if (userSessions != null) { // can be null in the test suite.
            userSessions.migrate(VERSION.toString());
        }

        super.migrate(session);
    }

    @Override
    public void migrateRealm(KeycloakSession session, RealmModel realm) {
        ClientModel adminConsoleClient = realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
        if (adminConsoleClient != null) {
            adminConsoleClient.setFullScopeAllowed(true);
            adminConsoleClient.setAttribute(Constants.USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED, String.valueOf(true));
        }
        ClientModel adminCliClient = realm.getClientByClientId(Constants.ADMIN_CLI_CLIENT_ID);
        if (adminCliClient != null) {
            adminCliClient.setFullScopeAllowed(true);
            adminCliClient.setAttribute(Constants.USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED, String.valueOf(true));
        }
    }
}
