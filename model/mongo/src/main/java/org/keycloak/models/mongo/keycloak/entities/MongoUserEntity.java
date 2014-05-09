package org.keycloak.models.mongo.keycloak.entities;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.entities.UserEntity;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoIdentifiableEntity;
import org.keycloak.models.mongo.api.MongoIndex;
import org.keycloak.models.mongo.api.MongoIndexes;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "users")
@MongoIndexes({
        @MongoIndex(fields = { "realmId", "loginName" }, unique = true),
        @MongoIndex(fields = { "emailIndex" }, unique = true, sparse = true),
})
public class MongoUserEntity extends UserEntity implements MongoIdentifiableEntity {


    public String getEmailIndex() {
        return getEmail() != null ? getRealmId() + "//" + getEmail() : null;
    }

    public void setEmailIndex(String ignored) {
    }

    @Override
    public void afterRemove(MongoStoreInvocationContext invocationContext) {
        DBObject query = new QueryBuilder()
                .and("userId").is(getId())
                .get();

        invocationContext.getMongoStore().removeEntities(MongoUserSessionEntity.class, query, invocationContext);
    }
}
