package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.connections.mongo.api.MongoCollection;
import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.MongoIndex;
import org.keycloak.connections.mongo.api.MongoIndexes;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.entities.UserEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "users")
@MongoIndexes({
        @MongoIndex(fields = { "realmId", "username" }, unique = true),
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
    }
}
