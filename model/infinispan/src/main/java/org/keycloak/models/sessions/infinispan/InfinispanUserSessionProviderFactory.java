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

package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.persistence.remote.RemoteStore;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Environment;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.changes.PersistentUpdate;
import org.keycloak.models.sessions.infinispan.changes.SerializeExecutionsByKey;
import org.keycloak.models.sessions.infinispan.changes.PersistentSessionsWorker;
import org.keycloak.models.sessions.infinispan.changes.sessions.CrossDCLastSessionRefreshStore;
import org.keycloak.models.sessions.infinispan.changes.sessions.CrossDCLastSessionRefreshStoreFactory;
import org.keycloak.models.sessions.infinispan.changes.sessions.PersisterLastSessionRefreshStore;
import org.keycloak.models.sessions.infinispan.changes.sessions.PersisterLastSessionRefreshStoreFactory;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheInvoker;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.events.AbstractUserSessionClusterListener;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RemoveUserSessionsEvent;
import org.keycloak.models.sessions.infinispan.initializer.InfinispanCacheInitializer;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheSessionListener;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheSessionsLoader;
import org.keycloak.models.sessions.infinispan.util.InfinispanKeyGenerator;
import org.keycloak.connections.infinispan.InfinispanUtil;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.keycloak.models.sessions.infinispan.InfinispanAuthenticationSessionProviderFactory.PROVIDER_PRIORITY;

