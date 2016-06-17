/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.mongo.store;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.mongo.adapter.ResourceServerAdapter;
import org.keycloak.authorization.mongo.entities.ResourceServerEntity;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class MongoResourceServerStore implements ResourceServerStore {

    private final MongoStoreInvocationContext invocationContext;
    private final AuthorizationProvider authorizationProvider;

    public MongoResourceServerStore(MongoStoreInvocationContext invocationContext, AuthorizationProvider authorizationProvider) {
        this.invocationContext = invocationContext;
        this.authorizationProvider = authorizationProvider;
    }

    @Override
    public ResourceServer create(String clientId) {
        ResourceServerEntity entity = new ResourceServerEntity();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setClientId(clientId);

        getMongoStore().insertEntity(entity, getInvocationContext());

        return new ResourceServerAdapter(entity, getInvocationContext());
    }

    @Override
    public void delete(String id) {
        getMongoStore().removeEntity(ResourceServerEntity.class, id, getInvocationContext());
    }

    @Override
    public ResourceServer findById(String id) {
        ResourceServerEntity entity = getMongoStore().loadEntity(ResourceServerEntity.class, id, getInvocationContext());

        if (entity == null) {
            return null;
        }

        return new ResourceServerAdapter(entity, getInvocationContext());
    }

    @Override
    public ResourceServer findByClient(String clientId) {
        DBObject query = new QueryBuilder()
                .and("clientId").is(clientId)
                .get();

        return getMongoStore().loadEntities(ResourceServerEntity.class, query, getInvocationContext()).stream()
                .map(resourceServer -> findById(resourceServer.getId())).findFirst().orElse(null);
    }

    private MongoStoreInvocationContext getInvocationContext() {
        return this.invocationContext;
    }

    private MongoStore getMongoStore() {
        return getInvocationContext().getMongoStore();
    }
}
