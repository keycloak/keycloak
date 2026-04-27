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
import java.util.stream.Collectors;

import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.delegate.ClientModelLazyDelegate;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@RequireProvider(EventStoreProvider.class)
public class AdminEventQueryTest extends KeycloakModelTest {

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
        EventStoreProvider eventStore = s.getProvider(EventStoreProvider.class);
        eventStore.clearAdmin(s.realms().getRealm(realmId));
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testQuery() {
        withRealm(realmId, (session, realm) -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            eventStore.onEvent(createClientEvent(realm, session, OperationType.CREATE), false);
            eventStore.onEvent(createClientEvent(realm, session, OperationType.UPDATE), false);
            eventStore.onEvent(createClientEvent(realm, session, OperationType.DELETE), false);
            eventStore.onEvent(createClientEvent(realm, session, OperationType.CREATE), false);
        return null;
        });

        withRealm(realmId, (session, realm) -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            assertThat(eventStore.createAdminQuery()
                    .realm(realmId)
                    .firstResult(2)
                    .getResultStream()
                    .collect(Collectors.counting()),
                    is(2L));
            return null;
        });
    }

    @Test
    public void testQueryOrder() {
        withRealm(realmId, (session, realm) -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            AdminEvent firstEvent = createClientEvent(realm, session, OperationType.CREATE);
            firstEvent.setTime(1L);
            AdminEvent secondEvent = createClientEvent(realm, session, OperationType.DELETE);
            secondEvent.setTime(2L);
            eventStore.onEvent(firstEvent, false);
            eventStore.onEvent(secondEvent, false);
            List<AdminEvent> adminEventsAsc = eventStore.createAdminQuery()
                    .realm(realmId)
                    .orderByAscTime()
                    .getResultStream()
                    .collect(Collectors.toList());
            assertThat(adminEventsAsc.size(), is(2));
            assertThat(adminEventsAsc.get(0).getOperationType(), is(OperationType.CREATE));
            assertThat(adminEventsAsc.get(1).getOperationType(), is(OperationType.DELETE));

            List<AdminEvent> adminEventsDesc = eventStore.createAdminQuery()
                    .realm(realmId)
                    .orderByDescTime()
                    .getResultStream()
                    .collect(Collectors.toList());
            assertThat(adminEventsDesc.size(), is(2));
            assertThat(adminEventsDesc.get(0).getOperationType(), is(OperationType.DELETE));
            assertThat(adminEventsDesc.get(1).getOperationType(), is(OperationType.CREATE));
            return null;
        });
    }

        @Test
    public void testAdminEventRepresentationLongValue() {
        String longValue = RandomStringUtils.random(30000, true, true);

        withRealm(realmId, (session, realm) -> {

            AdminEvent event = createClientEvent(realm, session, OperationType.CREATE);
            event.setRepresentation(longValue);

            session.getProvider(EventStoreProvider.class).onEvent(event, true);

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            List<AdminEvent> events = session.getProvider(EventStoreProvider.class).createAdminQuery().realm(realmId).getResultStream().collect(Collectors.toList());
            assertThat(events, hasSize(1));

            assertThat(events.get(0).getRepresentation(), equalTo(longValue));

            return null;
        });
    }

    private AdminEvent createClientEvent(RealmModel realm, KeycloakSession session, OperationType operation) {
        return new AdminEventBuilder(realm, new DummyAuth(realm), session, DummyClientConnection.DUMMY_CONNECTION)
                .resource(ResourceType.CLIENT).operation(operation).getEvent();
    }

    private static class DummyClientConnection implements ClientConnection {

        private static final AdminEventQueryTest.DummyClientConnection DUMMY_CONNECTION =
                new AdminEventQueryTest.DummyClientConnection();

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

    private static class DummyAuth extends AdminAuth {

        private final RealmModel realm;

        public DummyAuth(RealmModel realm) {
            super(realm, null, null, null);
            this.realm = realm;
        }

        @Override
        public RealmModel getRealm() {
            return realm;
        }

        @Override
        public ClientModel getClient() {
            return new ClientModelLazyDelegate.WithId("dummy-client", null);
        }

        @Override
        public UserModel getUser() {
            return new UserModelDelegate(null) {
                @Override
                public String getId() {
                    return "dummy-user";
                }
            };
        }
    }

}
