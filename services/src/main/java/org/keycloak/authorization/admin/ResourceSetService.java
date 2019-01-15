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
import org.keycloak.OAuthErrorException;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.common.util.PathMatcher;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;
import static org.keycloak.models.utils.ModelToRepresentation.toResourceHierarchy;
import static org.keycloak.models.utils.RepresentationToModel.toModel;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceSetService {

    private final AuthorizationProvider authorization;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private KeycloakSession session;
    private ResourceServer resourceServer;

    public ResourceSetService(KeycloakSession session, ResourceServer resourceServer, AuthorizationProvider authorization, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.resourceServer = resourceServer;
        this.authorization = authorization;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.AUTHORIZATION_RESOURCE);
    }

    @POST
    @NoCache
    @Consumes("application/json")
    @Produces("application/json")
    public Response createPost(ResourceRepresentation resource) {
        if (resource == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        Resource newResource = create(resource);

        audit(resource, resource.getId(), OperationType.CREATE);

        return Response.status(Status.CREATED).entity(toRepresentation(newResource, resourceServer, authorization)).build();
    }

    public Resource create(ResourceRepresentation resource) {
        requireManage();
        StoreFactory storeFactory = this.authorization.getStoreFactory();
        ResourceOwnerRepresentation owner = resource.getOwner();

        if (owner == null) {
            owner = new ResourceOwnerRepresentation();
            owner.setId(resourceServer.getId());
            resource.setOwner(owner);
        }

        String ownerId = owner.getId();

        if (ownerId == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "You must specify the resource owner.", Status.BAD_REQUEST);
        }

        Resource existingResource = storeFactory.getResourceStore().findByName(resource.getName(), ownerId, this.resourceServer.getId());

        if (existingResource != null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Resource with name [" + resource.getName() + "] already exists.", Status.CONFLICT);
        }

        return toModel(resource, this.resourceServer, authorization);
    }


    public Resource create(ResourceRepresentation resource, Resource parent) {
        requireManage();
        StoreFactory storeFactory = this.authorization.getStoreFactory();
        ResourceOwnerRepresentation owner = resource.getOwner();

        if (owner == null) {
            owner = new ResourceOwnerRepresentation();
            owner.setId(resourceServer.getId());
            resource.setOwner(owner);
        }

        String ownerId = owner.getId();

        if (ownerId == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "You must specify the resource owner.", Status.BAD_REQUEST);
        }

        Resource existingResource = storeFactory.getResourceStore().findByName(resource.getName(), ownerId, this.resourceServer.getId());

        if (existingResource != null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Resource with name [" + resource.getName() + "] already exists.", Status.CONFLICT);
        }

        return toModel(resource, parent, this.resourceServer, authorization);
    }


    public ResourceRepresentation createToRepresentation(ResourceRepresentation resource) {
        return toRepresentation(create(resource), resourceServer, authorization);
    }


    @POST
    @Path("{id}/children")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createChild(@PathParam("id") String id, ResourceRepresentation resource) {
        requireManage();
        StoreFactory storeFactory = this.authorization.getStoreFactory();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        Resource parent = resourceStore.findById(id, resourceServer.getId());
        for (Resource subResource : parent.getSubResources()) {
            if (subResource.getName().equals(resource.getName())) {
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Resource with name [" + resource.getName() + "] already exists.", Status.CONFLICT);
            }
        }
        Resource child = create(resource, parent);
        audit(resource, resource.getId(), OperationType.CREATE);
        return Response.status(Status.CREATED).entity(toRepresentation(child, resourceServer, authorization)).build();
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
        Resource model = resourceStore.findById(resource.getId(), resourceServer.getId());

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        toModel(resource, resourceServer, authorization);

        audit(resource, OperationType.UPDATE);

        return Response.noContent().build();
    }

    @Path("{id}")
    @DELETE
    public Response delete(@PathParam("id") String id) {
        requireManage();
        StoreFactory storeFactory = authorization.getStoreFactory();
        Resource resource = storeFactory.getResourceStore().findById(id, resourceServer.getId());

        if (resource == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        storeFactory.getResourceStore().delete(id);

        if (authorization.getRealm().isAdminEventsEnabled()) {
            audit(toRepresentation(resource, resourceServer, authorization), OperationType.DELETE);
        }

        return Response.noContent().build();
    }

    @Path("{id}")
    @GET
    @NoCache
    @Produces("application/json")
    public Response findById(@PathParam("id") String id) {
        return findById(id, resource -> toRepresentation(resource, resourceServer, authorization, true));
    }

    public Response findById(String id, Function<Resource, ? extends ResourceRepresentation> toRepresentation) {
        requireView();
        StoreFactory storeFactory = authorization.getStoreFactory();
        Resource model = storeFactory.getResourceStore().findById(id, resourceServer.getId());

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(toRepresentation.apply(model)).build();
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

        if (model.getType() != null && !model.getOwner().equals(resourceServer.getId())) {
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
        ResourceStore resourceStore = storeFactory.getResourceStore();
        Resource model = resourceStore.findById(id, resourceServer.getId());

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        PolicyStore policyStore = authorization.getStoreFactory().getPolicyStore();
        Set<Policy> policies = new HashSet<>();

        policies.addAll(policyStore.findByResource(model.getId(), resourceServer.getId()));

        if (model.getType() != null) {
            policies.addAll(policyStore.findByResourceType(model.getType(), resourceServer.getId()));

            HashMap<String, String[]> resourceFilter = new HashMap<>();

            resourceFilter.put("owner", new String[]{resourceServer.getId()});
            resourceFilter.put("type", new String[]{model.getType()});

            for (Resource resourceType : resourceStore.findByResourceServer(resourceFilter, resourceServer.getId(), -1, -1)) {
                policies.addAll(policyStore.findByResource(resourceType.getId(), resourceServer.getId()));
            }
        }

        policies.addAll(policyStore.findByScopeIds(model.getScopes().stream().map(scope -> scope.getId()).collect(Collectors.toList()), id, resourceServer.getId()));
        policies.addAll(policyStore.findByScopeIds(model.getScopes().stream().map(scope -> scope.getId()).collect(Collectors.toList()), null, resourceServer.getId()));

        List<PolicyRepresentation> representation = new ArrayList<>();

        for (Policy policyModel : policies) {
            if (!"uma".equalsIgnoreCase(policyModel.getType())) {
                PolicyRepresentation policy = new PolicyRepresentation();

                policy.setId(policyModel.getId());
                policy.setName(policyModel.getName());
                policy.setType(policyModel.getType());

                if (!representation.contains(policy)) {
                    representation.add(policy);
                }
            }
        }

        return Response.ok(representation).build();
    }

    @Path("{id}/attributes")
    @GET
    @NoCache
    @Produces("application/json")
    public Response getAttributes(@PathParam("id") String id) {
        requireView();
        StoreFactory storeFactory = authorization.getStoreFactory();
        Resource model = storeFactory.getResourceStore().findById(id, resourceServer.getId());

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(model.getAttributes()).build();
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
                         @QueryParam("matchingUri") Boolean matchingUri,
                         @QueryParam("deep") Boolean deep,
                         @QueryParam("first") Integer firstResult,
                         @QueryParam("max") Integer maxResult) {
        if (deep == null) {
            deep = true;
        }
        if (id != null || name != null || uri != null || type != null) {
            return find(id, name, uri, owner, type, scope, matchingUri, deep, firstResult, maxResult, (BiFunction<Resource, Boolean, ResourceRepresentation>) (resource, deep1) -> toResourceHierarchy(resource, resourceServer, authorization, deep1));
        }
        return find(deep, firstResult, maxResult, (BiFunction<Resource, Boolean, ResourceRepresentation>) (resource, deep1) -> toResourceHierarchy(resource, resourceServer, authorization, deep1));
    }

    public Response find(Boolean deep,
                         Integer firstResult,
                         Integer maxResult,
                         BiFunction<Resource, Boolean, ?> toRepresentation) {

        StoreFactory storeFactory = authorization.getStoreFactory();
        List<Resource> resources = storeFactory.getResourceStore().findTopLevel(this.resourceServer.getId(), firstResult != null ? firstResult : -1, maxResult != null ? maxResult : Constants.DEFAULT_MAX_RESULTS);

        Boolean finalDeep = deep;

        return Response.ok(
                resources.stream()
                        .map(resource -> toRepresentation.apply(resource, finalDeep))
                        .collect(Collectors.toList()))
                .build();

    }

    public Response find(String id,
                         String name,
                         String uri,
                         String owner,
                         String type,
                         String scope,
                         Boolean matchingUri,
                         Boolean deep,
                         Integer firstResult,
                         Integer maxResult,
                         BiFunction<Resource, Boolean, ?> toRepresentation) {
        requireView();

        StoreFactory storeFactory = authorization.getStoreFactory();

        Map<String, String[]> search = new HashMap<>();

        if (id != null && !"".equals(id.trim())) {
            search.put("id", new String[]{id});
        }

        if (name != null && !"".equals(name.trim())) {
            search.put("name", new String[]{name});
        }

        if (uri != null && !"".equals(uri.trim())) {
            search.put("uri", new String[]{uri});
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

            search.put("owner", new String[]{owner});
        }

        if (type != null && !"".equals(type.trim())) {
            search.put("type", new String[]{type});
        }

        if (scope != null && !"".equals(scope.trim())) {
            HashMap<String, String[]> scopeFilter = new HashMap<>();

            scopeFilter.put("name", new String[]{scope});

            List<Scope> scopes = authorization.getStoreFactory().getScopeStore().findByResourceServer(scopeFilter, resourceServer.getId(), -1, -1);

            if (scopes.isEmpty()) {
                return Response.ok(Collections.emptyList()).build();
            }

            search.put("scope", scopes.stream().map(Scope::getId).toArray(String[]::new));
        }

        List<Resource> resources = storeFactory.getResourceStore().findByResourceServer(search, this.resourceServer.getId(), firstResult != null ? firstResult : -1, maxResult != null ? maxResult : Constants.DEFAULT_MAX_RESULTS);

        if (matchingUri != null && matchingUri && resources.isEmpty()) {
            HashMap<String, String[]> attributes = new HashMap<>();

            attributes.put("uri_not_null", new String[]{"true"});
            attributes.put("owner", new String[]{resourceServer.getId()});

            List<Resource> serverResources = storeFactory.getResourceStore().findByResourceServer(attributes, this.resourceServer.getId(), firstResult != null ? firstResult : -1, maxResult != null ? maxResult : Constants.DEFAULT_MAX_RESULTS);

            PathMatcher<Map.Entry<String, Resource>> pathMatcher = new PathMatcher<Map.Entry<String, Resource>>() {
                @Override
                protected String getPath(Map.Entry<String, Resource> entry) {
                    return entry.getKey();
                }

                @Override
                protected Collection<Map.Entry<String, Resource>> getPaths() {
                    Map<String, Resource> result = new HashMap<>();
                    serverResources.forEach(resource -> resource.getUris().forEach(uri -> {
                        result.put(uri, resource);
                    }));

                    return result.entrySet();
                }
            };

            Map.Entry<String, Resource> matches = pathMatcher.matches(uri);

            if (matches != null) {
                resources = Collections.singletonList(matches.getValue());
            }
        }

        Boolean finalDeep = deep;

        return Response.ok(
                resources.stream()
                        .map(resource -> toRepresentation.apply(resource, finalDeep))
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

    private void audit(ResourceRepresentation resource, OperationType operation) {
        audit(resource, null, operation);
    }

    public void audit(ResourceRepresentation resource, String id, OperationType operation) {
        if (authorization.getRealm().isAdminEventsEnabled()) {
            if (id != null) {
                adminEvent.operation(operation).resourcePath(session.getContext().getUri(), id).representation(resource).success();
            } else {
                adminEvent.operation(operation).resourcePath(session.getContext().getUri()).representation(resource).success();
            }
        }
    }
}