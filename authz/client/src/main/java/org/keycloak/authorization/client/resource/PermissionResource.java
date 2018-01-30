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

import static org.keycloak.authorization.client.util.Throwables.handleAndWrapException;

import java.util.List;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.type.TypeReference;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.PermissionRequest;
import org.keycloak.authorization.client.representation.PermissionResponse;
import org.keycloak.authorization.client.util.Http;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;
import org.keycloak.util.JsonSerialization;

/**
 * An entry point for managing permission tickets using the Protection API.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PermissionResource {

    private final Http http;
    private final Configuration configuration;
    private final Callable<String> pat;

    public PermissionResource(Http http, Configuration configuration, Callable<String> pat) {
        this.http = http;
        this.configuration = configuration;
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
        if (request == null) {
            throw new IllegalArgumentException("Permission request must not be null");
        }
        try {
            return this.http.<PermissionResponse>post("/authz/protection/" + configuration.getResource() + "/permission")
                    .authorizationBearer(this.pat.call())
                    .json(JsonSerialization.writeValueAsBytes(request))
                    .response().json(PermissionResponse.class).execute();
        } catch (Exception cause) {
            throw handleAndWrapException("Error obtaining permission ticket", cause);
        }
    }

    /**
     * Creates a new permission ticket for a set of one or more resource and scope(s).
     *
     * @param request the {@link PermissionRequest} representing the resource and scope(s) (not {@code null})
     * @return a permission response holding a permission ticket with the requested permissions
     */
    public PermissionResponse create(List<PermissionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Permission request must not be null or empty");
        }
        try {
            return http.<PermissionResponse>post("/authz/protection/" + configuration.getResource() + "/permissions")
                    .authorizationBearer(this.pat.get())
                    .json(JsonSerialization.writeValueAsBytes(requests))
                    .response().json(PermissionResponse.class).execute();
        } catch (Exception cause) {
            throw handleAndWrapException("Error obtaining permission ticket", cause);
        }
    }

    /**
     * Query the server for any permission ticket associated with the given <code>scopeId</code>.
     *
     * @param scopeId the scope id (not {@code null})
     * @return a list of permission tickets associated with the given <code>scopeId</code>
     */
    public List<PermissionTicketRepresentation> findByScope(String scopeId) {
        if (scopeId == null) {
            throw new IllegalArgumentException("Scope id must not be null");
        }
        return http.<List<PermissionTicketRepresentation>>get("/authz/protection/" + configuration.getResource() + "/permission")
                .authorizationBearer(this.pat.get())
                .param("scopeId", scopeId)
                .response().json(new TypeReference<List<PermissionTicketRepresentation>>() {}).execute();
    }

    /**
     * Query the server for any permission ticket associated with the given <code>resourceId</code>.
     *
     * @param resourceId the resource id (not {@code null})
     * @return a list of permission tickets associated with the given <code>resourceId</code>
     */
    public List<PermissionTicketRepresentation> findByResource(String resourceId) {
        if (resourceId == null) {
            throw new IllegalArgumentException("Resource id must not be null");
        }
        return http.<List<PermissionTicketRepresentation>>get("/authz/protection/" + configuration.getResource() + "/permission")
                .authorizationBearer(this.pat.get())
                .param("resourceId", resourceId)
                .response().json(new TypeReference<List<PermissionTicketRepresentation>>() {
                }).execute();
    }

    public List<PermissionTicketRepresentation> find(String resourceId,
                                                     String scopeId,
                                                     String owner,
                                                     String requester,
                                                     Boolean granted,
                                                     Boolean returnNames,
                                                     Integer firstResult,
                                                     Integer maxResult) {
        return http.<List<PermissionTicketRepresentation>>get("/authz/protection/" + configuration.getResource() + "/permission")
                .authorizationBearer(this.pat.get())
                .param("resourceId", resourceId)
                .param("scopeId", scopeId)
                .param("owner", owner)
                .param("requester", requester)
                .param("granted", granted == null ? null : granted.toString())
                .param("returnNames", returnNames == null ? null : returnNames.toString())
                .param("firstResult", firstResult == null ? null : firstResult.toString())
                .param("maxResult", maxResult == null ? null : maxResult.toString())
                .response().json(new TypeReference<List<PermissionTicketRepresentation>>() {
                }).execute();
    }

    /**
     * Updates a permission ticket.
     *
     * @param ticket the permission ticket
     */
    public void update(PermissionTicketRepresentation ticket) {
        if (ticket == null) {
            throw new IllegalArgumentException("Permission ticket must not be null or empty");
        }
        if (ticket.getId() == null) {
            throw new IllegalArgumentException("Permission ticket must have an id");
        }
        try {
            http.<List>put("/authz/protection/" + configuration.getResource() + "/permission")
                    .json(JsonSerialization.writeValueAsBytes(ticket))
                    .authorizationBearer(this.pat.get())
                    .response().json(List.class).execute();
        } catch (Exception cause) {
            throw handleAndWrapException("Error updating permission ticket", cause);
        }
    }
}
