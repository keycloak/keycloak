/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.fgap.evaluation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision.Effect;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.authorization.policy.evaluation.Realm;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.SCHEMA;

class FGAPEvaluation implements Evaluation {

    private final Evaluation evaluation;
    private final Map<Scope, Set<Resource>> scopesGrantedByResource;

    FGAPEvaluation(Evaluation evaluation, Map<Scope, Set<Resource>> scopesGrantedByResource) {
        this.evaluation = evaluation;
        this.scopesGrantedByResource = scopesGrantedByResource;
    }

    @Override
    public boolean isGranted(Policy grantedPolicy, Scope grantedScope) {
        ResourcePermission permission = getPermission();
        String resourceType = permission.getResourceType();

        if (resourceType == null) {
            return false;
        }

        Set<Scope> policyScopes = grantedPolicy.getScopes();

        if (isForResourceType(grantedPolicy, resourceType) && policyScopes.contains(grantedScope)) {
            scopesGrantedByResource.computeIfAbsent(grantedScope, k -> new HashSet<>()).addAll(grantedPolicy.getResources());
            return true;
        }

        Set<String> aliases = SCHEMA.getScopeAliases(resourceType, grantedScope);

        if (aliases.isEmpty()) {
            return false;
        }

        boolean grantedByResourceGroup = policyScopes.stream().map(Scope::getName).anyMatch(aliases::contains);

        if (grantedByResourceGroup) {
            scopesGrantedByResource.computeIfAbsent(grantedScope, k -> new HashSet<>()).addAll(grantedPolicy.getResources());
        }

        return grantedByResourceGroup;
    }

    @Override
    public boolean isDenied(Policy deniedPolicy, Scope deniedScope) {
        ResourcePermission permission = getPermission();
        String resourceType = permission.getResourceType();

        if (resourceType == null) {
            return false;
        }

        Set<Scope> deniedScopes = deniedPolicy.getScopes();
        boolean isPermissionDeniedForSpecificResource = isForSpecificResource(deniedPolicy);

        if (isPermissionDeniedForSpecificResource && deniedScopes.contains(deniedScope)) {
            // scope denied for an specific resource
            return true;
        }

        if (isForResourceType(deniedPolicy, resourceType)) {
            // checks if the scope was not granted by a permission that maps to all resources
            return !isGranted(permission.getResource(), deniedScope);
        }

        Set<String> aliases = SCHEMA.getScopeAliases(resourceType, deniedScope);

        if (aliases.isEmpty()) {
            return false;
        }

        for (Scope scope : deniedScopes) {
            if (aliases.contains(scope.getName())) {
                if (isPermissionDeniedForSpecificResource) {
                    // denied for a specific group resource (e.g.: a user group)
                    // if a specific resource is denied, then it is denied
                    return true;
                }

                // denied if not granted for the specific resource (e.g.: user) or resource type
                // permissions granted for a specific resource have precedence over denies on a resource group
                return !isGranted(deniedScope);
            }
        }

        return false;
    }

    @Override
    public ResourcePermission getPermission() {
        return evaluation.getPermission();
    }

    @Override
    public EvaluationContext getContext() {
        return evaluation.getContext();
    }

    @Override
    public Policy getPolicy() {
        return evaluation.getPolicy();
    }

    @Override
    public Realm getRealm() {
        return evaluation.getRealm();
    }

    @Override
    public AuthorizationProvider getAuthorizationProvider() {
        return evaluation.getAuthorizationProvider();
    }

    @Override
    public void grant() {
        evaluation.grant();
    }

    @Override
    public void deny() {
        evaluation.deny();
    }

    @Override
    public void denyIfNoEffect() {
        evaluation.denyIfNoEffect();
    }

    @Override
    public Policy getParentPolicy() {
        return evaluation.getParentPolicy();
    }

    @Override
    public Effect getEffect() {
        return evaluation.getEffect();
    }

    @Override
    public void setEffect(Effect effect) {
        evaluation.setEffect(effect);
    }

    private boolean isForResourceType(Policy policy, String resourceType) {
        return policy.getResourceType().equals(resourceType);
    }

    private boolean isForSpecificResource(Policy policy) {
        return !policy.getResources().contains(getResourceTypeResource(policy, policy.getResourceType()));
    }

    private boolean isGranted(Resource resource, Scope scope) {
        return scopesGrantedByResource.getOrDefault(scope, Set.of()).stream().anyMatch(resource::equals);
    }

    private boolean isGranted(Scope scope) {
        return !scopesGrantedByResource.getOrDefault(scope, Set.of()).isEmpty();
    }

    private Resource getResourceTypeResource(Policy policy, String resourceType) {
        return SCHEMA.getResourceTypeResource(getAuthorizationProvider().getKeycloakSession(), policy.getResourceServer(), resourceType);
    }
}
