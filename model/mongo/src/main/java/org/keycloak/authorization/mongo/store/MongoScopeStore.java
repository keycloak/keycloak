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
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.mongo.adapter.ScopeAdapter;
import org.keycloak.authorization.mongo.entities.ScopeEntity;
import org.keycloak.authorization.store.ScopeStore;
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
public class MongoScopeStore implements ScopeStore {

    private final MongoStoreInvocationContext invocationContext;
    private final AuthorizationProvider authorizationProvider;

    public MongoScopeStore(MongoStoreInvocationContext invocationContext, AuthorizationProvider authorizationProvider) {
        this.invocationContext = invocationContext;
        this.authorizationProvider = authorizationProvider;
    }

    @Override
    public Scope create(final String name, final ResourceServer resourceServer) {
        ScopeEntity entity = new ScopeEntity();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setName(name);
        entity.setResourceServerId(resourceServer.getId());

        getMongoStore().insertEntity(entity, getInvocationContext());

        return new ScopeAdapter(entity, getInvocationContext(), this.authorizationProvider);
    }

    @Override
    public void delete(String id) {
        getMongoStore().removeEntity(ScopeEntity.class, id, getInvocationContext());
    }

    @Override
    public Scope findById(String id) {
        ScopeEntity entity = getMongoStore().loadEntity(ScopeEntity.class, id, getInvocationContext());

        if (entity == null) {
            return null;
        }

        return new ScopeAdapter(entity, getInvocationContext(), this.authorizationProvider);
    }

    @Override
    public Scope findByName(String name, String resourceServerId) {
        DBObject query = new QueryBuilder()
                .and("resourceServerId").is(resourceServerId)
                .and("name").is(name)
                .get();

        return getMongoStore().loadEntities(ScopeEntity.class, query, getInvocationContext()).stream()
                .map(scope -> findById(scope.getId())).findFirst().orElse(null);
    }

    @Override
    public List<Scope> findByResourceServer(String resourceServerId) {
        DBObject query = new QueryBuilder()
                .and("resourceServerId").is(resourceServerId)
                .get();

        return getMongoStore().loadEntities(ScopeEntity.class, query, getInvocationContext()).stream()
                .map(policyEntity -> findById(policyEntity.getId()))
                .collect(toList());
    }

    @Override
    public List<Scope> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
        QueryBuilder queryBuilder = new QueryBuilder()
                .and("resourceServerId").is(resourceServerId);

        attributes.forEach((name, value) -> {
            queryBuilder.and(name).regex(Pattern.compile(".*" + value[0] + ".*", Pattern.CASE_INSENSITIVE));
        });

        DBObject sort = new BasicDBObject("name", 1);

        return getMongoStore().loadEntities(ScopeEntity.class, queryBuilder.get(), sort, firstResult, maxResult, invocationContext).stream()
                .map(scope -> findById(scope.getId())).collect(toList());
    }

    private MongoStoreInvocationContext getInvocationContext() {
        return this.invocationContext;
    }

    private MongoStore getMongoStore() {
        return getInvocationContext().getMongoStore();
    }
}
