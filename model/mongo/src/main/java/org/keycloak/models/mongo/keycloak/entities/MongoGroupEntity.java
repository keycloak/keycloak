package org.keycloak.models.mongo.keycloak.entities;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.jboss.logging.Logger;
import org.keycloak.connections.mongo.api.MongoCollection;
import org.keycloak.connections.mongo.api.MongoField;
import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.entities.GroupEntity;
import org.keycloak.models.entities.RoleEntity;

import java.util.List;

/**
 */
@MongoCollection(collectionName = "groups")
public class MongoGroupEntity extends GroupEntity implements MongoIdentifiableEntity {

    private static final Logger logger = Logger.getLogger(MongoGroupEntity.class);

    @Override
    public void afterRemove(MongoStoreInvocationContext invContext) {
    }
}
