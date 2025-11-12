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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.connections.infinispan.remote.RemoteInfinispanConnectionProvider;
import org.keycloak.infinispan.health.ClusterHealth;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.marshalling.KeycloakIndexSchemaUtil;
import org.keycloak.marshalling.KeycloakModelSchema;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.cache.infinispan.ClearCacheEvent;
import org.keycloak.models.cache.infinispan.events.RealmRemovedEvent;
import org.keycloak.models.cache.infinispan.events.RealmUpdatedEvent;
import org.keycloak.models.sessions.infinispan.query.ClientSessionQueries;
import org.keycloak.models.sessions.infinispan.query.UserSessionQueries;
import org.keycloak.models.sessions.infinispan.remote.RemoteInfinispanAuthenticationSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.remote.RemoteUserLoginFailureProviderFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.InvalidationHandler.ObjectType;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.spi.infinispan.CacheEmbeddedConfigProvider;
import org.keycloak.spi.infinispan.CacheRemoteConfigProvider;
import org.keycloak.spi.infinispan.impl.embedded.CacheConfigurator;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.io.ConfigurationWriter;
import org.infinispan.commons.io.StringBuilderWriter;
import org.infinispan.commons.util.Version;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.health.CacheHealth;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLUSTERED_CACHE_NAMES;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CRL_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.KEYS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanUtil.setTimeServiceToKeycloakTime;
import static org.keycloak.models.cache.infinispan.InfinispanCacheRealmProviderFactory.REALM_CLEAR_CACHE_EVENTS;
import static org.keycloak.models.cache.infinispan.InfinispanCacheRealmProviderFactory.REALM_INVALIDATION_EVENTS;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultInfinispanConnectionProviderFactory implements InfinispanConnectionProviderFactory, ProviderEventListener, ServerInfoAwareProviderFactory {

    private static final ReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock();
    private static final Logger logger = Logger.getLogger(DefaultInfinispanConnectionProviderFactory.class);

    private Config.Scope config;

    private volatile EmbeddedCacheManager cacheManager;
    private volatile RemoteCacheManager remoteCacheManager;
    private volatile InfinispanConnectionProvider connectionProvider;
    private volatile ClusterHealth clusterHealth;

    @Override
    public InfinispanConnectionProvider create(KeycloakSession session) {
        return lazyInit(session);
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
        logger.debug("Closing provider");
        runWithWriteLockOnCacheManager(() -> {
            if (cacheManager != null) {
                cacheManager.stop();
                cacheManager = null;
            }
        });
        if (remoteCacheManager != null) {
            remoteCacheManager.close();
            remoteCacheManager = null;
        }
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
        factory.register(this);
    }

    protected InfinispanConnectionProvider lazyInit(KeycloakSession keycloakSession) {
        if (connectionProvider != null) {
            return connectionProvider;
        }
        synchronized (this) {
            if (connectionProvider != null) {
                return connectionProvider;
            }

            this.cacheManager = createEmbeddedCacheManager(keycloakSession);
            injectKeycloakTimeService(cacheManager);
            var topologyInfo = new TopologyInfo(cacheManager);
            var nodeInfo = NodeInfo.of(cacheManager);
            logger.info(nodeInfo.printInfo());

            this.remoteCacheManager = createRemoteCacheManager(keycloakSession);
            this.connectionProvider = InfinispanUtils.isRemoteInfinispan() ?
                    new RemoteInfinispanConnectionProvider(cacheManager, remoteCacheManager, topologyInfo, nodeInfo) :
                    new DefaultInfinispanConnectionProvider(cacheManager, topologyInfo, nodeInfo);

            clusterHealth = GlobalComponentRegistry.componentOf(cacheManager, ClusterHealth.class);
            return connectionProvider;
        }
    }

    protected EmbeddedCacheManager createEmbeddedCacheManager(KeycloakSession session) {
        var holder = session.getProvider(CacheEmbeddedConfigProvider.class).configuration();

        StringBuilderWriter sw = new StringBuilderWriter();
        ParserRegistry parser = new ParserRegistry();
        try (ConfigurationWriter w = ConfigurationWriter.to(sw).prettyPrint(true).build()) {
            var globalConfig = holder.getGlobalConfigurationBuilder().build();
            var cacheConfigs = holder.getNamedConfigurationBuilders().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build()));
            parser.serialize(w, globalConfig, cacheConfigs);
            logger.debugf("Infinispan configuration:\n%s", sw);
        }

        var cm = getDefaultCacheManager(session, holder);
        cm.getCache(KEYS_CACHE_NAME, true);
        cm.getCache(CRL_CACHE_NAME, true);

        logger.debugv("Using container managed Infinispan cache container, lookup={0}", cm);
        return cm;
    }

    private static DefaultCacheManager getDefaultCacheManager(KeycloakSession session, ConfigurationBuilderHolder holder) {
        // This disables the JTA transaction context to avoid binding all JDBC_PING2 interactions to the current transaction
        DefaultCacheManager[] _cm = new DefaultCacheManager[1];
        //noinspection resource
        KeycloakModelUtils.suspendJtaTransaction(session.getKeycloakSessionFactory(), () ->
                _cm[0] = new DefaultCacheManager(holder, true));
        return _cm[0];
    }

    protected RemoteCacheManager createRemoteCacheManager(KeycloakSession session) {
        var remoteConfig = session.getProvider(CacheRemoteConfigProvider.class).configuration();
        if (remoteConfig.isEmpty()) {
            logger.debug("Remote Cache feature is disabled");
            return null;
        }
        logger.debug("Remote Cache feature is enabled");
        var rcm = new RemoteCacheManager(remoteConfig.get());

        // upload the schema before trying to access the caches
        // not caching the list; it is only used during startup
        var entities = List.of(
                new KeycloakIndexSchemaUtil.IndexedEntity(RemoteUserLoginFailureProviderFactory.PROTO_ENTITY, LOGIN_FAILURE_CACHE_NAME),
                new KeycloakIndexSchemaUtil.IndexedEntity(RemoteInfinispanAuthenticationSessionProviderFactory.PROTO_ENTITY, AUTHENTICATION_SESSIONS_CACHE_NAME),
                new KeycloakIndexSchemaUtil.IndexedEntity(ClientSessionQueries.CLIENT_SESSION, CLIENT_SESSION_CACHE_NAME),
                new KeycloakIndexSchemaUtil.IndexedEntity(ClientSessionQueries.CLIENT_SESSION, OFFLINE_CLIENT_SESSION_CACHE_NAME),
                new KeycloakIndexSchemaUtil.IndexedEntity(UserSessionQueries.USER_SESSION, USER_SESSION_CACHE_NAME),
                new KeycloakIndexSchemaUtil.IndexedEntity(UserSessionQueries.USER_SESSION, OFFLINE_USER_SESSION_CACHE_NAME)
        );
        KeycloakIndexSchemaUtil.uploadAndReindexCaches(rcm, KeycloakModelSchema.INSTANCE, entities);
        return rcm;
    }

    /**
     * @deprecated not invoked anymore. Overwrite {@link #createEmbeddedCacheManager(KeycloakSession)}.
     */
    @Deprecated(since = "26.0", forRemoval = true)
    protected EmbeddedCacheManager initContainerManaged(EmbeddedCacheManager cacheManager) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated not used anymore. Overwrite {@link #createEmbeddedCacheManager(KeycloakSession)} if you want to
     * create a custom {@link EmbeddedCacheManager}.
     */
    @Deprecated(since = "26.3", forRemoval = true)
    protected EmbeddedCacheManager initEmbedded() {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated not used anymore
     */
    @Deprecated(since = "26.3", forRemoval = true)
    protected Configuration getKeysCacheConfig() {
        return CacheConfigurator.getCacheConfiguration(KEYS_CACHE_NAME, true).build();
    }

    /**
     * @deprecated Use {@link CacheConfigurator#getCrlCacheConfig()}
     */
    @Deprecated(since = "26.3", forRemoval = true)
    protected Configuration getCrlCacheConfig() {
        return CacheConfigurator.getCrlCacheConfig().build();
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

    private void injectKeycloakTimeService(EmbeddedCacheManager cacheManager) {
        if (config.getBoolean("useKeycloakTimeService", Boolean.FALSE)) {
            setTimeServiceToKeycloakTime(cacheManager);
        }
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(CacheRemoteConfigProvider.class, CacheEmbeddedConfigProvider.class);
    }

    @Override
    public void onEvent(ProviderEvent event) {
        if (event instanceof PostMigrationEvent pme) {
            KeycloakModelUtils.runJobInTransaction(pme.getFactory(), this::registerSystemWideListeners);
        }
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("product", Version.getBrandName());
        info.put("version", Version.getBrandVersion());
        if (InfinispanUtils.isRemoteInfinispan()) {
            addRemoteOperationalInfo(info);
        } else {
            addEmbeddedOperationalInfo(info);
        }
        return info;
    }

    @Override
    public boolean isClusterHealthy() {
        clusterHealth.triggerClusterHealthCheck();
        return clusterHealth.isHealthy();
    }

    @Override
    public boolean isClusterHealthSupported() {
        return clusterHealth.isSupported();
    }

    private void addEmbeddedOperationalInfo(Map<String, String> info) {
        var cacheManagerInfo = cacheManager.getCacheManagerInfo();
        info.put("clusterSize", Integer.toString(cacheManagerInfo.getClusterSize()));
        var cacheNames = Arrays.stream(CLUSTERED_CACHE_NAMES)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (CacheHealth health : cacheManager.getHealth().getCacheHealth(cacheNames)) {
            info.put(health.getCacheName() + ":Cache", health.getStatus().toString());
        }
    }

    private void addRemoteOperationalInfo(Map<String, String> info) {
        info.put("connectionCount", Integer.toString(remoteCacheManager.getConnectionCount()));
    }
}
