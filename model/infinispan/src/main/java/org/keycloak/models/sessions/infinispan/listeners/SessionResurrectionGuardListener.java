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

import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

import org.keycloak.common.util.Time;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.jboss.logging.Logger;

/**
 * Minimal, additive guard against session resurrection (see GH issue #51127).
 * Should be removed once GH issue #53251 is in place.
 * <p>
 * A concurrent cache reader can race a delete: it observes a cache miss, loads the (still present,
 * pre-delete) row from the database, and re-inserts it via {@code putIfAbsent()} after the delete has
 * already removed the entry from the cache. This listener closes that window, in cooperation with a
 * small write-path change: on removal, {@code InfinispanChangesUtils} writes a short-lived tombstone
 * marker in place of a bare {@code remove()} (see {@link SessionEntityWrapper#createTombstoneMarker}).
 * This guarantees a notification is observed for every deletion - even when the key wasn't cached at
 * removal time, so a plain {@code @CacheEntryRemoved} listener would never fire.
 * <p>
 * This listener only observes insertions/updates: when it sees a tombstone marker appear, it remembers
 * the key for a short grace period and immediately removes the marker again (restoring the normal
 * "absent" state for genuine reads). If a real entity re-appears at that key within the grace period, it
 * is treated as a resurrection and removed again. Since it never observes removals, a bulk
 * {@code Cache#clear()} (e.g. cluster-merge handling clearing caches to force a reload of potentially
 * stale data) is transparent to it - {@code clear()} only fires removal notifications, never
 * create/modify ones.
 * <p>
 * Only used for the user session caches (not client sessions). Online user session keys are random IDs
 * that are never intentionally reused after a delete, so a real entity re-appearing at a recently-
 * tombstoned online-session key can be assumed to be a resurrection. Offline session keys, however, are
 * <em>not</em> unique to a single incarnation: {@code UserSessionManager#createOrUpdateOfflineSession}
 * stores an offline session under its originating online session's ID, so revoking offline access and
 * re-granting it shortly afterwards legitimately reuses a key that was just tombstoned. To tell the two
 * cases apart, a tombstoned key also records the removed entity's {@code started} time; a real entity
 * re-appearing at that key is only treated as a resurrection if its own {@code started} is at or before
 * the removed entity's - a later {@code started} is a genuine newer incarnation and is left alone.
 * <p>
 * In addition to the per-key tombstone above, {@link #recordRealmNotBefore(String)} supports a
 * per-realm "not-before" watermark for bulk removals (e.g. "logout all sessions in a realm") where
 * writing a tombstone per removed key would not scale. Any real {@link UserSessionEntity} whose
 * {@code started} timestamp is at or before a realm's recorded watermark is treated the same way as a
 * per-key resurrection and removed again. The watermark is recorded before the corresponding bulk local
 * removal runs (see {@code InfinispanUserSessionProviderFactory#registerClusterListeners}), and that
 * removal only ever touches the local, in-memory embedded cache (no database or network I/O), so it
 * completes well within the {@link #TOMBSTONE_TTL_SECONDS} grace period even for realms with a very
 * large number of sessions.
 * <p>
 * This is deliberately not a complete fix for cache consistency (see the loading-marker + CAS pattern in
 * the full upstream fix); it only prevents deleted entries from reappearing in the cache.
 */
@Listener(primaryOnly = true, observation = Listener.Observation.POST)
public class SessionResurrectionGuardListener<K, V extends SessionEntity> {

    /** Grace period, in seconds, during which a tombstoned key is guarded against resurrection. */
    static final long TOMBSTONE_TTL_SECONDS = 30;

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    // Tracks, for keys with a recently observed tombstone marker, the removed entity's "started" time -
    // for a short grace period. A real entity re-appearing at that key is only a resurrection if its own
    // "started" is at or before the removed entity's; a later "started" means a legitimate newer
    // incarnation (see class javadoc: offline sessions reuse their online session's key).
    private final Cache<K, Integer> tombstonedKeys = Caffeine.newBuilder()
            .expireAfterWrite(TOMBSTONE_TTL_SECONDS, TimeUnit.SECONDS)
            .ticker(() -> TimeUnit.MILLISECONDS.toNanos(Time.currentTimeMillis()))
            .build();

    // Tracks, per realm, the time of the most recent bulk removal ("logout all sessions in a realm"), for
    // the same short grace period. Any user session with a started time at or before the watermark is
    // treated as a resurrection if it (re-)appears.
    private final Cache<String, Integer> realmNotBefore = Caffeine.newBuilder()
            .expireAfterWrite(TOMBSTONE_TTL_SECONDS, TimeUnit.SECONDS)
            .ticker(() -> TimeUnit.MILLISECONDS.toNanos(Time.currentTimeMillis()))
            .build();

