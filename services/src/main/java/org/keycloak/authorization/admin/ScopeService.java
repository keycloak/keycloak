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
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;
import static org.keycloak.models.utils.RepresentationToModel.toModel;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ScopeService {

    private final AuthorizationProvider authorization;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private KeycloakSession session;
    private ResourceServer resourceServer;

    public ScopeService(KeycloakSession session, ResourceServer resourceServer, AuthorizationProvider authorization, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.resourceServer = resourceServer;
        this.authorization = authorization;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.AUTHORIZATION_SCOPE);
    }

    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(ScopeRepresentation scope) {
            this.auth.realm().requireManageAuthorization();
        Scope model = toModel(scope, this.resourceServer, authorization);

        scope.setId(model.getId());

        audit(scope, scope.getId(), OperationType.CREATE);

        return Response.status(Status.CREATED).entity(scope).build();
    }

    @Path("{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") String id, ScopeRepresentation scope) {
            this.auth.realm().requireManageAuthorization();
        scope.setId(id);
        StoreFactory storeFactory = authorization.getStoreFactory();
        Scope model = storeFactory.getScopeStore().findById(scope.getId(), resourceServer.getId());

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        toModel(scope, resourceServer, authorization);

        audit(scope, OperationType.UPDATE);

        return Response.noContent().build();
    }

    @Path("{id}")
    @DELETE
    public Response delete(@PathParam("id") String id) {
        this.auth.realm().requireManageAuthorization();
        StoreFactory storeFactory = authorization.getStoreFactory();
        List<Resource> resources = storeFactory.getResourceStore().findByScope(Arrays.asList(id), resourceServer.getId());

        if (!resources.isEmpty()) {
            return ErrorResponse.error("Scopes can not be removed while associated with resources.", Status.BAD_REQUEST);
        }

        Scope scope = storeFactory.getScopeStore().findById(id, resourceServer.getId());

        if (scope == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        PolicyStore policyStore = storeFactory.getPolicyStore();
        List<Policy> policies = policyStore.findByScopeIds(Arrays.asList(scope.getId()), resourceServer.getId());

        for (Policy policyModel : policies) {
            if (policyModel.getScopes().size() == 1) {
                policyStore.delete(policyModel.getId());
            } else {
                policyModel.removeScope(scope);
            }
        }

        storeFactory.getScopeStore().delete(id);

        audit(toRepresentation(scope), OperationType.DELETE);

        return Response.noContent().build();
    }

    @Path("{id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response findById(@PathParam("id") String id) {
        this.auth.realm().requireViewAuthorization();
        Scope model = this.authorization.getStoreFactory().getScopeStore().findById(id, resourceServer.getId());

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(toRepresentation(model)).build();
    }

    @Path("{id}/resources")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResources(@PathParam("id") String id) {
        this.auth.realm().requireViewAuthorization();
        StoreFactory storeFactory = this.authorization.getStoreFactory();
        Scope model = storeFactory.getScopeStore().findById(id, resourceServer.getId());

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(storeFactory.getResourceStore().findByScope(Arrays.asList(model.getId()), resourceServer.getId()).stream().map(resource -> {
            ResourceRepresentation representation = new ResourceRepresentation();

            representation.setId(resource.getId());
            representation.setName(resource.getName());

            return representation;
        }).collect(Collectors.toList())).build();
    }

    @Path("{id}/permissions")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPermissions(@PathParam("id") String id) {
        this.auth.realm().requireViewAuthorization();
        StoreFactory storeFactory = this.authorization.getStoreFactory();
        Scope model = storeFactory.getScopeStore().findById(id, resourceServer.getId());

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        PolicyStore policyStore = storeFactory.getPolicyStore();

        return Response.ok(policyStore.findByScopeIds(Arrays.asList(model.getId()), resourceServer.getId()).stream().map(policy -> {
            PolicyRepresentation representation = new PolicyRepresentation();

            representation.setId(policy.getId());
            representation.setName(policy.getName());
            representation.setType(policy.getType());

            return representation;
        }).collect(Collectors.toList())).build();
    }

    @Path("/search")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response find(@QueryParam("name") String name) {
        this.auth.realm().requireViewAuthorization();
        StoreFactory storeFactory = authorization.getStoreFactory();

        if (name == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        Scope model = storeFactory.getScopeStore().findByName(name, this.resourceServer.getId());

        if (model == null) {
            return Response.status(Status.OK).build();
        }

        return Response.ok(toRepresentation(model)).build();
    }

    @GET
    @NoCache
    @Produces("application/json")
    public Response findAll(@QueryParam("scopeId") String id,
                            @QueryParam("name") String name,
                            @QueryParam("first") Integer firstResult,
                            @QueryParam("max") Integer maxResult) {
        this.auth.realm().requireViewAuthorization();

        Map<String, String[]> search = new HashMap<>();

        if (id != null && !"".equals(id.trim())) {
            search.put("id", new String[] {id});
        }

        if (name != null && !"".equals(name.trim())) {
            search.put("name", new String[] {name});
        }

        return Response.ok(
                this.authorization.getStoreFactory().getScopeStore().findByResourceServer(search, this.resourceServer.getId(), firstResult != null ? firstResult : -1, maxResult != null ? maxResult : Constants.DEFAULT_MAX_RESULTS).stream()
                        .map(scope -> toRepresentation(scope))
                        .collect(Collectors.toList()))
                .build();
    }

    private void audit(ScopeRepresentation resource, OperationType operation) {
        audit(resource, null, operation);
    }

    private void audit(ScopeRepresentation resource, String id, OperationType operation) {
        if (id != null) {
            adminEvent.operation(operation).resourcePath(session.getContext().getUri(), id).representation(resource).success();
        } else {
            adminEvent.operation(operation).resourcePath(session.getContext().getUri()).representation(resource).success();
        }
    }
}
