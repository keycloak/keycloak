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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.DecisionPermissionCollector;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.authorization.policy.evaluation.PolicyEvaluator;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.Permission;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
class IterablePermissionEvaluator implements PermissionEvaluator {

    private final Iterator<ResourcePermission> permissions;
    private final EvaluationContext executionContext;
    private final PolicyEvaluator policyEvaluator;
    private final AuthorizationProvider authorizationProvider;

    IterablePermissionEvaluator(Iterator<ResourcePermission> permissions, EvaluationContext executionContext, AuthorizationProvider authorizationProvider) {
        this.permissions = permissions;
        this.executionContext = executionContext;
        this.authorizationProvider = authorizationProvider;
        this.policyEvaluator = authorizationProvider.getPolicyEvaluator();
    }

    @Override
    public Decision evaluate(Decision decision) {
        StoreFactory storeFactory = authorizationProvider.getStoreFactory();

        try {
            Map<Policy, Map<Object, Decision.Effect>> decisionCache = new HashMap<>();

            storeFactory.setReadOnly(true);

            while (this.permissions.hasNext()) {
                this.policyEvaluator.evaluate(this.permissions.next(), authorizationProvider, executionContext, decision, decisionCache);
            }

            decision.onComplete();
        } catch (Throwable cause) {
            decision.onError(cause);
        } finally {
            storeFactory.setReadOnly(false);
        }

        return decision;
    }

    @Override
    public Collection<Permission> evaluate(ResourceServer resourceServer, AuthorizationRequest request) {
        DecisionPermissionCollector decision = new DecisionPermissionCollector(authorizationProvider, resourceServer, request);

        evaluate(decision);

        return decision.results();
    }
}
