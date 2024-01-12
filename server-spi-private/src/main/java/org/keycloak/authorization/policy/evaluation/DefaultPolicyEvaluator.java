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

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DefaultPolicyEvaluator implements PolicyEvaluator {

    @Override
    public void evaluate(ResourcePermission permission, AuthorizationProvider authorizationProvider, EvaluationContext executionContext, Decision decision, Map<Policy, Map<Object, Decision.Effect>> decisionCache) {
        StoreFactory storeFactory = authorizationProvider.getStoreFactory();
        PolicyStore policyStore = storeFactory.getPolicyStore();
        ResourceServer resourceServer = permission.getResourceServer();
        PolicyEnforcementMode enforcementMode = resourceServer.getPolicyEnforcementMode();

        // if we aren't enforcing policies then we should just grant and return
        if (PolicyEnforcementMode.DISABLED.equals(enforcementMode)) {
            grantAndComplete(permission, authorizationProvider, executionContext, decision);
            return;
        }
        
        // if marked as granted we just complete the evaluation
        if (permission.isGranted()) {
            grantAndComplete(permission, authorizationProvider, executionContext, decision);
            return;
        }

        AtomicBoolean verified = new AtomicBoolean();
        Consumer<Policy> policyConsumer = createPolicyEvaluator(permission, authorizationProvider, executionContext, decision, verified, decisionCache);
        Collection<Scope> scopes = permission.getScopes();
        Resource resource = permission.getResource();

        if(scopes != null) {
            if (resource != null) {
                policyStore.findByScopes(resourceServer, resource, new LinkedList<>(scopes), policyConsumer);
            }

            policyStore.findByScopes(resourceServer, null, new LinkedList<>(scopes), policyConsumer);
        }

        if(resource != null) {
            // get type policies that are explicitly not linked to any resources
            if(resource.getType() != null) {
                // evaluate policies explicitly set with a resource type
                policyStore.findByResourceType(resourceServer, true, resource.getType(), policyConsumer);

                if (!resource.getOwner().equals(resourceServer.getClientId())) {
                    // evaluate permissions for typed-resources owned by the resource server itself
                    ResourceStore resourceStore = authorizationProvider.getStoreFactory().getResourceStore();

                    for (Resource typedResource : resourceStore.findByType(resourceServer, resource.getType())) {
                        policyStore.findByResource(resourceServer, true, typedResource, policyConsumer);
                    }
                }
            }

            // get resource policies that are explicitly not linked to any scopes but are to the resource
            policyStore.findByResource(resourceServer, false, resource, policyConsumer);
        }

        if (verified.get()) {
            decision.onComplete(permission);
            return;
        }

        // requests are allowed even when no policies are evaluated, but we still want to keep track of what was denied
        if (PolicyEnforcementMode.PERMISSIVE.equals(enforcementMode)) {
            grantAndComplete(permission, authorizationProvider, executionContext, decision);
        }
    }

    private void grantAndComplete(ResourcePermission permission, AuthorizationProvider authorizationProvider,
            EvaluationContext executionContext, Decision decision) {
        DefaultEvaluation evaluation = new DefaultEvaluation(permission, executionContext, decision, authorizationProvider);

        evaluation.grant();

        decision.onComplete(permission);
    }

    private Consumer<Policy> createPolicyEvaluator(ResourcePermission permission, AuthorizationProvider authorizationProvider, EvaluationContext executionContext, Decision decision, AtomicBoolean verified, Map<Policy, Map<Object, Decision.Effect>> decisionCache) {
        return parentPolicy -> {
            PolicyProvider policyProvider = authorizationProvider.getProvider(parentPolicy.getType());

            if (policyProvider == null) {
                throw new RuntimeException("Unknown parentPolicy provider for type [" + parentPolicy.getType() + "].");
            }

            policyProvider.evaluate(new DefaultEvaluation(permission, executionContext, parentPolicy, decision, authorizationProvider, decisionCache));

            verified.compareAndSet(false, true);
        };
    }
}
