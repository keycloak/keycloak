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
        String applicationId = getApplicationId();
        String name = getName();

        if (realmId != null) {
            return realmId + "//" + name;
        } else {
            return applicationId + "//" + name;
        }
    }

    public void setNameIndex(String ignored) {
    }

    @Override
    public void afterRemove(MongoStoreInvocationContext invContext) {
        MongoStore mongoStore = invContext.getMongoStore();

        // Remove this role from all users, which has it
        DBObject query = new QueryBuilder()
                .and("roleIds").is(getId())
                .get();

        List<MongoUserEntity> users = mongoStore.loadEntities(MongoUserEntity.class, query, invContext);
        for (MongoUserEntity user : users) {
            //logger.info("Removing role " + getName() + " from user " + user.getUsername());
            mongoStore.pullItemFromList(user, "roleIds", getId(), invContext);
        }

        // Remove this scope from all users, which has it
        query = new QueryBuilder()
                .and("scopeIds").is(getId())
                .get();

        users = mongoStore.loadEntities(MongoUserEntity.class, query, invContext);
        for (MongoUserEntity user : users) {
            //logger.info("Removing scope " + getName() + " from user " + user.getUsername());
            mongoStore.pullItemFromList(user, "scopeIds", getId(), invContext);
        }

        // Remove defaultRoles from realm
        if (getRealmId() != null) {
            MongoRealmEntity realmEntity = mongoStore.loadEntity(MongoRealmEntity.class, getRealmId(), invContext);

            // Realm might be already removed at this point
            if (realmEntity != null) {
                mongoStore.pullItemFromList(realmEntity, "defaultRoles", getId(), invContext);
            }
        }

        // Remove defaultRoles from application
        if (getApplicationId() != null) {
            MongoApplicationEntity appEntity = mongoStore.loadEntity(MongoApplicationEntity.class, getApplicationId(), invContext);

            // Application might be already removed at this point
            if (appEntity != null) {
                mongoStore.pullItemFromList(appEntity, "defaultRoles", getId(), invContext);
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
