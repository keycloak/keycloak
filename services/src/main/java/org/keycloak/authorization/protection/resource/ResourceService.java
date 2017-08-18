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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.admin.ResourceSetService;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.protection.resource.representation.UmaResourceRepresentation;
import org.keycloak.authorization.protection.resource.representation.UmaScopeRepresentation;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.ErrorResponseException;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceService {

    private final ResourceServer resourceServer;
    private final ResourceSetService resourceManager;
    private final Identity identity;
    private final AuthorizationProvider authorization;

    public ResourceService(ResourceServer resourceServer, Identity identity, ResourceSetService resourceManager, AuthorizationProvider authorization) {
        this.identity = identity;
        this.resourceServer = resourceServer;
        this.resourceManager = resourceManager;
        this.authorization = authorization;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response create(@Context  UriInfo uriInfo, UmaResourceRepresentation umaResource) {
        checkResourceServerSettings();
        ResourceRepresentation resource = toResourceRepresentation(umaResource);
        Response response = this.resourceManager.create(uriInfo, resource);

        if (response.getEntity() instanceof ResourceRepresentation) {
            return Response.status(Status.CREATED).entity(toUmaRepresentation((ResourceRepresentation) response.getEntity())).build();
        }

        return response;
    }

    @Path("{id}")
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response update(@Context UriInfo uriInfo, @PathParam("id") String id, UmaResourceRepresentation representation) {
        ResourceRepresentation resource = toResourceRepresentation(representation);
        Response response = this.resourceManager.update(uriInfo, id, resource);

        if (response.getEntity() instanceof ResourceRepresentation) {
            return Response.noContent().build();
        }

        return response;
    }

    @Path("/{id}")
    @DELETE
    public Response delete(@Context UriInfo uriInfo, @PathParam("id") String id) {
        checkResourceServerSettings();
        return this.resourceManager.delete(uriInfo, id);
    }

    @Path("/{id}")
    @GET
    @Produces("application/json")
    public RegistrationResponse findById(@PathParam("id") String id) {
        Response response = this.resourceManager.findById(id);
        UmaResourceRepresentation resource = toUmaRepresentation((ResourceRepresentation) response.getEntity());

        if (resource == null) {
            throw new ErrorResponseException("not_found", "Resource with id [" + id + "] not found.", Status.NOT_FOUND);
        }

        return new RegistrationResponse(resource);
    }

    @GET
    @Produces("application/json")
    public Set<String> find(@QueryParam("filter") String filter) {
        if (filter == null) {
            return findAll();
        } else {
            return findByFilter(filter);
        }
    }

    private Set<String> findAll() {
        Response response = this.resourceManager.find(null, null, null, null, null, null, true, -1, -1);
        List<ResourceRepresentation> resources = (List<ResourceRepresentation>) response.getEntity();
        return resources.stream().map(ResourceRepresentation::getId).collect(Collectors.toSet());
    }

    private Set<String> findByFilter(String filter) {
        Set<ResourceRepresentation> resources = new HashSet<>();
        StoreFactory storeFactory = authorization.getStoreFactory();

        if (filter != null) {
            for (String currentFilter : filter.split("&")) {
                String[] parts = currentFilter.split("=");
                String filterType = parts[0];
                final String filterValue;

                if (parts.length > 1) {
                    filterValue = parts[1];
                } else {
                    filterValue = null;
                }


                if ("name".equals(filterType)) {
                    Resource resource = storeFactory.getResourceStore().findByName(filterValue, this.resourceServer.getId());

                    if (resource != null) {
                        resources.add(ModelToRepresentation.toRepresentation(resource, resourceServer, authorization));
                    }
                } else if ("type".equals(filterType)) {
                    resources.addAll(storeFactory.getResourceStore().findByResourceServer(this.resourceServer.getId()).stream().filter(description -> filterValue == null || filterValue.equals(description.getType())).collect(Collectors.toSet()).stream()
                            .map(resource -> ModelToRepresentation.toRepresentation(resource, this.resourceServer, authorization))
                            .collect(Collectors.toList()));
                } else if ("uri".equals(filterType)) {
                    resources.addAll(storeFactory.getResourceStore().findByUri(filterValue, this.resourceServer.getId()).stream()
                            .map(resource -> ModelToRepresentation.toRepresentation(resource, this.resourceServer, authorization))
                            .collect(Collectors.toList()));
                } else if ("owner".equals(filterType)) {
                    resources.addAll(storeFactory.getResourceStore().findByOwner(filterValue, this.resourceServer.getId()).stream()
                            .map(resource -> ModelToRepresentation.toRepresentation(resource, this.resourceServer, authorization))
                            .collect(Collectors.toList()));
                }
            }
        } else {
            resources = storeFactory.getResourceStore().findByOwner(identity.getId(), resourceServer.getId()).stream()
                    .map(resource -> ModelToRepresentation.toRepresentation(resource, this.resourceServer, authorization))
                    .collect(Collectors.toSet());
        }

        return resources.stream()
                .map(ResourceRepresentation::getId)
                .collect(Collectors.toSet());
    }

    private ResourceRepresentation toResourceRepresentation(UmaResourceRepresentation umaResource) {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setId(umaResource.getId());
        resource.setIconUri(umaResource.getIconUri());
        resource.setName(umaResource.getName());
        resource.setUri(umaResource.getUri());
        resource.setType(umaResource.getType());

        ResourceOwnerRepresentation owner = new ResourceOwnerRepresentation();
        String ownerId = umaResource.getOwner();

        if (ownerId == null) {
            ownerId = this.identity.getId();
        }

        owner.setId(ownerId);
        resource.setOwner(owner);

        resource.setScopes(umaResource.getScopes().stream().map(representation -> {
            ScopeRepresentation scopeRepresentation = new ScopeRepresentation();

            scopeRepresentation.setId(representation.getId());
            scopeRepresentation.setName(representation.getName());
            scopeRepresentation.setIconUri(representation.getIconUri());

            return scopeRepresentation;
        }).collect(Collectors.toSet()));

        return resource;
    }

    private UmaResourceRepresentation toUmaRepresentation(ResourceRepresentation representation) {
        if (representation == null) {
            return null;
        }

        UmaResourceRepresentation resource = new UmaResourceRepresentation();

        resource.setId(representation.getId());
        resource.setIconUri(representation.getIconUri());
        resource.setName(representation.getName());
        resource.setUri(representation.getUri());
        resource.setType(representation.getType());

        if (representation.getOwner() != null) {
            resource.setOwner(representation.getOwner().getId());
        }

        resource.setScopes(representation.getScopes().stream().map(scopeRepresentation -> {
            UmaScopeRepresentation umaScopeRep = new UmaScopeRepresentation();
            umaScopeRep.setId(scopeRepresentation.getId());
            umaScopeRep.setName(scopeRepresentation.getName());
            umaScopeRep.setIconUri(scopeRepresentation.getIconUri());
            return umaScopeRep;
        }).collect(Collectors.toSet()));

        return resource;
    }

    private void checkResourceServerSettings() {
        if (!this.resourceServer.isAllowRemoteResourceManagement()) {
            throw new ErrorResponseException("not_supported", "Remote management is disabled.", Status.BAD_REQUEST);
        }
    }
}