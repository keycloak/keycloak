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

import java.io.Serializable;
import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.initializer.BaseCacheInitializer;
import org.keycloak.models.sessions.infinispan.initializer.OfflinePersistentUserSessionLoader;
import org.keycloak.models.sessions.infinispan.initializer.SessionLoader;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RemoteCacheSessionsLoader implements SessionLoader {

    private static final Logger log = Logger.getLogger(RemoteCacheSessionsLoader.class);

    // Hardcoded limit for now. See if needs to be configurable (or if preloading can be enabled/disabled in configuration)
    public static final int LIMIT = 100000;

    private final String cacheName;

    public RemoteCacheSessionsLoader(String cacheName) {
        this.cacheName = cacheName;
    }

    @Override
    public void init(KeycloakSession session) {

    }

    @Override
    public int getSessionsCount(KeycloakSession session) {
        RemoteCache remoteCache = InfinispanUtil.getRemoteCache(getCache(session));
        return remoteCache.size();
    }

    @Override
    public boolean loadSessions(KeycloakSession session, int first, int max) {
        Cache cache = getCache(session);
        Cache decoratedCache = cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD, Flag.SKIP_CACHE_STORE, Flag.IGNORE_RETURN_VALUES);

        RemoteCache<?, ?> remoteCache = InfinispanUtil.getRemoteCache(cache);

        int size = remoteCache.size();

        if (size > LIMIT) {
            log.infof("Skip bulk load of '%d' sessions from remote cache '%s'. Sessions will be retrieved lazily", size, cache.getName());
            return true;
        } else {
            log.infof("Will do bulk load of '%d' sessions from remote cache '%s'", size, cache.getName());
        }


        for (Map.Entry<?, ?> entry : remoteCache.getBulk().entrySet()) {
            SessionEntity entity = (SessionEntity) entry.getValue();
            SessionEntityWrapper entityWrapper = new SessionEntityWrapper(entity);

            decoratedCache.putAsync(entry.getKey(), entityWrapper);
        }

        return true;
    }


    private Cache getCache(KeycloakSession session) {
        InfinispanConnectionProvider ispn = session.getProvider(InfinispanConnectionProvider.class);
        return ispn.getCache(cacheName);
    }


    @Override
    public boolean isFinished(BaseCacheInitializer initializer) {
        Cache<String, Serializable> workCache = initializer.getWorkCache();

        // Check if persistent sessions were already loaded in this DC. This is possible just for offline sessions ATM
        Boolean sessionsLoaded = (Boolean) workCache
                .getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD, Flag.SKIP_CACHE_STORE)
                .get(OfflinePersistentUserSessionLoader.PERSISTENT_SESSIONS_LOADED_IN_CURRENT_DC);

        if (cacheName.equals(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME) && sessionsLoaded != null && sessionsLoaded) {
            log.debugf("Sessions already loaded in current DC. Skip sessions loading from remote cache '%s'", cacheName);
            return true;
        } else {
            log.debugf("Sessions maybe not yet loaded in current DC. Will load them from remote cache '%s'", cacheName);
            return false;
        }
    }


    @Override
    public void afterAllSessionsLoaded(BaseCacheInitializer initializer) {

    }
}
