package org.keycloak.models.sessions.infinispan.changes.remote;

import java.util.Objects;

import org.infinispan.commons.util.concurrent.CompletionStages;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.client.AuthenticatedClientSessionUpdater;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.user.UserSessionUpdater;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionKey;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

/**
 * A {@link KeycloakTransaction} implementation that wraps all the user and client session transactions.
 * <p>
 * This implementation commits all modifications asynchronously and concurrently in both user and client sessions
 * transactions. Waits for all them to complete. This is an optimization to reduce the response time.
 */
public class UserSessionTransaction extends AbstractKeycloakTransaction {

    private final RemoteChangeLogTransaction<SessionKey, UserSessionEntity, UserSessionUpdater> userSessions;
    private final RemoteChangeLogTransaction<SessionKey, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> clientSessions;

    public UserSessionTransaction(RemoteChangeLogTransaction<SessionKey, UserSessionEntity, UserSessionUpdater> userSessions,
                                  RemoteChangeLogTransaction<SessionKey, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> clientSessions) {
        this.userSessions = Objects.requireNonNull(userSessions);
        this.clientSessions = Objects.requireNonNull(clientSessions);
    }

    @Override
    public void begin() {
        super.begin();
        userSessions.begin();
        clientSessions.begin();
    }

    @Override
    protected void commitImpl() {
        var stage = CompletionStages.aggregateCompletionStage();
        userSessions.commitAsync(stage);
        clientSessions.commitAsync(stage);
        CompletionStages.join(stage.freeze());
    }

    @Override
    protected void rollbackImpl() {
        userSessions.rollback();
        clientSessions.rollback();
    }

    public RemoteChangeLogTransaction<SessionKey, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> getClientSessions() {
        return clientSessions;
    }

    public RemoteChangeLogTransaction<SessionKey, UserSessionEntity, UserSessionUpdater> getUserSessions() {
        return userSessions;
    }
}
