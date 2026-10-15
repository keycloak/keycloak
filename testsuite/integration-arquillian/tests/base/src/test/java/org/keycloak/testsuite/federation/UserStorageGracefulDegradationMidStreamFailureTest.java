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
package org.keycloak.testsuite.federation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

/**
 * Test graceful degradation when a user storage provider fails while the search result is being
 * consumed, i.e. after the first page(s) of results were already returned - like an LDAP server
 * that stops responding between the pages of a paginated search. Bounded queries must degrade the
 * failing provider to an empty result instead of surfacing a partial result set, and the remaining
 * providers must continue to work.
 *
 * @see <a href="https://github.com/keycloak/keycloak/issues/54028">54028</a>
 */
public class UserStorageGracefulDegradationMidStreamFailureTest extends AbstractTestRealmKeycloakTest implements Serializable {

    private String failingStorageProviderId;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void addUserStorageProvider() {
        AtomicReference<String> providerIdRef = new AtomicReference<>();

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealm("test");

            // Create a failing user storage provider (will be highest priority, so it is queried first)
            ComponentModel failingProvider = new ComponentModel();
            failingProvider.setId(KeycloakModelUtils.generateId());
            failingProvider.setName("failing-user-storage");
            failingProvider.setProviderId("failing-user-storage");
            failingProvider.setProviderType(UserStorageProvider.class.getName());
            failingProvider.setParentId(realm.getId());
            failingProvider.getConfig().putSingle("priority", "0");
            failingProvider.getConfig().putSingle("failOnSearchAfterFirst", "true");
            realm.addComponentModel(failingProvider);
            providerIdRef.set(failingProvider.getId());

            // Create local users in the realm (these should always work)
            UserModel localUser1 = session.users().addUser(realm, "lazy-local-user1");
            localUser1.setEnabled(true);
            localUser1.setEmail("lazy-local1@example.com");

            UserModel localUser2 = session.users().addUser(realm, "lazy-local-user2");
            localUser2.setEnabled(true);
            localUser2.setEmail("lazy-local2@example.com");
        });

        failingStorageProviderId = providerIdRef.get();
    }

    @After
    public void removeUserStorageProvider() {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealm("test");

            if (failingStorageProviderId != null) {
                ComponentModel model = realm.getComponent(failingStorageProviderId);
                if (model != null) {
                    realm.removeComponent(model);
                }
            }

            UserModel localUser1 = session.users().getUserByUsername(realm, "lazy-local-user1");
            if (localUser1 != null) {
                session.users().removeUser(realm, localUser1);
            }

            UserModel localUser2 = session.users().getUserByUsername(realm, "lazy-local-user2");
            if (localUser2 != null) {
                session.users().removeUser(realm, localUser2);
            }
        });
    }

    @Test
    @ModelTest
    public void testGracefulDegradationWhenProviderFailsAfterFirstResults(KeycloakSession session) {
        RealmModel realm = session.realms().getRealm("test");

        Map<String, String> searchParams = new HashMap<>();
        searchParams.put(UserModel.SEARCH, "lazy");

        // This must not throw an exception although the provider with the highest priority fails
        // while its result is consumed, and the users it returned before failing must not surface
        // as a partial result: the provider contributes an empty result
        Stream<UserModel> users = session.users().searchForUserStream(realm, searchParams, 0, 10);
        List<String> usernames = users.map(UserModel::getUsername).toList();

        Assertions.assertFalse(usernames.contains("lazy-fail-user1"),
                "The user returned by the failing provider before it failed must not surface, but was found: " + usernames);
        Assertions.assertFalse(usernames.contains("lazy-fail-user2"),
                "The user returned by the failing provider before it failed must not surface, but was found: " + usernames);

        // users of the working (local) providers must still be returned
        assertThat("Should still contain the local users", usernames,
                hasItems("lazy-local-user1", "lazy-local-user2"));
    }
}
