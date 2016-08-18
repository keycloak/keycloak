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

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.mongo.adapter.ResourceAdapter;
import org.keycloak.authorization.mongo.entities.ResourceEntity;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class MongoResourceStore implements ResourceStore {

    private final MongoStoreInvocationContext invocationContext;
    private final AuthorizationProvider authorizationProvider;

    public MongoResourceStore(MongoStoreInvocationContext invocationContext, AuthorizationProvider authorizationProvider) {
        this.invocationContext = invocationContext;
        this.authorizationProvider = authorizationProvider;
    }

    @Override
    public Resource create(String name, ResourceServer resourceServer, String owner) {
        ResourceEntity entity = new ResourceEntity();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setName(name);
        entity.setResourceServerId(resourceServer.getId());
        entity.setOwner(owner);

        getMongoStore().insertEntity(entity, getInvocationContext());

        return new ResourceAdapter(entity, getInvocationContext(), this.authorizationProvider);
    }

    @Override
    public void delete(String id) {
        getMongoStore().removeEntity(ResourceEntity.class, id, getInvocationContext());
    }

    @Override
    public Resource findById(String id) {
        ResourceEntity entity = getMongoStore().loadEntity(ResourceEntity.class, id, getInvocationContext());

        if (entity == null) {
            return null;
        }

        return new ResourceAdapter(entity, getInvocationContext(), this.authorizationProvider);
    }

    @Override
    public List<Resource> findByOwner(String ownerId) {
        DBObject query = new QueryBuilder()
                .and("owner").is(ownerId)
                .get();

        return getMongoStore().loadEntities(ResourceEntity.class, query, getInvocationContext()).stream()
                .map(scope -> findById(scope.getId())).collect(toList());
    }

    @Override
    public List findByResourceServer(String resourceServerId) {
        DBObject query = new QueryBuilder()
                .and("resourceServerId").is(resourceServerId)
                .get();

        return getMongoStore().loadEntities(ResourceEntity.class, query, getInvocationContext()).stream()
                .map(scope -> findById(scope.getId())).collect(toList());
    }

    @Override
    public List<Resource> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
        QueryBuilder queryBuilder = new QueryBuilder()
                .and("resourceServerId").is(resourceServerId);

        attributes.forEach((name, value) -> {
            if ("scope".equals(name)) {
                queryBuilder.and("scopes").in(value);
            } else {
                queryBuilder.and(name).regex(Pattern.compile(".*" + value[0] + ".*", Pattern.CASE_INSENSITIVE));
            }
        });

        DBObject sort = new BasicDBObject("name", 1);

        return getMongoStore().loadEntities(ResourceEntity.class, queryBuilder.get(), sort, firstResult, maxResult, invocationContext).stream()
                .map(scope -> findById(scope.getId())).collect(toList());
    }

    @Override
    public List<Resource> findByScope(String... id) {
        DBObject query = new QueryBuilder()
                .and("scopes").in(id)
                .get();

        return getMongoStore().loadEntities(ResourceEntity.class, query, getInvocationContext()).stream()
                .map(policyEntity -> findById(policyEntity.getId()))
                .collect(toList());
    }

    @Override
    public Resource findByName(String name, String resourceServerId) {
        DBObject query = new QueryBuilder()
                .and("name").is(name)
                .and("resourceServerId").is(resourceServerId)
                .get();

        return getMongoStore().loadEntities(ResourceEntity.class, query, getInvocationContext()).stream()
                .map(policyEntity -> findById(policyEntity.getId())).findFirst().orElse(null);
    }

    @Override
    public List<Resource> findByType(String type) {
        DBObject query = new QueryBuilder()
                .and("type").is(type)
                .get();

        return getMongoStore().loadEntities(ResourceEntity.class, query, getInvocationContext()).stream()
                .map(policyEntity -> findById(policyEntity.getId()))
                .collect(toList());
    }

    private MongoStoreInvocationContext getInvocationContext() {
        return this.invocationContext;
    }

    private MongoStore getMongoStore() {
        return getInvocationContext().getMongoStore();
    }
}
