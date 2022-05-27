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
package org.keycloak.testsuite.model.session;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.map.userSession.MapUserSessionProvider;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.InfinispanUserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.changes.sessions.PersisterLastSessionRefreshStoreFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;
import org.keycloak.testsuite.model.infinispan.InfinispanTestUtil;
import org.keycloak.timer.TimerProvider;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.model.session.UserSessionPersisterProviderTest.createClients;
import static org.keycloak.testsuite.model.session.UserSessionPersisterProviderTest.createSessions;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
@RequireProvider(UserSessionProvider.class)
@RequireProvider(UserProvider.class)
@RequireProvider(RealmProvider.class)
public class UserSessionProviderModelTest extends KeycloakModelTest {

    private String realmId;
    private KeycloakSession kcSession;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().createRealm("test");
        realm.setOfflineSessionIdleTimeout(Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        realm.setSsoSessionIdleTimeout(1800);
        realm.setSsoSessionMaxLifespan(36000);
        realm.setClientSessionIdleTimeout(500);
        this.realmId = realm.getId();
        this.kcSession = s;

        s.users().addUser(realm, "user1").setEmail("user1@localhost");
        s.users().addUser(realm, "user2").setEmail("user2@localhost");

        createClients(s, realm);
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.sessions().removeUserSessions(realm);

        UserModel user1 = s.users().getUserByUsername(realm, "user1");
        UserModel user2 = s.users().getUserByUsername(realm, "user2");

        UserManager um = new UserManager(s);
        if (user1 != null) {
            um.removeUser(realm, user1);
        }
        if (user2 != null) {
            um.removeUser(realm, user2);
        }

        s.realms().removeRealm(realmId);
    }

    @Test
    public void testMultipleSessionsRemovalInOneTransaction() {
        UserSessionModel[] origSessions = inComittedTransaction(session -> { return createSessions(session, realmId); });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);

            UserSessionModel userSession = session.sessions().getUserSession(realm, origSessions[0].getId());
            Assert.assertEquals(origSessions[0], userSession);

