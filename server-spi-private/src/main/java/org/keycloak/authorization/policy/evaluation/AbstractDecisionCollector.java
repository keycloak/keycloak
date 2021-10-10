/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authorization.policy.evaluation;

import org.keycloak.authorization.Decision;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.representations.idm.authorization.DecisionStrategy;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractDecisionCollector implements Decision<DefaultEvaluation> {

    protected final Map<ResourcePermission, Result> results = new LinkedHashMap<>();

    @Override
    public void onDecision(DefaultEvaluation evaluation) {
        Policy parentPolicy = evaluation.getParentPolicy();
        ResourcePermission permission = evaluation.getPermission();

        if (parentPolicy != null) {
            if (parentPolicy.equals(evaluation.getPolicy())) {
                results.computeIfAbsent(permission, permission1 -> {
                    for (Result result : results.values()) {
                        Result.PolicyResult policyResult = result.getPolicy(parentPolicy);

                        if (policyResult != null) {
                            Result newResult = new Result(permission1, evaluation);
                            Result.PolicyResult newPolicyResult = newResult.policy(parentPolicy);

                            for (Result.PolicyResult associatePolicy : policyResult.getAssociatedPolicies()) {
                                newPolicyResult.policy(associatePolicy.getPolicy(), associatePolicy.getEffect());
                            }

                            Map<String, Set<String>> claims = result.getPermission().getClaims();

                            if (!claims.isEmpty()) {
                                permission1.addClaims(claims);
                            }

                            return newResult;
                        }
                    }

                    return new Result(permission1, evaluation);
                }).policy(parentPolicy);
            } else {
                results.computeIfAbsent(permission, p -> new Result(p, evaluation)).policy(parentPolicy).policy(evaluation.getPolicy(), evaluation.getEffect());
            }
        } else {
            results.computeIfAbsent(permission, p -> new Result(p, evaluation)).setStatus(evaluation.getEffect());
        }
    }

    @Override
    public void onComplete() {
        onComplete(results.values());
    }

    @Override
    public void onComplete(ResourcePermission permission) {
        Result result = results.get(permission);

        if (result != null) {
            onComplete(result);
        }
    }

    protected void onComplete(Result result) {

    }

    protected void onComplete(Collection<Result> permissions) {

    }

    protected boolean isGranted(Result.PolicyResult policyResult) {
        Policy policy = policyResult.getPolicy();
        DecisionStrategy decisionStrategy = policy.getDecisionStrategy();

        switch (decisionStrategy) {
            case AFFIRMATIVE:
                for (Result.PolicyResult decision : policyResult.getAssociatedPolicies()) {
                    if (Effect.PERMIT.equals(decision.getEffect())) {
                        return true;
                    }
                }
                return false;
            case CONSENSUS:
                int grantCount = 0;
                int denyCount = policy.getAssociatedPolicies().size();

                for (Result.PolicyResult decision : policyResult.getAssociatedPolicies()) {
                    if (decision.getEffect().equals(Effect.PERMIT)) {
                        grantCount++;
                        denyCount--;
                    }
                }

                return grantCount > denyCount;
            default:
                // defaults to UNANIMOUS
                for (Result.PolicyResult decision : policyResult.getAssociatedPolicies()) {
                    if (Effect.DENY.equals(decision.getEffect())) {
                        return false;
                    }
                }
                return true;
        }
    }
}
