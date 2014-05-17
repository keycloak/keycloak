package org.keycloak.models.mongo.keycloak.entities;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.entities.RealmEntity;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoIdentifiableEntity;
import org.keycloak.models.mongo.api.MongoIndex;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "realms")
@MongoIndex(fields = { "name" }, unique = true)
public class MongoRealmEntity extends RealmEntity implements MongoIdentifiableEntity {

    @Override
    public void afterRemove(MongoStoreInvocationContext context) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .get();

        // Remove all users of this realm
        context.getMongoStore().removeEntities(MongoUserEntity.class, query, context);

        // Remove all roles of this realm
        context.getMongoStore().removeEntities(MongoRoleEntity.class, query, context);

        // Remove all applications of this realm
        context.getMongoStore().removeEntities(MongoApplicationEntity.class, query, context);

        // Remove all clients of this realm
        context.getMongoStore().removeEntities(MongoOAuthClientEntity.class, query, context);

        // Remove all sessions of this realm
        context.getMongoStore().removeEntities(MongoUserSessionEntity.class, query, context);
    }
}
