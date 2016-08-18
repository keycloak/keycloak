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
import org.keycloak.authorization.Decision;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DefaultPolicyEvaluator implements PolicyEvaluator {

    private final AuthorizationProvider authorization;
    private Map<String, PolicyProviderFactory> policyProviders = new HashMap<>();

    public DefaultPolicyEvaluator(AuthorizationProvider authorization, List<PolicyProviderFactory> policyProviderFactories) {
        this.authorization = authorization;

        for (PolicyProviderFactory providerFactory : policyProviderFactories) {
            this.policyProviders.put(providerFactory.getId(), providerFactory);
        }
    }

    @Override
    public void evaluate(ResourcePermission permission, EvaluationContext executionContext, Decision decision) {
        ResourceServer resourceServer = permission.getResourceServer();

        if (PolicyEnforcementMode.DISABLED.equals(resourceServer.getPolicyEnforcementMode())) {
            createEvaluation(permission, executionContext, decision, null, null).grant();
            return;
        }

        StoreFactory storeFactory = this.authorization.getStoreFactory();
        PolicyStore policyStore = storeFactory.getPolicyStore();
        AtomicInteger policiesCount = new AtomicInteger(0);
        Consumer<Policy> consumer = createDecisionConsumer(permission, executionContext, decision, policiesCount);
        Resource resource = permission.getResource();

        if (resource != null) {
            List<? extends Policy> resourcePolicies = policyStore.findByResource(resource.getId());

            if (!resourcePolicies.isEmpty()) {
                resourcePolicies.forEach(consumer);
            }

            if (resource.getType() != null) {
                policyStore.findByResourceType(resource.getType(), resourceServer.getId()).forEach(consumer);
            }

            if (permission.getScopes().isEmpty() && !resource.getScopes().isEmpty()) {
                policyStore.findByScopeIds(resource.getScopes().stream().map(Scope::getId).collect(Collectors.toList()), resourceServer.getId()).forEach(consumer);
            }
        }

        if (!permission.getScopes().isEmpty()) {
            policyStore.findByScopeIds(permission.getScopes().stream().map(Scope::getId).collect(Collectors.toList()), resourceServer.getId()).forEach(consumer);
        }

        if (PolicyEnforcementMode.PERMISSIVE.equals(resourceServer.getPolicyEnforcementMode()) && policiesCount.get() == 0) {
            createEvaluation(permission, executionContext, decision, null, null).grant();
        }
    }

    private  Consumer<Policy> createDecisionConsumer(ResourcePermission permission, EvaluationContext executionContext, Decision decision, AtomicInteger policiesCount) {
        return (parentPolicy) -> {
            if (hasRequestedScopes(permission, parentPolicy)) {
                for (Policy associatedPolicy : parentPolicy.getAssociatedPolicies()) {
                    PolicyProviderFactory providerFactory = policyProviders.get(associatedPolicy.getType());

                    if (providerFactory == null) {
                        throw new RuntimeException("Could not find a policy provider for policy type [" + associatedPolicy.getType() + "].");
                    }

                    PolicyProvider policyProvider = providerFactory.create(associatedPolicy, this.authorization);

                    if (policyProvider == null) {
                        throw new RuntimeException("Unknown parentPolicy provider for type [" + associatedPolicy.getType() + "].");
                    }

                    DefaultEvaluation evaluation = createEvaluation(permission, executionContext, decision, parentPolicy, associatedPolicy);

                    policyProvider.evaluate(evaluation);
                    evaluation.denyIfNoEffect();

                    policiesCount.incrementAndGet();
                }
            }
        };
    }

    private DefaultEvaluation createEvaluation(ResourcePermission permission, EvaluationContext executionContext, Decision decision, Policy parentPolicy, Policy associatedPolicy) {
        return new DefaultEvaluation(permission, executionContext, parentPolicy, associatedPolicy, decision);
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
                    List<Resource> resourcesByType = authorization.getStoreFactory().getResourceStore().findByType(type);

                    for (Resource resourceType : resourcesByType) {
                        if (resourceType.getOwner().equals(resource.getResourceServer().getClientId())) {
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
