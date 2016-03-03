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
    public void beforeDBBulkUpdateOrRemove(Class<? extends MongoIdentifiableEntity> entityType) {
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
