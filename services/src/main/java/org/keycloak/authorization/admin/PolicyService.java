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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.authorization.AuthorizationProvider;
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
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyProviderRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
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
            return doCreatePolicyTypeResource(type);
        }

        Policy policy = authorization.getStoreFactory().getPolicyStore().findById(resourceServer.getRealm(), resourceServer, type);

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
    public Response create(String payload, @Context KeycloakSession session) {
        if (auth != null) {
            this.auth.realm().requireManageAuthorization();
        }

        AbstractPolicyRepresentation representation = doCreateRepresentation(payload);
        Policy policy = create(representation);

        representation.setId(policy.getId());

        audit(representation, representation.getId(), OperationType.CREATE, session);

        return Response.status(Status.CREATED).entity(representation).build();
    }

    protected AbstractPolicyRepresentation doCreateRepresentation(String payload) {
        PolicyRepresentation representation;

        try {
            representation = JsonSerialization.readValue(payload, PolicyRepresentation.class);
        } catch (IOException cause) {
            throw new RuntimeException("Failed to deserialize representation", cause);
        }

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
    public Response findByName(@QueryParam("name") String name, @QueryParam("fields") String fields) {
        if (auth != null) {
            this.auth.realm().requireViewAuthorization();
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
    public Response findAll(@QueryParam("policyId") String id,
                            @QueryParam("name") String name,
                            @QueryParam("type") String type,
                            @QueryParam("resource") String resource,
                            @QueryParam("scope") String scope,
                            @QueryParam("permission") Boolean permission,
                            @QueryParam("owner") String owner,
                            @QueryParam("fields") String fields,
                            @QueryParam("first") Integer firstResult,
                            @QueryParam("max") Integer maxResult) {
        if (auth != null) {
            this.auth.realm().requireViewAuthorization();
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
            Resource resourceModel = resourceStore.findById(resourceServer.getRealm(), resourceServer, resource);

            if (resourceModel == null) {
                Map<Resource.FilterOption, String[]> resourceFilters = new EnumMap<>(Resource.FilterOption.class);

                resourceFilters.put(Resource.FilterOption.NAME, new String[]{resource});

                if (owner != null) {
                    resourceFilters.put(Resource.FilterOption.OWNER, new String[]{owner});
                }

                Set<String> resources = resourceStore.find(resourceServer.getRealm(), resourceServer, resourceFilters, -1, 1).stream().map(Resource::getId).collect(Collectors.toSet());

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
            Scope scopeModel = scopeStore.findById(resourceServer.getRealm(), resourceServer, scope);

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

        return Response.ok(
                doSearch(firstResult, maxResult, fields, search))
                .build();
    }

    protected AbstractPolicyRepresentation toRepresentation(Policy model, String fields, AuthorizationProvider authorization) {
        return ModelToRepresentation.toRepresentation(model, authorization, true, false, fields != null && fields.equals("*"));
    }

    protected List<Object> doSearch(Integer firstResult, Integer maxResult, String fields, Map<Policy.FilterOption, String[]> filters) {
        PolicyStore policyStore = authorization.getStoreFactory().getPolicyStore();
        return policyStore.find(resourceServer.getRealm(), resourceServer, filters, firstResult != null ? firstResult : -1, maxResult != null ? maxResult : Constants.DEFAULT_MAX_RESULTS).stream()
                .map(policy -> toRepresentation(policy, fields, authorization))
                .collect(Collectors.toList());
    }

    @Path("providers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response findPolicyProviders() {
        if (auth != null) {
            this.auth.realm().requireViewAuthorization();
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
            this.auth.realm().requireViewAuthorization();
        }

        PolicyEvaluationService resource = new PolicyEvaluationService(this.resourceServer, this.authorization, this.auth);

        ResteasyProviderFactory.getInstance().injectProperties(resource);

        return resource;
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
}
