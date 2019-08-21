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
package org.keycloak.authorization.policy.provider.permission;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.DefaultEvaluation;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractPermissionProvider implements PolicyProvider {

    @Override
    public void evaluate(Evaluation evaluation) {
        AuthorizationProvider authorization = evaluation.getAuthorizationProvider();
        DefaultEvaluation defaultEvaluation = DefaultEvaluation.class.cast(evaluation);
        Map<Policy, Map<Object, Decision.Effect>> decisionCache = defaultEvaluation.getDecisionCache();
        Policy policy = evaluation.getPolicy();
        ResourcePermission permission = evaluation.getPermission();

        policy.getAssociatedPolicies().forEach(associatedPolicy -> {
            Map<Object, Decision.Effect> decisions = decisionCache.computeIfAbsent(associatedPolicy, p -> new HashMap<>());
            Decision.Effect effect = decisions.get(permission);

            defaultEvaluation.setPolicy(associatedPolicy);

            if (effect == null) {
                PolicyProvider policyProvider = authorization.getProvider(associatedPolicy.getType());
                
                if (policyProvider == null) {
                    throw new RuntimeException("No policy provider found for policy [" + associatedPolicy.getType() + "]");
                }
                
                policyProvider.evaluate(defaultEvaluation);
                evaluation.denyIfNoEffect();
                decisions.put(permission, defaultEvaluation.getEffect());
            } else {
                defaultEvaluation.setEffect(effect);
            }
        });
    }

    @Override
    public void close() {

    }
}
