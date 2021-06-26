/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.UserLoginFailureProvider;
import org.keycloak.models.UserLoginFailureProviderFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.events.AbstractUserSessionClusterListener;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RemoveAllUserLoginFailuresEvent;
import org.keycloak.models.sessions.infinispan.initializer.InfinispanCacheInitializer;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheInvoker;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheSessionListener;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheSessionsLoader;
import org.keycloak.connections.infinispan.InfinispanUtil;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;

import java.io.Serializable;
import java.util.Set;
import java.util.function.BiFunction;
import static org.keycloak.models.sessions.infinispan.InfinispanAuthenticationSessionProviderFactory.PROVIDER_PRIORITY;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class InfinispanUserLoginFailureProviderFactory implements UserLoginFailureProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanUserLoginFailureProviderFactory.class);

    public static final String PROVIDER_ID = "infinispan";

    public static final String REALM_REMOVED_SESSION_EVENT = "REALM_REMOVED_EVENT_SESSIONS";

    public static final String REMOVE_ALL_LOGIN_FAILURES_EVENT = "REMOVE_ALL_LOGIN_FAILURES_EVENT";

    private Config.Scope config;

    private RemoteCacheInvoker remoteCacheInvoker;

    @Override
    public UserLoginFailureProvider create(KeycloakSession session) {
        InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
        Cache<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>> loginFailures = connections.getCache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME);

        return new InfinispanUserLoginFailureProvider(session, remoteCacheInvoker, loginFailures);
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(final KeycloakSessionFactory factory) {
        this.remoteCacheInvoker = new RemoteCacheInvoker();

        factory.register(event -> {
            if (event instanceof PostMigrationEvent) {
                KeycloakModelUtils.runJobInTransaction(factory, (KeycloakSession session) -> {
                    checkRemoteCaches(session);
                    registerClusterListeners(session);
                    loadLoginFailuresFromRemoteCaches(session);
                });
            } else if (event instanceof UserModel.UserRemovedEvent) {
                UserModel.UserRemovedEvent userRemovedEvent = (UserModel.UserRemovedEvent) event;

                UserLoginFailureProvider provider = userRemovedEvent.getKeycloakSession().getProvider(UserLoginFailureProvider.class, getId());
                provider.removeUserLoginFailure(userRemovedEvent.getRealm(), userRemovedEvent.getUser().getId());
            }
        });
    }

    protected void registerClusterListeners(KeycloakSession session) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        ClusterProvider cluster = session.getProvider(ClusterProvider.class);

        cluster.registerListener(REALM_REMOVED_SESSION_EVENT,
                new AbstractUserSessionClusterListener<RealmRemovedSessionEvent, UserLoginFailureProvider>(sessionFactory, UserLoginFailureProvider.class) {

                    @Override
                    protected void eventReceived(KeycloakSession session, UserLoginFailureProvider provider, RealmRemovedSessionEvent sessionEvent) {
                        if (provider instanceof InfinispanUserLoginFailureProvider) {
                            ((InfinispanUserLoginFailureProvider) provider).removeAllLocalUserLoginFailuresEvent(sessionEvent.getRealmId());
                        }
                    }
        });

        cluster.registerListener(REMOVE_ALL_LOGIN_FAILURES_EVENT,
                new AbstractUserSessionClusterListener<RemoveAllUserLoginFailuresEvent, UserLoginFailureProvider>(sessionFactory, UserLoginFailureProvider.class) {

            @Override
            protected void eventReceived(KeycloakSession session, UserLoginFailureProvider provider, RemoveAllUserLoginFailuresEvent sessionEvent) {
                if (provider instanceof InfinispanUserLoginFailureProvider) {
                    ((InfinispanUserLoginFailureProvider) provider).removeAllLocalUserLoginFailuresEvent(sessionEvent.getRealmId());
                }
            }

        });

        log.debug("Registered cluster listeners");
    }


    protected void checkRemoteCaches(KeycloakSession session) {
        InfinispanConnectionProvider ispn = session.getProvider(InfinispanConnectionProvider.class);

        Cache<LoginFailureKey, SessionEntityWrapper<LoginFailureEntity>> loginFailuresCache = ispn.getCache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME);
        checkRemoteCache(session, loginFailuresCache, (RealmModel realm) ->
                Time.toMillis(realm.getMaxDeltaTimeSeconds()), SessionTimeouts::getLoginFailuresLifespanMs, SessionTimeouts::getLoginFailuresMaxIdleMs);
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

    // Max count of worker errors. Initialization will end with exception when this number is reached
    private int getMaxErrors() {
        return config.getInt("maxErrors", 20);
    }

    // Count of sessions to be computed in each segment
    private int getSessionsPerSegment() {
        return config.getInt("sessionsPerSegment", 64);
    }

    private void loadLoginFailuresFromRemoteCaches(KeycloakSession session) {
        for (String cacheName : remoteCacheInvoker.getRemoteCacheNames()) {
            loadLoginFailuresFromRemoteCaches(session.getKeycloakSessionFactory(), cacheName, getSessionsPerSegment(), getMaxErrors());
        }
    }

    private void loadLoginFailuresFromRemoteCaches(final KeycloakSessionFactory sessionFactory, String cacheName, final int sessionsPerSegment, final int maxErrors) {
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

        log.debugf("Pre-loading login failures from remote cache '%s' finished", cacheName);
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
