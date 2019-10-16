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

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.Collections;

public class MigrateTo8_0_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("8.0.0");

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void migrate(KeycloakSession session) {
        session.realms().getRealms().stream().forEach(realm -> migrateRealm(realm));
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        migrateRealm(realm);
    }

    protected void migrateRealm(RealmModel realm) {
        ClientModel adminConsoleClient = realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
        adminConsoleClient.setRootUrl(Constants.AUTH_ADMIN_URL_PROP);
        String adminConsoleBaseUrl = "/admin/" + realm.getName() + "/console/";
        adminConsoleClient.setBaseUrl(adminConsoleBaseUrl);
        adminConsoleClient.setRedirectUris(Collections.singleton(adminConsoleBaseUrl + "*"));
        adminConsoleClient.setWebOrigins(Collections.singleton("+"));

        ClientModel accountClient = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        accountClient.setRootUrl(Constants.AUTH_BASE_URL_PROP);
        String accountClientBaseUrl = "/realms/" + realm.getName() + "/account/";
        accountClient.setBaseUrl(accountClientBaseUrl);
        accountClient.setRedirectUris(Collections.singleton(accountClientBaseUrl + "*"));
    }
}
