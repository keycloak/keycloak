package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.entities.PersistentUserSessionEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class MongoUserSessionEntity extends PersistentUserSessionEntity implements MongoIdentifiableEntity {

    @Override
    public void afterRemove(MongoStoreInvocationContext invocationContext) {
    }
}
