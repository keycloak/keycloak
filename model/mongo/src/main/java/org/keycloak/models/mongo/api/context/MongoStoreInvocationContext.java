package org.keycloak.models.mongo.api.context;

import org.keycloak.models.mongo.api.MongoIdentifiableEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface MongoStoreInvocationContext {

    void addLoadedObject(MongoIdentifiableEntity entity);

    <T extends MongoIdentifiableEntity> T getLoadedObject(Class<T> type, String id);

    void addUpdateTask(MongoIdentifiableEntity entityToUpdate, MongoTask task);

    void addRemovedObject(MongoIdentifiableEntity entityToRemove);

    void beforeDBSearch(Class<? extends MongoIdentifiableEntity> entityType);

    void begin();

    void commit();

    void rollback();
}
