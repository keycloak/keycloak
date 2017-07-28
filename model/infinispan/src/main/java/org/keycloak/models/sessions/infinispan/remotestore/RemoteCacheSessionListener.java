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

package org.keycloak.models.sessions.infinispan.remotestore;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientCacheFailover;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryRemovedEvent;
import org.infinispan.client.hotrod.event.ClientCacheFailoverEvent;
import org.infinispan.client.hotrod.event.ClientEvent;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ClientListener
public class RemoteCacheSessionListener  {

    protected static final Logger logger = Logger.getLogger(RemoteCacheSessionListener.class);

    private Cache<String, SessionEntityWrapper> cache;
    private RemoteCache remoteCache;
    private boolean distributed;
    private String myAddress;


    protected RemoteCacheSessionListener() {
    }


    protected void init(KeycloakSession session, Cache<String, SessionEntityWrapper> cache, RemoteCache remoteCache) {
        this.cache = cache;
        this.remoteCache = remoteCache;

        this.distributed = InfinispanUtil.isDistributedCache(cache);
        if (this.distributed) {
            this.myAddress = InfinispanUtil.getMyAddress(session);
        } else {
            this.myAddress = null;
        }
    }


    @ClientCacheEntryCreated
    public void created(ClientCacheEntryCreatedEvent event) {
        String key = (String) event.getKey();

        if (shouldUpdateLocalCache(event.getType(), key, event.isCommandRetried())) {
            // Should load it from remoteStore
            cache.get(key);
        }
    }


    @ClientCacheEntryModified
    public void updated(ClientCacheEntryModifiedEvent event) {
        String key = (String) event.getKey();

        if (shouldUpdateLocalCache(event.getType(), key, event.isCommandRetried())) {

            replaceRemoteEntityInCache(key);
        }
    }


    private void replaceRemoteEntityInCache(String key) {
        // TODO can be optimized and remoteSession sent in the event itself?
        SessionEntityWrapper localEntityWrapper = cache.get(key);
        SessionEntity remoteSession = (SessionEntity) remoteCache.get(key);

        if (logger.isDebugEnabled()) {
            logger.debugf("Read session. Entity read from remote cache: %s", remoteSession.toString());
        }

        SessionEntityWrapper sessionWrapper = remoteSession.mergeRemoteEntityWithLocalEntity(localEntityWrapper);

        // We received event from remoteCache, so we won't update it back
        cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_STORE, Flag.SKIP_CACHE_LOAD, Flag.IGNORE_RETURN_VALUES)
                .replace(key, sessionWrapper);
    }


    @ClientCacheEntryRemoved
    public void removed(ClientCacheEntryRemovedEvent event) {
        String key = (String) event.getKey();

        if (shouldUpdateLocalCache(event.getType(), key, event.isCommandRetried())) {
            // We received event from remoteCache, so we won't update it back
            cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_STORE, Flag.SKIP_CACHE_LOAD, Flag.IGNORE_RETURN_VALUES)
                    .remove(key);
        }
    }


    @ClientCacheFailover
    public void failover(ClientCacheFailoverEvent event) {
        logger.infof("Received failover event: " + event.toString());
    }


    // For distributed caches, ensure that local modification is executed just on owner OR if event.isCommandRetried
    protected boolean shouldUpdateLocalCache(ClientEvent.Type type, String key, boolean commandRetried) {
        boolean result;

        // Case when cache is stopping or stopped already
        if (!cache.getStatus().allowInvocations()) {
            return false;
        }

        if (!distributed || commandRetried) {
            result = true;
        } else {
            String keyAddress = InfinispanUtil.getKeyPrimaryOwnerAddress(cache, key);
            result = myAddress.equals(keyAddress);
        }

        logger.debugf("Received event from remote store. Event '%s', key '%s', skip '%b'", type.toString(), key, !result);

        return result;
    }



    @ClientListener(includeCurrentState = true)
    public static class FetchInitialStateCacheListener extends RemoteCacheSessionListener {
    }


    @ClientListener(includeCurrentState = false)
    public static class DontFetchInitialStateCacheListener extends RemoteCacheSessionListener {
    }


    public static RemoteCacheSessionListener createListener(KeycloakSession session, Cache<String, SessionEntityWrapper> cache, RemoteCache remoteCache) {
        /*boolean isCoordinator = InfinispanUtil.isCoordinator(cache);

        // Just cluster coordinator will fetch userSessions from remote cache.
        // In case that coordinator is failover during state fetch, there is slight risk that not all userSessions will be fetched to local cluster. Assume acceptable for now
        RemoteCacheSessionListener listener;
        if (isCoordinator) {
            logger.infof("Will fetch initial state from remote cache for cache '%s'", cache.getName());
            listener = new FetchInitialStateCacheListener();
        } else {
            logger.infof("Won't fetch initial state from remote cache for cache '%s'", cache.getName());
            listener = new DontFetchInitialStateCacheListener();
        }*/

        RemoteCacheSessionListener listener = new RemoteCacheSessionListener();
        listener.init(session, cache, remoteCache);

        return listener;
    }


}
