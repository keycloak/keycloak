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
import org.keycloak.authorization.admin.PolicyEvaluationService;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.policy.evaluation.Result;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.DecisionEffect;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse.PolicyResultRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PolicyEvaluationResponseBuilder {
    public static PolicyEvaluationResponse build(PolicyEvaluationService.EvaluationDecisionCollector decision, ResourceServer resourceServer, AuthorizationProvider authorization, KeycloakIdentity identity) {
        PolicyEvaluationResponse response = new PolicyEvaluationResponse();
        List<PolicyEvaluationResponse.EvaluationResultRepresentation> resultsRep = new ArrayList<>();
        AccessToken accessToken = identity.getAccessToken();
        AccessToken.Authorization authorizationData = new AccessToken.Authorization();

        authorizationData.setPermissions(decision.results());
        accessToken.setAuthorization(authorizationData);

        ClientModel clientModel = authorization.getRealm().getClientById(resourceServer.getId());

        if (!accessToken.hasAudience(clientModel.getClientId())) {
            accessToken.audience(clientModel.getClientId());
        }

        response.setRpt(accessToken);

        Collection<Result> results = decision.getResults();

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
                PolicyResultRepresentation policyRep = toRepresentation(policy, authorization);

                if ("resource".equals(policy.getPolicy().getType())) {
                    policyRep.getPolicy().setScopes(result.getPermission().getResource().getScopes().stream().map(Scope::getName).collect(Collectors.toSet()));
                }

                policies.add(policyRep);
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

            if (DecisionEffect.PERMIT.equals(result.getStatus())) {
                result.setAllowedScopes(scopes);
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
                }
            }
        });

        response.setResults(groupedResults.values().stream().collect(Collectors.toList()));

        return response;
    }

    private static PolicyEvaluationResponse.PolicyResultRepresentation toRepresentation(Result.PolicyResult result, AuthorizationProvider authorization) {
        PolicyEvaluationResponse.PolicyResultRepresentation policyResultRep = new PolicyEvaluationResponse.PolicyResultRepresentation();

        PolicyRepresentation representation = new PolicyRepresentation();
        Policy policy = result.getPolicy();

        representation.setId(policy.getId());
        representation.setName(policy.getName());
        representation.setType(policy.getType());
        representation.setDecisionStrategy(policy.getDecisionStrategy());
        representation.setDescription(policy.getDescription());

        if ("uma".equals(representation.getType())) {
            Map<String, String> filters = new HashMap<>();

            filters.put(PermissionTicket.POLICY, policy.getId());

            List<PermissionTicket> tickets = authorization.getStoreFactory().getPermissionTicketStore().find(filters, policy.getResourceServer().getId(), -1, 1);

            if (!tickets.isEmpty()) {
                KeycloakSession keycloakSession = authorization.getKeycloakSession();
                PermissionTicket ticket = tickets.get(0);
                UserModel owner = keycloakSession.users().getUserById(ticket.getOwner(), authorization.getRealm());
                UserModel requester = keycloakSession.users().getUserById(ticket.getRequester(), authorization.getRealm());

                representation.setDescription("Resource owner (" + getUserEmailOrUserName(owner) + ") grants access to " + getUserEmailOrUserName(requester));
            } else {
                String description = representation.getDescription();

                if (description != null) {
                    representation.setDescription(description + " (User-Managed Policy)");
                } else {
                    representation.setDescription("User-Managed Policy");
                }
            }
        }

        representation.setResources(policy.getResources().stream().map(resource -> resource.getName()).collect(Collectors.toSet()));

        Set<String> scopeNames = policy.getScopes().stream().map(scope -> scope.getName()).collect(Collectors.toSet());

        representation.setScopes(scopeNames);

        policyResultRep.setPolicy(representation);

        if (result.getEffect() == Decision.Effect.DENY) {
            policyResultRep.setStatus(DecisionEffect.DENY);
            policyResultRep.setScopes(representation.getScopes());
        } else {
            policyResultRep.setStatus(DecisionEffect.PERMIT);
        }

        policyResultRep.setAssociatedPolicies(result.getAssociatedPolicies().stream().map(policy1 -> toRepresentation(policy1, authorization)).collect(Collectors.toList()));

        return policyResultRep;
    }

    private static String getUserEmailOrUserName(UserModel user) {
        return (user.getEmail() != null ? user.getEmail() : user.getUsername());
    }
}
