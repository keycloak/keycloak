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
import org.keycloak.models.sessions.infinispan.changes.remote.UserSessionTransaction;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.BaseUpdater;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.client.AuthenticatedClientSessionUpdater;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.user.ClientSessionMappingAdapter;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.user.ClientSessionProvider;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.user.UserSessionUpdater;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionStore;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
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
        var transaction = getClientSessionTransaction(false);
        var clientSessionId = UUID.randomUUID();
        var entity = AuthenticatedClientSessionEntity.create(clientSessionId, realm, client, userSession);
        var model = transaction.create(clientSessionId, entity);
        if (!model.isInitialized()) {
            model.initialize(userSession, client, transaction);
        }
        userSession.getAuthenticatedClientSessions().put(client.getId(), model);
        return model;
    }

    @Override
    public AuthenticatedClientSessionModel getClientSession(UserSessionModel userSession, ClientModel client, String clientSessionId, boolean offline) {
        if (clientSessionId == null) {
            return null;
        }
        var transaction = getClientSessionTransaction(offline);
        var updater = transaction.get(UUID.fromString(clientSessionId));
        if (updater == null) {
            return null;
        }
        if (!updater.isInitialized()) {
            updater.initialize(userSession, client, transaction);
        }
        return updater;
    }

    @Override
    public UserSessionModel createUserSession(String id, RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId, UserSessionModel.SessionPersistenceState persistenceState) {
        if (id == null) {
            id = KeycloakModelUtils.generateId();
        }

        var entity = UserSessionEntity.create(id, realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId);
        var updater = transaction.getUserSessions().create(id, entity);
        return initUserSessionUpdater(updater, persistenceState, realm, user, false);
    }

    @Override
    public UserSessionModel getUserSession(RealmModel realm, String id) {
        return getUserSession(realm, id, false);
    }

    @Override
    public Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, UserModel user) {
        return StreamsUtil.closing(streamUserSessions(new UserAndRealmPredicate(realm.getId(), user.getId()), realm, user, false));
    }

    @Override
    public Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, ClientModel client) {
        return StreamsUtil.closing(streamUserSessions(new ClientAndRealmPredicate(realm.getId(), client.getId()), realm, null, false));
    }

    @Override
    public Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, ClientModel client, Integer firstResult, Integer maxResults) {
        return StreamsUtil.paginatedStream(getUserSessionsStream(realm, client).sorted(Comparator.comparing(UserSessionModel::getLastSessionRefresh)), firstResult, maxResults);
    }

    @Override
    public Stream<UserSessionModel> getUserSessionByBrokerUserIdStream(RealmModel realm, String brokerUserId) {
        return StreamsUtil.closing(streamUserSessions(new BrokerUserIdAndRealmPredicate(realm.getId(), brokerUserId), realm, null, false));
    }

    @Override
    public UserSessionModel getUserSessionByBrokerSessionId(RealmModel realm, String brokerSessionId) {
        return StreamsUtil.closing(streamUserSessions(new BrokerSessionIdAndRealmPredicate(realm.getId(), brokerSessionId), realm, null, false))
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
        var userSessions = getUserSessionTransaction(offline);
        return Flowable.fromPublisher(userSessions.getCache().publishEntriesWithMetadata(null, batchSize))
                .filter(new RealmPredicate(realm.getId()))
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
        internalRemoveUserSession(userSession, false);
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
        Predicate<? extends SessionEntity> predicate = e -> Objects.equals(e.getRealmId(), realm.getId());
        transaction.getUserSessions().removeIf((Predicate<UserSessionEntity>) predicate);
        transaction.getClientSessions().removeIf((Predicate<AuthenticatedClientSessionEntity>) predicate);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onRealmRemoved(RealmModel realm) {
        Predicate<? extends SessionEntity> predicate = e -> Objects.equals(e.getRealmId(), realm.getId());
        transaction.getUserSessions().removeIf((Predicate<UserSessionEntity>) predicate);
        transaction.getOfflineUserSessions().removeIf((Predicate<UserSessionEntity>) predicate);
        transaction.getClientSessions().removeIf((Predicate<AuthenticatedClientSessionEntity>) predicate);
        transaction.getOfflineClientSessions().removeIf((Predicate<AuthenticatedClientSessionEntity>) predicate);
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

        var updater = getUserSessionTransaction(true).create(entity.getId(), entity);
        return initUserSessionUpdater(updater, userSession.getPersistenceState(), userSession.getRealm(), userSession.getUser(), true);
    }

    @Override
    public UserSessionModel getOfflineUserSession(RealmModel realm, String userSessionId) {
        return getUserSession(realm, userSessionId, true);
    }

    @Override
    public void removeOfflineUserSession(RealmModel realm, UserSessionModel userSession) {
        internalRemoveUserSession(userSession, true);
    }

    @Override
    public AuthenticatedClientSessionModel createOfflineClientSession(AuthenticatedClientSessionModel clientSession, UserSessionModel offlineUserSession) {
        var transaction = getClientSessionTransaction(true);
        var entity = AuthenticatedClientSessionEntity.createFromModel(clientSession);
        var model = transaction.create(entity.getId(), entity);
        if (!model.isInitialized()) {
            model.initialize(offlineUserSession, clientSession.getClient(), transaction);
        }
        offlineUserSession.getAuthenticatedClientSessions().put(clientSession.getClient().getId(), model);
        return model;
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, UserModel user) {
        return StreamsUtil.closing(streamUserSessions(new UserAndRealmPredicate(realm.getId(), user.getId()), realm, user, true));
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionByBrokerUserIdStream(RealmModel realm, String brokerUserId) {
        return StreamsUtil.closing(streamUserSessions(new BrokerUserIdAndRealmPredicate(realm.getId(), brokerUserId), realm, null, true));
    }

    @Override
    public long getOfflineSessionsCount(RealmModel realm, ClientModel client) {
        return StreamsUtil.closing(streamUserSessions(new ClientAndRealmPredicate(realm.getId(), client.getId()), realm, null, true)).count();
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, ClientModel client, Integer firstResult, Integer maxResults) {
        return StreamsUtil.closing(StreamsUtil.paginatedStream(streamUserSessions(new ClientAndRealmPredicate(realm.getId(), client.getId()), realm, null, true), firstResult, maxResults));
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
    public void importUserSessions(Collection<UserSessionModel> persistentUserSessions, boolean offline) {
        //no-op
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
        var userSessionCache = getUserSessionTransaction(offline).getCache();
        var clientSessionCache = getClientSessionTransaction(offline).getCache();

        log.infof("Migrating %s user(s) session(s) from database.", batchSize);

        return KeycloakModelUtils.runJobInTransactionWithResult(factory, kcSession -> {
            var database = kcSession.getProvider(UserSessionPersisterProvider.class);
            var stage = CompletionStages.aggregateCompletionStage();
            database.loadUserSessionsStream(-1, batchSize, offline, "")
                    .forEach(userSessionModel -> {
                        var userSessionEntity = UserSessionEntity.createFromModel(userSessionModel);
                        stage.dependsOn(userSessionCache.putIfAbsentAsync(userSessionModel.getId(), userSessionEntity));
                        userSessionBuffer.add(userSessionModel.getId());
                        for (var clientSessionModel : userSessionModel.getAuthenticatedClientSessions().values()) {
                            clientSessionBuffer.add(Map.entry(userSessionModel.getId(), clientSessionModel.getId()));
                            var clientSessionEntity = AuthenticatedClientSessionEntity.createFromModel(clientSessionModel);
                            stage.dependsOn(clientSessionCache.putIfAbsentAsync(clientSessionEntity.getId(), clientSessionEntity));
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
        var updater = getUserSessionTransaction(offline).get(id);
        if (updater == null || !updater.getValue().getRealmId().equals(realm.getId())) {
            return null;
        }
        if (updater.isInitialized()) {
            return updater;
        }
        UserModel user = session.users().getUserById(realm, updater.getValue().getUser());
        return initUserSessionUpdater(updater, UserSessionModel.SessionPersistenceState.PERSISTENT, realm, user, offline);
    }

    private void internalRemoveUserSession(UserSessionModel userSession, boolean offline) {
        var clientSessionTransaction = getClientSessionTransaction(offline);
        var userSessionTransaction = getUserSessionTransaction(offline);
        userSession.getAuthenticatedClientSessions().values()
                .stream()
                .filter(Objects::nonNull) // we need to filter, it may not be a UserSessionUpdater class.
                .map(AuthenticatedClientSessionModel::getId)
                .filter(Objects::nonNull) // we need to filter, it may not be a AuthenticatedClientSessionUpdater class.
                .map(UUID::fromString)
                .forEach(clientSessionTransaction::remove);
        userSessionTransaction.remove(userSession.getId());
    }

    private Stream<UserSessionModel> streamUserSessions(InternalUserSessionPredicate predicate, RealmModel realm, UserModel user, boolean offline) {
        var userSessions = getUserSessionTransaction(offline);
        return Flowable.fromPublisher(userSessions.getCache().publishEntriesWithMetadata(null, batchSize))
                .filter(predicate)
                .map(userSessions::wrap)
                .map(s -> initFromStream(s, realm, user, offline))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(UserSessionModel.class::cast)
                .blockingStream(batchSize);
    }

    private RemoteChangeLogTransaction<String, UserSessionEntity, UserSessionUpdater> getUserSessionTransaction(boolean offline) {
        return offline ? transaction.getOfflineUserSessions() : transaction.getUserSessions();
    }

    private RemoteChangeLogTransaction<UUID, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> getClientSessionTransaction(boolean offline) {
        return offline ? transaction.getOfflineClientSessions() : transaction.getClientSessions();
    }

    private Optional<UserSessionUpdater> initFromStream(UserSessionUpdater updater, RealmModel realm, UserModel user, boolean offline) {
        if (updater.isInitialized()) {
            return Optional.of(updater);
        }
        assert realm != null;
        if (user == null) {
            user = session.users().getUserById(realm, updater.getValue().getUser());
        }
        return Optional.ofNullable(initUserSessionUpdater(updater, UserSessionModel.SessionPersistenceState.PERSISTENT, realm, user, offline));
    }

    private UserSessionUpdater initUserSessionUpdater(UserSessionUpdater updater, UserSessionModel.SessionPersistenceState persistenceState, RealmModel realm, UserModel user, boolean offline) {
        var provider = new RemoteClientSessionAdapterProvider(getClientSessionTransaction(offline), updater);
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
            internalRemoveUserSession(updater, offline);
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

    private record RealmPredicate(String realmId) implements InternalUserSessionPredicate {

        @Override
        public boolean testUserSession(UserSessionEntity userSession) {
            return Objects.equals(userSession.getRealmId(), realmId);
        }
    }

    private interface InternalUserSessionPredicate extends io.reactivex.rxjava3.functions.Predicate<Map.Entry<String, MetadataValue<UserSessionEntity>>> {

        @Override
        default boolean test(Map.Entry<String, MetadataValue<UserSessionEntity>> e) {
            return testUserSession(e.getValue().getValue());
        }

        boolean testUserSession(UserSessionEntity userSession);
    }

    private record UserAndRealmPredicate(String realmId, String userId) implements InternalUserSessionPredicate {

        @Override
        public boolean testUserSession(UserSessionEntity userSession) {
            return Objects.equals(userSession.getRealmId(), realmId) && Objects.equals(userSession.getUser(), userId);
        }

    }

    private record ClientAndRealmPredicate(String realmId, String clientId) implements InternalUserSessionPredicate {

        @Override
        public boolean testUserSession(UserSessionEntity userSession) {
            return Objects.equals(userSession.getRealmId(), realmId) && userSession.getAuthenticatedClientSessions().containsKey(clientId);
        }
    }

    private record BrokerUserIdAndRealmPredicate(String realmId, String brokerUserId) implements InternalUserSessionPredicate {

        @Override
        public boolean testUserSession(UserSessionEntity userSession) {
            return Objects.equals(userSession.getRealmId(), realmId) && Objects.equals(userSession.getBrokerUserId(), brokerUserId);
        }
    }

    private record BrokerSessionIdAndRealmPredicate(String realmId,
                                                    String brokeSessionId) implements InternalUserSessionPredicate {

        @Override
        public boolean testUserSession(UserSessionEntity userSession) {
            return Objects.equals(userSession.getRealmId(), realmId) && Objects.equals(userSession.getBrokerSessionId(), brokeSessionId);
        }
    }

    private class RemoteClientSessionAdapterProvider implements ClientSessionProvider, UserSessionUpdater.ClientSessionAdapterFactory {

        private final RemoteChangeLogTransaction<UUID, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> transaction;
        private final UserSessionUpdater userSession;

        private RemoteClientSessionAdapterProvider(RemoteChangeLogTransaction<UUID, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> transaction, UserSessionUpdater userSession) {
            this.transaction = transaction;
            this.userSession = userSession;
        }

        @Override
        public AuthenticatedClientSessionModel getClientSession(String clientId, UUID clientSessionId) {
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
        public CompletionStage<AuthenticatedClientSessionModel> getClientSessionAsync(String clientId, UUID clientSessionId) {
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
        public void removeClientSession(UUID clientSessionId) {
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
        public ClientSessionMappingAdapter create(AuthenticatedClientSessionStore clientSessionStore) {
            return new ClientSessionMappingAdapter(clientSessionStore, this);
        }
    }

}
