package org.keycloak.models.mongo.keycloak.entities;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.entities.OAuthClientEntity;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoIdentifiableEntity;
import org.keycloak.models.mongo.api.MongoIndex;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "oauthClients")
@MongoIndex(fields = { "realmId", "name" }, unique = true)
public class MongoOAuthClientEntity extends OAuthClientEntity implements MongoIdentifiableEntity {

    @Override
    public void afterRemove(MongoStoreInvocationContext invocationContext) {
        DBObject query = new QueryBuilder()
                .and("clientId").is(getId())
                .get();
        invocationContext.getMongoStore().removeEntities(MongoClientUserSessionAssociationEntity.class, query, invocationContext);
    }
}
