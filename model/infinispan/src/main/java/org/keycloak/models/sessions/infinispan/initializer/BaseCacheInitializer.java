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

package org.keycloak.models.sessions.infinispan.initializer;

import java.io.Serializable;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.remoting.transport.Transport;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class BaseCacheInitializer extends CacheInitializer {

    private static final String STATE_KEY_PREFIX = "distributed::";

    private static final Logger log = Logger.getLogger(BaseCacheInitializer.class);

    protected final KeycloakSessionFactory sessionFactory;
    protected final Cache<String, Serializable> workCache;
    protected final SessionLoader sessionLoader;
    protected final int sessionsPerSegment;
    protected final String stateKey;

    public BaseCacheInitializer(KeycloakSessionFactory sessionFactory, Cache<String, Serializable> workCache, SessionLoader sessionLoader, String stateKeySuffix, int sessionsPerSegment) {
        this.sessionFactory = sessionFactory;
        this.workCache = workCache;
        this.sessionLoader = sessionLoader;
        this.sessionsPerSegment = sessionsPerSegment;
        this.stateKey = STATE_KEY_PREFIX + stateKeySuffix;
    }


    @Override
    protected boolean isFinished() {
        // Check if we should skipLoadingSessions. This can happen if someone else already did the task (For example in cross-dc environment, it was done by different DC)
        boolean isFinishedAlready = this.sessionLoader.isFinished(this);
        if (isFinishedAlready) {
            return true;
        }

        InitializerState state = getStateFromCache();
        return state != null && state.isFinished();
    }


    @Override
    protected boolean isCoordinator() {
        Transport transport = workCache.getCacheManager().getTransport();
        return transport == null || transport.isCoordinator();
    }


    protected InitializerState getStateFromCache() {
        // We ignore cacheStore for now, so that in Cross-DC scenario (with RemoteStore enabled) is the remoteStore ignored.
        return (InitializerState) workCache.getAdvancedCache()
                .withFlags(Flag.SKIP_CACHE_STORE, Flag.SKIP_CACHE_LOAD)
                .get(stateKey);
    }


    protected void saveStateToCache(final InitializerState state) {

        // 3 attempts to send the message (it may fail if some node fails in the meantime)
        retry(3, new Runnable() {

            @Override
            public void run() {

                // Save this synchronously to ensure all nodes read correct state
                // We ignore cacheStore for now, so that in Cross-DC scenario (with RemoteStore enabled) is the remoteStore ignored.
                BaseCacheInitializer.this.workCache.getAdvancedCache().
                        withFlags(Flag.IGNORE_RETURN_VALUES, Flag.FORCE_SYNCHRONOUS, Flag.SKIP_CACHE_STORE, Flag.SKIP_CACHE_LOAD)
                        .put(stateKey, state);
            }

        });
    }


    private void retry(int retry, Runnable runnable) {
        while (true) {
            try {
                runnable.run();
                return;
            } catch (RuntimeException e) {
                ComponentStatus status = workCache.getStatus();
                if (status.isStopping() || status.isTerminated()) {
                    log.warn("Failed to put initializerState to the cache. Cache is already terminating");
                    log.debug(e.getMessage(), e);
                    return;
                }
                retry--;
                if (retry == 0) {
                    throw e;
                }
            }
        }
    }


    public Cache<String, Serializable> getWorkCache() {
        return workCache;
    }
}
