/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authorization.fgap.evaluation.partial;

import java.util.Map;
import java.util.Set;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;

/**
 * A read-only view of a {@link Policy} that exposes only a subset of its scopes, so that a permission inherited from an
 * ancestor group only decides the scopes that are not already decided by a more specific group.
 *
 * <p>It is equal to the policy it wraps, so that evaluation caches keyed by policy are shared.
 */
final class ScopeRestrictedPolicy implements Policy {

    private final Policy delegate;
    private final Set<Scope> scopes;

    ScopeRestrictedPolicy(Policy delegate, Set<Scope> scopes) {
        this.delegate = delegate;
        this.scopes = Set.copyOf(scopes);
    }

    @Override
    public Set<Scope> getScopes() {
        return scopes;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getType() {
        return delegate.getType();
    }

    @Override
    public DecisionStrategy getDecisionStrategy() {
        return delegate.getDecisionStrategy();
    }

    @Override
    public Logic getLogic() {
        return delegate.getLogic();
    }

    @Override
    public Map<String, String> getConfig() {
        return delegate.getConfig();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public ResourceServer getResourceServer() {
        return delegate.getResourceServer();
    }

    @Override
    public Set<Policy> getAssociatedPolicies() {
        return delegate.getAssociatedPolicies();
    }

    @Override
    public Set<Resource> getResources() {
        return delegate.getResources();
    }

    @Override
    public String getOwner() {
        return delegate.getOwner();
    }

    @Override
    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        throw readOnly();
    }

    @Override
    public void setLogic(Logic logic) {
        throw readOnly();
    }

    @Override
    public void setConfig(Map<String, String> config) {
        throw readOnly();
    }

    @Override
    public void removeConfig(String name) {
        throw readOnly();
    }

    @Override
    public void putConfig(String name, String value) {
        throw readOnly();
    }

    @Override
    public void setName(String name) {
        throw readOnly();
    }

    @Override
    public void setDescription(String description) {
        throw readOnly();
    }

    @Override
    public void setOwner(String owner) {
        throw readOnly();
    }

    @Override
    public void addScope(Scope scope) {
        throw readOnly();
    }

    @Override
    public void removeScope(Scope scope) {
        throw readOnly();
    }

    @Override
    public void addAssociatedPolicy(Policy associatedPolicy) {
        throw readOnly();
    }

    @Override
    public void removeAssociatedPolicy(Policy associatedPolicy) {
        throw readOnly();
    }

    @Override
    public void addResource(Resource resource) {
        throw readOnly();
    }

    @Override
    public void removeResource(Resource resource) {
        throw readOnly();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Policy that)) return false;
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    private UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException("Policy is read-only during evaluation");
    }
}
