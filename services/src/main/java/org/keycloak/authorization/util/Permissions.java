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

        resourceStore.findByOwner(resourceServer.getClientId()).stream().forEach(resource -> permissions.addAll(createResourcePermissions(resource, resourceServer, authorization)));
        resourceStore.findByOwner(identity.getId()).stream().forEach(resource -> permissions.addAll(createResourcePermissions(resource, resourceServer, authorization)));

        return permissions;
    }

    public static List<ResourcePermission> createResourcePermissions(Resource resource, ResourceServer resourceServer, AuthorizationProvider authorization) {
        List<ResourcePermission> permissions = new ArrayList<>();
        List<Scope> scopes = resource.getScopes();

        if (scopes.isEmpty()) {
            String type = resource.getType();

            // check if there is a typed resource whose scopes are inherited by the resource being requested. In this case, we assume that parent resource
            // is owned by the resource server itself
            if (type != null) {
                StoreFactory storeFactory = authorization.getStoreFactory();
                ResourceStore resourceStore = storeFactory.getResourceStore();
                resourceStore.findByType(type).forEach(resource1 -> {
                    if (resource1.getOwner().equals(resourceServer.getClientId())) {
                        scopes.addAll(resource1.getScopes());
                    }
                });
            }
        }

        if (scopes.size() > 1) {
            for (Scope scope : scopes) {
                permissions.add(new ResourcePermission(resource, Arrays.asList(scope), resource.getResourceServer()));
            }
        } else {
            permissions.add(new ResourcePermission(resource, Collections.emptyList(), resource.getResourceServer()));
        }

        return permissions;
    }

    public static List<Permission> allPermits(List<Result> evaluation) {
        List<Permission> permissions = evaluation.stream()
                .filter(evaluationResult -> evaluationResult.getEffect().equals(Effect.PERMIT))
                .map(evaluationResult -> {
                    ResourcePermission permission = evaluationResult.getPermission();
                    String resourceId = null;
                    String resourceName = null;

                    Resource resource = permission.getResource();

                    if (resource != null) {
                        resourceId = resource.getId();
                        resourceName = resource.getName();
                    }

                    Set<String> scopes = permission.getScopes().stream().map(Scope::getName).collect(Collectors.toSet());

                    return new Permission(resourceId, resourceName, scopes);
                }).collect(Collectors.toList());

        Map<String, Permission> perms = new HashMap<>();

        permissions.forEach(permission -> {
            Permission evalPermission = perms.get(permission.getResourceSetId());

            if (evalPermission == null) {
                evalPermission = permission;
                perms.put(permission.getResourceSetId(), evalPermission);
            }

            Set<String> permissionScopes = permission.getScopes();

            if (permissionScopes != null && !permissionScopes.isEmpty()) {
                Set<String> scopes = evalPermission.getScopes();

                if (scopes == null) {
                    scopes = new HashSet();
                    evalPermission.setScopes(scopes);
                }

                for (String scopeName : permissionScopes) {
                    if (!scopes.contains(scopeName)) {
                        scopes.add(scopeName);
                    }
                }
            }
        });

        return perms.values().stream().collect(Collectors.toList());
    }
}
