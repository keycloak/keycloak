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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
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
    public static List<ResourcePermission> all(ResourceServer resourceServer, Identity identity, AuthorizationProvider authorization, AuthorizationRequest request) {
        List<ResourcePermission> permissions = new ArrayList<>();
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
        resourceStore.findByOwner(resourceServer.getId(), resourceServer.getId(), resource -> {
            if (limit.decrementAndGet() >= 0) {
                permissions.add(createResourcePermissions(resource, authorization, request));
            }
        });

        // resource server isn't current user
        if (resourceServer.getId() != identity.getId()) {
            // obtain all resources where owner is the current user
            resourceStore.findByOwner(identity.getId(), resourceServer.getId(), resource -> {
                if (limit.decrementAndGet() >= 0) {
                    permissions.add(createResourcePermissions(resource, authorization, request));
                }
            });
        }

        // obtain all resources granted to the user via permission tickets (uma)
        List<PermissionTicket> tickets = storeFactory.getPermissionTicketStore().findGranted(identity.getId(), resourceServer.getId());

        if (!tickets.isEmpty()) {
            Map<String, ResourcePermission> userManagedPermissions = new HashMap<>();

            for (PermissionTicket ticket : tickets) {
                ResourcePermission permission = userManagedPermissions.get(ticket.getResource().getId());

                if (permission == null) {
                    userManagedPermissions.put(ticket.getResource().getId(), new ResourcePermission(ticket.getResource(), new ArrayList<>(), resourceServer, request.getClaims()));
                    limit.decrementAndGet();
                }

                if (limit.decrementAndGet() <= 0) {
                    break;
                }
            }

            permissions.addAll(userManagedPermissions.values());
        }

        return permissions;
    }

    public static ResourcePermission createResourcePermissions(Resource resource, Collection<Scope> requestedScopes, AuthorizationProvider authorization, AuthorizationRequest request) {
        List<Scope> scopes;

        if (requestedScopes.isEmpty()) {
            scopes = populateTypedScopes(resource, authorization);
        } else {
            scopes = populateTypedScopes(resource, requestedScopes.stream().filter(scope -> resource.getScopes().contains(scope)).collect(Collectors.toList()), authorization);
        }

        return new ResourcePermission(resource, scopes, resource.getResourceServer(), request.getClaims());
    }

    public static ResourcePermission createResourcePermissions(Resource resource, AuthorizationProvider authorization, AuthorizationRequest request) {
        List<Scope> requestedScopes = resource.getScopes();

        if (requestedScopes.isEmpty()) {
            return new ResourcePermission(resource, populateTypedScopes(resource, authorization), resource.getResourceServer(), request.getClaims());
        }

        return new ResourcePermission(resource, resource.getResourceServer(), request.getClaims());
    }

    private static List<Scope> populateTypedScopes(Resource resource, AuthorizationProvider authorization) {
        return populateTypedScopes(resource, resource.getScopes(), authorization);
    }

    private static List<Scope> populateTypedScopes(Resource resource, List<Scope> defaultScopes, AuthorizationProvider authorization) {
        String type = resource.getType();
        ResourceServer resourceServer = resource.getResourceServer();

        if (type == null || resource.getOwner().equals(resourceServer.getId())) {
            return new ArrayList<>(defaultScopes);
        }

        List<Scope> scopes = new ArrayList<>(defaultScopes);

        // check if there is a typed resource whose scopes are inherited by the resource being requested. In this case, we assume that parent resource
        // is owned by the resource server itself
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        resourceStore.findByType(type, resourceServer.getId(), resource1 -> {
            for (Scope typeScope : resource1.getScopes()) {
                if (!scopes.contains(typeScope)) {
                    scopes.add(typeScope);
                }
            }
        });

        return scopes;
    }
}
