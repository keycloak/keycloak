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

import org.keycloak.authorization.Decision;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.authorization.policy.evaluation.PolicyEvaluator;

import java.util.Iterator;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
class IterablePermissionEvaluator implements PermissionEvaluator {

    private final Iterator<ResourcePermission> permissions;
    private final EvaluationContext executionContext;
    private final PolicyEvaluator policyEvaluator;

    IterablePermissionEvaluator(Iterator<ResourcePermission> permissions, EvaluationContext executionContext, PolicyEvaluator policyEvaluator) {
        this.permissions = permissions;
        this.executionContext = executionContext;
        this.policyEvaluator = policyEvaluator;
    }

    @Override
    public void evaluate(Decision decision) {
        try {
            while (this.permissions.hasNext()) {
                this.policyEvaluator.evaluate(this.permissions.next(), this.executionContext, decision);
            }
            decision.onComplete();
        } catch (Throwable cause) {
            decision.onError(cause);
        }
    }
}
