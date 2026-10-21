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
package org.keycloak.authorization.protection.resource;

import java.util.function.BiFunction;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.admin.ResourceSetService;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.events.admin.OperationType;
import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.services.ErrorResponseException;

import org.jboss.resteasy.reactive.NoCache;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceService {

    private final ResourceServer resourceServer;
    private final ResourceSetService resourceManager;
    private final AuthorizationProvider authorization;
    private final KeycloakIdentity identity;

    public ResourceService(AuthorizationProvider authorization, ResourceServer resourceServer, KeycloakIdentity identity, ResourceSetService resourceManager) {
        this.authorization = authorization;
        this.identity = identity;
        this.resourceServer = resourceServer;
        this.resourceManager = resourceManager;
        checkResourceServerSettings();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response create(UmaResourceRepresentation resource) {
        if (resource == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        ResourceOwnerRepresentation owner = new ResourceOwnerRepresentation();
        owner.setId(identity.getId());
        resource.setOwner(owner);

        ResourceRepresentation newResource = resourceManager.create(resource);

        resourceManager.audit(resource, resource.getId(), OperationType.CREATE);

        return Response.status(Status.CREATED).entity(new UmaResourceRepresentation(newResource)).build();
    }

    @Path("{id}")
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response update(@PathParam("id") String id, ResourceRepresentation resource) {
        checkOwner(id);
        return this.resourceManager.update(id, resource);
    }

    @Path("/{id}")
    @DELETE
    public Response delete(@PathParam("id") String id) {
        checkOwner(id);
        return this.resourceManager.delete(id);
    }

    @Path("/{id}")
    @GET
    @Produces("application/json")
    public Response findById(@PathParam("id") String id) {
        checkOwner(id);
        return this.resourceManager.findById(id, UmaResourceRepresentation::new);
    }

    @GET
    @NoCache
    @Produces("application/json")
    public Response find(@QueryParam("_id") String id,
                         @QueryParam("name") String name,
                         @QueryParam("uri") String uri,
                         @QueryParam("owner") String owner,
                         @QueryParam("type") String type,
                         @QueryParam("scope") String scope,
                         @QueryParam("matchingUri") Boolean matchingUri,
                         @QueryParam("exactName") Boolean exactName,
                         @QueryParam("deep") Boolean deep,
                         @QueryParam("first") Integer firstResult,
                         @QueryParam("max") Integer maxResult) {
        if (!identity.isResourceServer()) {
            owner = identity.getId();
        }

        if(deep != null && deep) {
            return resourceManager.find(id, name, uri, owner, type, scope, matchingUri, exactName, deep, firstResult, maxResult);
        } else {
            return resourceManager.find(id, name, uri, owner, type, scope, matchingUri, exactName, deep, firstResult, maxResult, (BiFunction<Resource, Boolean, String>) (resource, deep1) -> resource.getId());
        }
    }

    private void checkResourceServerSettings() {
        if (!this.resourceServer.isAllowRemoteResourceManagement()) {
            throw new ErrorResponseException("not_supported", "Remote management is disabled.", Status.BAD_REQUEST);
        }
    }

    private void checkOwner(String resourceId) {
        if (identity.isResourceServer()) {
            return;
        }

        Resource resource = authorization.getStoreFactory().getResourceStore().findById(resourceServer, resourceId);

        if (resource == null) {
            throw new ErrorResponseException("not_found", "Resource not found", Status.NOT_FOUND);
        }

        if (!resource.getOwner().equals(identity.getId())) {
            throw new ErrorResponseException("forbidden", "You don't have permission to manage this resource", Status.FORBIDDEN);
        }
    }
}
