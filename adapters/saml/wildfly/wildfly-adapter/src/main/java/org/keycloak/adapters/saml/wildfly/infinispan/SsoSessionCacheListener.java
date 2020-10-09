/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.adapters.saml.wildfly.infinispan;

import org.keycloak.adapters.spi.SessionIdMapper;

import java.util.*;
import java.util.concurrent.*;
import org.infinispan.Cache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryRemovedEvent;
import org.infinispan.context.Flag;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.*;
import org.infinispan.notifications.cachelistener.event.*;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
@Listener
@ClientListener
public class SsoSessionCacheListener {

    private static final Logger LOG = Logger.getLogger(SsoSessionCacheListener.class);

    private final ConcurrentMap<String, Queue<Event>> map = new ConcurrentHashMap<>();

    private final SessionIdMapper idMapper;

    private final Cache<String, String[]> ssoCache;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public SsoSessionCacheListener(Cache<String, String[]> ssoCache, SessionIdMapper idMapper) {
        this.ssoCache = ssoCache;
        this.idMapper = idMapper;
    }

    @TransactionRegistered
    public void startTransaction(TransactionRegisteredEvent event) {
        if (event.getGlobalTransaction() == null) {
            return;
        }

        map.put(event.getGlobalTransaction().globalId(), new ConcurrentLinkedQueue<Event>());
    }

    @CacheStarted
    public void cacheStarted(CacheStartedEvent event) {
        this.executor = Executors.newSingleThreadExecutor();
    }

    @CacheStopped
    public void cacheStopped(CacheStoppedEvent event) {
        this.executor.shutdownNow();
    }

    @CacheEntryCreated
    @CacheEntryRemoved
    public void addEvent(TransactionalEvent event) {
        if (event.isOriginLocal()) {
            // Local events are processed by local HTTP session listener
            return;
        }

        if (event.isPre()) {    // only handle post events
            return;
        }

        if (event.getGlobalTransaction() != null) {
            map.get(event.getGlobalTransaction().globalId()).add(event);
        } else {
            processEvent(event);
        }
    }

    @TransactionCompleted
    public void endTransaction(TransactionCompletedEvent event) {
        if (event.getGlobalTransaction() == null) {
            return;
        }

        Queue<Event> events = map.remove(event.getGlobalTransaction().globalId());

        if (events == null || ! event.isTransactionSuccessful()) {
            return;
        }

        for (final Event e : events) {
            processEvent(e);
        }
    }

    private void processEvent(final Event e) {
        switch (e.getType()) {
            case CACHE_ENTRY_CREATED:
                this.executor.submit(new Runnable() {
                    @Override public void run() {
                        cacheEntryCreated((CacheEntryCreatedEvent) e);
                    }
                });
                break;

            case CACHE_ENTRY_REMOVED:
                this.executor.submit(new Runnable() {
                    @Override public void run() {
                        cacheEntryRemoved((CacheEntryRemovedEvent) e);
                    }
                });
                break;
        }
    }

    private void cacheEntryCreated(CacheEntryCreatedEvent event) {
        if (! (event.getKey() instanceof String) || ! (event.getValue() instanceof String[])) {
            return;
        }
        String httpSessionId = (String) event.getKey();
        String[] value = (String[]) event.getValue();
        String ssoId = value[0];
        String principal = value[1];

        LOG.tracev("cacheEntryCreated {0}:{1}", httpSessionId, ssoId);

        this.idMapper.map(ssoId, principal, httpSessionId);
    }

    private void cacheEntryRemoved(CacheEntryRemovedEvent event) {
        if (! (event.getKey() instanceof String)) {
            return;
        }

        LOG.tracev("cacheEntryRemoved {0}", event.getKey());

        this.idMapper.removeSession((String) event.getKey());
    }

    @ClientCacheEntryCreated
    public void remoteCacheEntryCreated(ClientCacheEntryCreatedEvent event) {
        if (! (event.getKey() instanceof String)) {
            return;
        }

        String httpSessionId = (String) event.getKey();

        if (idMapper.hasSession(httpSessionId)) {
            // Ignore local events generated by remote store
            LOG.tracev("IGNORING remoteCacheEntryCreated {0}", httpSessionId);
            return;
        }

        this.executor.submit(new Runnable() {

            @Override
            public void run() {
                String[] value = ssoCache.get((String) httpSessionId);

                if (value != null) {
                    String ssoId = value[0];
                    String principal = value[1];

                    LOG.tracev("remoteCacheEntryCreated {0}:{1}", httpSessionId, ssoId);

                    idMapper.map(ssoId, principal, httpSessionId);
                } else {
                    LOG.tracev("remoteCacheEntryCreated {0}", event.getKey());

                }
            }
          });
    }

    @ClientCacheEntryRemoved
    public void remoteCacheEntryRemoved(ClientCacheEntryRemovedEvent event) {
        LOG.tracev("remoteCacheEntryRemoved {0}", event.getKey());

        this.idMapper.removeSession((String) event.getKey());
        ssoCache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_STORE).remove((String) event.getKey());
    }
}
