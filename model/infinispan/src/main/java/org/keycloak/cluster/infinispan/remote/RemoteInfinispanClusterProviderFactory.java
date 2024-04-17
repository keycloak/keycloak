package org.keycloak.cluster.infinispan.remote;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.util.ByRef;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ClusterProviderFactory;
import org.keycloak.cluster.infinispan.InfinispanClusterProvider;
import org.keycloak.cluster.infinispan.LockEntry;
import org.keycloak.common.util.Retry;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.infinispan.TopologyInfo;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.WORK_CACHE_NAME;

public class RemoteInfinispanClusterProviderFactory implements ClusterProviderFactory {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private volatile RemoteCache<String, LockEntry> workCache;
    private volatile int clusterStartupTime;
    private volatile RemoteInfinispanNotificationManager notificationManager;
    private volatile Executor executor;

    @Override
    public ClusterProvider create(KeycloakSession session) {
        assert workCache != null;
        assert notificationManager != null;
        assert executor != null;
        return new RemoteInfinispanClusterProvider(clusterStartupTime, workCache, notificationManager, executor);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public synchronized void postInit(KeycloakSessionFactory factory) {
        try (var session = factory.create()) {
            var ispnProvider = session.getProvider(InfinispanConnectionProvider.class);
            executor = ispnProvider.getExecutor("cluster-provider");
            workCache = ispnProvider.getRemoteCache(WORK_CACHE_NAME);
            clusterStartupTime = initClusterStartupTime(ispnProvider.getRemoteCache(WORK_CACHE_NAME), (int) (factory.getServerStartupTimestamp() / 1000));
            notificationManager = new RemoteInfinispanNotificationManager(executor, ispnProvider.getRemoteCache(WORK_CACHE_NAME), getTopologyInfo(factory));
            notificationManager.addClientListener();

            logger.debugf("Provider initialized. Cluster startup time: %s", Time.toDate(clusterStartupTime));
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

    private static TopologyInfo getTopologyInfo(KeycloakSessionFactory factory) {
        try (var session = factory.create()) {
            return session.getProvider(InfinispanConnectionProvider.class).getTopologyInfo();
        }
    }

    private static int initClusterStartupTime(RemoteCache<String, Integer> cache, int serverStartupTime) {
        Integer clusterStartupTime = putIfAbsentWithRetries(cache, InfinispanClusterProvider.CLUSTER_STARTUP_TIME_KEY, serverStartupTime, -1);
        return clusterStartupTime == null ? serverStartupTime : clusterStartupTime;
    }


    static <V extends Serializable> V putIfAbsentWithRetries(RemoteCache<String, V> workCache, String key, V value, int taskTimeoutInSeconds) {
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
}
