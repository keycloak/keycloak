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
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;
import static org.keycloak.models.utils.RepresentationToModel.toModel;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceSetService {

    private final AuthorizationProvider authorization;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private ResourceServer resourceServer;

    public ResourceSetService(ResourceServer resourceServer, AuthorizationProvider authorization, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.resourceServer = resourceServer;
        this.authorization = authorization;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.AUTHORIZATION_RESOURCE);
    }

    @POST
    @NoCache
    @Consumes("application/json")
    @Produces("application/json")
    public Response create(@Context UriInfo uriInfo, ResourceRepresentation resource) {
        Response response = create(resource);
        audit(uriInfo, resource, resource.getId(), OperationType.CREATE);
        return response;
    }

    public Response create(ResourceRepresentation resource) {
        requireManage();
        StoreFactory storeFactory = this.authorization.getStoreFactory();
        Resource existingResource = storeFactory.getResourceStore().findByName(resource.getName(), this.resourceServer.getId());
        ResourceOwnerRepresentation owner = resource.getOwner();

        if (owner == null) {
            owner = new ResourceOwnerRepresentation();
            owner.setId(resourceServer.getId());
        }

        String ownerId = owner.getId();

        if (ownerId == null) {
            return ErrorResponse.error("You must specify the resource owner.", Status.BAD_REQUEST);
        }

        if (existingResource != null && existingResource.getOwner().equals(ownerId)) {
            return ErrorResponse.exists("Resource with name [" + resource.getName() + "] already exists.");
        }

        ResourceRepresentation representation = new ResourceRepresentation();

        representation.setId(toModel(resource, this.resourceServer, authorization).getId());

        return Response.status(Status.CREATED).entity(representation).build();
    }

    @Path("{id}")
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response update(@Context UriInfo uriInfo, @PathParam("id") String id, ResourceRepresentation resource) {
        requireManage();
        resource.setId(id);
        StoreFactory storeFactory = this.authorization.getStoreFactory();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        Resource model = resourceStore.findById(resource.getId(), resourceServer.getId());

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        toModel(resource, resourceServer, authorization);

        audit(uriInfo, resource, OperationType.UPDATE);

        return Response.noContent().build();
    }

    @Path("{id}")
    @DELETE
    public Response delete(@Context UriInfo uriInfo, @PathParam("id") String id) {
        requireManage();
        StoreFactory storeFactory = authorization.getStoreFactory();
        Resource resource = storeFactory.getResourceStore().findById(id, resourceServer.getId());

        if (resource == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        PolicyStore policyStore = storeFactory.getPolicyStore();
        List<Policy> policies = policyStore.findByResource(id, resourceServer.getId());

        for (Policy policyModel : policies) {
            if (policyModel.getResources().size() == 1) {
                policyStore.delete(policyModel.getId());
            } else {
                policyModel.removeResource(resource);
            }
        }

        storeFactory.getResourceStore().delete(id);

        if (authorization.getRealm().isAdminEventsEnabled()) {
            audit(uriInfo, toRepresentation(resource, resourceServer, authorization), OperationType.DELETE);
        }

        return Response.noContent().build();
    }

    @Path("{id}")
    @GET
    @NoCache
    @Produces("application/json")
    public Response findById(@PathParam("id") String id) {
        requireView();
        StoreFactory storeFactory = authorization.getStoreFactory();
        Resource model = storeFactory.getResourceStore().findById(id, resourceServer.getId());

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(toRepresentation(model, this.resourceServer, authorization, true)).build();
    }

    @Path("{id}/scopes")
    @GET
    @NoCache
    @Produces("application/json")
    public Response getScopes(@PathParam("id") String id) {
        requireView();
        StoreFactory storeFactory = authorization.getStoreFactory();
        Resource model = storeFactory.getResourceStore().findById(id, resourceServer.getId());

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        List<ScopeRepresentation> scopes = model.getScopes().stream().map(scope -> {
            ScopeRepresentation representation = new ScopeRepresentation();

            representation.setId(scope.getId());
            representation.setName(scope.getName());

            return representation;
        }).collect(Collectors.toList());

        if (model.getType() != null) {
            ResourceStore resourceStore = authorization.getStoreFactory().getResourceStore();
            for (Resource typed : resourceStore.findByType(model.getType(), resourceServer.getId())) {
                if (typed.getOwner().equals(resourceServer.getId()) && !typed.getId().equals(model.getId())) {
                    scopes.addAll(typed.getScopes().stream().map(model1 -> {
                        ScopeRepresentation scope = new ScopeRepresentation();
                        scope.setId(model1.getId());
                        scope.setName(model1.getName());
                        String iconUri = model1.getIconUri();
                        if (iconUri != null) {
                            scope.setIconUri(iconUri);
                        }
                        return scope;
                    }).filter(scopeRepresentation -> !scopes.contains(scopeRepresentation)).collect(Collectors.toList()));
                }
            }
        }

        return Response.ok(scopes).build();
    }

    @Path("{id}/permissions")
    @GET
    @NoCache
    @Produces("application/json")
    public Response getPermissions(@PathParam("id") String id) {
        requireView();
        StoreFactory storeFactory = authorization.getStoreFactory();
        Resource model = storeFactory.getResourceStore().findById(id, resourceServer.getId());

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        PolicyStore policyStore = authorization.getStoreFactory().getPolicyStore();
        Set<Policy> policies = new HashSet<>();

        policies.addAll(policyStore.findByResource(model.getId(), resourceServer.getId()));
        policies.addAll(policyStore.findByResourceType(model.getType(), resourceServer.getId()));
        policies.addAll(policyStore.findByScopeIds(model.getScopes().stream().map(scope -> scope.getId()).collect(Collectors.toList()), resourceServer.getId()));

        List<PolicyRepresentation> representation = new ArrayList<>();

        for (Policy policyModel : policies) {
            PolicyRepresentation policy = new PolicyRepresentation();

            policy.setId(policyModel.getId());
            policy.setName(policyModel.getName());
            policy.setType(policyModel.getType());

            if (!representation.contains(policy)) {
                representation.add(policy);
            }
        }

        return Response.ok(representation).build();
    }

    @Path("/search")
    @GET
    @NoCache
    @Produces("application/json")
    public Response find(@QueryParam("name") String name) {
        this.auth.realm().requireViewAuthorization();
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
    public Response find(@QueryParam("_id") String id,
                            @QueryParam("name") String name,
                            @QueryParam("uri") String uri,
                            @QueryParam("owner") String owner,
                            @QueryParam("type") String type,
                            @QueryParam("scope") String scope,
                            @QueryParam("deep") Boolean deep,
                            @QueryParam("first") Integer firstResult,
                            @QueryParam("max") Integer maxResult) {
        requireView();

        StoreFactory storeFactory = authorization.getStoreFactory();

        if (deep == null) {
            deep = true;
        }

        Map<String, String[]> search = new HashMap<>();

        if (id != null && !"".equals(id.trim())) {
            search.put("id", new String[] {id});
        }

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

        Boolean finalDeep = deep;
        return Response.ok(
                storeFactory.getResourceStore().findByResourceServer(search, this.resourceServer.getId(), firstResult != null ? firstResult : -1, maxResult != null ? maxResult : Constants.DEFAULT_MAX_RESULTS).stream()
                        .map(resource -> toRepresentation(resource, resourceServer, authorization, finalDeep))
                        .collect(Collectors.toList()))
                .build();
    }

    private void requireView() {
        if (this.auth != null) {
            this.auth.realm().requireViewAuthorization();
        }
    }

    private void requireManage() {
        if (this.auth != null) {
            this.auth.realm().requireManageAuthorization();
        }
    }

    private void audit(@Context UriInfo uriInfo, ResourceRepresentation resource, OperationType operation) {
        audit(uriInfo, resource, null, operation);
    }

    private void audit(@Context UriInfo uriInfo, ResourceRepresentation resource, String id, OperationType operation) {
        if (authorization.getRealm().isAdminEventsEnabled()) {
            if (id != null) {
                adminEvent.operation(operation).resourcePath(uriInfo, id).representation(resource).success();
            } else {
                adminEvent.operation(operation).resourcePath(uriInfo).representation(resource).success();
            }
        }
    }
}