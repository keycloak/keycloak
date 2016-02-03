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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.PersistentClientSessionAdapter;
import org.keycloak.models.session.PersistentClientSessionModel;
import org.keycloak.models.session.PersistentUserSessionAdapter;
import org.keycloak.models.session.PersistentUserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JpaUserSessionPersisterProvider implements UserSessionPersisterProvider {

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
    public void createClientSession(ClientSessionModel clientSession, boolean offline) {
        PersistentClientSessionAdapter adapter = new PersistentClientSessionAdapter(clientSession);
        PersistentClientSessionModel model = adapter.getUpdatedModel();

        PersistentClientSessionEntity entity = new PersistentClientSessionEntity();
        entity.setClientSessionId(clientSession.getId());
        entity.setClientId(clientSession.getClient().getId());
        entity.setTimestamp(clientSession.getTimestamp());
        String offlineStr = offlineToString(offline);
        entity.setOffline(offlineStr);
        entity.setUserSessionId(clientSession.getUserSession().getId());
        entity.setData(model.getData());
        em.persist(entity);
        em.flush();
    }

    @Override
    public void updateUserSession(UserSessionModel userSession, boolean offline) {
        PersistentUserSessionAdapter adapter;
        if (userSession instanceof PersistentUserSessionAdapter) {
            adapter = (PersistentUserSessionAdapter) userSession;
        } else {
            adapter = new PersistentUserSessionAdapter(userSession);
        }

        PersistentUserSessionModel model = adapter.getUpdatedModel();

        String offlineStr = offlineToString(offline);
        PersistentUserSessionEntity entity = em.find(PersistentUserSessionEntity.class, new PersistentUserSessionEntity.Key(userSession.getId(), offlineStr));
        if (entity == null) {
            throw new ModelException("UserSession with ID " + userSession.getId() + ", offline: " + offline + " not found");
        }
        entity.setLastSessionRefresh(model.getLastSessionRefresh());
        entity.setData(model.getData());
    }

    @Override
    public void removeUserSession(String userSessionId, boolean offline) {
        String offlineStr = offlineToString(offline);

        em.createNamedQuery("deleteClientSessionsByUserSession")
                .setParameter("userSessionId", userSessionId)
                .setParameter("offline", offlineStr)
                .executeUpdate();

        PersistentUserSessionEntity sessionEntity = em.find(PersistentUserSessionEntity.class, new PersistentUserSessionEntity.Key(userSessionId, offlineStr));
        if (sessionEntity != null) {
            em.remove(sessionEntity);
            em.flush();
        }
    }

    @Override
    public void removeClientSession(String clientSessionId, boolean offline) {
        String offlineStr = offlineToString(offline);
        PersistentClientSessionEntity sessionEntity = em.find(PersistentClientSessionEntity.class, new PersistentClientSessionEntity.Key(clientSessionId, offlineStr));
        if (sessionEntity != null) {
            em.remove(sessionEntity);

            // Remove userSession if it was last clientSession
            List<PersistentClientSessionEntity> clientSessions = getClientSessionsByUserSession(sessionEntity.getUserSessionId(), offline);
            if (clientSessions.size() == 0) {
                offlineStr = offlineToString(offline);
                PersistentUserSessionEntity userSessionEntity = em.find(PersistentUserSessionEntity.class, new PersistentUserSessionEntity.Key(sessionEntity.getUserSessionId(), offlineStr));
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
        int num = em.createNamedQuery("deleteClientSessionsByRealm").setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteUserSessionsByRealm").setParameter("realmId", realm.getId()).executeUpdate();
    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {
        int num = em.createNamedQuery("deleteClientSessionsByClient").setParameter("clientId", client.getId()).executeUpdate();
        num = em.createNamedQuery("deleteDetachedUserSessions").executeUpdate();
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
    public void clearDetachedUserSessions() {
        int num = em.createNamedQuery("deleteDetachedClientSessions").executeUpdate();
        num = em.createNamedQuery("deleteDetachedUserSessions").executeUpdate();
    }

    @Override
    public void updateAllTimestamps(int time) {
        int num = em.createNamedQuery("updateClientSessionsTimestamps").setParameter("timestamp", time).executeUpdate();
        num = em.createNamedQuery("updateUserSessionsTimestamps").setParameter("lastSessionRefresh", time).executeUpdate();
    }

    @Override
    public List<UserSessionModel> loadUserSessions(int firstResult, int maxResults, boolean offline) {
        String offlineStr = offlineToString(offline);

        TypedQuery<PersistentUserSessionEntity> query = em.createNamedQuery("findUserSessions", PersistentUserSessionEntity.class);
        query.setParameter("offline", offlineStr);

        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }

        List<PersistentUserSessionEntity> results = query.getResultList();
        List<UserSessionModel> result = new ArrayList<>();
        List<String> userSessionIds = new ArrayList<>();
        for (PersistentUserSessionEntity entity : results) {
            RealmModel realm = session.realms().getRealm(entity.getRealmId());
            UserModel user = session.users().getUserById(entity.getUserId(), realm);

            // Case when user was deleted in the meantime
            if (user == null) {
                onUserRemoved(realm, entity.getUserId());
                return loadUserSessions(firstResult, maxResults, offline);
            }

            result.add(toAdapter(realm, user, entity));
            userSessionIds.add(entity.getUserSessionId());
        }

        if (!userSessionIds.isEmpty()) {
            TypedQuery<PersistentClientSessionEntity> query2 = em.createNamedQuery("findClientSessionsByUserSessions", PersistentClientSessionEntity.class);
            query2.setParameter("userSessionIds", userSessionIds);
            query2.setParameter("offline", offlineStr);
            List<PersistentClientSessionEntity> clientSessions = query2.getResultList();

            // Assume both userSessions and clientSessions ordered by userSessionId
            int j = 0;
            for (UserSessionModel ss : result) {
                PersistentUserSessionAdapter userSession = (PersistentUserSessionAdapter) ss;
                List<ClientSessionModel> currentClientSessions = userSession.getClientSessions(); // This is empty now and we want to fill it

                boolean next = true;
                while (next && j < clientSessions.size()) {
                    PersistentClientSessionEntity clientSession = clientSessions.get(j);
                    if (clientSession.getUserSessionId().equals(userSession.getId())) {
                        PersistentClientSessionAdapter clientSessAdapter = toAdapter(userSession.getRealm(), userSession, clientSession);
                        currentClientSessions.add(clientSessAdapter);
                        j++;
                    } else {
                        next = false;
                    }
                }
            }
        }

        return result;
    }

    private PersistentUserSessionAdapter toAdapter(RealmModel realm, UserModel user, PersistentUserSessionEntity entity) {
        PersistentUserSessionModel model = new PersistentUserSessionModel();
        model.setUserSessionId(entity.getUserSessionId());
        model.setLastSessionRefresh(entity.getLastSessionRefresh());
        model.setData(entity.getData());

        List<ClientSessionModel> clientSessions = new LinkedList<>();
        return new PersistentUserSessionAdapter(model, realm, user, clientSessions);
    }

    private PersistentClientSessionAdapter toAdapter(RealmModel realm, PersistentUserSessionAdapter userSession, PersistentClientSessionEntity entity) {
        ClientModel client = realm.getClientById(entity.getClientId());

        PersistentClientSessionModel model = new PersistentClientSessionModel();
        model.setClientSessionId(entity.getClientSessionId());
        model.setClientId(entity.getClientId());
        model.setUserSessionId(userSession.getId());
        model.setUserId(userSession.getUser().getId());
        model.setTimestamp(entity.getTimestamp());
        model.setData(entity.getData());
        return new PersistentClientSessionAdapter(model, realm, client, userSession);
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
    public void close() {

    }

    private String offlineToString(boolean offline) {
        return offline ? "1" : "0";
    }
}
