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
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ExecutionResult;
import org.keycloak.cluster.jpa.JpaClusterEventStoreProvider;
import org.keycloak.common.util.Retry;
import org.keycloak.connections.infinispan.NodeInfo;
import org.keycloak.models.KeycloakSession;
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

    private final ClusterProvider delegate;
    private final KeycloakSession session;
    private final NodeInfo nodeInfo;
    private final Marshaller marshaller;
    private final Duration awaitTimeout;

    public DatabaseAwareClusterProvider(ClusterProvider delegate, KeycloakSession session,
                                        NodeInfo nodeInfo, Marshaller marshaller, Duration awaitTimeout) {
        this.delegate = delegate;
        this.session = session;
        this.nodeInfo = nodeInfo;
        this.marshaller = marshaller;
        this.awaitTimeout = awaitTimeout;
    }

    @Override
    public int getClusterStartupTime() {
        return delegate.getClusterStartupTime();
    }

    @Override
    public <T> ExecutionResult<T> executeIfNotExecuted(String taskKey, int taskTimeoutInSeconds, Callable<T> task) {
        if (isPrimaryClusterSupported() && !isPrimaryCluster()) {
            return ExecutionResult.notExecuted();
        }
        return delegate.executeIfNotExecuted(taskKey, taskTimeoutInSeconds, task);
    }

    @Override
    public boolean isPrimaryCluster() {
        return Objects.equals(new JpaClusterEventStoreProvider(session).getPrimaryClusterName(), nodeInfo.clusterName());
    }

    @Override
    public boolean isPrimaryClusterSupported() {
        return new JpaClusterEventStoreProvider(session).isUsingJdbcPing(nodeInfo.clusterName(), nodeInfo.nodeName());
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
        return KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(),
                s -> new JpaClusterEventStoreProvider(s).persist(nodeInfo.clusterName(), data));
    }

    private static class RetryException extends RuntimeException {}

    private void awaitEventProcessing(String eventId) {
        if (eventId == null) {
            return;
        }

        long time = System.nanoTime();
        try {
            Retry.executeWithBackoff(iteration -> {
                boolean exists = KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), s -> {
                    JpaClusterEventStoreProvider store = new JpaClusterEventStoreProvider(s);
                    return store.eventExists(eventId);
                });
                if (exists) {
                    throw new RetryException();
                }
            }, awaitTimeout, 10);
        } catch (RetryException e) {
            logger.warnf("Timeout after %f seconds for cluster event %s to be processed", (System.nanoTime() - time) / 1000000000.0, eventId);
        }
        logger.debugf("Finished waiting for cluster event %s processing in %f seconds", eventId, (System.nanoTime() - time) / 1000000000.0);
    }

    @Override
    public void close() {
    }
}
