/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model.infinispan;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserLoginFailureProvider;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.sessions.infinispan.changes.remote.remover.ConditionalRemover;
import org.keycloak.models.sessions.infinispan.changes.remote.remover.query.ByRealmIdQueryConditionalRemover;
import org.keycloak.models.sessions.infinispan.changes.remote.remover.query.ClientSessionQueryConditionalRemover;
import org.keycloak.models.sessions.infinispan.changes.remote.remover.query.UserSessionQueryConditionalRemover;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionKey;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.entities.RemoteAuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.RemoteUserSessionEntity;
import org.keycloak.models.sessions.infinispan.query.ClientSessionQueries;
import org.keycloak.models.sessions.infinispan.query.QueryHelper;
import org.keycloak.models.sessions.infinispan.query.UserSessionQueries;
import org.keycloak.models.sessions.infinispan.remote.RemoteUserLoginFailureProviderFactory;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.api.query.Query;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

@RequireProvider(UserLoginFailureProvider.class)
@RequireProvider(UserSessionProvider.class)
@RequireProvider(UserProvider.class)
@RequireProvider(RealmProvider.class)
public class InfinispanIckleQueryTest extends KeycloakModelTest {

    private static final List<String> REALMS = IntStream.range(0, 2).mapToObj(value -> "realm" + value).toList();
    private static final List<String> USERS = IntStream.range(0, 2).mapToObj(value -> "user" + value).toList();
    private static final List<String> BROKER_SESSIONS = IntStream.range(0, 2).mapToObj(value -> "brokerSession" + value).toList();
    private static final List<String> BROKER_USERS = IntStream.range(0, 2).mapToObj(value -> "brokerUser" + value).toList();
    private static final List<String> USER_SESSIONS = IntStream.range(0, 2).mapToObj(value -> "userSession" + value).toList();
    private static final List<String> CLIENTS = IntStream.range(0, 2).mapToObj(value -> "client" + value).toList();

    @ClassRule
    public static final TestRule SKIPPED_PROFILES = (base, description) -> {
        Assume.assumeTrue(InfinispanUtils.isRemoteInfinispan());
        return base;
    };

    @Test
    public void testByRealmIdQueryConditionalRemover() {
        RemoteCache<LoginFailureKey, LoginFailureEntity> cache = assumeAndReturnCache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME);

        var realm0Key = new LoginFailureKey("realm0", "a");
        var realm1Key = new LoginFailureKey("realm1", "a");
        var realm2Key = new LoginFailureKey("realm2", "a");

        Map<LoginFailureKey, LoginFailureEntity> data = new HashMap<>();

        // create and store users
        Stream.of(realm0Key, realm1Key, realm2Key).forEach(key -> data.put(key, new LoginFailureEntity(key.realmId(), key.userId())));
        cache.putAll(data);
        assertCacheSize(cache, 3);

        ByRealmIdQueryConditionalRemover<LoginFailureKey, LoginFailureEntity> remover = new ByRealmIdQueryConditionalRemover<>(RemoteUserLoginFailureProviderFactory.PROTO_ENTITY);

        // nothing should be removed
        data.forEach((k, v) -> assertRemove(remover, k, v, false));
        executeRemover(remover, cache);
        assertCacheSize(cache, 3);

        // remove single realm
        remover.removeByRealmId("realm0");
        assertRemove(remover, realm0Key, data.get(realm0Key), true);
        assertRemove(remover, realm1Key, data.get(realm1Key), false);
        assertRemove(remover, realm2Key, data.get(realm2Key), false);
        executeRemover(remover, cache);
        assertCacheSize(cache, 2);
        Assert.assertFalse(cache.containsKey(realm0Key));

