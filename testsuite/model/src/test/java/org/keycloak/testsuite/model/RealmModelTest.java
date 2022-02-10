/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.model;

import org.junit.Test;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;

@RequireProvider(RealmProvider.class)
public class RealmModelTest extends KeycloakModelTest {

    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().createRealm("realm");
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        this.realmId = realm.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testRealmLocalizationTexts() {
        withRealm(realmId, (session, realm) -> {
            // Assert emptyMap
            assertThat(realm.getRealmLocalizationTexts(), anEmptyMap());
            // Add a localization test
            session.realms().saveLocalizationText(realm, "en", "key-a", "text-a_en");
            return null;
        });

        withRealm(realmId, (session, realm) -> {
            // Assert the map contains the added value
            assertThat(realm.getRealmLocalizationTexts(), aMapWithSize(1));
            assertThat(realm.getRealmLocalizationTexts(),
                    hasEntry(equalTo("en"), allOf(aMapWithSize(1),
                            hasEntry(equalTo("key-a"), equalTo("text-a_en")))));

            // Add another localization text to previous locale
            session.realms().saveLocalizationText(realm, "en", "key-b", "text-b_en");
            return null;
        });

        withRealm(realmId, (session, realm) -> {
            assertThat(realm.getRealmLocalizationTexts(), aMapWithSize(1));
            assertThat(realm.getRealmLocalizationTexts(),
                    hasEntry(equalTo("en"), allOf(aMapWithSize(2),
                            hasEntry(equalTo("key-a"), equalTo("text-a_en")),
                            hasEntry(equalTo("key-b"), equalTo("text-b_en")))));

            // Add new locale
            session.realms().saveLocalizationText(realm, "de", "key-a", "text-a_de");
            return null;
        });

        withRealm(realmId, (session, realm) -> {
            // Check everything created successfully
            assertThat(realm.getRealmLocalizationTexts(), aMapWithSize(2));
            assertThat(realm.getRealmLocalizationTexts(),
                    hasEntry(equalTo("en"), allOf(aMapWithSize(2),
                            hasEntry(equalTo("key-a"), equalTo("text-a_en")),
                            hasEntry(equalTo("key-b"), equalTo("text-b_en")))));
            assertThat(realm.getRealmLocalizationTexts(),
                    hasEntry(equalTo("de"), allOf(aMapWithSize(1),
                            hasEntry(equalTo("key-a"), equalTo("text-a_de")))));

            return null;
        });

    }
}
