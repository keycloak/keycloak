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

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.PersistentAuthenticatedClientSessionAdapter;
import org.keycloak.models.session.PersistentClientSessionModel;
import org.keycloak.models.session.PersistentUserSessionAdapter;
import org.keycloak.models.session.PersistentUserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.SessionTimeoutHelper;
import org.keycloak.storage.StorageId;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.LockModeType;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JpaUserSessionPersisterProvider implements UserSessionPersisterProvider {
    private static final Logger logger = Logger.getLogger(JpaUserSessionPersisterProvider.class);

    private final KeycloakSession session;
    private final EntityManager em;

    public JpaUserSessionPersisterProvider(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
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
        em.persist(entity);
        em.flush();
    }

    @Override
    public void createClientSession(AuthenticatedClientSessionModel clientSession, boolean offline) {
        PersistentAuthenticatedClientSessionAdapter adapter = new PersistentAuthenticatedClientSessionAdapter(session, clientSession);
        PersistentClientSessionModel model = adapter.getUpdatedModel();

        PersistentClientSessionEntity entity = new PersistentClientSessionEntity();
        StorageId clientStorageId = new StorageId(clientSession.getClient().getId());
        if (clientStorageId.isLocal()) {
            entity.setClientId(clientStorageId.getId());
            entity.setClientStorageProvider(PersistentClientSessionEntity.LOCAL);
            entity.setExternalClientId(PersistentClientSessionEntity.LOCAL);

        } else {
            entity.setClientId(PersistentClientSessionEntity.EXTERNAL);
            entity.setClientStorageProvider(clientStorageId.getProviderId());
            entity.setExternalClientId(clientStorageId.getExternalId());
        }
        entity.setTimestamp(clientSession.getTimestamp());
        String offlineStr = offlineToString(offline);
        entity.setOffline(offlineStr);
        entity.setUserSessionId(clientSession.getUserSession().getId());
        entity.setData(model.getData());
        em.persist(entity);
        em.flush();
    }

    @Override
    public void removeUserSession(String userSessionId, boolean offline) {
        String offlineStr = offlineToString(offline);

        em.createNamedQuery("deleteClientSessionsByUserSession")
                .setParameter("userSessionId", userSessionId)
                .setParameter("offline", offlineStr)
                .executeUpdate();

        PersistentUserSessionEntity sessionEntity = em.find(PersistentUserSessionEntity.class, new PersistentUserSessionEntity.Key(userSessionId, offlineStr), LockModeType.PESSIMISTIC_WRITE);
        if (sessionEntity != null) {
            em.remove(sessionEntity);
            em.flush();
        }
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

            // Remove userSession if it was last clientSession
            List<PersistentClientSessionEntity> clientSessions = getClientSessionsByUserSession(sessionEntity.getUserSessionId(), offline);
            if (clientSessions.size() == 0) {
                offlineStr = offlineToString(offline);
                PersistentUserSessionEntity userSessionEntity = em.find(PersistentUserSessionEntity.class, new PersistentUserSessionEntity.Key(sessionEntity.getUserSessionId(), offlineStr), LockModeType.PESSIMISTIC_WRITE);
                if (userSessionEntity != null) {
                    em.remove(userSessionEntity);
                }
            }

            em.flush();
        }
    }

    private List<PersistentClientSessionEntity> getClientSessionsByUserSession(String userSessionId, boolean offline) {
        String offlineStr = offlineToString(offline);

        TypedQuery<PersistentClientSessionEntity> query = em.createNamedQuery("findClientSessionsByUserSession", PersistentClientSessionEntity.class);
        query.setParameter("userSessionId", userSessionId);
        query.setParameter("offline", offlineStr);
        return query.getResultList();
    }



    @Override
    public void onRealmRemoved(RealmModel realm) {
        int deletedClientSessions = em.createNamedQuery("deleteClientSessionsByRealm")
                .setParameter("realmId", realm.getId())
                .executeUpdate();

        int deletedUserSessions = em.createNamedQuery("deleteUserSessionsByRealm")
                .setParameter("realmId", realm.getId())
                .executeUpdate();
    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {
        onClientRemoved(client.getId());
    }

    private void onClientRemoved(String clientUUID) {
        int num = 0;
        StorageId clientStorageId = new StorageId(clientUUID);
        if (clientStorageId.isLocal()) {
            num = em.createNamedQuery("deleteClientSessionsByClient").setParameter("clientId", clientUUID).executeUpdate();
        } else {
            num = em.createNamedQuery("deleteClientSessionsByExternalClient")
                    .setParameter("clientStorageProvider", clientStorageId.getProviderId())
                    .setParameter("externalClientId", clientStorageId.getExternalId())
                    .executeUpdate();
        }
    }

    @Override
    public void onUserRemoved(RealmModel realm, UserModel user) {
        onUserRemoved(realm, user.getId());
    }

    private void onUserRemoved(RealmModel realm, String userId) {
        int num = em.createNamedQuery("deleteClientSessionsByUser").setParameter("userId", userId).executeUpdate();
        num = em.createNamedQuery("deleteUserSessionsByUser").setParameter("userId", userId).executeUpdate();
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
        int expiredOffline = Time.currentTime() - realm.getOfflineSessionIdleTimeout() - SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS;

        // prefer client session timeout if set
        int expiredClientOffline = expiredOffline;
        if (realm.getClientOfflineSessionIdleTimeout() > 0) {
            expiredClientOffline = Time.currentTime() - realm.getClientOfflineSessionIdleTimeout() - SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS;
        }

        String offlineStr = offlineToString(true);

        logger.tracef("Trigger removing expired user sessions for realm '%s'", realm.getName());

        int cs = em.createNamedQuery("deleteExpiredClientSessions")
                .setParameter("realmId", realm.getId())
                .setParameter("lastSessionRefresh", expiredClientOffline)
                .setParameter("offline", offlineStr)
                .executeUpdate();

        int us = em.createNamedQuery("deleteExpiredUserSessions")
                .setParameter("realmId", realm.getId())
                .setParameter("lastSessionRefresh", expiredOffline)
                .setParameter("offline", offlineStr)
                .executeUpdate();

        logger.debugf("Removed %d expired user sessions and %d expired client sessions in realm '%s'", us, cs, realm.getName());

    }

    @Override
    public Map<String, Long> getUserSessionsCountsByClients(RealmModel realm, boolean offline) {

        String offlineStr = offlineToString(offline);

        TypedQuery<Object[]> query = em.createNamedQuery("findClientSessionsClientIds", Object[].class);

        query.setParameter("offline", offlineStr);
        query.setParameter("realmId", realm.getId());

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

        TypedQuery<PersistentUserSessionEntity> userSessionQuery = em.createNamedQuery("findUserSession", PersistentUserSessionEntity.class);
        userSessionQuery.setParameter("realmId", realm.getId());
        userSessionQuery.setParameter("offline", offlineStr);
        userSessionQuery.setParameter("userSessionId", userSessionId);
        userSessionQuery.setMaxResults(1);

        Stream<PersistentUserSessionAdapter> persistentUserSessions = closing(userSessionQuery.getResultStream().map(this::toAdapter));

        return persistentUserSessions.findAny().map(userSession -> {

            TypedQuery<PersistentClientSessionEntity> clientSessionQuery = em.createNamedQuery("findClientSessionsByUserSession", PersistentClientSessionEntity.class);
            clientSessionQuery.setParameter("userSessionId", Collections.singleton(userSessionId));
            clientSessionQuery.setParameter("offline", offlineStr);

            Set<String> removedClientUUIDs = new HashSet<>();

            closing(clientSessionQuery.getResultStream()).forEach(clientSession -> {
                        boolean added = addClientSessionToAuthenticatedClientSessionsIfPresent(userSession, clientSession);
                        if (!added) {
                            // client was removed in the meantime
                            removedClientUUIDs.add(clientSession.getClientId());
                        }
                    }
            );

            removedClientUUIDs.forEach(this::onClientRemoved);

            return userSession;
        }).orElse(null);
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
        } else {
            query = paginateQuery(
                    em.createNamedQuery("findUserSessionsByExternalClientId", PersistentUserSessionEntity.class),
                    firstResult, maxResults);
            query.setParameter("clientStorageProvider", clientStorageId.getProviderId());
            query.setParameter("externalClientId", clientStorageId.getExternalId());
        }

        query.setParameter("offline", offlineStr);
        query.setParameter("realmId", realm.getId());

        return loadUserSessionsWithClientSessions(query, offlineStr);
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

        return loadUserSessionsWithClientSessions(query, offlineStr);
    }

    public Stream<UserSessionModel> loadUserSessionsStream(Integer firstResult, Integer maxResults, boolean offline,
                                                           String lastUserSessionId) {
        String offlineStr = offlineToString(offline);

        TypedQuery<PersistentUserSessionEntity> query = paginateQuery(em.createNamedQuery("findUserSessionsOrderedById", PersistentUserSessionEntity.class)
            .setParameter("offline", offlineStr)
            .setParameter("lastSessionId", lastUserSessionId), firstResult, maxResults);

        return loadUserSessionsWithClientSessions(query, offlineStr);
    }

    private Stream<UserSessionModel> loadUserSessionsWithClientSessions(TypedQuery<PersistentUserSessionEntity> query, String offlineStr) {

        List<PersistentUserSessionAdapter> userSessionAdapters = closing(query.getResultStream()
                .map(this::toAdapter)
                .filter(Objects::nonNull))
                .collect(Collectors.toList());

        Map<String, PersistentUserSessionAdapter> sessionsById = userSessionAdapters.stream()
                .collect(Collectors.toMap(UserSessionModel::getId, Function.identity()));

        Set<String> removedClientUUIDs = new HashSet<>();

        if (!sessionsById.isEmpty()) {
            String fromUserSessionId = userSessionAdapters.get(0).getId();
            String toUserSessionId = userSessionAdapters.get(userSessionAdapters.size() - 1).getId();

            TypedQuery<PersistentClientSessionEntity> queryClientSessions = em.createNamedQuery("findClientSessionsOrderedById", PersistentClientSessionEntity.class);
            queryClientSessions.setParameter("offline", offlineStr);
            queryClientSessions.setParameter("fromSessionId", fromUserSessionId);
            queryClientSessions.setParameter("toSessionId", toUserSessionId);

            closing(queryClientSessions.getResultStream()).forEach(clientSession -> {
                PersistentUserSessionAdapter userSession = sessionsById.get(clientSession.getUserSessionId());
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

        for (String clientUUID : removedClientUUIDs) {
            onClientRemoved(clientUUID);
        }

        return userSessionAdapters.stream().map(UserSessionModel.class::cast);
    }

    private boolean addClientSessionToAuthenticatedClientSessionsIfPresent(PersistentUserSessionAdapter userSession, PersistentClientSessionEntity clientSessionEntity) {

        PersistentAuthenticatedClientSessionAdapter clientSessAdapter = toAdapter(userSession.getRealm(), userSession, clientSessionEntity);

        if (clientSessAdapter.getClient() == null) {
            return false;
        }

        String clientId = clientSessionEntity.getClientId();
        if (isExternalClient(clientSessionEntity)) {
            clientId = getExternalClientId(clientSessionEntity);
        }

        userSession.getAuthenticatedClientSessions().put(clientId, clientSessAdapter);
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
        PersistentUserSessionModel model = new PersistentUserSessionModel();
        model.setUserSessionId(entity.getUserSessionId());
        model.setStarted(entity.getCreatedOn());
        model.setLastSessionRefresh(entity.getLastSessionRefresh());
        model.setData(entity.getData());
        model.setOffline(offlineFromString(entity.getOffline()));

        Map<String, AuthenticatedClientSessionModel> clientSessions = new HashMap<>();
        return new PersistentUserSessionAdapter(session, model, realm, entity.getUserId(), clientSessions);
    }

    private PersistentAuthenticatedClientSessionAdapter toAdapter(RealmModel realm, PersistentUserSessionAdapter userSession, PersistentClientSessionEntity entity) {
        String clientId = entity.getClientId();
        if (isExternalClient(entity)) {
            clientId = getExternalClientId(entity);
        }
        ClientModel client = realm.getClientById(clientId);

        PersistentClientSessionModel model = new PersistentClientSessionModel();
        model.setClientId(clientId);
        model.setUserSessionId(userSession.getId());
        model.setUserId(userSession.getUserId());
        model.setTimestamp(entity.getTimestamp());
        model.setData(entity.getData());
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
    public void close() {
        // NOOP
    }

    private String offlineToString(boolean offline) {
        return offline ? "1" : "0";
    }

    private boolean offlineFromString(String offlineStr) {
        return "1".equals(offlineStr);
    }

    private boolean isExternalClient(PersistentClientSessionEntity entity) {
        return !entity.getExternalClientId().equals(PersistentClientSessionEntity.LOCAL);
    }

    private String getExternalClientId(PersistentClientSessionEntity entity) {
        return new StorageId(entity.getClientStorageProvider(), entity.getExternalClientId()).getId();
    }
}
