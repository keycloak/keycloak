package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.impl.context.TransactionMongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.entities.MongoRealmEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.ArrayList;
import java.util.List;

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
    public UserModel getUserById(String id, String realmId) {
        MongoUserEntity user = getMongoStore().loadEntity(MongoUserEntity.class, id, invocationContext);

        // Check that it's user from this realm
        if (user == null || !realmId.equals(user.getRealmId())) {
            return null;
        } else {
            return new UserAdapter(user, invocationContext);
        }
    }

    @Override
    public UserModel getUserByUsername(String username, String realmId) {
        DBObject query = new QueryBuilder()
                .and("loginName").is(username)
                .and("realmId").is(realmId)
                .get();
        MongoUserEntity user = getMongoStore().loadSingleEntity(MongoUserEntity.class, query, invocationContext);

        if (user == null) {
            return null;
        } else {
            return new UserAdapter(user, invocationContext);
        }
    }

    @Override
    public UserModel getUserByEmail(String email, String realmId) {
        DBObject query = new QueryBuilder()
                .and("email").is(email)
                .and("realmId").is(realmId)
                .get();
        MongoUserEntity user = getMongoStore().loadSingleEntity(MongoUserEntity.class, query, invocationContext);

        if (user == null) {
            return null;
        } else {
            return new UserAdapter(user, invocationContext);
        }
    }

    @Override
    public boolean removeRealm(String id) {
        return getMongoStore().removeEntity(MongoRealmEntity.class, id, invocationContext);
    }

    protected MongoStore getMongoStore() {
        return invocationContext.getMongoStore();
    }
}
