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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

    private final AuthorizationProvider authorization;
    private final StoreFactory storeFactory;
    private final PolicyStore policyStore;
    private final ResourceStore resourceStore;

    public DefaultPolicyEvaluator(AuthorizationProvider authorization) {
        this.authorization = authorization;
        storeFactory = this.authorization.getStoreFactory();
        policyStore = storeFactory.getPolicyStore();
        resourceStore = storeFactory.getResourceStore();
    }

    @Override
    public void evaluate(ResourcePermission permission, EvaluationContext executionContext, Decision decision) {
        ResourceServer resourceServer = permission.getResourceServer();
        PolicyEnforcementMode enforcementMode = resourceServer.getPolicyEnforcementMode();

        if (PolicyEnforcementMode.DISABLED.equals(enforcementMode)) {
            createEvaluation(permission, executionContext, decision, null, null).grant();
            return;
        }

        AtomicBoolean verified = new AtomicBoolean(false);
        Consumer<Policy> consumer = createDecisionConsumer(permission, executionContext, decision, verified);
        Resource resource = permission.getResource();
        List<Scope> scopes = permission.getScopes();

        if (resource != null) {
            evaluatePolicies(() -> policyStore.findByResource(resource.getId(), resourceServer.getId()), consumer);

            if (resource.getType() != null) {
                evaluatePolicies(() -> policyStore.findByResourceType(resource.getType(), resourceServer.getId()), consumer);
            }

            if (scopes.isEmpty() && !resource.getScopes().isEmpty()) {
                scopes.removeAll(resource.getScopes());
                evaluatePolicies(() -> policyStore.findByScopeIds(resource.getScopes().stream().map(Scope::getId).collect(Collectors.toList()), resourceServer.getId()), consumer);
            }
        }

        if (!scopes.isEmpty()) {
            evaluatePolicies(() -> policyStore.findByScopeIds(scopes.stream().map(Scope::getId).collect(Collectors.toList()), resourceServer.getId()), consumer);
        }

        if (PolicyEnforcementMode.PERMISSIVE.equals(enforcementMode) && !verified.get()) {
            createEvaluation(permission, executionContext, decision, null, null).grant();
        }
    }

    private void evaluatePolicies(Supplier<List<Policy>> supplier, Consumer<Policy> consumer) {
        List<Policy> policies = supplier.get();

        if (!policies.isEmpty()) {
            policies.forEach(consumer);
        }
    }

    private Consumer<Policy> createDecisionConsumer(ResourcePermission permission, EvaluationContext executionContext, Decision decision, AtomicBoolean verified) {
        return (parentPolicy) -> {
            if (!hasRequestedScopes(permission, parentPolicy)) {
                return;
            }

            for (Policy associatedPolicy : parentPolicy.getAssociatedPolicies()) {
                PolicyProvider policyProvider = authorization.getProvider(associatedPolicy.getType());

                if (policyProvider == null) {
                    throw new RuntimeException("Unknown parentPolicy provider for type [" + associatedPolicy.getType() + "].");
                }

                DefaultEvaluation evaluation = createEvaluation(permission, executionContext, decision, parentPolicy, associatedPolicy);

                policyProvider.evaluate(evaluation);
                evaluation.denyIfNoEffect();
            }

            verified.compareAndSet(false, true);
        };
    }

    private DefaultEvaluation createEvaluation(ResourcePermission permission, EvaluationContext executionContext, Decision decision, Policy parentPolicy, Policy associatedPolicy) {
        return new DefaultEvaluation(permission, executionContext, parentPolicy, associatedPolicy, decision, authorization);
    }

    private boolean hasRequestedScopes(final ResourcePermission permission, final Policy policy) {
        if (permission.getScopes().isEmpty()) {
            return true;
        }

        Resource resourcePermission = permission.getResource();
        Set<Resource> policyResources = policy.getResources();

        if (resourcePermission != null && !policyResources.isEmpty()) {
                if (!policyResources.stream().filter(resource -> resource.getId().equals(resourcePermission.getId())).findFirst().isPresent()) {
                return false;
            }
        }

        Set<Scope> scopes = new HashSet<>(policy.getScopes());

        if (scopes.isEmpty()) {
            Set<Resource> resources = new HashSet<>();

            resources.addAll(policyResources);

            for (Resource resource : resources) {
                scopes.addAll(resource.getScopes());
            }

            if (!resources.isEmpty() && scopes.isEmpty()) {
                return false;
            }

            if (scopes.isEmpty()) {
                Resource resource = permission.getResource();
                String type = resource.getType();

                if (type != null) {
                    List<Resource> resourcesByType = resourceStore.findByType(type, resource.getResourceServer().getId());

                    for (Resource resourceType : resourcesByType) {
                        if (resourceType.getOwner().equals(resource.getResourceServer().getId())) {
                            resources.add(resourceType);
                        }
                    }
                }
            }

            for (Resource resource : resources) {
                scopes.addAll(resource.getScopes());
            }
        }

        for (Scope givenScope : scopes) {
            for (Scope scope : permission.getScopes()) {
                if (givenScope.getId().equals(scope.getId())) {
                    return true;
                }
            }
        }

        return false;
    }
}
