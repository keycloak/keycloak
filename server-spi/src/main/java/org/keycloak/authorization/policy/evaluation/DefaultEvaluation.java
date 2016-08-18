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
import org.keycloak.authorization.Decision.Effect;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.representations.idm.authorization.Logic;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DefaultEvaluation implements Evaluation {

    private final ResourcePermission permission;
    private final EvaluationContext executionContext;
    private final Decision decision;
    private final Policy policy;
    private final Policy parentPolicy;
    private Effect effect;

    public DefaultEvaluation(ResourcePermission permission, EvaluationContext executionContext, Policy parentPolicy, Policy policy, Decision decision) {
        this.permission = permission;
        this.executionContext = executionContext;
        this.parentPolicy = parentPolicy;
        this.policy = policy;
        this.decision = decision;
    }

    /**
     * Returns the {@link ResourcePermission} to be evaluated.
     *
     * @return the permission to be evaluated
     */
    public ResourcePermission getPermission() {
        return this.permission;
    }

    /**
     * Returns the {@link org.keycloak.authorization.permission.evaluator.PermissionEvaluator}. Which provides access to the whole evaluation runtime context.
     *
     * @return the evaluation context
     */
    public EvaluationContext getContext() {
        return this.executionContext;
    }

    /**
     * Grants all the requested permissions to the caller.
     */
    public void grant() {
        if (policy != null && Logic.NEGATIVE.equals(policy.getLogic())) {
            this.effect = Effect.DENY;
        } else {
            this.effect = Effect.PERMIT;
        }

        this.decision.onDecision(this);
    }

    public void deny() {
        if (policy != null && Logic.NEGATIVE.equals(policy.getLogic())) {
            this.effect = Effect.PERMIT;
        } else {
            this.effect = Effect.DENY;
        }

        this.decision.onDecision(this);
    }

    public Policy getPolicy() {
        return this.policy;
    }

    public Policy getParentPolicy() {
        return this.parentPolicy;
    }

    public Effect getEffect() {
        return effect;
    }

    void denyIfNoEffect() {
        if (this.effect == null) {
            deny();
        }
    }
}
