/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.sessions.infinispan.changes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import org.keycloak.common.util.Time;
import org.keycloak.marshalling.KeycloakModelSchema;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.transaction.DatabaseUpdate;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;

import org.infinispan.Cache;
import org.infinispan.cache.impl.AbstractDelegatingCache;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.util.concurrent.ActionSequencer;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Test;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Focused tests for the security properties of loading-marker publication and
 * the maximum lifespan of database-backed cache entries.
 */
public class SessionLoadingMarkerTest {

    private static final Logger LOG = Logger.getLogger(SessionLoadingMarkerTest.class);
    private static final String SESSION_ID = "session-id";
    private static final long MAX_CACHE_LIFESPAN_MS = 10_000;

    @After
    public void resetTimeOffset() {
        Time.setOffset(0);
    }

    @Test
    public void staleDatabaseLoadCannotReplaceMarkerRemovedByLogout() throws Exception {
        try (DefaultCacheManager cacheManager = new DefaultCacheManager()) {
            cacheManager.defineConfiguration("sessions", new ConfigurationBuilder().build());
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = cacheManager.getCache("sessions");

            UserSessionEntity markerEntity = new UserSessionEntity(SESSION_ID);
            markerEntity.setRealmId("realm-id");
            SessionEntityWrapper<UserSessionEntity> marker = SessionEntityWrapper.createLoadingMarker(markerEntity);

            assertNull(cache.putIfAbsent(SESSION_ID, marker,
                    SessionEntityWrapper.LOADING_MARKER_LIFESPAN_MS, TimeUnit.MILLISECONDS));

            // The reader now owns the marker and has loaded this snapshot from the DB.
            UserSessionEntity staleDatabaseSnapshot = new UserSessionEntity(SESSION_ID);
            staleDatabaseSnapshot.setRealmId("realm-id");
            SessionEntityWrapper<UserSessionEntity> staleWrapper = new SessionEntityWrapper<>(staleDatabaseSnapshot);

            // A concurrent logout commits the DB delete and then removes the marker.
            cache.remove(SESSION_ID);

            // Releasing the delayed reader at the exact publication boundary must not
            // resurrect its stale snapshot. It may remain request-local only.
            boolean published = cache.replace(SESSION_ID, marker, staleWrapper,
                    60, TimeUnit.SECONDS);

            assertFalse("CAS publication must fail after logout removed the marker", published);
            assertNull("The stale session must not be present in shared cache state", cache.get(SESSION_ID));
        }
    }

    @Test
    public void clientMutationRemovesOnlyParentLoadingMarker() throws Exception {
        try (DefaultCacheManager cacheManager = new DefaultCacheManager()) {
            cacheManager.defineConfiguration("sessions", new ConfigurationBuilder().build());
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = cacheManager.getCache("sessions");

            UserSessionEntity entity = new UserSessionEntity(SESSION_ID);
            SessionEntityWrapper<UserSessionEntity> marker = SessionEntityWrapper.createLoadingMarker(entity);
            cache.put(SESSION_ID, marker);

            assertTrue(InfinispanChangesUtils.removeLoadingMarker(cache, SESSION_ID).toCompletableFuture().get());
            assertNull(cache.get(SESSION_ID));

            SessionEntityWrapper<UserSessionEntity> session = new SessionEntityWrapper<>(entity);
            cache.put(SESSION_ID, session);

            assertFalse(InfinispanChangesUtils.removeLoadingMarker(cache, SESSION_ID).toCompletableFuture().get());
            assertEquals(session, cache.get(SESSION_ID));
        }
    }

    @Test
    public void databaseBackedCreationPublishesOnlyThroughItsFence() throws Exception {
        try (DefaultCacheManager cacheManager = new DefaultCacheManager()) {
            cacheManager.defineConfiguration("sessions", new ConfigurationBuilder().build());
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = cacheManager.getCache("sessions");

            UserSessionEntity entity = new UserSessionEntity(SESSION_ID);
            SessionEntityWrapper<UserSessionEntity> marker = SessionEntityWrapper.createLoadingMarker(entity);
            SessionEntityWrapper<UserSessionEntity> created = new SessionEntityWrapper<>(entity);
            cache.put(SESSION_ID, marker);

            assertEquals(Boolean.TRUE, InfinispanChangesUtils.publishCreatedSessionOrInvalidate(cache, SESSION_ID, marker, created,
                    TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5)).toCompletableFuture().get());
            assertEquals(created, cache.get(SESSION_ID));

            cache.put(SESSION_ID, marker);
            cache.remove(SESSION_ID, marker);

            assertEquals(Boolean.FALSE, InfinispanChangesUtils.publishCreatedSessionOrInvalidate(cache, SESSION_ID, marker, created,
                    TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5)).toCompletableFuture().get());
            assertNull("A concurrent delete must prevent late creation publication", cache.get(SESSION_ID));

