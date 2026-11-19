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
 * Migrates login themes from Login v1 (`keycloak`) and Login v2 (`keycloak.v2`)
 * to Login v3 / PatternFly 6 (`keycloak.v3`), and Account Console themes
 * {@code keycloak} / {@code keycloak.v2} / {@code keycloak.v3} to {@code keycloak.v4}.
 */
public class MigrateTo27_0_0 extends RealmMigration {

    public static final ModelVersion VERSION = new ModelVersion("27.0.0");

    private static final String LOGIN_THEME_ATTRIBUTE = "login_theme";
    private static final String LOGIN_V3_THEME = "keycloak.v3";
    private static final String ACCOUNT_V4_THEME = "keycloak.v4";

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void migrate(KeycloakSession session) {
        // Liquibase migrates persisted themes; Java migration is only needed for imports.
    }

    @Override
    public void migrateRealm(KeycloakSession session, RealmModel realm) {
        updateLoginTheme(realm);
        updateAccountTheme(realm);
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        updateLoginTheme(realm);
        updateAccountTheme(realm);
    }

    private void updateLoginTheme(RealmModel realm) {
        String loginTheme = realm.getLoginTheme();
        if (isLegacyLoginTheme(loginTheme)) {
            realm.setLoginTheme(LOGIN_V3_THEME);
        }

        realm.getClientsStream()
                .filter(client -> isLegacyLoginTheme(client.getAttribute(LOGIN_THEME_ATTRIBUTE)))
                .forEach(client -> client.setAttribute(LOGIN_THEME_ATTRIBUTE, LOGIN_V3_THEME));
    }

    private void updateAccountTheme(RealmModel realm) {
        if (isLegacyAccountTheme(realm.getAccountTheme())) {
            realm.setAccountTheme(ACCOUNT_V4_THEME);
        }
    }

    private static boolean isLegacyLoginTheme(String theme) {
        return "keycloak".equals(theme)
                || "keycloak.v2".equals(theme)
                || "rh-sso.v2".equals(theme);
    }

    private static boolean isLegacyAccountTheme(String theme) {
        return "keycloak".equals(theme)
                || "keycloak.v2".equals(theme)
                || "keycloak.v3".equals(theme);
    }
}
