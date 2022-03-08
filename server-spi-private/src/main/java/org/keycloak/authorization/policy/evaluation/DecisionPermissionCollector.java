/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authorization.policy.evaluation;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DecisionPermissionCollector extends AbstractDecisionCollector {

    private final AuthorizationProvider authorizationProvider;
    private final ResourceServer resourceServer;
    private final AuthorizationRequest request;
    private final Set<Permission> permissions = new LinkedHashSet<>();

    public DecisionPermissionCollector(AuthorizationProvider authorizationProvider, ResourceServer resourceServer, AuthorizationRequest request) {
        this.authorizationProvider = authorizationProvider;
        this.resourceServer = resourceServer;
        this.request = request;
    }

    @Override
    public void onComplete(Result result) {
        ResourcePermission permission = result.getPermission();
        Resource resource = permission.getResource();
        Collection<Scope> requestedScopes = permission.getScopes();

        if (Effect.PERMIT.equals(result.getEffect())) {
            if (permission.getScopes().isEmpty() && !resource.getScopes().isEmpty()) {
                return;
            }
            grantPermission(authorizationProvider, permissions, permission, requestedScopes, resourceServer, request, result);
        } else {
            Set<Scope> grantedScopes = new HashSet<>();
            Set<Scope> deniedScopes = new HashSet<>();
            List<Result.PolicyResult> userManagedPermissions = new ArrayList<>();
            boolean resourceGranted = false;
            boolean anyDeny = false;

            for (Result.PolicyResult policyResult : result.getResults()) {
                Policy policy = policyResult.getPolicy();
                Set<Scope> policyScopes = policy.getScopes();
                Set<Resource> policyResources = policy.getResources();
                boolean containsResource = policyResources.contains(resource);

                if (isGranted(policyResult)) {
                    if (isScopePermission(policy)) {
                        for (Scope scope : requestedScopes) {
                            if (policyScopes.contains(scope)) {
                                grantedScopes.add(scope);
                                // we need to grant any scope granted by a permission in case it is not explicitly
                                // associated with the resource. For instance, resources inheriting scopes from parent resources.
                                if (resource != null && !resource.getScopes().contains(scope)) {
                                    deniedScopes.remove(scope);
                                }
                            }
                        }
                    } else if (isResourcePermission(policy)) {
                        grantedScopes.addAll(requestedScopes);
                    } else if (resource != null && resource.isOwnerManagedAccess() && "uma".equals(policy.getType())) {
                        userManagedPermissions.add(policyResult);
                    }
                    if (!resourceGranted) {
                        resourceGranted = isGrantingAccessToResource(resource, policy) && containsResource;
                    }
                } else {
                    if (isResourcePermission(policy)) {
                        // deny all requested scopes if the resource-based permission is associated with the resource or if the
                        // resource was not granted by any other permission
                        if (containsResource || !resourceGranted) {
                            deniedScopes.addAll(requestedScopes);
                        }
                    } else {
                        // deny all scopes associated with the scope-based permission if the permission is associated with the 
                        // resource or if the permission applies to any resource associated with the scopes
                        if (containsResource || policyResources.isEmpty()) {
                            deniedScopes.addAll(policyScopes);
                        }
                    }
                    if (!anyDeny) {
                        anyDeny = true;
                    }
                }
            }

            if (DecisionStrategy.AFFIRMATIVE.equals(resourceServer.getDecisionStrategy())) {
                // remove any scope that was granted from the list of denied scopes if the decision strategy is affirmative
                deniedScopes.removeAll(grantedScopes);
            }

            grantedScopes.removeAll(deniedScopes);

            if (userManagedPermissions.isEmpty()) {
                if (!resourceGranted && (grantedScopes.isEmpty() && !requestedScopes.isEmpty())) {
                    return;
                }
            } else {
                for (Result.PolicyResult userManagedPermission : userManagedPermissions) {
                    Set<Scope> scopes = new HashSet<>(userManagedPermission.getPolicy().getScopes());

                    if (!requestedScopes.isEmpty()) {
                        scopes.retainAll(requestedScopes);
                    }

                    grantedScopes.addAll(scopes);
                }

                if (grantedScopes.isEmpty() && !resource.getScopes().isEmpty()) {
                    return;
                }

                anyDeny = false;
            }

            if (anyDeny && grantedScopes.isEmpty()) {
                return;
            }

            grantPermission(authorizationProvider, permissions, permission, grantedScopes, resourceServer, request, result);
        }
    }

    /**
     * Checks if the given {@code policy} is eligible to grant access to a resource. Resources are only granted if policy is 
     * not a scope-permission or, if so, the resource is a user-owned resource so that permissions can be overridden when 
     * inheriting policies from a typed/parent resource.
     * 
     * @param resource the resource
     * @param policy the policy that grants access to the resources
     * @return {@code true} if the resource should be granted 
     */
    private boolean isGrantingAccessToResource(Resource resource, Policy policy) {
        boolean scopePermission = isScopePermission(policy);
        
        if (!scopePermission) {
            return true;
        }
        
        return resource != null && !resource.getOwner().equals(resourceServer.getClientId());
    }

    public Collection<Permission> results() {
        return permissions;
    }

    @Override
    public void onError(Throwable cause) {
        throw new RuntimeException("Failed to evaluate permissions", cause);
    }

    protected void grantPermission(AuthorizationProvider authorizationProvider, Set<Permission> permissions, ResourcePermission permission, Collection<Scope> grantedScopes, ResourceServer resourceServer, AuthorizationRequest request, Result result) {
        Set<String> scopeNames = grantedScopes.stream().map(Scope::getName).collect(Collectors.toSet());
        Resource resource = permission.getResource();

        if (resource != null) {
            permissions.add(createPermission(resource, scopeNames, permission.getClaims(), request));
        } else if (!grantedScopes.isEmpty()) {
            ResourceStore resourceStore = authorizationProvider.getStoreFactory().getResourceStore();

            resourceStore.findByScopes(resourceServer, new HashSet<>(grantedScopes), resource1 -> permissions.add(createPermission(resource, scopeNames, permission.getClaims(), request)));

            permissions.add(createPermission(null, scopeNames, permission.getClaims(), request));
        }
    }

    private Permission createPermission(Resource resource, Set<String> scopes, Map<String, Set<String>> claims, AuthorizationRequest request) {
        AuthorizationRequest.Metadata metadata = null;

        if (request != null) {
            metadata = request.getMetadata();
        }

        Permission permission;

        if (resource != null) {
            String resourceName = metadata == null || metadata.getIncludeResourceName() ? resource.getName() : null;
            permission = new Permission(resource.getId(), resourceName, scopes, claims);
        } else {
            permission = new Permission(null, null, scopes, claims);
        }

        onGrant(permission);

        return permission;
    }

    protected void onGrant(Permission permission) {

    }

    private static boolean isResourcePermission(Policy policy) {
        return "resource".equals(policy.getType());
    }

    private static boolean isScopePermission(Policy policy) {
        return "scope".equals(policy.getType());
    }
}
