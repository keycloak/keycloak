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

package org.keycloak.models.sessions.infinispan.listeners;

import java.util.concurrent.TimeUnit;

import org.keycloak.common.util.Time;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import org.awaitility.Awaitility;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit test for the additive resurrection guard (GH issue #51127): it verifies that a tombstone
 * marker is removed as soon as it is observed, and that a real entity re-inserted at a recently
 * tombstoned key within the grace period is undone, while an insertion at a key that was never
 * tombstoned is left untouched.
 */
public class SessionResurrectionGuardListenerTest {

    private static final String CACHE_NAME = "session-resurrection-guard-test";

    private DefaultCacheManager cacheManager;
    private Cache<String, SessionEntityWrapper<UserSessionEntity>> cache;

    @Before
    public void before() {
        Time.setOffset(0);

        GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder();
        cacheManager = new DefaultCacheManager(gcb.build());
        cacheManager.defineConfiguration(CACHE_NAME, new ConfigurationBuilder().build());
        cache = cacheManager.getCache(CACHE_NAME);
        cache.addListener(new SessionResurrectionGuardListener<>());
    }

    @After
    public void after() {
        cacheManager.stop();
        Time.setOffset(0);
    }

    @Test
    public void tombstoneMarkerIsRemovedImmediately() {
        String key = "session-1";
        SessionEntityWrapper<UserSessionEntity> tombstone = SessionEntityWrapper.createTombstoneMarker(entity(key, 1000));

        cache.put(key, tombstone);

        awaitRemoval(key);
        assertNull("Tombstone marker must be removed as soon as it is observed", cache.get(key));
    }

    @Test
    public void resurrectedEntityIsRemovedAgain() {
        String key = "session-2";
        SessionEntityWrapper<UserSessionEntity> original = wrap(key, 1000);
        SessionEntityWrapper<UserSessionEntity> tombstone = SessionEntityWrapper.createTombstoneMarker(original.getEntity());

        // Simulates the write-path removal: a tombstone marker is written in place of a bare remove().
        cache.put(key, tombstone);
        awaitRemoval(key);

        // Simulate a concurrent reader re-inserting the stale, pre-delete data it loaded from the DB.
        cache.putIfAbsent(key, original);

        awaitRemoval(key);
        assertNull("Resurrected entry must be removed again", cache.get(key));
    }

    @Test
    public void tombstoneExpiresAfterGracePeriod() {
        String key = "session-3";
        SessionEntityWrapper<UserSessionEntity> original = wrap(key, 1000);
        SessionEntityWrapper<UserSessionEntity> tombstone = SessionEntityWrapper.createTombstoneMarker(original.getEntity());

        cache.put(key, tombstone);
        awaitRemoval(key);

        // Move the clock past the tombstone TTL before the (late) re-insertion happens.
        Time.setOffset((int) (SessionResurrectionGuardListener.TOMBSTONE_TTL_SECONDS + 5L));
        cache.putIfAbsent(key, original);

        assertNotNull("Once the tombstone has expired, re-insertion is no longer guarded", cache.get(key));
    }

    @Test
    public void insertionWithoutPriorTombstoneIsUntouched() {
        String key = "session-4";
        SessionEntityWrapper<UserSessionEntity> original = wrap(key, 1000);

        cache.put(key, original);

        assertNotNull("An insertion at a key that was never tombstoned must be left untouched", cache.get(key));
    }

    @Test
    public void newerIncarnationAtSameKeyIsNotTreatedAsResurrection() {
        // Offline sessions are stored under their originating online session's key (see class javadoc),
        // so revoking and re-granting offline access shortly afterwards legitimately reuses a key that
        // was just tombstoned, with a later "started" time.
        String key = "session-5";
        SessionEntityWrapper<UserSessionEntity> removed = wrap(key, 1000);
        SessionEntityWrapper<UserSessionEntity> tombstone = SessionEntityWrapper.createTombstoneMarker(removed.getEntity());

        cache.put(key, tombstone);
        awaitRemoval(key);

        SessionEntityWrapper<UserSessionEntity> newerIncarnation = wrap(key, 1001);
        cache.putIfAbsent(key, newerIncarnation);

        assertNotNull("A genuinely newer incarnation reusing a tombstoned key must not be removed",
                cache.get(key));
    }

    @Test
    public void tombstoneOverwritingLiveEntryIsObservedViaModify() {
        // When a session is removed while it is still cached, the tombstone overwrites the live entry,
        // firing a CacheEntryModified (not CacheEntryCreated) notification. This test verifies that
        // the @CacheEntryModified path is functional.
        String key = "session-6";
        SessionEntityWrapper<UserSessionEntity> live = wrap(key, 1000);
        cache.put(key, live);
        assertNotNull(cache.get(key));

        SessionEntityWrapper<UserSessionEntity> tombstone = SessionEntityWrapper.createTombstoneMarker(live.getEntity());
        cache.put(key, tombstone);

        awaitRemoval(key);
        assertNull("Tombstone written over a live entry must be observed and removed", cache.get(key));

        // Verify the key is now guarded: a stale re-insertion should be removed.
        cache.putIfAbsent(key, live);
        awaitRemoval(key);
        assertNull("Re-insertion after tombstone-via-modify must be treated as resurrection", cache.get(key));
    }

    private static SessionEntityWrapper<UserSessionEntity> wrap(String id, int started) {
        return new SessionEntityWrapper<>(entity(id, started));
    }

    private static UserSessionEntity entity(String id, int started) {
        UserSessionEntity entity = new UserSessionEntity(id);
        entity.setStarted(started);
        return entity;
    }

    /**
     * The guard acts asynchronously ({@code Cache#removeAsync}), so its effect may not be visible
     * immediately after the triggering cache operation returns.
     */
    private void awaitRemoval(String key) {
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> cache.get(key) == null);
    }
}
