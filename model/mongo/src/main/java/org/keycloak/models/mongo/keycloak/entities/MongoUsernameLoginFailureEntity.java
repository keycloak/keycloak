package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.models.entities.UsernameLoginFailureEntity;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoIdentifiableEntity;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@MongoCollection(collectionName = "userFailures")
public class MongoUsernameLoginFailureEntity extends UsernameLoginFailureEntity implements MongoIdentifiableEntity {

    @Override
    public void afterRemove(MongoStoreInvocationContext invocationContext) {
    }
}
