/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision.Effect;
import org.keycloak.authorization.admin.PolicyEvaluationService.EvaluationDecisionCollector;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.policy.evaluation.Result;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.authorization.DecisionEffect;
import org.keycloak.representations.idm.authorization.PolicyEvaluationRequest;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse.PolicyResultRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
class FGAPPolicyEvaluationResponseBuilder {

    static PolicyEvaluationResponse build(EvaluationDecisionCollector decision, ResourceServer resourceServer, AuthorizationProvider authorization, PolicyEvaluationRequest request) {
        PolicyEvaluationResponse response = new PolicyEvaluationResponse();
        Collection<Result> results = decision.getResults();

        if (results.isEmpty()) {
            response.setResults(List.of());
            response.setStatus(DecisionEffect.DENY);
            return response;
        }

        Result result = results.iterator().next();
        PolicyEvaluationResponse.EvaluationResultRepresentation rep = new PolicyEvaluationResponse.EvaluationResultRepresentation();

        if (Effect.PERMIT.equals(result.getEffect())) {
            response.setStatus(DecisionEffect.PERMIT);
            rep.setStatus(DecisionEffect.PERMIT);
        } else {
            response.setStatus(DecisionEffect.DENY);
            rep.setStatus(DecisionEffect.DENY);
        }

        Resource resource = result.getPermission().getResource();
        ResourceRepresentation resourceRep = new ResourceRepresentation();

        resourceRep.setId(resource.getId());
        resourceRep.setName(resource.getName());

        rep.setResource(resourceRep);

        rep.setScopes(result.getPermission().getScopes().stream().map(scope -> {
            ScopeRepresentation representation = new ScopeRepresentation();

            representation.setId(scope.getId());
            representation.setName(scope.getName());

            return representation;
        }).collect(Collectors.toList()));

        Set<PolicyResultRepresentation> policies = new HashSet<>();

        for (Result.PolicyResult policy : result.getResults()) {
            PolicyResultRepresentation policyRep = toRepresentation(policy);

            if (Effect.PERMIT.equals(policy.getEffect())) {
                policyRep.setStatus(DecisionEffect.PERMIT);
            } else {
                policyRep.setStatus(DecisionEffect.DENY);
            }

            policyRep.setScopes(policy.getPolicy().getScopes().stream().map(Scope::getName).collect(Collectors.toSet()));
            policyRep.setResourceType(policy.getPolicy().getResourceType());

            policies.add(policyRep);
        }

        rep.setPolicies(policies);

        Set<ScopeRepresentation> scopes = result.getPermission().getScopes().stream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toSet());

        if (DecisionEffect.PERMIT.equals(rep.getStatus())) {
            rep.setAllowedScopes(scopes);
        } else {
            rep.setDeniedScopes(scopes);
        }

        rep.getAllowedScopes().removeAll(rep.getDeniedScopes());
        rep.getDeniedScopes().addAll(request.getResources().get(0).getScopes());
        rep.getDeniedScopes().removeAll(rep.getAllowedScopes());

        String resourceName = AdminPermissionsSchema.SCHEMA.getResourceName(authorization.getKeycloakSession(), resourceServer, request.getResourceType(), rep.getResource().getName());

        rep.getResource().setName(resourceName + " with scopes " + rep.getScopes().stream().flatMap((Function<ScopeRepresentation, Stream<?>>) scopeRepresentation -> Stream.of(scopeRepresentation.getName())).sorted().toList());
        rep.getPolicies().addAll(rep.getPolicies());

        response.setResults(List.of(rep));

        return response;
    }

    private static PolicyResultRepresentation toRepresentation(Result.PolicyResult result) {
        PolicyResultRepresentation policyResultRep = new PolicyResultRepresentation();

        PolicyRepresentation representation = new PolicyRepresentation();
        Policy policy = result.getPolicy();

        representation.setId(policy.getId());
        representation.setName(policy.getName());
        representation.setType(policy.getType());
        representation.setDecisionStrategy(policy.getDecisionStrategy());
        representation.setDescription(policy.getDescription());

        representation.setResources(policy.getResources().stream().map(Resource::getName).collect(Collectors.toSet()));

        Set<String> scopeNames = policy.getScopes().stream().map(Scope::getName).collect(Collectors.toSet());

        representation.setScopes(scopeNames);

        policyResultRep.setPolicy(representation);

        if (result.getEffect() == Effect.DENY) {
            policyResultRep.setStatus(DecisionEffect.DENY);
            policyResultRep.setScopes(representation.getScopes());
        } else {
            policyResultRep.setStatus(DecisionEffect.PERMIT);
        }

        policyResultRep.setAssociatedPolicies(result.getAssociatedPolicies().stream().map(FGAPPolicyEvaluationResponseBuilder::toRepresentation).toList());

        return policyResultRep;
    }
}
