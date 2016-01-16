package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.connections.mongo.api.MongoCollection;
import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.entities.ClientTemplateEntity;

/**
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
@MongoCollection(collectionName = "clientTemplates")
public class MongoClientTemplateEntity extends ClientTemplateEntity implements MongoIdentifiableEntity {

    @Override
    public void afterRemove(MongoStoreInvocationContext context) {

    }
}
