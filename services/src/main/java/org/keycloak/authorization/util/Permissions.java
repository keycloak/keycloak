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

package org.keycloak.authorization.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision.Effect;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.Result;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.representations.idm.authorization.Permission;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class Permissions {

    /**
     * Returns a list of permissions for all resources and scopes that belong to the given <code>resourceServer</code> and
     * <code>identity</code>.
     *
     * TODO: review once we support caches
     *
     * @param resourceServer
     * @param identity
     * @param authorization
     * @return
     */
    public static List<ResourcePermission> all(ResourceServer resourceServer, Identity identity, AuthorizationProvider authorization) {
        List<ResourcePermission> permissions = new ArrayList<>();
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceStore resourceStore = storeFactory.getResourceStore();

        resourceStore.findByOwner(resourceServer.getClientId(), resourceServer.getId()).stream().forEach(resource -> permissions.addAll(createResourcePermissionsWithScopes(resource, resource.getScopes(), authorization)));
        resourceStore.findByOwner(identity.getId(), resourceServer.getId()).stream().forEach(resource -> permissions.addAll(createResourcePermissionsWithScopes(resource, resource.getScopes(), authorization)));

        return permissions;
    }

    public static List<ResourcePermission> createResourcePermissions(Resource resource, Set<String> requestedScopes, AuthorizationProvider authorization) {
        List<ResourcePermission> permissions = new ArrayList<>();
        String type = resource.getType();
        ResourceServer resourceServer = resource.getResourceServer();
        List<Scope> scopes;

        if (requestedScopes.isEmpty()) {
            scopes = resource.getScopes();
            // check if there is a typed resource whose scopes are inherited by the resource being requested. In this case, we assume that parent resource
            // is owned by the resource server itself
            if (type != null && !resource.getOwner().equals(resourceServer.getClientId())) {
                StoreFactory storeFactory = authorization.getStoreFactory();
                ResourceStore resourceStore = storeFactory.getResourceStore();
                resourceStore.findByType(type, resourceServer.getId()).forEach(resource1 -> {
                    if (resource1.getOwner().equals(resourceServer.getClientId())) {
                        for (Scope typeScope : resource1.getScopes()) {
                            if (!scopes.contains(typeScope)) {
                                scopes.add(typeScope);
                            }
                        }
                    }
                });
            }
        } else {
            ScopeStore scopeStore = authorization.getStoreFactory().getScopeStore();
            scopes = requestedScopes.stream().map(scopeName -> {
                Scope byName = scopeStore.findByName(scopeName, resource.getResourceServer().getId());

                if (byName == null) {
                    throw new RuntimeException("Invalid scope [" + scopeName + "].");
                }

                return byName;
            }).collect(Collectors.toList());
        }

        permissions.add(new ResourcePermission(resource, scopes, resource.getResourceServer()));

        return permissions;
    }

    public static List<ResourcePermission> createResourcePermissionsWithScopes(Resource resource, List<Scope> scopes, AuthorizationProvider authorization) {
        List<ResourcePermission> permissions = new ArrayList<>();
        String type = resource.getType();
        ResourceServer resourceServer = resource.getResourceServer();

        // check if there is a typed resource whose scopes are inherited by the resource being requested. In this case, we assume that parent resource
        // is owned by the resource server itself
        if (type != null && !resource.getOwner().equals(resourceServer.getClientId())) {
            StoreFactory storeFactory = authorization.getStoreFactory();
            ResourceStore resourceStore = storeFactory.getResourceStore();
            resourceStore.findByType(type, resourceServer.getId()).forEach(resource1 -> {
                if (resource1.getOwner().equals(resourceServer.getClientId())) {
                    for (Scope typeScope : resource1.getScopes()) {
                        if (!scopes.contains(typeScope)) {
                            scopes.add(typeScope);
                        }
                    }
                }
            });
        }

        permissions.add(new ResourcePermission(resource, scopes, resource.getResourceServer()));

        return permissions;
    }

    public static List<Permission> permits(List<Result> evaluation, AuthorizationProvider authorizationProvider, String resourceServerId) {
        Map<String, Permission> permissions = new HashMap<>();

        for (Result result : evaluation) {
            Set<Scope> deniedScopes = new HashSet<>();
            Set<Scope> grantedScopes = new HashSet<>();
            boolean resourceDenied = false;
            ResourcePermission permission = result.getPermission();
            List<Result.PolicyResult> results = result.getResults();
            int deniedCount = results.size();

            for (Result.PolicyResult policyResult : results) {
                Policy policy = policyResult.getPolicy();
                Set<Scope> policyScopes = policy.getScopes();

                if (Effect.PERMIT.equals(policyResult.getStatus())) {
                    if (isScopePermission(policy)) {
                        // try to grant any scope from a scope-based permission
                        grantedScopes.addAll(policyScopes);
                    } else if (isResourcePermission(policy)) {
                        // we assume that all requested scopes should be granted given that we are processing a resource-based permission.
                        // Later they will be filtered based on any denied scope, if any.
                        // TODO: we could probably provide a configuration option to let users decide whether or not a resource-based permission should grant all scopes associated with the resource.
                        grantedScopes.addAll(permission.getScopes());
                    }
                    deniedCount--;
                } else {
                    if (isScopePermission(policy)) {
                        // store all scopes associated with the scope-based permission
                        deniedScopes.addAll(policyScopes);
                    } else if (isResourcePermission(policy)) {
                        // we should not grant anything
                        resourceDenied = true;
                        break;
                    }
                }
            }

            if (!resourceDenied) {
                // remove any scope denied from the list of granted scopes
                if (!deniedScopes.isEmpty()) {
                    grantedScopes.removeAll(deniedScopes);
                }

                // if there are no policy results is because the permission didn't match any policy.
                // In this case, if results is empty is because we are in permissive mode.
                if (!results.isEmpty()) {
                    // update the current permission with the granted scopes
                    permission.getScopes().clear();
                    permission.getScopes().addAll(grantedScopes);
                }

                if (deniedCount == 0) {
                    result.setStatus(Effect.PERMIT);
                    grantPermission(authorizationProvider, permissions, permission, resourceServerId);
                } else {
                    // if a full deny or resource denied or the requested scopes were denied
                    if (deniedCount == results.size() || resourceDenied || (!deniedScopes.isEmpty() && grantedScopes.isEmpty())) {
                        result.setStatus(Effect.DENY);
                    } else {
                        result.setStatus(Effect.PERMIT);
                        grantPermission(authorizationProvider, permissions, permission, resourceServerId);
                    }
                }
            }
        }

        return permissions.values().stream().collect(Collectors.toList());
    }

    private static boolean isResourcePermission(Policy policy) {
        return "resource".equals(policy.getType());
    }

    private static boolean isScopePermission(Policy policy) {
        return "scope".equals(policy.getType());
    }

    private static void grantPermission(AuthorizationProvider authorizationProvider, Map<String, Permission> permissions, ResourcePermission permission, String resourceServer) {
        List<Resource> resources = new ArrayList<>();
        Resource resource = permission.getResource();
        Set<String> scopes = permission.getScopes().stream().map(Scope::getName).collect(Collectors.toSet());

        if (resource != null) {
            resources.add(resource);
        } else {
            List<Scope> permissionScopes = permission.getScopes();

            if (!permissionScopes.isEmpty()) {
                ResourceStore resourceStore = authorizationProvider.getStoreFactory().getResourceStore();
                resources.addAll(resourceStore.findByScope(permissionScopes.stream().map(Scope::getId).collect(Collectors.toList()), resourceServer));
            }
        }

        if (!resources.isEmpty()) {
            for (Resource allowedResource : resources) {
                String resourceId = allowedResource.getId();
                String resourceName = allowedResource.getName();
                Permission evalPermission = permissions.get(allowedResource.getId());

                if (evalPermission == null) {
                    evalPermission = new Permission(resourceId, resourceName, scopes);
                    permissions.put(resourceId, evalPermission);
                }

                if (scopes != null && !scopes.isEmpty()) {
                    Set<String> finalScopes = evalPermission.getScopes();

                    if (finalScopes == null) {
                        finalScopes = new HashSet();
                        evalPermission.setScopes(finalScopes);
                    }

                    for (String scopeName : scopes) {
                        if (!finalScopes.contains(scopeName)) {
                            finalScopes.add(scopeName);
                        }
                    }
                }
            }
        } else {
            Permission scopePermission = new Permission(null, null, scopes);
            permissions.put(scopePermission.toString(), scopePermission);
        }
    }
}
