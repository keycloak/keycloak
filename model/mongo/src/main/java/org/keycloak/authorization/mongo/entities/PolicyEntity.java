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

package org.keycloak.authorization.mongo.entities;

import org.keycloak.connections.mongo.api.MongoCollection;
import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.entities.AbstractIdentifiableEntity;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@MongoCollection(collectionName = "policies")
public class PolicyEntity extends AbstractIdentifiableEntity implements MongoIdentifiableEntity {

    private String name;

    private String description;

    private String type;

    private DecisionStrategy decisionStrategy = DecisionStrategy.UNANIMOUS;

    private Logic logic = Logic.POSITIVE;

    private Map<String, String> config = new HashMap();

    private String resourceServerId;

    private Set<String> associatedPolicies = new HashSet<>();

    private Set<String> resources = new HashSet<>();

    private Set<String> scopes = new HashSet<>();

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public DecisionStrategy getDecisionStrategy() {
        return this.decisionStrategy;
    }

    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        this.decisionStrategy = decisionStrategy;
    }

    public Logic getLogic() {
        return this.logic;
    }

    public void setLogic(Logic logic) {
        this.logic = logic;
    }

    public Map<String, String> getConfig() {
        return this.config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResourceServerId() {
        return this.resourceServerId;
    }

    public void setResourceServerId(String resourceServerId) {
        this.resourceServerId = resourceServerId;
    }

    public Set<String> getAssociatedPolicies() {
        return this.associatedPolicies;
    }

    public void setAssociatedPolicies(Set<String> associatedPolicies) {
        this.associatedPolicies = associatedPolicies;
    }

    public Set<String> getResources() {
        return this.resources;
    }

    public void setResources(Set<String> resources) {
        this.resources = resources;
    }

    public Set<String> getScopes() {
        return this.scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public void addScope(String scopeId) {
        getScopes().add(scopeId);
    }

    public void removeScope(String scopeId) {
        getScopes().remove(scopeId);
    }

    public void addAssociatedPolicy(String policyId) {
        getAssociatedPolicies().add(policyId);
    }

    public void removeAssociatedPolicy(String policyId) {
        getAssociatedPolicies().remove(policyId);
    }

    public void addResource(String resourceId) {
        getResources().add(resourceId);
    }

    public void removeResource(String resourceId) {
        getResources().remove(resourceId);
    }

    public void afterRemove(MongoStoreInvocationContext invocationContext) {

    }
}
