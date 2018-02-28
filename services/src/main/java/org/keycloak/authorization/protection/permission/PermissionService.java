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
package org.keycloak.authorization.protection.permission;

import org.keycloak.OAuthErrorException;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;
import org.keycloak.services.ErrorResponseException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PermissionService extends AbstractPermissionService {

    private final AuthorizationProvider authorization;
    private final ResourceServer resourceServer;

    public PermissionService(KeycloakIdentity identity, ResourceServer resourceServer, AuthorizationProvider authorization) {
        super(identity, resourceServer, authorization);
        this.resourceServer = resourceServer;
        this.authorization = authorization;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response create(List<PermissionRequest> request) {
        return super.create(request);
    }

    @PUT
    @Consumes("application/json")
    public Response update(PermissionTicketRepresentation representation) {
        if (representation == null || representation.getId() == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "invalid_ticket", Response.Status.BAD_REQUEST);
        }

        PermissionTicketStore ticketStore = authorization.getStoreFactory().getPermissionTicketStore();
        PermissionTicket ticket = ticketStore.findById(representation.getId(), resourceServer.getId());

        if (ticket == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "invalid_ticket", Response.Status.BAD_REQUEST);
        }

        RepresentationToModel.toModel(representation, resourceServer.getId(), authorization);

        return Response.noContent().build();
    }

    @DELETE
    @Consumes("application/json")
    public Response delete(String id) {
        if (id == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "invalid_ticket", Response.Status.BAD_REQUEST);
        }

        PermissionTicketStore ticketStore = authorization.getStoreFactory().getPermissionTicketStore();
        PermissionTicket ticket = ticketStore.findById(id, resourceServer.getId());

        if (ticket == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "invalid_ticket", Response.Status.BAD_REQUEST);
        }

        ticketStore.delete(id);

        return Response.noContent().build();
    }

    @GET
    @Produces("application/json")
    public Response find(@QueryParam("scopeId") String scopeId,
                         @QueryParam("resourceId") String resourceId,
                         @QueryParam("owner") String owner,
                         @QueryParam("requester") String requester,
                         @QueryParam("granted") Boolean granted,
                         @QueryParam("returnNames") Boolean returnNames,
                         @QueryParam("first") Integer firstResult,
                         @QueryParam("max") Integer maxResult) {
        PermissionTicketStore permissionTicketStore = authorization.getStoreFactory().getPermissionTicketStore();

        Map<String, String> filters = new HashMap<>();

        if (resourceId != null) {
            filters.put(PermissionTicket.RESOURCE, resourceId);
        }

        if (scopeId != null) {
            filters.put(PermissionTicket.SCOPE, scopeId);
        }

        if (owner != null) {
            filters.put(PermissionTicket.OWNER, owner);
        }

        if (requester != null) {
            filters.put(PermissionTicket.REQUESTER, requester);
        }

        if (granted != null) {
            filters.put(PermissionTicket.GRANTED, granted.toString());
        }

        return Response.ok().entity(permissionTicketStore.find(filters, resourceServer.getId(), firstResult != null ? firstResult : -1, maxResult != null ? maxResult : Constants.DEFAULT_MAX_RESULTS)
                    .stream()
                        .map(permissionTicket -> ModelToRepresentation.toRepresentation(permissionTicket, returnNames == null ? false : returnNames))
                        .collect(Collectors.toList()))
                .build();
    }
}