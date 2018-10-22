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
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.persistence.remote.RemoteStore;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.Transport;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ClusterProviderFactory;
import org.keycloak.common.util.Retry;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.infinispan.TopologyInfo;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This impl is aware of Cross-Data-Center scenario too
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanClusterProviderFactory implements ClusterProviderFactory {

    public static final String PROVIDER_ID = "infinispan";

    protected static final Logger logger = Logger.getLogger(InfinispanClusterProviderFactory.class);

    // Infinispan cache
    private volatile Cache<String, Serializable> workCache;

    // Ensure that atomic operations (like putIfAbsent) must work correctly in any of: non-clustered, clustered or cross-Data-Center (cross-DC) setups
    private CrossDCAwareCacheFactory crossDCAwareCacheFactory;

    private int clusterStartupTime;

    // Just to extract notifications related stuff to separate class
    private InfinispanNotificationsManager notificationsManager;

    private ExecutorService localExecutor = Executors.newCachedThreadPool();

    @Override
    public ClusterProvider create(KeycloakSession session) {
        lazyInit(session);
        String myAddress = InfinispanUtil.getTopologyInfo(session).getMyNodeName();
        return new InfinispanClusterProvider(clusterStartupTime, myAddress, crossDCAwareCacheFactory, notificationsManager, localExecutor);
    }

    private void lazyInit(KeycloakSession session) {
        if (workCache == null) {
            synchronized (this) {
                if (workCache == null) {
                    InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
                    workCache = ispnConnections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

                    workCache.getCacheManager().addListener(new ViewChangeListener());

                    // See if we have RemoteStore (external JDG) configured for cross-Data-Center scenario
                    Set<RemoteStore> remoteStores = InfinispanUtil.getRemoteStores(workCache);
                    crossDCAwareCacheFactory = CrossDCAwareCacheFactory.getFactory(workCache, remoteStores);

                    clusterStartupTime = initClusterStartupTime(session);

                    TopologyInfo topologyInfo = InfinispanUtil.getTopologyInfo(session);
                    String myAddress = topologyInfo.getMyNodeName();
                    String mySite = topologyInfo.getMySiteName();

                    notificationsManager = InfinispanNotificationsManager.create(session, workCache, myAddress, mySite, remoteStores);
                }
            }
        }
    }


    protected int initClusterStartupTime(KeycloakSession session) {
        Integer existingClusterStartTime = (Integer) crossDCAwareCacheFactory.getCache().get(InfinispanClusterProvider.CLUSTER_STARTUP_TIME_KEY);
        if (existingClusterStartTime != null) {
            logger.debugf("Loaded cluster startup time: %s", Time.toDate(existingClusterStartTime).toString());
            return existingClusterStartTime;
        } else {
            // clusterStartTime not yet initialized. Let's try to put our startupTime
            int serverStartTime = (int) (session.getKeycloakSessionFactory().getServerStartupTimestamp() / 1000);

            existingClusterStartTime = putIfAbsentWithRetries(crossDCAwareCacheFactory, InfinispanClusterProvider.CLUSTER_STARTUP_TIME_KEY, serverStartTime, -1);
            if (existingClusterStartTime == null) {
                logger.debugf("Initialized cluster startup time to %s", Time.toDate(serverStartTime).toString());
                return serverStartTime;
            } else {
                logger.debugf("Loaded cluster startup time: %s", Time.toDate(existingClusterStartTime).toString());
                return existingClusterStartTime;
            }
        }
    }


    // Will retry few times for the case when backup site not available in cross-dc environment.
    // The site might be taken offline automatically if "take-offline" properly configured
    static <V extends Serializable> V putIfAbsentWithRetries(CrossDCAwareCacheFactory crossDCAwareCacheFactory, String key, V value, int taskTimeoutInSeconds) {
        AtomicReference<V> resultRef = new AtomicReference<>();

        Retry.executeWithBackoff((int iteration) -> {

            try {
                V result;
                if (taskTimeoutInSeconds > 0) {
                    result = (V) crossDCAwareCacheFactory.getCache().putIfAbsent(key, value);
                } else {
                    result = (V) crossDCAwareCacheFactory.getCache().putIfAbsent(key, value, taskTimeoutInSeconds, TimeUnit.SECONDS);
                }
                resultRef.set(result);

            } catch (HotRodClientException re) {
                logger.warnf(re, "Failed to write key '%s' and value '%s' in iteration '%d' . Retrying", key, value, iteration);

                // Rethrow the exception. Retry will take care of handle the exception and eventually retry the operation.
                throw re;
            }

        }, 10, 10);

        return resultRef.get();
    }



    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }


    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }


    @Listener
    public class ViewChangeListener {

        @ViewChanged
        public void viewChanged(ViewChangedEvent event) {
            EmbeddedCacheManager cacheManager = event.getCacheManager();
            Transport transport = cacheManager.getTransport();

            // Coordinator makes sure that entries for outdated nodes are cleaned up
            if (transport != null && transport.isCoordinator()) {

                Set<String> newAddresses = convertAddresses(event.getNewMembers());
                Set<String> removedNodesAddresses = convertAddresses(event.getOldMembers());
                removedNodesAddresses.removeAll(newAddresses);

                if (removedNodesAddresses.isEmpty()) {
                    return;
                }

                logger.debugf("Nodes %s removed from cluster. Removing tasks locked by this nodes", removedNodesAddresses.toString());

                Cache<String, Serializable> cache = cacheManager.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

                Iterator<String> toRemove = cache.entrySet().stream().filter(new Predicate<Map.Entry<String, Serializable>>() {

                    @Override
                    public boolean test(Map.Entry<String, Serializable> entry) {
                        if (!(entry.getValue() instanceof LockEntry)) {
                            return false;
                        }

                        LockEntry lock = (LockEntry) entry.getValue();
                        return removedNodesAddresses.contains(lock.getNode());
                    }

                }).map(new Function<Map.Entry<String, Serializable>, String>() {

                    @Override
                    public String apply(Map.Entry<String, Serializable> entry) {
                        return entry.getKey();
                    }

                }).iterator();

                while (toRemove.hasNext()) {
                    String rem = toRemove.next();
                    if (logger.isTraceEnabled()) {
                        logger.tracef("Removing task %s due it's node left cluster", rem);
                    }

                    // If we have task in progress, it needs to be notified
                    notificationsManager.taskFinished(rem, false);

                    cache.remove(rem);
                }
            }
        }

        private Set<String> convertAddresses(Collection<Address> addresses) {
            return addresses.stream().map(new Function<Address, String>() {

                @Override
                public String apply(Address address) {
                    return address.toString();
                }

            }).collect(Collectors.toSet());
        }

    }


}
