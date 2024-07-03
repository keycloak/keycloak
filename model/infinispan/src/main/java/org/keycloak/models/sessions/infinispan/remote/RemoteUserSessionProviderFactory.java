package org.keycloak.models.sessions.infinispan.remote;

import java.util.List;

import org.infinispan.client.hotrod.RemoteCache;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.changes.remote.RemoteChangeLogTransaction;
import org.keycloak.models.sessions.infinispan.changes.remote.UserSessionTransaction;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.client.AuthenticatedClientSessionUpdater;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.user.UserSessionUpdater;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionKey;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
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
        batchSize = config.getInt(CONFIG_MAX_BATCH_SIZE, DEFAULT_BATCH_SIZE);
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
        return InfinispanUtils.isRemoteInfinispan() && !Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS);
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
        RemoteCache<SessionKey, UserSessionEntity> userSessionCache = connections.getRemoteCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
        RemoteCache<SessionKey, AuthenticatedClientSessionEntity> clientSessionCache = connections.getRemoteCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
        remoteCacheHolder = new RemoteCacheHolder(userSessionCache, clientSessionCache);
    }

    private UserSessionTransaction createTransaction(KeycloakSession session) {
        lazyInit(session);
        return new UserSessionTransaction(createUserSessionTransaction(), createClientSessionTransaction());
    }

    private RemoteChangeLogTransaction<SessionKey, UserSessionEntity, UserSessionUpdater> createUserSessionTransaction() {
        return new RemoteChangeLogTransaction<>(UserSessionUpdater.FACTORY, remoteCacheHolder.userSession());
    }

    private RemoteChangeLogTransaction<SessionKey, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> createClientSessionTransaction() {
        return new RemoteChangeLogTransaction<>(AuthenticatedClientSessionUpdater.FACTORY, remoteCacheHolder.clientSession);
    }

    private record RemoteCacheHolder(
            RemoteCache<SessionKey, UserSessionEntity> userSession,
            RemoteCache<SessionKey, AuthenticatedClientSessionEntity> clientSession) {
    }
}
