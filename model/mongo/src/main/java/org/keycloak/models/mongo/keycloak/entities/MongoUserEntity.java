package org.keycloak.models.mongo.keycloak.entities;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.connections.mongo.api.MongoCollection;
import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.entities.UserEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "users")
public class MongoUserEntity extends UserEntity implements MongoIdentifiableEntity {

    public String getEmailIndex() {
        return getEmail() != null ? getRealmId() + "//" + getEmail() : null;
    }

    public void setEmailIndex(String ignored) {
    }

    @Override
    public void afterRemove(MongoStoreInvocationContext context) {
        // Remove all consents of this user
        DBObject query = new QueryBuilder()
                .and("userId").is(getId())
                .get();

        context.getMongoStore().removeEntities(MongoUserConsentEntity.class, query, true, context);
    }
}
