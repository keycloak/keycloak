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

import org.keycloak.common.util.Retry;
import org.keycloak.common.util.Time;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.VersionedValue;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.SessionUpdateTask;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RemoteCacheInvoker {

    public static final Logger logger = Logger.getLogger(RemoteCacheInvoker.class);

    private final Map<String, RemoteCacheContext> remoteCaches =  new HashMap<>();


    public void addRemoteCache(String cacheName, RemoteCache remoteCache, MaxIdleTimeLoader maxIdleLoader) {
        RemoteCacheContext ctx = new RemoteCacheContext(remoteCache, maxIdleLoader);
        remoteCaches.put(cacheName, ctx);
    }

    public Set<String> getRemoteCacheNames() {
        return Collections.unmodifiableSet(remoteCaches.keySet());
    }


    public <K, V extends SessionEntity> void runTask(KeycloakSession kcSession, RealmModel realm, String cacheName, K key, SessionUpdateTask<V> task, SessionEntityWrapper<V> sessionWrapper) {
        RemoteCacheContext context = remoteCaches.get(cacheName);
        if (context == null) {
            return;
        }

        V session = sessionWrapper.getEntity();

        SessionUpdateTask.CacheOperation operation = task.getOperation(session);
        SessionUpdateTask.CrossDCMessageStatus status = task.getCrossDCMessageStatus(sessionWrapper);

        if (status == SessionUpdateTask.CrossDCMessageStatus.NOT_NEEDED) {
            logger.debugf("Skip writing to remoteCache for entity '%s' of cache '%s' and operation '%s'", key, cacheName, operation);
            return;
        }

        long loadedMaxIdleTimeMs = context.maxIdleTimeLoader.getMaxIdleTimeMs(realm);

        // Double the timeout to ensure that entry won't expire on remoteCache in case that write of some entities to remoteCache is postponed (eg. userSession.lastSessionRefresh)
        final long maxIdleTimeMs = loadedMaxIdleTimeMs * 2;

        logger.debugf("Running task '%s' on remote cache '%s' . Key is '%s'", operation, cacheName, key);

        Retry.execute(() -> {

            try {
                runOnRemoteCache(context.remoteCache, maxIdleTimeMs, key, task, sessionWrapper);
            } catch (RuntimeException re) {
                if (logger.isDebugEnabled()) {
                    logger.debugf(re, "Failed running task '%s' on remote cache '%s' . Key: '%s' . Will try to retry the task",
                            operation, cacheName, key);
                }

                // Rethrow the exception. Retry will take care of handle the exception and eventually retry the operation.
                throw re;
            }

        }, 10, 0);
    }


    private <K, V extends SessionEntity> void runOnRemoteCache(RemoteCache<K, SessionEntityWrapper<V>> remoteCache, long maxIdleMs, K key, SessionUpdateTask<V> task, SessionEntityWrapper<V> sessionWrapper) {
        final V session = sessionWrapper.getEntity();
        SessionUpdateTask.CacheOperation operation = task.getOperation(session);

        switch (operation) {
            case REMOVE:
                remoteCache.remove(key);
                break;
            case ADD:
                remoteCache.put(key, sessionWrapper.forTransport(), task.getLifespanMs(), TimeUnit.MILLISECONDS, maxIdleMs, TimeUnit.MILLISECONDS);
                break;
            case ADD_IF_ABSENT:
                final int currentTime = Time.currentTime();
                SessionEntityWrapper<V> existing = remoteCache
                        .withFlags(Flag.FORCE_RETURN_VALUE)
                        .putIfAbsent(key, sessionWrapper.forTransport(), -1, TimeUnit.MILLISECONDS, maxIdleMs, TimeUnit.MILLISECONDS);
                if (existing != null) {
                    logger.debugf("Existing entity in remote cache for key: %s . Will update it", key);

                    replace(remoteCache, task.getLifespanMs(), maxIdleMs, key, task);
                } else {
                    sessionWrapper.putLocalMetadataNoteInt(UserSessionEntity.LAST_SESSION_REFRESH_REMOTE, currentTime);
                }
                break;
            case REPLACE:
                replace(remoteCache, task.getLifespanMs(), maxIdleMs, key, task);
                break;
            default:
                throw new IllegalStateException("Unsupported state " +  operation);
        }
    }


    private <K, V extends SessionEntity> void replace(RemoteCache<K, SessionEntityWrapper<V>> remoteCache, long lifespanMs, long maxIdleMs, K key, SessionUpdateTask<V> task) {
        boolean replaced = false;
        while (!replaced) {
            VersionedValue<SessionEntityWrapper<V>> versioned = remoteCache.getVersioned(key);
            if (versioned == null) {
                logger.warnf("Not found entity to replace for key '%s'", key);
                return;
            }

            SessionEntityWrapper<V> sessionWrapper = versioned.getValue();
            final V session = sessionWrapper.getEntity();

            // Run task on the remote session
            task.runUpdate(session);

            logger.debugf("Before replaceWithVersion. Entity to write version %d: %s", versioned.getVersion(), session);

            replaced = remoteCache.replaceWithVersion(key, SessionEntityWrapper.forTransport(session), versioned.getVersion(), lifespanMs, TimeUnit.MILLISECONDS, maxIdleMs, TimeUnit.MILLISECONDS);

            if (!replaced) {
                logger.debugf("Failed to replace entity '%s' version %d. Will retry again", key, versioned.getVersion());
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debugf("Replaced entity version %d in remote cache: %s", versioned.getVersion(), session);
                }
            }
        }
    }


    private class RemoteCacheContext {

        private final RemoteCache remoteCache;
        private final MaxIdleTimeLoader maxIdleTimeLoader;

        public RemoteCacheContext(RemoteCache remoteCache, MaxIdleTimeLoader maxIdleLoader) {
            this.remoteCache = remoteCache;
            this.maxIdleTimeLoader = maxIdleLoader;
        }

    }


    @FunctionalInterface
    public interface MaxIdleTimeLoader {

        long getMaxIdleTimeMs(RealmModel realm);

    }


}
