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

package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * Migrates realms and clients that still use the removed Login v1 theme (`keycloak`)
 * to the Login v2 theme (`keycloak.v2`).
 */
public class MigrateTo27_0_0 extends RealmMigration {

    public static final ModelVersion VERSION = new ModelVersion("27.0.0");

    private static final String LOGIN_THEME_ATTRIBUTE = "login_theme";
    private static final String LEGACY_LOGIN_THEME = "keycloak";
    private static final String LOGIN_V2_THEME = "keycloak.v2";

    @Override
    public void migrateRealm(KeycloakSession session, RealmModel realm) {
        updateLoginTheme(realm);
        updateClientLoginThemes(realm);
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        updateLoginTheme(realm);
        updateClientLoginThemes(realm);
    }

    private void updateLoginTheme(RealmModel realm) {
        if (LEGACY_LOGIN_THEME.equals(realm.getLoginTheme())) {
            realm.setLoginTheme(LOGIN_V2_THEME);
        }
    }

    private void updateClientLoginThemes(RealmModel realm) {
        realm.getClientsStream()
                .filter(client -> LEGACY_LOGIN_THEME.equals(client.getAttribute(LOGIN_THEME_ATTRIBUTE)))
                .forEach(client -> client.setAttribute(LOGIN_THEME_ATTRIBUTE, LOGIN_V2_THEME));
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}
