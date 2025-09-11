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
import java.util.Set;
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
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.changes.ClientSessionPersistentChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.InfinispanChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.PersistentSessionsWorker;
import org.keycloak.models.sessions.infinispan.changes.PersistentUpdate;
import org.keycloak.models.sessions.infinispan.changes.SerializeExecutionsByKey;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.UserSessionPersistentChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.sessions.PersisterLastSessionRefreshStore;
import org.keycloak.models.sessions.infinispan.changes.sessions.PersisterLastSessionRefreshStoreFactory;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.events.AbstractUserSessionClusterListener;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RemoveUserSessionsEvent;
import org.keycloak.models.sessions.infinispan.transaction.InfinispanTransactionProvider;
import org.keycloak.models.sessions.infinispan.util.InfinispanKeyGenerator;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;

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
    private static final boolean DEFAULT_USE_BATCHES = false;

    private long offlineSessionCacheEntryLifespanOverride;

    private long offlineClientSessionCacheEntryLifespanOverride;

    private PersisterLastSessionRefreshStore persisterLastSessionRefreshStore;
    private InfinispanKeyGenerator keyGenerator;
    final SerializeExecutionsByKey<String> serializerSession = new SerializeExecutionsByKey<>();
    final SerializeExecutionsByKey<String> serializerOfflineSession = new SerializeExecutionsByKey<>();
    final SerializeExecutionsByKey<UUID> serializerClientSession = new SerializeExecutionsByKey<>();
    final SerializeExecutionsByKey<UUID> serializerOfflineClientSession = new SerializeExecutionsByKey<>();
    ArrayBlockingQueue<PersistentUpdate> asyncQueuePersistentUpdate;
    private PersistentSessionsWorker persistentSessionsWorker;
    private int maxBatchSize;
    private boolean useCaches;
    private boolean useBatches;

    @Override
    public UserSessionProvider create(KeycloakSession session) {
        if (MultiSiteUtils.isPersistentSessionsEnabled()) {
            var tx = createPersistentTransaction(session);
            return new PersistentUserSessionProvider(
                    session,
                    keyGenerator,
                    tx.userTx,
                    tx.clientTx
            );
        }
        var tx = createVolatileTransaction(session);
        return new InfinispanUserSessionProvider(
                session,
                persisterLastSessionRefreshStore,
                keyGenerator,
                tx.sessionTx,
                tx.offlineSessionTx,
                tx.clientSessionTx,
                tx.offlineClientSessionTx,
                this::deriveOfflineSessionCacheEntryLifespanMs,
                this::deriveOfflineClientSessionCacheEntryLifespanOverrideMs
        );
    }

    @Override
    public void init(Config.Scope config) {
        offlineSessionCacheEntryLifespanOverride = config.getInt(CONFIG_OFFLINE_SESSION_CACHE_ENTRY_LIFESPAN_OVERRIDE, -1);
        if (offlineSessionCacheEntryLifespanOverride != -1) {
            // to be removed in KC 27
            log.warn("The option spi-user-sessions--infinispan--offline-session-cache-entry-lifespan-override is deprecated and will be removed in a future release");
        }
        offlineClientSessionCacheEntryLifespanOverride = config.getInt(CONFIG_OFFLINE_CLIENT_SESSION_CACHE_ENTRY_LIFESPAN_OVERRIDE, -1);
        if (offlineClientSessionCacheEntryLifespanOverride != -1) {
            // to be removed in KC 27
            log.warn("The option spi-user-sessions--infinispan--offline-client-session-cache-entry-lifespan-override is deprecated and will be removed in a future release");
        }
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
        factory.register(event -> {
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

            } else if (event instanceof UserModel.UserRemovedEvent userRemovedEvent) {

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
        });
        if (MultiSiteUtils.isPersistentSessionsEnabled() && useBatches) {
            persistentSessionsWorker = new PersistentSessionsWorker(factory,
                    asyncQueuePersistentUpdate,
                    maxBatchSize);
            persistentSessionsWorker.start();
        }
    }

    public void initializePersisterLastSessionRefreshStore(final KeycloakSessionFactory sessionFactory) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> {
            // Initialize persister for periodically doing bulk DB updates of lastSessionRefresh timestamps of refreshed sessions
            persisterLastSessionRefreshStore = new PersisterLastSessionRefreshStoreFactory().createAndInit(session, true);
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
              .name(CONFIG_USE_BATCHES)
              .type("boolean")
              .helpText("Enable or disable batch writes to the database. Enabled by default with the persistent-user-sessions Feature")
              .defaultValue(DEFAULT_USE_BATCHES)
              .add();

        builder.property()
                .name(CONFIG_MAX_BATCH_SIZE)
                .type("int")
                .helpText("Maximum size of a batch (only applicable to persistent sessions")
                .defaultValue(DEFAULT_MAX_BATCH_SIZE)
                .add();

        builder.property()
                .name(CONFIG_OFFLINE_CLIENT_SESSION_CACHE_ENTRY_LIFESPAN_OVERRIDE)
                .type("int")
                .helpText("Override how long offline client sessions should be kept in memory in seconds (deprecated, to be removed in Keycloak 27)")
                .add();

        builder.property()
                .name(CONFIG_OFFLINE_SESSION_CACHE_ENTRY_LIFESPAN_OVERRIDE)
                .type("int")
                .helpText("Override how long offline user sessions should be kept in memory in seconds (deprecated, to be removed in Keycloak 27)")
                .add();

        builder.property()
                .name(CONFIG_USE_CACHES)
                .type("boolean")
                .helpText("Enable or disable caches. Enabled by default unless the external feature to use only external remote caches is used")
                .add();

        return builder.build();
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(InfinispanConnectionProvider.class, InfinispanTransactionProvider.class);
    }

    private VolatileTransactions createVolatileTransaction(KeycloakSession session) {
        var connections = session.getProvider(InfinispanConnectionProvider.class);
        var blockingManager = connections.getBlockingManager();
        Cache<String, SessionEntityWrapper<UserSessionEntity>> sessionCache = connections.getCache(USER_SESSION_CACHE_NAME);
        Cache<String, SessionEntityWrapper<UserSessionEntity>> offlineSessionCache = connections.getCache(OFFLINE_USER_SESSION_CACHE_NAME);
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionCache = connections.getCache(CLIENT_SESSION_CACHE_NAME);
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> offlineClientSessionCache = connections.getCache(OFFLINE_CLIENT_SESSION_CACHE_NAME);

        var sessionTx = new InfinispanChangelogBasedTransaction<>(session, sessionCache, SessionTimeouts::getUserSessionLifespanMs, SessionTimeouts::getUserSessionMaxIdleMs, serializerSession, blockingManager);
        var offlineSessionTx = new InfinispanChangelogBasedTransaction<>(session, offlineSessionCache, this::deriveOfflineSessionCacheEntryLifespanMs, SessionTimeouts::getOfflineSessionMaxIdleMs, serializerOfflineSession, blockingManager);
        var clientSessionTx = new InfinispanChangelogBasedTransaction<>(session, clientSessionCache, SessionTimeouts::getClientSessionLifespanMs, SessionTimeouts::getClientSessionMaxIdleMs, serializerClientSession, blockingManager);
        var offlineClientSessionTx = new InfinispanChangelogBasedTransaction<>(session, offlineClientSessionCache, this::deriveOfflineClientSessionCacheEntryLifespanOverrideMs, SessionTimeouts::getOfflineClientSessionMaxIdleMs, serializerOfflineClientSession, blockingManager);

        var transactionProvider = session.getProvider(InfinispanTransactionProvider.class);
        transactionProvider.registerTransaction(sessionTx);
        transactionProvider.registerTransaction(offlineSessionTx);
        transactionProvider.registerTransaction(clientSessionTx);
        transactionProvider.registerTransaction(offlineClientSessionTx);
        return new VolatileTransactions(sessionTx, offlineSessionTx, clientSessionTx, offlineClientSessionTx);
    }

    private PersistentTransaction createPersistentTransaction(KeycloakSession session) {
        var connections = session.getProvider(InfinispanConnectionProvider.class);
        var blockingManager = connections.getBlockingManager();

        Cache<String, SessionEntityWrapper<UserSessionEntity>> sessionCache = null;
        Cache<String, SessionEntityWrapper<UserSessionEntity>> offlineSessionCache = null;
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessionCache = null;
        Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> offlineClientSessionCache = null;
        if (useCaches) {
            sessionCache = connections.getCache(USER_SESSION_CACHE_NAME);
            offlineSessionCache = connections.getCache(OFFLINE_USER_SESSION_CACHE_NAME);
            clientSessionCache = connections.getCache(CLIENT_SESSION_CACHE_NAME);
            offlineClientSessionCache = connections.getCache(OFFLINE_CLIENT_SESSION_CACHE_NAME);
        }

        var sessionTx = new UserSessionPersistentChangelogBasedTransaction(session,
                sessionCache, offlineSessionCache,
                SessionTimeouts::getUserSessionLifespanMs, SessionTimeouts::getUserSessionMaxIdleMs,
                SessionTimeouts::getOfflineSessionLifespanMs, SessionTimeouts::getOfflineSessionMaxIdleMs,
                asyncQueuePersistentUpdate,
                serializerSession,
                serializerOfflineSession,
                blockingManager);

        var clientSessionTx = new ClientSessionPersistentChangelogBasedTransaction(session,
                clientSessionCache, offlineClientSessionCache,
                SessionTimeouts::getClientSessionLifespanMs, SessionTimeouts::getClientSessionMaxIdleMs,
                SessionTimeouts::getOfflineClientSessionLifespanMs, SessionTimeouts::getOfflineClientSessionMaxIdleMs,
                sessionTx,
                asyncQueuePersistentUpdate,
                serializerClientSession,
                serializerOfflineClientSession,
                blockingManager);

        var transactionProvider = session.getProvider(InfinispanTransactionProvider.class);
        // Order matters!
        // We have to update the client sessions before the user sessions.
        // The user session update may modify the client session table and, when the client session updates is executed, it will throw a StaleObjectStateException.
        transactionProvider.registerTransaction(clientSessionTx);
        transactionProvider.registerTransaction(sessionTx);
        return new PersistentTransaction(sessionTx, clientSessionTx);
    }

    private record VolatileTransactions(InfinispanChangelogBasedTransaction<String, UserSessionEntity> sessionTx,
                                        InfinispanChangelogBasedTransaction<String, UserSessionEntity> offlineSessionTx,
                                        InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> clientSessionTx,
                                        InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> offlineClientSessionTx) {}

    private record PersistentTransaction(UserSessionPersistentChangelogBasedTransaction userTx, ClientSessionPersistentChangelogBasedTransaction clientTx) {}

}

