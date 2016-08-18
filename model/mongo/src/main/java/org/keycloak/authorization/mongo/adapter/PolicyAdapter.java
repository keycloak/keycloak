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
package org.keycloak.authorization.mongo.adapter;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.mongo.entities.PolicyEntity;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.adapters.AbstractMongoAdapter;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyAdapter extends AbstractMongoAdapter<PolicyEntity> implements Policy {

    private final PolicyEntity entity;
    private final AuthorizationProvider authorizationProvider;

    public PolicyAdapter(PolicyEntity entity, MongoStoreInvocationContext invocationContext, AuthorizationProvider authorizationProvider) {
        super(invocationContext);
        this.entity = entity;
        this.authorizationProvider = authorizationProvider;
    }

    @Override
    protected PolicyEntity getMongoEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return getMongoEntity().getId();
    }

    @Override
    public String getType() {
        return getMongoEntity().getType();
    }

    @Override
    public DecisionStrategy getDecisionStrategy() {
        return getMongoEntity().getDecisionStrategy();
    }

    @Override
    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        getMongoEntity().setDecisionStrategy(decisionStrategy);
        updateMongoEntity();
    }

    @Override
    public Logic getLogic() {
        return getMongoEntity().getLogic();
    }

    @Override
    public void setLogic(Logic logic) {
        getMongoEntity().setLogic(logic);
        updateMongoEntity();
    }

    @Override
    public Map<String, String> getConfig() {
        return getMongoEntity().getConfig();
    }

    @Override
    public void setConfig(Map<String, String> config) {
        getMongoEntity().setConfig(config);
        updateMongoEntity();
    }

    @Override
    public String getName() {
        return getMongoEntity().getName();
    }

    @Override
    public void setName(String name) {
        getMongoEntity().setName(name);
        updateMongoEntity();
    }

    @Override
    public String getDescription() {
        return getMongoEntity().getDescription();
    }

    @Override
    public void setDescription(String description) {
        getMongoEntity().setDescription(description);
        updateMongoEntity();
    }

    @Override
    public ResourceServer getResourceServer() {
        return this.authorizationProvider.getStoreFactory().getResourceServerStore().findById(getMongoEntity().getResourceServerId());
    }

    @Override
    public Set<Policy> getAssociatedPolicies() {
        return getMongoEntity().getAssociatedPolicies().stream()
                .map((Function<String, Policy>) id -> authorizationProvider.getStoreFactory().getPolicyStore().findById(id))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Resource> getResources() {
        return getMongoEntity().getResources().stream()
                .map((Function<String, Resource>) id -> authorizationProvider.getStoreFactory().getResourceStore().findById(id))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Scope> getScopes() {
        return getMongoEntity().getScopes().stream()
                .map((Function<String, Scope>) id -> authorizationProvider.getStoreFactory().getScopeStore().findById(id))
                .collect(Collectors.toSet());
    }

    @Override
    public void addScope(Scope scope) {
        getMongoEntity().addScope(scope.getId());
        updateMongoEntity();
    }

    @Override
    public void removeScope(Scope scope) {
        getMongoEntity().removeScope(scope.getId());
        updateMongoEntity();
    }

    @Override
    public void addAssociatedPolicy(Policy associatedPolicy) {
        getMongoEntity().addAssociatedPolicy(associatedPolicy.getId());
        updateMongoEntity();
    }

    @Override
    public void removeAssociatedPolicy(Policy associatedPolicy) {
        getMongoEntity().removeAssociatedPolicy(associatedPolicy.getId());
        updateMongoEntity();
    }

    @Override
    public void addResource(Resource resource) {
        getMongoEntity().addResource(resource.getId());
        updateMongoEntity();
    }

    @Override
    public void removeResource(Resource resource) {
        getMongoEntity().removeResource(resource.getId());
        updateMongoEntity();
    }
}