        // remove all realms
        remover.removeByRealmId("realm1");
        remover.removeByRealmId("realm2");
        data.forEach((k, v) -> assertRemove(remover, k, v, true));
        executeRemover(remover, cache);
        assertCacheSize(cache, 0);
    }

    @Test
    public void testUserSessionRemoveByRealm() {
        RemoteCache<String, RemoteUserSessionEntity> cache = assumeAndReturnCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);

        var realm0Key = "a";
        var realm1Key = "b";
        var realm2Key = "c";

        Map<String, RemoteUserSessionEntity> data = Map.of(
                realm0Key, RemoteUserSessionEntity.mockEntity(realm0Key, "realm0", "user0"),
                realm1Key, RemoteUserSessionEntity.mockEntity(realm1Key, "realm1", "user0"),
                realm2Key, RemoteUserSessionEntity.mockEntity(realm2Key, "realm2", "user0")
        );
        cache.putAll(data);
        assertCacheSize(cache, 3);

        var remover = new UserSessionQueryConditionalRemover();

        // nothing should be removed
        data.forEach((k, v) -> assertRemove(remover, k, v, false));
        executeRemover(remover, cache);
        assertCacheSize(cache, 3);

        // remove single realm
        remover.removeByRealmId("realm0");
        assertRemove(remover, realm0Key, data.get(realm0Key), true);
        assertRemove(remover, realm1Key, data.get(realm1Key), false);
        assertRemove(remover, realm2Key, data.get(realm2Key), false);
        executeRemover(remover, cache);
        assertCacheSize(cache, 2);
        Assert.assertFalse(cache.containsKey(realm0Key));

        // remove all realms
        remover.removeByRealmId("realm1");
        remover.removeByRealmId("realm2");
        data.forEach((k, v) -> assertRemove(remover, k, v, true));
        executeRemover(remover, cache);
        assertCacheSize(cache, 0);
    }

    @Test
    public void testUserSessionRemoveByUser() {
        RemoteCache<String, RemoteUserSessionEntity> cache = assumeAndReturnCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);

        var user0Key = "a";
        var user1Key = "b";
        var user2Key = "c";

        Map<String, RemoteUserSessionEntity> data = Map.of(
                user0Key, RemoteUserSessionEntity.mockEntity(user0Key, "realm0", "user0"),
                user1Key, RemoteUserSessionEntity.mockEntity(user1Key, "realm0", "user1"),
                user2Key, RemoteUserSessionEntity.mockEntity(user2Key, "realm1", "user2")
        );
        cache.putAll(data);
        assertCacheSize(cache, 3);

        var remover = new UserSessionQueryConditionalRemover();

        // nothing should be removed
        data.forEach((k, v) -> assertRemove(remover, k, v, false));
        executeRemover(remover, cache);
        assertCacheSize(cache, 3);

        // remove single user session
        remover.removeByUserId("realm0", "user1");
        assertRemove(remover, user0Key, data.get(user0Key), false);
        assertRemove(remover, user1Key, data.get(user1Key), true);
        assertRemove(remover, user2Key, data.get(user2Key), false);
        executeRemover(remover, cache);
        assertCacheSize(cache, 2);
        Assert.assertFalse(cache.containsKey(user1Key));

        // remove all user sessions
        remover.removeByUserId("realm0", "user0");
        remover.removeByUserId("realm1", "user2");
        data.forEach((k, v) -> assertRemove(remover, k, v, true));
        executeRemover(remover, cache);
        assertCacheSize(cache, 0);
    }

    @Test
    public void testUserSessionRemoveMultiple() {
        RemoteCache<String, RemoteUserSessionEntity> cache = assumeAndReturnCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);

        var k0 = "a";
        var k1 = "b";
        var k2 = "c";
        var k3 = "d";

        Map<String, RemoteUserSessionEntity> data = Map.of(
                k0, RemoteUserSessionEntity.mockEntity(k0, "realm0", "user0"),
                k1, RemoteUserSessionEntity.mockEntity(k1, "realm0", "user1"),
                k2, RemoteUserSessionEntity.mockEntity(k2, "realm1", "user2"),
                k3, RemoteUserSessionEntity.mockEntity(k3, "realm2", "user3")
        );
        cache.putAll(data);
        assertCacheSize(cache, 4);

        var remover = new UserSessionQueryConditionalRemover();

        // nothing should be removed
        data.forEach((k, v) -> assertRemove(remover, k, v, false));
        executeRemover(remover, cache);
        assertCacheSize(cache, 4);

        // remove all
        remover.removeByRealmId("realm0"); // removes k0, k1
        remover.removeByUserId("realm1", "user2"); // removes k2
        remover.removeByUserId("realm2", "user3"); // removes k3
        data.forEach((k, v) -> assertRemove(remover, k, v, true));
        executeRemover(remover, cache);
        assertCacheSize(cache, 0);
    }

    @Test
    public void testClientSessionRemoveByRealm() {
        RemoteCache<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> cache = assumeAndReturnCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);

        var realm0Key = new ClientSessionKey("a", "a");
        var realm1Key = new ClientSessionKey("b", "b");
        var realm2Key = new ClientSessionKey("c", "c");

        Map<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> data = Map.of(
                realm0Key, RemoteAuthenticatedClientSessionEntity.mockEntity("a", "a", "realm0"),
                realm1Key, RemoteAuthenticatedClientSessionEntity.mockEntity("a", "a", "realm1"),
                realm2Key, RemoteAuthenticatedClientSessionEntity.mockEntity("a", "a", "realm2")
        );
        cache.putAll(data);
        assertCacheSize(cache, 3);

        var remover = new ClientSessionQueryConditionalRemover();

        // nothing should be removed
        data.forEach((k, v) -> assertRemove(remover, k, v, false));
        executeRemover(remover, cache);
        assertCacheSize(cache, 3);

        // remove single realm
        remover.removeByRealmId("realm0");
        assertRemove(remover, realm0Key, data.get(realm0Key), true);
        assertRemove(remover, realm1Key, data.get(realm1Key), false);
        assertRemove(remover, realm2Key, data.get(realm2Key), false);
        executeRemover(remover, cache);
        assertCacheSize(cache, 2);
        Assert.assertFalse(cache.containsKey(realm0Key));

        // remove all realms
        remover.removeByRealmId("realm1");
        remover.removeByRealmId("realm2");
        data.forEach((k, v) -> assertRemove(remover, k, v, true));
        executeRemover(remover, cache);
        assertCacheSize(cache, 0);
    }

    @Test
    public void testClientSessionRemoveByUser() {
        RemoteCache<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> cache = assumeAndReturnCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);

        var user0Key = new ClientSessionKey("a", "a");
        var user1Key = new ClientSessionKey("b", "b");
        var user2Key = new ClientSessionKey("c", "c");

        Map<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> data = Map.of(
                user0Key, RemoteAuthenticatedClientSessionEntity.mockEntity("a", "user0", "realm0"),
                user1Key, RemoteAuthenticatedClientSessionEntity.mockEntity("a", "user1", "realm0"),
                user2Key, RemoteAuthenticatedClientSessionEntity.mockEntity("a", "user2", "realm1")
        );
        cache.putAll(data);
        assertCacheSize(cache, 3);

        var remover = new ClientSessionQueryConditionalRemover();

        // nothing should be removed
        data.forEach((k, v) -> assertRemove(remover, k, v, false));
        executeRemover(remover, cache);
        assertCacheSize(cache, 3);

        // remove client session
        remover.removeByUserId("realm0", "user1");
        assertRemove(remover, user0Key, data.get(user0Key), false);
        assertRemove(remover, user1Key, data.get(user1Key), true);
        assertRemove(remover, user2Key, data.get(user2Key), false);
        executeRemover(remover, cache);
        assertCacheSize(cache, 2);
        Assert.assertFalse(cache.containsKey(user1Key));

        // remove client sessions
        remover.removeByUserId("realm0", "user0");
        remover.removeByUserId("realm1", "user2");
        data.forEach((k, v) -> assertRemove(remover, k, v, true));
        executeRemover(remover, cache);
        assertCacheSize(cache, 0);
    }

    @Test
    public void testClientSessionRemoveByUserSession() {
        RemoteCache<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> cache = assumeAndReturnCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);

        var userSession0Key = new ClientSessionKey("a", "a");
        var userSession1Key = new ClientSessionKey("b", "b");
        var userSession2Key = new ClientSessionKey("c", "c");
        var userSession3Key = new ClientSessionKey("d", "d");

        Map<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> data = Map.of(
                userSession0Key, RemoteAuthenticatedClientSessionEntity.mockEntity("userSession0", "a", "a"),
                userSession1Key, RemoteAuthenticatedClientSessionEntity.mockEntity("userSession1", "a", "a"),
                userSession2Key, RemoteAuthenticatedClientSessionEntity.mockEntity("userSession1", "a", "a"),
                userSession3Key, RemoteAuthenticatedClientSessionEntity.mockEntity("userSession2", "a", "a")
        );
        cache.putAll(data);
        assertCacheSize(cache, 4);

        var remover = new ClientSessionQueryConditionalRemover();

        // nothing should be removed
        data.forEach((k, v) -> assertRemove(remover, k, v, false));
        executeRemover(remover, cache);
        assertCacheSize(cache, 4);

        // remove single client session
        remover.removeByUserSessionId("userSession0");
        assertRemove(remover, userSession0Key, data.get(userSession0Key), true);
        assertRemove(remover, userSession1Key, data.get(userSession1Key), false);
        assertRemove(remover, userSession2Key, data.get(userSession2Key), false);
        assertRemove(remover, userSession3Key, data.get(userSession3Key), false);
        executeRemover(remover, cache);
        assertCacheSize(cache, 3);
        Assert.assertFalse(cache.containsKey(userSession0Key));

        // remove all client sessions
        remover.removeByUserSessionId("userSession1");
        remover.removeByUserSessionId("userSession2");
        data.forEach((k, v) -> assertRemove(remover, k, v, true));
        executeRemover(remover, cache);
        assertCacheSize(cache, 0);
    }

    @Test
    public void testClientSessionRemoveMultiple() {
        RemoteCache<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> cache = assumeAndReturnCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);

        var key0 = new ClientSessionKey("a", "a");
        var key1 = new ClientSessionKey("b", "b");
        var key2 = new ClientSessionKey("c", "c");
        var key3 = new ClientSessionKey("d", "d");
        var key4 = new ClientSessionKey("e", "e");
        var key5 = new ClientSessionKey("f", "f");

        Map<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> data = Map.of(
                key0, RemoteAuthenticatedClientSessionEntity.mockEntity("userSession0", "user0", "realm0"),
                key1, RemoteAuthenticatedClientSessionEntity.mockEntity("userSession1", "user1", "realm0"),
                key2, RemoteAuthenticatedClientSessionEntity.mockEntity("userSession2", "user2", "realm1"),
                key3, RemoteAuthenticatedClientSessionEntity.mockEntity("userSession3", "user2", "realm1"),
                key4, RemoteAuthenticatedClientSessionEntity.mockEntity("userSession4", "user2", "realm2"),
                key5, RemoteAuthenticatedClientSessionEntity.mockEntity("userSession4", "user2", "realm2")
        );
        cache.putAll(data);
        assertCacheSize(cache, 6);

        var remover = new ClientSessionQueryConditionalRemover();

        // nothing should be removed
        data.forEach((k, v) -> assertRemove(remover, k, v, false));
        executeRemover(remover, cache);
        assertCacheSize(cache, 6);

        // remove all users
        remover.removeByRealmId("realm0"); // key0 & key1
        remover.removeByUserId("realm1", "user2"); // key2 & key3
        remover.removeByUserSessionId("userSession4"); // key4 && key5
        data.forEach((k, v) -> assertRemove(remover, k, v, true));
        executeRemover(remover, cache);
        assertCacheSize(cache, 0);
    }

    @Test
    public void testUserSessionQueries() {
        RemoteCache<String, RemoteUserSessionEntity> cache = assumeAndReturnCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);

        for (var realmId : REALMS) {
            for (var userId : USERS) {
                for (var brokerSessionId : BROKER_SESSIONS) {
                    for (var brokerUserId : BROKER_USERS) {
                        var id = String.format("%s-%s-%s-%s", realmId, userId, brokerSessionId, brokerUserId);
                        cache.put(id, RemoteUserSessionEntity.mockEntity(id, realmId, userId, brokerSessionId, brokerUserId));
                    }
                }
            }
        }

        var realm = random(REALMS);
        var brokerSession = random(BROKER_SESSIONS);
        var user = random(USERS);
        var brokerUser = random(BROKER_USERS);

        var query = UserSessionQueries.searchByBrokerSessionId(cache, realm, brokerSession);
        var expectedResults = expectUserSessionId(realm, USERS, List.of(brokerSession), BROKER_USERS);
        assertQuery(query, RemoteUserSessionEntity::getUserSessionId, expectedResults);

        query = UserSessionQueries.searchByUserId(cache, realm, user);
        expectedResults = expectUserSessionId(realm, List.of(user), BROKER_SESSIONS, BROKER_USERS);
        assertQuery(query, RemoteUserSessionEntity::getUserSessionId, expectedResults);

        query = UserSessionQueries.searchByBrokerUserId(cache, realm, brokerUser);
        expectedResults = expectUserSessionId(realm, USERS, BROKER_SESSIONS, List.of(brokerUser));
        assertQuery(query, RemoteUserSessionEntity::getUserSessionId, expectedResults);
    }

    @Test
    public void testClientSessionQueries() {
        RemoteCache<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> cache = assumeAndReturnCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);

        for (var realmId : REALMS) {
            for (var clientId : CLIENTS) {
                for (var userSessionId : USER_SESSIONS) {
                    var id = new ClientSessionKey(userSessionId + "-" + realmId, clientId);
                    cache.put(id, RemoteAuthenticatedClientSessionEntity.mockEntity(userSessionId + "-" + realmId, clientId, "user", realmId));
                }
            }
        }

        var realm = random(REALMS);
        var client = random(CLIENTS);
        var userSession = random(USER_SESSIONS) + "-" + realm;

        var query = ClientSessionQueries.countClientSessions(cache, realm, client);
        var expectedResults = Set.of(String.valueOf(USER_SESSIONS.size()));
        assertQuery(query, objects -> String.valueOf(objects[0]), expectedResults);
        var optCount = QueryHelper.fetchSingle(query, QueryHelper.SINGLE_PROJECTION_TO_LONG);
        Assert.assertTrue(optCount.isPresent());
        Assert.assertEquals(USER_SESSIONS.size(), (long) optCount.get());

        query = ClientSessionQueries.fetchUserSessionIdForClientId(cache, realm, client);
        expectedResults = USER_SESSIONS.stream().map(s -> s + "-" + realm).collect(Collectors.toSet());
        assertQuery(query, objects -> String.valueOf(objects[0]), expectedResults);

        var query2 = ClientSessionQueries.fetchClientSessions(cache, userSession);
        expectedResults = CLIENTS.stream().map(s -> new ClientSessionKey(userSession, s)).map(Objects::toString).collect(Collectors.toSet());
        assertQuery(query2, objects -> objects.createCacheKey().toString(), expectedResults);

        // each client has user-session * realms active client sessions
        query = ClientSessionQueries.activeClientCount(cache);
        expectedResults = CLIENTS.stream().map(s -> String.format("%s-%s", s, USER_SESSIONS.size() * REALMS.size())).collect(Collectors.toSet());
        assertQuery(query, objects -> String.format("%s-%s", objects[0], objects[1]), expectedResults);
    }

    private static <T> void assertQuery(Query<T> query, Function<T, String> resultMapping, Set<String> expectedResults) {
        var results = new HashSet<String>();

        // test streaming with batchSize = 1
        QueryHelper.streamAll(query, 1, resultMapping).forEach(results::add);
        Assert.assertEquals(expectedResults, results);
        results.clear();

        // test streaming with batchSize = results.size
        QueryHelper.streamAll(query, expectedResults.size(), resultMapping).forEach(results::add);
        Assert.assertEquals(expectedResults, results);
        results.clear();

        // test streaming with batchSize > results.size
        QueryHelper.streamAll(query, expectedResults.size() * 2, resultMapping).forEach(results::add);
        Assert.assertEquals(expectedResults, results);
        results.clear();

        query.startOffset(0).maxResults(Integer.MAX_VALUE);
        Assert.assertEquals(expectedResults, new HashSet<>(QueryHelper.toCollection(query, resultMapping)));
    }


    private static String random(List<String> elements) {
        return elements.get(ThreadLocalRandom.current().nextInt(elements.size()));
    }

    private static Set<String> expectUserSessionId(String realmId, List<String> users, List<String> brokerSessions, List<String> brokerUsers) {
        var results = new HashSet<String>();
        for (var userId : users) {
            for (var brokerSessionId : brokerSessions) {
                for (var brokerUserId : brokerUsers) {
                    results.add(String.format("%s-%s-%s-%s", realmId, userId, brokerSessionId, brokerUserId));
                }
            }
        }
        return results;
    }

    private <K, V> RemoteCache<K, V> assumeAndReturnCache(String cacheName) {
        var cache = getInfinispanConnectionProvider().<K, V>getRemoteCache(cacheName);
        cache.clear();
        return cache;
    }

    private static <K, V> void executeRemover(ConditionalRemover<K, V> remover, RemoteCache<K, V> cache) {
        var stage = CompletionStages.aggregateCompletionStage();
        remover.executeRemovals(cache, stage);
        CompletionStages.join(stage.freeze());
    }

    private static <K, V> void assertRemove(ConditionalRemover<K, V> remover, K key, V value, boolean willRemove) {
        Assert.assertEquals(willRemove, remover.willRemove(key, value));
    }

    private static void assertCacheSize(RemoteCache<?, ?> cache, int expectedSize) {
        Assert.assertEquals(expectedSize, cache.size());
    }

    private InfinispanConnectionProvider getInfinispanConnectionProvider() {
        return inComittedTransaction(InfinispanIckleQueryTest::getInfinispanConnectionProviderWithSession);
    }

    private static InfinispanConnectionProvider getInfinispanConnectionProviderWithSession(KeycloakSession session) {
        return session.getProvider(InfinispanConnectionProvider.class);
    }

}
