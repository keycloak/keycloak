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

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision.Effect;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.provider.PolicyProvider;

/**
 * <p>An {@link Evaluation} is mainly used by {@link PolicyProvider} in order to evaluate a single
 * and specific {@link ResourcePermission} against the configured policies.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface Evaluation {

    /**
     * Returns the {@link ResourcePermission} to be evaluated.
     *
     * @return the permission to be evaluated
     */
    ResourcePermission getPermission();

    /**
     * Returns the {@link EvaluationContext}. Which provides access to the whole evaluation runtime context.
     *
     * @return the evaluation context
     */
    EvaluationContext getContext();

    /**
     * Returns the {@link Policy}. being evaluated.
     *
     * @return the evaluation context
     */
    Policy getPolicy();

    /**
     * Returns a {@link Realm} that can be used by policies to query information.
     *
     * @return a {@link Realm} instance
     */
    Realm getRealm();

    AuthorizationProvider getAuthorizationProvider();

    /**
     * Grants the requested permission to the caller.
     */
    void grant();

    /**
     * Denies the requested permission.
     */
    void deny();

    /**
     * Denies the requested permission if a decision was not made yet.
     */
    void denyIfNoEffect();

    /**
     * Returns the parent policy (a permission) of the policy being evaluated.
     *
     * @return the parent policy
     */
    Policy getParentPolicy();

    Effect getEffect();

    void setEffect(Effect effect);

    /**
     * If the given scope should be granted when the given {@code grantedPolicy} is granting access to a resource or a specific scope.
     *
     * @param grantedPolicy the policy granting access
     * @param grantedScope the scope that should be granted
     * @return {@code true} if the scope is granted. Otherwise, returns {@code false}
     */
    default boolean isGranted(Policy grantedPolicy, Scope grantedScope) {
        return grantedPolicy.getScopes().contains(grantedScope);
    }

    /**
     * If the given scope should not be granted when the given {@code deniedPolicy} is associated with a resource group.
     *
     * @param deniedPolicy the policy granting access
     * @param deniedScope the scope that should be granted
     * @return {@code true} if the scope is granted. Otherwise, returns {@code false}
     */
    default boolean isDenied(Policy deniedPolicy, Scope deniedScope) {
        return false;
    }
}
