/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.session;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.UserSessionAdapter;
import org.keycloak.models.sessions.infinispan.changes.CacheHolder;
import org.keycloak.models.sessions.infinispan.changes.ClientSessionPersistentChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.UserSessionPersistentChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.EmbeddedClientSessionKey;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.infinispan.Cache;
import org.infinispan.cache.impl.AbstractDelegatingCache;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression tests for request isolation and ownership transfer at cache publication. */
@KeycloakIntegrationTest(config = SessionResurrectionConcurrencyTest.SessionCachingServerConfig.class)
public class SessionPublicationOwnershipProbeTest extends SessionResurrectionConcurrencyTest {

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    public void cachedLookupsAvoidWritesAndHandleConcurrentPublication(boolean offline, boolean initialMiss) {
        oauth.scope(offline ? "offline_access" : null);
        var response = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, response.getStatusCode());
        String sessionId = oauth.parseRefreshToken(response.getRefreshToken()).getSessionId();
        String realmName = realm.getName();
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            var client = realmModel.getClientByClientId("marker-test-client");
            var parent = offline ? session.sessions().getOfflineUserSession(realmModel, sessionId) : session.sessions().getUserSession(realmModel, sessionId);
            session.sessions().getClientSession(parent, client, offline);
            var infinispan = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, SessionEntityWrapper<UserSessionEntity>> parents = infinispan.getCache(offline ? InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME : InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> children = infinispan.getCache(offline ? InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME : InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
            var reads = new AtomicInteger();
            var writes = new AtomicInteger();
            var parentHolder = new CacheHolder<>(lookupCache(parents, initialMiss, reads, writes), null, null, null, null);
            var childHolder = new CacheHolder<>(lookupCache(children, initialMiss, reads, writes), null, null, null, null);
            // Fresh transaction objects ensure these lookups reach the shared cache.
            var parentTx = new UserSessionPersistentChangelogBasedTransaction(session, parentHolder, parentHolder, false, Long.MAX_VALUE);
            var childTx = new ClientSessionPersistentChangelogBasedTransaction(session, childHolder, childHolder, parentTx, false, Long.MAX_VALUE);
            var childKey = new EmbeddedClientSessionKey(sessionId, client.getId());
            assertEquals(parents.get(sessionId).getVersion(), parentTx.get(realmModel, sessionId, null, offline).getVersion());
            assertEquals(children.get(childKey).getVersion(), childTx.get(realmModel, client, parent, childKey, offline).getVersion());
            assertEquals(2, reads.get());
            assertEquals(initialMiss ? 2 : 0, writes.get());
        });
        oauth.doLogout(response.getRefreshToken());
        oauth.scope(null);
    }

    private static <K, V> Cache<K, V> lookupCache(Cache<K, V> cache, boolean initialMiss, AtomicInteger reads, AtomicInteger writes) {
        return new AbstractDelegatingCache<>(cache) {
            @Override
            public V get(Object key) {
                reads.incrementAndGet();
                // A value is already present when putIfAbsent runs, simulating publication after the miss.
                return initialMiss ? null : super.get(key);
            }

            @Override
            public V putIfAbsent(K key, V value, long lifespan, TimeUnit unit) {
                writes.incrementAndGet();
                return super.putIfAbsent(key, value, lifespan, unit);
            }
        };
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,false", "true,true", "false,true"})
    public void publishedSessionsAreIsolatedFromLateMutation(boolean cacheHit, boolean offline) {
        oauth.scope(offline ? "offline_access" : null);
        var response = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, response.getStatusCode());
        String sessionId = oauth.parseRefreshToken(response.getRefreshToken()).getSessionId();
        String realmName = realm.getName();
        runOnServer.run(session -> registerLateCallback(session, realmName, sessionId, cacheHit, offline));

        String result = runOnServer.fetch(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            var client = realmModel.getClientByClientId("marker-test-client");
            var persister = session.getProvider(UserSessionPersisterProvider.class);
            var parent = persister.loadUserSession(realmModel, sessionId, offline);
            var child = persister.loadClientSession(realmModel, client, parent, offline);
            var infinispan = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, SessionEntityWrapper<UserSessionEntity>> parents = infinispan.getCache(offline ? InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME : InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> children = infinispan.getCache(offline ? InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME : InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
            return "dbParent=" + parent.getNote("ownership-probe")
                    + ",dbChild=" + child.getNote("ownership-probe")
                    + ",dbMap=" + parent.getNote("retained-map-probe")
                    + ",cacheParent=" + parents.get(sessionId).getEntity().getNotes().get("ownership-probe")
                    + ",cacheChild=" + children.get(new EmbeddedClientSessionKey(sessionId, client.getId())).getEntity().getNotes().get("ownership-probe")
                    + ",cacheMap=" + parents.get(sessionId).getEntity().getNotes().get("retained-map-probe");
        }, String.class);
        System.out.println("PUBLICATION_OWNERSHIP_PROBE " + result);
        assertEquals("dbParent=committed,dbChild=committed,dbMap=null,cacheParent=committed,cacheChild=committed,cacheMap=null", result);
        oauth.doLogout(response.getRefreshToken());
        oauth.scope(null);
    }

    private static void registerLateCallback(org.keycloak.models.KeycloakSession session, String realmName, String sessionId, boolean cacheHit, boolean offline) {
        var realmModel = session.realms().getRealmByName(realmName);
        var client = realmModel.getClientByClientId("marker-test-client");
        var infinispan = session.getProvider(InfinispanConnectionProvider.class);
        Cache<String, SessionEntityWrapper<UserSessionEntity>> parents = infinispan.getCache(offline ? InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME : InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
        Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> children = infinispan.getCache(offline ? InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME : InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
        var childKey = new EmbeddedClientSessionKey(sessionId, client.getId());
        if (!cacheHit) {
            parents.remove(sessionId);
            children.remove(childKey);
        }
        var parent = offline ? session.sessions().getOfflineUserSession(realmModel, sessionId) : session.sessions().getUserSession(realmModel, sessionId);
        var child = session.sessions().getClientSession(parent, client, offline);
        var retainedNotes = parent.getNotes();
        var oldParentVersion = parents.get(sessionId).getVersion();
        var oldChildVersion = children.get(childKey).getVersion();
        parent.setNote("ownership-probe", "committed");
        child.setNote("ownership-probe", "committed");
        assertNull(parents.get(sessionId).getEntity().getNotes().get("ownership-probe"));
        assertNull(children.get(childKey).getEntity().getNotes().get("ownership-probe"));
        assertTrue(((UserSessionAdapter<?>) parent).getEntity() != parents.get(sessionId).getEntity());
        // The session transaction has already enlisted. This callback runs after its publication.
        session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
            @Override
            protected void commitImpl() {
                var publishedParent = parents.get(sessionId);
                var publishedChild = children.get(childKey);
                assertTrue(!oldParentVersion.equals(publishedParent.getVersion()));
                assertTrue(!oldChildVersion.equals(publishedChild.getVersion()));
                assertTrue(((UserSessionAdapter<?>) parent).getEntity() != publishedParent.getEntity());
                assertEquals("committed", publishedParent.getEntity().getNotes().get("ownership-probe"));
                assertEquals("committed", publishedChild.getEntity().getNotes().get("ownership-probe"));
                parent.setNote("ownership-probe", "late");
                child.setNote("ownership-probe", "late");
                retainedNotes.put("retained-map-probe", "late");
                assertNull(publishedParent.getEntity().getNotes().get("retained-map-probe"));
                parent.getNotes().put("retained-map-probe", "late");
                assertEquals("committed", parents.get(sessionId).getEntity().getNotes().get("ownership-probe"));
                assertEquals("committed", children.get(childKey).getEntity().getNotes().get("ownership-probe"));
                assertEquals(publishedParent.getVersion(), parents.get(sessionId).getVersion());
                assertEquals(publishedChild.getVersion(), children.get(childKey).getVersion());
            }

            @Override
            protected void rollbackImpl() {
            }
        });
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,false", "true,true", "false,true"})
    public void rollbackKeepsCacheSnapshotsUnchanged(boolean cacheHit, boolean offline) {
        oauth.scope(offline ? "offline_access" : null);
        var response = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, response.getStatusCode());
        String sessionId = oauth.parseRefreshToken(response.getRefreshToken()).getSessionId();
        String realmName = realm.getName();
        String before = runOnServer.fetch(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            var client = realmModel.getClientByClientId("marker-test-client");
            var infinispan = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, SessionEntityWrapper<UserSessionEntity>> parents = infinispan.getCache(offline ? InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME : InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> children = infinispan.getCache(offline ? InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME : InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
            var childKey = new EmbeddedClientSessionKey(sessionId, client.getId());
            if (!cacheHit) {
                parents.remove(sessionId);
                children.remove(childKey);
            }
            var parent = offline ? session.sessions().getOfflineUserSession(realmModel, sessionId) : session.sessions().getUserSession(realmModel, sessionId);
            var child = session.sessions().getClientSession(parent, client, offline);
            var cachedParent = parents.get(sessionId);
            var cachedChild = children.get(childKey);
            var parentVersion = cachedParent.getVersion();
            var childVersion = cachedChild.getVersion();
            parent.setNote("alias-probe", "uncommitted");
            child.setNote("alias-probe", "uncommitted");
            String result = "sameParent=" + (((UserSessionAdapter<?>) parent).getEntity() == cachedParent.getEntity())
                    + ",parentVisible=" + "uncommitted".equals(parents.get(sessionId).getEntity().getNotes().get("alias-probe"))
                    + ",childVisible=" + "uncommitted".equals(children.get(childKey).getEntity().getNotes().get("alias-probe"))
                    + ",parentUuidUnchanged=" + parentVersion.equals(parents.get(sessionId).getVersion())
                    + ",childUuidUnchanged=" + childVersion.equals(children.get(childKey).getVersion());
            session.getTransactionManager().setRollbackOnly();
            return result;
        }, String.class);
        System.out.println("ALIAS_PROBE_BEFORE_ROLLBACK " + before);
        assertEquals("sameParent=false,parentVisible=false,childVisible=false,parentUuidUnchanged=true,childUuidUnchanged=true", before);

        String after = runOnServer.fetch(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            var client = realmModel.getClientByClientId("marker-test-client");
            var persister = session.getProvider(UserSessionPersisterProvider.class);
            var persistedParent = persister.loadUserSession(realmModel, sessionId, offline);
            var persistedChild = persister.loadClientSession(realmModel, client, persistedParent, offline);
            var infinispan = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, SessionEntityWrapper<UserSessionEntity>> parents = infinispan.getCache(offline ? InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME : InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> children = infinispan.getCache(offline ? InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME : InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
            return "dbParent=" + persistedParent.getNote("alias-probe")
                    + ",dbChild=" + persistedChild.getNote("alias-probe")
                    + ",cacheParent=" + parents.get(sessionId).getEntity().getNotes().get("alias-probe")
                    + ",cacheChild=" + children.get(new EmbeddedClientSessionKey(sessionId, client.getId())).getEntity().getNotes().get("alias-probe");
        }, String.class);
        System.out.println("ALIAS_PROBE_AFTER_ROLLBACK " + after);
        assertEquals("dbParent=null,dbChild=null,cacheParent=null,cacheChild=null", after);
        oauth.doLogout(response.getRefreshToken());
        oauth.scope(null);
    }
}
