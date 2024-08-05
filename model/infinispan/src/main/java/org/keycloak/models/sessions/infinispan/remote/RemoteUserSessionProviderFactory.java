package org.keycloak.models.sessions.infinispan.remote;

import java.util.List;
import java.util.concurrent.Executor;

import org.infinispan.client.hotrod.RemoteCache;
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
import org.keycloak.models.sessions.infinispan.remote.transaction.ClientSessionChangeLogTransaction;
import org.keycloak.models.sessions.infinispan.remote.transaction.UseSessionChangeLogTransaction;
import org.keycloak.models.sessions.infinispan.remote.transaction.UserSessionTransaction;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

public class RemoteUserSessionProviderFactory implements UserSessionProviderFactory<RemoteUserSessionProvider>, EnvironmentDependentProviderFactory, ProviderEventListener {

    // Sessions are close to 1KB of data. Fetch 1MB per batch request (can be configured)
    private static final int DEFAULT_BATCH_SIZE = 1024;
    private static final String CONFIG_MAX_BATCH_SIZE = "batchSize";

    private volatile RemoteCacheHolder remoteCacheHolder;
    private volatile int batchSize = DEFAULT_BATCH_SIZE;

    @Override
    public RemoteUserSessionProvider create(KeycloakSession session) {
        var tx = createTransaction(session);
        session.getTransactionManager().enlistAfterCompletion(tx);
        return new RemoteUserSessionProvider(session, tx, batchSize);
    }

    @Override
    public void init(Config.Scope config) {
        batchSize = Math.max(1, config.getInt(CONFIG_MAX_BATCH_SIZE, DEFAULT_BATCH_SIZE));
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
        remoteCacheHolder = null;
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
        return builder.build();
    }

    @Override
    public void onEvent(ProviderEvent event) {
        if (event instanceof UserModel.UserRemovedEvent ure) {
            onUserRemoved(ure);
        }
    }

    private void onUserRemoved(UserModel.UserRemovedEvent event) {
        event.getKeycloakSession().getProvider(UserSessionProvider.class, getId()).removeUserSessions(event.getRealm(), event.getUser());
        event.getKeycloakSession().getProvider(UserSessionPersisterProvider.class).onUserRemoved(event.getRealm(), event.getUser());
    }

    private void lazyInit(KeycloakSession session) {
        if (remoteCacheHolder != null) {
            return;
        }
        InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
        RemoteCache<String, RemoteUserSessionEntity> userSessionCache = connections.getRemoteCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
        RemoteCache<String, RemoteUserSessionEntity> offlineUserSessionsCache = connections.getRemoteCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
        RemoteCache<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> clientSessionCache = connections.getRemoteCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
        RemoteCache<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> offlineClientSessionsCache = connections.getRemoteCache(InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME);
        var executor = connections.getExecutor("query-delete");
        remoteCacheHolder = new RemoteCacheHolder(userSessionCache, offlineUserSessionsCache, clientSessionCache, offlineClientSessionsCache, executor);
    }

    private UserSessionTransaction createTransaction(KeycloakSession session) {
        lazyInit(session);
        return new UserSessionTransaction(
                createUserSessionTransaction(false),
                createUserSessionTransaction(true),
                createClientSessionTransaction(false),
                createClientSessionTransaction(true)
        );
    }

    private UseSessionChangeLogTransaction createUserSessionTransaction(boolean offline) {
        return new UseSessionChangeLogTransaction(UserSessionUpdater.factory(offline), remoteCacheHolder.userSessionCache(offline));
    }

    private ClientSessionChangeLogTransaction createClientSessionTransaction(boolean offline) {
        return new ClientSessionChangeLogTransaction(AuthenticatedClientSessionUpdater.factory(offline), remoteCacheHolder.clientSessionCache(offline));
    }

    private record RemoteCacheHolder(
            RemoteCache<String, RemoteUserSessionEntity> userSession,
            RemoteCache<String, RemoteUserSessionEntity> offlineUserSession,
            RemoteCache<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> clientSession,
            RemoteCache<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> offlineClientSession,
            Executor executor) {

        RemoteCache<String, RemoteUserSessionEntity> userSessionCache(boolean offline) {
            return offline ? offlineUserSession : userSession;
        }

        RemoteCache<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> clientSessionCache(boolean offline) {
            return offline ? offlineClientSession : clientSession;
        }
    }
}
