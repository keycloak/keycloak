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
import org.keycloak.models.mongo.impl.context.SimpleMongoStoreInvocationContext;
import org.keycloak.models.mongo.impl.context.TransactionMongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.entities.RealmEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoKeycloakSession implements KeycloakSession {

    private final MongoStoreInvocationContext invocationContext;
    private final MongoKeycloakTransaction transaction;
    private final MongoStore mongoStore;

    public MongoKeycloakSession(MongoStore mongoStore) {
        this.mongoStore = mongoStore;
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
    public RealmModel createRealm(String name) {
        return createRealm(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        if (getRealm(id) != null) {
            throw new IllegalStateException("Realm with id '" + id + "' already exists");
        }

        RealmEntity newRealm = new RealmEntity();
        newRealm.setId(id);
        newRealm.setName(name);

        mongoStore.insertObject(newRealm, invocationContext);

        return new RealmAdapter(newRealm, mongoStore, invocationContext);
    }

    @Override
    public RealmModel getRealm(String id) {
        RealmEntity realmEntity = mongoStore.loadObject(RealmEntity.class, id, invocationContext);
        return realmEntity != null ? new RealmAdapter(realmEntity, mongoStore, invocationContext) : null;
    }

    @Override
    public List<RealmModel> getRealms(UserModel admin) {
        DBObject query = new BasicDBObject();
        List<RealmEntity> realms = mongoStore.loadObjects(RealmEntity.class, query, invocationContext);

        List<RealmModel> results = new ArrayList<RealmModel>();
        for (RealmEntity realmEntity : realms) {
            results.add(new RealmAdapter(realmEntity, mongoStore, invocationContext));
        }
        return results;
    }

    @Override
    public RealmModel getRealmByName(String name) {
        DBObject query = new QueryBuilder()
                .and("name").is(name)
                .get();
        RealmEntity realm = mongoStore.loadSingleObject(RealmEntity.class, query, invocationContext);

        if (realm == null) return null;
        return new RealmAdapter(realm, mongoStore, invocationContext);
    }

    @Override
    public boolean removeRealm(String id) {
        return mongoStore.removeObject(RealmEntity.class, id, invocationContext);
    }
}
