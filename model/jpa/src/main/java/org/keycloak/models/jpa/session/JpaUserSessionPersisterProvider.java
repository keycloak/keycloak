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

package org.keycloak.models.jpa.session;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.OfflineUserSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.PersistentAuthenticatedClientSessionAdapter;
import org.keycloak.models.session.PersistentClientSessionModel;
import org.keycloak.models.session.PersistentUserSessionAdapter;
import org.keycloak.models.session.PersistentUserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.RealmExpiration;
import org.keycloak.models.utils.SessionExpirationUtils;
import org.keycloak.storage.StorageId;
import org.keycloak.utils.StreamsUtil;

import org.jboss.logging.Logger;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.models.jpa.session.JpaSessionUtil.offlineFromString;
import static org.keycloak.models.jpa.session.JpaSessionUtil.offlineToString;
import static org.keycloak.utils.StreamsUtil.closing;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JpaUserSessionPersisterProvider implements UserSessionPersisterProvider {
    private static final Logger logger = Logger.getLogger(JpaUserSessionPersisterProvider.class);

    private final KeycloakSession session;
    private final EntityManager em;
    private final int expirationBatch;

    public JpaUserSessionPersisterProvider(KeycloakSession session, EntityManager em, int expirationBatch) {
        this.session = session;
        this.em = em;
        this.expirationBatch = expirationBatch;
    }

    @Override
    public void createUserSession(UserSessionModel userSession, boolean offline) {
        PersistentUserSessionAdapter adapter = new PersistentUserSessionAdapter(userSession);
        PersistentUserSessionModel model = adapter.getUpdatedModel();

        PersistentUserSessionEntity entity = new PersistentUserSessionEntity();
        entity.setUserSessionId(model.getUserSessionId());
        entity.setCreatedOn(model.getStarted());
        entity.setRealmId(adapter.getRealm().getId());
        entity.setUserId(adapter.getUser().getId());
        String offlineStr = offlineToString(offline);
        entity.setOffline(offlineStr);
        entity.setLastSessionRefresh(model.getLastSessionRefresh());
        entity.setData(model.getData());
        entity.setBrokerSessionId(userSession.getBrokerSessionId());
        entity.setRememberMe(userSession.isRememberMe());
        em.persist(entity);
    }

    @Override
    public void createClientSession(AuthenticatedClientSessionModel clientSession, boolean offline) {
        PersistentAuthenticatedClientSessionAdapter adapter = new PersistentAuthenticatedClientSessionAdapter(session, clientSession);
        PersistentClientSessionModel model = adapter.getUpdatedModel();

        String userSessionId = clientSession.getUserSession().getId();
        String clientId;
        String clientStorageProvider;
        String externalClientId;
        StorageId clientStorageId = new StorageId(clientSession.getClient().getId());
        if (clientStorageId.isLocal()) {
            clientId = clientStorageId.getId();
            clientStorageProvider = PersistentClientSessionEntity.LOCAL;
            externalClientId = PersistentClientSessionEntity.LOCAL;
        } else {
            clientId = PersistentClientSessionEntity.EXTERNAL;
            clientStorageProvider = clientStorageId.getProviderId();
            externalClientId = clientStorageId.getExternalId();
        }
        String offlineStr = offlineToString(offline);
        boolean exists = false;

        PersistentClientSessionEntity entity = em.find(PersistentClientSessionEntity.class, new PersistentClientSessionEntity.Key(userSessionId, clientId, clientStorageProvider, externalClientId, offlineStr));
        if (entity != null) {
            // client session can already exist in some circumstances (EG. in case it was already present, but expired in the infinispan, but not yet expired in the DB)
            exists = true;
        } else {
            entity = new PersistentClientSessionEntity();
            entity.setUserSessionId(userSessionId);
            entity.setClientId(clientId);
            entity.setClientStorageProvider(clientStorageProvider);
            entity.setExternalClientId(externalClientId);
            entity.setOffline(offlineStr);
        }

        entity.setTimestamp(clientSession.getTimestamp());
        entity.setData(model.getData());

        if (!exists) {
            em.persist(entity);
        }
    }

    @Override
    public void removeUserSession(String userSessionId, boolean offline) {
        String offlineStr = offlineToString(offline);

        em.createNamedQuery("deleteClientSessionsByUserSession")
                .setParameter("userSessionId", userSessionId)
                .setParameter("offline", offlineStr)
                .executeUpdate();

        removeUserSessionFromDatabase(userSessionId, offlineStr);
    }

    @Override
    public void removeClientSession(String userSessionId, String clientUUID, boolean offline) {
        String offlineStr = offlineToString(offline);
        StorageId clientStorageId = new StorageId(clientUUID);
        String clientId = PersistentClientSessionEntity.EXTERNAL;
        String clientStorageProvider = PersistentClientSessionEntity.LOCAL;
        String externalId = PersistentClientSessionEntity.LOCAL;
        if (clientStorageId.isLocal()) {
            clientId = clientUUID;
        } else {
            clientStorageProvider = clientStorageId.getProviderId();
            externalId = clientStorageId.getExternalId();

        }
        PersistentClientSessionEntity sessionEntity = em.find(PersistentClientSessionEntity.class, new PersistentClientSessionEntity.Key(userSessionId, clientId, clientStorageProvider, externalId, offlineStr), LockModeType.PESSIMISTIC_WRITE);
        if (sessionEntity != null) {
            em.remove(sessionEntity);

            if (offline) {
                // Remove userSession if it was last clientSession
                if (hasNoClientSessions(userSessionId, offlineStr)) {
                    removeUserSessionFromDatabase(userSessionId, offlineStr);
                }
            }
        }
    }

    private void removeUserSessionFromDatabase(String userSessionId, String offlineStr) {
        PersistentUserSessionEntity userSessionEntity = em.find(PersistentUserSessionEntity.class, new PersistentUserSessionEntity.Key(userSessionId, offlineStr), LockModeType.PESSIMISTIC_WRITE);
        if (userSessionEntity != null) {
            em.remove(userSessionEntity);
        }
    }

    private boolean hasNoClientSessions(String userSessionId, String offline) {
        TypedQuery<PersistentClientSessionEntity> query = em.createNamedQuery("findClientSessionsByUserSession", PersistentClientSessionEntity.class);
        query.setParameter("userSessionId", userSessionId);
        query.setParameter("offline", offline);
        query.setMaxResults(1);
        return query.getSingleResultOrNull() == null;
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        this.removeUserSessions(realm);
    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {
        onClientRemoved(client.getId());
    }

    /**
     * Remove client sessions for a specific client.
     * <p>
     * We need to remove the client sessions for clients that are no longer present, as we would otherwise
     * look up those non-existent clients again and again, and this would be slow as they would never be in the cache.
     */
    private void onClientRemoved(String clientUUID) {
        logger.debugf("Client sessions removed for client %s",  clientUUID);
        StorageId clientStorageId = new StorageId(clientUUID);
        if (clientStorageId.isLocal()) {
            em.createNamedQuery("deleteClientSessionsByClient").setParameter("clientId", clientUUID).executeUpdate();
        } else {
            em.createNamedQuery("deleteClientSessionsByExternalClient")
                    .setParameter("clientStorageProvider", clientStorageId.getProviderId())
                    .setParameter("externalClientId", clientStorageId.getExternalId())
                    .executeUpdate();
        }
    }

    @Override
    public void onUserRemoved(RealmModel realm, UserModel user) {
        var userId = user.getId();
        em.createNamedQuery("deleteClientSessionsByUser").setParameter("userId", userId).executeUpdate();
        em.createNamedQuery("deleteUserSessionsByUser").setParameter("userId", userId).executeUpdate();
    }

    @Override
    public void updateLastSessionRefreshes(RealmModel realm, int lastSessionRefresh, Collection<String> userSessionIds, boolean offline) {
        String offlineStr = offlineToString(offline);

        int us = em.createNamedQuery("updateUserSessionLastSessionRefresh")
                .setParameter("lastSessionRefresh", lastSessionRefresh)
                .setParameter("realmId", realm.getId())
                .setParameter("offline", offlineStr)
                .setParameter("userSessionIds", userSessionIds)
                .executeUpdate();

        logger.debugf("Updated lastSessionRefresh of %d user sessions in realm '%s'", us, realm.getName());
    }

    @Override
    public void removeExpired(RealmModel realm) {
        final RealmExpiration expiration = RealmExpiration.fromRealm(realm);
        final int currentTime = Time.currentTime();
        final KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();

        UserSessionExpirationLogic.expireOfflineSessions(sessionFactory, realm, currentTime, expiration, expirationBatch);

        if (!MultiSiteUtils.isPersistentSessionsEnabled()) {
            return;
        }

        // The offline sessions do not have remember_me flag. We do not waste time migrating them.
        UserSessionExpirationLogic.migrateRememberMe(sessionFactory, realm, expiration, currentTime, expirationBatch);

        UserSessionExpirationLogic.expireRegularSessions(sessionFactory, realm, currentTime, expiration, false, expirationBatch);
        if (realm.isRememberMe()) {
            UserSessionExpirationLogic.expireRegularSessions(sessionFactory, realm, currentTime, expiration, true, expirationBatch);
        } else {
            UserSessionExpirationLogic.deleteInvalidSessions(sessionFactory, realm);
        }
    }

    private static int calculateOldestSessionTime(RealmModel realm, boolean offline) {
        return Time.currentTime() - (int) TimeUnit.MILLISECONDS.toSeconds(SessionExpirationUtils.calculateUserSessionIdleTimestamp(offline, realm.isRememberMe(), 0, realm));
    }

    @Override
    public Map<String, Long> getUserSessionsCountsByClients(RealmModel realm, boolean offline) {

        String offlineStr = offlineToString(offline);

        TypedQuery<Object[]> query = em.createNamedQuery("findClientSessionsClientIds", Object[].class);

        query.setParameter("offline", offlineStr);
        query.setParameter("realmId", realm.getId());
        query.setParameter("lastSessionRefresh", calculateOldestSessionTime(realm, offline));

        return closing(query.getResultStream())
                .collect(Collectors.toMap(row -> {
                    String clientId = row[0].toString();
                    if (clientId.equals(PersistentClientSessionEntity.EXTERNAL)) {
                        final String externalClientId = row[1].toString();
                        final String clientStorageProvider = row[2].toString();
                        clientId = new StorageId(clientStorageProvider, externalClientId).getId();
                    }
                    return clientId;
                }, row -> (Long) row[3]));
    }

    @Override
    public UserSessionModel loadUserSession(RealmModel realm, String userSessionId, boolean offline) {

        String offlineStr = offlineToString(offline);

        TypedQuery<PersistentUserSessionEntity> userSessionQuery = em.createNamedQuery("findUserSession", PersistentUserSessionEntity.class)
                .setParameter("realmId", realm.getId())
                .setParameter("offline", offlineStr)
                .setParameter("userSessionId", userSessionId)
                .setParameter("lastSessionRefresh", calculateOldestSessionTime(realm, offline))
                .setMaxResults(1);

        return handleSingleQuery(userSessionQuery, offlineStr);
    }

    @Override
    public UserSessionModel loadUserSessionsStreamByBrokerSessionId(RealmModel realm, String brokerSessionId, boolean offline) {
        var offlineStr = offlineToString(offline);
        TypedQuery<PersistentUserSessionEntity> userSessionQuery = em.createNamedQuery("findUserSessionsByBrokerSessionId", PersistentUserSessionEntity.class);
        userSessionQuery.setParameter("realmId", realm.getId());
        userSessionQuery.setParameter("brokerSessionId", brokerSessionId);
        userSessionQuery.setParameter("offline", offlineStr);
        userSessionQuery.setParameter("lastSessionRefresh", calculateOldestSessionTime(realm, offline));
        userSessionQuery.setMaxResults(1);

        return handleSingleQuery(userSessionQuery, offlineStr);
    }

    private UserSessionModel handleSingleQuery(TypedQuery<PersistentUserSessionEntity> query, String offlineStr) {
        var entity = query.getSingleResultOrNull();
        if (entity == null) {
            return null;
        }

        var userSession = toAdapter(entity);
        if (userSession == null) {
            return null;
        }

        var initializer = new ClientSessionLoader(() -> fetchClientSessions(userSession, offlineStr));
        userSession.setClientSessionsLoader(initializer);

        return userSession;
    }

    @Override
    public Stream<UserSessionModel> loadUserSessionsStream(RealmModel realm, ClientModel client, boolean offline, Integer firstResult, Integer maxResults) {
        String offlineStr = offlineToString(offline);
        TypedQuery<PersistentUserSessionEntity> query;
        StorageId clientStorageId = new StorageId(client.getId());
        if (clientStorageId.isLocal()) {
            query = paginateQuery(
                    em.createNamedQuery("findUserSessionsByClientId", PersistentUserSessionEntity.class),
                    firstResult, maxResults);
            query.setParameter("clientId", client.getId());
            query.setParameter("lastSessionRefresh", calculateOldestSessionTime(realm, offline));
        } else {
            query = paginateQuery(
                    em.createNamedQuery("findUserSessionsByExternalClientId", PersistentUserSessionEntity.class),
                    firstResult, maxResults);
            query.setParameter("clientStorageProvider", clientStorageId.getProviderId());
            query.setParameter("externalClientId", clientStorageId.getExternalId());
            query.setParameter("lastSessionRefresh", calculateOldestSessionTime(realm, offline));
        }

        query.setParameter("offline", offlineStr);
        query.setParameter("realmId", realm.getId());

        return loadExactUserSessionsWithClientSessions(query, offlineStr);
    }

    @Override
    public Stream<UserSessionModel> loadUserSessionsStream(RealmModel realm, UserModel user, boolean offline, Integer firstResult, Integer maxResults) {

        String offlineStr = offlineToString(offline);

        TypedQuery<PersistentUserSessionEntity> query = paginateQuery(
                em.createNamedQuery("findUserSessionsByUserId", PersistentUserSessionEntity.class),
                firstResult, maxResults);

        query.setParameter("offline", offlineStr);
        query.setParameter("realmId", realm.getId());
        query.setParameter("userId", user.getId());
        query.setParameter("lastSessionRefresh", calculateOldestSessionTime(realm, offline));

        return loadExactUserSessionsWithClientSessions(query, offlineStr);
    }

    public Stream<UserSessionModel> loadUserSessionsStream(Integer firstResult, Integer maxResults, boolean offline,
                                                           String lastUserSessionId) {
        String offlineStr = offlineToString(offline);

        TypedQuery<PersistentUserSessionEntity> query = paginateQuery(em.createNamedQuery("findUserSessionsOrderedById", PersistentUserSessionEntity.class)
            .setParameter("offline", offlineStr)
            .setParameter("lastSessionId", lastUserSessionId), firstResult, maxResults);

        return loadUserSessionsWithClientSessions(query, offlineStr);
    }

    @Override
    public AuthenticatedClientSessionModel loadClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession, boolean offline) {
        TypedQuery<PersistentClientSessionEntity> query;
        StorageId clientStorageId = new StorageId(client.getId());
        if (clientStorageId.isLocal()) {
            query = em.createNamedQuery("findClientSessionsByUserSessionAndClient", PersistentClientSessionEntity.class);
            query.setParameter("clientId", client.getId());
        } else {
            query = em.createNamedQuery("findClientSessionsByUserSessionAndExternalClient", PersistentClientSessionEntity.class);
            query.setParameter("clientStorageProvider", clientStorageId.getProviderId());
            query.setParameter("externalClientId", clientStorageId.getExternalId());
        }

        String offlineStr = offlineToString(offline);
        query.setParameter("userSessionId", userSession.getId());
        query.setParameter("offline", offlineStr);
        query.setMaxResults(1);

        return closing(query.getResultStream())
                .map(entity -> toAdapter(realm, client, userSession, entity))
                .findFirst()
                .orElse(null);
    }

    /**
     * Only client sessions from the user sessions obtained from the {@code query} are loaded.
     */
    private Stream<UserSessionModel> loadExactUserSessionsWithClientSessions(TypedQuery<PersistentUserSessionEntity> query, String offlineStr) {
        // Take the results returned by the database in chunks and enrich them.
        // The chunking avoids loading all the entries, as the caller usually adds pagination for the frontend.
        return closing(StreamsUtil.chunkedStream(closing(query.getResultStream()).map(this::toAdapter).filter(Objects::nonNull), 100)
                .flatMap(batchedUserSessions -> {
                    Set<String> removedClientUUIDs = new HashSet<>();

                    Map<String, OfflineUserSessionModel> sessionsById = batchedUserSessions.stream()
                            .collect(Collectors.toMap(UserSessionModel::getId, Function.identity()));

                    Set<String> userSessionIds = sessionsById.keySet();

                    TypedQuery<PersistentClientSessionEntity> queryClientSessions;
                    queryClientSessions = em.createNamedQuery("findClientSessionsOrderedByIdExact", PersistentClientSessionEntity.class);
                    queryClientSessions.setParameter("offline", offlineStr);
                    queryClientSessions.setParameter("userSessionIds", userSessionIds);

                    processClientSessions(sessionsById, removedClientUUIDs, queryClientSessions);

                    removedClientUUIDs.forEach(this::onClientRemoved);

                    logger.tracef("Loaded %d batch of user sessions (offline=%s, sessionIds=%s)", batchedUserSessions.size(), offlineStr, sessionsById.keySet());

                    return batchedUserSessions.stream();
                }).map(UserSessionModel.class::cast));
    }

    /**
     * The IDs of user sessions returned by the query is taken as limits, and all client sessions are loaded that belong
     * to user sessions whose ID is in between the minimum and maximum ID from this result.
     */
    private Stream<UserSessionModel> loadUserSessionsWithClientSessions(TypedQuery<PersistentUserSessionEntity> query, String offlineStr) {
        List<PersistentUserSessionAdapter> userSessionAdapters = closing(query.getResultStream()
                .map(this::toAdapter)
                .filter(Objects::nonNull))
                .toList();

        if (userSessionAdapters.isEmpty()) {
            return Stream.of();
        }

        Map<String, OfflineUserSessionModel> sessionsById = userSessionAdapters.stream()
                .collect(Collectors.toMap(UserSessionModel::getId, Function.identity()));

        Set<String> removedClientUUIDs = new HashSet<>();

        TypedQuery<PersistentClientSessionEntity> queryClientSessions;
        String fromUserSessionId = userSessionAdapters.get(0).getId();
        String toUserSessionId = userSessionAdapters.get(userSessionAdapters.size() - 1).getId();

        queryClientSessions = em.createNamedQuery("findClientSessionsOrderedByIdInterval", PersistentClientSessionEntity.class);
        queryClientSessions.setParameter("offline", offlineStr);
        queryClientSessions.setParameter("fromSessionId", fromUserSessionId);
        queryClientSessions.setParameter("toSessionId", toUserSessionId);

        processClientSessions(sessionsById, removedClientUUIDs, queryClientSessions);

        removedClientUUIDs.forEach(this::onClientRemoved);

        logger.tracef("Loaded %d user sessions (offline=%s, sessionIds=%s)", userSessionAdapters.size(), offlineStr, sessionsById.keySet());

        return userSessionAdapters.stream().map(UserSessionModel.class::cast);
    }

    private void processClientSessions(Map<String, OfflineUserSessionModel> sessionsById, Set<String> removedClientUUIDs, TypedQuery<PersistentClientSessionEntity> queryClientSessions) {
        closing(queryClientSessions.getResultStream()).forEach(clientSession -> {
            OfflineUserSessionModel userSession = sessionsById.get(clientSession.getUserSessionId());
            // check if we have a user session for the client session
            if (userSession != null) {
                boolean added = addClientSessionToAuthenticatedClientSessionsIfPresent(userSession, clientSession);
                if (!added) {
                    // client was removed in the meantime
                    removedClientUUIDs.add(clientSession.getClientId());
                }
            }
        });
    }

    private boolean addClientSessionToAuthenticatedClientSessionsIfPresent(UserSessionModel userSession, PersistentClientSessionEntity clientSessionEntity) {

        AuthenticatedClientSessionModel clientSessAdapter = toAdapter(userSession.getRealm(), null, userSession, clientSessionEntity);

        if (clientSessAdapter.getClient() == null) {
            logger.debugf("Not adding client session %s / %s since client is null", userSession, clientSessAdapter);
            return false;
        }

        logger.tracef("Adding client session %s / %s", userSession, clientSessAdapter);

        userSession.getAuthenticatedClientSessions().put(JpaSessionUtil.getClientId(clientSessionEntity), clientSessAdapter);
        return true;
    }

    private PersistentUserSessionAdapter toAdapter(PersistentUserSessionEntity entity) {
        RealmModel realm = session.realms().getRealm(entity.getRealmId());
        if (realm == null) {    // Realm has been deleted concurrently, ignore the entity
            return null;
        }
        return toAdapter(realm, entity);
    }

    private PersistentUserSessionAdapter toAdapter(RealmModel realm, PersistentUserSessionEntity entity) {
        PersistentUserSessionModel model = new PersistentUserSessionModel() {
            @Override
            public String getUserSessionId() {
                return entity.getUserSessionId();
            }

            @Override
            public void setUserSessionId(String userSessionId) {
                entity.setUserSessionId(userSessionId);
            }

            @Override
            public int getStarted() {
                return entity.getCreatedOn();
            }

            @Override
            public void setStarted(int started) {
                entity.setCreatedOn(started);
            }

            @Override
            public int getLastSessionRefresh() {
                return entity.getLastSessionRefresh();
            }

            @Override
            public void setLastSessionRefresh(int lastSessionRefresh) {
                entity.setLastSessionRefresh(lastSessionRefresh);
            }

            @Override
            public boolean isOffline() {
                return offlineFromString(entity.getOffline());
            }

            @Override
            public void setOffline(boolean offline) {
                entity.setOffline(offlineToString(offline));
            }

            @Override
            public String getData() {
                return entity.getData();
            }

            @Override
            public void setData(String data) {
                entity.setData(data);
            }

            @Override
            public void setRealmId(String realmId) {
                entity.setRealmId(realmId);
            }

            @Override
            public void setUserId(String userId) {
                entity.setUserId(userId);
            }

            @Override
            public void setBrokerSessionId(String brokerSessionId) {
                entity.setBrokerSessionId(brokerSessionId);
            }

            @Override
            public boolean isRememberMe() {
                return entity.isRememberMe();
            }

            @Override
            public void setRememberMe(boolean rememberMe) {
                entity.setRememberMe(rememberMe);
            }
        };

        Map<String, AuthenticatedClientSessionModel> clientSessions = new HashMap<>();
        return new PersistentUserSessionAdapter(session, model, realm, entity.getUserId(), clientSessions);
    }

    private PersistentAuthenticatedClientSessionAdapter toAdapter(RealmModel realm, ClientModel client, UserSessionModel userSession, PersistentClientSessionEntity entity) {
        if (client == null) {
            // can be null if client is not found anymore
            client = realm.getClientById(JpaSessionUtil.getClientId(entity));
            if (client == null) {
                logger.debugf("Client not found for clientId %s clientStorageProvider %s externalClientId %s",
                        entity.getClientId(), entity.getClientStorageProvider(), entity.getExternalClientId());
            }
        }

        PersistentClientSessionModel model = new PersistentClientSessionModel() {
            @Override
            public String getUserSessionId() {
                return entity.getUserSessionId();
            }

            @Override
            public void setUserSessionId(String userSessionId) {
                entity.setUserSessionId(userSessionId);
            }

            @Override
            public String getClientId() {
                return JpaSessionUtil.getClientId(entity);
            }

            @Override
            public void setClientId(String clientId) {
                throw new IllegalStateException("forbidden");
            }

            @Override
            public int getTimestamp() {
                return entity.getTimestamp();
            }

            @Override
            public void setTimestamp(int timestamp) {
                entity.setTimestamp(timestamp);
            }

            @Override
            public String getData() {
                return entity.getData();
            }

            @Override
            public void setData(String data) {
                entity.setData(data);
            }
        };
        return new PersistentAuthenticatedClientSessionAdapter(session, model, realm, client, userSession);
    }

    @Override
    public int getUserSessionsCount(boolean offline) {
        String offlineStr = offlineToString(offline);

        Query query = em.createNamedQuery("findUserSessionsCount");
        query.setParameter("offline", offlineStr);
        Number n = (Number) query.getSingleResult();
        return n.intValue();
    }

    @Override
    public int getUserSessionsCount(RealmModel realm, ClientModel clientModel, boolean offline) {

        String offlineStr = offlineToString(offline);
        Query query;
        StorageId clientStorageId = new StorageId(clientModel.getId());
        if (clientStorageId.isLocal()) {
            query = em.createNamedQuery("findClientSessionsCountByClient");
            query.setParameter("clientId", clientModel.getId());
        } else {
            query = em.createNamedQuery("findClientSessionsCountByExternalClient");
            query.setParameter("clientStorageProvider", clientStorageId.getProviderId());
            query.setParameter("externalClientId", clientStorageId.getExternalId());
        }

        // Note, that realm is unused here, since the clientModel id already determines the offline user-sessions bound to an owning realm.
        query.setParameter("offline", offlineStr);

        Number n = (Number) query.getSingleResult();
        return n.intValue();
    }

    @Override
    public void removeUserSessions(RealmModel realm, boolean offline) {
        em.createNamedQuery("deleteClientSessionsByRealmSessionType")
                .setParameter("realmId", realm.getId())
                .setParameter("offline", offlineToString(offline))
                .executeUpdate();
        em.createNamedQuery("deleteUserSessionsByRealmSessionType")
                .setParameter("realmId", realm.getId())
                .setParameter("offline", offlineToString(offline))
                .executeUpdate();
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        em.createNamedQuery("deleteClientSessionsByRealm")
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteUserSessionsByRealm")
                .setParameter("realmId", realm.getId())
                .executeUpdate();
    }

    @Override
    public void close() {
        // NOOP
    }

    private Stream<PersistentAuthenticatedClientSessionAdapter> fetchClientSessions(PersistentUserSessionAdapter userSession, String offlineStr) {
        TypedQuery<PersistentClientSessionEntity> clientSessionQuery = em.createNamedQuery("findClientSessionsByUserSession", PersistentClientSessionEntity.class);
        clientSessionQuery.setParameter("userSessionId", userSession.getId());
        clientSessionQuery.setParameter("offline", offlineStr);

        return closing(clientSessionQuery.getResultStream()
                .map(entity -> toAdapter(userSession.getRealm(), null, userSession, entity))
                .filter(JpaSessionUtil::hasClient)
        );
    }
}
