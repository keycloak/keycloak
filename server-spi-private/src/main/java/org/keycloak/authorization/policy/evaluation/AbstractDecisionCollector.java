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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractDecisionCollector implements Decision<DefaultEvaluation> {
    protected final Map<ResourcePermission, Map<Policy, Result>> policyResults = new LinkedHashMap<>();

    @Override
    public void onDecision(DefaultEvaluation evaluation) {
        Policy parentPolicy = evaluation.getParentPolicy();
        ResourcePermission permission = evaluation.getPermission();

        Map<Policy, Result> permissionResults = policyResults.computeIfAbsent(permission, p -> new LinkedHashMap<>());
        if (parentPolicy != null) {
            // this happens at the root level of a policy evaluation
            if (parentPolicy.equals(evaluation.getPolicy())) {
                // When evaluating the parent policy we can just make sure that the entry exists and then set the evaluation with no cleanup
                Result parentPolicyResult = permissionResults.putIfAbsent(parentPolicy, new Result(permission, evaluation));
                if(parentPolicyResult != null) {
                    parentPolicyResult.setEvaluation(evaluation);
                }
            } else {
                // When evaluating any of the nested nodes we may already have a result at the top level for the current permission
                Result currentPolicy = permissionResults.getOrDefault(evaluation.getPolicy(), new Result(permission, evaluation));
                permissionResults.computeIfAbsent(parentPolicy, p -> new Result(permission, null)).addNestedResult(currentPolicy);
                // remove the current evaluation from the top level -- this occurs in the middle layers of a policy tree of N depth
                permissionResults.remove(evaluation.getPolicy());
            }
        } else {
            // in the case of no hierarchy we can just add to the top level for the permission
            permissionResults.put(evaluation.getPolicy(), new Result(permission, evaluation));
        }
    }

    @Override
    public void onComplete() {
        onComplete(policyResults.values());
    }

    @Override
    public void onComplete(ResourcePermission permission) {
        Map<Policy, Result>  results = policyResults.get(permission);

        if (results != null) {
            onComplete(permission, results);
        }
    }

    protected void onComplete(ResourcePermission permission, Map<Policy, Result> results) {

    }

    /**
     * Process the results for every single resource permission that was touched upon during this session
     * @param permissions
     */
    protected void onComplete(Collection<Map<Policy, Result>> permissions) {

    }
}
