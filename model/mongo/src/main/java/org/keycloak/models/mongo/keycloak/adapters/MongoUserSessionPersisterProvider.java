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

package org.keycloak.models.mongo.keycloak.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.entities.PersistentClientSessionEntity;
import org.keycloak.models.entities.PersistentUserSessionEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoOfflineUserSessionEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoOnlineUserSessionEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserSessionEntity;
import org.keycloak.models.session.PersistentClientSessionAdapter;
import org.keycloak.models.session.PersistentClientSessionModel;
import org.keycloak.models.session.PersistentUserSessionAdapter;
import org.keycloak.models.session.PersistentUserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoUserSessionPersisterProvider implements UserSessionPersisterProvider {

    private final MongoStoreInvocationContext invocationContext;
    private final KeycloakSession session;

    public MongoUserSessionPersisterProvider(KeycloakSession session, MongoStoreInvocationContext invocationContext) {
        this.session = session;
        this.invocationContext = invocationContext;
    }

    protected MongoStore getMongoStore() {
        return invocationContext.getMongoStore();
    }

    private MongoUserSessionEntity loadUserSession(String userSessionId, boolean offline) {
        Class<? extends MongoUserSessionEntity> clazz = offline ? MongoOfflineUserSessionEntity.class : MongoOnlineUserSessionEntity.class;
        return getMongoStore().loadEntity(clazz, userSessionId, invocationContext);
    }

    @Override
    public void createUserSession(UserSessionModel userSession, boolean offline) {
        PersistentUserSessionAdapter adapter = new PersistentUserSessionAdapter(userSession);
        PersistentUserSessionModel model = adapter.getUpdatedModel();

        MongoUserSessionEntity entity = offline ? new MongoOfflineUserSessionEntity() : new MongoOnlineUserSessionEntity();
        entity.setId(model.getUserSessionId());
        entity.setRealmId(adapter.getRealm().getId());
        entity.setUserId(adapter.getUser().getId());
        entity.setLastSessionRefresh(model.getLastSessionRefresh());
        entity.setData(model.getData());
        entity.setClientSessions(new ArrayList<PersistentClientSessionEntity>());
        getMongoStore().insertEntity(entity, invocationContext);
    }

    @Override
    public void createClientSession(ClientSessionModel clientSession, boolean offline) {
        PersistentClientSessionAdapter adapter = new PersistentClientSessionAdapter(clientSession);
        PersistentClientSessionModel model = adapter.getUpdatedModel();

        MongoUserSessionEntity userSession = loadUserSession(model.getUserSessionId(), offline);
        if (userSession == null) {
            throw new ModelException("Not userSession found with ID " + clientSession.getUserSession().getId() + ". Requested by clientSession: " + clientSession.getId());
        } else {
            PersistentClientSessionEntity entity = new PersistentClientSessionEntity();
            entity.setClientSessionId(clientSession.getId());
            entity.setClientId(clientSession.getClient().getId());
            entity.setData(model.getData());
            userSession.getClientSessions().add(entity);
            getMongoStore().updateEntity(userSession, invocationContext);
        }
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

        MongoUserSessionEntity entity = loadUserSession(model.getUserSessionId(), offline);
        if (entity == null) {
            throw new ModelException("UserSession with ID " + userSession.getId() + ", offline: " + offline + " not found");
        }
        entity.setLastSessionRefresh(model.getLastSessionRefresh());
        entity.setData(model.getData());

        getMongoStore().updateEntity(entity, invocationContext);
    }

    @Override
    public void removeUserSession(String userSessionId, boolean offline) {
        MongoUserSessionEntity entity = loadUserSession(userSessionId, offline);
        if (entity != null) {
            getMongoStore().removeEntity(entity, invocationContext);
        }
    }

    @Override
    public void removeClientSession(String clientSessionId, boolean offline) {
        DBObject query = new QueryBuilder()
                .and("clientSessions.clientSessionId").is(clientSessionId)
                .get();
        Class<? extends MongoUserSessionEntity> clazz = offline ? MongoOfflineUserSessionEntity.class : MongoOnlineUserSessionEntity.class;
        MongoUserSessionEntity userSession = getMongoStore().loadSingleEntity(clazz, query, invocationContext);
        if (userSession != null) {

            PersistentClientSessionEntity found = null;
            for (PersistentClientSessionEntity clientSession : userSession.getClientSessions()) {
                if (clientSession.getClientSessionId().equals(clientSessionId)) {
                    found = clientSession;
                    break;
                }
            }

            if (found != null) {
                userSession.getClientSessions().remove(found);

                // Remove userSession if it was last clientSession attached
                if (userSession.getClientSessions().size() == 0) {
                    getMongoStore().removeEntity(userSession, invocationContext);
                } else {
                    getMongoStore().updateEntity(userSession, invocationContext);
                }
            }
        }
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .get();
        getMongoStore().removeEntities(MongoOnlineUserSessionEntity.class, query, false, invocationContext);
        getMongoStore().removeEntities(MongoOfflineUserSessionEntity.class, query, false, invocationContext);
    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {
        DBObject query = new QueryBuilder()
                .and("clientSessions.clientId").is(client.getId())
                .get();

        List<MongoOnlineUserSessionEntity> userSessions = getMongoStore().loadEntities(MongoOnlineUserSessionEntity.class, query, invocationContext);
        for (MongoOnlineUserSessionEntity userSession : userSessions) {
            removeClientSessionOfClient(userSession, client.getId());
        }

        List<MongoOfflineUserSessionEntity> userSessions2 = getMongoStore().loadEntities(MongoOfflineUserSessionEntity.class, query, invocationContext);
        for (MongoOfflineUserSessionEntity userSession : userSessions2) {
            removeClientSessionOfClient(userSession, client.getId());
        }
    }

    private void removeClientSessionOfClient(MongoUserSessionEntity userSession, String clientId) {
        PersistentClientSessionEntity found = null;
        for (PersistentClientSessionEntity clientSession : userSession.getClientSessions()) {
            if (clientSession.getClientId().equals(clientId)) {
                found = clientSession;
                break;
            }
        }

        if (found != null) {
            userSession.getClientSessions().remove(found);

            // Remove userSession if it was last clientSession attached
            if (userSession.getClientSessions().size() == 0) {
                getMongoStore().removeEntity(userSession, invocationContext);
            } else {
                getMongoStore().updateEntity(userSession, invocationContext);
            }
        }
    }

    @Override
    public void onUserRemoved(RealmModel realm, UserModel user) {
        onUserRemoved(realm, user.getId());
    }

    private void onUserRemoved(RealmModel realm, String userId) {
        DBObject query = new QueryBuilder()
                .and("userId").is(userId)
                .get();
        getMongoStore().removeEntities(MongoOnlineUserSessionEntity.class, query, false, invocationContext);
        getMongoStore().removeEntities(MongoOfflineUserSessionEntity.class, query, false, invocationContext);
    }

    @Override
    public void clearDetachedUserSessions() {
        DBObject query = new QueryBuilder()
                .and("clientSessions").is(Collections.emptyList())
                .get();
        getMongoStore().removeEntities(MongoOnlineUserSessionEntity.class, query, false, invocationContext);
        getMongoStore().removeEntities(MongoOfflineUserSessionEntity.class, query, false, invocationContext);
    }

    @Override
    public int getUserSessionsCount(boolean offline) {
        DBObject query = new QueryBuilder()
                .get();

        Class<? extends MongoUserSessionEntity> clazz = offline ? MongoOfflineUserSessionEntity.class : MongoOnlineUserSessionEntity.class;
        return getMongoStore().countEntities(clazz, query, invocationContext);
    }

    @Override
    public void updateAllTimestamps(int time) {
        // 1) Update timestamp of clientSessions

        DBObject timestampSubquery = new QueryBuilder()
                .and("timestamp").notEquals(time).get();

        DBObject query = new QueryBuilder()
                .and("clientSessions").elemMatch(timestampSubquery).get();


        DBObject update = new QueryBuilder()
                .and("$set").is(new BasicDBObject("clientSessions.$.timestamp", time)).get();

        // Not sure how to do in single query :/
        int countModified = 1;
        while (countModified > 0) {
            countModified = getMongoStore().updateEntities(MongoOfflineUserSessionEntity.class, query, update, invocationContext);
        }

        countModified = 1;
        while (countModified > 0) {
            countModified = getMongoStore().updateEntities(MongoOnlineUserSessionEntity.class, query, update, invocationContext);
        }

        // 2) update lastSessionRefresh of userSessions
        query = new QueryBuilder().get();

        update = new QueryBuilder()
                .and("$set").is(new BasicDBObject("lastSessionRefresh", time)).get();

        getMongoStore().updateEntities(MongoOfflineUserSessionEntity.class, query, update, invocationContext);
        getMongoStore().updateEntities(MongoOnlineUserSessionEntity.class, query, update, invocationContext);
    }

    @Override
    public List<UserSessionModel> loadUserSessions(int firstResult, int maxResults, boolean offline) {
        DBObject query = new QueryBuilder()
                .get();
        DBObject sort = new BasicDBObject("id", 1);

        Class<? extends MongoUserSessionEntity> clazz = offline ? MongoOfflineUserSessionEntity.class : MongoOnlineUserSessionEntity.class;

        List<? extends MongoUserSessionEntity> entities = getMongoStore().loadEntities(clazz, query, sort, firstResult, maxResults, invocationContext);

        List<UserSessionModel> results = new LinkedList<>();
        for (MongoUserSessionEntity entity : entities) {
            RealmModel realm = session.realms().getRealm(entity.getRealmId());
            UserModel user = session.users().getUserById(entity.getUserId(), realm);

            // Case when user was deleted in the meantime
            if (user == null) {
                onUserRemoved(realm, entity.getUserId());
                return loadUserSessions(firstResult, maxResults, offline);
            }

            PersistentUserSessionAdapter userSession = toAdapter(realm, user, entity);
            results.add(userSession);
        }
        return results;
    }

    private PersistentUserSessionAdapter toAdapter(RealmModel realm, UserModel user, PersistentUserSessionEntity entity) {
        PersistentUserSessionModel model = new PersistentUserSessionModel();
        model.setUserSessionId(entity.getId());
        model.setLastSessionRefresh(entity.getLastSessionRefresh());
        model.setData(entity.getData());

        List<ClientSessionModel> clientSessions = new LinkedList<>();
        PersistentUserSessionAdapter userSessionAdapter = new PersistentUserSessionAdapter(model, realm, user, clientSessions);
        for (PersistentClientSessionEntity clientSessEntity : entity.getClientSessions()) {
            PersistentClientSessionAdapter clientSessAdapter = toAdapter(realm, userSessionAdapter, clientSessEntity);
            clientSessions.add(clientSessAdapter);
        }

        return userSessionAdapter;
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
    public void close() {

    }
}
