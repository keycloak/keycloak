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

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.common.Profile.Feature;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyProviderRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.resteasy.reactive.NoCache;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class PolicyService {

    protected final ResourceServer resourceServer;
    protected final AuthorizationProvider authorization;
    protected final AdminPermissionEvaluator auth;
    protected final AdminEventBuilder adminEvent;

    public PolicyService(ResourceServer resourceServer, AuthorizationProvider authorization, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.resourceServer = resourceServer;
        this.authorization = authorization;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.AUTHORIZATION_POLICY);
    }

    @Path("{type}")
    public Object getResource(@PathParam("type") String type) {
        PolicyProviderFactory providerFactory = getPolicyProviderFactory(type);

        if (providerFactory != null) {
            checkIfSupportedPolicyType(type);
            return doCreatePolicyTypeResource(type);
        }

        Policy policy = authorization.getStoreFactory().getPolicyStore().findById(resourceServer, type);

        if (policy != null) {
            checkIfSupportedPolicyType(policy.getType());
        }

        return doCreatePolicyResource(policy);
    }

    protected PolicyTypeService doCreatePolicyTypeResource(String type) {
        return new PolicyTypeService(type, resourceServer, authorization, auth, adminEvent);
    }

    protected Object doCreatePolicyResource(Policy policy) {
        return new PolicyResourceService(policy, resourceServer, authorization, auth, adminEvent);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @APIResponse(responseCode = "201", description = "Created")
    public Response create(String payload) {
        if (auth != null) {
            this.auth.realm().requireManageAuthorization(resourceServer);
        }

        AbstractPolicyRepresentation representation = doCreateRepresentation(payload);
        Policy policy = create(representation);

        representation.setId(policy.getId());

        audit(representation, representation.getId(), OperationType.CREATE, authorization.getKeycloakSession());

        return Response.status(Status.CREATED).entity(representation).build();
    }

    protected AbstractPolicyRepresentation doCreateRepresentation(String payload) {
        PolicyRepresentation representation;

        try {
            representation = JsonSerialization.readValue(payload, PolicyRepresentation.class);
        } catch (IOException cause) {
            throw new RuntimeException("Failed to deserialize representation", cause);
        }

        checkIfSupportedPolicyType(representation.getType());

        return representation;
    }

    public Policy create(AbstractPolicyRepresentation representation) {
        PolicyStore policyStore = authorization.getStoreFactory().getPolicyStore();
        Policy existing = policyStore.findByName(resourceServer, representation.getName());

        if (existing != null) {
            throw new ErrorResponseException("Policy with name [" + representation.getName() + "] already exists", "Conflicting policy", Status.CONFLICT);
        }

        return policyStore.create(resourceServer, representation);
    }

    @Path("/search")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = AbstractPolicyRepresentation.class))
        ),
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request")
    })
    public Response findByName(@QueryParam("name") String name, @QueryParam("fields") String fields) {
        if (auth != null) {
            this.auth.realm().requireViewAuthorization(resourceServer);
        }

        StoreFactory storeFactory = authorization.getStoreFactory();

        if (name == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        Policy model = storeFactory.getPolicyStore().findByName(this.resourceServer, name);

        if (model == null) {
            return Response.noContent().build();
        }

        return Response.ok(toRepresentation(model, fields, authorization)).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = AbstractPolicyRepresentation.class, type = SchemaType.ARRAY))
        ),
        @APIResponse(responseCode = "204", description = "No Content")
    })
    public Response findAll(@QueryParam("policyId") String id,
                            @QueryParam("name") String name,
                            @QueryParam("type") String type,
                            @QueryParam("resourceType") String resourceType,
                            @QueryParam("resource") String resource,
                            @QueryParam("scope") String scope,
                            @QueryParam("permission") Boolean permission,
                            @QueryParam("owner") String owner,
                            @QueryParam("fields") String fields,
                            @QueryParam("first") Integer firstResult,
                            @QueryParam("max") Integer maxResult) {
        if (auth != null) {
            this.auth.realm().requireViewAuthorization(resourceServer);
        }

        Map<Policy.FilterOption, String[]> search = new EnumMap<>(Policy.FilterOption.class);

        if (id != null && !"".equals(id.trim())) {
            search.put(Policy.FilterOption.ID, new String[] {id});
        }

        if (name != null && !"".equals(name.trim())) {
            search.put(Policy.FilterOption.NAME, new String[] {name});
        }

        if (type != null && !"".equals(type.trim())) {
            search.put(Policy.FilterOption.TYPE, new String[] {type});
        }

        if (owner != null && !"".equals(owner.trim())) {
            search.put(Policy.FilterOption.OWNER, new String[] {owner});
        }

        StoreFactory storeFactory = authorization.getStoreFactory();

        if (resource != null && !"".equals(resource.trim())) {
            ResourceStore resourceStore = storeFactory.getResourceStore();
            Resource resourceModel = resourceStore.findById(resourceServer, resource);

            if (resourceModel == null) {
                Map<Resource.FilterOption, String[]> resourceFilters = new EnumMap<>(Resource.FilterOption.class);

                resourceFilters.put(Resource.FilterOption.NAME, new String[]{resource});

                if (owner != null) {
                    resourceFilters.put(Resource.FilterOption.OWNER, new String[]{owner});
                }

                Set<String> resources = resourceStore.find(resourceServer, resourceFilters, -1, 1).stream().map(Resource::getId).collect(Collectors.toSet());

                if (resources.isEmpty()) {
                    return Response.noContent().build();
                }

                search.put(Policy.FilterOption.RESOURCE_ID, resources.toArray(new String[resources.size()]));
            } else {
                search.put(Policy.FilterOption.RESOURCE_ID, new String[] {resourceModel.getId()});
            }
        }

        if (scope != null && !"".equals(scope.trim())) {
            ScopeStore scopeStore = storeFactory.getScopeStore();
            Scope scopeModel = scopeStore.findById(resourceServer, scope);

            if (scopeModel == null) {
                Map<Scope.FilterOption, String[]> scopeFilters = new EnumMap<>(Scope.FilterOption.class);

                scopeFilters.put(Scope.FilterOption.NAME, new String[]{scope});

                Set<String> scopes = scopeStore.findByResourceServer(resourceServer, scopeFilters, -1, 1).stream().map(Scope::getId).collect(Collectors.toSet());

                if (scopes.isEmpty()) {
                    return Response.noContent().build();
                }

                search.put(Policy.FilterOption.SCOPE_ID, scopes.toArray(new String[scopes.size()]));
            } else {
                search.put(Policy.FilterOption.SCOPE_ID, new String[] {scopeModel.getId()});
            }
        }

        if (permission != null) {
            search.put(Policy.FilterOption.PERMISSION, new String[] {permission.toString()});
        }

        if (StringUtil.isNotBlank(resourceType)) {
            search.put(Policy.FilterOption.CONFIG, new String[] {"defaultResourceType", resourceType});
        }

        return Response.ok(
                doSearch(firstResult, maxResult, fields, search))
                .build();
    }

    protected AbstractPolicyRepresentation toRepresentation(Policy model, String fields, AuthorizationProvider authorization) {
        checkIfSupportedPolicyType(model.getType());
        return ModelToRepresentation.toRepresentation(model, authorization, true, false, fields != null && fields.equals("*"));
    }

    protected List<Object> doSearch(Integer firstResult, Integer maxResult, String fields, Map<Policy.FilterOption, String[]> filters) {
        PolicyStore policyStore = authorization.getStoreFactory().getPolicyStore();
        return policyStore.find(resourceServer, filters, firstResult != null ? firstResult : -1, maxResult != null ? maxResult : Constants.DEFAULT_MAX_RESULTS).stream()
                .map(policy -> toRepresentation(policy, fields, authorization))
                .collect(Collectors.toList());
    }

    @Path("providers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @APIResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = PolicyProviderRepresentation.class, type = SchemaType.ARRAY))
    )
    public Response findPolicyProviders() {
        if (auth != null) {
            this.auth.realm().requireViewAuthorization(resourceServer);
        }

        return Response.ok(
                authorization.getProviderFactoriesStream()
                        .filter(((Predicate<PolicyProviderFactory>) PolicyProviderFactory::isInternal).negate())
                        .map(factory -> {
                            PolicyProviderRepresentation representation = new PolicyProviderRepresentation();

                            representation.setName(factory.getName());
                            representation.setGroup(factory.getGroup());
                            representation.setType(factory.getId());

                            return representation;
                        })
                        .collect(Collectors.toList()))
                .build();
    }

    @Path("evaluate")
    public PolicyEvaluationService getPolicyEvaluateResource() {
        if (auth != null) {
            this.auth.realm().requireViewAuthorization(resourceServer);
        }

        return new PolicyEvaluationService(this.resourceServer, this.authorization, this.auth);
    }

    protected PolicyProviderAdminService getPolicyProviderAdminResource(String policyType) {
        return getPolicyProviderFactory(policyType).getAdminResource(resourceServer, authorization);
    }

    protected PolicyProviderFactory getPolicyProviderFactory(String policyType) {
        return authorization.getProviderFactory(policyType);
    }

    private void audit(AbstractPolicyRepresentation resource, String id, OperationType operation, KeycloakSession session) {
        if (id != null) {
            adminEvent.operation(operation).resourcePath(session.getContext().getUri(), id).representation(resource).success();
        } else {
            adminEvent.operation(operation).resourcePath(session.getContext().getUri()).representation(resource).success();
        }
    }

    private void checkIfSupportedPolicyType(String type) throws BadRequestException {
        if (AdminPermissionsSchema.SCHEMA.isSupportedPolicyType(authorization.getKeycloakSession(), resourceServer, type)) {
            return;
        }

        throw new BadRequestException("Policy type not supported by feature " + Feature.ADMIN_FINE_GRAINED_AUTHZ_V2.getVersionedKey());
    }
}
