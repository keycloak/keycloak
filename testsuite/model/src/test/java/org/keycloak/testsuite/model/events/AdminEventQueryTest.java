/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.model.events;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.events.Event;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;
import org.keycloak.models.RealmModel;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 *
 * @author hmlnarik
 */
@RequireProvider(EventStoreProvider.class)
public class AdminEventQueryTest extends KeycloakModelTest {

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
    public void testClear() {
        inRolledBackTransaction(null, (session, t) -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            eventStore.clear();
        });
    }

    private Event createAuthEventForUser(RealmModel realm, String user) {
        return new EventBuilder(realm, null, DummyClientConnection.DUMMY_CONNECTION)
                .event(EventType.LOGIN)
                .user(user)
                .getEvent();
    }

    @Test
    public void testQuery() {
        withRealm(realmId, (session, realm) -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            eventStore.onEvent(createAuthEventForUser(realm,"u1"));
            eventStore.onEvent(createAuthEventForUser(realm,"u2"));
            eventStore.onEvent(createAuthEventForUser(realm,"u3"));
            eventStore.onEvent(createAuthEventForUser(realm,"u4"));

            return realm.getId();
        });

        withRealm(realmId, (session, realm) -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            assertThat(eventStore.createQuery()
                            .firstResult(2)
                            .getResultStream()
                            .collect(Collectors.counting()),
                    is(2L)
            );

            return null;
        });
    }

    @Test
    @RequireProvider(value = EventStoreProvider.class, only = "map")
    public void testEventExpiration() {
        withRealm(realmId, (session, realm) -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

            // Set expiration so no event is valid
            realm.setEventsExpiration(5);
            Event e = createAuthEventForUser(realm, "u1");
            eventStore.onEvent(e);

            // Set expiration to 1000 seconds
            realm.setEventsExpiration(1000);
            e = createAuthEventForUser(realm, "u2");
            eventStore.onEvent(e);

            return null;
        });

        Time.setOffset(10);

        try {
            withRealm(realmId, (session, realm) -> {
                EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

                Set<Event> events = eventStore.createQuery()
                        .getResultStream().collect(Collectors.toSet());

                assertThat(events, hasSize(1));
                assertThat(events.iterator().next().getUserId(), equalTo("u2"));
                return null;
            });
        } finally {
            Time.setOffset(0);
        }


    }

    @Test
    @RequireProvider(value = EventStoreProvider.class, only = "map")
    public void testEventsClearedOnRealmRemoval() {
        // Create another realm
        String newRealmId = inComittedTransaction(null, (session, t) -> {
            RealmModel realm = session.realms().createRealm("events-realm");
            realm.setDefaultRole(session.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));

            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            Event e = createAuthEventForUser(realm, "u1");
            eventStore.onEvent(e);

            AdminEvent ae = new AdminEvent();
            ae.setRealmId(realm.getId());
            eventStore.onEvent(ae, false);

            return realm.getId();
        });

        // Check if events were created
        inComittedTransaction(session -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            assertThat(eventStore.createQuery().getResultStream().count(), is(1L));
            assertThat(eventStore.createAdminQuery().getResultStream().count(), is(1L));
        });

        // Remove realm
        inComittedTransaction((Consumer<KeycloakSession>) session -> session.realms().removeRealm(newRealmId));

        // Check events were removed
        inComittedTransaction(session -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            assertThat(eventStore.createQuery().getResultStream().count(), is(0L));
            assertThat(eventStore.createAdminQuery().getResultStream().count(), is(0L));
        });
    }

    private static class DummyClientConnection implements ClientConnection {

        private static DummyClientConnection DUMMY_CONNECTION = new DummyClientConnection();

        @Override
        public String getRemoteAddr() {
            return "remoteAddr";
        }

        @Override
        public String getRemoteHost() {
            return "remoteHost";
        }

        @Override
        public int getRemotePort() {
            return -1;
        }

        @Override
        public String getLocalAddr() {
            return "localAddr";
        }

        @Override
        public int getLocalPort() {
            return -2;
        }
    }

}
