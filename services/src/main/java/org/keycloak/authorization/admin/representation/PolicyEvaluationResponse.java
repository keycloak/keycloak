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
import org.keycloak.authorization.admin.util.Models;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.policy.evaluation.Result;
import org.keycloak.authorization.policy.evaluation.Result.PolicyResult;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.authorization.util.Permissions;
import org.keycloak.representations.authorization.Permission;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyEvaluationResponse {

    private List<EvaluationResultRepresentation> results;
    private boolean entitlements;
    private Effect status;

    private PolicyEvaluationResponse() {

    }

    public static PolicyEvaluationResponse build(PolicyEvaluationRequest evaluationRequest, List<Result> results, ResourceServer resourceServer, AuthorizationProvider authorization) {
        PolicyEvaluationResponse response = new PolicyEvaluationResponse();
        List<EvaluationResultRepresentation> resultsRep = new ArrayList<>();

        response.entitlements = evaluationRequest.isEntitlements();

        if (response.entitlements) {
            List<Permission> entitlements = Permissions.allPermits(results);

            if (entitlements.isEmpty()) {
                response.status = Effect.DENY;
            } else {
                StoreFactory storeFactory = authorization.getStoreFactory();

                for (Permission permission : entitlements) {
                    EvaluationResultRepresentation rep = new EvaluationResultRepresentation();

                    rep.setStatus(Effect.PERMIT);
                    resultsRep.add(rep);

                    Resource resource = storeFactory.getResourceStore().findById(permission.getResourceSetId());

                    if (resource != null) {
                        rep.setResource(Models.toRepresentation(resource, resourceServer, authorization));
                    } else {
                        ResourceRepresentation representation = new ResourceRepresentation();

                        representation.setName("Any Resource with Scopes " + permission.getScopes());

                        rep.setResource(representation);
                    }

                    rep.setScopes(permission.getScopes().stream().map(ScopeRepresentation::new).collect(Collectors.toList()));
                }
            }
        } else {
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
                    rep.setResource(Models.toRepresentation(result.getPermission().getResource(), resourceServer, authorization));
                } else {
                    ResourceRepresentation resource = new ResourceRepresentation();

                    resource.setName("Any Resource with Scopes " + result.getPermission().getScopes());

                    rep.setResource(resource);
                }

                rep.setScopes(result.getPermission().getScopes().stream().map(new Function<Scope, ScopeRepresentation>() {
                    @Override
                    public ScopeRepresentation apply(Scope scope) {
                        return Models.toRepresentation(scope, authorization);
                    }
                }).collect(Collectors.toList()));

                List<PolicyResultRepresentation> policies = new ArrayList<>();

                for (PolicyResult policy : result.getResults()) {
                    policies.add(toRepresentation(policy, authorization));
                }

                rep.setPolicies(policies);
            }
        }

        response.results = resultsRep;

        return response;
    }

    private static PolicyResultRepresentation toRepresentation(PolicyResult policy, AuthorizationProvider authorization) {
        PolicyResultRepresentation policyResultRep = new PolicyResultRepresentation();

        policyResultRep.setPolicy(Models.toRepresentation(policy.getPolicy(), authorization));
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

    public static class EvaluationResultRepresentation {

        private ResourceRepresentation resource;
        private List<ScopeRepresentation> scopes;
        private List<PolicyResultRepresentation> policies;
        private Effect status;

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
    }

    public static class PolicyResultRepresentation {

        private PolicyRepresentation policy;
        private Effect status;
        private List<PolicyResultRepresentation> associatedPolicies;

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
    }
}
