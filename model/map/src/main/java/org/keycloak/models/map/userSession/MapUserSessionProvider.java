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
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.UserSessionModel.CORRESPONDING_SESSION_ID;
import static org.keycloak.models.UserSessionModel.SessionPersistenceState.TRANSIENT;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;
import static org.keycloak.models.map.userSession.SessionExpiration.setClientSessionExpiration;
import static org.keycloak.models.map.userSession.SessionExpiration.setUserSessionExpiration;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapUserSessionProvider implements UserSessionProvider {

    private static final Logger LOG = Logger.getLogger(MapUserSessionProvider.class);
    private final KeycloakSession session;
    protected final MapKeycloakTransaction<MapUserSessionEntity, UserSessionModel> userSessionTx;
    protected final MapKeycloakTransaction<MapAuthenticatedClientSessionEntity, AuthenticatedClientSessionModel> clientSessionTx;

    /**
     * Storage for transient user sessions which lifespan is limited to one request.
     */
    private final Map<String, MapUserSessionEntity> transientUserSessions = new HashMap<>();

    public MapUserSessionProvider(KeycloakSession session, MapStorage<MapUserSessionEntity, UserSessionModel> userSessionStore,
                                  MapStorage<MapAuthenticatedClientSessionEntity, AuthenticatedClientSessionModel> clientSessionStore) {
        this.session = session;
        userSessionTx = userSessionStore.createTransaction(session);
        clientSessionTx = clientSessionStore.createTransaction(session);

        session.getTransactionManager().enlistAfterCompletion(userSessionTx);
        session.getTransactionManager().enlistAfterCompletion(clientSessionTx);
    }

    private Function<MapUserSessionEntity, UserSessionModel> userEntityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return (origEntity) -> {
            if (origEntity.getExpiration() <= Time.currentTime()) {
                if (Objects.equals(origEntity.getPersistenceState(), TRANSIENT)) {
                    transientUserSessions.remove(origEntity.getId());
                }
                userSessionTx.delete(origEntity.getId());
                return null;
            } else {
                return new MapUserSessionAdapter(session, realm, origEntity) {
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

    private Function<MapAuthenticatedClientSessionEntity, AuthenticatedClientSessionModel> clientEntityToAdapterFunc(RealmModel realm,
                                                                                                                     ClientModel client,
                                                                                                                     UserSessionModel userSession) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return origEntity -> {
            if (origEntity.getExpiration() <= Time.currentTime()) {
                userSession.removeAuthenticatedClientSessions(Arrays.asList(origEntity.getClientId()));
                clientSessionTx.delete(origEntity.getId());
                return null;
            } else {
                return new MapAuthenticatedClientSessionAdapter(session, realm, client, userSession, origEntity) {
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
        MapAuthenticatedClientSessionEntity entity =
                new MapAuthenticatedClientSessionEntity(null, userSession.getId(), realm.getId(), client.getId(), false);
        entity.getNotes().put(AuthenticatedClientSessionModel.STARTED_AT_NOTE, String.valueOf(entity.getTimestamp()));
        setClientSessionExpiration(entity, realm, client);

        LOG.tracef("createClientSession(%s, %s, %s)%s", realm, client, userSession, getShortStackTrace());

        entity = clientSessionTx.create(entity);

        MapUserSessionEntity userSessionEntity = getUserSessionById(userSession.getId());

        if (userSessionEntity == null) {
            throw new IllegalStateException("User session entity does not exist: " + userSession.getId());
        }

        userSessionEntity.addAuthenticatedClientSession(client.getId(), entity.getId());

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

        DefaultModelCriteria<AuthenticatedClientSessionModel> mcb = criteria();
        mcb = mcb.compare(AuthenticatedClientSessionModel.SearchableFields.ID, Operator.EQ, clientSessionId)
                .compare(AuthenticatedClientSessionModel.SearchableFields.USER_SESSION_ID, Operator.EQ, userSession.getId())
                .compare(AuthenticatedClientSessionModel.SearchableFields.REALM_ID, Operator.EQ, userSession.getRealm().getId())
                .compare(AuthenticatedClientSessionModel.SearchableFields.CLIENT_ID, Operator.EQ, client.getId())
                .compare(AuthenticatedClientSessionModel.SearchableFields.IS_OFFLINE, Operator.EQ, offline);

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
        LOG.tracef("createUserSession(%s, %s, %s, %s)%s", id, realm, loginUsername, persistenceState, getShortStackTrace());

        MapUserSessionEntity entity;
        if (Objects.equals(persistenceState, TRANSIENT)) {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            entity = new MapUserSessionEntity(id, realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId, false);
            transientUserSessions.put(entity.getId(), entity);
        } else {
            if (id != null && userSessionTx.read(id) != null) {
                throw new ModelDuplicateException("User session exists: " + id);
            }
            entity = new MapUserSessionEntity(id, realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId, false);
            entity = userSessionTx.create(entity);
        }

        entity.setPersistenceState(persistenceState);
        setUserSessionExpiration(entity, realm);
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

        MapUserSessionEntity userSessionEntity = transientUserSessions.get(id);
        if (userSessionEntity != null) {
            return userEntityToAdapterFunc(realm).apply(userSessionEntity);
        }

        DefaultModelCriteria<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false)
                .compare(UserSessionModel.SearchableFields.ID, Operator.EQ, id);

        return userSessionTx.read(withCriteria(mcb))
                .findFirst()
                .map(userEntityToAdapterFunc(realm))
                .orElse(null);
    }

    @Override
    public Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, UserModel user) {
        DefaultModelCriteria<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false)
                .compare(UserSessionModel.SearchableFields.USER_ID, Operator.EQ, user.getId());

        LOG.tracef("getUserSessionsStream(%s, %s)%s", realm, user, getShortStackTrace());

        return userSessionTx.read(withCriteria(mcb))
                .map(userEntityToAdapterFunc(realm))
                .filter(Objects::nonNull);
    }

    @Override
    public Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, ClientModel client) {
        DefaultModelCriteria<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false)
                .compare(UserSessionModel.SearchableFields.CLIENT_ID, Operator.EQ, client.getId());

        LOG.tracef("getUserSessionsStream(%s, %s)%s", realm, client, getShortStackTrace());

        return userSessionTx.read(withCriteria(mcb))
                .map(userEntityToAdapterFunc(realm))
                .filter(Objects::nonNull);
    }

    @Override
    public Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, ClientModel client,
                                                          Integer firstResult, Integer maxResults) {
        LOG.tracef("getUserSessionsStream(%s, %s, %s, %s)%s", realm, client, firstResult, maxResults, getShortStackTrace());

        DefaultModelCriteria<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false)
                .compare(UserSessionModel.SearchableFields.CLIENT_ID, Operator.EQ, client.getId());


        return userSessionTx.read(withCriteria(mcb).pagination(firstResult, maxResults,
                        UserSessionModel.SearchableFields.LAST_SESSION_REFRESH))
                .map(userEntityToAdapterFunc(realm))
                .filter(Objects::nonNull);
    }

    @Override
    public Stream<UserSessionModel> getUserSessionByBrokerUserIdStream(RealmModel realm, String brokerUserId) {
        DefaultModelCriteria<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false)
                .compare(UserSessionModel.SearchableFields.BROKER_USER_ID, Operator.EQ, brokerUserId);

        LOG.tracef("getUserSessionByBrokerUserIdStream(%s, %s)%s", realm, brokerUserId, getShortStackTrace());

        return userSessionTx.read(withCriteria(mcb))
                .map(userEntityToAdapterFunc(realm))
                .filter(Objects::nonNull);
    }

    @Override
    public UserSessionModel getUserSessionByBrokerSessionId(RealmModel realm, String brokerSessionId) {
        DefaultModelCriteria<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false)
                .compare(UserSessionModel.SearchableFields.BROKER_SESSION_ID, Operator.EQ, brokerSessionId);

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
        DefaultModelCriteria<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false)
                .compare(UserSessionModel.SearchableFields.CLIENT_ID, Operator.EQ, client.getId());

        LOG.tracef("getActiveUserSessions(%s, %s)%s", realm, client, getShortStackTrace());

        return userSessionTx.getCount(withCriteria(mcb));
    }

    @Override
    public Map<String, Long> getActiveClientSessionStats(RealmModel realm, boolean offline) {
        DefaultModelCriteria<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, offline);

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

        DefaultModelCriteria<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false)
                .compare(UserSessionModel.SearchableFields.ID, Operator.EQ, session.getId());

        LOG.tracef("removeUserSession(%s, %s)%s", realm, session, getShortStackTrace());

        userSessionTx.delete(withCriteria(mcb));
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        DefaultModelCriteria<UserSessionModel> mcb = criteria();
        mcb = mcb.compare(UserSessionModel.SearchableFields.REALM_ID, Operator.EQ, realm.getId())
                .compare(UserSessionModel.SearchableFields.USER_ID, Operator.EQ, user.getId());

        LOG.tracef("removeUserSessions(%s, %s)%s", realm, user, getShortStackTrace());

        userSessionTx.delete(withCriteria(mcb));
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
        DefaultModelCriteria<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, false);

        LOG.tracef("removeUserSessions(%s)%s", realm, getShortStackTrace());

        userSessionTx.delete(withCriteria(mcb));
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

        MapUserSessionEntity offlineUserSession = createUserSessionEntityInstance(userSession, true);
        offlineUserSession = userSessionTx.create(offlineUserSession);

        // set a reference for the offline user session to the original online user session
        userSession.setNote(CORRESPONDING_SESSION_ID, offlineUserSession.getId());

        int currentTime = Time.currentTime();
        offlineUserSession.setStarted(currentTime);
        offlineUserSession.setLastSessionRefresh(currentTime);
        setUserSessionExpiration(offlineUserSession, userSession.getRealm());

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

        DefaultModelCriteria<UserSessionModel> mcb;
        if (userSession.isOffline()) {
            userSessionTx.delete(userSession.getId());
        } else if (userSession.getNote(CORRESPONDING_SESSION_ID) != null) {
            String uk = userSession.getNote(CORRESPONDING_SESSION_ID);
            mcb = realmAndOfflineCriteriaBuilder(realm, true)
                    .compare(UserSessionModel.SearchableFields.ID, Operator.EQ, uk);
            userSessionTx.delete(withCriteria(mcb));
            userSession.removeNote(CORRESPONDING_SESSION_ID);
        }
    }

    @Override
    public AuthenticatedClientSessionModel createOfflineClientSession(AuthenticatedClientSessionModel clientSession,
                                                                      UserSessionModel offlineUserSession) {
        LOG.tracef("createOfflineClientSession(%s, %s)%s", clientSession, offlineUserSession, getShortStackTrace());

        MapAuthenticatedClientSessionEntity clientSessionEntity = createAuthenticatedClientSessionInstance(clientSession, offlineUserSession, true);
        int currentTime = Time.currentTime();
        clientSessionEntity.getNotes().put(AuthenticatedClientSessionModel.STARTED_AT_NOTE, String.valueOf(currentTime));
        clientSessionEntity.setTimestamp(currentTime);
        setClientSessionExpiration(clientSessionEntity, clientSession.getRealm(), clientSession.getClient());
        clientSessionEntity = clientSessionTx.create(clientSessionEntity);

        Optional<MapUserSessionEntity> userSessionEntity = getOfflineUserSessionEntityStream(clientSession.getRealm(), offlineUserSession.getId()).findFirst();
        if (userSessionEntity.isPresent()) {
            userSessionEntity.get().addAuthenticatedClientSession(clientSession.getClient().getId(), clientSessionEntity.getId());
        }

        return clientEntityToAdapterFunc(clientSession.getRealm(),
                clientSession.getClient(), offlineUserSession).apply(clientSessionEntity);
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, UserModel user) {
        DefaultModelCriteria<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, true)
                .compare(UserSessionModel.SearchableFields.USER_ID, Operator.EQ, user.getId());

        LOG.tracef("getOfflineUserSessionsStream(%s, %s)%s", realm, user, getShortStackTrace());

        return userSessionTx.read(withCriteria(mcb))
                .map(userEntityToAdapterFunc(realm))
                .filter(Objects::nonNull);
    }

    @Override
    public UserSessionModel getOfflineUserSessionByBrokerSessionId(RealmModel realm, String brokerSessionId) {
        DefaultModelCriteria<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, true)
                .compare(UserSessionModel.SearchableFields.BROKER_SESSION_ID, Operator.EQ, brokerSessionId);

        LOG.tracef("getOfflineUserSessionByBrokerSessionId(%s, %s)%s", realm, brokerSessionId, getShortStackTrace());

        return userSessionTx.read(withCriteria(mcb))
                .findFirst()
                .map(userEntityToAdapterFunc(realm))
                .orElse(null);
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionByBrokerUserIdStream(RealmModel realm, String brokerUserId) {
        DefaultModelCriteria<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, true)
                .compare(UserSessionModel.SearchableFields.BROKER_USER_ID, Operator.EQ, brokerUserId);

        LOG.tracef("getOfflineUserSessionByBrokerUserIdStream(%s, %s)%s", realm, brokerUserId, getShortStackTrace());

        return userSessionTx.read(withCriteria(mcb))
                .map(userEntityToAdapterFunc(realm))
                .filter(Objects::nonNull);
    }

    @Override
    public long getOfflineSessionsCount(RealmModel realm, ClientModel client) {
        DefaultModelCriteria<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, true)
                .compare(UserSessionModel.SearchableFields.CLIENT_ID, Operator.EQ, client.getId());

        LOG.tracef("getOfflineSessionsCount(%s, %s)%s", realm, client, getShortStackTrace());

        return userSessionTx.getCount(withCriteria(mcb));
    }

    @Override
    public Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, ClientModel client,
                                                                 Integer firstResult, Integer maxResults) {
        DefaultModelCriteria<UserSessionModel> mcb = realmAndOfflineCriteriaBuilder(realm, true)
                .compare(UserSessionModel.SearchableFields.CLIENT_ID, Operator.EQ, client.getId());

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
                MapUserSessionEntity userSessionEntity = new MapUserSessionEntity(null, pus.getRealm(), pus.getUser(),
                        pus.getLoginUsername(), pus.getIpAddress(), pus.getAuthMethod(),
                        pus.isRememberMe(), pus.getBrokerSessionId(), pus.getBrokerUserId(), offline);

                for (Map.Entry<String, AuthenticatedClientSessionModel> entry : pus.getAuthenticatedClientSessions().entrySet()) {
                    MapAuthenticatedClientSessionEntity clientSession = createAuthenticatedClientSessionInstance(entry.getValue(), entry.getValue().getUserSession(), offline);

                    // Update timestamp to same value as userSession. LastSessionRefresh of userSession from DB will have correct value
                    clientSession.setTimestamp(userSessionEntity.getLastSessionRefresh());

                    clientSession = clientSessionTx.create(clientSession);
                    userSessionEntity.addAuthenticatedClientSession(entry.getKey(), clientSession.getId());
                }

                return userSessionEntity;
            })
            .forEach(use -> userSessionTx.create(use));
    }

    @Override
    public void close() {

    }

    private Stream<MapUserSessionEntity> getOfflineUserSessionEntityStream(RealmModel realm, String userSessionId) {
        if (userSessionId == null) {
            return Stream.empty();
        }

        // first get a user entity by ID
        DefaultModelCriteria<UserSessionModel> mcb = criteria();
        mcb = mcb.compare(UserSessionModel.SearchableFields.REALM_ID, Operator.EQ, realm.getId())
                .compare(UserSessionModel.SearchableFields.ID, Operator.EQ, userSessionId);

        // check if it's an offline user session
        MapUserSessionEntity userSessionEntity = userSessionTx.read(withCriteria(mcb)).findFirst().orElse(null);
        if (userSessionEntity != null) {
            if (userSessionEntity.isOffline()) {
                return Stream.of(userSessionEntity);
            }
        } else {
            // no session found by the given ID, try to find by corresponding session ID
            mcb = realmAndOfflineCriteriaBuilder(realm, true)
                    .compare(UserSessionModel.SearchableFields.CORRESPONDING_SESSION_ID, Operator.EQ, userSessionId);
            return userSessionTx.read(withCriteria(mcb));
        }

        // it's online user session so lookup offline user session by corresponding session id reference
        String offlineUserSessionId = userSessionEntity.getNote(CORRESPONDING_SESSION_ID);
        if (offlineUserSessionId != null) {
            mcb = realmAndOfflineCriteriaBuilder(realm, true)
                    .compare(UserSessionModel.SearchableFields.ID, Operator.EQ, offlineUserSessionId);
            return userSessionTx.read(withCriteria(mcb));
        }

        return Stream.empty();
    }

    private DefaultModelCriteria<UserSessionModel> realmAndOfflineCriteriaBuilder(RealmModel realm, boolean offline) {
        return DefaultModelCriteria.<UserSessionModel>criteria()
                .compare(UserSessionModel.SearchableFields.REALM_ID, Operator.EQ, realm.getId())
                .compare(UserSessionModel.SearchableFields.IS_OFFLINE, Operator.EQ, offline);
    }

    private MapUserSessionEntity getUserSessionById(String id) {
        MapUserSessionEntity userSessionEntity = transientUserSessions.get(id);

        if (userSessionEntity == null) {
            MapUserSessionEntity userSession = userSessionTx.read(id);
            return userSession;
        }
        return userSessionEntity;
    }

    private MapUserSessionEntity createUserSessionEntityInstance(UserSessionModel userSession, boolean offline) {
        MapUserSessionEntity entity = new MapUserSessionEntity(null, userSession.getRealm().getId());

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

    private MapAuthenticatedClientSessionEntity createAuthenticatedClientSessionInstance(AuthenticatedClientSessionModel clientSession,
                                                                                         UserSessionModel userSession, boolean offline) {
        MapAuthenticatedClientSessionEntity entity = new MapAuthenticatedClientSessionEntity(null,
                userSession.getId(), clientSession.getRealm().getId(), clientSession.getClient().getId(), offline);

        entity.setAction(clientSession.getAction());
        entity.setAuthMethod(clientSession.getProtocol());

        entity.setNotes(new ConcurrentHashMap<>(clientSession.getNotes()));
        entity.setRedirectUri(clientSession.getRedirectUri());
        entity.setTimestamp(clientSession.getTimestamp());

        return entity;
    }
}