    /**
     * Records that all user sessions in {@code realmId} that existed prior to this call are being bulk
     * removed. Called from the node handling a "logout all sessions in a realm" cluster event, on every
     * node in the cluster (each node executes that removal locally), so the watermark ends up recorded on
     * whichever node later ends up as primary owner of a resurrected key.
     */
    public void recordRealmNotBefore(String realmId) {
        realmNotBefore.put(realmId, Time.currentTime());
    }

    @CacheEntryCreated
    public void onCreated(CacheEntryCreatedEvent<K, SessionEntityWrapper<V>> event) {
        handle(event.getCache(), event.getKey(), event.getValue());
    }

    @CacheEntryModified
    public void onModified(CacheEntryModifiedEvent<K, SessionEntityWrapper<V>> event) {
        handle(event.getCache(), event.getKey(), event.getNewValue());
    }

    private void handle(org.infinispan.Cache<K, SessionEntityWrapper<V>> cache, K key, SessionEntityWrapper<V> value) {
        if (value == null) {
            return;
        }
        if (value.isTombstoneMarker()) {
            tombstonedKeys.put(key, tombstonedStartedOf(value.getEntity()));
            // Restores the "absent" state expected by genuine reads. The tombstone marker also carries
            // its own short lifespan as a safety net in case this removal doesn't happen for any reason.
            cache.removeAsync(key, value);
            return;
        }
        V entity = value.getEntity();
        if (entity == null) {
            return;
        }
        boolean keyResurrection = isAtOrBeforeTombstonedStarted(key, entity);
        boolean realmResurrection = !keyResurrection && isBeforeRealmNotBefore(entity);
        if (!keyResurrection && !realmResurrection) {
            return;
        }
        logger.infof("Detected resurrection of previously removed cache entry '%s' in cache '%s' - removing it again",
                key, cache.getName());
        // Conditional remove: only delete if the entry still holds exactly the resurrected value. This
        // avoids discarding a legitimate write that may have landed on the same key in the meantime.
        cache.removeAsync(key, value).whenComplete((removed, error) -> {
            if (error != null) {
                logger.warnf(error, "Failed to remove resurrected cache entry '%s' in cache '%s'", key, cache.getName());
            }
        });
    }

    /**
     * Whether {@code entity} was created at or before the recorded "not-before" watermark for its realm,
     * meaning it existed prior to a bulk "logout all sessions in a realm" removal and should not have
     * survived (or reappeared after) that removal.
     */
    private boolean isBeforeRealmNotBefore(V entity) {
        if (!(entity instanceof UserSessionEntity userSessionEntity) || entity.getRealmId() == null) {
            return false;
        }
        Integer notBefore = realmNotBefore.getIfPresent(entity.getRealmId());
        return notBefore != null && userSessionEntity.getStarted() <= notBefore;
    }

    /**
     * Whether {@code entity} (re-)appearing at {@code key} is a resurrection of a recently tombstoned
     * entry at that same key, rather than a legitimate newer incarnation. Offline sessions are stored
     * under the same key as their originating online session (see class javadoc), so revoking and
     * re-granting offline access within the grace period can legitimately reuse a tombstoned key; the
     * "started" time distinguishes the two cases the same way the realm watermark does.
     */
    private boolean isAtOrBeforeTombstonedStarted(K key, V entity) {
        Integer tombstonedStarted = tombstonedKeys.getIfPresent(key);
        if (tombstonedStarted == null) {
            return false;
        }
        if (!(entity instanceof UserSessionEntity userSessionEntity)) {
            // No discriminator available for the reappearing entity - fall back to the pre-existing
            // conservative behavior and treat any reappearance at a tombstoned key as a resurrection.
            return true;
        }
        return userSessionEntity.getStarted() <= tombstonedStarted;
    }

    /**
     * The removed entity's "started" time to record for a tombstoned key, or {@link Integer#MAX_VALUE} if
     * unavailable (not a {@link UserSessionEntity}, or {@code null}) so that, absent a discriminator, any
     * reappearance at that key is still conservatively treated as a resurrection (the pre-existing
     * behavior).
     */
    private static int tombstonedStartedOf(SessionEntity removedEntity) {
        return removedEntity instanceof UserSessionEntity userSessionEntity ? userSessionEntity.getStarted() : Integer.MAX_VALUE;
    }
}
