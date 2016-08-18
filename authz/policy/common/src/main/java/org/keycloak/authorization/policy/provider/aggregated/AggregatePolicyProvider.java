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

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.DecisionResultCollector;
import org.keycloak.authorization.policy.evaluation.DefaultEvaluation;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.evaluation.Result;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;

import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AggregatePolicyProvider implements PolicyProvider {

    private final Policy policy;
    private final AuthorizationProvider authorization;

    public AggregatePolicyProvider(Policy policy, AuthorizationProvider authorization) {
        this.policy = policy;
        this.authorization = authorization;
    }

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

        this.policy.getAssociatedPolicies().forEach(associatedPolicy -> {
            PolicyProviderFactory providerFactory = authorization.getProviderFactory(associatedPolicy.getType());
            PolicyProvider policyProvider = providerFactory.create(associatedPolicy, authorization);
            policyProvider.evaluate(new DefaultEvaluation(evaluation.getPermission(), evaluation.getContext(), policy, associatedPolicy, decision));
        });

        decision.onComplete();
    }

    @Override
    public void close() {

    }
}
