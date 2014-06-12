package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.impl.context.TransactionMongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.entities.MongoRealmEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoKeycloakSession implements KeycloakSession {

    private final MongoStoreInvocationContext invocationContext;
    private final MongoKeycloakTransaction transaction;

    public MongoKeycloakSession(MongoStore mongoStore) {
        // this.invocationContext = new SimpleMongoStoreInvocationContext(mongoStore);
        this.invocationContext = new TransactionMongoStoreInvocationContext(mongoStore);
        this.transaction = new MongoKeycloakTransaction(invocationContext);
    }

    @Override
    public KeycloakTransaction getTransaction() {
        return transaction;
    }

    @Override
    public void close() {
        // TODO
    }

    @Override
    public void removeAllData() {
        getMongoStore().removeAllEntities();
    }

    @Override
    public RealmModel createRealm(String name) {
        return createRealm(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        MongoRealmEntity newRealm = new MongoRealmEntity();
        newRealm.setId(id);
        newRealm.setName(name);

        getMongoStore().insertEntity(newRealm, invocationContext);

        return new RealmAdapter(newRealm, invocationContext);
    }

    @Override
    public RealmModel getRealm(String id) {
        MongoRealmEntity realmEntity = getMongoStore().loadEntity(MongoRealmEntity.class, id, invocationContext);
        return realmEntity != null ? new RealmAdapter(realmEntity, invocationContext) : null;
    }

    @Override
    public List<RealmModel> getRealms() {
        DBObject query = new BasicDBObject();
        List<MongoRealmEntity> realms = getMongoStore().loadEntities(MongoRealmEntity.class, query, invocationContext);

        List<RealmModel> results = new ArrayList<RealmModel>();
        for (MongoRealmEntity realmEntity : realms) {
            results.add(new RealmAdapter(realmEntity, invocationContext));
        }
        return results;
    }

    @Override
    public RealmModel getRealmByName(String name) {
        DBObject query = new QueryBuilder()
                .and("name").is(name)
                .get();
        MongoRealmEntity realm = getMongoStore().loadSingleEntity(MongoRealmEntity.class, query, invocationContext);

        if (realm == null) return null;
        return new RealmAdapter(realm, invocationContext);
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        MongoUserEntity user = getMongoStore().loadEntity(MongoUserEntity.class, id, invocationContext);

        // Check that it's user from this realm
        if (user == null || !realm.getId().equals(user.getRealmId())) {
            return null;
        } else {
            return new UserAdapter(realm, user, invocationContext);
        }
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("loginName").is(username)
                .and("realmId").is(realm.getId())
                .get();
        MongoUserEntity user = getMongoStore().loadSingleEntity(MongoUserEntity.class, query, invocationContext);

        if (user == null) {
            return null;
        } else {
            return new UserAdapter(realm, user, invocationContext);
        }
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("email").is(email)
                .and("realmId").is(realm.getId())
                .get();
        MongoUserEntity user = getMongoStore().loadSingleEntity(MongoUserEntity.class, query, invocationContext);

        if (user == null) {
            return null;
        } else {
            return new UserAdapter(realm, user, invocationContext);
        }
    }

    @Override
    public boolean removeRealm(String id) {
        return getMongoStore().removeEntity(MongoRealmEntity.class, id, invocationContext);
    }

    protected MongoStore getMongoStore() {
        return invocationContext.getMongoStore();
    }

    @Override
    public UserModel getUserBySocialLink(SocialLinkModel socialLink, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<SocialLinkModel> getSocialLinks(UserModel user, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SocialLinkModel getSocialLink(UserModel user, String socialProvider, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ApplicationModel getApplicationById(String id, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(String username, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(String username, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UsernameLoginFailureModel> getAllUserLoginFailures() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserSessionModel createUserSession(RealmModel realm, UserModel user, String ipAddress) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserSessionModel getUserSession(String id, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserSessionModel> getUserSessions(UserModel user, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getActiveUserSessions(RealmModel realm, ClientModel client) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeUserSession(UserSessionModel session) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
