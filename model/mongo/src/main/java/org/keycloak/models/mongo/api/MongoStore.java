package org.keycloak.models.mongo.api;

import com.mongodb.DBObject;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface MongoStore {

    /**
     * Insert new entity
     *
     * @param entity to insert
     */
    void insertEntity(MongoIdentifiableEntity entity, MongoStoreInvocationContext context);

    /**
     * Update existing entity
     *
     * @param entity to update
     */
    void updateEntity(MongoIdentifiableEntity entity, MongoStoreInvocationContext context);


    <T extends MongoIdentifiableEntity> T loadEntity(Class<T> type, String id, MongoStoreInvocationContext context);

    <T extends MongoIdentifiableEntity> T loadSingleEntity(Class<T> type, DBObject query, MongoStoreInvocationContext context);

    <T extends MongoIdentifiableEntity> List<T> loadEntities(Class<T> type, DBObject query, MongoStoreInvocationContext context);

    boolean removeEntity(MongoIdentifiableEntity entity, MongoStoreInvocationContext context);

    boolean removeEntity(Class<? extends MongoIdentifiableEntity> type, String id, MongoStoreInvocationContext context);

    boolean removeEntities(Class<? extends MongoIdentifiableEntity> type, DBObject query, MongoStoreInvocationContext context);

    <S> boolean pushItemToList(MongoIdentifiableEntity entity, String listPropertyName, S itemToPush, boolean skipIfAlreadyPresent, MongoStoreInvocationContext context);

    <S> boolean pullItemFromList(MongoIdentifiableEntity entity, String listPropertyName, S itemToPull, MongoStoreInvocationContext context);
}
