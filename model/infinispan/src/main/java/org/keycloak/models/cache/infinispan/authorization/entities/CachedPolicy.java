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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.cache.infinispan.DefaultLazyLoader;
import org.keycloak.models.cache.infinispan.LazyLoader;
import org.keycloak.models.cache.infinispan.entities.AbstractRevisioned;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedPolicy extends AbstractRevisioned implements InResourceServer {

    private final String type;
    private final DecisionStrategy decisionStrategy;
    private final Logic logic;
    private final String name;
    private final String description;
    private final String resourceServerId;
    private final LazyLoader<Policy, Set<String>> associatedPoliciesIds;
    private final LazyLoader<Policy, Set<String>> resourcesIds;
    private final LazyLoader<Policy, Set<String>> resourcesNames;
    private final LazyLoader<Policy, Set<String>> scopesIds;
    private final LazyLoader<Policy, Map<String, String>> config;
    private final String owner;

    public CachedPolicy(Long revision, Policy policy) {
        super(revision, policy.getId());
        this.type = policy.getType();
        this.decisionStrategy = policy.getDecisionStrategy();
        this.logic = policy.getLogic();
        this.name = policy.getName();
        this.description = policy.getDescription();
        this.resourceServerId = policy.getResourceServer().getId();

        this.associatedPoliciesIds = new DefaultLazyLoader<>(source -> source.getAssociatedPolicies().stream().map(Policy::getId).collect(Collectors.toSet()), Collections::emptySet);

        this.resourcesIds = new DefaultLazyLoader<>(source -> source.getResources().stream().map(Resource::getId).collect(Collectors.toSet()), Collections::emptySet);
        this.resourcesNames = new DefaultLazyLoader<>(source -> source.getResources().stream().map(Resource::getName).collect(Collectors.toSet()), Collections::emptySet);

        this.scopesIds = new DefaultLazyLoader<>(source -> source.getScopes().stream().map(Scope::getId).collect(Collectors.toSet()), Collections::emptySet);

        this.config = new DefaultLazyLoader<>(source -> new HashMap<>(source.getConfig()), Collections::emptyMap);

        this.owner = policy.getOwner();
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

    public Map<String, String> getConfig(KeycloakSession session, Supplier<Policy> policy) {
        return this.config.get(session, policy);
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public Set<String> getAssociatedPoliciesIds(KeycloakSession session, Supplier<Policy> policy) {
        return this.associatedPoliciesIds.get(session, policy);
    }

    public Set<String> getResourcesIds(KeycloakSession session, Supplier<Policy> policy) {
        return this.resourcesIds.get(session, policy);
    }

    public Set<String> getResourceNames(KeycloakSession session, Supplier<Policy> policy) {
        return this.resourcesNames.get(session, policy);
    }

    public Set<String> getScopesIds(KeycloakSession session, Supplier<Policy> policy) {
        return this.scopesIds.get(session, policy);
    }

    public String getResourceServerId() {
        return this.resourceServerId;
    }

    public String getOwner() {
        return owner;
    }
}
