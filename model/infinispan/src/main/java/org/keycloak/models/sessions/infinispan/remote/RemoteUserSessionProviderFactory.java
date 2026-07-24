package org.keycloak.models.sessions.infinispan.remote;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.client.AuthenticatedClientSessionUpdater;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.user.UserSessionUpdater;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionKey;
import org.keycloak.models.sessions.infinispan.entities.RemoteAuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.RemoteUserSessionEntity;
import org.keycloak.models.sessions.infinispan.listeners.RemoteUserSessionExpirationListener;
import org.keycloak.models.sessions.infinispan.remote.transaction.ClientSessionChangeLogTransaction;
import org.keycloak.models.sessions.infinispan.remote.transaction.RemoteChangeLogTransaction;
import org.keycloak.models.sessions.infinispan.remote.transaction.UserSessionChangeLogTransaction;
import org.keycloak.models.sessions.infinispan.remote.transaction.UserSessionTransaction;
import org.keycloak.models.sessions.infinispan.transaction.InfinispanTransactionProvider;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.util.concurrent.BlockingManager;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;

public class RemoteUserSessionProviderFactory implements UserSessionProviderFactory<RemoteUserSessionProvider>, EnvironmentDependentProviderFactory, ProviderEventListener, ServerInfoAwareProviderFactory {

    // Sessions are close to 1KB of data. Fetch 1MB per batch request (can be configured)
    private static final int DEFAULT_BATCH_SIZE = 1024;
    private static final String CONFIG_MAX_BATCH_SIZE = "batchSize";

    private volatile SharedStateImpl<String, RemoteUserSessionEntity> userSessionState;
    private volatile SharedStateImpl<String, RemoteUserSessionEntity> offlineUserSessionState;
    private volatile SharedStateImpl<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> clientSessionState;
    private volatile SharedStateImpl<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> offlineClientSessionState;
    private volatile BlockingManager blockingManager;
    private volatile int batchSize = DEFAULT_BATCH_SIZE;
    private volatile int maxRetries = InfinispanUtils.DEFAULT_MAX_RETRIES;
    private volatile int backOffBaseTimeMillis = InfinispanUtils.DEFAULT_RETRIES_BASE_TIME_MILLIS;
    private volatile RemoteUserSessionExpirationListener expirationListener;

    @Override
    public RemoteUserSessionProvider create(KeycloakSession session) {
        var provider = session.getProvider(InfinispanTransactionProvider.class);
        var tx = createTransaction(session);
        provider.registerTransaction(tx);
        return new RemoteUserSessionProvider(session, tx, batchSize);
    }

    @Override
    public void init(Config.Scope config) {
        batchSize = Math.max(1, config.getInt(CONFIG_MAX_BATCH_SIZE, DEFAULT_BATCH_SIZE));
        maxRetries = InfinispanUtils.getMaxRetries(config);
        backOffBaseTimeMillis = InfinispanUtils.getRetryBaseTimeMillis(config);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        try (var session = factory.create()) {
            lazyInit(session);
        }
        factory.register(this);
    }

    @Override
    public void close() {
        if (expirationListener != null) {
            userSessionState.cache().removeClientListener(expirationListener);
            offlineUserSessionState.cache().removeClientListener(expirationListener);
        }
        expirationListener = null;
        blockingManager = null;
        userSessionState = null;
        offlineUserSessionState = null;
        clientSessionState = null;
        offlineClientSessionState = null;
    }

    @Override
    public String getId() {
        return InfinispanUtils.REMOTE_PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return InfinispanUtils.isRemoteInfinispan() && !MultiSiteUtils.isPersistentSessionsEnabled();
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();
        builder.property()
                .name(CONFIG_MAX_BATCH_SIZE)
                .type("int")
                .helpText("Batch size when streaming session from the remote cache")
                .defaultValue(DEFAULT_BATCH_SIZE)
                .add();

        InfinispanUtils.configureMaxRetries(builder);
        InfinispanUtils.configureRetryBaseTime(builder);

        return builder.build();
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        Map<String, String> map = new HashMap<>();
        map.put(CONFIG_MAX_BATCH_SIZE, Integer.toString(batchSize));
        InfinispanUtils.maxRetriesToOperationalInfo(map, maxRetries);
        InfinispanUtils.retryBaseTimeMillisToOperationalInfo(map, backOffBaseTimeMillis);
        return map;
    }

    @Override
    public void onEvent(ProviderEvent event) {
        if (event instanceof UserModel.UserRemovedEvent ure) {
            onUserRemoved(ure);
        }
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(InfinispanTransactionProvider.class, InfinispanConnectionProvider.class);
    }

    private void onUserRemoved(UserModel.UserRemovedEvent event) {
        event.getKeycloakSession().getProvider(UserSessionProvider.class, getId()).removeUserSessions(event.getRealm(), event.getUser());
        event.getKeycloakSession().getProvider(UserSessionPersisterProvider.class).onUserRemoved(event.getRealm(), event.getUser());
    }

    private void lazyInit(KeycloakSession session) {
        if (blockingManager != null) {
            return;
        }
        var connections = session.getProvider(InfinispanConnectionProvider.class);
        userSessionState = new SharedStateImpl<>(connections.getRemoteCache(USER_SESSION_CACHE_NAME));
        offlineUserSessionState = new SharedStateImpl<>(connections.getRemoteCache(OFFLINE_USER_SESSION_CACHE_NAME));
        clientSessionState = new SharedStateImpl<>(connections.getRemoteCache(CLIENT_SESSION_CACHE_NAME));
        offlineClientSessionState = new SharedStateImpl<>(connections.getRemoteCache(OFFLINE_CLIENT_SESSION_CACHE_NAME));
        blockingManager = connections.getBlockingManager();
        expirationListener = new RemoteUserSessionExpirationListener(session.getKeycloakSessionFactory(), blockingManager, userSessionState.cache().getRemoteCacheContainer().getMarshaller());
        userSessionState.cache().addClientListener(expirationListener);
        offlineUserSessionState.cache().addClientListener(expirationListener);
    }

    private UserSessionTransaction createTransaction(KeycloakSession session) {
        lazyInit(session);
        return new UserSessionTransaction(
                new UserSessionChangeLogTransaction(UserSessionUpdater.onlineFactory(), userSessionState),
                new UserSessionChangeLogTransaction(UserSessionUpdater.offlineFactory(), offlineUserSessionState),
                new ClientSessionChangeLogTransaction(AuthenticatedClientSessionUpdater.onlineFactory(), clientSessionState),
                new ClientSessionChangeLogTransaction(AuthenticatedClientSessionUpdater.offlineFactory(), offlineClientSessionState)
        );
    }

    private class SharedStateImpl<K, V> implements RemoteChangeLogTransaction.SharedState<K, V> {

        private final RemoteCache<K, V> cache;

        private SharedStateImpl(RemoteCache<K, V> cache) {
            this.cache = cache;
        }

        @Override
        public RemoteCache<K, V> cache() {
            return cache;
        }

        @Override
        public int maxRetries() {
            return maxRetries;
        }

        @Override
        public int backOffBaseTimeMillis() {
            return backOffBaseTimeMillis;
        }

        @Override
        public BlockingManager blockingManager() {
            return blockingManager;
        }
    }
}