            userSession = session.sessions().getUserSession(realm, origSessions[1].getId());
            Assert.assertEquals(origSessions[1], userSession);
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);

            session.sessions().removeUserSession(realm, origSessions[0]);
            session.sessions().removeUserSession(realm, origSessions[1]);
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);

            UserSessionModel userSession = session.sessions().getUserSession(realm, origSessions[0].getId());
            Assert.assertNull(userSession);

            userSession = session.sessions().getUserSession(realm, origSessions[1].getId());
            Assert.assertNull(userSession);
        });
    }

    @Test
    public void testExpiredClientSessions() {
        // Suspend periodic tasks to avoid race-conditions, which may cause missing updates of lastSessionRefresh times to UserSessionPersisterProvider
        TimerProvider timer = kcSession.getProvider(TimerProvider.class);
        TimerProvider.TimerTaskContext timerTaskCtx = null;
        if (timer != null) {
            timerTaskCtx = timer.cancelTask(PersisterLastSessionRefreshStoreFactory.DB_LSR_PERIODIC_TASK_NAME);
            log.info("Cancelled periodic task " + PersisterLastSessionRefreshStoreFactory.DB_LSR_PERIODIC_TASK_NAME);

            InfinispanTestUtil.setTestingTimeService(kcSession);
        }

        try {
            UserSessionModel[] origSessions = inComittedTransaction(session -> {
                // create some user and client sessions
                return createSessions(session, realmId);
            });

            AtomicReference<List<String>> clientSessionIds = new AtomicReference<>();
            clientSessionIds.set(origSessions[0].getAuthenticatedClientSessions().values().stream().map(AuthenticatedClientSessionModel::getId).collect(Collectors.toList()));

            inComittedTransaction(session -> {
                RealmModel realm = session.realms().getRealm(realmId);

                UserSessionModel userSession = session.sessions().getUserSession(realm, origSessions[0].getId());
                Assert.assertEquals(origSessions[0], userSession);

                AuthenticatedClientSessionModel clientSession = session.sessions().getClientSession(userSession, realm.getClientByClientId("test-app"),
                        origSessions[0].getAuthenticatedClientSessionByClient(realm.getClientByClientId("test-app").getId()).getId(),
                        false);
                Assert.assertEquals(origSessions[0].getAuthenticatedClientSessionByClient(realm.getClientByClientId("test-app").getId()).getId(), clientSession.getId());

                userSession = session.sessions().getUserSession(realm, origSessions[1].getId());
                Assert.assertEquals(origSessions[1], userSession);
            });


            // not possible to expire client session without expiring user sessions with time offset in map storage because
            // expiration in map storage takes min of (clientSessionIdleExpiration, ssoSessionIdleTimeout)
            inComittedTransaction(session -> {
                if (session.getProvider(UserSessionProvider.class) instanceof MapUserSessionProvider) {
                    RealmModel realm = session.realms().getRealm(realmId);

                    UserSessionModel userSession = session.sessions().getUserSession(realm, origSessions[0].getId());

                    userSession.getAuthenticatedClientSessions().values().stream().forEach(clientSession -> {
                        // expire client sessions
                        clientSession.setTimestamp(1);
                    });
                } else {
                    Time.setOffset(1000);
                }
            });

            inComittedTransaction(session -> {
                RealmModel realm = session.realms().getRealm(realmId);

                // assert the user session is still there
                UserSessionModel userSession = session.sessions().getUserSession(realm, origSessions[0].getId());
                Assert.assertEquals(origSessions[0], userSession);

                // assert the client sessions are expired
                clientSessionIds.get().forEach(clientSessionId -> {
                    Assert.assertNull(session.sessions().getClientSession(userSession, realm.getClientByClientId("test-app"), clientSessionId, false));
                    Assert.assertNull(session.sessions().getClientSession(userSession, realm.getClientByClientId("third-party"), clientSessionId, false));
                });
            });
        } finally {
            Time.setOffset(0);
            kcSession.getKeycloakSessionFactory().publish(new ResetTimeOffsetEvent());
            if (timer != null && timerTaskCtx != null) {
                timer.schedule(timerTaskCtx.getRunnable(), timerTaskCtx.getIntervalMillis(), PersisterLastSessionRefreshStoreFactory.DB_LSR_PERIODIC_TASK_NAME);

                InfinispanTestUtil.revertTimeService();
            }
        }
    }

    @Test
    public void testTransientUserSessionIsNotPersisted() {
        String id = inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            UserSessionModel userSession = session.sessions().createUserSession(KeycloakModelUtils.generateId(), realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", false, null, null, UserSessionModel.SessionPersistenceState.TRANSIENT);

            ClientModel testApp = realm.getClientByClientId("test-app");
            AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, testApp, userSession);
            
            // assert the client sessions are present
            assertThat(session.sessions().getClientSession(userSession, testApp, clientSession.getId(), false), notNullValue());
            return userSession.getId();
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            UserSessionModel userSession = session.sessions().getUserSession(realm, id);

            // in new transaction transient session should not be present
            assertThat(userSession, nullValue());
        });
    }

    @Test
    @RequireProvider(value = UserSessionProvider.class, only = InfinispanUserSessionProviderFactory.PROVIDER_ID)
    public void testClientSessionIsNotPersistedForTransientUserSession() {
        Object[] transientUserSessionWithClientSessionId = inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            UserSessionModel userSession = session.sessions().createUserSession(null, realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", false, null, null, UserSessionModel.SessionPersistenceState.TRANSIENT);
            ClientModel testApp = realm.getClientByClientId("test-app");
            AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, testApp, userSession);

            // assert the client sessions are present
            assertThat(session.sessions().getClientSession(userSession, testApp, clientSession.getId(), false), notNullValue());
            Object[] result = new Object[2];
            result[0] = userSession;
            result[1] = clientSession.getId();
            return result;
        });
        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            ClientModel testApp = realm.getClientByClientId("test-app");
            UserSessionModel userSession = (UserSessionModel) transientUserSessionWithClientSessionId[0];
            String clientSessionId = (String) transientUserSessionWithClientSessionId[1];
            // in new transaction transient session should not be present
            assertThat(session.sessions().getClientSession(userSession, testApp, clientSessionId, false), nullValue());
        });
    }
}
