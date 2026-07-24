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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.sessions.infinispan.changes.sessions.PersisterLastSessionRefreshStoreFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;
import org.keycloak.testsuite.model.infinispan.InfinispanTestUtil;
import org.keycloak.timer.TimerProvider;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import static org.keycloak.testsuite.model.session.UserSessionPersisterProviderTest.createClients;
import static org.keycloak.testsuite.model.session.UserSessionPersisterProviderTest.createSessions;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        RealmModel realm = createRealm(s, "test");
        s.getContext().setRealm(realm);
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
        s.getContext().setRealm(realm);
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testMultipleSessionsRemovalInOneTransaction() {
        UserSessionModel[] origSessions = inComittedTransaction(session -> { return createSessions(session, realmId); });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);

            UserSessionModel userSession = session.sessions().getUserSession(realm, origSessions[0].getId());
            Assert.assertEquals(origSessions[0], userSession);

            userSession = session.sessions().getUserSession(realm, origSessions[1].getId());
            Assert.assertEquals(origSessions[1], userSession);
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);

            session.sessions().removeUserSession(realm, session.sessions().getUserSession(realm, origSessions[0].getId()));
            session.sessions().removeUserSession(realm, session.sessions().getUserSession(realm, origSessions[1].getId()));
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);

            UserSessionModel userSession = session.sessions().getUserSession(realm, origSessions[0].getId());
            Assert.assertNull(userSession);

            userSession = session.sessions().getUserSession(realm, origSessions[1].getId());
            Assert.assertNull(userSession);
        });
    }

    @Test
    public void testExpiredClientSessions() {
        // Suspend periodic tasks to avoid race-conditions, which may cause missing updates of lastSessionRefresh times to UserSessionPersisterProvider
        //  skip for persistent user sessions as the periodic task is not used there
        TimerProvider timer = kcSession.getProvider(TimerProvider.class);
        TimerProvider.TimerTaskContext timerTaskCtx = null;
        if (timer != null && !MultiSiteUtils.isPersistentSessionsEnabled()) {
            timerTaskCtx = timer.cancelTask(PersisterLastSessionRefreshStoreFactory.DB_LSR_PERIODIC_TASK_NAME);
            log.info("Cancelled periodic task " + PersisterLastSessionRefreshStoreFactory.DB_LSR_PERIODIC_TASK_NAME);
        }

        InfinispanTestUtil.setTestingTimeService(kcSession);

        try {
            UserSessionModel[] origSessions = inComittedTransaction(session -> {
                // create some user and client sessions
                return createSessions(session, realmId);
            });

            inComittedTransaction(session -> {
                RealmModel realm = session.realms().getRealm(realmId);
                session.getContext().setRealm(realm);

                UserSessionModel userSession = session.sessions().getUserSession(realm, origSessions[0].getId());
                Assert.assertEquals(origSessions[0], userSession);

                AuthenticatedClientSessionModel clientSession = session.sessions().getClientSession(userSession, realm.getClientByClientId("test-app"),
                        false);
                Assert.assertEquals(origSessions[0].getAuthenticatedClientSessionByClient(realm.getClientByClientId("test-app").getId()).getId(), clientSession.getId());

                userSession = session.sessions().getUserSession(realm, origSessions[1].getId());
                Assert.assertEquals(origSessions[1], userSession);
            });

            inComittedTransaction(session -> {
                setTimeOffset(1000);
            });

            inComittedTransaction(session -> {
                RealmModel realm = session.realms().getRealm(realmId);
                session.getContext().setRealm(realm);

                // assert the user session is still there
                UserSessionModel userSession = session.sessions().getUserSession(realm, origSessions[0].getId());
                Assert.assertEquals(origSessions[0], userSession);

                // assert the client sessions are expired
                Assert.assertNull(session.sessions().getClientSession(userSession, realm.getClientByClientId("test-app"), false));
                Assert.assertNull(session.sessions().getClientSession(userSession, realm.getClientByClientId("third-party"), false));
            });
        } finally {
            setTimeOffset(0);
            kcSession.getKeycloakSessionFactory().publish(new ResetTimeOffsetEvent());
            // Enable periodic task again, skip for persistent user sessions as the periodic task is not used there
            if (timer != null && timerTaskCtx != null && !MultiSiteUtils.isPersistentSessionsEnabled()) {
                timer.schedule(timerTaskCtx.getRunnable(), timerTaskCtx.getIntervalMillis(), PersisterLastSessionRefreshStoreFactory.DB_LSR_PERIODIC_TASK_NAME);
            }

            InfinispanTestUtil.revertTimeService(kcSession);
        }
    }

    @Test
    public void testTransientUserSessionIsNotPersisted() {
        String id = inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            UserSessionModel userSession = session.sessions().createUserSession(KeycloakModelUtils.generateId(), realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", false, null, null, UserSessionModel.SessionPersistenceState.TRANSIENT);

            ClientModel testApp = realm.getClientByClientId("test-app");
            session.sessions().createClientSession(realm, testApp, userSession);

            // assert the client sessions are present
            assertThat(session.sessions().getClientSession(userSession, testApp, false), notNullValue());
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
    public void testClientSessionIsNotPersistedForTransientUserSession() {
        UserSessionModel userSession = inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            UserSessionModel us = session.sessions().createUserSession(null, realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", false, null, null, UserSessionModel.SessionPersistenceState.TRANSIENT);
            ClientModel testApp = realm.getClientByClientId("test-app");
            session.sessions().createClientSession(realm, testApp, us);

            // assert the client sessions are present
            assertThat(session.sessions().getClientSession(us, testApp, false), notNullValue());
            return us;
        });
        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            ClientModel testApp = realm.getClientByClientId("test-app");
            // in new transaction transient session should not be present
            assertThat(session.sessions().getClientSession(userSession, testApp, false), nullValue());
        });
    }

    @Test
    public void testCreateUserSessionsParallel() throws InterruptedException {
        Set<String> userSessionIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        CountDownLatch latch = new CountDownLatch(4);

        inIndependentFactories(4, 30, () -> {
            withRealm(realmId, (session, realm) -> {
                UserModel user = session.users().getUserByUsername(realm, "user1");
                UserSessionModel userSession = session.sessions().createUserSession(null, realm, user, "user1", "", "", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                userSessionIds.add(userSession.getId());

                latch.countDown();

                return null;
            });

            // wait for other nodes to finish
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            assertThat(userSessionIds, Matchers.iterableWithSize(4));

            // wait a bit to allow replication
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            withRealm(realmId, (session, realm) -> {
                userSessionIds.forEach(id -> Assert.assertNotNull(session.sessions().getUserSession(realm, id)));

                return null;
            });
        });
    }

    @Test
    public void testStreamsMarshalling() throws InterruptedException {
        Assume.assumeTrue(InfinispanUtils.isEmbeddedInfinispan());
        closeKeycloakSessionFactory();
        var clusterSize = 4;
        var barrier = new CyclicBarrier(clusterSize);

        inIndependentFactories(clusterSize, 30, () -> {
            // populate the cache
            withRealmConsumer(realmId, (keycloakSession, realm) -> {
                var user = keycloakSession.users().getUserByUsername(realm, "user1");
                var client = realm.getClientByClientId("test-app");
                assertNotNull(user);
                assertNotNull(client);
                var userSession = keycloakSession.sessions().createUserSession(null, realm, user,  "user1", "127.0.0.1", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                assertNotNull(userSession);
                var clientSession = keycloakSession.sessions().createClientSession(realm, client, userSession);
                assertNotNull(clientSession);
            });

            try {
                barrier.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (BrokenBarrierException | TimeoutException e) {
                throw new RuntimeException(e);
            }

            withRealmConsumer(realmId, (keycloakSession, realm) -> {
                var user = keycloakSession.users().getUserByUsername(realm, "user1");
                assertNotNull(user);

                var client = realm.getClientByClientId("test-app");
                assertNotNull(client);

                var activeClientSessionsStats = keycloakSession.sessions().getActiveClientSessionStats(realm, false);
                assertNotNull(activeClientSessionsStats);
                assertEquals(1, activeClientSessionsStats.size());
                assertTrue(activeClientSessionsStats.containsKey(client.getId()));
                assertEquals(4L, (long) activeClientSessionsStats.get(client.getId()));

                var userSessions = keycloakSession.sessions().getUserSessionsStream(realm, user).toList();
                assertNotNull(userSessions);
                assertEquals(4, userSessions.size());

                // sync everybody here since we are going to remove everything.
                try {
                    barrier.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (BrokenBarrierException | TimeoutException e) {
                    throw new RuntimeException(e);
                }

                keycloakSession.sessions().removeUserSessions(realm, user);
            });
        });
    }
}
