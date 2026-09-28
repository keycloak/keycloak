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
 * Only used for the user session caches (not client sessions): user session keys are random IDs that
 * are never intentionally reused after a delete, so any real entity re-appearing at a recently-tombstoned
 * key can be assumed to be a resurrection, with no need for a version/discriminator check.
 * <p>
 * This is deliberately not a complete fix for cache consistency (see the loading-marker + CAS pattern in
 * the full upstream fix); it only prevents deleted entries from reappearing in the cache.
 */
@Listener(primaryOnly = true, observation = Listener.Observation.POST)
public class SessionResurrectionGuardListener<K, V extends SessionEntity> {

    /** Grace period, in seconds, during which a tombstoned key is guarded against resurrection. */
    static final long TOMBSTONE_TTL_SECONDS = 30;

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    // Tracks keys for which a tombstone marker was recently observed, for a short grace period.
    private final Cache<K, Boolean> tombstonedKeys = Caffeine.newBuilder()
            .expireAfterWrite(TOMBSTONE_TTL_SECONDS, TimeUnit.SECONDS)
            .ticker(() -> TimeUnit.MILLISECONDS.toNanos(Time.currentTimeMillis()))
            .build();

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
            tombstonedKeys.put(key, Boolean.TRUE);
            // Restores the "absent" state expected by genuine reads. The tombstone marker also carries
            // its own short lifespan as a safety net in case this removal doesn't happen for any reason.
            cache.removeAsync(key, value);
            return;
        }
        if (tombstonedKeys.getIfPresent(key) == null || value.getEntity() == null) {
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
}
