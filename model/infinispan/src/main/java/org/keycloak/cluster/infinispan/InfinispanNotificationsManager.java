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

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.ConcurrentMultivaluedHashMap;
import org.keycloak.models.sessions.infinispan.CacheDecorators;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static org.keycloak.cluster.infinispan.InfinispanClusterProvider.TASK_KEY_PREFIX;

/**
 * Impl for sending infinispan messages across cluster and listening to them
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanNotificationsManager {

    protected static final Logger logger = Logger.getLogger(InfinispanNotificationsManager.class);

    private final ConcurrentMultivaluedHashMap<String, ClusterListener> listeners = new ConcurrentMultivaluedHashMap<>();

    private final ConcurrentMap<String, TaskCallback> taskCallbacks = new ConcurrentHashMap<>();

    private final Cache<String, Object> workCache;

    private final String myAddress;

    private final String mySite;

    protected InfinispanNotificationsManager(Cache<String, Object> workCache, String myAddress, String mySite) {
        this.workCache = workCache;
        this.myAddress = myAddress;
        this.mySite = mySite;
    }


    // Create and init manager including all listeners etc
    public static InfinispanNotificationsManager create(Cache<String, Object> workCache, String myAddress, String mySite) {
        InfinispanNotificationsManager manager = new InfinispanNotificationsManager(workCache, myAddress, mySite);

        // We need CacheEntryListener for communication within current DC
        workCache.addListener(manager.new CacheEntryListener());
        logger.debugf("Added listener for infinispan cache: %s", workCache.getName());

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


    void notify(String taskKey, Collection<? extends ClusterEvent> events, boolean ignoreSender, ClusterProvider.DCNotify dcNotify) {
        if (events == null || events.isEmpty()) {
            return;
        }
        var wrappedEvent = WrapperClusterEvent.wrap(taskKey, events, myAddress, mySite, dcNotify, ignoreSender);

        String eventKey = UUID.randomUUID().toString();

        if (logger.isTraceEnabled()) {
            logger.tracef("Sending event with key %s: %s", eventKey, events);
        }

        CacheDecorators.ignoreReturnValues(workCache)
                .put(eventKey, wrappedEvent, 120, TimeUnit.SECONDS);
    }


    @Listener(observation = Listener.Observation.POST)
    public class CacheEntryListener {

        @CacheEntryCreated
        public void cacheEntryCreated(CacheEntryCreatedEvent<String, Object> event) {
            eventReceived(event.getKey(), event.getValue());
        }

        @CacheEntryModified
        public void cacheEntryModified(CacheEntryModifiedEvent<String, Object> event) {
            eventReceived(event.getKey(), event.getNewValue());
        }

        @CacheEntryRemoved
        public void cacheEntryRemoved(CacheEntryRemovedEvent<String, Object> event) {
            taskFinished(event.getKey());
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

        if (event.rejectEvent(myAddress, mySite)) {
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


    void taskFinished(String taskKey) {
        TaskCallback callback = taskCallbacks.remove(taskKey);

        if (callback != null) {
            if (logger.isDebugEnabled()) {
                logger.debugf("Finished task '%s' with '%b'", taskKey, true);
            }
            callback.setSuccess(true);
            callback.getTaskCompletedLatch().countDown();
        }
    }
}
