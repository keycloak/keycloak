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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyGenerator;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.changes.PersistentSessionsWorker;
import org.keycloak.models.sessions.infinispan.changes.PersistentUpdate;
import org.keycloak.models.sessions.infinispan.changes.SerializeExecutionsByKey;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.sessions.PersisterLastSessionRefreshStore;
import org.keycloak.models.sessions.infinispan.changes.sessions.PersisterLastSessionRefreshStoreFactory;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.events.AbstractUserSessionClusterListener;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RemoveUserSessionsEvent;
import org.keycloak.models.sessions.infinispan.util.InfinispanKeyGenerator;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

public class InfinispanUserSessionProviderFactory implements UserSessionProviderFactory<UserSessionProvider>, ServerInfoAwareProviderFactory, EnvironmentDependentProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanUserSessionProviderFactory.class);

    public static final String REALM_REMOVED_SESSION_EVENT = "REALM_REMOVED_EVENT_SESSIONS";

    public static final String REMOVE_USER_SESSIONS_EVENT = "REMOVE_USER_SESSIONS_EVENT";
    public static final String CONFIG_OFFLINE_SESSION_CACHE_ENTRY_LIFESPAN_OVERRIDE = "offlineSessionCacheEntryLifespanOverride";
    public static final String CONFIG_OFFLINE_CLIENT_SESSION_CACHE_ENTRY_LIFESPAN_OVERRIDE = "offlineClientSessionCacheEntryLifespanOverride";
    public static final String CONFIG_MAX_BATCH_SIZE = "maxBatchSize";
    public static final int DEFAULT_MAX_BATCH_SIZE = Math.max(Runtime.getRuntime().availableProcessors(), 2);
    public static final String CONFIG_USE_CACHES = "useCaches";
    private static final boolean DEFAULT_USE_CACHES = true;
    public static final String CONFIG_USE_BATCHES = "useBatches";
    private static final boolean DEFAULT_USE_BATCHES = true;

    private long offlineSessionCacheEntryLifespanOverride;

    private long offlineClientSessionCacheEntryLifespanOverride;

    private Config.Scope config;

    private PersisterLastSessionRefreshStore persisterLastSessionRefreshStore;
    private InfinispanKeyGenerator keyGenerator;
    SerializeExecutionsByKey<String> serializerSession = new SerializeExecutionsByKey<>();
    SerializeExecutionsByKey<String> serializerOfflineSession = new SerializeExecutionsByKey<>();
    SerializeExecutionsByKey<UUID> serializerClientSession = new SerializeExecutionsByKey<>();
    SerializeExecutionsByKey<UUID> serializerOfflineClientSession = new SerializeExecutionsByKey<>();
    ArrayBlockingQueue<PersistentUpdate> asyncQueuePersistentUpdate;
    private PersistentSessionsWorker persistentSessionsWorker;
    private int maxBatchSize;
    private boolean useCaches;
    private boolean useBatches;

    @Override
    public UserSessionProvider create(KeycloakSession session) {
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = null;
        Cache<String, SessionEntityWrapper<UserSessionEntity>> offlineSessionsCache = null;
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionCache = null;
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> offlineClientSessionsCache = null;

        if (useCaches) {
            InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
            cache = connections.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            offlineSessionsCache = connections.getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
            clientSessionCache = connections.getCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
            offlineClientSessionsCache = connections.getCache(InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME);
        }

        if (MultiSiteUtils.isPersistentSessionsEnabled()) {
            return new PersistentUserSessionProvider(
                    session,
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
        // Do not use caches for sessions if explicitly disabled or if embedded caches are not used
        useCaches = config.getBoolean(CONFIG_USE_CACHES, DEFAULT_USE_CACHES) && InfinispanUtils.isEmbeddedInfinispan();
        useBatches = config.getBoolean(CONFIG_USE_BATCHES, DEFAULT_USE_BATCHES) && MultiSiteUtils.isPersistentSessionsEnabled();
        if (useBatches) {
            asyncQueuePersistentUpdate = new ArrayBlockingQueue<>(1000);
        }
    }

    @Override
    public void postInit(final KeycloakSessionFactory factory) {
        factory.register(new ProviderEventListener() {

            @Override
            public void onEvent(ProviderEvent event) {
                if (event instanceof PostMigrationEvent) {
                if (!useCaches) {
                    keyGenerator = new InfinispanKeyGenerator() {
                        @Override
                        protected <K> K generateKey(KeycloakSession session, Cache<K, ?> cache, KeyGenerator<K> keyGenerator) {
                            return keyGenerator.getKey();
                        }
                    };
                } else {
                    KeycloakModelUtils.runJobInTransaction(factory, (KeycloakSession session) -> {

                        keyGenerator = new InfinispanKeyGenerator();
                        if (!MultiSiteUtils.isPersistentSessionsEnabled()) {
                            initializePersisterLastSessionRefreshStore(factory);
                        }
                        registerClusterListeners(session);
                    });
                }

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
                }
            }
        });
        if (MultiSiteUtils.isPersistentSessionsEnabled() && useBatches) {
            persistentSessionsWorker = new PersistentSessionsWorker(factory,
                    asyncQueuePersistentUpdate,
                    maxBatchSize);
            persistentSessionsWorker.start();
        }
    }

    public void initializePersisterLastSessionRefreshStore(final KeycloakSessionFactory sessionFactory) {
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

    @Override
    public void close() {
        if (persistentSessionsWorker != null) {
            persistentSessionsWorker.stop();
        }
    }

    @Override
    public String getId() {
        return InfinispanUtils.EMBEDDED_PROVIDER_ID;
    }

    @Override
    public int order() {
        return InfinispanUtils.PROVIDER_ORDER;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return InfinispanUtils.isEmbeddedInfinispan() || MultiSiteUtils.isPersistentSessionsEnabled();
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        Map<String, String> info = new HashMap<>();
        info.put(CONFIG_OFFLINE_SESSION_CACHE_ENTRY_LIFESPAN_OVERRIDE, Long.toString(offlineSessionCacheEntryLifespanOverride));
        info.put(CONFIG_OFFLINE_CLIENT_SESSION_CACHE_ENTRY_LIFESPAN_OVERRIDE, Long.toString(offlineClientSessionCacheEntryLifespanOverride));
        info.put(CONFIG_MAX_BATCH_SIZE, Integer.toString(maxBatchSize));
        info.put(CONFIG_USE_CACHES, Boolean.toString(useCaches));
        info.put(CONFIG_USE_BATCHES, Boolean.toString(useBatches));
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

        builder.property()
                .name(CONFIG_USE_CACHES)
                .type("boolean")
                .helpText("Enable or disable caches. Enabled by default unless the external feature to use only external remote caches is used")
                .add();

        return builder.build();
    }

}

