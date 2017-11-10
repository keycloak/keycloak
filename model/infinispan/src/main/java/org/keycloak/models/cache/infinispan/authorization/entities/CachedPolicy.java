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

package org.keycloak.models.cache.infinispan.authorization.entities;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.Scope;
import org.keycloak.models.cache.infinispan.entities.AbstractRevisioned;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedPolicy extends AbstractRevisioned implements InResourceServer {

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

    public CachedPolicy(Long revision, Policy policy) {
        super(revision, policy.getId());
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

    public String getType() {
        return this.type;
    }

    public DecisionStrategy getDecisionStrategy() {
        return this.decisionStrategy;
    }

    public Logic getLogic() {
        return this.logic;
    }

    public Map<String, String> getConfig() {
        return this.config;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
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

}
