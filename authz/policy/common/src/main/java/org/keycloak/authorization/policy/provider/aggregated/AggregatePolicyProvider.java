/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.authorization.policy.provider.aggregated;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.DecisionResultCollector;
import org.keycloak.authorization.policy.evaluation.DefaultEvaluation;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.evaluation.Result;
import org.keycloak.authorization.policy.provider.PolicyProvider;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AggregatePolicyProvider implements PolicyProvider {
    private static final Logger logger = Logger.getLogger(AggregatePolicyProvider.class);

    @Override
    public void evaluate(Evaluation evaluation) {
        logger.debugf("Aggregate policy %s evaluating using parent class", evaluation.getPolicy().getName());
        DecisionResultCollector decision = new DecisionResultCollector() {
            @Override
            protected void onComplete(Result result) {
                if (isGranted(result.getResults().iterator().next())) {
                    evaluation.grant();
                } else {
                    evaluation.deny();
                }
            }
        };
        AuthorizationProvider authorization = evaluation.getAuthorizationProvider();
        Policy policy = evaluation.getPolicy();
        DefaultEvaluation defaultEvaluation = DefaultEvaluation.class.cast(evaluation);
        Map<Policy, Map<Object, Decision.Effect>> decisionCache = defaultEvaluation.getDecisionCache();
        ResourcePermission permission = evaluation.getPermission();

        for (Policy associatedPolicy : policy.getAssociatedPolicies()) {
            Map<Object, Decision.Effect> decisions = decisionCache.computeIfAbsent(associatedPolicy, p -> new HashMap<>());
            Decision.Effect effect = decisions.get(permission);
            DefaultEvaluation eval = new DefaultEvaluation(evaluation.getPermission(), evaluation.getContext(), policy, associatedPolicy, decision, authorization, decisionCache);

            if (effect == null) {
                PolicyProvider policyProvider = authorization.getProvider(associatedPolicy.getType());

                policyProvider.evaluate(eval);

                eval.denyIfNoEffect();
                decisions.put(permission, eval.getEffect());
            } else {
                eval.setEffect(effect);
            }
        }

        decision.onComplete(permission);
    }

    @Override
    public void close() {

    }
}
