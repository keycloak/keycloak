/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryRemovedEvent;
import org.infinispan.context.Flag;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.persistence.remote.RemoteStore;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.ConcurrentMultivaluedHashMap;
import org.keycloak.common.util.Retry;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.KeycloakSession;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;

/**
 * Impl for sending infinispan messages across cluster and listening to them
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanNotificationsManager {

    protected static final Logger logger = Logger.getLogger(InfinispanNotificationsManager.class);

    private final ConcurrentMultivaluedHashMap<String, ClusterListener> listeners = new ConcurrentMultivaluedHashMap<>();

    private final ConcurrentMap<String, TaskCallback> taskCallbacks = new ConcurrentHashMap<>();

    private final Cache<String, Serializable> workCache;

    private final RemoteCache workRemoteCache;

    private final String myAddress;

    private final String mySite;

    private final ExecutorService listenersExecutor;


    protected InfinispanNotificationsManager(Cache<String, Serializable> workCache, RemoteCache workRemoteCache, String myAddress, String mySite, ExecutorService listenersExecutor) {
        this.workCache = workCache;
        this.workRemoteCache = workRemoteCache;
        this.myAddress = myAddress;
        this.mySite = mySite;
        this.listenersExecutor = listenersExecutor;
    }


    // Create and init manager including all listeners etc
    public static InfinispanNotificationsManager create(KeycloakSession session, Cache<String, Serializable> workCache, String myAddress, String mySite, Set<RemoteStore> remoteStores) {
        RemoteCache workRemoteCache = null;

        if (!remoteStores.isEmpty()) {
            RemoteStore remoteStore = remoteStores.iterator().next();
            workRemoteCache = remoteStore.getRemoteCache();

            if (mySite == null) {
                throw new IllegalStateException("Multiple datacenters available, but site name is not configured! Check your configuration");
            }
        }

        ExecutorService listenersExecutor = workRemoteCache==null ? null : session.getProvider(ExecutorsProvider.class).getExecutor("work-cache-event-listener");
        InfinispanNotificationsManager manager = new InfinispanNotificationsManager(workCache, workRemoteCache, myAddress, mySite, listenersExecutor);

        // We need CacheEntryListener for communication within current DC
        workCache.addListener(manager.new CacheEntryListener());
        logger.debugf("Added listener for infinispan cache: %s", workCache.getName());

        // Added listener for remoteCache to notify other DCs
        if (workRemoteCache != null) {
            workRemoteCache.addClientListener(manager.new HotRodListener(workRemoteCache));
            logger.debugf("Added listener for HotRod remoteStore cache: %s", workRemoteCache.getName());
        }

        return manager;
    }


    void registerListener(String taskKey, ClusterListener task) {
        listeners.add(taskKey, task);
    }


    TaskCallback registerTaskCallback(String taskKey, TaskCallback callback) {
        TaskCallback existing = taskCallbacks.putIfAbsent(taskKey, callback);

        if (existing != null) {
            return existing;
        } else {
            return callback;
        }
    }


    void notify(String taskKey, ClusterEvent event, boolean ignoreSender, ClusterProvider.DCNotify dcNotify) {
        WrapperClusterEvent wrappedEvent = new WrapperClusterEvent();
        wrappedEvent.setEventKey(taskKey);
        wrappedEvent.setDelegateEvent(event);
        wrappedEvent.setIgnoreSender(ignoreSender);
        wrappedEvent.setIgnoreSenderSite(dcNotify == ClusterProvider.DCNotify.ALL_BUT_LOCAL_DC);
        wrappedEvent.setSender(myAddress);
        wrappedEvent.setSenderSite(mySite);

        String eventKey = UUID.randomUUID().toString();

        if (logger.isTraceEnabled()) {
            logger.tracef("Sending event with key %s: %s", eventKey, event);
        }

        if (dcNotify == ClusterProvider.DCNotify.LOCAL_DC_ONLY || workRemoteCache == null) {
            // Just put it to workCache, but skip notifying remoteCache
            workCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES, Flag.SKIP_CACHE_STORE)
                    .put(eventKey, wrappedEvent, 120, TimeUnit.SECONDS);
        } else {
            // Add directly to remoteCache. Will notify remote listeners on all nodes in all DCs
            Retry.executeWithBackoff((int iteration) -> {
                try {
                    workRemoteCache.put(eventKey, wrappedEvent, 120, TimeUnit.SECONDS);
                } catch (HotRodClientException re) {
                if (logger.isDebugEnabled()) {
                    logger.debugf(re, "Failed sending notification to remote cache '%s'. Key: '%s', iteration '%s'. Will try to retry the task",
                            workRemoteCache.getName(), eventKey, iteration);
                }

                // Rethrow the exception. Retry will take care of handle the exception and eventually retry the operation.
                throw re;
            }

        }, 10, 10);

        }
    }


    @Listener(observation = Listener.Observation.POST)
    public class CacheEntryListener {

        @CacheEntryCreated
        public void cacheEntryCreated(CacheEntryCreatedEvent<String, Serializable> event) {
            eventReceived(event.getKey(), event.getValue());
        }

        @CacheEntryModified
        public void cacheEntryModified(CacheEntryModifiedEvent<String, Serializable> event) {
            eventReceived(event.getKey(), event.getValue());
        }

        @CacheEntryRemoved
        public void cacheEntryRemoved(CacheEntryRemovedEvent<String, Serializable> event) {
            taskFinished(event.getKey(), true);
        }

    }


    @ClientListener
    public class HotRodListener {

        private final RemoteCache<Object, Object> remoteCache;

        public HotRodListener(RemoteCache<Object, Object> remoteCache) {
            this.remoteCache = remoteCache;
        }


        @ClientCacheEntryCreated
        public void created(ClientCacheEntryCreatedEvent event) {
            String key = event.getKey().toString();
            hotrodEventReceived(key);
        }


        @ClientCacheEntryModified
        public void updated(ClientCacheEntryModifiedEvent event) {
            String key = event.getKey().toString();
            hotrodEventReceived(key);
        }


        @ClientCacheEntryRemoved
        public void removed(ClientCacheEntryRemovedEvent event) {
            String key = event.getKey().toString();
            taskFinished(key, true);
        }


        private void hotrodEventReceived(String key) {
            // TODO: Look at CacheEventConverter stuff to possibly include value in the event and avoid additional remoteCache request
            try {
                listenersExecutor.submit(() -> {

                    Object value = remoteCache.get(key);
                    eventReceived(key, (Serializable) value);

                });
            } catch (RejectedExecutionException ree) {
                logger.errorf("Rejected submitting of the event for key: %s. Value: %s, Server going to shutdown or pool exhausted. Pool: %s", key, workCache.get(key), listenersExecutor.toString());
                throw ree;
            }
        }

    }

    private void eventReceived(String key, Serializable obj) {
        if (!(obj instanceof WrapperClusterEvent)) {
            if (obj == null) {
                logger.warnf("Event object wasn't available in remote cache after event was received. Event key: %s", key);
            }
            return;
        }

        WrapperClusterEvent event = (WrapperClusterEvent) obj;

        if (event.isIgnoreSender()) {
            if (this.myAddress.equals(event.getSender())) {
                return;
            }
        }

        if (event.isIgnoreSenderSite()) {
            if (this.mySite == null || this.mySite.equals(event.getSenderSite())) {
                return;
            }
        }

        String eventKey = event.getEventKey();

        if (logger.isTraceEnabled()) {
            logger.tracef("Received event: %s", event);
        }

        ClusterEvent wrappedEvent = event.getDelegateEvent();

        List<ClusterListener> myListeners = listeners.get(eventKey);
        if (myListeners != null) {
            for (ClusterListener listener : myListeners) {
                listener.eventReceived(wrappedEvent);
            }
        }
    }


    void taskFinished(String taskKey, boolean success) {
        TaskCallback callback = taskCallbacks.remove(taskKey);

        if (callback != null) {
            if (logger.isDebugEnabled()) {
                logger.debugf("Finished task '%s' with '%b'", taskKey, success);
            }
            callback.setSuccess(success);
            callback.getTaskCompletedLatch().countDown();
        }

    }
}
