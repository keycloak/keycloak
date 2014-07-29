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
import java.util.Set;

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

    @Override
    public ClientSessionModel createClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession, String redirectUri, String state, Set<String> roles) {
        MongoUserSessionEntity userSessionEntity = getUserSessionEntity(realm, userSession.getId());

        MongoClientSessionEntity entity = new MongoClientSessionEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setTimestamp(Time.currentTime());
        entity.setClientId(client.getId());
        entity.setRedirectUri(redirectUri);
        entity.setState(state);
        if (roles != null) {
            entity.setRoles(new LinkedList<String>(roles));
        }

        mongoStore.pushItemToList(userSessionEntity, "clientSessions", entity, false, invocationContext);

        return new ClientSessionAdapter(session, this, realm, entity, userSessionEntity, invocationContext);
    }

    @Override
    public ClientSessionModel getClientSession(RealmModel realm, String id) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .and("clientSessions.id").is(id).get();

        List<MongoUserSessionEntity> entities = mongoStore.loadEntities(MongoUserSessionEntity.class, query, invocationContext);
        if (entities.isEmpty()) {
            return null;
        }

        MongoUserSessionEntity userSessionEntity = entities.get(0);
        List<MongoClientSessionEntity> sessions = userSessionEntity.getClientSessions();
        for (MongoClientSessionEntity s : sessions) {
            if (s.getId().equals(id)) {
                return new ClientSessionAdapter(session, this, realm, s, userSessionEntity, invocationContext);
            }
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
                .and("clientSessions.clientId").is(client.getId())
                .get();
        DBObject sort = new BasicDBObject("started", 1).append("id", 1);

        List<MongoUserSessionEntity> sessions = mongoStore.loadEntities(MongoUserSessionEntity.class, query, sort, firstResult, maxResults, invocationContext);
        List<UserSessionModel> result = new LinkedList<UserSessionModel>();
        for (MongoUserSessionEntity session : sessions) {
            result.add(new UserSessionAdapter(this.session, this, session, realm, invocationContext));
        }
        return result;
    }

    @Override
    public int getActiveUserSessions(RealmModel realm, ClientModel client) {
        DBObject query = new QueryBuilder()
                .and("clientSessions.clientId").is(client.getId())
                .get();
        return mongoStore.countEntities(MongoUserSessionEntity.class, query, invocationContext);
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
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        int currentTime = Time.currentTime();
        DBObject query = new QueryBuilder()
                .and("started").lessThan(currentTime - realm.getSsoSessionMaxLifespan())
                .get();

        mongoStore.removeEntities(MongoUserSessionEntity.class, query, invocationContext);
        query = new QueryBuilder()
                .and("lastSessionRefresh").lessThan(currentTime - realm.getSsoSessionIdleTimeout())
                .get();

        mongoStore.removeEntities(MongoUserSessionEntity.class, query, invocationContext);
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
    public List<UsernameLoginFailureModel> getAllUserLoginFailures(RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .get();
        List<MongoUsernameLoginFailureEntity> failures = mongoStore.loadEntities(MongoUsernameLoginFailureEntity.class, query, invocationContext);

        List<UsernameLoginFailureModel> result = new LinkedList<UsernameLoginFailureModel>();
        if (failures == null) return result;
        for (MongoUsernameLoginFailureEntity failure : failures) {
            result.add(new UsernameLoginFailureAdapter(invocationContext, failure));
        }

        return result;
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        removeUserSessions(realm);
    }

    @Override
    // TODO Not very efficient, should use Mongo $pull to remove directly
    public void onClientRemoved(RealmModel realm, ClientModel client) {
        DBObject query = new QueryBuilder()
                .and("clientSessions.clientId").is(client.getId())
                .get();
        List<MongoUserSessionEntity> userSessionEntities = mongoStore.loadEntities(MongoUserSessionEntity.class, query, invocationContext);
        for (MongoUserSessionEntity e : userSessionEntities) {
            List<MongoClientSessionEntity> remove = new LinkedList<MongoClientSessionEntity>();
            for (MongoClientSessionEntity c : e.getClientSessions()) {
                if (c.getClientId().equals(client.getId())) {
                    remove.add(c);
                }
            }
            for (MongoClientSessionEntity c : remove) {
                mongoStore.pullItemFromList(e, "clientSessions", c, invocationContext);
            }
        }
    }

    @Override
    public void onUserRemoved(RealmModel realm, UserModel user) {
        removeUserSessions(realm, user);
    }

    @Override
    public void close() {
    }

}
