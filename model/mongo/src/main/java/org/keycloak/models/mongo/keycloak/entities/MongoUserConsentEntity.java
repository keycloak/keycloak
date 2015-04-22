package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.connections.mongo.api.MongoCollection;
import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.entities.UserConsentEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "userConsents")
public class MongoUserConsentEntity extends UserConsentEntity implements MongoIdentifiableEntity {

    @Override
    public void afterRemove(MongoStoreInvocationContext invocationContext) {
    }
}
