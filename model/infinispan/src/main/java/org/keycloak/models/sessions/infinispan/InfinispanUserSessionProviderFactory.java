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
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.changes.sessions.LastSessionRefreshStore;
import org.keycloak.models.sessions.infinispan.changes.sessions.LastSessionRefreshStoreFactory;
import org.keycloak.models.sessions.infinispan.initializer.BaseCacheInitializer;
import org.keycloak.models.sessions.infinispan.initializer.CacheInitializer;
import org.keycloak.models.sessions.infinispan.initializer.DBLockBasedCacheInitializer;
import org.keycloak.models.sessions.infinispan.initializer.SingleWorkerCacheInitializer;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheInvoker;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.events.AbstractUserSessionClusterListener;
import org.keycloak.models.sessions.infinispan.events.ClientRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RemoveAllUserLoginFailuresEvent;
import org.keycloak.models.sessions.infinispan.events.RemoveUserSessionsEvent;
import org.keycloak.models.sessions.infinispan.initializer.InfinispanCacheInitializer;
import org.keycloak.models.sessions.infinispan.initializer.OfflinePersistentUserSessionLoader;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheSessionListener;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheSessionsLoader;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

import java.io.Serializable;
import java.util.Set;

public class InfinispanUserSessionProviderFactory implements UserSessionProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanUserSessionProviderFactory.class);

    public static final String PROVIDER_ID = "infinispan";

    public static final String REALM_REMOVED_SESSION_EVENT = "REALM_REMOVED_EVENT_SESSIONS";

    public static final String CLIENT_REMOVED_SESSION_EVENT = "CLIENT_REMOVED_SESSION_SESSIONS";

    public static final String REMOVE_USER_SESSIONS_EVENT = "REMOVE_USER_SESSIONS_EVENT";

    public static final String REMOVE_ALL_LOGIN_FAILURES_EVENT = "REMOVE_ALL_LOGIN_FAILURES_EVENT";

    private Config.Scope config;

    private RemoteCacheInvoker remoteCacheInvoker;
    private LastSessionRefreshStore lastSessionRefreshStore;
    private LastSessionRefreshStore offlineLastSessionRefreshStore;

    @Override
    public InfinispanUserSessionProvider create(KeycloakSession session) {
        InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = connections.getCache(InfinispanConnectionProvider.SESSION_CACHE_NAME);
        Cache<String, SessionEntityWrapper<UserSessionEntity>> offlineSessionsCache = connections.getCache(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME);
        Cache<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>> loginFailures = connections.getCache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME);

        return new InfinispanUserSessionProvider(session, remoteCacheInvoker, lastSessionRefreshStore, offlineLastSessionRefreshStore, cache, offlineSessionsCache, loginFailures);
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
                    KeycloakSession session = ((PostMigrationEvent) event).getSession();

                    checkRemoteCaches(session);
                    loadPersistentSessions(factory, getMaxErrors(), getSessionsPerSegment());
                    registerClusterListeners(session);
                    loadSessionsFromRemoteCaches(session);

                } else if (event instanceof UserModel.UserRemovedEvent) {
                    UserModel.UserRemovedEvent userRemovedEvent = (UserModel.UserRemovedEvent) event;

                    InfinispanUserSessionProvider provider = (InfinispanUserSessionProvider) userRemovedEvent.getKeycloakSession().getProvider(UserSessionProvider.class, getId());
                    provider.onUserRemoved(userRemovedEvent.getRealm(), userRemovedEvent.getUser());
                }
            }
        });
    }

    // Max count of worker errors. Initialization will end with exception when this number is reached
    private int getMaxErrors() {
        return config.getInt("maxErrors", 20);
    }

    // Count of sessions to be computed in each segment
    private int getSessionsPerSegment() {
        return config.getInt("sessionsPerSegment", 100);
    }


    @Override
    public void loadPersistentSessions(final KeycloakSessionFactory sessionFactory, final int maxErrors, final int sessionsPerSegment) {
        log.debug("Start pre-loading userSessions from persistent storage");

        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
                Cache<String, Serializable> workCache = connections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

                InfinispanCacheInitializer ispnInitializer = new InfinispanCacheInitializer(sessionFactory, workCache, new OfflinePersistentUserSessionLoader(), "offlineUserSessions", sessionsPerSegment, maxErrors);

                // DB-lock to ensure that persistent sessions are loaded from DB just on one DC. The other DCs will load them from remote cache.
                CacheInitializer initializer = new DBLockBasedCacheInitializer(session, ispnInitializer);

                initializer.initCache();
                initializer.loadSessions();
            }

        });

        log.debug("Pre-loading userSessions from persistent storage finished");
    }


    protected void registerClusterListeners(KeycloakSession session) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        ClusterProvider cluster = session.getProvider(ClusterProvider.class);

        cluster.registerListener(REALM_REMOVED_SESSION_EVENT, new AbstractUserSessionClusterListener<RealmRemovedSessionEvent>(sessionFactory) {

            @Override
            protected void eventReceived(KeycloakSession session, InfinispanUserSessionProvider provider, RealmRemovedSessionEvent sessionEvent) {
                provider.onRealmRemovedEvent(sessionEvent.getRealmId());
            }

        });

        cluster.registerListener(CLIENT_REMOVED_SESSION_EVENT, new AbstractUserSessionClusterListener<ClientRemovedSessionEvent>(sessionFactory) {

            @Override
            protected void eventReceived(KeycloakSession session, InfinispanUserSessionProvider provider, ClientRemovedSessionEvent sessionEvent) {
                provider.onClientRemovedEvent(sessionEvent.getRealmId(), sessionEvent.getClientUuid());
            }

        });

        cluster.registerListener(REMOVE_USER_SESSIONS_EVENT, new AbstractUserSessionClusterListener<RemoveUserSessionsEvent>(sessionFactory) {

            @Override
            protected void eventReceived(KeycloakSession session, InfinispanUserSessionProvider provider, RemoveUserSessionsEvent sessionEvent) {
                provider.onRemoveUserSessionsEvent(sessionEvent.getRealmId());
            }

        });

        cluster.registerListener(REMOVE_ALL_LOGIN_FAILURES_EVENT, new AbstractUserSessionClusterListener<RemoveAllUserLoginFailuresEvent>(sessionFactory) {

            @Override
            protected void eventReceived(KeycloakSession session, InfinispanUserSessionProvider provider, RemoveAllUserLoginFailuresEvent sessionEvent) {
                provider.onRemoveAllUserLoginFailuresEvent(sessionEvent.getRealmId());
            }

        });

        log.debug("Registered cluster listeners");
    }


    protected void checkRemoteCaches(KeycloakSession session) {
        this.remoteCacheInvoker = new RemoteCacheInvoker();

        InfinispanConnectionProvider ispn = session.getProvider(InfinispanConnectionProvider.class);

        Cache sessionsCache = ispn.getCache(InfinispanConnectionProvider.SESSION_CACHE_NAME);
        boolean sessionsRemoteCache = checkRemoteCache(session, sessionsCache, (RealmModel realm) -> {
            return realm.getSsoSessionIdleTimeout() * 1000;
        });

        if (sessionsRemoteCache) {
            lastSessionRefreshStore = new LastSessionRefreshStoreFactory().createAndInit(session, sessionsCache, false);
        }


        Cache offlineSessionsCache = ispn.getCache(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME);
        boolean offlineSessionsRemoteCache = checkRemoteCache(session, offlineSessionsCache, (RealmModel realm) -> {
            return realm.getOfflineSessionIdleTimeout() * 1000;
        });

        if (offlineSessionsRemoteCache) {
            offlineLastSessionRefreshStore = new LastSessionRefreshStoreFactory().createAndInit(session, offlineSessionsCache, true);
        }

        Cache loginFailuresCache = ispn.getCache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME);
        boolean loginFailuresRemoteCache = checkRemoteCache(session, loginFailuresCache, (RealmModel realm) -> {
            return realm.getMaxDeltaTimeSeconds() * 1000;
        });
    }

    private boolean checkRemoteCache(KeycloakSession session, Cache ispnCache, RemoteCacheInvoker.MaxIdleTimeLoader maxIdleLoader) {
        Set<RemoteStore> remoteStores = InfinispanUtil.getRemoteStores(ispnCache);

        if (remoteStores.isEmpty()) {
            log.debugf("No remote store configured for cache '%s'", ispnCache.getName());
            return false;
        } else {
            log.infof("Remote store configured for cache '%s'", ispnCache.getName());

            RemoteCache remoteCache = remoteStores.iterator().next().getRemoteCache();

            remoteCacheInvoker.addRemoteCache(ispnCache.getName(), remoteCache, maxIdleLoader);

            RemoteCacheSessionListener hotrodListener = RemoteCacheSessionListener.createListener(session, ispnCache, remoteCache);
            remoteCache.addClientListener(hotrodListener);
            return true;
        }
    }


    private void loadSessionsFromRemoteCaches(KeycloakSession session) {
        for (String cacheName : remoteCacheInvoker.getRemoteCacheNames()) {
            loadSessionsFromRemoteCache(session.getKeycloakSessionFactory(), cacheName, getSessionsPerSegment(), getMaxErrors());
        }
    }


    private void loadSessionsFromRemoteCache(final KeycloakSessionFactory sessionFactory, String cacheName, final int sessionsPerSegment, final int maxErrors) {
        log.debugf("Check pre-loading userSessions from remote cache '%s'", cacheName);

        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
                Cache<String, Serializable> workCache = connections.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

                InfinispanCacheInitializer initializer = new InfinispanCacheInitializer(sessionFactory, workCache, new RemoteCacheSessionsLoader(cacheName), "remoteCacheLoad::" + cacheName, sessionsPerSegment, maxErrors);

                initializer.initCache();
                initializer.loadSessions();
            }

        });

        log.debugf("Pre-loading userSessions from remote cache '%s' finished", cacheName);
    }


    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}

