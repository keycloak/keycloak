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

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ClusterProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.DefaultInfinispanConnectionProviderFactory;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.context.Flag;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.Merged;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.MergeEvent;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.WORK_CACHE_NAME;

/**
 * This impl is aware of Cross-Data-Center scenario too
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanClusterProviderFactory implements ClusterProviderFactory, EnvironmentDependentProviderFactory {

    protected static final Logger logger = Logger.getLogger(InfinispanClusterProviderFactory.class);

    private volatile Cache<String, Object> workCache;
    private volatile ClusterProvider clusterProvider;

    private final ExecutorService localExecutor = Executors.newCachedThreadPool(r -> {
        Thread thread = Executors.defaultThreadFactory().newThread(r);
        thread.setName(this.getClass().getName() + "-" + thread.getName());
        return thread;
    });

    private ViewChangeListener workCacheListener;

    @Override
    public ClusterProvider create(KeycloakSession session) {
        return lazyInit(session);
    }

    private ClusterProvider lazyInit(KeycloakSession session) {
        if (clusterProvider != null)
            return clusterProvider;

        synchronized (this) {
            if (clusterProvider != null)
                return clusterProvider;
            InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
            this.workCache = ispnConnections.getCache(WORK_CACHE_NAME);

            workCacheListener = new ViewChangeListener();
            workCache.getCacheManager().addListener(workCacheListener);

            var clusterStartupTime = initClusterStartupTime(session);
            var cp = new InfinispanClusterProvider(clusterStartupTime, ispnConnections.getNodeInfo(), workCache, localExecutor);

            // We need CacheEntryListener for communication within current DC
            workCache.addListener(cp.new CacheEntryListener());
            logger.debugf("Added listener for infinispan cache: %s", workCache.getName());

            this.clusterProvider = cp;
            return clusterProvider;
        }
    }

    protected int initClusterStartupTime(KeycloakSession session) {
        Integer existingClusterStartTime = (Integer) workCache.get(InfinispanClusterProvider.CLUSTER_STARTUP_TIME_KEY);
        if (existingClusterStartTime != null) {
            if (logger.isDebugEnabled()) {
                logger.debugf("Loaded cluster startup time: %s", Time.toDate(existingClusterStartTime).toString());
            }
            return existingClusterStartTime;
        } else {
            // clusterStartTime not yet initialized. Let's try to put our startupTime
            int serverStartTime = (int) (session.getKeycloakSessionFactory().getServerStartupTimestamp() / 1000);

            existingClusterStartTime = (Integer) workCache.putIfAbsent(InfinispanClusterProvider.CLUSTER_STARTUP_TIME_KEY, serverStartTime);
            if (existingClusterStartTime == null) {
                if (logger.isDebugEnabled()) {
                    logger.debugf("Initialized cluster startup time to %s", Time.toDate(serverStartTime).toString());
                }
                return serverStartTime;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debugf("Loaded cluster startup time: %s", Time.toDate(existingClusterStartTime).toString());
                }
                return existingClusterStartTime;
            }
        }
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }


    @Override
    public void close() {
        synchronized (this) {
            if (workCache != null && workCacheListener != null) {
                workCache.removeListener(workCacheListener);
                workCacheListener = null;
                localExecutor.shutdown();
            }
        }
    }

    @Override
    public String getId() {
        return InfinispanUtils.EMBEDDED_PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return InfinispanUtils.isEmbeddedInfinispan();
    }

    @Listener
    public class ViewChangeListener {

        @Merged
        public void mergeEvent(MergeEvent event) {
            // During split-brain only Keycloak instances contained within the same partition will receive updates via
            // the work cache. On split-brain healing, it's necessary for us to clear all local caches so that potentially
            // stale values are invalidated and subsequent requests are forced to read from the DB.
            localExecutor.execute(() ->
                    Arrays.stream(InfinispanConnectionProvider.LOCAL_CACHE_NAMES)
                            .map(name -> workCache.getCacheManager().getCache(name))
                            .filter(cache -> cache.getCacheConfiguration().clustering().cacheMode() == CacheMode.LOCAL)
                            .forEach(Cache::clear)
            );

            if (Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS)) {
                // If persistent user sessions are enabled, the reasoning from above is true for the user and client sessions as well.
                // As the session caches are distributed caches and as this runs on every node, run it locally on each node.
                localExecutor.execute(() ->
                        Arrays.stream(InfinispanConnectionProvider.USER_AND_CLIENT_SESSION_CACHES)
                                .map(name -> workCache.getCacheManager().getCache(name).getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL))
                                .forEach(Cache::clear)
                );
            }
        }

        @ViewChanged
        public void viewChanged(ViewChangedEvent event) {
            Set<String> removedNodesAddresses = convertAddresses(event.getOldMembers());
            Set<String> newAddresses = convertAddresses(event.getNewMembers());

            // Use separate thread to avoid potential deadlock
            localExecutor.execute(() -> {
                try {
                    // Coordinator makes sure that entries for outdated nodes are cleaned up
                    if (workCache.getCacheManager().isCoordinator()) {

                        removedNodesAddresses.removeAll(newAddresses);

                        if (removedNodesAddresses.isEmpty()) {
                            return;
                        }

                        logger.debugf("Nodes %s removed from cluster. Removing tasks locked by this nodes", removedNodesAddresses.toString());
                        DefaultInfinispanConnectionProviderFactory.runWithReadLockOnCacheManager(() -> {
                            if (workCache.getStatus() == ComponentStatus.RUNNING) {
                                workCache.entrySet().removeIf(new LockEntryPredicate(removedNodesAddresses));
                            } else {
                                logger.warn("work cache is not running, ignoring event");
                            }
                        });
                    }
                } catch (Throwable t) {
                    logger.error("caught exception in ViewChangeListener", t);
                }
            });
        }

        private Set<String> convertAddresses(Collection<Address> addresses) {
            return addresses.stream().map(Object::toString).collect(Collectors.toSet());
        }
    }
}
