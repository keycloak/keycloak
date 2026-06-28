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

package org.keycloak.cluster.infinispan;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.keycloak.cluster.StoredClusterEvent;
import org.keycloak.cluster.jpa.JpaClusterEventStoreProvider;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.infinispan.InfinispanConnectionProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.CacheDecorators;
import org.keycloak.timer.ScheduledTask;

import org.infinispan.Cache;
import org.infinispan.commons.marshall.Marshaller;
import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.WORK_CACHE_NAME;

/**
 * Polls the CLUSTER_EVENT database table for events addressed to this cluster,
 * replays them into the local Infinispan work cache, and deletes the consumed rows.
 */
public class DatabaseClusterEventPollerTask implements ScheduledTask {

    private static final Logger logger = Logger.getLogger(DatabaseClusterEventPollerTask.class);

    private static final int BATCH_SIZE = 100;
    private static final long STALE_EVENT_RETENTION_MS = TimeUnit.MINUTES.toMillis(5);

    private final String clusterName;
    private final Marshaller marshaller;
    private long staleEventHorizon;

    public DatabaseClusterEventPollerTask(String clusterName, Marshaller marshaller) {
        Objects.requireNonNull(clusterName);
        Objects.requireNonNull(marshaller);
        this.clusterName = clusterName;
        this.marshaller = marshaller;
    }

    @Override
    public void run(KeycloakSession session) {
        JpaClusterEventStoreProvider store = new JpaClusterEventStoreProvider(session);
        processEvents(session, store, clusterName, marshaller);
        cleanupStaleEvents(session, store);
    }

    static void processEvents(KeycloakSession session, JpaClusterEventStoreProvider store, String clusterName, Marshaller marshaller) {
        if (!shouldProcessEvents(session)) return;

        List<StoredClusterEvent> events = store.readEvents(clusterName, BATCH_SIZE);
        if (events.isEmpty()) {
            return;
        }

        Cache<String, Object> workCache = session.getProvider(InfinispanConnectionProvider.class)
                .getCache(WORK_CACHE_NAME);

        List<String> processedIds = new java.util.ArrayList<>();
        for (StoredClusterEvent event : events) {
            WrapperClusterEvent wrappedEvent;
            try {
                wrappedEvent = (WrapperClusterEvent) marshaller.objectFromByteBuffer(event.eventData());
            } catch (IOException | ClassNotFoundException e) {
                logger.warnf(e, "Failed to deserialize cluster event %s, skipping", event.id());
                processedIds.add(event.id());
                continue;
            }

            String key = SecretGenerator.getInstance().generateSecureID();

            if (logger.isTraceEnabled()) {
                logger.tracef("Replaying cluster event from DB: key=%s, event=%s", key, wrappedEvent);
            }

            CacheDecorators.ignoreReturnValues(workCache)
                    .put(key, wrappedEvent, 120, TimeUnit.SECONDS);
            processedIds.add(event.id());
        }

        if (!processedIds.isEmpty()) {
            store.deleteEvents(clusterName, processedIds);
            if (logger.isDebugEnabled()) {
                logger.debugf("Consumed and deleted %d cluster event(s) for cluster '%s'", processedIds.size(), clusterName);
            }
        }
    }

    private void cleanupStaleEvents(KeycloakSession session, JpaClusterEventStoreProvider store) {
        if (staleEventHorizon < Time.currentTimeMillis()) {
            if (!shouldProcessEvents(session)) return;
            store.deleteEventsOlderThan(Time.currentTimeMillis() - STALE_EVENT_RETENTION_MS);
            // Stale event deletion should run less frequently than the processing of events
            staleEventHorizon = Time.currentTimeMillis() + STALE_EVENT_RETENTION_MS;
        }
    }

    private static boolean shouldProcessEvents(KeycloakSession session) {
        InfinispanConnectionProviderFactory providerFactory = (InfinispanConnectionProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(InfinispanConnectionProvider.class);
        // It is sufficient to run this on the coordinator only, let's save the bandwidth on the other nodes
        return !providerFactory.isCoordinatorSupported() || providerFactory.isCoordinator();
    }
}
