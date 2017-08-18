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
import java.util.HashMap;
import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.marshall.Marshaller;
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


    // Javascript to be executed on remote infinispan server (Flag CACHE_MODE_LOCAL assumes that remoteCache is replicated)
    private static final String REMOTE_SCRIPT_FOR_LOAD_SESSIONS =
            "function loadSessions() {" +
            "  var flagClazz = cache.getClass().getClassLoader().loadClass(\"org.infinispan.context.Flag\"); \n" +
            "  var localFlag = java.lang.Enum.valueOf(flagClazz, \"CACHE_MODE_LOCAL\"); \n" +
            "  var cacheStream = cache.getAdvancedCache().withFlags([ localFlag ]).entrySet().stream();\n" +
            "  var result = cacheStream.skip(first).limit(max).collect(java.util.stream.Collectors.toMap(\n" +
            "    new java.util.function.Function() {\n" +
            "      apply: function(entry) {\n" +
            "        return entry.getKey();\n" +
            "      }\n" +
            "    },\n" +
            "    new java.util.function.Function() {\n" +
            "      apply: function(entry) {\n" +
            "        return entry.getValue();\n" +
            "      }\n" +
            "    }\n" +
            "  ));\n" +
            "\n" +
            "  cacheStream.close();\n" +
            "  return result;\n" +
            "};\n" +
            "\n" +
            "loadSessions();";



    private final String cacheName;

    public RemoteCacheSessionsLoader(String cacheName) {
        this.cacheName = cacheName;
    }

    @Override
    public void init(KeycloakSession session) {
        RemoteCache remoteCache = InfinispanUtil.getRemoteCache(getCache(session));

        RemoteCache<String, String> scriptCache = remoteCache.getRemoteCacheManager().getCache("___script_cache");

        if (!scriptCache.containsKey("load-sessions.js")) {
            scriptCache.put("load-sessions.js",
                    "// mode=local,language=javascript\n" +
                            REMOTE_SCRIPT_FOR_LOAD_SESSIONS);
        }
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

        log.debugf("Will do bulk load of sessions from remote cache '%s' . First: %d, max: %d", cache.getName(), first, max);

        Map<String, Integer> remoteParams = new HashMap<>();
        remoteParams.put("first", first);
        remoteParams.put("max", max);
        Map<byte[], byte[]> remoteObjects = remoteCache.execute("load-sessions.js", remoteParams);

        log.debugf("Successfully finished loading sessions '%s' . First: %d, max: %d", cache.getName(), first, max);

        Marshaller marshaller = remoteCache.getRemoteCacheManager().getMarshaller();

        for (Map.Entry<byte[], byte[]> entry : remoteObjects.entrySet()) {
            try {
                Object key = marshaller.objectFromByteBuffer(entry.getKey());
                SessionEntity entity = (SessionEntity) marshaller.objectFromByteBuffer(entry.getValue());

                SessionEntityWrapper entityWrapper = new SessionEntityWrapper(entity);

                decoratedCache.putAsync(key, entityWrapper);
            } catch (Exception e) {
                log.warn("Error loading session from remote cache", e);
            }
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
