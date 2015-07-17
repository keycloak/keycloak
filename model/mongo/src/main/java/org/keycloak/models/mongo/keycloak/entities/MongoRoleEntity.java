package org.keycloak.models.mongo.keycloak.entities;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.jboss.logging.Logger;
import org.keycloak.connections.mongo.api.MongoCollection;
import org.keycloak.connections.mongo.api.MongoField;
import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.entities.RoleEntity;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "roles")
public class MongoRoleEntity extends RoleEntity implements MongoIdentifiableEntity {

    private static final Logger logger = Logger.getLogger(MongoRoleEntity.class);

    @MongoField
    // TODO This is required as Mongo doesn't support sparse indexes with compound keys (see https://jira.mongodb.org/browse/SERVER-2193)
    public String getNameIndex() {
        String realmId = getRealmId();
        String clientId = getClientId();
        String name = getName();

        if (realmId != null) {
            return realmId + "//" + name;
        } else {
            return clientId + "//" + name;
        }
    }

    public void setNameIndex(String ignored) {
    }

    @Override
    public void afterRemove(MongoStoreInvocationContext invContext) {
        MongoStore mongoStore = invContext.getMongoStore();

        // Remove this scope from all clients, which has it
        DBObject query = new QueryBuilder()
                .and("scopeIds").is(getId())
                .get();

        List<MongoClientEntity> clients = mongoStore.loadEntities(MongoClientEntity.class, query, invContext);
        for (MongoClientEntity client : clients) {
            //logger.info("Removing scope " + getName() + " from user " + user.getUsername());
            mongoStore.pullItemFromList(client, "scopeIds", getId(), invContext);
        }

        // Remove defaultRoles from realm
        if (getRealmId() != null) {
            MongoRealmEntity realmEntity = mongoStore.loadEntity(MongoRealmEntity.class, getRealmId(), invContext);

            // Realm might be already removed at this point
            if (realmEntity != null) {
                mongoStore.pullItemFromList(realmEntity, "defaultRoles", getName(), invContext);
            }
        }

        // Remove defaultRoles from application
        if (getClientId() != null) {
            MongoClientEntity appEntity = mongoStore.loadEntity(MongoClientEntity.class, getClientId(), invContext);

            // Application might be already removed at this point
            if (appEntity != null) {
                mongoStore.pullItemFromList(appEntity, "defaultRoles", getName(), invContext);
            }
        }

        // Remove this role from others who has it as composite
        query = new QueryBuilder()
                .and("compositeRoleIds").is(getId())
                .get();
        List<MongoRoleEntity> parentRoles = mongoStore.loadEntities(MongoRoleEntity.class, query, invContext);
        for (MongoRoleEntity role : parentRoles) {
            mongoStore.pullItemFromList(role, "compositeRoleIds", getId(), invContext);
        }
    }
}
