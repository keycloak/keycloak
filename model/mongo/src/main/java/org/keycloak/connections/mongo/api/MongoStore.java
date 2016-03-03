/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.connections.mongo.api;

import com.mongodb.DBObject;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;

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

    /**
     * Bulk  update of more entities of some type
     *
     * @param type
     * @param query
     * @param update
     * @param context
     * @return count of updated entities
     */
    <T extends MongoIdentifiableEntity> int updateEntities(Class<T> type, DBObject query, DBObject update, MongoStoreInvocationContext context);

    <T extends MongoIdentifiableEntity> T loadEntity(Class<T> type, String id, MongoStoreInvocationContext context);

    <T extends MongoIdentifiableEntity> T loadSingleEntity(Class<T> type, DBObject query, MongoStoreInvocationContext context);

    /**
     * @param type
     * @param query
     * @param context
     * @return query result or empty list if no results available for the query. Doesn't return null
     */
    <T extends MongoIdentifiableEntity> List<T> loadEntities(Class<T> type, DBObject query, MongoStoreInvocationContext context);

    /**
     * @param type
     * @param query
     * @param context
     * @return query result or empty list if no results available for the query. Doesn't return null
     */
    <T extends MongoIdentifiableEntity> List<T> loadEntities(Class<T> type, DBObject query, DBObject sort, int firstResult, int maxResults, MongoStoreInvocationContext context);

    <T extends MongoIdentifiableEntity> int countEntities(Class<T> type, DBObject query, MongoStoreInvocationContext context);

    boolean removeEntity(MongoIdentifiableEntity entity, MongoStoreInvocationContext context);

    boolean removeEntity(Class<? extends MongoIdentifiableEntity> type, String id, MongoStoreInvocationContext context);

    /**
     *
     * @param type
     * @param query
     * @param callback if true, then store will first load all entities, then call "afterRemove" for every entity. If false, the entities are removed directly without load and calling "afterRemove" callback
     *                 false has better performance (especially if we are going to remove big number of entities)
     * @param context
     * @return count of removed entities
     */
    int removeEntities(Class<? extends MongoIdentifiableEntity> type, DBObject query, boolean callback, MongoStoreInvocationContext context);

    <S> boolean pushItemToList(MongoIdentifiableEntity entity, String listPropertyName, S itemToPush, boolean skipIfAlreadyPresent, MongoStoreInvocationContext context);

    <S> boolean pullItemFromList(MongoIdentifiableEntity entity, String listPropertyName, S itemToPull, MongoStoreInvocationContext context);

}
