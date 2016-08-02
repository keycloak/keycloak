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

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision.Effect;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.Result;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.representations.idm.authorization.Permission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

        resourceStore.findByOwner(resourceServer.getClientId()).stream().forEach(resource -> permissions.addAll(createResourcePermissions(resource, resource.getScopes().stream().map(Scope::getName).collect(Collectors.toSet()), authorization)));
        resourceStore.findByOwner(identity.getId()).stream().forEach(resource -> permissions.addAll(createResourcePermissions(resource, resource.getScopes().stream().map(Scope::getName).collect(Collectors.toSet()), authorization)));

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
                resourceStore.findByType(type).forEach(resource1 -> {
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
                return byName;
            }).collect(Collectors.toList());
        }

        if (scopes.isEmpty()) {
            permissions.add(new ResourcePermission(resource, Collections.emptyList(), resource.getResourceServer()));
        } else {
            for (Scope scope : scopes) {
                permissions.add(new ResourcePermission(resource, Arrays.asList(scope), resource.getResourceServer()));
            }
        }

        return permissions;
    }

    public static List<Permission> allPermits(List<Result> evaluation, AuthorizationProvider authorizationProvider) {
        Map<String, Permission> permissions = new HashMap<>();

        for (Result evaluationResult : evaluation) {
            ResourcePermission permission = evaluationResult.getPermission();
            Set<String> scopes = permission.getScopes().stream().map(Scope::getName).collect(Collectors.toSet());

            if (evaluationResult.getEffect().equals(Effect.DENY)) {
                continue;
            }

            List<Resource> resources = new ArrayList<>();
            Resource resource = permission.getResource();

            if (resource != null) {
                resources.add(resource);
            } else {
                List<Scope> permissionScopes = permission.getScopes();

                if (!permissionScopes.isEmpty()) {
                    ResourceStore resourceStore = authorizationProvider.getStoreFactory().getResourceStore();
                    resources.addAll(resourceStore.findByScope(permissionScopes.stream().map(Scope::getId).collect(Collectors.toList()).toArray(new String[permissionScopes.size()])));
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

        return permissions.values().stream().collect(Collectors.toList());
    }
}
