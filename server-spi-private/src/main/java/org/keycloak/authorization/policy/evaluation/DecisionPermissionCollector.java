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
        List<Scope> requestedScopes = permission.getScopes();

        if (Effect.PERMIT.equals(result.getEffect())) {
            grantPermission(authorizationProvider, permissions, permission, resource != null ? resource.getScopes() : requestedScopes, resourceServer, request, result);
        } else {
            Set<Scope> grantedScopes = new HashSet<>();
            Set<Scope> deniedScopes = new HashSet<>();
            List<Result.PolicyResult> userManagedPermissions = new ArrayList<>();
            boolean resourceGranted = false;
            boolean anyDeny = false;

            for (Result.PolicyResult policyResult : result.getResults()) {
                Policy policy = policyResult.getPolicy();
                Set<Scope> policyScopes = policy.getScopes();

                if (isGranted(policyResult)) {
                    if (isScopePermission(policy)) {
                        for (Scope scope : requestedScopes) {
                            if (policyScopes.contains(scope)) {
                                grantedScopes.add(scope);
                            }
                        }
                    } else if (isResourcePermission(policy)) {
                        grantedScopes.addAll(requestedScopes);
                    } else if (resource != null && resource.isOwnerManagedAccess() && "uma".equals(policy.getType())) {
                        userManagedPermissions.add(policyResult);
                    }
                    if (!resourceGranted) {
                        resourceGranted = policy.getResources().contains(resource);
                    }
                } else {
                    if (isResourcePermission(policy)) {
                        if (!resourceGranted) {
                            deniedScopes.addAll(requestedScopes);
                        }
                    } else {
                        deniedScopes.addAll(policyScopes);
                    }
                    if (!anyDeny) {
                        anyDeny = true;
                    }
                }
            }

            // remove any scope denied from the list of granted scopes
            grantedScopes.removeAll(deniedScopes);

            if (userManagedPermissions.isEmpty()) {
                if (!resourceGranted && (grantedScopes.isEmpty() && !requestedScopes.isEmpty())) {
                    return;
                }
            } else {
                for (Result.PolicyResult userManagedPermission : userManagedPermissions) {
                    grantedScopes.addAll(userManagedPermission.getPolicy().getScopes());
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

    public Collection<Permission> results() {
        return permissions;
    }

    @Override
    public void onError(Throwable cause) {
        throw new RuntimeException("Failed to evaluate permissions", cause);
    }

    protected void grantPermission(AuthorizationProvider authorizationProvider, List<Permission> permissions, ResourcePermission permission, Collection<Scope> grantedScopes, ResourceServer resourceServer, AuthorizationRequest request, Result result) {
        Set<String> scopeNames = grantedScopes.stream().map(Scope::getName).collect(Collectors.toSet());
        Resource resource = permission.getResource();

        if (resource != null) {
            permissions.add(createPermission(resource, scopeNames, permission.getClaims(), request));
        } else if (!grantedScopes.isEmpty()) {
            ResourceStore resourceStore = authorizationProvider.getStoreFactory().getResourceStore();

            resourceStore.findByScope(grantedScopes.stream().map(Scope::getId).collect(Collectors.toList()), resourceServer.getId(), resource1 -> permissions.add(createPermission(resource, scopeNames, permission.getClaims(), request)));

            if (permissions.isEmpty()) {
                permissions.add(createPermission(null, scopeNames, permission.getClaims(), request));
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
