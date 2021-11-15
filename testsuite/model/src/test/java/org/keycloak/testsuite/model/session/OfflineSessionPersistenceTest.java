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
import org.keycloak.models.sessions.infinispan.InfinispanUserSessionProviderFactory;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

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
        this.realmId = realm.getId();

        userIds = IntStream.range(0, USER_COUNT)
          .mapToObj(i -> s.users().addUser(realm, "user-" + i))
          .map(UserModel::getId)
          .collect(Collectors.toList());
    }

    private static RealmModel prepareRealm(KeycloakSession s, String name) {
        RealmModel realm = s.realms().createRealm(name);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        realm.setSsoSessionMaxLifespan(10 * 60 * 60);
        realm.setSsoSessionIdleTimeout(1 * 60 * 60);
        realm.setOfflineSessionMaxLifespan(365 * 24 * 60 * 60);
        realm.setOfflineSessionIdleTimeout(30 * 24 * 60 * 60);
        return realm;
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        new RealmManager(s).removeRealm(s.realms().getRealm(realmId));  // See https://issues.redhat.com/browse/KEYCLOAK-17876
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
    public void testPersistenceMultipleNodesClientSessionAtSameNode() throws InterruptedException {
        List<String> clientIds = withRealm(realmId, (session, realm) -> {
            return IntStream.range(0, 5)
              .mapToObj(cid -> (ClientModel) session.clients().addClient(realm, "client-" + cid))
              .map(ClientModel::getId)
              .collect(Collectors.toList());
        });

        // Shutdown factory -> enforce session persistence
        closeKeycloakSessionFactory();

        Map<String, List<String>> clientSessionIds = new ConcurrentHashMap<>();
        inIndependentFactories(3, 30, () -> {
            withRealm(realmId, (session, realm) -> {
                // Create offline sessions
                userIds.forEach(userId -> createOfflineSessions(session, realm, userId, offlineUserSession -> {
                  List<String> innerClientSessionIds = IntStream.range(0, 5)
                    .mapToObj(cid -> session.clients().getClientById(realm, clientIds.get(cid)))
                    // TODO in the future: The following two lines are weird. Why an online client session needs to exist in order to create an offline one?
                    .map(client -> session.sessions().createClientSession(realm, client, offlineUserSession))
                    .map(clientSession -> session.sessions().createOfflineClientSession(clientSession, offlineUserSession))
                    .map(AuthenticatedClientSessionModel::getId)
                    .collect(Collectors.toList());
                  clientSessionIds.put(offlineUserSession.getId(), innerClientSessionIds);
                }));
                return null;
            });
        });

        reinitializeKeycloakSessionFactory();
        inIndependentFactories(4, 30, () -> assertOfflineSessionsExist(realmId, clientSessionIds.keySet()));
    }

    @Test(timeout = 90 * 1000)
    public void testPersistenceMultipleNodesClientSessionsAtRandomNode() throws InterruptedException {
        List<String> clientIds = withRealm(realmId, (session, realm) -> {
            return IntStream.range(0, 5)
              .mapToObj(cid -> (ClientModel) session.clients().addClient(realm, "client-" + cid))
              .map(ClientModel::getId)
              .collect(Collectors.toList());
        });
        List<String> offlineSessionIds = createOfflineSessions(realmId, userIds);

        // Shutdown factory -> enforce session persistence
        closeKeycloakSessionFactory();

        Map<String, List<String>> clientSessionIds = new ConcurrentHashMap<>();
        AtomicInteger i = new AtomicInteger();
        inIndependentFactories(3, 30, () -> {
            for (int j = 0; j < USER_COUNT * 3; j ++) {
                int index = i.incrementAndGet();
                int oid = index % offlineSessionIds.size();
                String offlineSessionId = offlineSessionIds.get(oid);
                int cid = index % clientIds.size();
                String clientSessionId = createOfflineClientSession(offlineSessionId, clientIds.get(cid));
                clientSessionIds.computeIfAbsent(offlineSessionId, a -> new LinkedList<>()).add(clientSessionId);
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
    @RequireProvider(value = UserSessionProvider.class, only = InfinispanUserSessionProviderFactory.PROVIDER_ID)
    public void testOfflineSessionLoadingAfterCacheRemoval() {
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
            ((InfinispanUserSessionProvider) provider).removeLocalUserSessions(realm.getId(), true);

            return null;
        });

        // assert sessions are lazily loaded from DB
        assertOfflineSessionsExist(realmId, offlineSessionIds);
    }

    @Test
    @RequireProvider(UserSessionPersisterProvider.class)
    @RequireProvider(value = UserSessionProvider.class, only = InfinispanUserSessionProviderFactory.PROVIDER_ID)
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
    @RequireProvider(value = UserSessionProvider.class, only = InfinispanUserSessionProviderFactory.PROVIDER_ID)
    public void testLazyOfflineUserSessionFetching() {
        List<String> offlineSessionIds = createOfflineSessions(realmId, userIds);
        assertOfflineSessionsExist(realmId, offlineSessionIds);

        // Simulate server restart
        reinitializeKeycloakSessionFactory();

        List<String> actualOfflineSessionIds = withRealm(realmId, (session, realm) -> session.users().getUsersStream(realm).flatMap(user ->
                session.sessions().getOfflineUserSessionsStream(realm, user)).map(UserSessionModel::getId).collect(Collectors.toList()));

        assertThat(actualOfflineSessionIds, containsInAnyOrder(offlineSessionIds.toArray()));
    }

    private String createOfflineClientSession(String offlineUserSessionId, String clientId) {
        return withRealm(realmId, (session, realm) -> {
            UserSessionModel offlineUserSession = session.sessions().getOfflineUserSession(realm, offlineUserSessionId);
            ClientModel client = session.clients().getClientById(realm, clientId);
            AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, client, offlineUserSession);
            return session.sessions().createOfflineClientSession(clientSession, offlineUserSession).getId();
        });
    }

    @Test(timeout = 90 * 1000)
    public void testPersistenceClientSessionsMultipleNodes() throws InterruptedException {
        // Create offline sessions
        List<String> offlineSessionIds = createOfflineSessions(realmId, userIds);

        // Shutdown factory -> enforce session persistence
        closeKeycloakSessionFactory();

        inIndependentFactories(4, 30, () -> assertOfflineSessionsExist(realmId, offlineSessionIds));
    }

    /**
     * Assert that all the offline sessions passed in the {@code offlineSessionIds} parameter exist
     * @param factory
     * @param offlineSessionIds
     * @return
     */
    private Void assertOfflineSessionsExist(String realmId, Collection<String> offlineSessionIds) {
        int foundOfflineSessions = withRealm(realmId, (session, realm) -> offlineSessionIds.stream()
          .map(offlineSessionId -> session.sessions().getOfflineUserSession(realm, offlineSessionId))
          .map(ous -> ous == null ? 0 : 1)
          .reduce(0, Integer::sum));

        assertThat(foundOfflineSessions, Matchers.is(USER_COUNT * OFFLINE_SESSION_COUNT_PER_USER));

        return null;
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
        UserSessionModel us = session.sessions().createUserSession(realm, user, "un" + sessionIndex, "ip1", "auth", false, null, null);
        return session.sessions().createOfflineUserSession(us);
    }

}
