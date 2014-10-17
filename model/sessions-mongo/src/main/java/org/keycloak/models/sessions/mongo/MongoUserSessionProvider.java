package org.keycloak.models.sessions.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.sessions.mongo.entities.MongoClientSessionEntity;
import org.keycloak.models.sessions.mongo.entities.MongoUserSessionEntity;
import org.keycloak.models.sessions.mongo.entities.MongoUsernameLoginFailureEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.util.Time;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MongoUserSessionProvider implements UserSessionProvider {

    private final KeycloakSession session;
    private final MongoStore mongoStore;
    private final MongoStoreInvocationContext invocationContext;

    public MongoUserSessionProvider(KeycloakSession session, MongoStore mongoStore, MongoStoreInvocationContext invocationContext) {
        this.session = session;
        this.mongoStore = mongoStore;
        this.invocationContext = invocationContext;
    }

    public MongoStore getMongoStore() {
        return mongoStore;
    }

    @Override
    public ClientSessionModel createClientSession(RealmModel realm, ClientModel client) {
        MongoClientSessionEntity entity = new MongoClientSessionEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setTimestamp(Time.currentTime());
        entity.setClientId(client.getId());
        entity.setRealmId(realm.getId());

        mongoStore.insertEntity(entity, invocationContext);

        return new ClientSessionAdapter(session, this, realm, entity, invocationContext);
    }

    @Override
    public ClientSessionModel getClientSession(RealmModel realm, String id) {
        MongoClientSessionEntity entity = getClientSessionEntity(id);
        if (entity == null) return null;
        return new ClientSessionAdapter(session, this, realm, entity, invocationContext);
    }

    @Override
    public ClientSessionModel getClientSession(String id) {
        MongoClientSessionEntity entity = getClientSessionEntity(id);
        if (entity != null) {
            RealmModel realm = session.realms().getRealm(entity.getRealmId());
            return  new ClientSessionAdapter(session, this, realm, entity, invocationContext);
        }
        return null;
    }


    @Override
    public UserSessionModel createUserSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe) {
        MongoUserSessionEntity entity = new MongoUserSessionEntity();
        entity.setRealmId(realm.getId());
        entity.setUser(user.getId());
        entity.setLoginUsername(loginUsername);
        entity.setIpAddress(ipAddress);
        entity.setAuthMethod(authMethod);
        entity.setRememberMe(rememberMe);
        entity.setRealmId(realm.getId());

        int currentTime = Time.currentTime();

        entity.setStarted(currentTime);
        entity.setLastSessionRefresh(currentTime);

        mongoStore.insertEntity(entity, invocationContext);
        return new UserSessionAdapter(session, this, entity, realm, invocationContext);
    }

    @Override
    public UserSessionModel getUserSession(RealmModel realm, String id) {
        MongoUserSessionEntity entity = getUserSessionEntity(realm, id);
        if (entity == null) {
            return null;
        } else {
            return new UserSessionAdapter(session, this, entity, realm, invocationContext);
        }
    }

    MongoUserSessionEntity getUserSessionEntity(RealmModel realm, String id) {
        return mongoStore.loadEntity(MongoUserSessionEntity.class, id, invocationContext);
    }

    MongoClientSessionEntity getClientSessionEntity(String id) {
        return mongoStore.loadEntity(MongoClientSessionEntity.class, id, invocationContext);
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, UserModel user) {
        DBObject query = new BasicDBObject("user", user.getId());
        List<UserSessionModel> sessions = new LinkedList<UserSessionModel>();
        for (MongoUserSessionEntity e : mongoStore.loadEntities(MongoUserSessionEntity.class, query, invocationContext)) {
            sessions.add(new UserSessionAdapter(session, this, e, realm, invocationContext));
        }
        return sessions;
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client) {
        return getUserSessions(realm, client, -1, -1);
    }

    public List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client, int firstResult, int maxResults) {
        DBObject query = new QueryBuilder()
                .and("clientId").is(client.getId())
                .and("sessionId").notEquals(null)
                .get();
        DBObject sort = new BasicDBObject("timestamp", 1).append("id", 1);

        List<MongoClientSessionEntity> clientSessions = mongoStore.loadEntities(MongoClientSessionEntity.class, query, sort, firstResult, maxResults, invocationContext);
        List<UserSessionModel> result = new LinkedList<UserSessionModel>();
        for (MongoClientSessionEntity clientSession : clientSessions) {
            MongoUserSessionEntity userSession = mongoStore.loadEntity(MongoUserSessionEntity.class, clientSession.getSessionId(), invocationContext);
            result.add(new UserSessionAdapter(session, this, userSession, realm, invocationContext));
        }
        return result;
    }

    @Override
    public int getActiveUserSessions(RealmModel realm, ClientModel client) {
        DBObject query = new QueryBuilder()
                .and("clientId").is(client.getId())
                .and("sessionId").notEquals(null)
                .get();
        return mongoStore.countEntities(MongoClientSessionEntity.class, query, invocationContext);
    }

    @Override
    public void removeUserSession(RealmModel realm, UserSessionModel session) {
        mongoStore.removeEntity(((UserSessionAdapter) session).getMongoEntity(), invocationContext);
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        DBObject query = new BasicDBObject("user", user.getId());
        mongoStore.removeEntities(MongoUserSessionEntity.class, query, invocationContext);
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        DBObject query = new BasicDBObject("realmId", realm.getId());
        mongoStore.removeEntities(MongoUserSessionEntity.class, query, invocationContext);
        query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .get();

        mongoStore.removeEntities(MongoClientSessionEntity.class, query, invocationContext);
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        int currentTime = Time.currentTime();
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .and("started").lessThan(currentTime - realm.getSsoSessionMaxLifespan())
                .get();

        mongoStore.removeEntities(MongoUserSessionEntity.class, query, invocationContext);
        query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .and("lastSessionRefresh").lessThan(currentTime - realm.getSsoSessionIdleTimeout())
                .get();

        mongoStore.removeEntities(MongoUserSessionEntity.class, query, invocationContext);
        query = new QueryBuilder()
                .and("sessionId").is(null)
                .and("realmId").is(realm.getId())
                .and("timestamp").lessThan(currentTime - realm.getSsoSessionIdleTimeout())
                .get();

        mongoStore.removeEntities(MongoClientSessionEntity.class, query, invocationContext);
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(RealmModel realm, String username) {
        DBObject query = new QueryBuilder()
                .and("username").is(username)
                .and("realmId").is(realm.getId())
                .get();
        MongoUsernameLoginFailureEntity user = mongoStore.loadSingleEntity(MongoUsernameLoginFailureEntity.class, query, invocationContext);

        if (user == null) {
            return null;
        } else {
            return new UsernameLoginFailureAdapter(invocationContext, user);
        }
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(RealmModel realm, String username) {
        UsernameLoginFailureModel userLoginFailure = getUserLoginFailure(realm, username);
        if (userLoginFailure != null) {
            return userLoginFailure;
        }

        MongoUsernameLoginFailureEntity userEntity = new MongoUsernameLoginFailureEntity();
        userEntity.setUsername(username);
        userEntity.setRealmId(realm.getId());

        mongoStore.insertEntity(userEntity, invocationContext);
        return new UsernameLoginFailureAdapter(invocationContext, userEntity);
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        removeUserSessions(realm);
    }

    @Override
    // TODO Not very efficient, should use Mongo $pull to remove directly
    public void onClientRemoved(RealmModel realm, ClientModel client) {
        DBObject query = new QueryBuilder()
                .and("clientId").is(client.getId())
                .get();
        DBObject sort = new BasicDBObject("timestamp", 1).append("id", 1);

        List<MongoClientSessionEntity> clientSessions = mongoStore.loadEntities(MongoClientSessionEntity.class, query, sort, -1, -1, invocationContext);
        for (MongoClientSessionEntity clientSession : clientSessions) {
            MongoUserSessionEntity userSession = mongoStore.loadEntity(MongoUserSessionEntity.class, clientSession.getSessionId(), invocationContext);
            getMongoStore().pullItemFromList(userSession, "clientSessions", clientSession.getId(), invocationContext);
            mongoStore.removeEntity(clientSession, invocationContext);
        }
    }

    @Override
    public void onUserRemoved(RealmModel realm, UserModel user) {
        removeUserSessions(realm, user);

        DBObject query = new QueryBuilder()
                .or(new BasicDBObject("username", user.getUsername()), new BasicDBObject("username", user.getEmail()))
                .and("realmId").is(realm.getId())
                .get();
        mongoStore.removeEntities(MongoUsernameLoginFailureEntity.class, query, invocationContext);
    }

    @Override
    public void close() {
    }

}
