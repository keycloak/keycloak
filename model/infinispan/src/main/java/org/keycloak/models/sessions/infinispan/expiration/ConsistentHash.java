/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.expiration;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import org.keycloak.models.RealmModel;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryExpired;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryExpiredEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryRemovedEvent;
import org.infinispan.commons.hash.MurmurHash3;
import org.jboss.logging.Logger;

/**
 * A consistent hash for expiration, relying on the external Infinispan.
 * <p>
 * Each Keycloak instance will add itself, periodically, to the remote cache, and it relies on the Hot Rod client
 * listener to keep the membership up to date.
 * <p>
 * During network partitions, it has a probability of two or more Keycloak instances to be assigned to the same realm.
 * In this scenario, we rely on the database lock to keep data consistent.
 * <p>
 * Keycloak instances starting and stopping information may not be available in real time, and it is possible some
 * realms not being checked during an iteration.
 */
@ClientListener(includeCurrentState = true)
class ConsistentHash {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private static final int MIN_HEARTBEAT_PERIOD_SECONDS = 30;
    private static final int LIFESPAN_MULTIPLIER = 3;
    private static final int HEARTBEATS_PER_EXPIRATION_ROUND = 4;
    private static final int STOP_TIMEOUT_MILLISECONDS = 500;
    private static final String MEMBER_KEY_PREFIX = "node:";

    private final Set<String> membership = ConcurrentHashMap.newKeySet();
    private final String nodeUUID;
    private final String nodeName;
    private final int heartBeatPeriodSeconds;
    private final int heartBeatLifespan;
    private final ScheduledExecutorService scheduledExecutorService;
    private final RemoteCache<String, String> cache;
    private volatile ScheduledFuture<?> schedule;


    private ConsistentHash(ScheduledExecutorService scheduledExecutorService, RemoteCache<String, String> cache, String nodeUUID, String nodeName, int heartBeatPeriodSeconds, int heartBeatLifespan) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.nodeName = nodeName;
        this.heartBeatPeriodSeconds = heartBeatPeriodSeconds;
        this.cache = cache;
        this.nodeUUID = MEMBER_KEY_PREFIX + nodeUUID;
        this.heartBeatLifespan = heartBeatLifespan;
    }

    static ConsistentHash create(RemoteCache<String, String> cache, ScheduledExecutorService scheduledExecutorService, String nodeUUID, String nodeName, int expirationPeriodSeconds) {
        int period = Math.max(MIN_HEARTBEAT_PERIOD_SECONDS, expirationPeriodSeconds / HEARTBEATS_PER_EXPIRATION_ROUND);
        int lifespan = period * LIFESPAN_MULTIPLIER;
        return new ConsistentHash(Objects.requireNonNull(scheduledExecutorService), Objects.requireNonNull(cache), nodeUUID, nodeName, period, lifespan);
    }

    void start() {
        if (schedule != null) {
            return;
        }
        sendHeartBeat();
        schedule = scheduledExecutorService.scheduleAtFixedRate(this::sendHeartBeat, heartBeatPeriodSeconds, heartBeatPeriodSeconds, TimeUnit.SECONDS);
        cache.addClientListener(this);
    }

    void stop() {
        var existing = schedule;
        if (existing == null) {
            return;
        }
        cache.removeClientListener(this);
        existing.cancel(true);
        schedule = null;
        try {
            cache.removeAsync(nodeUUID).get(STOP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            log.debugf("Exception caught during stop", e);
        }
    }

    Predicate<RealmModel> consistentHashSnapshot() {
        return new HashingPredicate(membership.stream().sorted().toList(), nodeUUID);
    }

    int size() {
        return membership.size();
    }

    @ClientCacheEntryCreated
    public void onKeycloakConnected(ClientCacheEntryCreatedEvent<String> event) {
        addKeycloakNode(event.getKey());
    }

    @ClientCacheEntryModified
    public void onHeartbeat(ClientCacheEntryModifiedEvent<String> event) {
        addKeycloakNode(event.getKey());
    }

    @ClientCacheEntryExpired
    public void onMissingHeartbeat(ClientCacheEntryExpiredEvent<String> event) {
        removeKeycloakNode(event.getKey());
    }

    @ClientCacheEntryRemoved
    public void onKeycloakDisconnect(ClientCacheEntryRemovedEvent<String> event) {
        removeKeycloakNode(event.getKey());
    }

    private void addKeycloakNode(String uuid) {
        if (uuid.startsWith(MEMBER_KEY_PREFIX)) {
            log.debugf("Adding a keycloak instance with ID: %s", uuid);
            membership.add(uuid);
        }
    }

    private void removeKeycloakNode(String uuid) {
        if (uuid.startsWith(MEMBER_KEY_PREFIX)) {
            log.debugf("Removing keycloak instance with ID: %s", uuid);
            membership.remove(uuid);
        }
    }

    private void sendHeartBeat() {
        cache.putAsync(nodeUUID, nodeName, heartBeatLifespan, TimeUnit.SECONDS);
        addKeycloakNode(nodeUUID);
    }

    private record HashingPredicate(List<String> members, String myUUID) implements Predicate<RealmModel> {

        @Override
        public boolean test(RealmModel realm) {
            var size = members.size();
            assert size > 0;
            var index = Math.abs(MurmurHash3.getInstance().hash(realm.getId())) % size;
            return myUUID.equals(members.get(index));
        }
    }
}
