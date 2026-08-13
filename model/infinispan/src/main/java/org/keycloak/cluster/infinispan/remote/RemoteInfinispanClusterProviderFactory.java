/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.cluster.infinispan.remote;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.keycloak.Config;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ClusterProviderFactory;
import org.keycloak.cluster.infinispan.InfinispanClusterProvider;
import org.keycloak.cluster.infinispan.LockEntry;
import org.keycloak.common.util.Retry;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.util.ByRef;
import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.WORK_CACHE_NAME;

public class RemoteInfinispanClusterProviderFactory implements ClusterProviderFactory, RemoteInfinispanClusterProvider.SharedData, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private volatile RemoteCache<String, LockEntry> workCache;
    private volatile int clusterStartupTime;
    private volatile RemoteInfinispanNotificationManager notificationManager;
    private volatile Executor executor;

    @Override
    public ClusterProvider create(KeycloakSession session) {
        if (workCache == null) {
            // Keycloak does not ensure postInit() is invoked before create()
            lazyInit(session);
        }
        assert workCache != null;
        assert notificationManager != null;
        assert executor != null;
        return new RemoteInfinispanClusterProvider(this);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        try (var session = factory.create()) {
            lazyInit(session);
        }
    }

    @Override
    public synchronized void close() {
        logger.debug("Closing provider");
        if (notificationManager != null) {
            notificationManager.removeClientListener();
            notificationManager = null;
        }
        // executor is managed by Infinispan, do not shutdown.
        executor = null;
        workCache = null;
    }

    @Override
    public String getId() {
        return InfinispanUtils.REMOTE_PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return InfinispanUtils.isRemoteInfinispan();
    }

    private synchronized void lazyInit(KeycloakSession session) {
        if (workCache != null) {
            return;
        }
        var provider = session.getProvider(InfinispanConnectionProvider.class);
        executor = provider.getExecutor("cluster-provider");
        clusterStartupTime = initClusterStartupTime(provider.getRemoteCache(WORK_CACHE_NAME), (int) (session.getKeycloakSessionFactory().getServerStartupTimestamp() / 1000));
        notificationManager = new RemoteInfinispanNotificationManager(executor, provider.getRemoteCache(WORK_CACHE_NAME), provider.getNodeInfo());
        notificationManager.addClientListener();
        workCache = provider.getRemoteCache(WORK_CACHE_NAME);

        logger.debugf("Provider initialized. Cluster startup time: %s", Time.toDate(clusterStartupTime));
    }

    private static int initClusterStartupTime(RemoteCache<String, Integer> cache, int serverStartupTime) {
        Integer clusterStartupTime = putIfAbsentWithRetries(cache, InfinispanClusterProvider.CLUSTER_STARTUP_TIME_KEY, serverStartupTime, -1);
        return clusterStartupTime == null ? serverStartupTime : clusterStartupTime;
    }

    static <V> V putIfAbsentWithRetries(RemoteCache<String, V> workCache, String key, V value, int taskTimeoutInSeconds) {
        ByRef<V> ref = new ByRef<>(null);

        Retry.executeWithBackoff((int iteration) -> {
            try {
                if (taskTimeoutInSeconds > 0) {
                    ref.set(workCache.putIfAbsent(key, value, taskTimeoutInSeconds, TimeUnit.SECONDS));
                } else {
                    ref.set(workCache.putIfAbsent(key, value));
                }
            } catch (HotRodClientException re) {
                logger.warnf(re, "Failed to write key '%s' and value '%s' in iteration '%d' . Retrying", key, value, iteration);

                // Rethrow the exception. Retry will take care of handle the exception and eventually retry the operation.
                throw re;
            }

        }, 10, 10);

        return ref.get();
    }

    @Override
    public int clusterStartupTime() {
        return clusterStartupTime;
    }

    @Override
    public RemoteCache<String, LockEntry> cache() {
        return workCache;
    }

    @Override
    public RemoteInfinispanNotificationManager notificationManager() {
        return notificationManager;
    }

    @Override
    public Executor executor() {
        return executor;
    }
}
