package org.keycloak.models.mongo.api;

import com.mongodb.DBObject;

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
    void insertObject(MongoEntity object);

    /**
     * Update existing object
     *
     * @param object to update
     */
    void updateObject(MongoEntity object);


    <T extends MongoEntity> T loadObject(Class<T> type, String oid);

    <T extends MongoEntity> T loadSingleObject(Class<T> type, DBObject query);

    <T extends MongoEntity> List<T> loadObjects(Class<T> type, DBObject query);

    // Object must have filled oid
    boolean removeObject(MongoEntity object);

    boolean removeObject(Class<? extends MongoEntity> type, String oid);

    boolean removeObjects(Class<? extends MongoEntity> type, DBObject query);

    <S> boolean pushItemToList(MongoEntity object, String listPropertyName, S itemToPush, boolean skipIfAlreadyPresent);

    <S> void pullItemFromList(MongoEntity object, String listPropertyName, S itemToPull);
}
