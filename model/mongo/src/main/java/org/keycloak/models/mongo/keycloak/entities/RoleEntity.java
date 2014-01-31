package org.keycloak.models.mongo.keycloak.entities;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.jboss.logging.Logger;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.MongoField;
import org.keycloak.models.mongo.api.MongoId;
import org.keycloak.models.mongo.api.MongoStore;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "roles")
public class RoleEntity implements MongoEntity {

    private static final Logger logger = Logger.getLogger(RoleEntity.class);

    private String id;
    private String name;
    private String description;
    private boolean composite;

    private List<String> compositeRoleIds;

    private String realmId;
    private String applicationId;

    @MongoId
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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
    public boolean isComposite() {
        return composite;
    }

    public void setComposite(boolean composite) {
        this.composite = composite;
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
    public void afterRemove(MongoStore mongoStore) {
        // Remove this role from all users, which has it
        DBObject query = new QueryBuilder()
                .and("roleIds").is(id)
                .get();

        List<UserEntity> users = mongoStore.loadObjects(UserEntity.class, query);
        for (UserEntity user : users) {
            logger.info("Removing role " + getName() + " from user " + user.getLoginName());
            mongoStore.pullItemFromList(user, "roleIds", getId());
        }

        // Remove this scope from all users, which has it
        query = new QueryBuilder()
                .and("scopeIds").is(id)
                .get();

        users = mongoStore.loadObjects(UserEntity.class, query);
        for (UserEntity user : users) {
            logger.info("Removing scope " + getName() + " from user " + user.getLoginName());
            mongoStore.pullItemFromList(user, "scopeIds", getId());
        }

        // Remove defaultRoles from realm
        if (realmId != null) {
            RealmEntity realmEntity = mongoStore.loadObject(RealmEntity.class, realmId);

            // Realm might be already removed at this point
            if (realmEntity != null) {
                mongoStore.pullItemFromList(realmEntity, "defaultRoles", getId());
            }
        }

        // Remove defaultRoles from application
        if (applicationId != null) {
            ApplicationEntity appEntity = mongoStore.loadObject(ApplicationEntity.class, applicationId);

            // Application might be already removed at this point
            if (appEntity != null) {
                mongoStore.pullItemFromList(appEntity, "defaultRoles", getId());
            }
        }

        // Remove this role from others who has it as composite
        query = new QueryBuilder()
                .and("compositeRoleIds").is(getId())
                .get();
        List<RoleEntity> parentRoles = mongoStore.loadObjects(RoleEntity.class, query);
        for (RoleEntity role : parentRoles) {
            mongoStore.pullItemFromList(role, "compositeRoleIds", getId());
        }
    }
}
