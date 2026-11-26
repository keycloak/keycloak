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

import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.sessions.infinispan.InfinispanUserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.awaitility.Awaitility;
import org.infinispan.Cache;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Regression test for GH issue #51127: a concurrent cache reader can race a delete of a user session
 * (observe a cache miss, load the still-present pre-delete row from the database, and re-insert it
 * via {@code putIfAbsent()}), resurrecting an already deleted session in the cache.
 * <p>
 * This test exercises the minimal, additive fix (a write-path tombstone marker plus a cache listener
 * that tracks recently tombstoned keys for a short grace period and undoes a resurrection) directly
 * against the real embedded caches used by persistent user sessions, without needing to actually win
 * the race with concurrent threads: the stale {@code putIfAbsent()} that a racing reader would perform
 * is simulated explicitly. Client sessions are out of scope for this fix (see
 * {@code SessionResurrectionGuardListener}).
 */
@KeycloakIntegrationTest(config = SessionResurrectionTombstoneTest.SessionCachingServerConfig.class)
public class SessionResurrectionTombstoneTest {

    private static final String USERNAME = "user1";
    private static final String CLIENT_ID = "test-app";

    @InjectRealm(config = SessionResurrectionRealmConfig.class)
    ManagedRealm managedRealm;

    // A separate realm, used only by the bulk realm-wide removal test: that test records a realm-scoped
    // "not-before" watermark (SessionResurrectionGuardListener#recordRealmNotBefore) that stays active for
    // up to TOMBSTONE_TTL_SECONDS. Since @InjectRealm defaults to a class-scoped lifecycle, sharing
    // managedRealm across all test methods would let that watermark leak into sibling tests and wipe
    // brand-new, legitimate sessions they create in the same realm shortly afterwards.
    @InjectRealm(ref = "bulk-realm-removal", config = SessionResurrectionBulkRealmConfig.class)
    ManagedRealm bulkRemovalRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void resurrectedUserSessionIsRemovedAgain() {
        assumeOnlineUserSessionGuardSupported();

        final String realmName = managedRealm.getName();
        final String realmId = managedRealm.getId();
        final String userSessionId = createUserSession(realmName);
        final int started = readCachedStarted(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME, userSessionId);

        // Logout: runs in its own transaction/request, so the (deferred) cache removal that accompanies
        // the database delete is fully applied by the time this call returns - exactly as it would be
        // for a real, separate logout request.
        runOnServer.run(session -> {
            RealmModel realm = setRealmContext(session, realmName);
            UserSessionModel userSession = session.sessions().getUserSession(realm, userSessionId);
            session.sessions().removeUserSession(realm, userSession);
        });

        // A concurrent reader (a separate request) that had loaded the pre-delete row from the database
        // moments earlier now re-inserts it via putIfAbsent() - after the delete above already ran.
        runOnServer.run(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            cache.putIfAbsent(userSessionId, staleUserSessionWrapper(userSessionId, realmId, started));
        });

