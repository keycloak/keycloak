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

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;
import static org.keycloak.models.utils.RepresentationToModel.toModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

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

        ResourceRepresentation newResource = create(resource);

        audit(resource, resource.getId(), OperationType.CREATE);

        return Response.status(Status.CREATED).entity(newResource).build();
    }

    public ResourceRepresentation create(ResourceRepresentation resource) {
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

        return toRepresentation(toModel(resource, this.resourceServer, authorization), resourceServer.getId(), authorization);
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

        audit(toRepresentation(resource, resourceServer.getId(), authorization), OperationType.DELETE);

        return Response.noContent().build();
    }

    @Path("{id}")
    @GET
    @NoCache
    @Produces("application/json")
    public Response findById(@PathParam("id") String id) {
        return findById(id, resource -> toRepresentation(resource, resourceServer.getId(), authorization, true));
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

            Map<Resource.FilterOption, String[]> resourceFilter = new EnumMap<>(Resource.FilterOption.class);

            resourceFilter.put(Resource.FilterOption.OWNER, new String[]{resourceServer.getId()});
            resourceFilter.put(Resource.FilterOption.TYPE, new String[]{model.getType()});

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
            return Response.status(Status.NO_CONTENT).build();
        }

        return Response.ok(toRepresentation(model, this.resourceServer.getId(), authorization)).build();
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
        return find(id, name, uri, owner, type, scope, matchingUri, exactName, deep, firstResult, maxResult, (BiFunction<Resource, Boolean, ResourceRepresentation>) (resource, deep1) -> toRepresentation(resource, resourceServer.getId(), authorization, deep1));
    }

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
                         @QueryParam("max") Integer maxResult,
                         BiFunction<Resource, Boolean, ?> toRepresentation) {
        requireView();

        StoreFactory storeFactory = authorization.getStoreFactory();

        if (deep == null) {
            deep = true;
        }

        Map<Resource.FilterOption, String[]> search = new EnumMap<>(Resource.FilterOption.class);

        if (id != null && !"".equals(id.trim())) {
            search.put(Resource.FilterOption.ID, new String[] {id});
        }

        if (name != null && !"".equals(name.trim())) {
            search.put(exactName != null && exactName ? Resource.FilterOption.EXACT_NAME : Resource.FilterOption.NAME, new String[] {name});
        }

        if (uri != null && !"".equals(uri.trim())) {
            search.put(Resource.FilterOption.URI, new String[] {uri});
        }

        if (owner != null && !"".equals(owner.trim())) {
            RealmModel realm = authorization.getKeycloakSession().getContext().getRealm();
            ClientModel clientModel = realm.getClientByClientId(owner);

            if (clientModel != null) {
                owner = clientModel.getId();
            } else {
                UserModel user = authorization.getKeycloakSession().users().getUserByUsername(realm, owner);

                if (user != null) {
                    owner = user.getId();
                }
            }

            search.put(Resource.FilterOption.OWNER, new String[] {owner});
        }

        if (type != null && !"".equals(type.trim())) {
            search.put(Resource.FilterOption.TYPE, new String[] {type});
        }

        if (scope != null && !"".equals(scope.trim())) {
            Map<Scope.FilterOption, String[]> scopeFilter = new EnumMap<>(Scope.FilterOption.class);

            scopeFilter.put(Scope.FilterOption.NAME, new String[] {scope});

            List<Scope> scopes = authorization.getStoreFactory().getScopeStore().findByResourceServer(scopeFilter, resourceServer.getId(), -1, -1);

            if (scopes.isEmpty()) {
                return Response.ok(Collections.emptyList()).build();
            }

            search.put(Resource.FilterOption.SCOPE_ID, scopes.stream().map(Scope::getId).toArray(String[]::new));
        }

        List<Resource> resources = storeFactory.getResourceStore().findByResourceServer(search, this.resourceServer.getId(), firstResult != null ? firstResult : -1, maxResult != null ? maxResult : Constants.DEFAULT_MAX_RESULTS);

        if (matchingUri != null && matchingUri && resources.isEmpty()) {
            Map<Resource.FilterOption, String[]> attributes = new EnumMap<>(Resource.FilterOption.class);

            attributes.put(Resource.FilterOption.URI_NOT_NULL, new String[] {"true"});
            attributes.put(Resource.FilterOption.OWNER, new String[] {resourceServer.getId()});

            List<Resource> serverResources = storeFactory.getResourceStore().findByResourceServer(attributes, this.resourceServer.getId(), firstResult != null ? firstResult : -1, maxResult != null ? maxResult : -1);

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
        if (id != null) {
            adminEvent.operation(operation).resourcePath(session.getContext().getUri(), id).representation(resource).success();
        } else {
            adminEvent.operation(operation).resourcePath(session.getContext().getUri()).representation(resource).success();
        }
    }
}
