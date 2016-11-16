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
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.remote.RemoteStore;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.Transport;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ClusterProviderFactory;
import org.keycloak.common.util.HostUtils;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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

    private String myAddress;

    private int clusterStartupTime;

    // Just to extract notifications related stuff to separate class
    private InfinispanNotificationsManager notificationsManager;

    @Override
    public ClusterProvider create(KeycloakSession session) {
        lazyInit(session);
        return new InfinispanClusterProvider(clusterStartupTime, myAddress, crossDCAwareCacheFactory, notificationsManager);
    }

    private void lazyInit(KeycloakSession session) {
        if (workCache == null) {
            synchronized (this) {
                if (workCache == null) {
                    InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
                    workCache = ispnConnections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

                    workCache.getCacheManager().addListener(new ViewChangeListener());
                    initMyAddress();

                    Set<RemoteStore> remoteStores = getRemoteStores();
                    crossDCAwareCacheFactory = CrossDCAwareCacheFactory.getFactory(workCache, remoteStores);

                    clusterStartupTime = initClusterStartupTime(session);

                    notificationsManager = InfinispanNotificationsManager.create(workCache, myAddress, remoteStores);
                }
            }
        }
    }


    // See if we have RemoteStore (external JDG) configured for cross-Data-Center scenario
    private Set<RemoteStore> getRemoteStores() {
        return workCache.getAdvancedCache().getComponentRegistry().getComponent(PersistenceManager.class).getStores(RemoteStore.class);
    }


    protected void initMyAddress() {
        Transport transport = workCache.getCacheManager().getTransport();
        this.myAddress = transport == null ? HostUtils.getHostName() + "-" + workCache.hashCode() : transport.getAddress().toString();
        logger.debugf("My address: %s", this.myAddress);
    }


    protected int initClusterStartupTime(KeycloakSession session) {
        Integer existingClusterStartTime = (Integer) crossDCAwareCacheFactory.getCache().get(InfinispanClusterProvider.CLUSTER_STARTUP_TIME_KEY);
        if (existingClusterStartTime != null) {
            logger.debugf("Loaded cluster startup time: %s", Time.toDate(existingClusterStartTime).toString());
            return existingClusterStartTime;
        } else {
            // clusterStartTime not yet initialized. Let's try to put our startupTime
            int serverStartTime = (int) (session.getKeycloakSessionFactory().getServerStartupTimestamp() / 1000);

            existingClusterStartTime = (Integer) crossDCAwareCacheFactory.getCache().putIfAbsent(InfinispanClusterProvider.CLUSTER_STARTUP_TIME_KEY, serverStartTime);
            if (existingClusterStartTime == null) {
                logger.debugf("Initialized cluster startup time to %s", Time.toDate(serverStartTime).toString());
                return serverStartTime;
            } else {
                logger.debugf("Loaded cluster startup time: %s", Time.toDate(existingClusterStartTime).toString());
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
