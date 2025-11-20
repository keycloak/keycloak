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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.keycloak.common.Profile;
import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
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
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.changes.sessions.PersisterLastSessionRefreshStoreFactory;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;
import org.keycloak.testsuite.model.infinispan.InfinispanTestUtil;
import org.keycloak.timer.TimerProvider;

import org.hamcrest.Matchers;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeFalse;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
@RequireProvider(UserSessionPersisterProvider.class)
@RequireProvider(UserSessionProvider.class)
@RequireProvider(UserProvider.class)
@RequireProvider(RealmProvider.class)
public class UserSessionProviderOfflineModelTest extends KeycloakModelTest {

    private String realmId;
    private KeycloakSession kcSession;

    private UserSessionManager sessionManager;
    private UserSessionPersisterProvider persister;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "test");
        s.getContext().setRealm(realm);
        realm.setOfflineSessionIdleTimeout(Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        realm.setOfflineSessionMaxLifespanEnabled(true);
        realm.setClientOfflineSessionIdleTimeout(999999999);
        realm.setClientOfflineSessionMaxLifespan(999999999);
        this.realmId = realm.getId();
        this.kcSession = s;

        s.users().addUser(realm, "user1").setEmail("user1@localhost");
        s.users().addUser(realm, "user2").setEmail("user2@localhost");

        UserSessionPersisterProviderTest.createClients(s, realm);
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
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
    public void testExpired() {
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
            // Key is userSessionId, value is set of client UUIDS
            Map<String, Set<String>> offlineSessions = new HashMap<>();
            ClientModel[] testApp = new ClientModel[1];

            UserSessionModel[] origSessions = inComittedTransaction(session -> {
                // Create some online sessions in infinispan
                return UserSessionPersisterProviderTest.createSessions(session, realmId);
            });

            inComittedTransaction(session -> {
                RealmModel realm = session.realms().getRealm(realmId);
                session.getContext().setRealm(realm);
                sessionManager = new UserSessionManager(session);
                persister = session.getProvider(UserSessionPersisterProvider.class);

                // Persist 3 created userSessions and clientSessions as offline
                testApp[0] = realm.getClientByClientId("test-app");
                session.sessions().getUserSessionsStream(realm, testApp[0]).collect(Collectors.toList())
                        .forEach(userSession -> offlineSessions.put(userSession.getId(), createOfflineSessionIncludeClientSessions(session, userSession)));

                // Assert all previously saved offline sessions found
                for (Map.Entry<String, Set<String>> entry : offlineSessions.entrySet()) {
                    UserSessionModel foundSession = sessionManager.findOfflineUserSession(realm, entry.getKey());
                    Assert.assertEquals(foundSession.getAuthenticatedClientSessions().keySet(), entry.getValue());
                }
            });

            log.info("Persisted 3 sessions to UserSessionPersisterProvider");

            inComittedTransaction(session -> {
                RealmModel realm = session.realms().getRealm(realmId);
                session.getContext().setRealm(realm);
                persister = session.getProvider(UserSessionPersisterProvider.class);

                UserSessionModel session0 = session.sessions().getOfflineUserSession(realm, origSessions[0].getId());
                Assert.assertNotNull(session0);

                // skip for remote cache feature
                if (InfinispanUtils.isEmbeddedInfinispan()) {
                    // sessions are in persister too
                    Assert.assertEquals(3, persister.getUserSessionsCount(true));
                }

                setTimeOffset(300);
                log.infof("Set time offset to 300. Time is: %d", Time.currentTime());

                // Set lastSessionRefresh to currentSession[0] to 0
                session0.setLastSessionRefresh(Time.currentTime());
            });


            // Increase timeOffset and update LSR of the session two times - first to 20 days and then to 21 days. At least one of updates
            // will propagate to PersisterLastSessionRefreshStore and update DB (Single update is not 100% sure as there is still a
            // chance of delayed periodic task to be run in the meantime and causing race-condition, which would mean LSR not updated in the DB)
            IntStream.range(0, 2).sequential().forEach(index -> inComittedTransaction(index, (session, i) -> {
                int timeOffset = 1728000 + (i * 86400);

                RealmModel realm = session.realms().getRealm(realmId);
                session.getContext().setRealm(realm);
                setTimeOffset(timeOffset);
                log.infof("Set time offset to %d. Time is: %d", timeOffset, Time.currentTime());

                UserSessionModel session0 = session.sessions().getOfflineUserSession(realm, origSessions[0].getId());
                session0.setLastSessionRefresh(Time.currentTime());

                return null;
            }));

            inComittedTransaction(session -> {
                RealmModel realm = session.realms().getRealm(realmId);
                persister = session.getProvider(UserSessionPersisterProvider.class);

                // Increase timeOffset - 40 days
                setTimeOffset(3456000);
                log.infof("Set time offset to 3456000. Time is: %d", Time.currentTime());

                // Expire and ensure that all sessions despite session0 were removed
                persister.removeExpired(realm);
            });

            inComittedTransaction(session -> {
                RealmModel realm = session.realms().getRealm(realmId);
                session.getContext().setRealm(realm);
                persister = session.getProvider(UserSessionPersisterProvider.class);

                // assert session0 is the only session found
                Assert.assertNotNull(session.sessions().getOfflineUserSession(realm, origSessions[0].getId()));
                Assert.assertNull(session.sessions().getOfflineUserSession(realm, origSessions[1].getId()));
                Assert.assertNull(session.sessions().getOfflineUserSession(realm, origSessions[2].getId()));

                if (InfinispanUtils.isEmbeddedInfinispan()) {
                    Assert.assertEquals(1, persister.getUserSessionsCount(true));
                }

                // Expire everything and assert nothing found
                setTimeOffset(7000000);

                persister.removeExpired(realm);
            });

            inComittedTransaction(session -> {
                RealmModel realm = session.realms().getRealm(realmId);
                sessionManager = new UserSessionManager(session);
                persister = session.getProvider(UserSessionPersisterProvider.class);

                for (String userSessionId : offlineSessions.keySet()) {
                    Assert.assertNull(sessionManager.findOfflineUserSession(realm, userSessionId));
                }
                Assert.assertEquals(0, persister.getUserSessionsCount(true));
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
    public void testLoadUserSessionsWithNotDeletedOfflineClientSessions() {
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
                // Create some online sessions in infinispan
                return UserSessionPersisterProviderTest.createSessions(session, realmId);
            });

            inComittedTransaction(session -> {
                RealmModel realm = session.realms().getRealm(realmId);
                session.getContext().setRealm(realm);
                sessionManager = new UserSessionManager(session);
                persister = session.getProvider(UserSessionPersisterProvider.class);

                session.sessions().getUserSessionsStream(realm, realm.getClientByClientId("test-app")).collect(Collectors.toList())
                        .forEach(userSession -> createOfflineSessionIncludeClientSessions(session, userSession));
            });

            log.info("Persisted 3 sessions to UserSessionPersisterProvider");

            if (InfinispanUtils.isEmbeddedInfinispan()) {
                // external Infinispan does not store data in UserSessionPersisterProvider
                inComittedTransaction(session -> {
                    persister = session.getProvider(UserSessionPersisterProvider.class);

                    Assert.assertEquals(3, persister.getUserSessionsCount(true));
                });
            }

            inComittedTransaction(session -> {
                RealmModel realm = session.realms().getRealm(realmId);
                session.getContext().setRealm(realm);
                persister = session.getProvider(UserSessionPersisterProvider.class);

                // Expire everything except offline client sessions
                setTimeOffset(7000000);

                persister.removeExpired(realm);
            });

            inComittedTransaction(session -> {
                RealmModel realm = session.realms().getRealm(realmId);
                session.getContext().setRealm(realm);
                sessionManager = new UserSessionManager(session);
                persister = session.getProvider(UserSessionPersisterProvider.class);

                Assert.assertEquals(0, persister.getUserSessionsCount(true));

                // create two offline user sessions
                UserSessionModel userSession = session.sessions().createUserSession(null, realm, session.users().getUserByUsername(realm, "user1"), "user1", "ip1", null, false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                session.sessions().createOfflineUserSession(userSession);
                session.sessions().createOfflineUserSession(origSessions[0]);

                if (!MultiSiteUtils.isPersistentSessionsEnabled() && InfinispanUtils.isEmbeddedInfinispan()) {
                    // This does not work with persistent user sessions because we currently have two transactions and the one that creates the offline user sessions is not committing the changes
                    // try to load user session from persister
                    Assert.assertEquals(2, persister.loadUserSessionsStream(0, 10, true, "").count());
                }
            });

            if (MultiSiteUtils.isPersistentSessionsEnabled() && InfinispanUtils.isEmbeddedInfinispan()) {
                inComittedTransaction(session -> {
                    persister = session.getProvider(UserSessionPersisterProvider.class);
                    Assert.assertEquals(2, persister.loadUserSessionsStream(0, 10, true, "").count());
                });
            }

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
    public void testOfflineSessionLazyLoading() throws InterruptedException {
        // as one thread fills this list and the others read it, ensure that it is synchronized to avoid side effects
        List<UserSessionModel> offlineUserSessions = Collections.synchronizedList(new LinkedList<>());
        List<AuthenticatedClientSessionModel> offlineClientSessions = Collections.synchronizedList(new LinkedList<>());
        createOfflineSessions("user1", 10, offlineUserSessions, offlineClientSessions);

        closeKeycloakSessionFactory();

        inIndependentFactories(4, 60, () -> {
            withRealm(realmId, (session, realm) -> {
                final UserModel user = session.users().getUserByUsername(realm, "user1");
                Assert.assertTrue(assertOfflineSession(offlineUserSessions, session.sessions().getOfflineUserSessionsStream(realm, user).collect(Collectors.toList())));
                return null;
            });
        });

    }

    @Test
    public void testOfflineSessionLazyLoadingPropagationBetweenNodes() throws InterruptedException {
        // as one thread fills this list and the others read it, ensure that it is synchronized to avoid side effects
        List<UserSessionModel> offlineUserSessions = Collections.synchronizedList(new LinkedList<>());
        List<AuthenticatedClientSessionModel> offlineClientSessions = Collections.synchronizedList(new LinkedList<>());

        AtomicInteger index = new AtomicInteger();
        CountDownLatch afterFirstNodeLatch = new CountDownLatch(1);

        inIndependentFactories(4, 60, () -> {
            if (index.incrementAndGet() == 1) {
                createOfflineSessions("user1", 10, offlineUserSessions, offlineClientSessions);

                afterFirstNodeLatch.countDown();
            }
            awaitLatch(afterFirstNodeLatch);

            if (InfinispanUtils.isEmbeddedInfinispan()) {
                log.debug("Joining the cluster");
                inComittedTransaction(session -> {
                    InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
                    Cache<String, Object> cache = provider.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
                    while (!cache.getAdvancedCache().getDistributionManager().isJoinComplete()) {
                        sleep(1000);
                    }
                    cache.keySet().forEach(s -> {
                    });
                });
                log.debug("Cluster joined");
            }

            withRealm(realmId, (session, realm) -> {
                final UserModel user = session.users().getUserByUsername(realm, "user1");
                // it might take a moment to propagate, therefore loop
                while (!assertOfflineSession(offlineUserSessions, session.sessions().getOfflineUserSessionsStream(realm, user).collect(Collectors.toList()))) {
                    sleep(1000);
                }
                return null;
            });

        });

    }

    @Test
    public void testOfflineClientSessionLoading() {
        Assume.assumeTrue("Remote Infinispan feature does not store sessions in UserSessionPersisterProvider", InfinispanUtils.isEmbeddedInfinispan());
        // create online user and client sessions
        inComittedTransaction((Consumer<KeycloakSession>) session -> UserSessionPersisterProviderTest.createSessions(session, realmId));

        // create offline user and client sessions
        withRealm(realmId, (session, realm) -> {
            session.sessions().getUserSessionsStream(realm, realm.getClientByClientId("test-app")).collect(Collectors.toList())
                    .forEach(userSession -> createOfflineSessionIncludeClientSessions(session, userSession));
            return null;
        });

        List<String> offlineUserSessionIds =  withRealm(realmId, (session, realm) -> {
            UserModel user = session.users().getUserByUsername(realm, "user1");
            List<String> ids = session.sessions().getOfflineUserSessionsStream(realm, user).map(UserSessionModel::getId).collect(Collectors.toList());
            assertThat(ids, Matchers.hasSize(2));
            return ids;
        });

        withRealm(realmId, (session, realm) -> {
            // remove offline client sessions from the cache
            // this simulates the cases when offline client sessions are lost from the cache due to various reasons (a cache limit/expiration/preloading issue)
            session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME).clear();

            String clientUUID = realm.getClientByClientId("test-app").getId();

            offlineUserSessionIds.forEach(id -> {
                UserSessionModel offlineUserSession = session.sessions().getOfflineUserSession(realm, id);

                // each associated offline client session should be found by looking into persister
                Assert.assertNotNull(offlineUserSession.getAuthenticatedClientSessionByClient(clientUUID));
            });
            return null;
        });
    }

    @Test
    public void testLoadingOfflineClientSessionWhenCreatedBeforeSessionTime() {
        Assume.assumeTrue("Remote Infinispan feature does not store sessions in UserSessionPersisterProvider", InfinispanUtils.isEmbeddedInfinispan());
        // setup idle timeout for the realm
        int idleTimeout = (int) TimeUnit.DAYS.toSeconds(1);
        withRealm(realmId, (session, realmModel) -> {
            realmModel.setClientOfflineSessionIdleTimeout(idleTimeout);
            return null;
        });

        // create online user and client sessions
        inComittedTransaction((Consumer<KeycloakSession>) session -> UserSessionPersisterProviderTest.createSessions(session, realmId));

        // create offline user and client sessions
        List<String> offlineUserSessionIds = withRealm(realmId, (session, realm) -> session.sessions()
                .getUserSessionsStream(realm, realm.getClientByClientId("test-app"))
                .map(userSession -> {
                            UserSessionModel offlineUserSession = Optional.ofNullable(
                                    session.sessions().getOfflineUserSession(realm, userSession.getId())
                            ).orElseGet(() -> session.sessions().createOfflineUserSession(userSession));

                            userSession.getAuthenticatedClientSessions()
                                    .values()
                                    .forEach(clientSession -> {
                                        // set timestamp manually to make sure the client session is created before session time
                                        // this simulates the cases when the offline client sessions are created before the session time
                                        clientSession.setTimestamp(Time.currentTime() - idleTimeout * 2);

                                        session.sessions().createOfflineClientSession(clientSession, offlineUserSession);
                                    });

                            return offlineUserSession.getId();
                        }
                ).collect(Collectors.toList())
        );

        withRealm(realmId, (session, realm) -> {
            // remove offline client sessions from the cache
            // this simulates the cases when offline client sessions are lost from the cache due to various reasons (a cache limit/expiration/preloading issue)
            session.getProvider(InfinispanConnectionProvider.class)
                    .getCache(InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME).clear();

            String clientUUID = realm.getClientByClientId("test-app").getId();

            offlineUserSessionIds.forEach(id -> {
                UserSessionModel offlineUserSession = session.sessions().getOfflineUserSession(realm, id);

                // each associated offline client session should be found by looking into persister
                AuthenticatedClientSessionModel offlineClientSession = offlineUserSession.getAuthenticatedClientSessionByClient(clientUUID);
                Assert.assertNotNull(offlineClientSession);
                Assert.assertEquals(offlineUserSession.getLastSessionRefresh(), offlineClientSession.getTimestamp());
            });
            return null;
        });
    }

    @Test
    public void testOfflineSessionLifespanOverride() {
        // As offline session's timeout is not overriden when PERSISTENT_USER_SESSIONS is enabled
        Assume.assumeFalse(MultiSiteUtils.isPersistentSessionsEnabled());
        assumeFalse("Clusterless Feature enabled", Profile.isFeatureEnabled(Profile.Feature.CLUSTERLESS));

        createOfflineSessions("user1", 2, new LinkedList<>(), new LinkedList<>());

        reinitializeKeycloakSessionFactory();

        withRealm(realmId, (session, realm) -> {
            InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);

            // skip remote cache load as we are only interested in embedded caches
            AdvancedCache offlineUSCache = provider.getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME).getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD);
            AdvancedCache offlineCSCache = provider.getCache(InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME).getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD);

            Assert.assertEquals(0, offlineUSCache.size());
            Assert.assertEquals(0, offlineCSCache.size());

            // lazy load offline user sessions from DB => this should also import user and client sessions to the caches
            Assert.assertEquals(2, session.sessions().getOfflineUserSessionsStream(realm, session.users().getUserByUsername(realm, "user1")).count());

            // check sessions were imported to the caches
            Assert.assertEquals(2, offlineUSCache.size());
            Assert.assertEquals(4, offlineCSCache.size());

            // lifespan override set to 12h (43200s)
            setTimeOffset(44000);

            // check sessions were evicted from the caches
            Assert.assertEquals(0, offlineUSCache.size());
            Assert.assertEquals(0, offlineCSCache.size());

            // sessions should still be in the DB
            Assert.assertEquals(2, session.sessions().getOfflineUserSessionsStream(realm, session.users().getUserByUsername(realm, "user1")).count());

            return null;
        });
    }

    private static Set<String> createOfflineSessionIncludeClientSessions(KeycloakSession session, UserSessionModel
            userSession) {
        Set<String> offlineSessions = new HashSet<>();
        UserSessionManager localManager = new UserSessionManager(session);
        for (AuthenticatedClientSessionModel clientSession : userSession.getAuthenticatedClientSessions().values()) {
            localManager.createOrUpdateOfflineSession(clientSession, userSession);
            offlineSessions.add(clientSession.getClient().getId());
        }

        return offlineSessions;
    }

    private void createOfflineSessions(String username, int sessionsPerUser, List<UserSessionModel> offlineUserSessions, List<AuthenticatedClientSessionModel> offlineClientSessions) {
        withRealm(realmId, (session, realm) -> {
            final UserModel user = session.users().getUserByUsername(realm, username);
            ClientModel testAppClient = realm.getClientByClientId("test-app");
            ClientModel thirdPartyClient = realm.getClientByClientId("third-party");

            IntStream.range(0, sessionsPerUser)
                    .mapToObj(index -> session.sessions().createUserSession(null, realm, user, username + index, "ip" + index, "auth", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT))
                    .forEach(userSession -> {
                        AuthenticatedClientSessionModel testAppClientSession = session.sessions().createClientSession(realm, testAppClient, userSession);
                        AuthenticatedClientSessionModel thirdPartyClientSession = session.sessions().createClientSession(realm, thirdPartyClient, userSession);
                        UserSessionModel offlineUserSession = session.sessions().createOfflineUserSession(userSession);
                        offlineUserSessions.add(offlineUserSession);
                        offlineClientSessions.add(session.sessions().createOfflineClientSession(testAppClientSession, offlineUserSession));
                        offlineClientSessions.add(session.sessions().createOfflineClientSession(thirdPartyClientSession, offlineUserSession));
                    });

            return null;
        });
    }

    private boolean assertOfflineSession(List<UserSessionModel> expectedUserSessions, List<UserSessionModel> actualUserSessions) {
        boolean result = true;
        // User sessions are compared by their ID given the
        for (UserSessionModel userSession: expectedUserSessions) {
            if (!actualUserSessions.contains(userSession)) {
                log.warnf("missing session %s", userSession);
                result = false;
            }
        }
        for (UserSessionModel userSession: actualUserSessions) {
            if (!expectedUserSessions.contains(userSession)) {
                log.warnf("seeing an additional session %s by user %s", userSession.getId(), userSession.getUser().getId());
                result = false;
            }
        }
        if (!result) {
            log.warnf("all expected sessions: %s, all actual sessions: %s",
                    expectedUserSessions.stream().map(UserSessionModel::getId).collect(Collectors.toList()),
                    actualUserSessions.stream().map(UserSessionModel::getId).collect(Collectors.toList()));
        }
        return result;
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
