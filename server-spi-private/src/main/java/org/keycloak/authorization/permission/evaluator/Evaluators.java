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

package org.keycloak.authorization.permission.evaluator;

import java.util.List;

import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.DefaultPolicyEvaluator;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;

/**
 * A factory for the different {@link PermissionEvaluator} implementations.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class Evaluators {

    private final DefaultPolicyEvaluator policyEvaluator;

    public Evaluators(DefaultPolicyEvaluator policyEvaluator) {
        this.policyEvaluator = policyEvaluator;
    }

    public PermissionEvaluator from(List<ResourcePermission> permissions, EvaluationContext evaluationContext) {
        return schedule(permissions, evaluationContext);
    }

    public PermissionEvaluator schedule(List<ResourcePermission> permissions, EvaluationContext evaluationContext) {
        return new DefaultPermissionEvaluator(new IterablePermissionEvaluator(permissions.iterator(), evaluationContext, this.policyEvaluator));
    }
}
