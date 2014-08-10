package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.mongo.keycloak.entities.MongoApplicationEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoOAuthClientEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRealmEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoRealmProvider implements RealmProvider {

    private final MongoStoreInvocationContext invocationContext;
    private final KeycloakSession session;
    private final MongoStore mongoStore;

    public MongoRealmProvider(KeycloakSession session, MongoStore mongoStore, MongoStoreInvocationContext invocationContext) {
        this.session = session;
        this.mongoStore = mongoStore;
        this.invocationContext = invocationContext;
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
        MongoRealmEntity newRealm = new MongoRealmEntity();
        newRealm.setId(id);
        newRealm.setName(name);

        getMongoStore().insertEntity(newRealm, invocationContext);

        return new RealmAdapter(session, newRealm, invocationContext);
    }

    @Override
    public RealmModel getRealm(String id) {
        MongoRealmEntity realmEntity = getMongoStore().loadEntity(MongoRealmEntity.class, id, invocationContext);
        return realmEntity != null ? new RealmAdapter(session, realmEntity, invocationContext) : null;
    }

    @Override
    public List<RealmModel> getRealms() {
        DBObject query = new BasicDBObject();
        List<MongoRealmEntity> realms = getMongoStore().loadEntities(MongoRealmEntity.class, query, invocationContext);

        List<RealmModel> results = new ArrayList<RealmModel>();
        for (MongoRealmEntity realmEntity : realms) {
            results.add(new RealmAdapter(session, realmEntity, invocationContext));
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
        return new RealmAdapter(session, realm, invocationContext);
    }

    @Override
    public boolean removeRealm(String id) {
        RealmModel realm = getRealm(id);
        if (realm == null) return false;
        session.users().preRemove(realm);
        return getMongoStore().removeEntity(MongoRealmEntity.class, id, invocationContext);
    }

    protected MongoStore getMongoStore() {
        return invocationContext.getMongoStore();
    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
        MongoRoleEntity role = getMongoStore().loadEntity(MongoRoleEntity.class, id, invocationContext);
        if (role == null) return null;
        if (role.getRealmId() != null && !role.getRealmId().equals(realm.getId())) return null;
        if (role.getApplicationId() != null && realm.getApplicationById(role.getApplicationId()) == null) return null;
        return new RoleAdapter(session, realm, role, null, invocationContext);
    }

    @Override
    public ApplicationModel getApplicationById(String id, RealmModel realm) {
        MongoApplicationEntity appData = getMongoStore().loadEntity(MongoApplicationEntity.class, id, invocationContext);

        // Check if application belongs to this realm
        if (appData == null || !realm.getId().equals(appData.getRealmId())) {
            return null;
        }

        return new ApplicationAdapter(session, realm, appData, invocationContext);
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id, RealmModel realm) {
        MongoOAuthClientEntity clientEntity = getMongoStore().loadEntity(MongoOAuthClientEntity.class, id, invocationContext);

        // Check if client belongs to this realm
        if (clientEntity == null || !realm.getId().equals(clientEntity.getRealmId())) return null;

        return new OAuthClientAdapter(session, realm, clientEntity, invocationContext);
    }

}