public class InfinispanUserSessionProviderFactory implements UserSessionProviderFactory, ServerInfoAwareProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanUserSessionProviderFactory.class);

    public static final String PROVIDER_ID = "infinispan";

    public static final String REALM_REMOVED_SESSION_EVENT = "REALM_REMOVED_EVENT_SESSIONS";

    public static final String REMOVE_USER_SESSIONS_EVENT = "REMOVE_USER_SESSIONS_EVENT";
    public static final String CONFIG_OFFLINE_SESSION_CACHE_ENTRY_LIFESPAN_OVERRIDE = "offlineSessionCacheEntryLifespanOverride";
    public static final String CONFIG_OFFLINE_CLIENT_SESSION_CACHE_ENTRY_LIFESPAN_OVERRIDE = "offlineClientSessionCacheEntryLifespanOverride";
    public static final String CONFIG_MAX_BATCH_SIZE = "maxBatchSize";
    public static final int DEFAULT_MAX_BATCH_SIZE = Math.max(Runtime.getRuntime().availableProcessors(), 2);

    private long offlineSessionCacheEntryLifespanOverride;

    private long offlineClientSessionCacheEntryLifespanOverride;

    private Config.Scope config;

    private RemoteCacheInvoker remoteCacheInvoker;
    private CrossDCLastSessionRefreshStore lastSessionRefreshStore;
    private CrossDCLastSessionRefreshStore offlineLastSessionRefreshStore;
    private PersisterLastSessionRefreshStore persisterLastSessionRefreshStore;
    private InfinispanKeyGenerator keyGenerator;
    SerializeExecutionsByKey<String> serializerSession = new SerializeExecutionsByKey<>();
    SerializeExecutionsByKey<String> serializerOfflineSession = new SerializeExecutionsByKey<>();
    SerializeExecutionsByKey<UUID> serializerClientSession = new SerializeExecutionsByKey<>();
    SerializeExecutionsByKey<UUID> serializerOfflineClientSession = new SerializeExecutionsByKey<>();
    ArrayBlockingQueue<PersistentUpdate> asyncQueuePersistentUpdate = new ArrayBlockingQueue<>(1000);
    private PersistentSessionsWorker persistentSessionsWorker;
    private int maxBatchSize;

    @Override
    public UserSessionProvider create(KeycloakSession session) {
        InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = connections.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
        Cache<String, SessionEntityWrapper<UserSessionEntity>> offlineSessionsCache = connections.getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionCache = connections.getCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> offlineClientSessionsCache = connections.getCache(InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME);

        if (Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS)) {
            return new PersistentUserSessionProvider(
                    session,
                    remoteCacheInvoker,
                    lastSessionRefreshStore,
                    offlineLastSessionRefreshStore,
                    keyGenerator,
                    cache,
                    offlineSessionsCache,
                    clientSessionCache,
                    offlineClientSessionsCache,
                    asyncQueuePersistentUpdate,
                    serializerSession,
                    serializerOfflineSession,
                    serializerClientSession,
                    serializerOfflineClientSession
            );
        }
        return new InfinispanUserSessionProvider(
                session,
                remoteCacheInvoker,
                lastSessionRefreshStore,
                offlineLastSessionRefreshStore,
                persisterLastSessionRefreshStore,
                keyGenerator,
                cache,
                offlineSessionsCache,
                clientSessionCache,
                offlineClientSessionsCache,
                this::deriveOfflineSessionCacheEntryLifespanMs,
                this::deriveOfflineClientSessionCacheEntryLifespanOverrideMs,
                serializerSession,
                serializerOfflineSession,
                serializerClientSession,
                serializerOfflineClientSession
        );
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
        offlineSessionCacheEntryLifespanOverride = config.getInt(CONFIG_OFFLINE_SESSION_CACHE_ENTRY_LIFESPAN_OVERRIDE, -1);
        offlineClientSessionCacheEntryLifespanOverride = config.getInt(CONFIG_OFFLINE_CLIENT_SESSION_CACHE_ENTRY_LIFESPAN_OVERRIDE, -1);
        maxBatchSize = config.getInt(CONFIG_MAX_BATCH_SIZE, DEFAULT_MAX_BATCH_SIZE);
    }

    @Override
    public void postInit(final KeycloakSessionFactory factory) {
        factory.register(new ProviderEventListener() {

            @Override
            public void onEvent(ProviderEvent event) {
                if (event instanceof PostMigrationEvent) {

                    int preloadTransactionTimeout = getTimeoutForPreloadingSessionsSeconds();
                    log.debugf("Will preload sessions with transaction timeout %d seconds", preloadTransactionTimeout);

                    KeycloakModelUtils.runJobInTransactionWithTimeout(factory, (KeycloakSession session) -> {

                        keyGenerator = new InfinispanKeyGenerator();
                        checkRemoteCaches(session);
                        if (!Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS)) {
                            initializeLastSessionRefreshStore(factory);
                        }
                        registerClusterListeners(session);
                        loadSessionsFromRemoteCaches(session);

                    }, preloadTransactionTimeout);

                } else if (event instanceof UserModel.UserRemovedEvent) {
                    UserModel.UserRemovedEvent userRemovedEvent = (UserModel.UserRemovedEvent) event;

                    UserSessionProvider provider1 = userRemovedEvent.getKeycloakSession().getProvider(UserSessionProvider.class, getId());
                    if (provider1 instanceof InfinispanUserSessionProvider) {
                        ((InfinispanUserSessionProvider) provider1).onUserRemoved(userRemovedEvent.getRealm(), userRemovedEvent.getUser());
                    } else if (provider1 instanceof PersistentUserSessionProvider) {
                        ((PersistentUserSessionProvider) provider1).onUserRemoved(userRemovedEvent.getRealm(), userRemovedEvent.getUser());
                    } else {
                        throw new IllegalStateException("Unknown provider type: " + provider1.getClass());
                    }

                } else if (event instanceof ResetTimeOffsetEvent) {
                    if (persisterLastSessionRefreshStore != null) {
                        persisterLastSessionRefreshStore.reset();
                    }
                    if (lastSessionRefreshStore != null) {
                        lastSessionRefreshStore.reset();
                    }
                    if (offlineLastSessionRefreshStore != null) {
                        offlineLastSessionRefreshStore.reset();
                    }
                }
            }
        });
        if (Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS)) {
            persistentSessionsWorker = new PersistentSessionsWorker(factory,
                    asyncQueuePersistentUpdate,
                    maxBatchSize);
            persistentSessionsWorker.start();
        }
    }

    // Max count of worker errors. Initialization will end with exception when this number is reached
    private int getMaxErrors() {
        return config.getInt("maxErrors", 20);
    }

    // Count of sessions to be computed in each segment
    private int getSessionsPerSegment() {
        return config.getInt("sessionsPerSegment", 64);
    }

    private int getTimeoutForPreloadingSessionsSeconds() {
        Integer timeout = config.getInt("sessionsPreloadTimeoutInSeconds", null);
        return timeout != null ? timeout : Environment.getServerStartupTimeout();
    }

    private int getStalledTimeoutInSeconds(int defaultTimeout) {
         return config.getInt("sessionPreloadStalledTimeoutInSeconds", defaultTimeout);
    }

    public void initializeLastSessionRefreshStore(final KeycloakSessionFactory sessionFactory) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                // Initialize persister for periodically doing bulk DB updates of lastSessionRefresh timestamps of refreshed sessions
                persisterLastSessionRefreshStore = new PersisterLastSessionRefreshStoreFactory().createAndInit(session, true);
            }
        });
    }


    protected void registerClusterListeners(KeycloakSession session) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        ClusterProvider cluster = session.getProvider(ClusterProvider.class);

        cluster.registerListener(REALM_REMOVED_SESSION_EVENT,
                new AbstractUserSessionClusterListener<RealmRemovedSessionEvent, UserSessionProvider>(sessionFactory, UserSessionProvider.class) {

            @Override
            protected void eventReceived(UserSessionProvider provider, RealmRemovedSessionEvent sessionEvent) {
                if (provider instanceof InfinispanUserSessionProvider) {
                    ((InfinispanUserSessionProvider) provider).onRealmRemovedEvent(sessionEvent.getRealmId());
                } else if (provider instanceof PersistentUserSessionProvider) {
                    ((PersistentUserSessionProvider) provider).onRealmRemovedEvent(sessionEvent.getRealmId());
                }
            }

        });

        cluster.registerListener(REMOVE_USER_SESSIONS_EVENT,
                new AbstractUserSessionClusterListener<RemoveUserSessionsEvent, UserSessionProvider>(sessionFactory, UserSessionProvider.class) {

            @Override
            protected void eventReceived(UserSessionProvider provider, RemoveUserSessionsEvent sessionEvent) {
                if (provider instanceof InfinispanUserSessionProvider) {
                    ((InfinispanUserSessionProvider) provider).onRemoveUserSessionsEvent(sessionEvent.getRealmId());
                } else if (provider instanceof PersistentUserSessionProvider) {
                    ((PersistentUserSessionProvider) provider).onRemoveUserSessionsEvent(sessionEvent.getRealmId());
                }
            }

        });

        log.debug("Registered cluster listeners");
    }


    protected void checkRemoteCaches(KeycloakSession session) {
        this.remoteCacheInvoker = new RemoteCacheInvoker();

        InfinispanConnectionProvider ispn = session.getProvider(InfinispanConnectionProvider.class);

        Cache<String, SessionEntityWrapper<UserSessionEntity>> sessionsCache = ispn.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
        RemoteCache sessionsRemoteCache = checkRemoteCache(session, sessionsCache, SessionTimeouts::getUserSessionLifespanMs, SessionTimeouts::getUserSessionMaxIdleMs);

        if (sessionsRemoteCache != null) {
            lastSessionRefreshStore = new CrossDCLastSessionRefreshStoreFactory().createAndInit(session, sessionsCache, false);
        }

        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionsCache = ispn.getCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
        checkRemoteCache(session, clientSessionsCache, SessionTimeouts::getClientSessionLifespanMs, SessionTimeouts::getClientSessionMaxIdleMs);

        Cache<String, SessionEntityWrapper<UserSessionEntity>> offlineSessionsCache = ispn.getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
        RemoteCache offlineSessionsRemoteCache = checkRemoteCache(session, offlineSessionsCache, this::deriveOfflineSessionCacheEntryLifespanMs, SessionTimeouts::getOfflineSessionMaxIdleMs);

        if (offlineSessionsRemoteCache != null) {
            offlineLastSessionRefreshStore = new CrossDCLastSessionRefreshStoreFactory().createAndInit(session, offlineSessionsCache, true);
        }

        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> offlineClientSessionsCache = ispn.getCache(InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME);
        checkRemoteCache(session, offlineClientSessionsCache, this::deriveOfflineClientSessionCacheEntryLifespanOverrideMs, SessionTimeouts::getOfflineClientSessionMaxIdleMs);
    }

    private <K, V extends SessionEntity> RemoteCache checkRemoteCache(KeycloakSession session, Cache<K, SessionEntityWrapper<V>> ispnCache,
                                                                      SessionFunction<V> lifespanMsLoader, SessionFunction<V> maxIdleTimeMsLoader) {
        Set<RemoteStore> remoteStores = InfinispanUtil.getRemoteStores(ispnCache);

        if (remoteStores.isEmpty()) {
            log.debugf("No remote store configured for cache '%s'", ispnCache.getName());
            return null;
        } else {
            log.infof("Remote store configured for cache '%s'", ispnCache.getName());

            RemoteCache<K, SessionEntityWrapper<V>> remoteCache = (RemoteCache) remoteStores.iterator().next().getRemoteCache();

            if (remoteCache == null) {
                throw new IllegalStateException("No remote cache available for the infinispan cache: " + ispnCache.getName());
            }

            remoteCacheInvoker.addRemoteCache(ispnCache.getName(), remoteCache);

            Runnable onFailover = null;
            if (Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS)) {
                // If persistent sessions are enabled, we want to clear the local caches when a failover of the listener on the remote store changes as we might have missed some of the remote store events
                // which might have been triggered by another Keycloak site connected to the same remote Infinispan cluster.
                // Due to this, we can be sure that we never have outdated information in our local cache. All entries will be re-loaded from the remote cache or the database as necessary lazily.
                onFailover = ispnCache::clear;
            }

            RemoteCacheSessionListener hotrodListener = RemoteCacheSessionListener.createListener(session, ispnCache, remoteCache, lifespanMsLoader, maxIdleTimeMsLoader, onFailover);
            remoteCache.addClientListener(hotrodListener);
            return remoteCache;
        }
    }

    protected Long deriveOfflineSessionCacheEntryLifespanMs(RealmModel realm, ClientModel client, UserSessionEntity entity) {

        long configuredOfflineSessionLifespan = SessionTimeouts.getOfflineSessionLifespanMs(realm, client, entity);

        if (offlineSessionCacheEntryLifespanOverride == -1) {
            // override not configured -> take the value from realm settings
            return configuredOfflineSessionLifespan;
        }

        if (configuredOfflineSessionLifespan == -1) {
            // "Offline Session Max Limited" is "off"
            return TimeUnit.SECONDS.toMillis(offlineSessionCacheEntryLifespanOverride);
        }

        // both values are configured, Offline Session Max could be smaller than the override, so we use the minimum of both
        return Math.min(TimeUnit.SECONDS.toMillis(offlineSessionCacheEntryLifespanOverride), configuredOfflineSessionLifespan);
    }

    protected Long deriveOfflineClientSessionCacheEntryLifespanOverrideMs(RealmModel realm, ClientModel client, AuthenticatedClientSessionEntity entity) {

        long configuredOfflineClientSessionLifespan = SessionTimeouts.getOfflineClientSessionLifespanMs(realm, client, entity);

        if (offlineClientSessionCacheEntryLifespanOverride == -1) {
            // override not configured -> take the value from realm settings
            return configuredOfflineClientSessionLifespan;
        }

        if (configuredOfflineClientSessionLifespan == -1) {
            // "Offline Session Max Limited" is "off"
            return TimeUnit.SECONDS.toMillis(offlineClientSessionCacheEntryLifespanOverride);
        }

        // both values are configured, Offline Session Max could be smaller than the override, so we use the minimum of both
        return Math.min(TimeUnit.SECONDS.toMillis(offlineClientSessionCacheEntryLifespanOverride), configuredOfflineClientSessionLifespan);
    }


    private void loadSessionsFromRemoteCaches(KeycloakSession session) {
        for (String cacheName : remoteCacheInvoker.getRemoteCacheNames()) {
            loadSessionsFromRemoteCache(session.getKeycloakSessionFactory(), cacheName, getSessionsPerSegment(), getMaxErrors());
        }
    }


    private void loadSessionsFromRemoteCache(final KeycloakSessionFactory sessionFactory, String cacheName, final int sessionsPerSegment, final int maxErrors) {
        log.debugf("Check pre-loading sessions from remote cache '%s'", cacheName);

        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
                Cache<String, Serializable> workCache = connections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);
                int defaultStateTransferTimeout = (int) (connections.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME)
                  .getCacheConfiguration().clustering().stateTransfer().timeout() / 1000);

                InfinispanCacheInitializer initializer = new InfinispanCacheInitializer(sessionFactory, workCache,
                        new RemoteCacheSessionsLoader(cacheName, sessionsPerSegment), "remoteCacheLoad::" + cacheName, maxErrors,
                        getStalledTimeoutInSeconds(defaultStateTransferTimeout));
                initializer.loadSessions();
            }
        });

        log.debugf("Pre-loading sessions from remote cache '%s' finished", cacheName);
    }

    @Override
    public void close() {
        if (persistentSessionsWorker != null) {
            persistentSessionsWorker.stop();
        }
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public int order() {
        return PROVIDER_PRIORITY;
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        Map<String, String> info = new HashMap<>();
        info.put(CONFIG_OFFLINE_SESSION_CACHE_ENTRY_LIFESPAN_OVERRIDE, Long.toString(offlineSessionCacheEntryLifespanOverride));
        info.put(CONFIG_OFFLINE_CLIENT_SESSION_CACHE_ENTRY_LIFESPAN_OVERRIDE, Long.toString(offlineClientSessionCacheEntryLifespanOverride));
        info.put(CONFIG_MAX_BATCH_SIZE, Integer.toString(maxBatchSize));
        return info;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();

        builder.property()
                .name(CONFIG_MAX_BATCH_SIZE)
                .type("int")
                .helpText("Maximum size of a batch size (only applicable to persistent sessions")
                .defaultValue(DEFAULT_MAX_BATCH_SIZE)
                .add();

        builder.property()
                .name(CONFIG_OFFLINE_CLIENT_SESSION_CACHE_ENTRY_LIFESPAN_OVERRIDE)
                .type("int")
                .helpText("Override how long offline client sessions should be kept in memory")
                .add();

        builder.property()
                .name(CONFIG_OFFLINE_SESSION_CACHE_ENTRY_LIFESPAN_OVERRIDE)
                .type("int")
                .helpText("Override how long offline user sessions should be kept in memory")
                .add();

        return builder.build();
    }

}

