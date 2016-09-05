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

package org.keycloak.authorization.policy.evaluation;

import org.keycloak.authorization.Decision;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.representations.idm.authorization.DecisionStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class DecisionResultCollector implements Decision<DefaultEvaluation> {

    private Map<ResourcePermission, Result> results = new HashMap();

    @Override
    public void onDecision(DefaultEvaluation evaluation) {
        if (evaluation.getParentPolicy() != null) {
            results.computeIfAbsent(evaluation.getPermission(), Result::new).policy(evaluation.getParentPolicy()).policy(evaluation.getPolicy()).setStatus(evaluation.getEffect());
        } else {
            results.computeIfAbsent(evaluation.getPermission(), Result::new).setStatus(evaluation.getEffect());
        }
    }

    @Override
    public void onComplete() {
        for (Result result : results.values()) {
            for (Result.PolicyResult policyResult : result.getResults()) {
                if (isGranted(policyResult)) {
                    policyResult.setStatus(Effect.PERMIT);
                } else {
                    policyResult.setStatus(Effect.DENY);
                }
            }

            if (result.getResults().stream()
                    .filter(policyResult -> Effect.DENY.equals(policyResult.getStatus())).count() > 0) {
                result.setStatus(Effect.DENY);
            } else {
                result.setStatus(Effect.PERMIT);
            }
        }

        onComplete(results.values().stream().collect(Collectors.toList()));
    }

    protected abstract void onComplete(List<Result> results);

    private boolean isGranted(Result.PolicyResult policyResult) {
        List<Result.PolicyResult> values = policyResult.getAssociatedPolicies();

        int grantCount = 0;
        int denyCount = policyResult.getPolicy().getAssociatedPolicies().size();

        for (Result.PolicyResult decision : values) {
            if (decision.getStatus().equals(Effect.PERMIT)) {
                grantCount++;
                denyCount--;
            }
        }

        Policy policy = policyResult.getPolicy();
        DecisionStrategy decisionStrategy = policy.getDecisionStrategy();

        if (decisionStrategy == null) {
            decisionStrategy = DecisionStrategy.UNANIMOUS;
        }

        if (DecisionStrategy.AFFIRMATIVE.equals(decisionStrategy) && grantCount > 0) {
            return true;
        } else if (DecisionStrategy.UNANIMOUS.equals(decisionStrategy) && denyCount == 0) {
            return true;
        } else if (DecisionStrategy.CONSENSUS.equals(decisionStrategy)) {
            if (grantCount > denyCount) {
                return true;
            }
        }

        return false;
    }
}
