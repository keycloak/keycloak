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

package org.keycloak.connections.infinispan;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.ProtocolVersion;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.EmbeddedTransactionManagerLookup;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ManagedCacheManagerProvider;
import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.cache.infinispan.ClearCacheEvent;
import org.keycloak.models.cache.infinispan.events.RealmRemovedEvent;
import org.keycloak.models.cache.infinispan.events.RealmUpdatedEvent;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.InvalidationHandler.ObjectType;
import org.keycloak.provider.ProviderEvent;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.ACTION_TOKEN_CACHE;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHORIZATION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHORIZATION_REVISIONS_CACHE_DEFAULT_MAX;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHORIZATION_REVISIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.REALM_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.REALM_REVISIONS_CACHE_DEFAULT_MAX;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.REALM_REVISIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_REVISIONS_CACHE_DEFAULT_MAX;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_REVISIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.WORK_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanUtil.configureTransport;
import static org.keycloak.connections.infinispan.InfinispanUtil.createCacheConfigurationBuilder;
import static org.keycloak.connections.infinispan.InfinispanUtil.getActionTokenCacheConfig;
import static org.keycloak.connections.infinispan.InfinispanUtil.setTimeServiceToKeycloakTime;
import static org.keycloak.models.cache.infinispan.InfinispanCacheRealmProviderFactory.REALM_CLEAR_CACHE_EVENTS;
import static org.keycloak.models.cache.infinispan.InfinispanCacheRealmProviderFactory.REALM_INVALIDATION_EVENTS;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultInfinispanConnectionProviderFactory implements InfinispanConnectionProviderFactory {

    private static final ReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock();
    private static final Logger logger = Logger.getLogger(DefaultInfinispanConnectionProviderFactory.class);

    private Config.Scope config;

    private volatile EmbeddedCacheManager cacheManager;

    private volatile RemoteCacheProvider remoteCacheProvider;

    protected volatile boolean containerManaged;

    private volatile TopologyInfo topologyInfo;

    @Override
    public InfinispanConnectionProvider create(KeycloakSession session) {
        lazyInit();

        return new DefaultInfinispanConnectionProvider(cacheManager, remoteCacheProvider, topologyInfo);
    }

    /*
        Workaround for Infinispan 12.1.7.Final and tested until 14.0.19.Final to prevent a deadlock while
        DefaultInfinispanConnectionProviderFactory is shutting down. Kept as a permanent solution and considered
        good enough after a lot of analysis went into this difficult to reproduce problem.
        See https://github.com/keycloak/keycloak/issues/9871 for the discussion.
    */
    public static void runWithReadLockOnCacheManager(Runnable task) {
        Lock lock = DefaultInfinispanConnectionProviderFactory.READ_WRITE_LOCK.readLock();
        lock.lock();
        try {
            task.run();
        } finally {
            lock.unlock();
        }
    }

    public static <T> T runWithReadLockOnCacheManager(Supplier<T> task) {
        Lock lock = DefaultInfinispanConnectionProviderFactory.READ_WRITE_LOCK.readLock();
        lock.lock();
        try {
            return task.get();
        } finally {
            lock.unlock();
        }
    }

    public static void runWithWriteLockOnCacheManager(Runnable task) {
        Lock lock = DefaultInfinispanConnectionProviderFactory.READ_WRITE_LOCK.writeLock();
        lock.lock();
        try {
            task.run();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        runWithWriteLockOnCacheManager(() -> {
            if (cacheManager != null && !containerManaged) {
                cacheManager.stop();
            }
            if (remoteCacheProvider != null) {
                remoteCacheProvider.stop();
            }
        });
    }

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register((ProviderEvent event) -> {
            if (event instanceof PostMigrationEvent) {
                KeycloakModelUtils.runJobInTransaction(factory, this::registerSystemWideListeners);
            }
        });
    }

    protected void lazyInit() {
        if (cacheManager == null) {
            synchronized (this) {
                if (cacheManager == null) {
                    EmbeddedCacheManager managedCacheManager = null;
                    Iterator<ManagedCacheManagerProvider> providers = ServiceLoader.load(ManagedCacheManagerProvider.class, DefaultInfinispanConnectionProvider.class.getClassLoader())
                            .iterator();

                    if (providers.hasNext()) {
                        ManagedCacheManagerProvider provider = providers.next();
                        
                        if (providers.hasNext()) {
                            throw new RuntimeException("Multiple " + org.keycloak.cluster.ManagedCacheManagerProvider.class + " providers found.");
                        }
                        
                        managedCacheManager = provider.getEmbeddedCacheManager(config);
                    }

                    // store it in a locale variable first, so it is not visible to the outside, yet
                    EmbeddedCacheManager localCacheManager;
                    if (managedCacheManager == null) {
                        if (!config.getBoolean("embedded", false)) {
                            throw new RuntimeException("No " + ManagedCacheManagerProvider.class.getName() + " found. If running in embedded mode set the [embedded] property to this provider.");
                        }
                        localCacheManager = initEmbedded();
                    } else {
                        localCacheManager = initContainerManaged(managedCacheManager);
                    }

                    logger.infof(topologyInfo.toString());

                    remoteCacheProvider = new RemoteCacheProvider(config, localCacheManager);
                    // only set the cache manager attribute at the very end to avoid passing a half-initialized entry callers
                    cacheManager = localCacheManager;
                }
            }
        }
    }

    protected EmbeddedCacheManager initContainerManaged(EmbeddedCacheManager cacheManager) {
        containerManaged = true;

        defineRevisionCache(cacheManager, REALM_CACHE_NAME, REALM_REVISIONS_CACHE_NAME, REALM_REVISIONS_CACHE_DEFAULT_MAX);
        defineRevisionCache(cacheManager, USER_CACHE_NAME, USER_REVISIONS_CACHE_NAME, USER_REVISIONS_CACHE_DEFAULT_MAX);
        defineRevisionCache(cacheManager, AUTHORIZATION_CACHE_NAME, AUTHORIZATION_REVISIONS_CACHE_NAME, AUTHORIZATION_REVISIONS_CACHE_DEFAULT_MAX);

        cacheManager.getCache(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME, true);
        cacheManager.getCache(InfinispanConnectionProvider.KEYS_CACHE_NAME, true);
        cacheManager.getCache(InfinispanConnectionProvider.ACTION_TOKEN_CACHE, true);

        this.topologyInfo = new TopologyInfo(cacheManager, config, false, getId());

        logger.debugv("Using container managed Infinispan cache container, lookup={0}", cacheManager);

        return cacheManager;
    }

    protected EmbeddedCacheManager initEmbedded() {
        GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder();

        boolean clustered = config.getBoolean("clustered", false);
        boolean async = config.getBoolean("async", false);
        boolean useKeycloakTimeService = config.getBoolean("useKeycloakTimeService", false);

        this.topologyInfo = new TopologyInfo(cacheManager, config, true, getId());

        if (clustered) {
            String jgroupsUdpMcastAddr = config.get("jgroupsUdpMcastAddr", System.getProperty(InfinispanConnectionProvider.JGROUPS_UDP_MCAST_ADDR));
            configureTransport(gcb, topologyInfo.getMyNodeName(), topologyInfo.getMySiteName(), jgroupsUdpMcastAddr,
                    "default-configs/default-keycloak-jgroups-udp.xml");
            gcb.jmx()
              .domain(InfinispanConnectionProvider.JMX_DOMAIN + "-" + topologyInfo.getMyNodeName()).enable();
        } else {
            gcb.jmx().domain(InfinispanConnectionProvider.JMX_DOMAIN).enable();
        }

        Marshalling.configure(gcb);
        EmbeddedCacheManager cacheManager = new DefaultCacheManager(gcb.build());
        if (useKeycloakTimeService) {
            setTimeServiceToKeycloakTime(cacheManager);
        }
        containerManaged = false;

        logger.debug("Started embedded Infinispan cache container");

        var localConfiguration = createCacheConfigurationBuilder().build();

        // local caches first
        defineLocalCache(cacheManager, REALM_CACHE_NAME, REALM_REVISIONS_CACHE_NAME, localConfiguration, REALM_REVISIONS_CACHE_DEFAULT_MAX);
        defineLocalCache(cacheManager, AUTHORIZATION_CACHE_NAME, AUTHORIZATION_REVISIONS_CACHE_NAME, localConfiguration, AUTHORIZATION_REVISIONS_CACHE_DEFAULT_MAX);
        defineLocalCache(cacheManager, USER_CACHE_NAME, USER_REVISIONS_CACHE_NAME, localConfiguration, USER_REVISIONS_CACHE_DEFAULT_MAX);

        cacheManager.defineConfiguration(InfinispanConnectionProvider.KEYS_CACHE_NAME, getKeysCacheConfig());
        cacheManager.getCache(InfinispanConnectionProvider.KEYS_CACHE_NAME, true);

        var builder = createCacheConfigurationBuilder();
        if (clustered) {
            builder.simpleCache(false);
            String sessionsMode = config.get("sessionsMode", "distributed");
            if (sessionsMode.equalsIgnoreCase("replicated")) {
                builder.clustering().cacheMode(async ? CacheMode.REPL_ASYNC : CacheMode.REPL_SYNC);
            } else if (sessionsMode.equalsIgnoreCase("distributed")) {
                builder.clustering().cacheMode(async ? CacheMode.DIST_ASYNC : CacheMode.DIST_SYNC);
            } else {
                throw new RuntimeException("Invalid value for sessionsMode");
            }

            int owners = config.getInt("sessionsOwners", 2);
            logger.debugf("Session owners: %d", owners);

            int l1Lifespan = config.getInt("l1Lifespan", 600000);
            boolean l1Enabled = l1Lifespan > 0;
            Boolean awaitInitialTransfer = config.getBoolean("awaitInitialTransfer", true);
            builder.clustering()
                    .hash()
                        .numOwners(owners)
                        .numSegments(config.getInt("sessionsSegments", 60))
                    .l1()
                        .enabled(l1Enabled)
                        .lifespan(l1Lifespan)
                    .stateTransfer().awaitInitialTransfer(awaitInitialTransfer).timeout(30, TimeUnit.SECONDS);
        }

        // Base configuration doesn't contain any remote stores
        var clusteredConfiguration = builder.build();

        defineClusteredCache(cacheManager, USER_SESSION_CACHE_NAME, clusteredConfiguration);
        defineClusteredCache(cacheManager, OFFLINE_USER_SESSION_CACHE_NAME, clusteredConfiguration);
        defineClusteredCache(cacheManager, CLIENT_SESSION_CACHE_NAME, clusteredConfiguration);
        defineClusteredCache(cacheManager, OFFLINE_CLIENT_SESSION_CACHE_NAME, clusteredConfiguration);
        defineClusteredCache(cacheManager, LOGIN_FAILURE_CACHE_NAME, clusteredConfiguration);

        var actionTokenBuilder = getActionTokenCacheConfig();
        if (clustered) {
            actionTokenBuilder.simpleCache(false);
            actionTokenBuilder.clustering().cacheMode(async ? CacheMode.REPL_ASYNC : CacheMode.REPL_SYNC);
        }
        defineClusteredCache(cacheManager, ACTION_TOKEN_CACHE, actionTokenBuilder.build());

        defineClusteredCache(cacheManager, AUTHENTICATION_SESSIONS_CACHE_NAME, clusteredConfiguration);

        var workBuilder = createCacheConfigurationBuilder()
                .expiration().enableReaper().wakeUpInterval(15, TimeUnit.SECONDS);
        if (clustered) {
            workBuilder.simpleCache(false);
            workBuilder.clustering().cacheMode(async ? CacheMode.REPL_ASYNC : CacheMode.REPL_SYNC);
        }
        defineClusteredCache(cacheManager, WORK_CACHE_NAME, builder.build());

        return cacheManager;
    }

    private void defineLocalCache(EmbeddedCacheManager cacheManager, String cacheName, String revCacheName, Configuration configuration, long defaultMaxEntries) {
        cacheManager.defineConfiguration(cacheName, configuration);
        defineRevisionCache(cacheManager, cacheName, revCacheName, defaultMaxEntries);
    }

    private void defineRevisionCache(EmbeddedCacheManager cacheManager, String cacheName, String revCacheName, long defaultMaxEntries) {
        var maxCount = cacheManager.getCache(cacheName).getCacheConfiguration().memory().maxCount();
        maxCount = maxCount > 0 ? 2 * maxCount : defaultMaxEntries;
        cacheManager.defineConfiguration(revCacheName, getRevisionCacheConfig(maxCount));
        cacheManager.getCache(revCacheName);
    }

    private void defineClusteredCache(EmbeddedCacheManager cacheManager, String cacheName, Configuration baseConfiguration) {
        // copy base configuration
        var builder = createCacheConfigurationBuilder();
        builder.read(baseConfiguration);
        if (config.getBoolean("remoteStoreEnabled", false)) {
            configureRemoteCacheStore(builder, config.getBoolean("async", false), cacheName);
        }
        cacheManager.defineConfiguration(cacheName, builder.build());
        cacheManager.getCache(cacheName);
    }

    private Configuration getRevisionCacheConfig(long maxEntries) {
        ConfigurationBuilder cb = createCacheConfigurationBuilder();
        cb.simpleCache(false);
        cb.invocationBatching().enable().transaction().transactionMode(TransactionMode.TRANSACTIONAL);

        // Use Embedded manager even in managed ( wildfly/eap ) environment. We don't want infinispan to participate in global transaction
        cb.transaction().transactionManagerLookup(new EmbeddedTransactionManagerLookup());

        cb.transaction().lockingMode(LockingMode.PESSIMISTIC);
        if (cb.memory().storage().canStoreReferences()) {
            cb.encoding().mediaType(MediaType.APPLICATION_OBJECT_TYPE);
        }

        cb.memory()
                .whenFull(EvictionStrategy.REMOVE)
                .maxCount(maxEntries);

        return cb.build();
    }

    // Used for cross-data centers scenario. Usually integration with external JDG server, which itself handles communication between DCs.
    private void configureRemoteCacheStore(ConfigurationBuilder builder, boolean async, String cacheName) {
        String jdgServer = config.get("remoteStoreHost", "127.0.0.1");
        Integer jdgPort = config.getInt("remoteStorePort", 11222);

        // After upgrade to Infinispan 12.1.7.Final it's required that both remote store and embedded cache use
        // the same key media type to allow segmentation. Also, the number of segments in an embedded cache needs to match number of segments in the remote store.
        boolean segmented = config.getBoolean("segmented", false);

        //noinspection removal
        builder.persistence()
                .passivation(false)
                .addStore(RemoteStoreConfigurationBuilder.class)
                .ignoreModifications(false)
                .purgeOnStartup(false)
                .preload(false)
                .shared(true)
                .remoteCacheName(cacheName)
                .segmented(segmented)
                .rawValues(true)
                .forceReturnValues(false)
                .protocolVersion(getHotrodVersion())
                .addServer()
                .host(jdgServer)
                .port(jdgPort)
                .async()
                .enabled(async);
    }

    private ProtocolVersion getHotrodVersion() {
        String hotrodVersionStr = config.get("hotrodProtocolVersion", ProtocolVersion.DEFAULT_PROTOCOL_VERSION.toString());
        ProtocolVersion hotrodVersion = ProtocolVersion.parseVersion(hotrodVersionStr);
        if (hotrodVersion == null) {
            hotrodVersion = ProtocolVersion.DEFAULT_PROTOCOL_VERSION;
        }

        logger.debugf("HotRod protocol version: %s", hotrodVersion);

        return hotrodVersion;
    }

    protected Configuration getKeysCacheConfig() {
        ConfigurationBuilder cb = createCacheConfigurationBuilder();

        cb.memory()
                .whenFull(EvictionStrategy.REMOVE)
                .maxCount(InfinispanConnectionProvider.KEYS_CACHE_DEFAULT_MAX);

        cb.expiration().maxIdle(InfinispanConnectionProvider.KEYS_CACHE_MAX_IDLE_SECONDS, TimeUnit.SECONDS);

        return cb.build();
    }

    private void registerSystemWideListeners(KeycloakSession session) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        ClusterProvider cluster = session.getProvider(ClusterProvider.class);
        cluster.registerListener(REALM_CLEAR_CACHE_EVENTS, (ClusterEvent event) -> {
            if (event instanceof ClearCacheEvent) {
                sessionFactory.invalidate(null, ObjectType._ALL_);
            }
        });
        cluster.registerListener(REALM_INVALIDATION_EVENTS, (ClusterEvent event) -> {
            if (event instanceof RealmUpdatedEvent rr) {
                sessionFactory.invalidate(null, ObjectType.REALM, rr.getId());
            } else if (event instanceof RealmRemovedEvent rr) {
                sessionFactory.invalidate(null, ObjectType.REALM, rr.getId());
            }
        });
    }
}
