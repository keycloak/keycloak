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

import org.keycloak.authorization.Decision.Effect;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.permission.ResourcePermission;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class Result {

    private final ResourcePermission permission;
    private List<PolicyResult> results = new ArrayList<>();
    private Effect status;

    public Result(ResourcePermission permission) {
        this.permission = permission;
    }

    public ResourcePermission getPermission() {
        return permission;
    }

    public List<PolicyResult> getResults() {
        return results;
    }

    public PolicyResult policy(Policy policy) {
        for (PolicyResult result : this.results) {
            if (result.getPolicy().equals(policy)) {
                return result;
            }
        }

        PolicyResult policyResult = new PolicyResult(policy);

        this.results.add(policyResult);

        return policyResult;
    }

    public void setStatus(final Effect status) {
        this.status = status;
    }

    public Effect getEffect() {
        return status;
    }

    public static class PolicyResult {

        private final Policy policy;
        private List<PolicyResult> associatedPolicies = new ArrayList<>();
        private Effect status;

        public PolicyResult(Policy policy) {
            this.policy = policy;
        }

        public PolicyResult status(Effect status) {
            this.status = status;
            return this;
        }

        public PolicyResult policy(Policy policy) {
            return getPolicy(policy, this.associatedPolicies);
        }

        private PolicyResult getPolicy(Policy policy, List<PolicyResult> results) {
            for (PolicyResult result : results) {
                if (result.getPolicy().equals(policy)) {
                    return result;
                }
            }

            PolicyResult policyResult = new PolicyResult(policy);

            results.add(policyResult);

            return policyResult;
        }

        public Policy getPolicy() {
            return policy;
        }

        public List<PolicyResult> getAssociatedPolicies() {
            return associatedPolicies;
        }

        public Effect getStatus() {
            return status;
        }

        public void setStatus(final Effect status) {
            this.status = status;
        }
    }
}
