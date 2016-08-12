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

package org.keycloak.models.authorization.infinispan.entities;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedPolicy implements Policy, Serializable {

    private static final long serialVersionUID = -144247681046298128L;

    private String id;
    private String type;
    private DecisionStrategy decisionStrategy;
    private Logic logic;
    private Map<String, String> config;
    private String name;
    private String description;
    private String resourceServerId;
    private Set<String> associatedPoliciesIds;
    private Set<String> resourcesIds;
    private Set<String> scopesIds;

    public CachedPolicy(Policy policy) {
        this.id = policy.getId();
        this.type = policy.getType();
        this.decisionStrategy = policy.getDecisionStrategy();
        this.logic = policy.getLogic();
        this.config = new HashMap(policy.getConfig());
        this.name = policy.getName();
        this.description = policy.getDescription();
        this.resourceServerId = policy.getResourceServer().getId();
        this.associatedPoliciesIds = policy.getAssociatedPolicies().stream().map(Policy::getId).collect(Collectors.toSet());
        this.resourcesIds = policy.getResources().stream().map(Resource::getId).collect(Collectors.toSet());
        this.scopesIds = policy.getScopes().stream().map(Scope::getId).collect(Collectors.toSet());
    }

    public CachedPolicy(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public DecisionStrategy getDecisionStrategy() {
        return this.decisionStrategy;
    }

    @Override
    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        this.decisionStrategy = decisionStrategy;
    }

    @Override
    public Logic getLogic() {
        return this.logic;
    }

    @Override
    public void setLogic(Logic logic) {
        this.logic = logic;
    }

    @Override
    public Map<String, String> getConfig() {
        return this.config;
    }

    @Override
    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public ResourceServer getResourceServer() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void addScope(Scope scope) {
        this.scopesIds.add(scope.getId());
    }

    @Override
    public void removeScope(Scope scope) {
        this.scopesIds.remove(scope.getId());
    }

    @Override
    public void addAssociatedPolicy(Policy associatedPolicy) {
        this.associatedPoliciesIds.add(associatedPolicy.getId());
    }

    @Override
    public void removeAssociatedPolicy(Policy associatedPolicy) {
        this.associatedPoliciesIds.remove(associatedPolicy.getId());
    }

    @Override
    public void addResource(Resource resource) {
        this.resourcesIds.add(resource.getId());
    }

    @Override
    public void removeResource(Resource resource) {
        this.resourcesIds.add(resource.getId());
    }

    @Override
    public Set<Policy> getAssociatedPolicies() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Set<Resource> getResources() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Set<Scope> getScopes() {
        throw new RuntimeException("Not implemented");
    }

    public Set<String> getAssociatedPoliciesIds() {
        return this.associatedPoliciesIds;
    }

    public Set<String> getResourcesIds() {
        return this.resourcesIds;
    }

    public Set<String> getScopesIds() {
        return this.scopesIds;
    }

    public String getResourceServerId() {
        return this.resourceServerId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (this.id == null) return false;

        if (o == null || getClass() != o.getClass()) return false;

        Policy that = (Policy) o;

        if (!getId().equals(that.getId())) return false;

        return true;

    }

    @Override
    public int hashCode() {
        return id!=null ? id.hashCode() : super.hashCode();
    }
}
