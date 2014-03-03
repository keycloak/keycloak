package org.keycloak.models.mongo.keycloak.entities;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.jboss.logging.Logger;
import org.keycloak.models.mongo.api.AbstractMongoIdentifiableEntity;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.MongoField;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "roles")
public class RoleEntity extends AbstractMongoIdentifiableEntity implements MongoEntity {

    private static final Logger logger = Logger.getLogger(RoleEntity.class);

    private String name;
    private String description;

    private List<String> compositeRoleIds;

    private String realmId;
    private String applicationId;

    @MongoField
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @MongoField
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @MongoField
    public List<String> getCompositeRoleIds() {
        return compositeRoleIds;
    }

    public void setCompositeRoleIds(List<String> compositeRoleIds) {
        this.compositeRoleIds = compositeRoleIds;
    }

    @MongoField
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    @MongoField
    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    @Override
    public void afterRemove(MongoStoreInvocationContext invContext) {
        MongoStore mongoStore = invContext.getMongoStore();

        // Remove this role from all users, which has it
        DBObject query = new QueryBuilder()
                .and("roleIds").is(getId())
                .get();

        List<UserEntity> users = mongoStore.loadEntities(UserEntity.class, query, invContext);
        for (UserEntity user : users) {
            logger.info("Removing role " + getName() + " from user " + user.getLoginName());
            mongoStore.pullItemFromList(user, "roleIds", getId(), invContext);
        }

        // Remove this scope from all users, which has it
        query = new QueryBuilder()
                .and("scopeIds").is(getId())
                .get();

        users = mongoStore.loadEntities(UserEntity.class, query, invContext);
        for (UserEntity user : users) {
            logger.info("Removing scope " + getName() + " from user " + user.getLoginName());
            mongoStore.pullItemFromList(user, "scopeIds", getId(), invContext);
        }

        // Remove defaultRoles from realm
        if (realmId != null) {
            RealmEntity realmEntity = mongoStore.loadEntity(RealmEntity.class, realmId, invContext);

            // Realm might be already removed at this point
            if (realmEntity != null) {
                mongoStore.pullItemFromList(realmEntity, "defaultRoles", getId(), invContext);
            }
        }

        // Remove defaultRoles from application
        if (applicationId != null) {
            ApplicationEntity appEntity = mongoStore.loadEntity(ApplicationEntity.class, applicationId, invContext);

            // Application might be already removed at this point
            if (appEntity != null) {
                mongoStore.pullItemFromList(appEntity, "defaultRoles", getId(), invContext);
            }
        }

        // Remove this role from others who has it as composite
        query = new QueryBuilder()
                .and("compositeRoleIds").is(getId())
                .get();
        List<RoleEntity> parentRoles = mongoStore.loadEntities(RoleEntity.class, query, invContext);
        for (RoleEntity role : parentRoles) {
            mongoStore.pullItemFromList(role, "compositeRoleIds", getId(), invContext);
        }
    }
}
