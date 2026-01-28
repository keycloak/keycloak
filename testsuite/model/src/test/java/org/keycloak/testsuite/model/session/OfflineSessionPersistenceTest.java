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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.InfinispanUserSessionProvider;
import org.keycloak.models.sessions.infinispan.PersistentUserSessionProvider;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.hamcrest.Matchers;
import org.infinispan.commons.CacheException;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assume.assumeTrue;

/**
 *
 * @author hmlnarik
 */
@RequireProvider(UserProvider.class)
@RequireProvider(RealmProvider.class)
@RequireProvider(UserSessionProvider.class)
public class OfflineSessionPersistenceTest extends KeycloakModelTest {

    private static final int USER_COUNT = 50;
    private static final int OFFLINE_SESSION_COUNT_PER_USER = 10;

    private String realmId;
    private List<String> userIds;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = prepareRealm(s, "realm");
        s.getContext().setRealm(realm);
        this.realmId = realm.getId();

        userIds = IntStream.range(0, USER_COUNT)
          .mapToObj(i -> s.users().addUser(realm, "user-" + i))
          .map(UserModel::getId)
          .collect(Collectors.toList());
    }

    private static RealmModel prepareRealm(KeycloakSession s, String name) {
        RealmModel realm = createRealm(s, name);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        realm.setSsoSessionMaxLifespan(10 * 60 * 60);
        realm.setSsoSessionIdleTimeout(1 * 60 * 60);
        realm.setOfflineSessionMaxLifespan(365 * 24 * 60 * 60);
        realm.setOfflineSessionIdleTimeout(30 * 24 * 60 * 60);
        return realm;
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
        new RealmManager(s).removeRealm(realm);  // See https://issues.redhat.com/browse/KEYCLOAK-17876
    }

    @Test
    public void testPersistenceSingleNodeDeleteRealm() {
        String realmId2 = inComittedTransaction(session -> { return prepareRealm(session, "realm2").getId(); });
        List<String> userIds2 = withRealm(realmId2, (session, realm) -> IntStream.range(0, USER_COUNT)
          .mapToObj(i -> session.users().addUser(realm, "user2-" + i))
          .map(UserModel::getId)
          .collect(Collectors.toList())
        );

        try {
            List<String> offlineSessionIds = createOfflineSessions(realmId, userIds);
            assertOfflineSessionsExist(realmId, offlineSessionIds);

            List<String> offlineSessionIds2 = createOfflineSessions(realmId2, userIds2);
            assertOfflineSessionsExist(realmId2, offlineSessionIds2);

            // Simulate server restart
            reinitializeKeycloakSessionFactory();

            withRealm(realmId2, (session, realm) -> new RealmManager(session).removeRealm(realm));

            // Simulate server restart
            reinitializeKeycloakSessionFactory();
            assertOfflineSessionsExist(realmId, offlineSessionIds);
        } finally {
            withRealm(realmId2, (session, realm) -> realm == null ? false : new RealmManager(session).removeRealm(realm));
        }
    }

    @Test
    public void testPersistenceSingleNode() {
        List<String> offlineSessionIds = createOfflineSessions(realmId, userIds);
        assertOfflineSessionsExist(realmId, offlineSessionIds);

        // Simulate server restart
        reinitializeKeycloakSessionFactory();
        assertOfflineSessionsExist(realmId, offlineSessionIds);
    }

    @Test(timeout = 90 * 1000)
    @RequireProvider(UserSessionPersisterProvider.class)
    public void testPersistenceMultipleNodesClientSessionAtSameNode() throws InterruptedException {
        int numClients = 2;
        List<String> clientIds = withRealm(realmId, (session, realm) -> IntStream.range(0, numClients)
              .mapToObj(cid -> session.clients().addClient(realm, "client-" + cid))
              .map(ClientModel::getId)
              .collect(Collectors.toList()));

        // Shutdown factory -> enforce session persistence
        closeKeycloakSessionFactory();
        Set<String> clientSessionIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

        int NUM_FACTORIES = 3;
        CountDownLatch intermediate = new CountDownLatch(NUM_FACTORIES);
        inIndependentFactories(NUM_FACTORIES, 60, () -> {
            withRealm(realmId, (session, realm) -> {
                // Create offline sessions
                userIds.stream().limit(userIds.size() / 10).forEach(userId -> createOfflineSessions(session, realm, userId, offlineUserSession -> {
                  IntStream.range(0, numClients)
                    .mapToObj(cid -> session.clients().getClientById(realm, clientIds.get(cid)))
                    // TODO in the future: The following two lines are weird. Why an online client session needs to exist in order to create an offline one?
                    .map(client -> session.sessions().createClientSession(realm, client, offlineUserSession))
                    .map(clientSession -> session.sessions().createOfflineClientSession(clientSession, offlineUserSession))
                    .map(AuthenticatedClientSessionModel::getId)
                    .forEach(s -> {}); // ensure that stream is consumed
                }).forEach(userSessionModel -> clientSessionIds.add(userSessionModel.getId())));
                return null;
            });

            // ensure that all session have been created on all nodes
            intermediate.countDown();
            try {
                intermediate.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            // defer the shutdown and check if all sessions exist to ensure that they replicate across the different nodes
            // this should avoid an "org.infinispan.remoting.transport.jgroups.SuspectException: ISPN000400: Node node-XX was suspected"
            while (true) {
                try {
                    assertOfflineSessionsExist(realmId, clientSessionIds);
                    break;
                } catch (AssertionError e) {
                    log.warn("assertion failed, retrying to see if all sessions exist.");
                    sleep(1000);
                }
            }
        });

        reinitializeKeycloakSessionFactory();
        inIndependentFactories(NUM_FACTORIES + 1, 30, () -> assertOfflineSessionsExist(realmId, clientSessionIds));
    }

    @Test(timeout = 90 * 1000)
    @RequireProvider(UserSessionPersisterProvider.class)
    public void testPersistenceMultipleNodesClientSessionsAtRandomNode() throws InterruptedException {
        List<String> clientIds = withRealm(realmId, (session, realm) -> IntStream.range(0, 5)
              .mapToObj(cid -> session.clients().addClient(realm, "client-" + cid))
              .map(ClientModel::getId)
              .collect(Collectors.toList()));
        List<String> offlineSessionIds = createOfflineSessions(realmId, userIds);

        // Shutdown factory -> enforce session persistence
        closeKeycloakSessionFactory();

        Map<String, List<String>> clientSessionIds = new ConcurrentHashMap<>();
        AtomicInteger i = new AtomicInteger();
        inIndependentFactories(3, 60, () -> {
            for (int j = 0; j < USER_COUNT * 3; j ++) {
                int index = i.incrementAndGet();
                int oid = index % offlineSessionIds.size();
                String offlineSessionId = offlineSessionIds.get(oid);
                int cid = index % clientIds.size();
                try {
                    clientSessionIds.computeIfAbsent(offlineSessionId, a -> Collections.synchronizedList(new LinkedList<>())).add(createOfflineClientSession(offlineSessionId, clientIds.get(cid)));
                } catch (RuntimeException ex) {
                    // invocation can fail when remote cache is stopping, this is actually part of this test:
                    // "ISPN000217: Received exception from node-8, see cause for remote stack trace
                    // IllegalLifecycleStateException: ISPN000324: Cache 'clientSessions' is in 'STOPPING' state and this is an invocation not belonging to an
                    // on-going transaction, so it does not accept new invocations."
                    // also: org.infinispan.commons.CacheException: java.lang.IllegalStateException: Read commands must ignore leavers
                    if ((ex.getCause() != null && ex.getCause().getMessage().contains("ISPN000324")) ||
                            (ex.getMessage() != null && ex.getMessage().contains("ISPN000217")) ||
                            (ex instanceof CacheException && ex.getMessage().contains("Read commands must ignore leavers"))) {
                        log.warn("invocation failed, skipping. Retrying might lead to a 'Unique index or primary key violation' when the offline session has already been stored in the DB in the current session", ex);
                    } else {
                        throw ex;
                    }
                }

                // re-initialize the session factory N times in this test
                if (index % 100 == 0) {
                    reinitializeKeycloakSessionFactory();
                }
            }
        });

        reinitializeKeycloakSessionFactory();
        assertOfflineSessionsExist(realmId, offlineSessionIds);
    }

    @Test
    @RequireProvider(UserSessionPersisterProvider.class)
    public void testOfflineSessionLoadingAfterCacheRemoval() {
        assumeTrue("Run only if Embedded Infinispan is used for storing/caching sessions.", InfinispanUtils.isEmbeddedInfinispan());

        List<String> offlineSessionIds = createOfflineSessions(realmId, userIds);
        assertOfflineSessionsExist(realmId, offlineSessionIds);

        // Simulate server restart
        reinitializeKeycloakSessionFactory();
        assertOfflineSessionsExist(realmId, offlineSessionIds);

        // remove sessions from the cache
        withRealm(realmId, (session, realm) -> {
            // Delete local user cache (persisted sessions are still kept)
            UserSessionProvider provider = session.getProvider(UserSessionProvider.class);
            // Remove in-memory representation of the offline sessions
            if (provider instanceof InfinispanUserSessionProvider) {
                ((InfinispanUserSessionProvider) provider).removeLocalUserSessions(realm.getId(), true);
            } else if (provider instanceof PersistentUserSessionProvider) {
                ((PersistentUserSessionProvider) provider).removeLocalUserSessions(realm.getId(), true);
            } else {
                throw new IllegalStateException("Unknown UserSessionProvider: " + provider);
            }

            return null;
        });

        // assert sessions are lazily loaded from DB
        assertOfflineSessionsExist(realmId, offlineSessionIds);
    }

    @Test
    @RequireProvider(UserSessionPersisterProvider.class)
    public void testLazyClientSessionStatsFetching() {
        List<String> clientIds = withRealm(realmId, (session, realm) -> IntStream.range(0, 5)
                .mapToObj(cid -> session.clients().addClient(realm, "client-" + cid))
                .map(ClientModel::getId)
                .collect(Collectors.toList()));

        List<String> offlineSessionIds = createOfflineSessions(realmId, userIds);
        assertOfflineSessionsExist(realmId, offlineSessionIds);

        Random r = new Random();
        offlineSessionIds.stream().forEach(offlineSessionId -> createOfflineClientSession(offlineSessionId, clientIds.get(r.nextInt(5))));

        // Simulate server restart
        reinitializeKeycloakSessionFactory();

        // load active client sessions stats from DB
        Map<String, Long> sessionStats = withRealm(realmId, (session, realm) -> session.sessions().getActiveClientSessionStats(realm, true));

        long client1SessionCount = sessionStats.get(clientIds.get(0));
        int clientSessionsCount = sessionStats.values().stream().reduce(0l, Long::sum).intValue();
        assertThat(clientSessionsCount, Matchers.is(USER_COUNT * OFFLINE_SESSION_COUNT_PER_USER));

        // Simulate server restart
        reinitializeKeycloakSessionFactory();

        long actualClient1SessionCount = withRealm(realmId, (session, realm) -> {
            ClientModel client = realm.getClientById(clientIds.get(0));
            return session.sessions().getOfflineSessionsCount(realm, client);
        });
        assertThat(actualClient1SessionCount, Matchers.is(client1SessionCount));
    }

    @Test
    @RequireProvider(UserSessionPersisterProvider.class)
    public void testLazyOfflineUserSessionFetching() {
        Map<String, Set<String>> offlineSessionIdsDetailed = createOfflineSessionsDetailed(realmId, userIds);
        Collection<String> offlineSessionIds = offlineSessionIdsDetailed.values().stream().flatMap(Set::stream).collect(Collectors.toCollection(TreeSet::new));
        assertOfflineSessionsExist(realmId, offlineSessionIds);

        // Simulate server restart
        reinitializeKeycloakSessionFactory();

        Map<String, Set<String>> actualOfflineSessionIds = withRealm(realmId, (session, realm) -> session.users()
          .searchForUserStream(realm, Collections.emptyMap())
          .collect(Collectors.toMap(
            UserModel::getId,
            user -> session.sessions().getOfflineUserSessionsStream(realm, user).map(UserSessionModel::getId).collect(Collectors.toCollection(TreeSet::new))
          ))
        );

        assertThat("User IDs", actualOfflineSessionIds.keySet(), equalTo(offlineSessionIdsDetailed.keySet()));
        for (Entry<String, Set<String>> me : offlineSessionIdsDetailed.entrySet()) {
            assertThat("Session IDs", actualOfflineSessionIds.get(me.getKey()), equalTo(me.getValue()));
        }
    }

    private String createOfflineClientSession(String offlineUserSessionId, String clientId) {
        return withRealm(realmId, (session, realm) -> {
            UserSessionModel offlineUserSession = session.sessions().getOfflineUserSession(realm, offlineUserSessionId);
            assertThat("Can't retrieve offline session for " + offlineUserSessionId, offlineUserSession, Matchers.notNullValue());
            ClientModel client = session.clients().getClientById(realm, clientId);
            AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, client, offlineUserSession);
            return session.sessions().createOfflineClientSession(clientSession, offlineUserSession).getId();
        });
    }

    @Test(timeout = 90 * 1000)
    @RequireProvider(UserSessionPersisterProvider.class)
    public void testPersistenceClientSessionsMultipleNodes() throws InterruptedException {
        // Create offline sessions
        List<String> offlineSessionIds = createOfflineSessions(realmId, userIds);

        // Shutdown factory -> enforce session persistence
        closeKeycloakSessionFactory();

        inIndependentFactories(4, 60, () -> assertOfflineSessionsExist(realmId, offlineSessionIds));
    }

    /**
     * Assert that all the offline sessions passed in the {@code offlineSessionIds} parameter exist
     */
    private void assertOfflineSessionsExist(String realmId, Collection<String> offlineSessionIds) {
        int foundOfflineSessions = withRealm(realmId, (session, realm) -> offlineSessionIds.stream()
          .map(offlineSessionId -> session.sessions().getOfflineUserSession(realm, offlineSessionId))
          .map(ous -> ous == null ? 0 : 1)
          .reduce(0, Integer::sum));

        assertThat(foundOfflineSessions, Matchers.is(offlineSessionIds.size()));
        // catch a programming error where an empty collection of offline session IDs is passed
        assertThat(foundOfflineSessions, Matchers.greaterThan(0));
    }

    // ***************** Helper methods *****************

    /**
     * Creates {@link #OFFLINE_SESSION_COUNT_PER_USER} offline sessions for every user from {@link #userIds}.
     * @return Ids of the offline sessions
     */
    private List<String> createOfflineSessions(String realmId, List<String> userIds) {
        return withRealm(realmId, (session, realm) ->
          userIds.stream()
            .flatMap(userId -> createOfflineSessions(session, realm, userId, us -> {}))
            .map(UserSessionModel::getId)
            .collect(Collectors.toList())
        );
    }

    private Map<String, Set<String>> createOfflineSessionsDetailed(String realmId, List<String> userIds) {
        return withRealm(realmId, (session, realm) ->
          userIds.stream()
            .collect(Collectors.toMap(
              Function.identity(),
              userId -> createOfflineSessions(session, realm, userId, us -> {}).map(UserSessionModel::getId).collect(Collectors.toCollection(TreeSet::new))
            ))
        );
    }

    /**
     * Creates {@link #OFFLINE_SESSION_COUNT_PER_USER} offline sessions for {@code userId} user.
     */
    private Stream<UserSessionModel> createOfflineSessions(KeycloakSession session, RealmModel realm, String userId, Consumer<? super UserSessionModel> alterUserSession) {
        return IntStream.range(0, OFFLINE_SESSION_COUNT_PER_USER)
          .mapToObj(sess -> createOfflineSession(session, realm, userId, sess))
          .peek(alterUserSession == null ? us -> {} : us -> alterUserSession.accept(us));
    }

    private UserSessionModel createOfflineSession(KeycloakSession session, RealmModel realm, String userId, int sessionIndex) {
        final UserModel user = session.users().getUserById(realm, userId);
        UserSessionModel us = session.sessions().createUserSession(null, realm, user, "un" + sessionIndex, "ip1", "auth", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
        return session.sessions().createOfflineUserSession(us);
    }

}
