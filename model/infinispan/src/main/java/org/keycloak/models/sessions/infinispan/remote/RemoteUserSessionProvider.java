/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.remote;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.reactivex.rxjava3.core.Flowable;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.changes.remote.RemoteChangeLogTransaction;
import org.keycloak.models.sessions.infinispan.changes.remote.RemoveEntryPredicate;
import org.keycloak.models.sessions.infinispan.changes.remote.UserSessionTransaction;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.BaseUpdater;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.client.AuthenticatedClientSessionUpdater;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.user.ClientSessionMappingAdapter;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.user.ClientSessionProvider;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.user.UserSessionUpdater;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionStore;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionKey;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.utils.StreamsUtil;

import static org.keycloak.models.Constants.SESSION_NOTE_LIGHTWEIGHT_USER;

/**
 * An {@link UserSessionProvider} implementation that uses only {@link RemoteCache} as storage.
 */
public class RemoteUserSessionProvider implements UserSessionProvider {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final KeycloakSession session;
    private final UserSessionTransaction transaction;
    private final int batchSize;

    public RemoteUserSessionProvider(KeycloakSession session, UserSessionTransaction transaction, int batchSize) {
        this.session = session;
        this.transaction = transaction;
        this.batchSize = batchSize;
    }

    @Override
    public AuthenticatedClientSessionModel createClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession) {
        var clientTx = transaction.getClientSessions();
        var key = SessionKey.randomOnlineSessionKey();
        var entity = AuthenticatedClientSessionEntity.create(UUID.fromString(key.uuid()), realm, client, userSession);
        var model = clientTx.create(key, entity);
        if (!model.isInitialized()) {
            model.initialize(userSession, client, clientTx);
        }
        userSession.getAuthenticatedClientSessions().put(client.getId(), model);
        return model;
    }

    @Override
    public AuthenticatedClientSessionModel getClientSession(UserSessionModel userSession, ClientModel client, String clientSessionId, boolean offline) {
        if (clientSessionId == null) {
            return null;
        }
        var clientTx = transaction.getClientSessions();
        var updater = clientTx.get(new SessionKey(clientSessionId, offline));
        if (updater == null) {
            return null;
        }
        if (!updater.isInitialized()) {
            updater.initialize(userSession, client, clientTx);
        }
        return updater;
    }

    @Override
    public UserSessionModel createUserSession(String id, RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId, UserSessionModel.SessionPersistenceState persistenceState) {
        var key = id == null ?
                SessionKey.randomOnlineSessionKey() :
                new SessionKey(id, false);

        var entity = UserSessionEntity.create(key.uuid(), realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId);
        var updater = transaction.getUserSessions().create(key, entity);
        return initUserSessionUpdater(updater, persistenceState, realm, user);
    }

    @Override
    public UserSessionModel getUserSession(RealmModel realm, String id) {
        return getUserSession(realm, id, false);
    }

    @Override
    public Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, UserModel user) {
        return StreamsUtil.closing(streamUserSessions(new UserAndRealmPredicate(realm.getId(), user.getId(), false), realm, user));
    }

    @Override
    public Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, ClientModel client) {
        return StreamsUtil.closing(streamUserSessions(new ClientAndRealmPredicate(realm.getId(), client.getId(), false), realm, null));
    }

    @Override
    public Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, ClientModel client, Integer firstResult, Integer maxResults) {
        return StreamsUtil.paginatedStream(getUserSessionsStream(realm, client).sorted(Comparator.comparing(UserSessionModel::getLastSessionRefresh)), firstResult, maxResults);
    }

    @Override
    public Stream<UserSessionModel> getUserSessionByBrokerUserIdStream(RealmModel realm, String brokerUserId) {
        return StreamsUtil.closing(streamUserSessions(new BrokerUserIdAndRealmPredicate(realm.getId(), brokerUserId, false), realm, null));
    }

    @Override
    public UserSessionModel getUserSessionByBrokerSessionId(RealmModel realm, String brokerSessionId) {
        return StreamsUtil.closing(streamUserSessions(new BrokerSessionIdAndRealmPredicate(realm.getId(), brokerSessionId, false), realm, null))
                .findFirst()
                .orElse(null);
    }

    @Override
    public UserSessionModel getUserSessionWithPredicate(RealmModel realm, String id, boolean offline, Predicate<UserSessionModel> predicate) {
        var updater = getUserSession(realm, id, offline);
        return updater != null && predicate.test(updater) ? updater : null;
    }

    @Override
    public long getActiveUserSessions(RealmModel realm, ClientModel client) {
        return StreamsUtil.closing(getUserSessionsStream(realm, client)).count();
    }

    @Override
    public Map<String, Long> getActiveClientSessionStats(RealmModel realm, boolean offline) {
        var userSessions = transaction.getUserSessions();
        return Flowable.fromPublisher(userSessions.getCache().publishEntriesWithMetadata(null, batchSize))
                .filter(new RealmPredicate(realm.getId(), offline))
                .map(Map.Entry::getValue)
                .map(MetadataValue::getValue)
                .map(UserSessionEntity::getAuthenticatedClientSessions)
                .map(AuthenticatedClientSessionStore::keySet)
                .map(Collection::stream)
                .flatMap(Flowable::fromStream)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .blockingGet();
    }

    @Override
    public void removeUserSession(RealmModel realm, UserSessionModel userSession) {
        internalRemoveUserSession(userSession, new SessionKey(userSession.getId(), false));
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        getUserSessionsStream(realm, user).forEach(s -> removeUserSession(realm, s));
    }

    @Override
    public void removeAllExpired() {
        //rely on Infinispan expiration
    }

    @Override
    public void removeExpired(RealmModel realm) {
        //rely on Infinispan expiration
    }

    @SuppressWarnings("unchecked")
    @Override
    public void removeUserSessions(RealmModel realm) {
        RemoveEntryPredicate<SessionKey, ? extends SessionEntity> predicate = (key, value) -> !key.offline() && Objects.equals(value.getRealmId(), realm.getId());
        transaction.getUserSessions().removeIf((RemoveEntryPredicate<SessionKey, UserSessionEntity>) predicate);
        transaction.getClientSessions().removeIf((RemoveEntryPredicate<SessionKey, AuthenticatedClientSessionEntity>) predicate);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onRealmRemoved(RealmModel realm) {
        RemoveEntryPredicate<SessionKey, ? extends SessionEntity> predicate = (ignored, value) -> Objects.equals(value.getRealmId(), realm.getId());
        transaction.getUserSessions().removeIf((RemoveEntryPredicate<SessionKey, UserSessionEntity>) predicate);
        transaction.getClientSessions().removeIf((RemoveEntryPredicate<SessionKey, AuthenticatedClientSessionEntity>) predicate);
        var database = session.getProvider(UserSessionPersisterProvider.class);
        if (database != null) {
            database.onRealmRemoved(realm);
        }
    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {
        var database = session.getProvider(UserSessionPersisterProvider.class);
        if (database != null) {
            database.onClientRemoved(realm, client);
        }
    }

    @Override
    public UserSessionModel createOfflineUserSession(UserSessionModel userSession) {
        var entity = UserSessionEntity.createFromModel(userSession);

        int currentTime = Time.currentTime();
        entity.setStarted(currentTime);
        entity.setLastSessionRefresh(currentTime);

        var updater = transaction.getUserSessions().create(new SessionKey(entity.getId(), true), entity);
        return initUserSessionUpdater(updater, userSession.getPersistenceState(), userSession.getRealm(), userSession.getUser());
    }

    @Override
    public UserSessionModel getOfflineUserSession(RealmModel realm, String userSessionId) {
        return getUserSession(realm, userSessionId, true);
    }

    @Override
    public void removeOfflineUserSession(RealmModel realm, UserSessionModel userSession) {
        internalRemoveUserSession(userSession, new SessionKey(userSession.getId(), true));
    }

    @Override
    public AuthenticatedClientSessionModel createOfflineClientSession(AuthenticatedClientSessionModel clientSession, UserSessionModel offlineUserSession) {
        var clientTx = transaction.getClientSessions();
        var entity = AuthenticatedClientSessionEntity.createFromModel(clientSession);
        var model = clientTx.create(new SessionKey(entity.getId().toString(), true), entity);
        if (!model.isInitialized()) {
            model.initialize(offlineUserSession, clientSession.getClient(), clientTx);
        }
        offlineUserSession.getAuthenticatedClientSessions().put(clientSession.getClient().getId(), model);
        return model;
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, UserModel user) {
        return StreamsUtil.closing(streamUserSessions(new UserAndRealmPredicate(realm.getId(), user.getId(), true), realm, user));
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionByBrokerUserIdStream(RealmModel realm, String brokerUserId) {
        return StreamsUtil.closing(streamUserSessions(new BrokerUserIdAndRealmPredicate(realm.getId(), brokerUserId, true), realm, null));
    }

    @Override
    public long getOfflineSessionsCount(RealmModel realm, ClientModel client) {
        return StreamsUtil.closing(streamUserSessions(new ClientAndRealmPredicate(realm.getId(), client.getId(), true), realm, null)).count();
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, ClientModel client, Integer firstResult, Integer maxResults) {
        return StreamsUtil.closing(StreamsUtil.paginatedStream(streamUserSessions(new ClientAndRealmPredicate(realm.getId(), client.getId(), true), realm, null), firstResult, maxResults));
    }

    @Override
    public int getStartupTime(RealmModel realm) {
        return session.getProvider(ClusterProvider.class).getClusterStartupTime();
    }

    @Override
    public KeycloakSession getKeycloakSession() {
        return session;
    }

    @Override
    public void close() {

    }

    @Override
    public void migrate(String modelVersion) {
        if ("25.0.0".equals(modelVersion)) {
            migrateUserSessions(true);
            migrateUserSessions(false);
        }

    }

    private void migrateUserSessions(boolean offline) {
        log.info("Migrate user sessions from database to the remote cache");

        List<String> userSessionIds = Collections.synchronizedList(new ArrayList<>(batchSize));
        List<Map.Entry<String, String>> clientSessionIds = Collections.synchronizedList(new ArrayList<>(batchSize));
        boolean hasSessions;
        do {
            hasSessions = migrateUserSessionBatch(session.getKeycloakSessionFactory(), offline, userSessionIds, clientSessionIds);
        } while (hasSessions);

        log.info("All sessions migrated.");
    }

    private boolean migrateUserSessionBatch(KeycloakSessionFactory factory, boolean offline, List<String> userSessionBuffer, List<Map.Entry<String, String>> clientSessionBuffer) {
        var userSessionCache = transaction.getUserSessions().getCache();
        var clientSessionCache = transaction.getClientSessions().getCache();

        log.infof("Migrating %s user(s) session(s) from database.", batchSize);

        return KeycloakModelUtils.runJobInTransactionWithResult(factory, kcSession -> {
            var database = kcSession.getProvider(UserSessionPersisterProvider.class);
            var stage = CompletionStages.aggregateCompletionStage();
            database.loadUserSessionsStream(-1, batchSize, offline, "")
                    .forEach(userSessionModel -> {
                        var userSessionEntity = UserSessionEntity.createFromModel(userSessionModel);
                        stage.dependsOn(userSessionCache.putIfAbsentAsync(new SessionKey(userSessionModel.getId(), offline), userSessionEntity));
                        userSessionBuffer.add(userSessionModel.getId());
                        for (var clientSessionModel : userSessionModel.getAuthenticatedClientSessions().values()) {
                            clientSessionBuffer.add(Map.entry(userSessionModel.getId(), clientSessionModel.getId()));
                            var clientSessionEntity = AuthenticatedClientSessionEntity.createFromModel(clientSessionModel);
                            stage.dependsOn(clientSessionCache.putIfAbsentAsync(new SessionKey(clientSessionEntity.getId().toString(), offline), clientSessionEntity));
                        }
                    });
            CompletionStages.join(stage.freeze());

            if (userSessionBuffer.isEmpty() && clientSessionBuffer.isEmpty()) {
                return false;
            }

            log.infof("%s user(s) session(s) stored in the remote cache. Removing them from database.", userSessionBuffer.size());

            userSessionBuffer.forEach(s -> database.removeUserSession(s, offline));
            userSessionBuffer.clear();

            clientSessionBuffer.forEach(e -> database.removeClientSession(e.getKey(), e.getValue(), offline));
            clientSessionBuffer.clear();

            return true;
        });
    }

    private UserSessionUpdater getUserSession(RealmModel realm, String id, boolean offline) {
        if (id == null) {
            return null;
        }
        var updater = transaction.getUserSessions().get(new SessionKey(id, offline));
        if (updater == null || !updater.getValue().getRealmId().equals(realm.getId())) {
            return null;
        }
        if (updater.isInitialized()) {
            return updater;
        }
        UserModel user = session.users().getUserById(realm, updater.getValue().getUser());
        return initUserSessionUpdater(updater, UserSessionModel.SessionPersistenceState.PERSISTENT, realm, user);
    }

    private void internalRemoveUserSession(UserSessionModel userSession, SessionKey userSessionKey) {
        var clientSessionTransaction = transaction.getClientSessions();
        var userSessionTransaction = transaction.getUserSessions();
        userSession.getAuthenticatedClientSessions().values()
                .stream()
                .filter(Objects::nonNull) // we need to filter, it may not be a UserSessionUpdater class.
                .map(AuthenticatedClientSessionModel::getId)
                .filter(Objects::nonNull) // we need to filter, it may not be a AuthenticatedClientSessionUpdater class.
                .map(userSessionKey::withId)
                .forEach(clientSessionTransaction::remove);
        userSessionTransaction.remove(userSessionKey);
    }

    private Stream<UserSessionModel> streamUserSessions(InternalUserSessionPredicate predicate, RealmModel realm, UserModel user) {
        var userSessions = transaction.getUserSessions();
        return Flowable.fromPublisher(userSessions.getCache().publishEntriesWithMetadata(null, batchSize))
                .filter(predicate)
                .map(userSessions::wrap)
                .mapOptional(s -> initFromStream(s, realm, user))
                .map(UserSessionModel.class::cast)
                .blockingStream(batchSize);
    }

    private Optional<UserSessionUpdater> initFromStream(UserSessionUpdater updater, RealmModel realm, UserModel user) {
        if (updater.isInitialized()) {
            return Optional.of(updater);
        }
        assert realm != null;
        if (user == null) {
            user = session.users().getUserById(realm, updater.getValue().getUser());
        }
        return Optional.ofNullable(initUserSessionUpdater(updater, UserSessionModel.SessionPersistenceState.PERSISTENT, realm, user));
    }

    private UserSessionUpdater initUserSessionUpdater(UserSessionUpdater updater, UserSessionModel.SessionPersistenceState persistenceState, RealmModel realm, UserModel user) {
        var provider = new RemoteClientSessionAdapterProvider(transaction.getClientSessions(), updater);
        if (user instanceof LightweightUserAdapter) {
            updater.initialize(persistenceState, realm, user, provider);
            return checkExpiration(updater);
        }
        // copied from org.keycloak.models.sessions.infinispan.InfinispanUserSessionProvider
        if (Profile.isFeatureEnabled(Profile.Feature.TRANSIENT_USERS) && updater.getNotes().containsKey(SESSION_NOTE_LIGHTWEIGHT_USER)) {
            LightweightUserAdapter lua = LightweightUserAdapter.fromString(session, realm, updater.getNotes().get(SESSION_NOTE_LIGHTWEIGHT_USER));
            updater.initialize(persistenceState, realm, lua, provider);
            lua.setUpdateHandler(lua1 -> {
                if (lua == lua1) {  // Ensure there is no conflicting user model, only the latest lightweight user can be used
                    updater.setNote(SESSION_NOTE_LIGHTWEIGHT_USER, lua1.serialize());
                }
            });
            return checkExpiration(updater);
        }

        if (user == null) {
            // remove orphaned user session from the cache
            internalRemoveUserSession(updater, updater.getKey());
            return null;
        }
        updater.initialize(persistenceState, realm, user, provider);
        return checkExpiration(updater);
    }

    private <K, V, T extends BaseUpdater<K, V>> T checkExpiration(T updater) {
        var expiration = updater.computeExpiration();
        if (expiration.isExpired()) {
            updater.markDeleted();
            return null;
        }
        return updater;
    }

    private record RealmPredicate(String realmId, boolean offline) implements InternalUserSessionPredicate {

        @Override
        public boolean testUserSession(UserSessionEntity userSession) {
            return Objects.equals(userSession.getRealmId(), realmId);
        }

        @Override
        public boolean testSessionKey(SessionKey key) {
            return key.offline() == offline;
        }
    }

    private interface InternalUserSessionPredicate extends io.reactivex.rxjava3.functions.Predicate<Map.Entry<SessionKey, MetadataValue<UserSessionEntity>>> {

        @Override
        default boolean test(Map.Entry<SessionKey, MetadataValue<UserSessionEntity>> e) {
            return testSessionKey(e.getKey()) && testUserSession(e.getValue().getValue());
        }

        boolean testUserSession(UserSessionEntity userSession);

        boolean testSessionKey(SessionKey key);
    }

    private record UserAndRealmPredicate(String realmId, String userId, boolean offline) implements InternalUserSessionPredicate {

        @Override
        public boolean testUserSession(UserSessionEntity userSession) {
            return Objects.equals(userSession.getRealmId(), realmId) && Objects.equals(userSession.getUser(), userId);
        }

        @Override
        public boolean testSessionKey(SessionKey key) {
            return key.offline() == offline;
        }

    }

    private record ClientAndRealmPredicate(String realmId, String clientId, boolean offline) implements InternalUserSessionPredicate {

        @Override
        public boolean testUserSession(UserSessionEntity userSession) {
            return Objects.equals(userSession.getRealmId(), realmId) && userSession.getAuthenticatedClientSessions().containsKey(clientId);
        }

        @Override
        public boolean testSessionKey(SessionKey key) {
            return key.offline() == offline;
        }
    }

    private record BrokerUserIdAndRealmPredicate(String realmId, String brokerUserId, boolean offline) implements InternalUserSessionPredicate {

        @Override
        public boolean testUserSession(UserSessionEntity userSession) {
            return Objects.equals(userSession.getRealmId(), realmId) && Objects.equals(userSession.getBrokerUserId(), brokerUserId);
        }

        @Override
        public boolean testSessionKey(SessionKey key) {
            return key.offline() == offline;
        }
    }

    private record BrokerSessionIdAndRealmPredicate(String realmId, String brokeSessionId, boolean offline)
            implements InternalUserSessionPredicate {

        @Override
        public boolean testUserSession(UserSessionEntity userSession) {
            return Objects.equals(userSession.getRealmId(), realmId) && Objects.equals(userSession.getBrokerSessionId(), brokeSessionId);
        }

        @Override
        public boolean testSessionKey(SessionKey key) {
            return key.offline() == offline;
        }
    }

    private class RemoteClientSessionAdapterProvider implements ClientSessionProvider, UserSessionUpdater.ClientSessionAdapterFactory {

        private final RemoteChangeLogTransaction<SessionKey, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> transaction;
        private final UserSessionUpdater userSession;

        private RemoteClientSessionAdapterProvider(RemoteChangeLogTransaction<SessionKey, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> transaction, UserSessionUpdater userSession) {
            this.transaction = transaction;
            this.userSession = userSession;
        }

        @Override
        public AuthenticatedClientSessionModel getClientSession(String clientId, SessionKey clientSessionId) {
            if (clientId == null || clientSessionId == null) {
                return null;
            }
            var client = userSession.getRealm().getClientById(clientId);
            if (client == null) {
                return null;
            }
            return initialize(client, transaction.get(clientSessionId));
        }

        @Override
        public CompletionStage<AuthenticatedClientSessionModel> getClientSessionAsync(String clientId, SessionKey clientSessionId) {
            if (clientId == null || clientSessionId == null) {
                return CompletableFutures.completedNull();
            }
            var client = userSession.getRealm().getClientById(clientId);
            if (client == null) {
                return CompletableFutures.completedNull();
            }
            return transaction.getAsync(clientSessionId).thenApply(updater -> initialize(client, updater));
        }

        @Override
        public void removeClientSession(SessionKey clientSessionId) {
            if (clientSessionId == null) {
                return;
            }
            transaction.remove(clientSessionId);
        }

        private AuthenticatedClientSessionModel initialize(ClientModel client, AuthenticatedClientSessionUpdater updater) {
            if (updater == null) {
                return null;
            }
            if (updater.isInitialized()) {
                return updater;
            }
            updater.initialize(userSession, client, transaction);
            return checkExpiration(updater);
        }

        @Override
        public ClientSessionMappingAdapter create(AuthenticatedClientSessionStore clientSessionStore, boolean offline) {
            return new ClientSessionMappingAdapter(clientSessionStore, this, offline);
        }
    }

}
