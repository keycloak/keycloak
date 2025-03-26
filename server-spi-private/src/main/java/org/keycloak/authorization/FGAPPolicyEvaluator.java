/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2025 Red Hat, Inc., and individual contributors
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

package org.keycloak.authorization;

import java.util.function.Consumer;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.DefaultPolicyEvaluator;
import org.keycloak.authorization.policy.evaluation.PolicyEvaluator;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceStore;

/**
 * A {@link PolicyEvaluator} specific for evaluating permisions in the context of the {@link org.keycloak.common.Profile.Feature#ADMIN_FINE_GRAINED_AUTHZ_V2} feature.
 */
public class FGAPPolicyEvaluator extends DefaultPolicyEvaluator {

    @Override
    protected void evaluateResourceTypePolicies(ResourcePermission permission, Resource resource, PolicyStore policyStore, ResourceServer resourceServer, Consumer<Policy> policyConsumer, ResourceStore resourceStore) {
        String resourceType = permission.getResourceType();

        if (resourceType == null || resource.getName().equals(permission.getResourceType())) {
            return;
        }

        Resource resourceTypeResource = resourceStore.findByName(resourceServer, resourceType);
        policyStore.findByResource(resourceServer, resourceTypeResource, policyConsumer);
    }

    @Override
    protected void evaluateScopePolicies(ResourcePermission permission, PolicyStore policyStore, ResourceServer resourceServer, Consumer<Policy> policyConsumer) {
        // do not evaluate permissions for individual scopes
    }
}
