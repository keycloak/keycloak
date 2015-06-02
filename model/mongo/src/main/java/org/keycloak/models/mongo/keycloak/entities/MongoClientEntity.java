package org.keycloak.models.mongo.keycloak.entities;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.connections.mongo.api.MongoCollection;
import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.entities.ClientEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "clients")
public class MongoClientEntity extends ClientEntity implements MongoIdentifiableEntity {

    @Override
    public void afterRemove(MongoStoreInvocationContext context) {
        // Remove all roles, which belongs to this application
        DBObject query = new QueryBuilder()
                .and("clientId").is(getId())
                .get();
        context.getMongoStore().removeEntities(MongoRoleEntity.class, query, true, context);
    }
}
