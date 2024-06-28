package org.keycloak.models.sessions.infinispan.changes.remote;

import java.util.Objects;
import java.util.UUID;

import org.infinispan.commons.util.concurrent.CompletionStages;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.client.AuthenticatedClientSessionUpdater;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.user.UserSessionUpdater;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

/**
 * A {@link KeycloakTransaction} implementation that wraps all the user and client session transactions.
 * <p>
 * This implementation commits all modifications asynchronously and concurrently in both user and client sessions
 * transactions. Waits for all them to complete. This is an optimization to reduce the response time.
 */
public class UserSessionTransaction extends AbstractKeycloakTransaction {

    private final RemoteChangeLogTransaction<String, UserSessionEntity, UserSessionUpdater> userSessions;
    private final RemoteChangeLogTransaction<String, UserSessionEntity, UserSessionUpdater> offlineUserSessions;
    private final RemoteChangeLogTransaction<UUID, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> clientSessions;
    private final RemoteChangeLogTransaction<UUID, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> offlineClientSessions;

    public UserSessionTransaction(RemoteChangeLogTransaction<String, UserSessionEntity, UserSessionUpdater> userSessions, RemoteChangeLogTransaction<String, UserSessionEntity, UserSessionUpdater> offlineUserSessions, RemoteChangeLogTransaction<UUID, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> clientSessions, RemoteChangeLogTransaction<UUID, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> offlineClientSessions) {
        this.userSessions = Objects.requireNonNull(userSessions);
        this.offlineUserSessions = Objects.requireNonNull(offlineUserSessions);
        this.clientSessions = Objects.requireNonNull(clientSessions);
        this.offlineClientSessions = Objects.requireNonNull(offlineClientSessions);
    }

    @Override
    public void begin() {
        super.begin();
        userSessions.begin();
        offlineUserSessions.begin();
        clientSessions.begin();
        offlineClientSessions.begin();
    }

    @Override
    protected void commitImpl() {
        var stage = CompletionStages.aggregateCompletionStage();
        userSessions.commitAsync(stage);
        offlineUserSessions.commitAsync(stage);
        clientSessions.commitAsync(stage);
        offlineClientSessions.commitAsync(stage);
        CompletionStages.join(stage.freeze());
    }

    @Override
    protected void rollbackImpl() {
        userSessions.rollback();
        offlineUserSessions.rollback();
        clientSessions.rollback();
        offlineClientSessions.rollback();
    }

    public RemoteChangeLogTransaction<UUID, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> getClientSessions() {
        return clientSessions;
    }

    public RemoteChangeLogTransaction<UUID, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> getOfflineClientSessions() {
        return offlineClientSessions;
    }

    public RemoteChangeLogTransaction<String, UserSessionEntity, UserSessionUpdater> getOfflineUserSessions() {
        return offlineUserSessions;
    }

    public RemoteChangeLogTransaction<String, UserSessionEntity, UserSessionUpdater> getUserSessions() {
        return userSessions;
    }
}
