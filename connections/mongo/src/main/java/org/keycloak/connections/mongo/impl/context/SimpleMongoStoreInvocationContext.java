package org.keycloak.connections.mongo.impl.context;

import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.connections.mongo.api.context.MongoTask;

/**
 * Context, which is not doing any postponing of tasks and does not cache anything
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SimpleMongoStoreInvocationContext implements MongoStoreInvocationContext {

    private final MongoStore mongoStore;

    public SimpleMongoStoreInvocationContext(MongoStore mongoStore) {
        this.mongoStore = mongoStore;
    }

    @Override
    public void addCreatedEntity(MongoIdentifiableEntity entity) {
    }

    @Override
    public void addLoadedEntity(MongoIdentifiableEntity entity) {
    }

    @Override
    public <T extends MongoIdentifiableEntity> T getLoadedEntity(Class<T> type, String id) {
        return null;
    }

    @Override
    public void addUpdateTask(MongoIdentifiableEntity entityToUpdate, MongoTask task) {
        task.execute();
    }

    @Override
    public void addRemovedEntity(MongoIdentifiableEntity entity) {
        entity.afterRemove(this);
    }

    @Override
    public void beforeDBSearch(Class<? extends MongoIdentifiableEntity> entityType) {
    }

    @Override
    public void begin() {
    }

    @Override
    public void commit() {
    }

    @Override
    public void rollback() {
    }

    @Override
    public MongoStore getMongoStore() {
        return mongoStore;
    }
}
