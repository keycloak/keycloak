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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testsuite.model.HotRodServerRule;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.hamcrest.Matchers;
import org.infinispan.Cache;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.interceptors.AsyncInterceptorChain;
import org.infinispan.interceptors.impl.CacheMgmtInterceptor;
import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.testsuite.model.session.UserSessionPersisterProviderTest.createClients;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
@RequireProvider(value = AuthenticationSessionProvider.class)
public class AuthenticationSessionTest extends KeycloakModelTest {

    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "test");
        s.getContext().setRealm(realm);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        realm.setAccessCodeLifespanLogin(1800);

        this.realmId = realm.getId();

        createClients(s, realm);
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testLimitAuthSessions() {
        AtomicReference<String> rootAuthSessionId = new AtomicReference<>();
        List<String> tabIds = withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel ras = session.authenticationSessions().createRootAuthenticationSession(realm);
            rootAuthSessionId.set(ras.getId());
            ClientModel client = realm.getClientByClientId("test-app");
            return IntStream.range(0, 300)
                    .mapToObj(i -> {
                        setTimeOffset(i);
                        return ras.createAuthenticationSession(client);
                    })
                    .map(AuthenticationSessionModel::getTabId)
                    .collect(Collectors.toList());
        });

        String tabId = withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel ras = session.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionId.get());
            ClientModel client = realm.getClientByClientId("test-app");

            // create 301st auth session
            return ras.createAuthenticationSession(client).getTabId();
        });

        withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel ras = session.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionId.get());
            ClientModel client = realm.getClientByClientId("test-app");

            assertThat(ras.getAuthenticationSessions(), Matchers.aMapWithSize(300));

            Assert.assertEquals(tabId, ras.getAuthenticationSession(client, tabId).getTabId());

            // assert the first authentication session was deleted
            Assert.assertNull(ras.getAuthenticationSession(client, tabIds.get(0)));

            return null;
        });
    }

    @Test
    public void testAuthSessions() {
        AtomicReference<String> rootAuthSessionId = new AtomicReference<>();
        List<String> tabIds = withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().createRootAuthenticationSession(realm);
            rootAuthSessionId.set(rootAuthSession.getId());

            ClientModel client = realm.getClientByClientId("test-app");
            return IntStream.range(0, 5)
                    .mapToObj(i -> {
                        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);
                        authSession.setExecutionStatus("username", AuthenticationSessionModel.ExecutionStatus.ATTEMPTED);
                        authSession.setAuthNote("foo", "bar");
                        authSession.setClientNote("foo", "bar");
                        return authSession;
                    })
                    .map(AuthenticationSessionModel::getTabId)
                    .collect(Collectors.toList());
        });

        withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionId.get());
            Assert.assertNotNull(rootAuthSession);
            Assert.assertEquals(rootAuthSessionId.get(), rootAuthSession.getId());

            ClientModel client = realm.getClientByClientId("test-app");
            tabIds.forEach(tabId -> {
                AuthenticationSessionModel authSession = rootAuthSession.getAuthenticationSession(client, tabId);
                Assert.assertNotNull(authSession);

                Assert.assertEquals(AuthenticationSessionModel.ExecutionStatus.ATTEMPTED, authSession.getExecutionStatus().get("username"));
                Assert.assertEquals("bar", authSession.getAuthNote("foo"));
                Assert.assertEquals("bar", authSession.getClientNote("foo"));
            });

            // remove first two auth sessions
            rootAuthSession.removeAuthenticationSessionByTabId(tabIds.get(0));
            rootAuthSession.removeAuthenticationSessionByTabId(tabIds.get(1));

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionId.get());
            Assert.assertNotNull(rootAuthSession);
            Assert.assertEquals(rootAuthSessionId.get(), rootAuthSession.getId());

            assertThat(rootAuthSession.getAuthenticationSessions(), Matchers.aMapWithSize(3));

            Assert.assertNull(rootAuthSession.getAuthenticationSessions().get(tabIds.get(0)));
            Assert.assertNull(rootAuthSession.getAuthenticationSessions().get(tabIds.get(1)));
            IntStream.range(2,4).mapToObj(i -> rootAuthSession.getAuthenticationSessions().get(tabIds.get(i))).forEach(Assert::assertNotNull);

            session.authenticationSessions().removeRootAuthenticationSession(realm, rootAuthSession);

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionId.get());
            Assert.assertNull(rootAuthSession);

            return null;
        });
    }

    @Test
    public void testRemoveExpiredAuthSessions() {
        AtomicReference<String> rootAuthSessionId = new AtomicReference<>();
        withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().createRootAuthenticationSession(realm);
            ClientModel client = realm.getClientByClientId("test-app");
            rootAuthSession.createAuthenticationSession(client);
            rootAuthSessionId.set(rootAuthSession.getId());

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionId.get());
            Assert.assertNotNull(rootAuthSession);

            setTimeOffset(1900);

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, rootAuthSessionId.get());
            Assert.assertNull(rootAuthSession);

            return null;
        });
    }

    @Test
    public void testConcurrentAuthenticationSessionsCreation() throws InterruptedException {
        final String rootId = withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().createRootAuthenticationSession(realm);
            return rootAuthSession.getId();
        });
        ConcurrentHashMap.KeySetView<String, Boolean> tabIds = ConcurrentHashMap.newKeySet();
        inIndependentFactories(4, 60, () -> {
                withRealm(realmId, (session, realm) -> {
                    RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, rootId);
                    ClientModel client = realm.getClientByClientId("test-app");
                    AuthenticationSessionModel authenticationSession = rootAuthSession.createAuthenticationSession(client);
                    tabIds.add(authenticationSession.getTabId());
                    return null;
                });
        });

        withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, rootId);
            Assert.assertEquals(4, rootAuthSession.getAuthenticationSessions().size());
            assertThat(rootAuthSession.getAuthenticationSessions().keySet(), Matchers.containsInAnyOrder(tabIds.toArray()));
            return null;
        });
    }

    @Test
    public void testConcurrentAuthenticationSessionsRemoval() throws InterruptedException {
        ConcurrentLinkedQueue<String> tabIds = new ConcurrentLinkedQueue<>();
        int concurrentTabs = 4;
        final String rootId = withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().createRootAuthenticationSession(realm);
            ClientModel client = realm.getClientByClientId("test-app");

            for (int i = 0; i < concurrentTabs; i++) {
                tabIds.add(rootAuthSession.createAuthenticationSession(client).getTabId());
            }
            return rootAuthSession.getId();
        });
        inIndependentFactories(concurrentTabs, 60, () -> {
            withRealm(realmId, (session, realm) -> {
                RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, rootId);
                rootAuthSession.removeAuthenticationSessionByTabId(tabIds.remove());
                return null;
            });
        });

        withRealm(realmId, (session, realm) -> {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, rootId);
            Assert.assertNull(rootAuthSession);
            return null;
        });
    }

    @Test
    public void testRemoveAfterCreation() {
        var computeOperationCount = operationCounterSupplier();
        var operationsBefore = computeOperationCount.getAsLong();

        withRealmConsumer(realmId, (session, realm) -> {
            // optimization in place:
            // create and remove in the same transaction should not trigger any operation in the Infinispan cache.
            var rootAuthSession = session.authenticationSessions().createRootAuthenticationSession(realm);
            var client = realm.getClientByClientId("test-app");
            rootAuthSession.setTimestamp(1000);
            var authSession = rootAuthSession.createAuthenticationSession(client);
            rootAuthSession.removeAuthenticationSessionByTabId(authSession.getTabId());
            session.authenticationSessions().removeRootAuthenticationSession(realm, rootAuthSession);
        });

        var operationsAfter = computeOperationCount.getAsLong();
        Assert.assertEquals("No operations expected in the cache", operationsBefore, operationsAfter);
    }

    private static long getNumberOfOperations(Cache<?, ?> cache) {
        var statsInterceptor = ComponentRegistry.componentOf(cache, AsyncInterceptorChain.class).findInterceptorWithClass(CacheMgmtInterceptor.class);
        statsInterceptor.setStatisticsEnabled(true);
        return statsInterceptor.getHits() + statsInterceptor.getMisses() + // reads
                statsInterceptor.getStores() + // writes
                statsInterceptor.getRemoveHits() + statsInterceptor.getRemoveMisses(); // removes
    }

    private LongSupplier operationCounterSupplier() {
        var hotRodServers = getParameters(HotRodServerRule.class).findFirst();
        if (hotRodServers.isEmpty()) {
            // fetch stats from embedded cache
            return () -> withRealm(realmId, (session, realm) -> {
                var cache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME);
                return getNumberOfOperations(cache);
            });
        }
        // fetch stats from external cache
        return () -> hotRodServers.stream()
                .flatMap(HotRodServerRule::streamCacheManagers)
                .map(manager -> manager.getCache(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME))
                .mapToLong(AuthenticationSessionTest::getNumberOfOperations)
                .sum();

    }
}
