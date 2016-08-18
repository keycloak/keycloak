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

package org.keycloak.authorization.admin.representation;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision.Effect;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.policy.evaluation.Result;
import org.keycloak.authorization.policy.evaluation.Result.PolicyResult;
import org.keycloak.authorization.util.Permissions;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyEvaluationResponse {

    private List<EvaluationResultRepresentation> results;
    private boolean entitlements;
    private Effect status;
    private AccessToken rpt;

    private PolicyEvaluationResponse() {

    }

    public static PolicyEvaluationResponse build(List<Result> results, ResourceServer resourceServer, AuthorizationProvider authorization, KeycloakIdentity identity) {
        PolicyEvaluationResponse response = new PolicyEvaluationResponse();
        List<EvaluationResultRepresentation> resultsRep = new ArrayList<>();
        AccessToken accessToken = identity.getAccessToken();
        AccessToken.Authorization authorizationData = new AccessToken.Authorization();

        authorizationData.setPermissions(Permissions.allPermits(results, authorization));
        accessToken.setAuthorization(authorizationData);

        response.rpt = accessToken;

        if (results.stream().anyMatch(evaluationResult -> evaluationResult.getEffect().equals(Effect.DENY))) {
            response.status = Effect.DENY;
        } else {
            response.status = Effect.PERMIT;
        }

        for (Result result : results) {
            EvaluationResultRepresentation rep = new EvaluationResultRepresentation();

            rep.setStatus(result.getEffect());
            resultsRep.add(rep);

            if (result.getPermission().getResource() != null) {
                rep.setResource(ModelToRepresentation.toRepresentation(result.getPermission().getResource(), resourceServer, authorization));
            } else {
                ResourceRepresentation resource = new ResourceRepresentation();

                resource.setName("Any Resource with Scopes " + result.getPermission().getScopes().stream().map(Scope::getName).collect(Collectors.toList()));

                rep.setResource(resource);
            }

            rep.setScopes(result.getPermission().getScopes().stream().map(scope -> ModelToRepresentation.toRepresentation(scope, authorization)).collect(Collectors.toList()));

            List<PolicyResultRepresentation> policies = new ArrayList<>();

            for (PolicyResult policy : result.getResults()) {
                policies.add(toRepresentation(policy, authorization));
            }

            rep.setPolicies(policies);
        }

        resultsRep.sort((o1, o2) -> o1.getResource().getName().compareTo(o2.getResource().getName()));

        Map<String, EvaluationResultRepresentation> groupedResults = new HashMap<>();

        resultsRep.forEach(evaluationResultRepresentation -> {
            EvaluationResultRepresentation result = groupedResults.get(evaluationResultRepresentation.getResource().getId());
            ResourceRepresentation resource = evaluationResultRepresentation.getResource();

            if (result == null) {
                groupedResults.put(resource.getId(), evaluationResultRepresentation);
                result = evaluationResultRepresentation;
            }

            if (result.getStatus().equals(Effect.PERMIT) || (evaluationResultRepresentation.getStatus().equals(Effect.PERMIT) && result.getStatus().equals(Effect.DENY))) {
                result.setStatus(Effect.PERMIT);
            }

            List<ScopeRepresentation> scopes = result.getScopes();

            if (scopes == null) {
                scopes = new ArrayList<>();
                result.setScopes(scopes);
            }

            List<ScopeRepresentation> currentScopes = evaluationResultRepresentation.getScopes();

            if (currentScopes != null) {
                for (ScopeRepresentation scope : currentScopes) {
                    if (!scopes.contains(scope)) {
                        scopes.add(scope);
                    }
                    if (evaluationResultRepresentation.getStatus().equals(Effect.PERMIT)) {
                        result.getAllowedScopes().add(scope);
                    }
                }
            }

            if (resource.getId() != null) {
                if (!scopes.isEmpty()) {
                    result.getResource().setName(evaluationResultRepresentation.getResource().getName() + " with scopes " + scopes.stream().flatMap((Function<ScopeRepresentation, Stream<?>>) scopeRepresentation -> Arrays.asList(scopeRepresentation.getName()).stream()).collect(Collectors.toList()));
                } else {
                    result.getResource().setName(evaluationResultRepresentation.getResource().getName());
                }
            } else {
                result.getResource().setName("Any Resource with Scopes " + scopes.stream().flatMap((Function<ScopeRepresentation, Stream<?>>) scopeRepresentation -> Arrays.asList(scopeRepresentation.getName()).stream()).collect(Collectors.toList()));
            }

            List<PolicyResultRepresentation> policies = result.getPolicies();

            for (PolicyResultRepresentation policy : new ArrayList<>(evaluationResultRepresentation.getPolicies())) {
                if (!policies.contains(policy)) {
                    policies.add(policy);
                } else {
                    policy = policies.get(policies.indexOf(policy));
                }

                if (policy.getStatus().equals(Effect.DENY)) {
                    Policy policyModel = authorization.getStoreFactory().getPolicyStore().findById(policy.getPolicy().getId());
                    for (ScopeRepresentation scope : policyModel.getScopes().stream().map(scopeModel -> ModelToRepresentation.toRepresentation(scopeModel, authorization)).collect(Collectors.toList())) {
                        if (!policy.getScopes().contains(scope)) {
                            policy.getScopes().add(scope);
                        }
                    }
                    for (ScopeRepresentation scope : currentScopes) {
                        if (!policy.getScopes().contains(scope)) {
                            policy.getScopes().add(scope);
                        }
                    }
                }
            }
        });

        response.results = groupedResults.values().stream().collect(Collectors.toList());

        return response;
    }

    private static PolicyResultRepresentation toRepresentation(PolicyResult policy, AuthorizationProvider authorization) {
        PolicyResultRepresentation policyResultRep = new PolicyResultRepresentation();

        policyResultRep.setPolicy(ModelToRepresentation.toRepresentation(policy.getPolicy(), authorization));
        policyResultRep.setStatus(policy.getStatus());
        policyResultRep.setAssociatedPolicies(policy.getAssociatedPolicies().stream().map(result -> toRepresentation(result, authorization)).collect(Collectors.toList()));

        return policyResultRep;
    }

    public List<EvaluationResultRepresentation> getResults() {
        return results;
    }

    public Effect getStatus() {
        return status;
    }

    public boolean isEntitlements() {
        return entitlements;
    }

    public AccessToken getRpt() {
        return rpt;
    }

    public static class EvaluationResultRepresentation {

        private ResourceRepresentation resource;
        private List<ScopeRepresentation> scopes;
        private List<PolicyResultRepresentation> policies;
        private Effect status;
        private List<ScopeRepresentation> allowedScopes = new ArrayList<>();

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

        public void setPolicies(final List<PolicyResultRepresentation> policies) {
            this.policies = policies;
        }

        public List<PolicyResultRepresentation> getPolicies() {
            return policies;
        }

        public void setStatus(final Effect status) {
            this.status = status;
        }

        public Effect getStatus() {
            return status;
        }

        public void setAllowedScopes(List<ScopeRepresentation> allowedScopes) {
            this.allowedScopes = allowedScopes;
        }

        public List<ScopeRepresentation> getAllowedScopes() {
            return allowedScopes;
        }
    }

    public static class PolicyResultRepresentation {

        private PolicyRepresentation policy;
        private Effect status;
        private List<PolicyResultRepresentation> associatedPolicies;
        private List<ScopeRepresentation> scopes = new ArrayList<>();

        public PolicyRepresentation getPolicy() {
            return policy;
        }

        public void setPolicy(final PolicyRepresentation policy) {
            this.policy = policy;
        }

        public Effect getStatus() {
            return status;
        }

        public void setStatus(final Effect status) {
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
            return this.policy.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final PolicyResultRepresentation policy = (PolicyResultRepresentation) o;
            return this.policy.equals(policy.getPolicy());
        }

        public void setScopes(List<ScopeRepresentation> scopes) {
            this.scopes = scopes;
        }

        public List<ScopeRepresentation> getScopes() {
            return scopes;
        }
    }
}
