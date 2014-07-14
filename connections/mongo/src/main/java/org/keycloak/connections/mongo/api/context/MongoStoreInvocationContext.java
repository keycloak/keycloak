package org.keycloak.connections.mongo.api.context;

import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.MongoStore;

/**
 * Context, which provides callback methods to be invoked by MongoStore
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface MongoStoreInvocationContext {

    void addCreatedEntity(MongoIdentifiableEntity entity);

    void addLoadedEntity(MongoIdentifiableEntity entity);

    <T extends MongoIdentifiableEntity> T getLoadedEntity(Class<T> type, String id);

    void addUpdateTask(MongoIdentifiableEntity entityToUpdate, MongoTask task);

    void addRemovedEntity(MongoIdentifiableEntity entity);

    void beforeDBSearch(Class<? extends MongoIdentifiableEntity> entityType);

    void begin();

    void commit();

    void rollback();

    MongoStore getMongoStore();
}
