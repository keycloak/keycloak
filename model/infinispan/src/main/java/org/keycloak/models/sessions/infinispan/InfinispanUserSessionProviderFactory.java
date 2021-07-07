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
import org.keycloak.common.util.Environment;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.changes.sessions.CrossDCLastSessionRefreshStore;
import org.keycloak.models.sessions.infinispan.changes.sessions.CrossDCLastSessionRefreshStoreFactory;
import org.keycloak.models.sessions.infinispan.changes.sessions.PersisterLastSessionRefreshStore;
import org.keycloak.models.sessions.infinispan.changes.sessions.PersisterLastSessionRefreshStoreFactory;
import org.keycloak.models.sessions.infinispan.initializer.CacheInitializer;
import org.keycloak.models.sessions.infinispan.initializer.DBLockBasedCacheInitializer;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheInvoker;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.events.AbstractUserSessionClusterListener;
import org.keycloak.models.sessions.infinispan.events.ClientRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RemoveUserSessionsEvent;
import org.keycloak.models.sessions.infinispan.initializer.InfinispanCacheInitializer;
import org.keycloak.models.sessions.infinispan.initializer.OfflinePersistentUserSessionLoader;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheSessionListener;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheSessionsLoader;
import org.keycloak.models.sessions.infinispan.util.InfinispanKeyGenerator;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import static org.keycloak.models.sessions.infinispan.InfinispanAuthenticationSessionProviderFactory.PROVIDER_PRIORITY;

