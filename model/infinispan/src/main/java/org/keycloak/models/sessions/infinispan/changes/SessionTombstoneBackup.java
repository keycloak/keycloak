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

package org.keycloak.models.sessions.infinispan.changes;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SingleUseObjectValueEntity;

import org.infinispan.Cache;

/**
 * Backup tombstone markers in the action token cache (unbounded, no eviction) to guard against
 * tombstone eviction from bounded session caches.
 * <p>
 * The session cache tombstone provides atomic protection via {@code putIfAbsent}. This backup
 * ensures that if the tombstone is evicted from a bounded session cache, the import path can
 * still detect the deletion.
 */
public final class SessionTombstoneBackup {

    private static final String KEY_PREFIX = "tomb:";
    private static final String TIMESTAMP_NOTE = "ts";
    static final long TOMBSTONE_LIFESPAN_MS = 30_000;

    private SessionTombstoneBackup() {
    }

    static <K, V extends SessionEntity> CompletionStage<Void> writeAsync(
            Cache<String, SingleUseObjectValueEntity> backupCache,
            String cacheName,
            K key,
            SessionEntityWrapper<V> sessionWrapper) {
        String backupKey = buildKey(cacheName, key);
        Map<String, String> notes = buildNotes(sessionWrapper);
        var entity = new SingleUseObjectValueEntity(notes);
        return backupCache.putAsync(backupKey, entity, TOMBSTONE_LIFESPAN_MS, TimeUnit.MILLISECONDS)
                .thenRun(() -> {});
    }

    static <K, V extends SessionEntity> boolean isBlockingImport(
            Cache<String, SingleUseObjectValueEntity> backupCache,
            String cacheName,
            K key,
            SessionEntityWrapper<V> candidate) {
        if (backupCache == null) {
            return false;
        }
        String backupKey = buildKey(cacheName, key);
        SingleUseObjectValueEntity marker = backupCache.get(backupKey);
        if (marker == null) {
            return false;
        }
        String tombstoneStarted = marker.getNote(TIMESTAMP_NOTE);
        if (tombstoneStarted != null && candidate != null
                && candidate.getEntity() instanceof AuthenticatedClientSessionEntity clientSession) {
            return tombstoneStarted.equals(String.valueOf(clientSession.getStarted()));
        }
        return true;
    }

    static Cache<String, SingleUseObjectValueEntity> getBackupCache(KeycloakSession session) {
        return session.getProvider(InfinispanConnectionProvider.class)
                .getCache(InfinispanConnectionProvider.ACTION_TOKEN_CACHE);
    }

    private static <K> String buildKey(String cacheName, K key) {
        return KEY_PREFIX + cacheName + ":" + key;
    }

    private static <V extends SessionEntity> Map<String, String> buildNotes(SessionEntityWrapper<V> sessionWrapper) {
        if (sessionWrapper.getEntity() instanceof AuthenticatedClientSessionEntity clientSession) {
            return Map.of(TIMESTAMP_NOTE, String.valueOf(clientSession.getStarted()));
        }
        return Map.of();
    }
}
