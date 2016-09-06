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
package org.keycloak.authorization.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.RealmAuth;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;
import static org.keycloak.models.utils.RepresentationToModel.toModel;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceSetService {

    private final AuthorizationProvider authorization;
    private final RealmAuth auth;
    private ResourceServer resourceServer;

    public ResourceSetService(ResourceServer resourceServer, AuthorizationProvider authorization, RealmAuth auth) {
        this.resourceServer = resourceServer;
        this.authorization = authorization;
        this.auth = auth;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response create(ResourceRepresentation resource) {
        requireManage();
        StoreFactory storeFactory = this.authorization.getStoreFactory();
        Resource existingResource = storeFactory.getResourceStore().findByName(resource.getName(), this.resourceServer.getId());

        if (existingResource != null && existingResource.getResourceServer().getId().equals(this.resourceServer.getId())
                && existingResource.getOwner().equals(resource.getOwner())) {
            return ErrorResponse.exists("Resource with name [" + resource.getName() + "] already exists.");
        }

        Resource model = toModel(resource, this.resourceServer, authorization);

        ResourceRepresentation representation = new ResourceRepresentation();

        representation.setId(model.getId());

        return Response.status(Status.CREATED).entity(representation).build();
    }

    @Path("{id}")
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response update(@PathParam("id") String id, ResourceRepresentation resource) {
        requireManage();
        resource.setId(id);
        StoreFactory storeFactory = this.authorization.getStoreFactory();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        Resource model = resourceStore.findById(resource.getId());

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        toModel(resource, resourceServer, authorization);

        return Response.noContent().build();
    }

    @Path("{id}")
    @DELETE
    public Response delete(@PathParam("id") String id) {
        requireManage();
        StoreFactory storeFactory = authorization.getStoreFactory();
        Resource resource = storeFactory.getResourceStore().findById(id);

        if (resource == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        PolicyStore policyStore = storeFactory.getPolicyStore();
        List<Policy> policies = policyStore.findByResource(id);

        for (Policy policyModel : policies) {
            if (policyModel.getResources().size() == 1) {
                policyStore.delete(policyModel.getId());
            } else {
                policyModel.addResource(resource);
            }
        }

        storeFactory.getResourceStore().delete(id);

        return Response.noContent().build();
    }

    @Path("{id}")
    @GET
    @NoCache
    @Produces("application/json")
    public Response findById(@PathParam("id") String id) {
        requireView();
        StoreFactory storeFactory = authorization.getStoreFactory();
        Resource model = storeFactory.getResourceStore().findById(id);

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(toRepresentation(model, this.resourceServer, authorization)).build();
    }

    @Path("/search")
    @GET
    @Produces("application/json")
    @NoCache
    public Response find(@QueryParam("name") String name) {
        this.auth.requireView();
        StoreFactory storeFactory = authorization.getStoreFactory();

        if (name == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        Resource model = storeFactory.getResourceStore().findByName(name, this.resourceServer.getId());

        if (model == null) {
            return Response.status(Status.OK).build();
        }

        return Response.ok(toRepresentation(model, this.resourceServer, authorization)).build();
    }

    @GET
    @NoCache
    @Produces("application/json")
    public Response findAll(@QueryParam("name") String name,
                            @QueryParam("uri") String uri,
                            @QueryParam("owner") String owner,
                            @QueryParam("type") String type,
                            @QueryParam("scope") String scope,
                            @QueryParam("first") Integer firstResult,
                            @QueryParam("max") Integer maxResult) {
        requireView();
        StoreFactory storeFactory = authorization.getStoreFactory();

        Map<String, String[]> search = new HashMap<>();

        if (name != null && !"".equals(name.trim())) {
            search.put("name", new String[] {name});
        }

        if (uri != null && !"".equals(uri.trim())) {
            search.put("uri", new String[] {uri});
        }

        if (owner != null && !"".equals(owner.trim())) {
            RealmModel realm = authorization.getKeycloakSession().getContext().getRealm();
            ClientModel clientModel = realm.getClientByClientId(owner);

            if (clientModel != null) {
                owner = clientModel.getId();
            } else {
                UserModel user = authorization.getKeycloakSession().users().getUserByUsername(owner, realm);

                if (user != null) {
                    owner = user.getId();
                }
            }

            search.put("owner", new String[] {owner});
        }

        if (type != null && !"".equals(type.trim())) {
            search.put("type", new String[] {type});
        }

        if (scope != null && !"".equals(scope.trim())) {
            HashMap<String, String[]> scopeFilter = new HashMap<>();

            scopeFilter.put("name", new String[] {scope});

            List<Scope> scopes = authorization.getStoreFactory().getScopeStore().findByResourceServer(scopeFilter, resourceServer.getId(), -1, -1);

            if (scopes.isEmpty()) {
                return Response.ok(Collections.emptyList()).build();
            }

            search.put("scope", scopes.stream().map(Scope::getId).toArray(String[]::new));
        }

        return Response.ok(
                storeFactory.getResourceStore().findByResourceServer(search, this.resourceServer.getId(), firstResult != null ? firstResult : -1, maxResult != null ? maxResult : -1).stream()
                        .map(resource -> toRepresentation(resource, this.resourceServer, authorization))
                        .collect(Collectors.toList()))
                .build();
    }

    private void requireView() {
        if (this.auth != null) {
            this.auth.requireView();
        }
    }

    private void requireManage() {
        if (this.auth != null) {
            this.auth.requireManage();
        }
    }
}