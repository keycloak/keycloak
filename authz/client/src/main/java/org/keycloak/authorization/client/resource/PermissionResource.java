/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
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
package org.keycloak.authorization.client.resource;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.type.TypeReference;
import org.keycloak.authorization.client.representation.ServerConfiguration;
import org.keycloak.authorization.client.util.Http;
import org.keycloak.authorization.client.util.Throwables;
import org.keycloak.authorization.client.util.TokenCallable;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.PermissionResponse;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;
import org.keycloak.util.JsonSerialization;

/**
 * An entry point for managing permission tickets using the Protection API.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PermissionResource {

    private final Http http;
    private final ServerConfiguration serverConfiguration;
    private final TokenCallable pat;

    public PermissionResource(Http http, ServerConfiguration serverConfiguration, TokenCallable pat) {
        this.http = http;
        this.serverConfiguration = serverConfiguration;
        this.pat = pat;
    }

    /**
     * @deprecated use {@link #create(PermissionRequest)}
     * @param request
     * @return
     */
    @Deprecated
    public PermissionResponse forResource(PermissionRequest request) {
        return create(request);
    }

    /**
     * Creates a new permission ticket for a single resource and scope(s).
     *
     * @param request the {@link PermissionRequest} representing the resource and scope(s) (not {@code null})
     * @return a permission response holding a permission ticket with the requested permissions
     */
    public PermissionResponse create(PermissionRequest request) {
        return create(Arrays.asList(request));
    }

    /**
     * Creates a new permission ticket for a set of one or more resource and scope(s).
     *
     * @param request the {@link PermissionRequest} representing the resource and scope(s) (not {@code null})
     * @return a permission response holding a permission ticket with the requested permissions
     */
    public PermissionResponse create(final List<PermissionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Permission request must not be null or empty");
        }
        Callable<PermissionResponse> callable = new Callable<PermissionResponse>() {
            @Override
            public PermissionResponse call() throws Exception {
                return http.<PermissionResponse>post(serverConfiguration.getPermissionEndpoint())
                        .authorizationBearer(pat.call())
                        .json(JsonSerialization.writeValueAsBytes(requests))
                        .response().json(PermissionResponse.class).execute();
            }
        };
        try {
            return callable.call();
        } catch (Exception cause) {
            return Throwables.retryAndWrapExceptionIfNecessary(callable, pat, "Error creating permission ticket", cause);
        }
    }

    /**
     * Creates a new uma permission for a single resource and scope(s).
     *
     * @param ticket the {@link PermissionTicketRepresentation} representing the resource and scope(s) (not {@code null})
     * @return a permission response holding the permission ticket representation
     */
    public PermissionTicketRepresentation create(final PermissionTicketRepresentation ticket) {
        if (ticket == null) {
            throw new IllegalArgumentException("Permission ticket must not be null or empty");
        }
        if (ticket.getRequester() == null && ticket.getRequesterName() == null) {
            throw new IllegalArgumentException("Permission ticket must have a requester");
        }
        if (ticket.getResource() == null && ticket.getResourceName() == null) {
            throw new IllegalArgumentException("Permission ticket must have a resource");
        }
        if (ticket.getScope() == null && ticket.getScopeName() == null) {
            throw new IllegalArgumentException("Permission ticket must have a scope");
        }
        Callable<PermissionTicketRepresentation> callable = new Callable<PermissionTicketRepresentation>() {
            @Override
            public PermissionTicketRepresentation call() throws Exception {
                return http.<PermissionTicketRepresentation>post(serverConfiguration.getPermissionEndpoint()+"/ticket")
                        .json(JsonSerialization.writeValueAsBytes(ticket))
                        .authorizationBearer(pat.call())
                        .response().json(new TypeReference<PermissionTicketRepresentation>(){}).execute();
            }
        };
        try {
            return callable.call();
        } catch (Exception cause) {
            return Throwables.retryAndWrapExceptionIfNecessary(callable, pat, "Error updating permission ticket", cause);
        }
    }
    
    /**
     * Query the server for any permission ticket associated with the given <code>scopeId</code>.
     *
     * @param scopeId the scope id (not {@code null})
     * @return a list of permission tickets associated with the given <code>scopeId</code>
     */
    public List<PermissionTicketRepresentation> findByScope(final String scopeId) {
        if (scopeId == null) {
            throw new IllegalArgumentException("Scope id must not be null");
        }
        Callable<List<PermissionTicketRepresentation>> callable = new Callable<List<PermissionTicketRepresentation>>() {
            @Override
            public List<PermissionTicketRepresentation> call() throws Exception {
                return http.<List<PermissionTicketRepresentation>>get(serverConfiguration.getPermissionEndpoint()+"/ticket")
                        .authorizationBearer(pat.call())
                        .param("scopeId", scopeId)
                        .response().json(new TypeReference<List<PermissionTicketRepresentation>>(){}).execute();
            }
        };
        try {
            return callable.call();
        } catch (Exception cause) {
            return Throwables.retryAndWrapExceptionIfNecessary(callable, pat, "Error querying permission ticket by scope", cause);
        }
    }

    /**
     * Query the server for any permission ticket associated with the given <code>resourceId</code>.
     *
     * @param resourceId the resource id (not {@code null})
     * @return a list of permission tickets associated with the given <code>resourceId</code>
     */
    public List<PermissionTicketRepresentation> findByResource(final String resourceId) {
        if (resourceId == null) {
            throw new IllegalArgumentException("Resource id must not be null");
        }
        Callable<List<PermissionTicketRepresentation>> callable = new Callable<List<PermissionTicketRepresentation>>() {
            @Override
            public List<PermissionTicketRepresentation> call() throws Exception {
                return http.<List<PermissionTicketRepresentation>>get(serverConfiguration.getPermissionEndpoint()+"/ticket")
                        .authorizationBearer(pat.call())
                        .param("resourceId", resourceId)
                        .response().json(new TypeReference<List<PermissionTicketRepresentation>>(){}).execute();
            }
        };
        try {
            return callable.call();
        } catch (Exception cause) {
            return Throwables.retryAndWrapExceptionIfNecessary(callable, pat, "Error querying permission ticket by resource", cause);
        }
    }

    /**
     * Query the server for any permission ticket with the matching arguments.
     *
     * @param resourceId the resource id or name
     * @param scopeId the scope id or name
     * @param owner the owner id or name
     * @param requester the requester id or name
     * @param granted if true, only permission tickets marked as granted are returned.
     * @param returnNames if the response should include names for resource, scope and owner
     * @param firstResult the position of the first resource to retrieve
     * @param maxResult the maximum number of resources to retrieve
     * @return a list of permission tickets with the matching arguments
     */
    public List<PermissionTicketRepresentation> find(final String resourceId,
                                                     final String scopeId,
                                                     final String owner,
                                                     final String requester,
                                                     final Boolean granted,
                                                     final Boolean returnNames,
                                                     final Integer firstResult,
                                                     final Integer maxResult) {
        Callable<List<PermissionTicketRepresentation>> callable = new Callable<List<PermissionTicketRepresentation>>() {
            @Override
            public List<PermissionTicketRepresentation> call() throws Exception {
                return http.<List<PermissionTicketRepresentation>>get(serverConfiguration.getPermissionEndpoint()+"/ticket")
                        .authorizationBearer(pat.call())
                        .param("resourceId", resourceId)
                        .param("scopeId", scopeId)
                        .param("owner", owner)
                        .param("requester", requester)
                        .param("granted", granted == null ? null : granted.toString())
                        .param("returnNames", returnNames == null ? null : returnNames.toString())
                        .param("first", firstResult == null ? null : firstResult.toString())
                        .param("max", maxResult == null ? null : maxResult.toString())
                        .response().json(new TypeReference<List<PermissionTicketRepresentation>>(){}).execute();
            }
        };
        try {
            return callable.call();
        } catch (Exception cause) {
            return Throwables.retryAndWrapExceptionIfNecessary(callable, pat, "Error querying permission ticket", cause);
        }
    }

    /**
     * Updates a permission ticket.
     *
     * @param ticket the permission ticket
     */
    public void update(final PermissionTicketRepresentation ticket) {
        if (ticket == null) {
            throw new IllegalArgumentException("Permission ticket must not be null or empty");
        }
        if (ticket.getId() == null) {
            throw new IllegalArgumentException("Permission ticket must have an id");
        }
        Callable callable = new Callable() {
            @Override
            public Object call() throws Exception {
                http.<List>put(serverConfiguration.getPermissionEndpoint()+"/ticket")
                        .json(JsonSerialization.writeValueAsBytes(ticket))
                        .authorizationBearer(pat.call())
                        .response().json(List.class).execute();
                return null;
            }
        };
        try {
            callable.call();
        } catch (Exception cause) {
            Throwables.retryAndWrapExceptionIfNecessary(callable, pat, "Error updating permission ticket", cause);
        }
    }
}
