package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.keycloak.entities.RealmEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoKeycloakSession implements KeycloakSession {

    private static final MongoKeycloakTransaction PLACEHOLDER = new MongoKeycloakTransaction();
    private final MongoStore mongoStore;

    public MongoKeycloakSession(MongoStore mongoStore) {
        this.mongoStore = mongoStore;
    }

    @Override
    public KeycloakTransaction getTransaction() {
        return PLACEHOLDER;
    }

    @Override
    public void close() {
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

        mongoStore.insertObject(newRealm);

        RealmAdapter realm = new RealmAdapter(newRealm, mongoStore);
        return realm;
    }

    @Override
    public RealmModel getRealm(String id) {
        RealmEntity realmEntity = mongoStore.loadObject(RealmEntity.class, id);
        return realmEntity != null ? new RealmAdapter(realmEntity, mongoStore) : null;
    }

    @Override
    public List<RealmModel> getRealms(UserModel admin) {
        DBObject query = new BasicDBObject();
        List<RealmEntity> realms = mongoStore.loadObjects(RealmEntity.class, query);

        List<RealmModel> results = new ArrayList<RealmModel>();
        for (RealmEntity realmEntity : realms) {
            results.add(new RealmAdapter(realmEntity, mongoStore));
        }
        return results;
    }

    @Override
    public RealmModel getRealmByName(String name) {
        DBObject query = new QueryBuilder()
                .and("name").is(name)
                .get();
        RealmEntity realm = mongoStore.loadSingleObject(RealmEntity.class, query);

        if (realm == null) return null;
        return new RealmAdapter(realm, mongoStore);
    }

    @Override
    public boolean removeRealm(String id) {
        return mongoStore.removeObject(RealmEntity.class, id);
    }
}
