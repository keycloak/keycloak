/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.resources.account.resources;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.managers.Auth;
import org.keycloak.utils.MediaType;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceService extends AbstractResourceService {

    private final org.keycloak.authorization.model.Resource resource;
    private final ResourceServer resourceServer;

    ResourceService(org.keycloak.authorization.model.Resource resource, KeycloakSession session, UserModel user,
            Auth auth, HttpRequest request) {
        super(session, user, auth, request);
        this.resource = resource;
        this.resourceServer = resource.getResourceServer();
    }

    /**
     * Returns a {@link Resource} where the {@link #user} is the resource owner.
     * 
     * @return the resource
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResource() {
        return cors(Response.ok(new Resource(resource, provider)));
    }

    /**
     * Returns a list of {@link Permission} containing the users to which the {@link #user} granted access to a resource.
     * 
     * @return the users with access to a resource
     */
    @GET
    @Path("permissions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response toPermissions() {
        Map<String, String> filters = new HashMap<>();

        filters.put(PermissionTicket.OWNER, user.getId());
        filters.put(PermissionTicket.GRANTED, Boolean.TRUE.toString());
        filters.put(PermissionTicket.RESOURCE, resource.getId());

        Collection<ResourcePermission> resources = toPermissions(ticketStore.find(filters, null, -1, -1));
        Collection<Permission> permissions = Collections.EMPTY_LIST;
        
        if (!resources.isEmpty()) {
            permissions = resources.iterator().next().getPermissions();
        }

        return cors(Response.ok(permissions));
    }

    /**
     * Updates the permission set for a resource based on the given {@code permissions}.
     *
     * @param permissions the permissions that should be updated
     * @return if successful, a {@link Response.Status#NO_CONTENT} response
     */
    @PUT
    @Path("permissions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response revoke(List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            throw new BadRequestException("invalid_permissions");    
        }
        
        ResourceServer resourceServer = resource.getResourceServer();
        Map<String, String> filters = new HashMap<>();

        filters.put(PermissionTicket.RESOURCE, resource.getId());

        for (Permission permission : permissions) {
            UserModel user = getUser(permission.getUsername());

            filters.put(PermissionTicket.REQUESTER, user.getId());

            List<PermissionTicket> tickets = ticketStore.find(filters, resource.getResourceServer().getId(), -1, -1);

            // grants all requested permissions
            if (tickets.isEmpty()) {
                for (String scope : permission.getScopes()) {
                    grantPermission(user, scope);
                }
            } else {
                Iterator<String> scopesIterator = permission.getScopes().iterator();

                while (scopesIterator.hasNext()) {
                    org.keycloak.authorization.model.Scope scope = getScope(scopesIterator.next(), resourceServer);
                    Iterator<PermissionTicket> ticketIterator = tickets.iterator();

                    while (ticketIterator.hasNext()) {
                        PermissionTicket ticket = ticketIterator.next();

                        if (scope.getId().equals(ticket.getScope().getId())) {
                            if (!ticket.isGranted()) {
                                ticket.setGrantedTimestamp(System.currentTimeMillis());
                            }
                            // permission exists, remove from the list to avoid deletion
                            ticketIterator.remove();
                            // scope already granted, remove from the list to avoid creating it again
                            scopesIterator.remove();
                        }
                    }
                }

                // only create permissions for the scopes that don't have a tocket
                for (String scope : permission.getScopes()) {
                    grantPermission(user, scope);
                }
                
                // remove all tickets that are not within the requested permissions
                for (PermissionTicket ticket : tickets) {
                    ticketStore.delete(ticket.getId());
                }                
            }
        }

        return cors(Response.noContent());
    }

    /**
     * Returns a list of {@link Permission} requests waiting for the {@link #user} approval.
     *
     * @return the permission requests waiting for the user approval
     */
    @GET
    @Path("permissions/requests")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPermissionRequests() {
        Map<String, String> filters = new HashMap<>();

        filters.put(PermissionTicket.OWNER, user.getId());
        filters.put(PermissionTicket.GRANTED, Boolean.FALSE.toString());
        filters.put(PermissionTicket.RESOURCE, resource.getId());
        
        Map<String, Permission> requests = new HashMap<>();

        for (PermissionTicket ticket : ticketStore.find(filters, null, -1, -1)) {
            requests.computeIfAbsent(ticket.getRequester(), requester -> new Permission(ticket, provider)).addScope(ticket.getScope().getName());
        }
        
        return cors(Response.ok(requests.values()));
    }

    private void grantPermission(UserModel user, String scopeId) {
        org.keycloak.authorization.model.Scope scope = getScope(scopeId, resourceServer);
        PermissionTicket ticket = ticketStore.create(resource.getId(), scope.getId(), user.getId(), resourceServer);
        ticket.setGrantedTimestamp(Calendar.getInstance().getTimeInMillis());
    }

    private org.keycloak.authorization.model.Scope getScope(String scopeId, ResourceServer resourceServer) {
        org.keycloak.authorization.model.Scope scope = scopeStore.findByName(scopeId, resourceServer.getId());

        if (scope == null) {
            scope = scopeStore.findById(scopeId, resourceServer.getId());
        }
        
        return scope;
    }

    private UserModel getUser(String requester) {
        UserProvider users = provider.getKeycloakSession().users();
        UserModel user = users.getUserByUsername(requester, provider.getRealm());

        if (user == null) {
            user = users.getUserById(requester, provider.getRealm());
        }

        return user;
    }

    private Collection<ResourcePermission> toPermissions(List<PermissionTicket> tickets) {
        Map<String, ResourcePermission> permissions = new HashMap<>();

        for (PermissionTicket ticket : tickets) {
            ResourcePermission resource = permissions
                    .computeIfAbsent(ticket.getResource().getId(), s -> new ResourcePermission(ticket, provider));

            Permission user = resource.getPermission(ticket.getRequester());

            if (user == null) {
                resource.addPermission(ticket.getRequester(), user = new Permission(ticket.getRequester(), provider));
            }

            user.addScope(ticket.getScope().getName());
        }

        return permissions.values();
    }
}
