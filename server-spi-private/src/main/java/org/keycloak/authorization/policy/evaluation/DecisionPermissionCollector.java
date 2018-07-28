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
import org.keycloak.representations.idm.authorization.Permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
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
    private final List<Permission> permissions = new ArrayList<>();

    public DecisionPermissionCollector(AuthorizationProvider authorizationProvider, ResourceServer resourceServer, AuthorizationRequest request) {
        this.authorizationProvider = authorizationProvider;
        this.resourceServer = resourceServer;
        this.request = request;
    }

    @Override
    public void onComplete(Result result) {
        ResourcePermission permission = result.getPermission();
        Resource resource = permission.getResource();
        Set<Scope> grantedScopes = new HashSet<>();

        if (Effect.PERMIT.equals(result.getEffect())) {
            if (resource != null) {
                grantedScopes.addAll(resource.getScopes());
            } else {
                grantedScopes.addAll(permission.getScopes());
            }

            grantPermission(authorizationProvider, permissions, permission, grantedScopes, resourceServer, request, result);
        } else {
            Set<Scope> deniedScopes = new HashSet<>();
            List<Result.PolicyResult> userManagedPermissions = new ArrayList<>();
            Collection<Result.PolicyResult> permissionResults = new ArrayList<>(result.getResults());
            Iterator<Result.PolicyResult> iterator = permissionResults.iterator();

            while (iterator.hasNext()) {
                Result.PolicyResult policyResult = iterator.next();
                Policy policy = policyResult.getPolicy();
                Set<Scope> policyScopes = policy.getScopes();

                if (isGranted(policyResult)) {
                    if (isScopePermission(policy)) {
                        for (Scope scope : permission.getScopes()) {
                            if (policyScopes.contains(scope)) {
                                // try to grant any scope from a scope-based permission
                                grantedScopes.add(scope);
                            }
                        }
                    } else if (isResourcePermission(policy)) {
                        // we assume that all requested scopes should be granted given that we are processing a resource-based permission.
                        // Later they will be filtered based on any denied scope, if any.
                        // TODO: we could probably provide a configuration option to let users decide whether or not a resource-based permission should grant all scopes associated with the resource.
                        grantedScopes.addAll(permission.getScopes());
                    }
                    if (resource != null && resource.isOwnerManagedAccess() && "uma".equals(policy.getType())) {
                        userManagedPermissions.add(policyResult);
                    }
                    iterator.remove();
                } else {
                    if (isResourcePermission(policy)) {
                        deniedScopes.addAll(resource.getScopes());
                    } else {
                        deniedScopes.addAll(policyScopes);
                    }
                }
            }

            // remove any scope denied from the list of granted scopes
            grantedScopes.removeAll(deniedScopes);

            if (!userManagedPermissions.isEmpty()) {
                Set<Scope> scopes = new HashSet<>();

                for (Result.PolicyResult userManagedPermission : userManagedPermissions) {
                    grantedScopes.addAll(userManagedPermission.getPolicy().getScopes());
                }

                if (!scopes.isEmpty()) {
                    grantedScopes.clear();
                }

                // deny scopes associated with a resource that are not explicitly granted by the user
                if (!resource.getScopes().isEmpty() && scopes.isEmpty()) {
                    deniedScopes.addAll(resource.getScopes());
                } else {
                    permissionResults.clear();
                }
            }

            if (!grantedScopes.isEmpty() || (permissionResults.isEmpty() && deniedScopes.isEmpty())) {
                grantPermission(authorizationProvider, permissions, permission, grantedScopes, resourceServer, request, result);
            }
        }
    }

    public Collection<Permission> results() {
        return permissions;
    }

    @Override
    public void onError(Throwable cause) {
        throw new RuntimeException("Failed to evaluate permissions", cause);
    }

    protected void grantPermission(AuthorizationProvider authorizationProvider, List<Permission> permissions, ResourcePermission permission, Set<Scope> grantedScopes, ResourceServer resourceServer, AuthorizationRequest request, Result result) {
        Set<String> scopeNames = grantedScopes.stream().map(Scope::getName).collect(Collectors.toSet());
        Resource resource = permission.getResource();

        if (resource != null) {
            permissions.add(createPermission(resource, scopeNames, permission.getClaims(), request));
        } else if (!grantedScopes.isEmpty()) {
            ResourceStore resourceStore = authorizationProvider.getStoreFactory().getResourceStore();
            List<Resource> resources = resourceStore.findByScope(grantedScopes.stream().map(Scope::getId).collect(Collectors.toList()), resourceServer.getId());

            if (resources.isEmpty()) {
                permissions.add(createPermission(null, scopeNames, permission.getClaims(), request));
            } else {
                for (Resource grantedResource : resources) {
                    permissions.add(createPermission(grantedResource, scopeNames, permission.getClaims(), request));
                }
            }
        }
    }

    private Permission createPermission(Resource resource, Set<String> scopes, Map<String, Set<String>> claims, AuthorizationRequest request) {
        AuthorizationRequest.Metadata metadata = null;

        if (request != null) {
            metadata = request.getMetadata();
        }

        if (resource != null) {
            String resourceName = metadata == null || metadata.getIncludeResourceName() ? resource.getName() : null;
            return new Permission(resource.getId(), resourceName, scopes, claims);
        }

        return new Permission(null, null, scopes, claims);
    }

    private static boolean isResourcePermission(Policy policy) {
        return "resource".equals(policy.getType());
    }

    private static boolean isScopePermission(Policy policy) {
        return "scope".equals(policy.getType());
    }
}
