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

package org.keycloak.tests.migration;

import org.keycloak.migration.migrators.MigrateTo27_0_0;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.theme.DefaultThemeSelectorProvider;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Covers Login v1 ({@code keycloak}) → Login v2 ({@code keycloak.v2}) theme migration
 * for both database upgrades ({@link MigrateTo27_0_0#migrateRealm}) and JSON imports.
 */
@KeycloakIntegrationTest
public class MigrateTo27_0_0LoginThemeTest {

    private static final String REALM_NAME = "login-v1-theme-db";
    private static final String LEGACY_LOGIN_THEME = "keycloak";
    private static final String LOGIN_V2_THEME = "keycloak.v2";
    private static final String CUSTOM_LOGIN_THEME = "custom-theme";
    private static final String LEGACY_CLIENT_ID = "legacy-theme-client";
    private static final String CUSTOM_CLIENT_ID = "custom-theme-client";
    private static final String UNSET_CLIENT_ID = "unset-theme-client";

    @InjectRealm(config = LoginV1ThemeRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectRealm(ref = "import", fromJson = "migration-realm-login-v1-themes.json")
    ManagedRealm importedRealm;

    @TestOnServer
    public void testDatabaseUpgradeMigratesLoginThemes(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), s -> {
            RealmModel realm = s.realms().getRealmByName(REALM_NAME);
            Assertions.assertNotNull(realm);

            // Simulate a pre-27 database: legacy theme selections still present
            realm.setLoginTheme(LEGACY_LOGIN_THEME);
            ClientModel legacyClient = realm.getClientByClientId(LEGACY_CLIENT_ID);
            Assertions.assertNotNull(legacyClient);
            legacyClient.setAttribute(DefaultThemeSelectorProvider.LOGIN_THEME_KEY, LEGACY_LOGIN_THEME);

            ClientModel customClient = realm.getClientByClientId(CUSTOM_CLIENT_ID);
            Assertions.assertNotNull(customClient);
            Assertions.assertEquals(CUSTOM_LOGIN_THEME,
                    customClient.getAttribute(DefaultThemeSelectorProvider.LOGIN_THEME_KEY));

            ClientModel unsetClient = realm.getClientByClientId(UNSET_CLIENT_ID);
            Assertions.assertNotNull(unsetClient);
            Assertions.assertNull(unsetClient.getAttribute(DefaultThemeSelectorProvider.LOGIN_THEME_KEY));

            new MigrateTo27_0_0().migrateRealm(s, realm);

            Assertions.assertEquals(LOGIN_V2_THEME, realm.getLoginTheme());
            Assertions.assertEquals(LOGIN_V2_THEME,
                    legacyClient.getAttribute(DefaultThemeSelectorProvider.LOGIN_THEME_KEY));
            Assertions.assertEquals(CUSTOM_LOGIN_THEME,
                    customClient.getAttribute(DefaultThemeSelectorProvider.LOGIN_THEME_KEY));
            Assertions.assertNull(unsetClient.getAttribute(DefaultThemeSelectorProvider.LOGIN_THEME_KEY));
        });
    }

    @Test
    public void testJsonImportMigratesLoginThemes() {
        RealmRepresentation realm = importedRealm.admin().toRepresentation();
        Assertions.assertEquals(LOGIN_V2_THEME, realm.getLoginTheme());

        ClientRepresentation legacyClient = findClient(LEGACY_CLIENT_ID);
        Assertions.assertEquals(LOGIN_V2_THEME,
                legacyClient.getAttributes().get(DefaultThemeSelectorProvider.LOGIN_THEME_KEY));

        ClientRepresentation customClient = findClient(CUSTOM_CLIENT_ID);
        Assertions.assertEquals(CUSTOM_LOGIN_THEME,
                customClient.getAttributes().get(DefaultThemeSelectorProvider.LOGIN_THEME_KEY));

        ClientRepresentation unsetClient = findClient(UNSET_CLIENT_ID);
        Assertions.assertTrue(unsetClient.getAttributes() == null
                || unsetClient.getAttributes().get(DefaultThemeSelectorProvider.LOGIN_THEME_KEY) == null);
    }

    private ClientRepresentation findClient(String clientId) {
        return importedRealm.admin().clients().findByClientId(clientId).stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing client " + clientId));
    }

    public static class LoginV1ThemeRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .name(REALM_NAME)
                    .loginTheme(CUSTOM_LOGIN_THEME)
                    .clients(
                            ClientBuilder.create(LEGACY_CLIENT_ID),
                            ClientBuilder.create(CUSTOM_CLIENT_ID)
                                    .attribute(DefaultThemeSelectorProvider.LOGIN_THEME_KEY, CUSTOM_LOGIN_THEME),
                            ClientBuilder.create(UNSET_CLIENT_ID)
                    );
        }
    }
}
