package org.keycloak.models.mongo.api;

import com.mongodb.DBObject;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface MongoStore {

    /**
     * Insert new object
     *
     * @param object to update
     */
    void insertObject(MongoIdentifiableEntity object, MongoStoreInvocationContext context);

    /**
     * Update existing object
     *
     * @param object to update
     */
    void updateObject(MongoIdentifiableEntity object, MongoStoreInvocationContext context);


    <T extends MongoIdentifiableEntity> T loadObject(Class<T> type, String oid, MongoStoreInvocationContext context);

    <T extends MongoIdentifiableEntity> T loadSingleObject(Class<T> type, DBObject query, MongoStoreInvocationContext context);

    <T extends MongoIdentifiableEntity> List<T> loadObjects(Class<T> type, DBObject query, MongoStoreInvocationContext context);

    boolean removeObject(MongoIdentifiableEntity object, MongoStoreInvocationContext context);

    boolean removeObject(Class<? extends MongoIdentifiableEntity> type, String id, MongoStoreInvocationContext context);

    boolean removeObjects(Class<? extends MongoIdentifiableEntity> type, DBObject query, MongoStoreInvocationContext context);

    <S> boolean pushItemToList(MongoIdentifiableEntity object, String listPropertyName, S itemToPush, boolean skipIfAlreadyPresent, MongoStoreInvocationContext context);

    <S> boolean pullItemFromList(MongoIdentifiableEntity object, String listPropertyName, S itemToPull, MongoStoreInvocationContext context);
}
