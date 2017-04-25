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
package org.keycloak.authorization.admin.representation;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.policy.evaluation.Result;
import org.keycloak.authorization.util.Permissions;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.DecisionEffect;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PolicyEvaluationResponseBuilder {
    public static PolicyEvaluationResponse build(List<Result> results, ResourceServer resourceServer, AuthorizationProvider authorization, KeycloakIdentity identity) {
        PolicyEvaluationResponse response = new PolicyEvaluationResponse();
        List<PolicyEvaluationResponse.EvaluationResultRepresentation> resultsRep = new ArrayList<>();
        AccessToken accessToken = identity.getAccessToken();
        AccessToken.Authorization authorizationData = new AccessToken.Authorization();

        authorizationData.setPermissions(Permissions.permits(results, authorization, resourceServer.getId()));
        accessToken.setAuthorization(authorizationData);

        response.setRpt(accessToken);

        if (results.stream().anyMatch(evaluationResult -> evaluationResult.getEffect().equals(Decision.Effect.DENY))) {
            response.setStatus(DecisionEffect.DENY);
        } else {
            response.setStatus(DecisionEffect.PERMIT);
        }

        for (Result result : results) {
            PolicyEvaluationResponse.EvaluationResultRepresentation rep = new PolicyEvaluationResponse.EvaluationResultRepresentation();

            if (result.getEffect() == Decision.Effect.DENY) {
                rep.setStatus(DecisionEffect.DENY);
            } else {
                rep.setStatus(DecisionEffect.PERMIT);

            }
            resultsRep.add(rep);

            if (result.getPermission().getResource() != null) {
                ResourceRepresentation resource = new ResourceRepresentation();

                resource.setId(result.getPermission().getResource().getId());
                resource.setName(result.getPermission().getResource().getName());

                rep.setResource(resource);
            } else {
                ResourceRepresentation resource = new ResourceRepresentation();

                resource.setName("Any Resource with Scopes " + result.getPermission().getScopes().stream().map(Scope::getName).collect(Collectors.toList()));

                rep.setResource(resource);
            }

            rep.setScopes(result.getPermission().getScopes().stream().map(scope -> {
                ScopeRepresentation representation = new ScopeRepresentation();

                representation.setId(scope.getId());
                representation.setName(scope.getName());

                return representation;
            }).collect(Collectors.toList()));

            List<PolicyEvaluationResponse.PolicyResultRepresentation> policies = new ArrayList<>();

            for (Result.PolicyResult policy : result.getResults()) {
                policies.add(toRepresentation(policy, authorization));
            }

            rep.setPolicies(policies);
        }

        resultsRep.sort(Comparator.comparing(o -> o.getResource().getName()));

        Map<String, PolicyEvaluationResponse.EvaluationResultRepresentation> groupedResults = new HashMap<>();

        resultsRep.forEach(evaluationResultRepresentation -> {
            PolicyEvaluationResponse.EvaluationResultRepresentation result = groupedResults.get(evaluationResultRepresentation.getResource().getId());
            ResourceRepresentation resource = evaluationResultRepresentation.getResource();

            if (result == null) {
                groupedResults.put(resource.getId(), evaluationResultRepresentation);
                result = evaluationResultRepresentation;
            }

            if (result.getStatus().equals(DecisionEffect.PERMIT) || (evaluationResultRepresentation.getStatus().equals(DecisionEffect.PERMIT) && result.getStatus().equals(DecisionEffect.DENY))) {
                result.setStatus(DecisionEffect.PERMIT);
            }

            List<ScopeRepresentation> scopes = result.getScopes();

            if (scopes == null) {
                scopes = new ArrayList<>();
                result.setScopes(scopes);
            }

            List<ScopeRepresentation> currentScopes = evaluationResultRepresentation.getScopes();

            if (currentScopes != null) {
                List<ScopeRepresentation> allowedScopes = result.getAllowedScopes();
                for (ScopeRepresentation scope : currentScopes) {
                    if (!scopes.contains(scope)) {
                        scopes.add(scope);
                    }
                    if (evaluationResultRepresentation.getStatus().equals(Decision.Effect.PERMIT)) {
                        if (!allowedScopes.contains(scope)) {
                            allowedScopes.add(scope);
                        }
                    } else {
                        evaluationResultRepresentation.getPolicies().forEach(new Consumer<PolicyEvaluationResponse.PolicyResultRepresentation>() {
                            @Override
                            public void accept(PolicyEvaluationResponse.PolicyResultRepresentation policyResultRepresentation) {
                                if (policyResultRepresentation.getStatus().equals(Decision.Effect.PERMIT)) {
                                    if (!allowedScopes.contains(scope)) {
                                        allowedScopes.add(scope);
                                    }
                                }
                            }
                        });
                    }
                }
                result.setAllowedScopes(allowedScopes);
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

            List<PolicyEvaluationResponse.PolicyResultRepresentation> policies = result.getPolicies();

            for (PolicyEvaluationResponse.PolicyResultRepresentation policy : new ArrayList<>(evaluationResultRepresentation.getPolicies())) {
                if (!policies.contains(policy)) {
                    policies.add(policy);
                } else {
                    policy = policies.get(policies.indexOf(policy));
                }

                if (policy.getStatus().equals(Decision.Effect.DENY)) {
                    Policy policyModel = authorization.getStoreFactory().getPolicyStore().findById(policy.getPolicy().getId(), resourceServer.getId());
                    for (ScopeRepresentation scope : policyModel.getScopes().stream().map(scopeModel -> ModelToRepresentation.toRepresentation(scopeModel, authorization)).collect(Collectors.toList())) {
                        if (!policy.getScopes().contains(scope) && policyModel.getScopes().stream().filter(policyScope -> policyScope.getId().equals(scope.getId())).findFirst().isPresent()) {
                            result.getAllowedScopes().remove(scope);
                            policy.getScopes().add(scope);
                        }
                    }
                } else {}
            }
        });

        response.setResults(groupedResults.values().stream().collect(Collectors.toList()));

        return response;
    }

    private static PolicyEvaluationResponse.PolicyResultRepresentation toRepresentation(Result.PolicyResult policy, AuthorizationProvider authorization) {
        PolicyEvaluationResponse.PolicyResultRepresentation policyResultRep = new PolicyEvaluationResponse.PolicyResultRepresentation();

        PolicyRepresentation representation = new PolicyRepresentation();

        representation.setId(policy.getPolicy().getId());
        representation.setName(policy.getPolicy().getName());
        representation.setType(policy.getPolicy().getType());
        representation.setDecisionStrategy(policy.getPolicy().getDecisionStrategy());

        policyResultRep.setPolicy(representation);
        if (policy.getStatus() == Decision.Effect.DENY) {
            policyResultRep.setStatus(DecisionEffect.DENY);
        } else {
            policyResultRep.setStatus(DecisionEffect.PERMIT);

        }

        policyResultRep.setAssociatedPolicies(policy.getAssociatedPolicies().stream().map(result -> toRepresentation(result, authorization)).collect(Collectors.toList()));

        return policyResultRep;
    }
}
