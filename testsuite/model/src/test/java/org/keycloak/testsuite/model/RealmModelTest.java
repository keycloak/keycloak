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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderEventListener;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@RequireProvider(RealmProvider.class)
public class RealmModelTest extends KeycloakModelTest {

    private String realmId;
    private String realm1Id;
    private String realm2Id;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "realm");
        s.getContext().setRealm(realm);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        this.realmId = realm.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        this.removeRealm(s, realmId);
        if (realm1Id != null) this.removeRealm(s, realm1Id);
        if (realm2Id != null) this.removeRealm(s, realm2Id);
    }

    private void removeRealm(KeycloakSession s, String realmId) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
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

    @Test
    public void testRealmPreRemoveDoesntRemoveEntitiesFromOtherRealms() {
        realm1Id = inComittedTransaction(session -> {
            RealmModel realm = session.realms().createRealm("realm1");
            realm.setDefaultRole(session.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
            return realm.getId();
        });
        realm2Id = inComittedTransaction(session -> {
            RealmModel realm = session.realms().createRealm("realm2");
            realm.setDefaultRole(session.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
            return realm.getId();
        });

        // Create client with resource server
        String clientRealm1 = withRealm(realm1Id, (keycloakSession, realmModel) -> {
            ClientModel clientRealm = realmModel.addClient("clientRealm1");
            AuthorizationProvider provider = keycloakSession.getProvider(AuthorizationProvider.class);
            provider.getStoreFactory().getResourceServerStore().create(clientRealm);

            return clientRealm.getId();
        });

        // Remove realm 2
        inComittedTransaction( (Consumer<KeycloakSession>)  keycloakSession -> this.removeRealm(keycloakSession, realm2Id));

        // ResourceServer in realm1 must still exist
        ResourceServer resourceServer = withRealm(realm1Id, (keycloakSession, realmModel) -> {
            ClientModel client1 = realmModel.getClientById(clientRealm1);
            return keycloakSession.getProvider(AuthorizationProvider.class).getStoreFactory().getResourceServerStore().findByClient(client1);
        });

        assertThat(resourceServer, notNullValue());
    }

    @Test
    public void testMoveGroup() {
        ProviderEventListener providerEventListener = null;
        try {
            List<GroupModel.GroupPathChangeEvent> groupPathChangeEvents = new ArrayList<>();
            providerEventListener = event -> {
                if (event instanceof GroupModel.GroupPathChangeEvent) {
                    groupPathChangeEvents.add((GroupModel.GroupPathChangeEvent) event);
                }
            };
            getFactory().register(providerEventListener);

            withRealm(realmId, (session, realm) -> {
                GroupModel groupA = realm.createGroup("a");
                GroupModel groupB = realm.createGroup("b");

                final String previousPath = "/a";
                assertThat(KeycloakModelUtils.buildGroupPath(groupA), equalTo(previousPath));

                realm.moveGroup(groupA, groupB);

                final String expectedNewPath = "/b/a";
                assertThat(KeycloakModelUtils.buildGroupPath(groupA), equalTo(expectedNewPath));

                assertThat(groupPathChangeEvents, hasSize(1));
                GroupModel.GroupPathChangeEvent groupPathChangeEvent = groupPathChangeEvents.get(0);
                assertThat(groupPathChangeEvent.getPreviousPath(), equalTo(previousPath));
                assertThat(groupPathChangeEvent.getNewPath(), equalTo(expectedNewPath));

                return null;
            });
        } finally {
            if (providerEventListener != null) {
                getFactory().unregister(providerEventListener);
            }
        }
    }
}