            SessionEntityWrapper<UserSessionEntity> displaced = new SessionEntityWrapper<>(new UserSessionEntity(SESSION_ID));
            cache.put(SESSION_ID, displaced);
            assertEquals(Boolean.FALSE, InfinispanChangesUtils.publishCreatedSessionOrInvalidate(cache, SESSION_ID, marker, created,
                    TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5)).toCompletableFuture().get());
            assertNull("A value that displaced the creation fence may contain a pre-commit snapshot", cache.get(SESSION_ID));

            cache.put(SESSION_ID, marker);
            InfinispanChangesUtils.publishCreatedSessionOrInvalidate(cache, SESSION_ID, null, created,
                    TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5)).toCompletableFuture().get();
            assertNull("A marker owned by another transaction must be invalidated after commit", cache.get(SESSION_ID));

            SessionEntityWrapper<UserSessionEntity> existing = new SessionEntityWrapper<>(new UserSessionEntity(SESSION_ID));
            cache.put(SESSION_ID, existing);
            InfinispanChangesUtils.publishCreatedSessionOrInvalidate(cache, SESSION_ID, null, created,
                    TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5)).toCompletableFuture().get();
            assertNull("A complete pre-existing entry must not be overwritten by an incomplete creation snapshot", cache.get(SESSION_ID));
        }
    }

    @Test
    public void failedMarkerCasHasExplicitRequestLocalResult() throws Exception {
        try (DefaultCacheManager cacheManager = new DefaultCacheManager()) {
            cacheManager.defineConfiguration("sessions", new ConfigurationBuilder().build());
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = cacheManager.getCache("sessions");
            CacheHolder<String, UserSessionEntity> cacheHolder = new CacheHolder<>(cache, directSequencer(),
                    (realm, client, entity) -> TimeUnit.MINUTES.toMillis(5),
                    (realm, client, entity) -> TimeUnit.MINUTES.toMillis(5), null);
            InfinispanChangelogBasedTransaction<String, UserSessionEntity> transaction =
                    new InfinispanChangelogBasedTransaction<>(null, cacheHolder);

            UserSessionEntity markerEntity = new UserSessionEntity(SESSION_ID);
            assertNull(transaction.placeLoadingMarker(SESSION_ID, markerEntity));
            cache.remove(SESSION_ID);

            UserSessionEntity loaded = new UserSessionEntity(SESSION_ID);
            SessionImportResult<UserSessionEntity> result = transaction.importSession(null, SESSION_ID,
                    new SessionEntityWrapper<>(loaded), TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5));

            assertEquals(SessionImportResult.Status.REQUEST_LOCAL, result.status());
            assertNull(cache.get(SESSION_ID));
        }
    }

    @Test
    public void markerlessDatabaseSnapshotRemainsRequestLocal() throws Exception {
        try (DefaultCacheManager cacheManager = new DefaultCacheManager()) {
            cacheManager.defineConfiguration("sessions", new ConfigurationBuilder().build());
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = cacheManager.getCache("sessions");
            CacheHolder<String, UserSessionEntity> cacheHolder = new CacheHolder<>(cache, directSequencer(),
                    (realm, client, entity) -> TimeUnit.MINUTES.toMillis(5),
                    (realm, client, entity) -> TimeUnit.MINUTES.toMillis(5), null);
            InfinispanChangelogBasedTransaction<String, UserSessionEntity> transaction =
                    new InfinispanChangelogBasedTransaction<>(null, cacheHolder);

            UserSessionEntity loaded = new UserSessionEntity(SESSION_ID);
            SessionImportResult<UserSessionEntity> result = transaction.importSession(null, SESSION_ID,
                    new SessionEntityWrapper<>(loaded), TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5));

            assertEquals(SessionImportResult.Status.REQUEST_LOCAL, result.status());
            assertNull("A snapshot read without a pre-read marker must not be published", cache.get(SESSION_ID));
        }
    }

    @Test
    public void databaseReplacementKeepsUnchangedCacheEntryWarm() throws Exception {
        try (DefaultCacheManager cacheManager = new DefaultCacheManager()) {
            cacheManager.defineConfiguration("sessions", new ConfigurationBuilder().build());
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = cacheManager.getCache("sessions");
            CacheHolder<String, UserSessionEntity> cacheHolder = new CacheHolder<>(cache, directSequencer(),
                    (realm, client, entity) -> TimeUnit.MINUTES.toMillis(5),
                    (realm, client, entity) -> TimeUnit.MINUTES.toMillis(5), null);

            UserSessionEntity entity = new UserSessionEntity(SESSION_ID);
            SessionEntityWrapper<UserSessionEntity> expected = new SessionEntityWrapper<>(entity);
            cache.put(SESSION_ID, expected);

            InfinispanChangesUtils.replaceAfterDatabaseCommit(
                            cacheHolder, SESSION_ID, persistentReplace(expected), expected, LOG)
                    .toCompletableFuture().get();

            SessionEntityWrapper<UserSessionEntity> published = cache.get(SESSION_ID);
            assertTrue("An unchanged cache entry should remain warm", published != null);
            assertFalse("Publication should replace the wrapper UUID",
                    expected.getVersion().equals(published.getVersion()));
        }
    }

    @Test
    public void databaseReplacementInvalidatesChangedCacheEntryInsteadOfReplaying() throws Exception {
        try (DefaultCacheManager cacheManager = new DefaultCacheManager()) {
            cacheManager.defineConfiguration("sessions", new ConfigurationBuilder().build());
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = cacheManager.getCache("sessions");
            CacheHolder<String, UserSessionEntity> cacheHolder = new CacheHolder<>(cache, directSequencer(),
                    (realm, client, entity) -> TimeUnit.MINUTES.toMillis(5),
                    (realm, client, entity) -> TimeUnit.MINUTES.toMillis(5), null);

            SessionEntityWrapper<UserSessionEntity> expected =
                    new SessionEntityWrapper<>(new UserSessionEntity(SESSION_ID));
            SessionEntityWrapper<UserSessionEntity> concurrentlyPublished =
                    new SessionEntityWrapper<>(new UserSessionEntity(SESSION_ID));
            cache.put(SESSION_ID, concurrentlyPublished);

            InfinispanChangesUtils.replaceAfterDatabaseCommit(
                            cacheHolder, SESSION_ID, persistentReplace(expected), expected, LOG)
                    .toCompletableFuture().get();

            assertNull("A changed UUID must be invalidated instead of receiving a reordered database update",
                    cache.get(SESSION_ID));
        }
    }

    @Test
    public void databaseReplacementInvalidatesConflictingLoadingMarker() throws Exception {
        try (DefaultCacheManager cacheManager = new DefaultCacheManager()) {
            cacheManager.defineConfiguration("sessions", new ConfigurationBuilder().build());
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = cacheManager.getCache("sessions");
            CacheHolder<String, UserSessionEntity> cacheHolder = new CacheHolder<>(cache, directSequencer(),
                    (realm, client, entity) -> TimeUnit.MINUTES.toMillis(5),
                    (realm, client, entity) -> TimeUnit.MINUTES.toMillis(5), null);

            SessionEntityWrapper<UserSessionEntity> expected =
                    new SessionEntityWrapper<>(new UserSessionEntity(SESSION_ID));
            SessionEntityWrapper<UserSessionEntity> marker =
                    SessionEntityWrapper.createLoadingMarker(new UserSessionEntity(SESSION_ID));
            cache.put(SESSION_ID, marker);

            InfinispanChangesUtils.replaceAfterDatabaseCommit(
                            cacheHolder, SESSION_ID, persistentReplace(expected), expected, LOG)
                    .toCompletableFuture().get();

            assertNull("A marker for a pre-commit load must be consumed after a database replacement",
                    cache.get(SESSION_ID));
            assertFalse("The consumed marker must not be able to publish its stale snapshot",
                    cache.replace(SESSION_ID, marker,
                            new SessionEntityWrapper<>(new UserSessionEntity(SESSION_ID))));
        }
    }

    @Test
    public void databaseReplacementPreventsPublicationBetweenComputeAndCompletion() throws Exception {
        try (DefaultCacheManager cacheManager = new DefaultCacheManager()) {
            cacheManager.defineConfiguration("sessions", new ConfigurationBuilder().build());
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = cacheManager.getCache("sessions");
            var expected = new SessionEntityWrapper<>(new UserSessionEntity(SESSION_ID));
            var marker = SessionEntityWrapper.createLoadingMarker(new UserSessionEntity(SESSION_ID));
            var stale = new SessionEntityWrapper<>(new UserSessionEntity(SESSION_ID));
            cache.put(SESSION_ID, marker);
            AtomicBoolean publicationAttempted = new AtomicBoolean();
            AtomicBoolean published = new AtomicBoolean();

            // The compute has released the key, but the writer has not received its result yet.
            // In the old implementation, this is where the marker owner could publish before
            // the writer attempted remove(key, observedMarker), which would then miss.
            Cache<String, SessionEntityWrapper<UserSessionEntity>> intercepted =
                    afterCompute(cache, () -> {
                        publicationAttempted.set(true);
                        published.set(cache.replace(SESSION_ID, marker, stale));
                    });
            var holder = new CacheHolder<>(intercepted, directSequencer(),
                    (realm, client, entity) -> TimeUnit.MINUTES.toMillis(5),
                    (realm, client, entity) -> TimeUnit.MINUTES.toMillis(5), null);

            InfinispanChangesUtils.replaceAfterDatabaseCommit(holder, SESSION_ID,
                    persistentReplace(expected), expected, LOG).toCompletableFuture().get(10, TimeUnit.SECONDS);

            assertTrue("The stale reader must run at the compute/completion boundary", publicationAttempted.get());
            assertFalse("Conflict handling must consume the marker before the reader can publish", published.get());
            assertNull("The writer must not finish with the stale snapshot cached", cache.get(SESSION_ID));
        }
    }

    @Test
    public void databaseReplacementPreservesPublicationAfterAtomicInvalidation() throws Exception {
        try (DefaultCacheManager cacheManager = new DefaultCacheManager()) {
            cacheManager.defineConfiguration("sessions", new ConfigurationBuilder().build());
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = cacheManager.getCache("sessions");
            var expected = new SessionEntityWrapper<>(new UserSessionEntity(SESSION_ID));
            cache.put(SESSION_ID, SessionEntityWrapper.createLoadingMarker(new UserSessionEntity(SESSION_ID)));
            var fresh = new SessionEntityWrapper<>(new UserSessionEntity(SESSION_ID));
            Cache<String, SessionEntityWrapper<UserSessionEntity>> intercepted =
                    afterCompute(cache, () -> {
                        var nextMarker = SessionEntityWrapper.createLoadingMarker(new UserSessionEntity(SESSION_ID));
                        assertNull("Atomic conflict handling should already have removed the old marker",
                                cache.putIfAbsent(SESSION_ID, nextMarker));
                        // This reader acquired its marker after invalidation and read committed DB state.
                        assertTrue(cache.replace(SESSION_ID, nextMarker, fresh));
                    });
            var holder = new CacheHolder<>(intercepted, directSequencer(),
                    (realm, client, entity) -> TimeUnit.MINUTES.toMillis(5),
                    (realm, client, entity) -> TimeUnit.MINUTES.toMillis(5), null);

            InfinispanChangesUtils.replaceAfterDatabaseCommit(holder, SESSION_ID,
                    persistentReplace(expected), expected, LOG).toCompletableFuture().get(10, TimeUnit.SECONDS);

            assertEquals("Writer completion must not remove a later publication", fresh, cache.get(SESSION_ID));
        }
    }

    @Test
    public void replacementConflictModeSurvivesMarshalling() throws Exception {
        var context = ProtobufUtil.newSerializationContext();
        KeycloakModelSchema.INSTANCE.registerSchema(context);
        KeycloakModelSchema.INSTANCE.registerMarshallers(context);
        var expected = new SessionEntityWrapper<>(new UserSessionEntity(SESSION_ID));
        var replacement = new SessionEntityWrapper<>(new UserSessionEntity(SESSION_ID));
        var conflicting = SessionEntityWrapper.createLoadingMarker(new UserSessionEntity(SESSION_ID));

        for (boolean removeOnMismatch : new boolean[] { false, true }) {
            var function = new ReplaceFunction<String, UserSessionEntity>(expected.getVersion(), replacement, removeOnMismatch);
            @SuppressWarnings("unchecked")
            var decoded = (ReplaceFunction<String, UserSessionEntity>) ProtobufUtil.fromWrappedByteArray(context,
                    ProtobufUtil.toWrappedByteArray(context, function));
            assertEquals(replacement.getVersion(), decoded.apply(SESSION_ID, expected).getVersion());
            assertEquals(removeOnMismatch ? null : conflicting, decoded.apply(SESSION_ID, conflicting));
        }
    }

    @Test
    public void volatileOfflinePersistedReplacementDoesNotReplayOnNewerEntry() throws Exception {
        verifyVolatileOfflineReplacementRouting(true);
    }

    @Test
    public void volatileOfflineCacheOnlyReplacementRetainsRetryBehavior() throws Exception {
        verifyVolatileOfflineReplacementRouting(false);
    }

    private void verifyVolatileOfflineReplacementRouting(boolean persist) throws Exception {
        try (DefaultCacheManager cacheManager = new DefaultCacheManager()) {
            cacheManager.defineConfiguration("sessions", new ConfigurationBuilder().build());
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = cacheManager.getCache("sessions");
            var holder = new CacheHolder<>(cache, directSequencer(),
                    (realm, client, entity) -> TimeUnit.MINUTES.toMillis(5),
                    (realm, client, entity) -> TimeUnit.MINUTES.toMillis(5), null);
            var tx = new InfinispanChangelogBasedTransaction<String, UserSessionEntity>(null, holder);
            tx.setPersistToDatabaseCacheName(OFFLINE_USER_SESSION_CACHE_NAME);
            var entity = new UserSessionEntity(SESSION_ID);
            entity.setOffline(true);
            var updates = new SessionUpdatesList<>(null, new SessionEntityWrapper<>(entity));
            updates.addAndExecute(new SessionUpdateTask<>() {
                @Override
                public void runUpdate(UserSessionEntity value) {
                    value.getNotes().put("update", "earlier");
                }

                @Override
                public CacheOperation getOperation() {
                    return CacheOperation.REPLACE;
                }

                @Override
                public boolean requiresDatabasePersistence() {
                    return persist;
                }
            });
            tx.updates.put(SESSION_ID, updates);
            var newerEntity = new UserSessionEntity(SESSION_ID);
            newerEntity.getNotes().put("update", "later");
            var newer = new SessionEntityWrapper<>(newerEntity);
            cache.put(SESSION_ID, newer);

            List<DatabaseUpdate> writes = new ArrayList<>();
            var prepare = CompletionStages.<Void>aggregateCompletionStage();
            tx.asyncCommit(prepare, writes::add);
            prepare.freeze().toCompletableFuture().get(10, TimeUnit.SECONDS);
            assertEquals(persist ? 1 : 0, writes.size());
            assertEquals("Preparation must defer cache publication", newer, cache.get(SESSION_ID));

            // This is a dispatch test: simulate completion of the queued database work.
            // Actual database commit ordering requires separate integration coverage.
            var committed = CompletionStages.<Void>aggregateCompletionStage();
            tx.asyncPostDatabaseCommit(committed);
            committed.freeze().toCompletableFuture().get(10, TimeUnit.SECONDS);
            if (persist) {
                assertNull("A persisted mutation must invalidate the changed UUID without replay", cache.get(SESSION_ID));
            } else {
                assertEquals("earlier", cache.get(SESSION_ID).getEntity().getNotes().get("update"));
            }
        }
    }

    private static Cache<String, SessionEntityWrapper<UserSessionEntity>> afterCompute(
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache, Runnable action) {
        return new AbstractDelegatingCache<>(cache) {
            @Override
            public CompletableFuture<SessionEntityWrapper<UserSessionEntity>> computeIfPresentAsync(String key,
                    BiFunction<? super String, ? super SessionEntityWrapper<UserSessionEntity>,
                            ? extends SessionEntityWrapper<UserSessionEntity>> function,
                    long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
                return super.computeIfPresentAsync(key, function, lifespan, lifespanUnit, maxIdle, maxIdleUnit)
                        .thenApply(result -> {
                            action.run();
                            return result;
                        });
            }
        };
    }

    private static MergedUpdate<UserSessionEntity> persistentReplace(SessionEntityWrapper<UserSessionEntity> wrapper) {
        PersistentSessionUpdateTask<UserSessionEntity> replace = new PersistentSessionUpdateTask<>() {
            @Override
            public boolean isOffline() {
                return false;
            }

            @Override
            public void runUpdate(UserSessionEntity entity) {
            }

            @Override
            public CacheOperation getOperation() {
                return CacheOperation.REPLACE;
            }
        };
        return MergedUpdate.computeUpdate(List.of(replace), wrapper,
                TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5));
    }

    private static ActionSequencer directSequencer() {
        return new ActionSequencer(Runnable::run, false, null);
    }

    @Test
    public void effectiveCacheOnlyRemoveDoesNotRequireDatabasePersistence() {
        UserSessionEntity entity = new UserSessionEntity(SESSION_ID);
        entity.setOffline(true);
        SessionEntityWrapper<UserSessionEntity> wrapper = new SessionEntityWrapper<>(entity);
        PersistentSessionUpdateTask<UserSessionEntity> replace = new PersistentSessionUpdateTask<>() {
            @Override
            public boolean isOffline() {
                return true;
            }

            @Override
            public void runUpdate(UserSessionEntity entity) {
            }

            @Override
            public CacheOperation getOperation() {
                return CacheOperation.REPLACE;
            }
        };

        MergedUpdate<UserSessionEntity> merged = MergedUpdate.computeUpdate(
                List.of(replace, Tasks.removeSyncCacheOnly(true)), wrapper,
                TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5));

        assertEquals(SessionUpdateTask.CacheOperation.REMOVE, merged.getOperation());
        assertFalse(merged.requiresDatabasePersistence());
    }

    @Test
    public void replacementsCannotExtendMaximumCacheLifespan() {
        UserSessionEntity entity = new UserSessionEntity(SESSION_ID);
        SessionEntityWrapper<UserSessionEntity> initial = new SessionEntityWrapper<>(entity);
        initial.getLocalMetadata().put(SessionEntityWrapper.CACHED_AT_KEY, String.valueOf(Time.currentTimeMillis()));

        long initialRemaining = SessionEntityWrapper.capLifespan(
                SessionTimeouts.IMMORTAL_FLAG, MAX_CACHE_LIFESPAN_MS, initial);

        Time.setOffset(3);
        SessionEntityWrapper<UserSessionEntity> firstReplacement =
                new SessionEntityWrapper<>(initial.getLocalMetadata(), entity);
        long afterFirstReplace = SessionEntityWrapper.capLifespan(
                SessionTimeouts.IMMORTAL_FLAG, MAX_CACHE_LIFESPAN_MS, firstReplacement);

        Time.setOffset(6);
        SessionEntityWrapper<UserSessionEntity> secondReplacement =
                new SessionEntityWrapper<>(firstReplacement.getLocalMetadata(), entity);
        long afterSecondReplace = SessionEntityWrapper.capLifespan(
                SessionTimeouts.IMMORTAL_FLAG, MAX_CACHE_LIFESPAN_MS, secondReplacement);

        assertTrue("Initial lifespan should be capped", initialRemaining <= MAX_CACHE_LIFESPAN_MS);
        assertTrue("First replacement must retain the original cachedAt deadline",
                afterFirstReplace <= initialRemaining - TimeUnit.SECONDS.toMillis(2));
        assertTrue("Second replacement must not restart the cap window",
                afterSecondReplace <= afterFirstReplace - TimeUnit.SECONDS.toMillis(2));
        assertTrue("The entry should still have time remaining before the original deadline",
                afterSecondReplace > 0);
    }

    @Test
    public void disabledMaximumCacheLifespanDoesNotClampSessionLifespan() {
        UserSessionEntity entity = new UserSessionEntity(SESSION_ID);
        SessionEntityWrapper<UserSessionEntity> wrapper = new SessionEntityWrapper<>(entity);
        wrapper.getLocalMetadata().put(SessionEntityWrapper.CACHED_AT_KEY, String.valueOf(Time.currentTimeMillis()));
        long requestedLifespan = TimeUnit.HOURS.toMillis(12);

        Time.setOffset(60);

        long effectiveLifespan = SessionEntityWrapper.capLifespan(
                requestedLifespan, Long.MAX_VALUE, wrapper);

        assertTrue("A disabled cap must preserve the configured session lifespan",
                effectiveLifespan == requestedLifespan);
    }
}
