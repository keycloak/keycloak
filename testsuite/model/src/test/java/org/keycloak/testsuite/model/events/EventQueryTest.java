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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.common.ClientConnection;
import org.keycloak.events.Event;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.apache.commons.lang3.RandomStringUtils;
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
public class EventQueryTest extends KeycloakModelTest {

    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "realm");
        s.getContext().setRealm(realm);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        this.realmId = realm.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testClear() {
        inRolledBackTransaction(null, (session, t) -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            eventStore.clear();
        });
    }

    private Event createAuthEventForUser(KeycloakSession session, RealmModel realm, String user) {
        return new EventBuilder(realm, session, DummyClientConnection.DUMMY_CONNECTION)
                .event(EventType.LOGIN)
                .user(user)
                .getEvent();
    }

    @Test
    public void testQuery() {
        withRealm(realmId, (session, realm) -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

            eventStore.onEvent(createAuthEventForUser(session, realm, "u1"));
            eventStore.onEvent(createAuthEventForUser(session, realm, "u2"));
            eventStore.onEvent(createAuthEventForUser(session, realm, "u3"));
            eventStore.onEvent(createAuthEventForUser(session, realm, "u4"));

            return realm.getId();
        });

        withRealm(realmId, (session, realm) -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            assertThat(eventStore.createQuery()
                            .realm(realmId)
                            .firstResult(2)
                            .getResultStream()
                            .collect(Collectors.counting()),
                    is(2L)
            );

            return null;
        });
    }

    @Test
    public void testQueryOrder() {
        withRealm(realmId, (session, realm) -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

            Event firstEvent = createAuthEventForUser(session, realm, "u1");
            firstEvent.setTime(1L);
            Event secondEvent = createAuthEventForUser(session, realm, "u2");
            secondEvent.setTime(2L);
            eventStore.onEvent(firstEvent);
            eventStore.onEvent(secondEvent);

            return realm.getId();
        });

        withRealm(realmId, (session, realm) -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            List<Event> eventsAsc = eventStore.createQuery()
                    .realm(realmId)
                    .orderByAscTime()
                    .getResultStream()
                    .collect(Collectors.toList());
            assertThat(eventsAsc.size(), is(2));
            assertThat(eventsAsc.get(0).getUserId(), is("u1"));
            assertThat(eventsAsc.get(1).getUserId(), is("u2"));

            List<Event> eventsDesc = eventStore.createQuery()
                    .realm(realmId)
                    .orderByDescTime()
                    .getResultStream()
                    .collect(Collectors.toList());
            assertThat(eventsDesc.size(), is(2));
            assertThat(eventsDesc.get(0).getUserId(), is("u2"));
            assertThat(eventsDesc.get(1).getUserId(), is("u1"));

            return null;
        });
    }

    @Test
    public void testEventDetailsLongValue() {
        String v1 = RandomStringUtils.random(1000, true, true);
        String v2 = RandomStringUtils.random(1000, true, true);
        String v3 = RandomStringUtils.random(1000, true, true);
        String v4 = RandomStringUtils.random(1000, true, true);

        withRealm(realmId, (session, realm) -> {

            Map<String, String> details = Map.of("k1", v1, "k2", v2, "k3", v3, "k4", v4);

            Event event = createAuthEventForUser(session, realm, "u1");
            event.setDetails(details);

            session.getProvider(EventStoreProvider.class).onEvent(event);

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            List<Event> events = session.getProvider(EventStoreProvider.class).createQuery().realm(realmId).getResultStream().collect(Collectors.toList());
            assertThat(events, hasSize(1));
            Map<String, String> details = events.get(0).getDetails();

            assertThat(details.get("k1"), equalTo(v1));
            assertThat(details.get("k2"), equalTo(v2));
            assertThat(details.get("k3"), equalTo(v3));
            assertThat(details.get("k4"), equalTo(v4));
            return null;
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
