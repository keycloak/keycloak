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
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.mongo.adapter.PolicyAdapter;
import org.keycloak.authorization.mongo.entities.PolicyEntity;
import org.keycloak.authorization.store.PolicyStore;
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
public class MongoPolicyStore implements PolicyStore {

    private final MongoStoreInvocationContext invocationContext;
    private final AuthorizationProvider authorizationProvider;

    public MongoPolicyStore(MongoStoreInvocationContext invocationContext, AuthorizationProvider authorizationProvider) {
        this.invocationContext = invocationContext;
        this.authorizationProvider = authorizationProvider;
    }

    @Override
    public Policy create(String name, String type, ResourceServer resourceServer) {
        PolicyEntity entity = new PolicyEntity();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setName(name);
        entity.setType(type);
        entity.setResourceServerId(resourceServer.getId());

        getMongoStore().insertEntity(entity, getInvocationContext());

        return new PolicyAdapter(entity, getInvocationContext(), this.authorizationProvider) ;
    }

    @Override
    public void delete(String id) {
        getMongoStore().removeEntity(PolicyEntity.class, id, getInvocationContext());
    }

    @Override
    public Policy findById(String id) {
        PolicyEntity entity = getMongoStore().loadEntity(PolicyEntity.class, id, getInvocationContext());

        if (entity == null) {
            return null;
        }

        return new PolicyAdapter(entity, getInvocationContext(), this.authorizationProvider);
    }


    @Override
    public Policy findByName(String name, String resourceServerId) {
        DBObject query = new QueryBuilder()
                .and("resourceServerId").is(resourceServerId)
                .and("name").is(name)
                .get();

        return getMongoStore().loadEntities(PolicyEntity.class, query, getInvocationContext()).stream()
                .map(policyEntity -> findById(policyEntity.getId())).findFirst().orElse(null);
    }

    @Override
    public List<Policy> findByResourceServer(String resourceServerId) {
        DBObject query = new QueryBuilder()
                .and("resourceServerId").is(resourceServerId)
                .get();

        return getMongoStore().loadEntities(PolicyEntity.class, query, getInvocationContext()).stream()
                .map(policyEntity -> findById(policyEntity.getId()))
                .collect(toList());
    }

    @Override
    public List<Policy> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
        QueryBuilder queryBuilder = new QueryBuilder()
                .and("resourceServerId").is(resourceServerId);

        attributes.forEach((name, value) -> {
            if ("permission".equals(name)) {
                if (Boolean.valueOf(value[0])) {
                    queryBuilder.and("type").in(new String[] {"resource", "scope"});
                } else {
                    queryBuilder.and("type").notIn(new String[] {"resource", "scope"});
                }
            } else if ("id".equals(name)) {
                queryBuilder.and("_id").in(value);
            } else {
                queryBuilder.and(name).regex(Pattern.compile(".*" + value[0] + ".*", Pattern.CASE_INSENSITIVE));
            }
        });

        DBObject sort = new BasicDBObject("name", 1);

        return getMongoStore().loadEntities(PolicyEntity.class, queryBuilder.get(), sort, firstResult, maxResult, invocationContext).stream()
                .map(policy -> findById(policy.getId())).collect(toList());
    }

    @Override
    public List<Policy> findByResource(String resourceId) {
        DBObject query = new QueryBuilder()
                .and("resources").is(resourceId)
                .get();

        return getMongoStore().loadEntities(PolicyEntity.class, query, getInvocationContext()).stream()
                .map(policyEntity -> findById(policyEntity.getId()))
                .collect(toList());
    }

    @Override
    public List<Policy> findByResourceType(String resourceType, String resourceServerId) {
        DBObject query = new QueryBuilder()
                .and("resourceServerId").is(resourceServerId)
                .get();

        return getMongoStore().loadEntities(PolicyEntity.class, query, getInvocationContext()).stream()
                .filter(policyEntity -> {
                    String defaultResourceType = policyEntity.getConfig().get("defaultResourceType");
                    return defaultResourceType != null && defaultResourceType.equals(resourceType);
                })
                .map(policyEntity -> findById(policyEntity.getId()))
                .collect(toList());
    }

    @Override
    public List<Policy> findByScopeIds(List<String> scopeIds, String resourceServerId) {
        DBObject query = new QueryBuilder()
                .and("resourceServerId").is(resourceServerId)
                .and("scopes").in(scopeIds)
                .get();

        return getMongoStore().loadEntities(PolicyEntity.class, query, getInvocationContext()).stream()
                .map(policyEntity -> findById(policyEntity.getId()))
                .collect(toList());
    }

    @Override
    public List<Policy> findByType(String type) {
        DBObject query = new QueryBuilder()
                .and("type").is(type)
                .get();

        return getMongoStore().loadEntities(PolicyEntity.class, query, getInvocationContext()).stream()
                .map(policyEntity -> findById(policyEntity.getId()))
                .collect(toList());
    }

    @Override
    public List<Policy> findDependentPolicies(String policyId) {
        DBObject query = new QueryBuilder()
                .and("associatedPolicies").is(policyId)
                .get();

        return getMongoStore().loadEntities(PolicyEntity.class, query, getInvocationContext()).stream()
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
