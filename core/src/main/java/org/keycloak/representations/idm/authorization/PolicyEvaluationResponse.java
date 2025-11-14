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

package org.keycloak.representations.idm.authorization;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.representations.AccessToken;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyEvaluationResponse {

    private List<EvaluationResultRepresentation> results;
    private boolean entitlements;
    private DecisionEffect status;
    private AccessToken rpt;

    public List<EvaluationResultRepresentation> getResults() {
        return results;
    }

    public DecisionEffect getStatus() {
        return status;
    }

    public boolean isEntitlements() {
        return entitlements;
    }

    public AccessToken getRpt() {
        return rpt;
    }

    public void setResults(List<EvaluationResultRepresentation> results) {
        this.results = results;
    }

    public void setEntitlements(boolean entitlements) {
        this.entitlements = entitlements;
    }

    public void setStatus(DecisionEffect status) {
        this.status = status;
    }

    public void setRpt(AccessToken rpt) {
        this.rpt = rpt;
    }

    public static class EvaluationResultRepresentation {

        private ResourceRepresentation resource;
        private List<ScopeRepresentation> scopes;
        private Set<PolicyResultRepresentation> policies;
        private DecisionEffect status;
        private Set<ScopeRepresentation> allowedScopes = new HashSet<>();
        private Set<ScopeRepresentation> deniedScopes = new HashSet<>();

        public void setResource(final ResourceRepresentation resource) {
            this.resource = resource;
        }

        public ResourceRepresentation getResource() {
            return resource;
        }

        public void setScopes(List<ScopeRepresentation> scopes) {
            this.scopes = scopes;
        }

        public List<ScopeRepresentation> getScopes() {
            return scopes;
        }

        public void setPolicies(final Set<PolicyResultRepresentation> policies) {
            this.policies = policies;
        }

        public Set<PolicyResultRepresentation> getPolicies() {
            return policies;
        }

        public void setStatus(final DecisionEffect status) {
            this.status = status;
        }

        public DecisionEffect getStatus() {
            return status;
        }

        public void setAllowedScopes(Set<ScopeRepresentation> allowedScopes) {
            this.allowedScopes = allowedScopes;
        }

        public Set<ScopeRepresentation> getAllowedScopes() {
            return allowedScopes;
        }

        public void setDeniedScopes(Set<ScopeRepresentation> deniedScopes) {
            this.deniedScopes = deniedScopes;
        }

        public Set<ScopeRepresentation> getDeniedScopes() {
            return deniedScopes;
        }
    }

    public static class PolicyResultRepresentation {

        private PolicyRepresentation policy;
        private DecisionEffect status;
        private List<PolicyResultRepresentation> associatedPolicies;
        private Set<String> scopes = new HashSet<>();
        private String resourceType;

        public PolicyRepresentation getPolicy() {
            return policy;
        }

        public void setPolicy(final PolicyRepresentation policy) {
            this.policy = policy;
        }

        public DecisionEffect getStatus() {
            return status;
        }

        public void setStatus(final DecisionEffect status) {
            this.status = status;
        }

        public List<PolicyResultRepresentation> getAssociatedPolicies() {
            return associatedPolicies;
        }

        public void setAssociatedPolicies(final List<PolicyResultRepresentation> associatedPolicies) {
            this.associatedPolicies = associatedPolicies;
        }

        @Override
        public int hashCode() {
            return this.policy.getName().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final PolicyResultRepresentation policy = (PolicyResultRepresentation) o;
            return this.policy.getName().equals(policy.getPolicy().getName());
        }

        public void setScopes(Set<String> scopes) {
            this.scopes = scopes;
        }

        public Set<String> getScopes() {
            return scopes;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getResourceType() {
            return resourceType;
        }
    }
}
