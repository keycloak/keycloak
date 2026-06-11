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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterEventStoreProvider;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ExecutionResult;
import org.keycloak.common.util.Retry;
import org.keycloak.connections.infinispan.NodeInfo;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;

import org.infinispan.commons.marshall.Marshaller;
import org.jboss.logging.Logger;
import org.jspecify.annotations.Nullable;


/**
 * Wraps an {@link InfinispanClusterProvider} and additionally persists events to the database
 * for cross-cluster communication.
 *
 * @author Alexander Schwartz
 */
public class DatabaseAwareClusterProvider implements ClusterProvider {

    private static final Logger logger = Logger.getLogger(DatabaseAwareClusterProvider.class);

    /**
     * When LISTEN/NOTIFY is enabled on PostgreSQL, this should complete in ~20-50 milliseconds.
     * When polling used on other database, the polling interval is 100 milliseconds, so this should complete
     * within 200-300 milliseconds. See DatabaseAwareClusterProviderFactory DEFAULT_POLL_INTERVAL_MS for the setting.
     * This is not configurable yet.
     */
    private static final Duration AWAIT_TIMEOUT = Duration.of(1, ChronoUnit.SECONDS);

    private final ClusterProvider delegate;
    private final KeycloakSessionFactory sessionFactory;
    private final NodeInfo nodeInfo;
    private final Marshaller marshaller;

    public DatabaseAwareClusterProvider(ClusterProvider delegate, KeycloakSessionFactory sessionFactory,
                                        NodeInfo nodeInfo, Marshaller marshaller) {
        this.delegate = delegate;
        this.sessionFactory = sessionFactory;
        this.nodeInfo = nodeInfo;
        this.marshaller = marshaller;
    }

    @Override
    public int getClusterStartupTime() {
        return delegate.getClusterStartupTime();
    }

    @Override
    public <T> ExecutionResult<T> executeIfNotExecuted(String taskKey, int taskTimeoutInSeconds, Callable<T> task) {
        return delegate.executeIfNotExecuted(taskKey, taskTimeoutInSeconds, task);
    }

    @Override
    public Future<Boolean> executeIfNotExecutedAsync(String taskKey, int taskTimeoutInSeconds, Callable task) {
        return delegate.executeIfNotExecutedAsync(taskKey, taskTimeoutInSeconds, task);
    }

    @Override
    public void registerListener(String taskKey, ClusterListener task) {
        delegate.registerListener(taskKey, task);
    }

    @Override
    public void notify(String taskKey, ClusterEvent event, boolean ignoreSender, DCNotify dcNotify) {
        delegate.notify(taskKey, event, ignoreSender, dcNotify);
        if (dcNotify != DCNotify.LOCAL_DC_ONLY) {
            persistToDatabase(taskKey, Collections.singleton(event), ignoreSender, dcNotify);
        }
    }

    @Override
    public void notify(String taskKey, ClusterEvent event, boolean ignoreSender) {
        delegate.notify(taskKey, event, ignoreSender);
        persistToDatabase(taskKey, Collections.singleton(event), ignoreSender, DCNotify.ALL_DCS);
    }

    @Override
    public void notify(String taskKey, Collection<? extends ClusterEvent> events, boolean ignoreSender, DCNotify dcNotify) {
        delegate.notify(taskKey, events, ignoreSender, dcNotify);
        if (dcNotify != DCNotify.LOCAL_DC_ONLY) {
            persistToDatabase(taskKey, events, ignoreSender, dcNotify);
        }
    }

    @Override
    public void notify(String taskKey, Collection<? extends ClusterEvent> events, boolean ignoreSender) {
        delegate.notify(taskKey, events, ignoreSender);
        persistToDatabase(taskKey, events, ignoreSender, DCNotify.ALL_DCS);
    }

    private void persistToDatabase(String taskKey, Collection<? extends ClusterEvent> events, boolean ignoreSender, DCNotify dcNotify) {
        if (events == null || events.isEmpty() || nodeInfo.clusterName() == null) {
            return;
        }

        var wrappedEvent = WrapperClusterEvent.wrap(taskKey, events,
                // append the cluster name to the node name to avoid situations where the same node name exists in both clusters
                nodeInfo.nodeName() + "@" + nodeInfo.clusterName(),
                nodeInfo.siteName(),
                dcNotify, ignoreSender);
        byte[] data;
        try {
            data = marshaller.objectToByteBuffer(wrappedEvent);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Unable to serialize event", e);
        }

        String eventId = storeEvent(data);
        awaitEventProcessing(eventId);
    }

    private @Nullable String storeEvent(byte[] data) {
        return KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, session -> {
            ClusterEventStoreProvider store = session.getProvider(ClusterEventStoreProvider.class);
            if (store != null) {
                return store.persist(nodeInfo.clusterName(), data);
            } else {
                return null;
            }
        });
    }

    private static class RetryException extends RuntimeException {}

    private void awaitEventProcessing(String eventId) {
        if (eventId == null) {
            return;
        }

        long time = System.nanoTime();
        try {
            Retry.executeWithBackoff(iteration -> {
                boolean exists = KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, session -> {
                    ClusterEventStoreProvider store = session.getProvider(ClusterEventStoreProvider.class);
                    return store != null && store.eventExists(eventId);
                });
                if (exists) {
                    throw new RetryException();
                }
            }, AWAIT_TIMEOUT, 10);
        } catch (RetryException e) {
            logger.warnf("Timeout after %f seconds for cluster event %s to be processed", (System.nanoTime() - time) / 1000000000.0, eventId);
        }
        logger.debugf("Finished waiting for cluster event %s processing in %f seconds", eventId, (System.nanoTime() - time) / 1000000000.0);
    }

    @Override
    public void close() {
    }
}