        runOnServer.run(session -> {
            RealmModel realm = setRealmContext(session, realmName);
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);

            awaitNull(() -> cache.get(userSessionId));
            assertNull(cache.get(userSessionId), "Resurrected user session must be removed again from the cache");
            assertNull(session.sessions().getUserSession(realm, userSessionId), "Session must not be resurrected after logout");
        });
    }

    @Test
    public void resurrectedOfflineUserSessionIsRemovedAgain() {
        assumeOfflineUserSessionGuardSupported();

        final String realmName = managedRealm.getName();
        final String realmId = managedRealm.getId();
        final String userSessionId = createOfflineUserSession(realmName);
        final int started = readCachedStarted(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME, userSessionId);

        runOnServer.run(session -> {
            RealmModel realm = setRealmContext(session, realmName);
            UserSessionModel offlineSession = session.sessions().getOfflineUserSession(realm, userSessionId);
            session.sessions().removeOfflineUserSession(realm, offlineSession);
        });

        runOnServer.run(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
            cache.putIfAbsent(userSessionId, staleUserSessionWrapper(userSessionId, realmId, started));
        });

        runOnServer.run(session -> {
            RealmModel realm = setRealmContext(session, realmName);
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);

            awaitNull(() -> cache.get(userSessionId));
            assertNull(cache.get(userSessionId), "Resurrected offline user session must be removed again from the cache");
            assertNull(session.sessions().getOfflineUserSession(realm, userSessionId), "Offline session must not be resurrected after removal");
        });
    }

    /**
     * Bulk realm-wide removal ("logout all sessions in a realm", {@code PersistentUserSessionProvider#
     * removeEntriesByRealm}) bypasses per-key tombstoning for performance - it removes cache entries
     * directly instead of going through {@code InfinispanChangesUtils}. Instead, a per-realm "not-before"
     * watermark is recorded (see {@code SessionResurrectionGuardListener#recordRealmNotBefore}), and this
     * test exercises that path specifically: it simulates the same stale-reader race as the other tests,
     * but after a bulk realm removal rather than a single-session removal.
     */
    @Test
    public void resurrectedUserSessionAfterBulkRealmRemovalIsRemovedAgain() {
        assumeOnlineUserSessionGuardSupported();

        final String realmName = bulkRemovalRealm.getName();
        final String realmId = bulkRemovalRealm.getId();
        final String userSessionId = createUserSession(realmName);
        final int started = readCachedStarted(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME, userSessionId);

        // Logout all sessions in the realm: runs in its own transaction/request, so the bulk cache
        // removal and the "not-before" watermark recording are both fully applied by the time this call
        // returns - exactly as they would be for a real, separate admin request.
        runOnServer.run(session -> {
            RealmModel realm = setRealmContext(session, realmName);
            session.sessions().removeUserSessions(realm);
        });

        // A concurrent reader (a separate request) that had loaded the pre-delete row from the database
        // moments earlier now re-inserts it via putIfAbsent() - after the bulk removal above already ran.
        // No per-key tombstone exists for this key (the bulk path never writes one), so only the
        // realm-wide watermark can catch this resurrection.
        runOnServer.run(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            cache.putIfAbsent(userSessionId, staleUserSessionWrapper(userSessionId, realmId, started));
        });

        runOnServer.run(session -> {
            RealmModel realm = setRealmContext(session, realmName);
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);

            awaitNull(() -> cache.get(userSessionId));
            assertNull(cache.get(userSessionId), "Resurrected user session must be removed again from the cache");
            assertNull(session.sessions().getUserSession(realm, userSessionId), "Session must not be resurrected after bulk realm removal");
        });
    }

    /**
     * The online user session cache is only guarded when persistent user sessions with embedded
     * Infinispan caches are in use - see InfinispanUserSessionProviderFactory. Without a database
     * fallback behind it, a plain (volatile) online session cache is not susceptible to the
     * resurrection race.
     */
    private void assumeOnlineUserSessionGuardSupported() {
        boolean supported = runOnServer.fetch(session -> {
            if (!MultiSiteUtils.isPersistentSessionsEnabled() || !InfinispanUtils.isEmbeddedInfinispan()) {
                return false;
            }
            var factory = (InfinispanUserSessionProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(UserSessionProvider.class);
            return factory.useCaches();
        }, Boolean.class);
        Assumptions.assumeTrue(supported, "Requires persistent user sessions with embedded Infinispan caches enabled");
    }

    /**
     * The offline user session cache always falls back to the database (via
     * {@code JpaUserSessionPersisterProvider}), whether or not persistent user sessions are enabled -
     * see InfinispanUserSessionProviderFactory. The guard is only registered for embedded caches.
     */
    private void assumeOfflineUserSessionGuardSupported() {
        boolean supported = runOnServer.fetch(session -> InfinispanUtils.isEmbeddedInfinispan(), Boolean.class);
        Assumptions.assumeTrue(supported, "Requires embedded Infinispan caches enabled");
    }

    private String createUserSession(String realmName) {
        return runOnServer.fetch(session -> {
            RealmModel realm = setRealmContext(session, realmName);
            UserModel user = session.users().getUserByUsername(realm, USERNAME);
            UserSessionModel userSession = session.sessions().createUserSession(null, realm, user, USERNAME, "127.0.0.1", "form", false, null, null,
                    UserSessionModel.SessionPersistenceState.PERSISTENT);
            return userSession.getId();
        }, String.class);
    }

    private String createOfflineUserSession(String realmName) {
        return runOnServer.fetch(session -> {
            RealmModel realm = setRealmContext(session, realmName);
            UserModel user = session.users().getUserByUsername(realm, USERNAME);
            UserSessionModel userSession = session.sessions().createUserSession(null, realm, user, USERNAME, "127.0.0.1", "form", false, null, null,
                    UserSessionModel.SessionPersistenceState.PERSISTENT);
            UserSessionModel offlineSession = session.sessions().createOfflineUserSession(userSession);
            return offlineSession.getId();
        }, String.class);
    }

    /**
     * Reads the "started" value of the cached user session entity, to be reused when building a
     * stand-in for the stale, pre-delete row a racing reader would have loaded from the database.
     */
    private int readCachedStarted(String cacheName, String userSessionId) {
        return runOnServer.fetch(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(cacheName);
            SessionEntityWrapper<UserSessionEntity> wrapper = cache.get(userSessionId);
            if (wrapper == null) {
                fail("User session should be cached after creation: " + userSessionId);
            }
            return wrapper.getEntity().getStarted();
        }, Integer.class);
    }

    /**
     * Builds a stand-in for the pre-delete row a racing reader would have loaded from the database:
     * same key and "started" value as the entity that was (or is about to be) removed.
     */
    private static SessionEntityWrapper<UserSessionEntity> staleUserSessionWrapper(String userSessionId, String realmId, int started) {
        UserSessionEntity entity = new UserSessionEntity(userSessionId);
        entity.setRealmId(realmId);
        entity.setStarted(started);
        return new SessionEntityWrapper<>(entity);
    }

    private static RealmModel setRealmContext(KeycloakSession session, String realmName) {
        RealmModel realm = session.realms().getRealmByName(realmName);
        session.getContext().setRealm(realm);
        return realm;
    }

    /**
     * The guard removes a resurrected entry asynchronously ({@code Cache#removeAsync}), so the removal
     * may not be visible immediately after the racing {@code putIfAbsent} returns.
     */
    private static <V> void awaitNull(java.util.function.Supplier<V> supplier) {
        Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).until(() -> supplier.get() == null);
    }

    public static class SessionResurrectionRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("session-resurrection-tombstone");
            realm.users(UserBuilder.create(USERNAME));
            realm.clients(ClientBuilder.create(CLIENT_ID));
            return realm;
        }
    }

    public static class SessionResurrectionBulkRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("session-resurrection-bulk-removal");
            realm.users(UserBuilder.create(USERNAME));
            realm.clients(ClientBuilder.create(CLIENT_ID));
            return realm;
        }
    }

    public static class SessionCachingServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.spiOption("user-sessions", "infinispan", "use-caches", "true");
        }
    }
}