public class InfinispanUserSessionProviderFactory implements UserSessionProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanUserSessionProviderFactory.class);

    public static final String PROVIDER_ID = "infinispan";

    public static final String REALM_REMOVED_SESSION_EVENT = "REALM_REMOVED_EVENT_SESSIONS";

    public static final String CLIENT_REMOVED_SESSION_EVENT = "CLIENT_REMOVED_SESSION_SESSIONS";

    public static final String REMOVE_USER_SESSIONS_EVENT = "REMOVE_USER_SESSIONS_EVENT";

    private Config.Scope config;

    private RemoteCacheInvoker remoteCacheInvoker;
    private CrossDCLastSessionRefreshStore lastSessionRefreshStore;
    private CrossDCLastSessionRefreshStore offlineLastSessionRefreshStore;
    private PersisterLastSessionRefreshStore persisterLastSessionRefreshStore;
    private InfinispanKeyGenerator keyGenerator;

    @Override
    public InfinispanUserSessionProvider create(KeycloakSession session) {
        InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = connections.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
        Cache<String, SessionEntityWrapper<UserSessionEntity>> offlineSessionsCache = connections.getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionCache = connections.getCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> offlineClientSessionsCache = connections.getCache(InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME);

        boolean loadOfflineSessionsStatsFromDatabase = !isPreloadingOfflineSessionsFromDatabaseEnabled();

        return new InfinispanUserSessionProvider(session, remoteCacheInvoker, lastSessionRefreshStore, offlineLastSessionRefreshStore,
                persisterLastSessionRefreshStore, keyGenerator, cache, offlineSessionsCache, clientSessionCache, offlineClientSessionsCache, loadOfflineSessionsStatsFromDatabase);
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
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
                        loadPersistentSessions(factory, getMaxErrors(), getSessionsPerSegment());
                        registerClusterListeners(session);
                        loadSessionsFromRemoteCaches(session);

                    }, preloadTransactionTimeout);

                } else if (event instanceof UserModel.UserRemovedEvent) {
                    UserModel.UserRemovedEvent userRemovedEvent = (UserModel.UserRemovedEvent) event;

                    InfinispanUserSessionProvider provider = (InfinispanUserSessionProvider) userRemovedEvent.getKeycloakSession().getProvider(UserSessionProvider.class, getId());
                    provider.onUserRemoved(userRemovedEvent.getRealm(), userRemovedEvent.getUser());
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
    }

    private boolean isPreloadingOfflineSessionsFromDatabaseEnabled() {
        return config.getBoolean("preloadOfflineSessionsFromDatabase", true);
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


    @Override
    public void loadPersistentSessions(final KeycloakSessionFactory sessionFactory, final int maxErrors, final int sessionsPerSegment) {

        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {

                if (isPreloadingOfflineSessionsFromDatabaseEnabled()) {
                    // only preload offline-sessions if necessary
                    log.debug("Start pre-loading userSessions from persistent storage");

                    InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
                    Cache<String, Serializable> workCache = connections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

                    InfinispanCacheInitializer ispnInitializer = new InfinispanCacheInitializer(sessionFactory, workCache,
                            new OfflinePersistentUserSessionLoader(sessionsPerSegment), "offlineUserSessions", sessionsPerSegment, maxErrors);

                    // DB-lock to ensure that persistent sessions are loaded from DB just on one DC. The other DCs will load them from remote cache.
                    CacheInitializer initializer = new DBLockBasedCacheInitializer(session, ispnInitializer);

                    initializer.initCache();
                    initializer.loadSessions();

                    log.debug("Pre-loading userSessions from persistent storage finished");
                } else {
                    log.debug("Skipping pre-loading of userSessions from persistent storage");
                }

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
            protected void eventReceived(KeycloakSession session, UserSessionProvider provider, RealmRemovedSessionEvent sessionEvent) {
                if (provider instanceof InfinispanUserSessionProvider) {
                    ((InfinispanUserSessionProvider) provider).onRealmRemovedEvent(sessionEvent.getRealmId());
                }
            }

        });

        cluster.registerListener(CLIENT_REMOVED_SESSION_EVENT,
                new AbstractUserSessionClusterListener<ClientRemovedSessionEvent, UserSessionProvider>(sessionFactory, UserSessionProvider.class) {

            @Override
            protected void eventReceived(KeycloakSession session, UserSessionProvider provider, ClientRemovedSessionEvent sessionEvent) {
                if (provider instanceof InfinispanUserSessionProvider) {
                    ((InfinispanUserSessionProvider) provider).onClientRemovedEvent(sessionEvent.getRealmId(), sessionEvent.getClientUuid());
                }
            }

        });

        cluster.registerListener(REMOVE_USER_SESSIONS_EVENT,
                new AbstractUserSessionClusterListener<RemoveUserSessionsEvent, UserSessionProvider>(sessionFactory, UserSessionProvider.class) {

            @Override
            protected void eventReceived(KeycloakSession session, UserSessionProvider provider, RemoveUserSessionsEvent sessionEvent) {
                if (provider instanceof InfinispanUserSessionProvider) {
                    ((InfinispanUserSessionProvider) provider).onRemoveUserSessionsEvent(sessionEvent.getRealmId());
                }
            }

        });

        log.debug("Registered cluster listeners");
    }


    protected void checkRemoteCaches(KeycloakSession session) {
        this.remoteCacheInvoker = new RemoteCacheInvoker();

        InfinispanConnectionProvider ispn = session.getProvider(InfinispanConnectionProvider.class);

        Cache<String, SessionEntityWrapper<UserSessionEntity>> sessionsCache = ispn.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
        RemoteCache sessionsRemoteCache = checkRemoteCache(session, sessionsCache, (RealmModel realm) -> {
            // We won't write to the remoteCache during token refresh, so the timeout needs to be longer.
            return Time.toMillis(realm.getSsoSessionMaxLifespan());
        }, SessionTimeouts::getUserSessionLifespanMs, SessionTimeouts::getUserSessionMaxIdleMs);

        if (sessionsRemoteCache != null) {
            lastSessionRefreshStore = new CrossDCLastSessionRefreshStoreFactory().createAndInit(session, sessionsCache, false);
        }

        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionsCache = ispn.getCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
        checkRemoteCache(session, clientSessionsCache, (RealmModel realm) -> {
            // We won't write to the remoteCache during token refresh, so the timeout needs to be longer.
            return Time.toMillis(realm.getSsoSessionMaxLifespan());
        }, SessionTimeouts::getClientSessionLifespanMs, SessionTimeouts::getClientSessionMaxIdleMs);

        Cache<String, SessionEntityWrapper<UserSessionEntity>> offlineSessionsCache = ispn.getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
        RemoteCache offlineSessionsRemoteCache = checkRemoteCache(session, offlineSessionsCache, (RealmModel realm) -> {
            return Time.toMillis(realm.getOfflineSessionIdleTimeout());
        }, SessionTimeouts::getOfflineSessionLifespanMs, SessionTimeouts::getOfflineSessionMaxIdleMs);

        if (offlineSessionsRemoteCache != null) {
            offlineLastSessionRefreshStore = new CrossDCLastSessionRefreshStoreFactory().createAndInit(session, offlineSessionsCache, true);
        }

        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> offlineClientSessionsCache = ispn.getCache(InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME);
        checkRemoteCache(session, offlineClientSessionsCache, (RealmModel realm) -> {
            return Time.toMillis(realm.getOfflineSessionIdleTimeout());
        }, SessionTimeouts::getOfflineClientSessionLifespanMs, SessionTimeouts::getOfflineClientSessionMaxIdleMs);
    }

    private <K, V extends SessionEntity> RemoteCache checkRemoteCache(KeycloakSession session, Cache<K, SessionEntityWrapper<V>> ispnCache, RemoteCacheInvoker.MaxIdleTimeLoader maxIdleLoader,
                                                                      BiFunction<RealmModel, V, Long> lifespanMsLoader, BiFunction<RealmModel, V, Long> maxIdleTimeMsLoader) {
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

            remoteCacheInvoker.addRemoteCache(ispnCache.getName(), remoteCache, maxIdleLoader);

            RemoteCacheSessionListener hotrodListener = RemoteCacheSessionListener.createListener(session, ispnCache, remoteCache, lifespanMsLoader, maxIdleTimeMsLoader);
            remoteCache.addClientListener(hotrodListener);
            return remoteCache;
        }
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

                InfinispanCacheInitializer initializer = new InfinispanCacheInitializer(sessionFactory, workCache,
                        new RemoteCacheSessionsLoader(cacheName, sessionsPerSegment), "remoteCacheLoad::" + cacheName, sessionsPerSegment, maxErrors);

                initializer.initCache();
                initializer.loadSessions();
            }

        });

        log.debugf("Pre-loading sessions from remote cache '%s' finished", cacheName);
    }


    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public int order() {
        return PROVIDER_PRIORITY;
    }
}

