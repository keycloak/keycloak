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

import java.util.List;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.DecisionResultCollector;
import org.keycloak.authorization.policy.evaluation.DefaultEvaluation;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.evaluation.Result;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AggregatePolicyProvider implements PolicyProvider {

    @Override
    public void evaluate(Evaluation evaluation) {
        //TODO: need to detect deep recursions
        DecisionResultCollector decision = new DecisionResultCollector() {
            @Override
            protected void onComplete(List<Result> results) {
                if (results.isEmpty()) {
                    evaluation.deny();
                } else {
                    Result result = results.iterator().next();

                    if (Effect.PERMIT.equals(result.getEffect())) {
                        evaluation.grant();
                    }
                }
            }
        };

        Policy policy = evaluation.getPolicy();
        AuthorizationProvider authorization = evaluation.getAuthorizationProvider();

        policy.getAssociatedPolicies().forEach(associatedPolicy -> {
            PolicyProvider policyProvider = authorization.getProvider(associatedPolicy.getType());
            policyProvider.evaluate(new DefaultEvaluation(evaluation.getPermission(), evaluation.getContext(), policy, associatedPolicy, decision, authorization));
        });

        decision.onComplete();
    }

    @Override
    public void close() {

    }
}
