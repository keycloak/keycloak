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
package org.keycloak.authorization.admin;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
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
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.resteasy.reactive.NoCache;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;
import static org.keycloak.models.utils.RepresentationToModel.toModel;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class ScopeService {

    private final AuthorizationProvider authorization;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final KeycloakSession session;
    private final ResourceServer resourceServer;

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
        AdminPermissionsSchema.SCHEMA.throwExceptionIfAdminPermissionClient(session, resourceServer.getId());
        this.auth.realm().requireManageAuthorization(resourceServer);
        Scope model = toModel(scope, this.resourceServer, authorization);

        scope.setId(model.getId());

        audit(scope, scope.getId(), OperationType.CREATE);

        return Response.status(Status.CREATED).entity(scope).build();
    }

    @Path("{scope-id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("scope-id") String id, ScopeRepresentation scope) {
        AdminPermissionsSchema.SCHEMA.throwExceptionIfAdminPermissionClient(session, resourceServer.getId());
        this.auth.realm().requireManageAuthorization(resourceServer);
        scope.setId(id);
        StoreFactory storeFactory = authorization.getStoreFactory();
        Scope model = storeFactory.getScopeStore().findById(resourceServer, scope.getId());

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        toModel(scope, resourceServer, authorization);

        audit(scope, OperationType.UPDATE);

        return Response.noContent().build();
    }

    @Path("{scope-id}")
    @DELETE
    public Response delete(@PathParam("scope-id") String id) {
        AdminPermissionsSchema.SCHEMA.throwExceptionIfAdminPermissionClient(session, resourceServer.getId());
        this.auth.realm().requireManageAuthorization(resourceServer);
        StoreFactory storeFactory = authorization.getStoreFactory();
        Scope scope = storeFactory.getScopeStore().findById(resourceServer, id);
        if (scope == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        List<Resource> resources = storeFactory.getResourceStore().findByScopes(resourceServer, Collections.singleton(scope));
        if (!resources.isEmpty()) {
            throw ErrorResponse.error("Scopes can not be removed while associated with resources.", Status.BAD_REQUEST);
        }


        PolicyStore policyStore = storeFactory.getPolicyStore();
        List<Policy> policies = policyStore.findByScopes(resourceServer, Collections.singletonList(scope));

        for (Policy policyModel : policies) {
            if (policyModel.getScopes().size() == 1) {
                policyStore.delete(policyModel.getId());
            } else {
                policyModel.removeScope(scope);
            }
        }

        //to be able to access all lazy loaded fields it's needed to create representation before it's deleted
        ScopeRepresentation scopeRep = toRepresentation(scope);

        storeFactory.getScopeStore().delete(id);

        audit(scopeRep, OperationType.DELETE);

        return Response.noContent().build();
    }

    @Path("{scope-id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = ScopeRepresentation.class))
        ),
        @APIResponse(responseCode = "404", description = "Not found")
    })
    public Response findById(@PathParam("scope-id") String id) {
        this.auth.realm().requireViewAuthorization(resourceServer);
        Scope model = this.authorization.getStoreFactory().getScopeStore().findById(resourceServer, id);

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(toRepresentation(model)).build();
    }

    @Path("{scope-id}/resources")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = ResourceRepresentation.class, type = SchemaType.ARRAY))
        ),
        @APIResponse(responseCode = "404", description = "Not found")
    })
    public Response getResources(@PathParam("scope-id") String id) {
        this.auth.realm().requireViewAuthorization(resourceServer);
        StoreFactory storeFactory = this.authorization.getStoreFactory();
        Scope model = storeFactory.getScopeStore().findById(resourceServer, id);

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(storeFactory.getResourceStore().findByScopes(resourceServer, Collections.singleton(model)).stream().map(resource -> {
            ResourceRepresentation representation = new ResourceRepresentation();

            representation.setId(resource.getId());
            representation.setName(resource.getName());

            return representation;
        }).collect(Collectors.toList())).build();
    }

    @Path("{scope-id}/permissions")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = PolicyRepresentation.class, type = SchemaType.ARRAY))
        ),
        @APIResponse(responseCode = "404", description = "Not found")
    })
    public Response getPermissions(@PathParam("scope-id") String id) {
        this.auth.realm().requireViewAuthorization(resourceServer);
        StoreFactory storeFactory = this.authorization.getStoreFactory();
        Scope model = storeFactory.getScopeStore().findById(resourceServer, id);

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        PolicyStore policyStore = storeFactory.getPolicyStore();

        return Response.ok(policyStore.findByScopes(resourceServer, Collections.singletonList(model)).stream().map(policy -> {
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
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = ScopeRepresentation.class, type = SchemaType.ARRAY))
        ),
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request")
    })
    public Response find(@QueryParam("name") String name) {
        this.auth.realm().requireViewAuthorization(resourceServer);
        StoreFactory storeFactory = authorization.getStoreFactory();

        if (name == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        Scope model = storeFactory.getScopeStore().findByName(this.resourceServer, name);

        if (model == null) {
            return Response.status(Status.NO_CONTENT).build();
        }

        return Response.ok(toRepresentation(model)).build();
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<ScopeRepresentation> findAll(@QueryParam("scopeId") String id,
                            @QueryParam("name") String name,
                            @QueryParam("first") Integer firstResult,
                            @QueryParam("max") Integer maxResult) {
        this.auth.realm().requireViewAuthorization(resourceServer);

        Map<Scope.FilterOption, String[]> search = new EnumMap<>(Scope.FilterOption.class);

        if (id != null && !"".equals(id.trim())) {
            search.put(Scope.FilterOption.ID, new String[] {id});
        }

        if (name != null && !"".equals(name.trim())) {
            search.put(Scope.FilterOption.NAME, new String[] {name});
        }

        return this.authorization.getStoreFactory().getScopeStore().findByResourceServer(this.resourceServer, search, firstResult != null ? firstResult : -1, maxResult != null ? maxResult : Constants.DEFAULT_MAX_RESULTS).stream()
            .map(scope -> toRepresentation(scope));
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
