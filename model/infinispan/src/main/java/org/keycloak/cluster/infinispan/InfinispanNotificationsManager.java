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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
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
import org.infinispan.context.Flag;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryExpired;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryExpiredEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.persistence.remote.RemoteStore;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.MultivaluedHashMap;

/**
 * Impl for sending infinispan messages across cluster and listening to them
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanNotificationsManager {

    protected static final Logger logger = Logger.getLogger(InfinispanNotificationsManager.class);

    private final MultivaluedHashMap<String, ClusterListener> listeners = new MultivaluedHashMap<>();

    private final ConcurrentMap<String, TaskCallback> taskCallbacks = new ConcurrentHashMap<>();

    private final Cache<String, Serializable> workCache;

    private final String myAddress;

    private final String mySite;


    protected InfinispanNotificationsManager(Cache<String, Serializable> workCache, String myAddress, String mySite) {
        this.workCache = workCache;
        this.myAddress = myAddress;
        this.mySite = mySite;
    }


    // Create and init manager including all listeners etc
    public static InfinispanNotificationsManager create(Cache<String, Serializable> workCache, String myAddress, String mySite, Set<RemoteStore> remoteStores) {
        InfinispanNotificationsManager manager = new InfinispanNotificationsManager(workCache, myAddress, mySite);

        // We need CacheEntryListener just if we don't have remoteStore. With remoteStore will be all cluster nodes notified anyway from HotRod listener
        if (remoteStores.isEmpty()) {
            workCache.addListener(manager.new CacheEntryListener());

            logger.debugf("Added listener for infinispan cache: %s", workCache.getName());
        } else {
            for (RemoteStore remoteStore : remoteStores) {
                RemoteCache<Object, Object> remoteCache = remoteStore.getRemoteCache();
                remoteCache.addClientListener(manager.new HotRodListener(remoteCache));

                logger.debugf("Added listener for HotRod remoteStore cache: %s", remoteCache.getName());
            }

            if (mySite == null) {
                throw new IllegalStateException("Multiple datacenters available, but site name is not configured! Check your configuration");
            }
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

        if (logger.isTraceEnabled()) {
            logger.tracef("Sending event: %s", event);
        }

        Flag[] flags = dcNotify == ClusterProvider.DCNotify.LOCAL_DC_ONLY
                ? new Flag[] { Flag.IGNORE_RETURN_VALUES, Flag.SKIP_CACHE_STORE }
                : new Flag[] { Flag.IGNORE_RETURN_VALUES };

        // Put the value to the cache to notify listeners on all the nodes
        workCache.getAdvancedCache().withFlags(flags)
                .put(UUID.randomUUID().toString(), wrappedEvent, 120, TimeUnit.SECONDS);
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
            Object value = workCache.get(key);
            eventReceived(key, (Serializable) value);
        }

    }

    private void eventReceived(String key, Serializable obj) {
        if (!(obj instanceof WrapperClusterEvent)) {
            return;
        }

        WrapperClusterEvent event = (WrapperClusterEvent) obj;

        if (event.isIgnoreSender()) {
            if (this.myAddress.equals(event.getSender())) {
                return;
            }
        }

        if (event.isIgnoreSenderSite()) {
            if (this.mySite != null && this.mySite.equals(event.getSender())) {
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
