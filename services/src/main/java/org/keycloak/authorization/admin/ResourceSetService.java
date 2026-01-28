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

import org.keycloak.OAuthErrorException;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
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
public class ResourceSetService {

    private final AuthorizationProvider authorization;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final KeycloakSession session;
    private final ResourceServer resourceServer;

    public ResourceSetService(KeycloakSession session, ResourceServer resourceServer, AuthorizationProvider authorization, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.resourceServer = resourceServer;
        this.authorization = authorization;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.AUTHORIZATION_RESOURCE);
    }

    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(
            responseCode = "201", description = "Created",
            content = @Content(schema = @Schema(implementation = ResourceRepresentation.class))
        ),
        @APIResponse(responseCode = "400", description = "Bad Request")
    })
    public Response createPost(ResourceRepresentation resource) {
        if (resource == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        ResourceRepresentation newResource = create(resource);

        audit(resource, resource.getId(), OperationType.CREATE);

        return Response.status(Status.CREATED).entity(newResource).build();
    }

    public ResourceRepresentation create(ResourceRepresentation resource) {
        // direct creation of resources it's not expected for admin permission
        AdminPermissionsSchema.SCHEMA.throwExceptionIfAdminPermissionClient(session, resourceServer.getId());

        requireManage();
        StoreFactory storeFactory = this.authorization.getStoreFactory();
        ResourceOwnerRepresentation owner = resource.getOwner();

        if (owner == null) {
            owner = new ResourceOwnerRepresentation();
            owner.setId(resourceServer.getClientId());
            resource.setOwner(owner);
        }

        String ownerId = owner.getId();

        if (ownerId == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "You must specify the resource owner.", Status.BAD_REQUEST);
        }

        Resource existingResource = storeFactory.getResourceStore().findByName(this.resourceServer, resource.getName(), ownerId);

        if (existingResource != null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Resource with name [" + resource.getName() + "] already exists.", Status.CONFLICT);
        }

        return toRepresentation(toModel(resource, this.resourceServer, authorization), resourceServer, authorization);
    }

    @Path("{resource-id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public Response update(@PathParam("resource-id") String id, ResourceRepresentation resource) {
        AdminPermissionsSchema.SCHEMA.throwExceptionIfAdminPermissionClient(session, resourceServer.getId());
        requireManage();
        resource.setId(id);
        StoreFactory storeFactory = this.authorization.getStoreFactory();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        Resource model = resourceStore.findById(resourceServer, resource.getId());

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        toModel(resource, resourceServer, authorization);

        audit(resource, OperationType.UPDATE);

        return Response.noContent().build();
    }

    @Path("{resource-id}")
    @DELETE
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public Response delete(@PathParam("resource-id") String id) {
        AdminPermissionsSchema.SCHEMA.throwExceptionIfAdminPermissionClient(session, resourceServer.getId());
        requireManage();
        StoreFactory storeFactory = authorization.getStoreFactory();
        Resource resource = storeFactory.getResourceStore().findById(resourceServer, id);

        if (resource == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        //to be able to access all lazy loaded fields it's needed to create representation before it's deleted
        ResourceRepresentation resourceRep = toRepresentation(resource, resourceServer, authorization);

        storeFactory.getResourceStore().delete(id);

        audit(resourceRep, OperationType.DELETE);

        return Response.noContent().build();
    }

    @Path("{resource-id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = ResourceRepresentation.class))
        ),
        @APIResponse(responseCode = "404", description = "Not found")
    })
    public Response findById(@PathParam("resource-id") String id) {
        return findById(id, resource -> toRepresentation(resource, resourceServer, authorization, true));
    }

    public Response findById(String id, Function<Resource, ? extends ResourceRepresentation> toRepresentation) {
        requireView();
        StoreFactory storeFactory = authorization.getStoreFactory();
        Resource model = storeFactory.getResourceStore().findById(resourceServer, id);

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(toRepresentation.apply(model)).build();
    }

    @Path("{resource-id}/scopes")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = ScopeRepresentation.class, type = SchemaType.ARRAY))
        ),
        @APIResponse(responseCode = "404", description = "Not found")
    })
    public Response getScopes(@PathParam("resource-id") String id) {
        requireView();
        StoreFactory storeFactory = authorization.getStoreFactory();
        Resource model = storeFactory.getResourceStore().findById(resourceServer, id);

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        List<ScopeRepresentation> scopes = model.getScopes().stream().map(scope -> {
            ScopeRepresentation representation = new ScopeRepresentation();

            representation.setId(scope.getId());
            representation.setName(scope.getName());

            return representation;
        }).collect(Collectors.toList());

        if (model.getType() != null && !model.getOwner().equals(resourceServer.getClientId())) {
            ResourceStore resourceStore = authorization.getStoreFactory().getResourceStore();
            for (Resource typed : resourceStore.findByType(resourceServer, model.getType())) {
                if (typed.getOwner().equals(resourceServer.getClientId()) && !typed.getId().equals(model.getId())) {
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

    @Path("{resource-id}/permissions")
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
    public Response getPermissions(@PathParam("resource-id") String id) {
        requireView();
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        Resource model = resourceStore.findById(resourceServer, id);

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        PolicyStore policyStore = authorization.getStoreFactory().getPolicyStore();
        Set<Policy> policies = new HashSet<>();

        policies.addAll(policyStore.findByResource(resourceServer, model));

        if (model.getType() != null) {
            if (!model.getOwner().equals(resourceServer.getClientId())) {
                // only add policies if the resource is a resource type instance
                policies.addAll(policyStore.findByResourceType(resourceServer, model.getType()));

                Map<Resource.FilterOption, String[]> resourceFilter = new EnumMap<>(Resource.FilterOption.class);

                resourceFilter.put(Resource.FilterOption.OWNER, new String[]{resourceServer.getClientId()});
                resourceFilter.put(Resource.FilterOption.TYPE, new String[]{model.getType()});

            for (Resource resourceType : resourceStore.find(resourceServer, resourceFilter, null, null)) {
                policies.addAll(policyStore.findByResource(resourceServer, resourceType));
            }
        }

        }

        policies.addAll(policyStore.findByScopes(resourceServer, model, model.getScopes()));
        policies.addAll(policyStore.findByScopes(resourceServer, null, model.getScopes()));

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

    @Path("{resource-id}/attributes")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAttributes(@PathParam("resource-id") String id) {
        requireView();
        StoreFactory storeFactory = authorization.getStoreFactory();
        Resource model = storeFactory.getResourceStore().findById(resourceServer, id);

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(model.getAttributes()).build();
    }

    @Path("/search")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = ResourceRepresentation.class))
        ),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "204", description = "No Content")
    })
    public Response find(@QueryParam("name") String name) {
        this.auth.realm().requireViewAuthorization(resourceServer);
        StoreFactory storeFactory = authorization.getStoreFactory();

        if (name == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        Resource model = storeFactory.getResourceStore().findByName(this.resourceServer, name);

        if (model == null) {
            return Response.status(Status.NO_CONTENT).build();
        }

        return Response.ok(toRepresentation(model, this.resourceServer, authorization)).build();
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = ResourceRepresentation.class, type = SchemaType.ARRAY))
    )
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
        return find(id, name, uri, owner, type, scope, matchingUri, exactName, deep, firstResult, maxResult, (BiFunction<Resource, Boolean, ResourceRepresentation>) (resource, deep1) -> toRepresentation(resource, resourceServer, authorization, deep1));
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

            List<Scope> scopes = authorization.getStoreFactory().getScopeStore().findByResourceServer(resourceServer, scopeFilter, null, null);

            if (scopes.isEmpty()) {
                return Response.ok(Collections.emptyList()).build();
            }

            search.put(Resource.FilterOption.SCOPE_ID, scopes.stream().map(Scope::getId).toArray(String[]::new));
        }

        List<Resource> resources = storeFactory.getResourceStore().find(this.resourceServer, search, firstResult != null ? firstResult : -1, maxResult != null ? maxResult : Constants.DEFAULT_MAX_RESULTS);

        if (matchingUri != null && matchingUri && resources.isEmpty()) {
            Map<Resource.FilterOption, String[]> attributes = new EnumMap<>(Resource.FilterOption.class);

            attributes.put(Resource.FilterOption.URI_NOT_NULL, new String[] {"true"});
            attributes.put(Resource.FilterOption.OWNER, new String[] {resourceServer.getClientId()});

            List<Resource> serverResources = storeFactory.getResourceStore().find(this.resourceServer, attributes, firstResult != null ? firstResult : -1, maxResult != null ? maxResult : -1);

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
            this.auth.realm().requireViewAuthorization(resourceServer);
        }
    }

    private void requireManage() {
        if (this.auth != null) {
            this.auth.realm().requireManageAuthorization(resourceServer);
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
