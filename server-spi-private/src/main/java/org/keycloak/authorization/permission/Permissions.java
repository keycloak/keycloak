/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authorization.permission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationRequest.Metadata;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class Permissions {
    public static ResourcePermission permission(ResourceServer server, Resource resource, Scope scope) {
       return new ResourcePermission(resource, new ArrayList<>(Arrays.asList(scope)), server);
    }

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
    public static void all(ResourceServer resourceServer, Identity identity, AuthorizationProvider authorization, AuthorizationRequest request, Consumer<ResourcePermission> evaluator) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        Metadata metadata = request.getMetadata();
        final AtomicLong limit;

        if (metadata != null && metadata.getLimit() != null) {
            limit = new AtomicLong(metadata.getLimit());
        } else {
            limit = new AtomicLong(Long.MAX_VALUE);
        }

        // obtain all resources where owner is the resource server
        resourceStore.findByOwner(resourceServer, resourceServer.getClientId(), resource -> {
            if (limit.decrementAndGet() >= 0) {
                evaluator.accept(createResourcePermissions(resource, resourceServer, resource.getScopes(), authorization, request));
            }
        });

        // resource server isn't current user
        if (!Objects.equals(resourceServer.getClientId(), identity.getId())) {
            // obtain all resources where owner is the current user
            resourceStore.findByOwner(resourceServer, identity.getId(), resource -> {
                if (limit.decrementAndGet() >= 0) {
                    evaluator.accept(createResourcePermissions(resource, resourceServer, resource.getScopes(), authorization, request));
                }
            });
        }

        // obtain all resources granted to the user via permission tickets (uma)
        List<PermissionTicket> tickets = storeFactory.getPermissionTicketStore().findGranted(resourceServer, identity.getId());

        if (!tickets.isEmpty()) {
            Map<String, ResourcePermission> userManagedPermissions = new HashMap<>();

            for (PermissionTicket ticket : tickets) {
                if (limit.get() < 0) {
                    break;
                }
                ResourcePermission permission = userManagedPermissions.computeIfAbsent(ticket.getResource().getId(),
                        s -> {
                            limit.decrementAndGet();
                            ResourcePermission resourcePermission = new ResourcePermission(ticket.getResource(),
                                    new ArrayList<>(), resourceServer,
                                    request.getClaims());
                            resourcePermission.setGranted(true);
                            return resourcePermission;
                        });

                permission.addScope(ticket.getScope());
            }

            for (ResourcePermission permission : userManagedPermissions.values()) {
                evaluator.accept(permission);
            }
        }
    }

    public static ResourcePermission createResourcePermissions(Resource resource,
            ResourceServer resourceServer, Collection<Scope> requestedScopes,
            AuthorizationProvider authorization, AuthorizationRequest request) {
        Set<Scope> scopes = resolveScopes(resource, resourceServer, requestedScopes, authorization);
        return new ResourcePermission(resource, scopes, resourceServer, request.getClaims());
    }

    public static ResourcePermission createResourcePermissions(String resourceType, Resource resource,
                                                               ResourceServer resourceServer, Collection<Scope> requestedScopes,
                                                               AuthorizationProvider authorization, AuthorizationRequest request) {
        Set<Scope> scopes = resolveScopes(resource, resourceServer, requestedScopes, authorization);
        return new ResourcePermission(resourceType, resource, scopes, resourceServer, request.getClaims());
    }
    
    public static Set<Scope> resolveScopes(Resource resource, ResourceServer resourceServer,
            Collection<Scope> requestedScopes, AuthorizationProvider authorization) {
        if (requestedScopes.isEmpty()) {
            return populateTypedScopes(resource, resourceServer, authorization);
        }
        return populateTypedScopes(resource, resourceServer, resource.getScopes(), authorization).stream()
                .filter(scope -> requestedScopes.contains(scope)).collect(Collectors.toSet());
    }

    private static Set<Scope> populateTypedScopes(Resource resource,
            ResourceServer resourceServer, AuthorizationProvider authorization) {
        return populateTypedScopes(resource, resourceServer, resource.getScopes(), authorization);
    }

    private static Set<Scope> populateTypedScopes(Resource resource, ResourceServer resourceServer, List<Scope> defaultScopes, AuthorizationProvider authorization) {
        String type = resource.getType();
        if (type == null || resource.getOwner().equals(resourceServer.getId())) {
            return new LinkedHashSet<>(defaultScopes);
        }

        Set<Scope> scopes = new LinkedHashSet<>(defaultScopes);

        // check if there is a typed resource whose scopes are inherited by the resource being requested. In this case, we assume that parent resource
        // is owned by the resource server itself
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        resourceStore.findByType(resourceServer, type, resource1 -> {
            for (Scope typeScope : resource1.getScopes()) {
                if (!scopes.contains(typeScope)) {
                    scopes.add(typeScope);
                }
            }
        });

        return scopes;
    }
}
