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

        getMongoStore().insertEntity(newRealm, invocationContext);

        return new RealmAdapter(newRealm, invocationContext);
    }

    @Override
    public RealmModel getRealm(String id) {
        RealmEntity realmEntity = getMongoStore().loadEntity(RealmEntity.class, id, invocationContext);
        return realmEntity != null ? new RealmAdapter(realmEntity, invocationContext) : null;
    }

    @Override
    public List<RealmModel> getRealms() {
        DBObject query = new BasicDBObject();
        List<RealmEntity> realms = getMongoStore().loadEntities(RealmEntity.class, query, invocationContext);

        List<RealmModel> results = new ArrayList<RealmModel>();
        for (RealmEntity realmEntity : realms) {
            results.add(new RealmAdapter(realmEntity, invocationContext));
        }
        return results;
    }

    @Override
    public RealmModel getRealmByName(String name) {
        DBObject query = new QueryBuilder()
                .and("name").is(name)
                .get();
        RealmEntity realm = getMongoStore().loadSingleEntity(RealmEntity.class, query, invocationContext);

        if (realm == null) return null;
        return new RealmAdapter(realm, invocationContext);
    }

    @Override
    public boolean removeRealm(String id) {
        return getMongoStore().removeEntity(RealmEntity.class, id, invocationContext);
    }

    protected MongoStore getMongoStore() {
        return invocationContext.getMongoStore();
    }
}
