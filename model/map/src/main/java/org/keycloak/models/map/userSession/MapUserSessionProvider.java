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
package org.keycloak.models.map.userSession;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.device.DeviceActivityManager;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.UserSessionModel.CORRESPONDING_SESSION_ID;
import static org.keycloak.models.UserSessionModel.SessionPersistenceState.TRANSIENT;
import static org.keycloak.models.map.common.MapStorageUtils.registerEntityForChanges;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;
import static org.keycloak.models.map.userSession.SessionExpiration.setClientSessionExpiration;
import static org.keycloak.models.map.userSession.SessionExpiration.setUserSessionExpiration;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapUserSessionProvider<UK, CK> implements UserSessionProvider {

    private static final Logger LOG = Logger.getLogger(MapUserSessionProvider.class);
    private final KeycloakSession session;
    protected final MapKeycloakTransaction<UK, MapUserSessionEntity<UK>, UserSessionModel> userSessionTx;
    protected final MapKeycloakTransaction<CK, MapAuthenticatedClientSessionEntity<CK>, AuthenticatedClientSessionModel> clientSessionTx;
    private final MapStorage<UK, MapUserSessionEntity<UK>, UserSessionModel> userSessionStore;
    private final MapStorage<CK, MapAuthenticatedClientSessionEntity<CK>, AuthenticatedClientSessionModel> clientSessionStore;

    /**
     * Storage for transient user sessions which lifespan is limited to one request.
     */
    private final Map<UK, MapUserSessionEntity<UK>> transientUserSessions = new HashMap<>();

    public MapUserSessionProvider(KeycloakSession session, MapStorage<UK, MapUserSessionEntity<UK>, UserSessionModel> userSessionStore,
                                  MapStorage<CK, MapAuthenticatedClientSessionEntity<CK>, AuthenticatedClientSessionModel> clientSessionStore) {
        this.session = session;
        this.userSessionStore = userSessionStore;
        this.clientSessionStore = clientSessionStore;
        userSessionTx = userSessionStore.createTransaction(session);
        clientSessionTx = clientSessionStore.createTransaction(session);

        session.getTransactionManager().enlistAfterCompletion(userSessionTx);
        session.getTransactionManager().enlistAfterCompletion(clientSessionTx);
    }

    private Function<MapUserSessionEntity<UK>, UserSessionModel> userEntityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return (origEntity) -> {
            if (origEntity.getExpiration() <= Time.currentTime()) {
                if (Objects.equals(origEntity.getPersistenceState(), TRANSIENT)) {
                    transientUserSessions.remove(origEntity.getId());
                }
                userSessionTx.delete(origEntity.getId());
                return null;
            } else {
                return new MapUserSessionAdapter<UK>(session, realm,
                        Objects.equals(origEntity.getPersistenceState(), TRANSIENT) ? origEntity : registerEntityForChanges(userSessionTx, origEntity)) {
                    @Override
                    public String getId() {
                        return userSessionStore.getKeyConvertor().keyToString(entity.getId());
                    }

                    @Override
                    public void removeAuthenticatedClientSessions(Collection<String> removedClientUKS) {
                        removedClientUKS.forEach(entity::removeAuthenticatedClientSession);
                    }

                    @Override
                    public void setLastSessionRefresh(int lastSessionRefresh) {
                        entity.setLastSessionRefresh(lastSessionRefresh);
                        // whenever the lastSessionRefresh is changed recompute the expiration time
                        setUserSessionExpiration(entity, realm);
                    }
                };
            }
        };
    }

    private Function<MapAuthenticatedClientSessionEntity<CK>, AuthenticatedClientSessionModel> clientEntityToAdapterFunc(RealmModel realm,
                                                                                                                     ClientModel client,
                                                                                                                     UserSessionModel userSession) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return origEntity -> {
            if (origEntity.getExpiration() <= Time.currentTime()) {
                userSession.removeAuthenticatedClientSessions(Arrays.asList(origEntity.getClientId()));
                clientSessionTx.delete(origEntity.getId());
                return null;
            } else {
                return new MapAuthenticatedClientSessionAdapter<CK>(session, realm, client, userSession, registerEntityForChanges(clientSessionTx, origEntity)) {
                    @Override
                    public String getId() {
                        return clientSessionStore.getKeyConvertor().keyToString(entity.getId());
                    }

                    @Override
                    public void detachFromUserSession() {
                        this.userSession = null;

                        clientSessionTx.delete(entity.getId());
                    }

                    @Override
                    public void setTimestamp(int timestamp) {
                        entity.setTimestamp(timestamp);
                        // whenever the timestamp is changed recompute the expiration time
                        setClientSessionExpiration(entity, realm, client);
                    }
                };
            }
        };
    }

    @Override
    public KeycloakSession getKeycloakSession() {
        return session;
    }

    @Override
    public AuthenticatedClientSessionModel createClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession) {
        MapAuthenticatedClientSessionEntity<CK> entity =
                new MapAuthenticatedClientSessionEntity<>(clientSessionStore.getKeyConvertor().yieldNewUniqueKey(), userSession.getId(), realm.getId(), client.getId(), false);
        entity.getNotes().put(AuthenticatedClientSessionModel.STARTED_AT_NOTE, String.valueOf(entity.getTimestamp()));
        setClientSessionExpiration(entity, realm, client);

        LOG.tracef("createClientSession(%s, %s, %s)%s", realm, client, userSession, getShortStackTrace());

        clientSessionTx.create(entity);

        MapUserSessionEntity<UK> userSessionEntity = getUserSessionById(userSessionStore.getKeyConvertor().fromString(userSession.getId()));

        if (userSessionEntity == null) {
            throw new IllegalStateException("User session entity does not exist: " + userSession.getId());
        }

        userSessionEntity.addAuthenticatedClientSession(client.getId(), clientSessionStore.getKeyConvertor().keyToString(entity.getId()));

        return clientEntityToAdapterFunc(realm, client, userSession).apply(entity);
    }

    @Override
    public AuthenticatedClientSessionModel getClientSession(UserSessionModel userSession, ClientModel client,
                                                            String clientSessionId, boolean offline) {
        LOG.tracef("getClientSession(%s, %s, %s, %s)%s", userSession, client,
                clientSessionId, offline, getShortStackTrace());

        Objects.requireNonNull(userSession, "The provided user session cannot be null!");
        Objects.requireNonNull(client, "The provided client cannot be null!");
        if (clientSessionId == null) {
            return null;
        }

        CK ck = clientSessionStore.getKeyConvertor().fromStringSafe(clientSessionId);
        ModelCriteriaBuilder<AuthenticatedClientSessionModel> mcb = clientSessionStore.createCriteriaBuilder()
                .compare(AuthenticatedClientSessionModel.SearchableFields.ID, ModelCriteriaBuilder.Operator.EQ, ck)
                .compare(AuthenticatedClientSessionModel.SearchableFields.USER_SESSION_ID, ModelCriteriaBuilder.Operator.EQ, userSession.getId())
                .compare(AuthenticatedClientSessionModel.SearchableFields.REALM_ID, ModelCriteriaBuilder.Operator.EQ, userSession.getRealm().getId())
                .compare(AuthenticatedClientSessionModel.SearchableFields.CLIENT_ID, ModelCriteriaBuilder.Operator.EQ, client.getId())
                .compare(AuthenticatedClientSessionModel.SearchableFields.IS_OFFLINE, ModelCriteriaBuilder.Operator.EQ, offline);

        return clientSessionTx.read(withCriteria(mcb))
                .findFirst()
                .map(clientEntityToAdapterFunc(client.getRealm(), client, userSession))
                .orElse(null);
    }

    @Override
    public UserSessionModel createUserSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress,
                                              String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        return createUserSession(null, realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId,
                brokerUserId, UserSessionModel.SessionPersistenceState.PERSISTENT);
    }

    @Override
    public UserSessionModel createUserSession(String id, RealmModel realm, UserModel user, String loginUsername,
                                              String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId,
                                              String brokerUserId, UserSessionModel.SessionPersistenceState persistenceState) {
        final UK entityId = id == null ? userSessionStore.getKeyConvertor().yieldNewUniqueKey(): userSessionStore.getKeyConvertor().fromString(id);

        LOG.tracef("createUserSession(%s, %s, %s, %s)%s", id, realm, loginUsername, persistenceState, getShortStackTrace());

        MapUserSessionEntity<UK> entity = new MapUserSessionEntity<>(entityId, realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId, false);
        entity.setPersistenceState(persistenceState);
        setUserSessionExpiration(entity, realm);

        if (Objects.equals(persistenceState, TRANSIENT)) {
            transientUserSessions.put(entityId, entity);
        } else {
            if (userSessionTx.read(entity.getId()) != null) {
                throw new ModelDuplicateException("User session exists: " + entity.getId());
            }

            userSessionTx.create(entity);
        }

        UserSessionModel userSession = userEntityToAdapterFunc(realm).apply(entity);

        if (userSession != null) {
            DeviceActivityManager.attachDevice(userSession, session);
        }

        return userSession;
    }

    @Override
    public UserSessionModel getUserSession(RealmModel realm, String id) {
        Objects.requireNonNull(realm, "The provided realm can't be null!");

        LOG.tracef("getUserSession(%s, %s)%s", realm, id, getShortStackTrace());

        UK uuid = userSessionStore.getKeyConvertor().fromStringSafe(id);
        if (uuid == null) {
            return null;
        }

        MapUserSessionEntity<UK> userSessionEntity = transientUserSessions.get(uuid);
        if (userSessionEntity != null) {
            return userEntityToAdapterFunc(realm).apply(userSessionEntity);
        }

        ModelCriteriaBuilder<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false)
                .compare(UserSessionModel.SearchableFields.ID, ModelCriteriaBuilder.Operator.EQ, uuid);

        return userSessionTx.read(withCriteria(mcb))
                .findFirst()
                .map(userEntityToAdapterFunc(realm))
                .orElse(null);
    }

    @Override
    public Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, UserModel user) {
        ModelCriteriaBuilder<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false)
                .compare(UserSessionModel.SearchableFields.USER_ID, ModelCriteriaBuilder.Operator.EQ, user.getId());

        LOG.tracef("getUserSessionsStream(%s, %s)%s", realm, user, getShortStackTrace());

        return userSessionTx.read(withCriteria(mcb))
                .map(userEntityToAdapterFunc(realm))
                .filter(Objects::nonNull);
    }

    @Override
    public Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, ClientModel client) {
        ModelCriteriaBuilder<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false)
                .compare(UserSessionModel.SearchableFields.CLIENT_ID, ModelCriteriaBuilder.Operator.EQ, client.getId());

        LOG.tracef("getUserSessionsStream(%s, %s)%s", realm, client, getShortStackTrace());

        return userSessionTx.read(withCriteria(mcb))
                .map(userEntityToAdapterFunc(realm))
                .filter(Objects::nonNull);
    }

    @Override
    public Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, ClientModel client,
                                                          Integer firstResult, Integer maxResults) {
        LOG.tracef("getUserSessionsStream(%s, %s, %s, %s)%s", realm, client, firstResult, maxResults, getShortStackTrace());

        ModelCriteriaBuilder<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false)
                .compare(UserSessionModel.SearchableFields.CLIENT_ID, ModelCriteriaBuilder.Operator.EQ, client.getId());


        return userSessionTx.read(withCriteria(mcb).pagination(firstResult, maxResults,
                        UserSessionModel.SearchableFields.LAST_SESSION_REFRESH))
                .map(userEntityToAdapterFunc(realm))
                .filter(Objects::nonNull);
    }

    @Override
    public Stream<UserSessionModel> getUserSessionByBrokerUserIdStream(RealmModel realm, String brokerUserId) {
        ModelCriteriaBuilder<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false)
                .compare(UserSessionModel.SearchableFields.BROKER_USER_ID, ModelCriteriaBuilder.Operator.EQ, brokerUserId);

        LOG.tracef("getUserSessionByBrokerUserIdStream(%s, %s)%s", realm, brokerUserId, getShortStackTrace());

        return userSessionTx.read(withCriteria(mcb))
                .map(userEntityToAdapterFunc(realm))
                .filter(Objects::nonNull);
    }

    @Override
    public UserSessionModel getUserSessionByBrokerSessionId(RealmModel realm, String brokerSessionId) {
        ModelCriteriaBuilder<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false)
                .compare(UserSessionModel.SearchableFields.BROKER_SESSION_ID, ModelCriteriaBuilder.Operator.EQ, brokerSessionId);

        LOG.tracef("getUserSessionByBrokerSessionId(%s, %s)%s", realm, brokerSessionId, getShortStackTrace());

        return userSessionTx.read(withCriteria(mcb))
                .findFirst()
                .map(userEntityToAdapterFunc(realm))
                .orElse(null);
    }

    @Override
    public UserSessionModel getUserSessionWithPredicate(RealmModel realm, String id, boolean offline,
                                                        Predicate<UserSessionModel> predicate) {
        LOG.tracef("getUserSessionWithPredicate(%s, %s, %s)%s", realm, id, offline, getShortStackTrace());

        Stream<UserSessionModel> userSessionEntityStream;
        if (offline) {
            userSessionEntityStream = getOfflineUserSessionEntityStream(realm, id)
                    .map(userEntityToAdapterFunc(realm)).filter(Objects::nonNull);
        } else {
            UserSessionModel userSession = getUserSession(realm, id);
            userSessionEntityStream = userSession != null ? Stream.of(userSession) : Stream.empty();
        }

        return userSessionEntityStream
                .filter(predicate)
                .findFirst()
                .orElse(null);
    }

    @Override
    public long getActiveUserSessions(RealmModel realm, ClientModel client) {
        ModelCriteriaBuilder<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false)
                .compare(UserSessionModel.SearchableFields.CLIENT_ID, ModelCriteriaBuilder.Operator.EQ, client.getId());

        LOG.tracef("getActiveUserSessions(%s, %s)%s", realm, client, getShortStackTrace());

        return userSessionTx.getCount(withCriteria(mcb));
    }

    @Override
    public Map<String, Long> getActiveClientSessionStats(RealmModel realm, boolean offline) {
        ModelCriteriaBuilder<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, offline);

        LOG.tracef("getActiveClientSessionStats(%s, %s)%s", realm, offline, getShortStackTrace());

        return userSessionTx.read(withCriteria(mcb))
                .map(userEntityToAdapterFunc(realm))
                .filter(Objects::nonNull)
                .map(UserSessionModel::getAuthenticatedClientSessions)
                .map(Map::keySet)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    @Override
    public void removeUserSession(RealmModel realm, UserSessionModel session) {
        Objects.requireNonNull(session, "The provided user session can't be null!");

        UK uk = userSessionStore.getKeyConvertor().fromString(session.getId());
        ModelCriteriaBuilder<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false)
                .compare(UserSessionModel.SearchableFields.ID, ModelCriteriaBuilder.Operator.EQ, uk);

        LOG.tracef("removeUserSession(%s, %s)%s", realm, session, getShortStackTrace());

        userSessionTx.delete(userSessionStore.getKeyConvertor().yieldNewUniqueKey(), withCriteria(mcb));
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        ModelCriteriaBuilder<UserSessionModel> mcb = userSessionStore.createCriteriaBuilder()
                .compare(UserSessionModel.SearchableFields.REALM_ID, ModelCriteriaBuilder.Operator.EQ, realm.getId())
                .compare(UserSessionModel.SearchableFields.USER_ID, ModelCriteriaBuilder.Operator.EQ, user.getId());

        LOG.tracef("removeUserSessions(%s, %s)%s", realm, user, getShortStackTrace());

        userSessionTx.delete(userSessionStore.getKeyConvertor().yieldNewUniqueKey(), withCriteria(mcb));
    }

    @Override
    public void removeAllExpired() {
        LOG.tracef("removeAllExpired()%s", getShortStackTrace());
    }

    @Override
    public void removeExpired(RealmModel realm) {
        LOG.tracef("removeExpired(%s)%s", realm, getShortStackTrace());
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        ModelCriteriaBuilder<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false);

        LOG.tracef("removeUserSessions(%s)%s", realm, getShortStackTrace());

        userSessionTx.delete(userSessionStore.getKeyConvertor().yieldNewUniqueKey(), withCriteria(mcb));
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        LOG.tracef("onRealmRemoved(%s)%s", realm, getShortStackTrace());

        removeUserSessions(realm);
    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {

    }

    @Override
    public UserSessionModel createOfflineUserSession(UserSessionModel userSession) {
        LOG.tracef("createOfflineUserSession(%s)%s", userSession, getShortStackTrace());

        MapUserSessionEntity<UK> offlineUserSession = createUserSessionEntityInstance(userSession, true);

        // set a reference for the offline user session to the original online user session
        userSession.setNote(CORRESPONDING_SESSION_ID, offlineUserSession.getId().toString());

        int currentTime = Time.currentTime();
        offlineUserSession.setStarted(currentTime);
        offlineUserSession.setLastSessionRefresh(currentTime);
        setUserSessionExpiration(offlineUserSession, userSession.getRealm());

        userSessionTx.create(offlineUserSession);

        return userEntityToAdapterFunc(userSession.getRealm()).apply(offlineUserSession);
    }

    @Override
    public UserSessionModel getOfflineUserSession(RealmModel realm, String userSessionId) {
        LOG.tracef("getOfflineUserSession(%s, %s)%s", realm, userSessionId, getShortStackTrace());

        return getOfflineUserSessionEntityStream(realm, userSessionId)
                .findFirst()
                .map(userEntityToAdapterFunc(realm))
                .orElse(null);
    }

    @Override
    public void removeOfflineUserSession(RealmModel realm, UserSessionModel userSession) {
        Objects.requireNonNull(userSession, "The provided user session can't be null!");

        LOG.tracef("removeOfflineUserSession(%s, %s)%s", realm, userSession, getShortStackTrace());

        ModelCriteriaBuilder<UserSessionModel> mcb;
        if (userSession.isOffline()) {
            userSessionTx.delete(userSessionStore.getKeyConvertor().fromString(userSession.getId()));
        } else if (userSession.getNote(CORRESPONDING_SESSION_ID) != null) {
            UK uk = userSessionStore.getKeyConvertor().fromString(userSession.getNote(CORRESPONDING_SESSION_ID));
            mcb = realmAndOfflineCriteriaBuilder(realm, true)
                    .compare(UserSessionModel.SearchableFields.ID, ModelCriteriaBuilder.Operator.EQ, uk);
            userSessionTx.delete(userSessionStore.getKeyConvertor().yieldNewUniqueKey(), withCriteria(mcb));
            userSession.removeNote(CORRESPONDING_SESSION_ID);
        }
    }

    @Override
    public AuthenticatedClientSessionModel createOfflineClientSession(AuthenticatedClientSessionModel clientSession,
                                                                      UserSessionModel offlineUserSession) {
        LOG.tracef("createOfflineClientSession(%s, %s)%s", clientSession, offlineUserSession, getShortStackTrace());

        MapAuthenticatedClientSessionEntity<CK> clientSessionEntity = createAuthenticatedClientSessionInstance(clientSession, offlineUserSession, true);
        int currentTime = Time.currentTime();
        clientSessionEntity.getNotes().put(AuthenticatedClientSessionModel.STARTED_AT_NOTE, String.valueOf(currentTime));
        clientSessionEntity.setTimestamp(currentTime);
        setClientSessionExpiration(clientSessionEntity, clientSession.getRealm(), clientSession.getClient());

        Optional<MapUserSessionEntity<UK>> userSessionEntity = getOfflineUserSessionEntityStream(clientSession.getRealm(), offlineUserSession.getId()).findFirst();
        if (userSessionEntity.isPresent()) {
            userSessionEntity.get().addAuthenticatedClientSession(clientSession.getClient().getId(), clientSessionStore.getKeyConvertor().keyToString(clientSessionEntity.getId()));
        }

        clientSessionTx.create(clientSessionEntity);

        return clientEntityToAdapterFunc(clientSession.getRealm(),
                clientSession.getClient(), offlineUserSession).apply(clientSessionEntity);
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, UserModel user) {
        ModelCriteriaBuilder<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, true)
                .compare(UserSessionModel.SearchableFields.USER_ID, ModelCriteriaBuilder.Operator.EQ, user.getId());

        LOG.tracef("getOfflineUserSessionsStream(%s, %s)%s", realm, user, getShortStackTrace());

        return userSessionTx.read(withCriteria(mcb))
                .map(userEntityToAdapterFunc(realm))
                .filter(Objects::nonNull);
    }

    @Override
    public UserSessionModel getOfflineUserSessionByBrokerSessionId(RealmModel realm, String brokerSessionId) {
        ModelCriteriaBuilder<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, true)
                .compare(UserSessionModel.SearchableFields.BROKER_SESSION_ID, ModelCriteriaBuilder.Operator.EQ, brokerSessionId);

        LOG.tracef("getOfflineUserSessionByBrokerSessionId(%s, %s)%s", realm, brokerSessionId, getShortStackTrace());

        return userSessionTx.read(withCriteria(mcb))
                .findFirst()
                .map(userEntityToAdapterFunc(realm))
                .orElse(null);
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionByBrokerUserIdStream(RealmModel realm, String brokerUserId) {
        ModelCriteriaBuilder<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, true)
                .compare(UserSessionModel.SearchableFields.BROKER_USER_ID, ModelCriteriaBuilder.Operator.EQ, brokerUserId);

        LOG.tracef("getOfflineUserSessionByBrokerUserIdStream(%s, %s)%s", realm, brokerUserId, getShortStackTrace());

        return userSessionTx.read(withCriteria(mcb))
                .map(userEntityToAdapterFunc(realm))
                .filter(Objects::nonNull);
    }

    @Override
    public long getOfflineSessionsCount(RealmModel realm, ClientModel client) {
        ModelCriteriaBuilder<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, true)
                .compare(UserSessionModel.SearchableFields.CLIENT_ID, ModelCriteriaBuilder.Operator.EQ, client.getId());

        LOG.tracef("getOfflineSessionsCount(%s, %s)%s", realm, client, getShortStackTrace());

        return userSessionTx.getCount(withCriteria(mcb));
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, ClientModel client,
                                                                 Integer firstResult, Integer maxResults) {
        ModelCriteriaBuilder<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, true)
                .compare(UserSessionModel.SearchableFields.CLIENT_ID, ModelCriteriaBuilder.Operator.EQ, client.getId());

        LOG.tracef("getOfflineUserSessionsStream(%s, %s, %s, %s)%s", realm, client, firstResult, maxResults, getShortStackTrace());

        return userSessionTx.read(withCriteria(mcb).pagination(firstResult, maxResults,
                        UserSessionModel.SearchableFields.LAST_SESSION_REFRESH))
                .map(userEntityToAdapterFunc(realm))
                .filter(Objects::nonNull);
    }

    @Override
    public void importUserSessions(Collection<UserSessionModel> persistentUserSessions, boolean offline) {
        if (persistentUserSessions == null || persistentUserSessions.isEmpty()) {
            return;
        }

        persistentUserSessions.stream()
            .map(pus -> {
                MapUserSessionEntity<UK> userSessionEntity = new MapUserSessionEntity<UK>(userSessionStore.getKeyConvertor().yieldNewUniqueKey(), pus.getRealm(), pus.getUser(),
                        pus.getLoginUsername(), pus.getIpAddress(), pus.getAuthMethod(),
                        pus.isRememberMe(), pus.getBrokerSessionId(), pus.getBrokerUserId(), offline);

                for (Map.Entry<String, AuthenticatedClientSessionModel> entry : pus.getAuthenticatedClientSessions().entrySet()) {
                    MapAuthenticatedClientSessionEntity<CK> clientSession = createAuthenticatedClientSessionInstance(entry.getValue(), entry.getValue().getUserSession(), offline);

                    // Update timestamp to same value as userSession. LastSessionRefresh of userSession from DB will have correct value
                    clientSession.setTimestamp(userSessionEntity.getLastSessionRefresh());

                    userSessionEntity.addAuthenticatedClientSession(entry.getKey(), clientSessionStore.getKeyConvertor().keyToString(clientSession.getId()));

                    clientSessionTx.create(clientSession);
                }

                return userSessionEntity;
            })
            .forEach(use -> userSessionTx.create(use));
    }

    @Override
    public void close() {

    }

    private Stream<MapUserSessionEntity<UK>> getOfflineUserSessionEntityStream(RealmModel realm, String userSessionId) {
        UK uuid = userSessionStore.getKeyConvertor().fromStringSafe(userSessionId);
        if (uuid == null) {
            return Stream.empty();
        }

        // first get a user entity by ID
        ModelCriteriaBuilder<UserSessionModel> mcb = userSessionStore.createCriteriaBuilder()
                .compare(UserSessionModel.SearchableFields.REALM_ID, ModelCriteriaBuilder.Operator.EQ, realm.getId())
                .compare(UserSessionModel.SearchableFields.ID, ModelCriteriaBuilder.Operator.EQ, uuid);

        // check if it's an offline user session
        MapUserSessionEntity<UK> userSessionEntity = userSessionTx.read(withCriteria(mcb)).findFirst().orElse(null);
        if (userSessionEntity != null) {
            if (userSessionEntity.isOffline()) {
                return Stream.of(userSessionEntity);
            }
        } else {
            // no session found by the given ID, try to find by corresponding session ID
            mcb = realmAndOfflineCriteriaBuilder(realm, true)
                    .compare(UserSessionModel.SearchableFields.CORRESPONDING_SESSION_ID, ModelCriteriaBuilder.Operator.EQ, userSessionId);
            return userSessionTx.read(withCriteria(mcb));
        }

        // it's online user session so lookup offline user session by corresponding session id reference
        String offlineUserSessionId = userSessionEntity.getNote(CORRESPONDING_SESSION_ID);
        if (offlineUserSessionId != null) {
            UK uk = userSessionStore.getKeyConvertor().fromStringSafe(offlineUserSessionId);
            mcb = realmAndOfflineCriteriaBuilder(realm, true)
                    .compare(UserSessionModel.SearchableFields.ID, ModelCriteriaBuilder.Operator.EQ, uk);
            return userSessionTx.read(withCriteria(mcb));
        }

        return Stream.empty();
    }

    private ModelCriteriaBuilder<UserSessionModel> realmAndOfflineCriteriaBuilder(RealmModel realm, boolean offline) {
        return userSessionStore.createCriteriaBuilder()
                .compare(UserSessionModel.SearchableFields.REALM_ID, ModelCriteriaBuilder.Operator.EQ, realm.getId())
                .compare(UserSessionModel.SearchableFields.IS_OFFLINE, ModelCriteriaBuilder.Operator.EQ, offline);
    }

    private MapUserSessionEntity<UK> getUserSessionById(UK id) {
        MapUserSessionEntity<UK> userSessionEntity = transientUserSessions.get(id);

        if (userSessionEntity == null) {
            MapUserSessionEntity<UK> userSession = userSessionTx.read(id);
            return userSession != null ? registerEntityForChanges(userSessionTx, userSession) : null;
        }
        return userSessionEntity;
    }

    private MapUserSessionEntity<UK> createUserSessionEntityInstance(UserSessionModel userSession, boolean offline) {
        MapUserSessionEntity<UK> entity = new MapUserSessionEntity<UK>(userSessionStore.getKeyConvertor().yieldNewUniqueKey(), userSession.getRealm().getId());

        entity.setAuthMethod(userSession.getAuthMethod());
        entity.setBrokerSessionId(userSession.getBrokerSessionId());
        entity.setBrokerUserId(userSession.getBrokerUserId());
        entity.setIpAddress(userSession.getIpAddress());
        entity.setNotes(new ConcurrentHashMap<>(userSession.getNotes()));
        entity.addNote(CORRESPONDING_SESSION_ID, userSession.getId());

        entity.clearAuthenticatedClientSessions();
        entity.setRememberMe(userSession.isRememberMe());
        entity.setState(userSession.getState());
        entity.setLoginUsername(userSession.getLoginUsername());
        entity.setUserId(userSession.getUser().getId());

        entity.setStarted(userSession.getStarted());
        entity.setLastSessionRefresh(userSession.getLastSessionRefresh());
        entity.setOffline(offline);

        return entity;
    }

    private MapAuthenticatedClientSessionEntity<CK> createAuthenticatedClientSessionInstance(AuthenticatedClientSessionModel clientSession,
                                                                                         UserSessionModel userSession, boolean offline) {
        MapAuthenticatedClientSessionEntity<CK> entity = new MapAuthenticatedClientSessionEntity<CK>(clientSessionStore.getKeyConvertor().yieldNewUniqueKey(),
                userSession.getId(), clientSession.getRealm().getId(), clientSession.getClient().getId(), offline);

        entity.setAction(clientSession.getAction());
        entity.setAuthMethod(clientSession.getProtocol());

        entity.setNotes(new ConcurrentHashMap<>(clientSession.getNotes()));
        entity.setRedirectUri(clientSession.getRedirectUri());
        entity.setTimestamp(clientSession.getTimestamp());

        return entity;
    }
}
