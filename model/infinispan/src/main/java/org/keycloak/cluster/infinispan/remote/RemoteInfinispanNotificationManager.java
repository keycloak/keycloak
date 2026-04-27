/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.cluster.infinispan.remote;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider.DCNotify;
import org.keycloak.cluster.infinispan.TaskCallback;
import org.keycloak.cluster.infinispan.WrapperClusterEvent;
import org.keycloak.common.util.ConcurrentMultivaluedHashMap;
import org.keycloak.common.util.Retry;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.connections.infinispan.NodeInfo;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCustomEvent;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.Marshaller;
import org.jboss.logging.Logger;

import static org.keycloak.cluster.infinispan.InfinispanClusterProvider.TASK_KEY_PREFIX;

@ClientListener(converterFactoryName = "___eager-key-value-version-converter", useRawData = true)
public class RemoteInfinispanNotificationManager {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final ConcurrentMap<String, TaskCallback> taskCallbacks = new ConcurrentHashMap<>();
    private final ConcurrentMultivaluedHashMap<String, ClusterListener> listeners = new ConcurrentMultivaluedHashMap<>();
    private final Executor executor;
    private final RemoteCache<String, Object> workCache;
    private final NodeInfo nodeInfo;
    private final Marshaller marshaller;

    public RemoteInfinispanNotificationManager(Executor executor, RemoteCache<String, Object> workCache, NodeInfo nodeInfo) {
        this.executor = executor;
        this.workCache = workCache;
        this.nodeInfo = nodeInfo;
        this.marshaller = workCache.getRemoteCacheContainer().getMarshaller();
    }

    public void addClientListener() {
        workCache.addClientListener(this);
    }

    public void removeClientListener() {
        // workaround because providers are independent and close() can be invoked in any order.
        if (workCache.getRemoteCacheContainer().isStarted()) {
            workCache.removeClientListener(this);
        }
    }

    public void registerListener(String taskKey, ClusterListener task) {
        listeners.add(taskKey, task);
    }

    public TaskCallback registerTaskCallback(String taskKey, TaskCallback callback) {
        var existing = taskCallbacks.putIfAbsent(taskKey, callback);
        return existing != null ? existing : callback;
    }

    public void notify(String taskKey, Collection<? extends ClusterEvent> events, boolean ignoreSender, DCNotify dcNotify) {
        if (events == null || events.isEmpty()) {
            return;
        }
        var wrappedEvent = WrapperClusterEvent.wrap(taskKey, events, nodeInfo.nodeName(), nodeInfo.siteName(), dcNotify, ignoreSender);

        var eventKey = SecretGenerator.getInstance().generateSecureID();

        if (logger.isTraceEnabled()) {
            logger.tracef("Sending event with key %s: %s", eventKey, events);
        }

        Retry.executeWithBackoff((int iteration) -> {
            try {
                workCache.put(eventKey, wrappedEvent, 120, TimeUnit.SECONDS);
            } catch (HotRodClientException re) {
                if (logger.isDebugEnabled()) {
                    logger.debugf(re, "Failed sending notification to remote cache '%s'. Key: '%s', iteration '%s'. Will try to retry the task",
                            workCache.getName(), eventKey, iteration);
                }

                // Rethrow the exception. Retry will take care of handle the exception and eventually retry the operation.
                throw re;
            }

        }, 10, 10);

    }

    public String getMyNodeName() {
        return nodeInfo.nodeName();
    }

    @ClientCacheEntryCreated
    @ClientCacheEntryModified
    public void onEntryUpdated(ClientCacheEntryCustomEvent<byte[]> event) {
        try {
            byte[] data = event.getEventData();
            ByteBuffer buffer = ByteBuffer.wrap(data);
            int length = UnsignedNumeric.readUnsignedInt(buffer);

            // unmarshall the key
            String key = (String) marshaller.objectFromByteBuffer(data, buffer.position(), length);

            buffer.position(buffer.position() + length);
            length = UnsignedNumeric.readUnsignedInt(buffer);

            // unmarshall the value
            Object value = marshaller.objectFromByteBuffer(data, buffer.position(), length);
            executor.execute(() -> eventReceived(key, value));
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Unexpected error handling an update/create event from Infinispan cluster", e);
        }
    }

    @ClientCacheEntryRemoved
    public void onEntryRemoved(ClientCacheEntryCustomEvent<byte[]> event) {
        try {
            byte[] data = event.getEventData();
            ByteBuffer buffer = ByteBuffer.wrap(data);
            int length = UnsignedNumeric.readUnsignedInt(buffer);

            // unmarshall the key
            taskFinished((String) marshaller.objectFromByteBuffer(data, buffer.position(), length));
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Unexpected error handling a remove event from Infinispan cluster", e);
        }
    }

    private void eventReceived(String key, Object obj) {
        if (!(obj instanceof WrapperClusterEvent event)) {
            // Items with the TASK_KEY_PREFIX might be gone fast once the locking is complete, therefore, don't log them.
            // It is still good to have the warning in case of real events return null because they have been, for example, expired
            if (obj == null && !key.startsWith(TASK_KEY_PREFIX)) {
                logger.warnf("Event object wasn't available in remote cache after event was received. Event key: %s", key);
            }
            return;
        }

        if (event.rejectEvent(nodeInfo.nodeName(), nodeInfo.siteName())) {
            return;
        }

        String eventKey = event.getEventKey();

        if (logger.isTraceEnabled()) {
            logger.tracef("Received event: %s", event);
        }

        List<ClusterListener> myListeners = listeners.get(eventKey);
        if (myListeners != null) {
            for (var e : event.getDelegateEvents()) {
                myListeners.forEach(e);
            }
        }
    }

    private void taskFinished(String taskKey) {
        TaskCallback callback = taskCallbacks.remove(taskKey);
        if (callback == null) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debugf("Finished task '%s' with '%b'", taskKey, true);
        }
        callback.setSuccess(true);
        callback.getTaskCompletedLatch().countDown();
    }
}
