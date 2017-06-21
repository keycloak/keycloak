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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyProviderRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
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

        Policy policy = authorization.getStoreFactory().getPolicyStore().findById(type, resourceServer.getId());

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
    public Response create(@Context UriInfo uriInfo, String payload) {
        this.auth.realm().requireManageAuthorization();

        AbstractPolicyRepresentation representation = doCreateRepresentation(payload);
        Policy policy = create(representation);

        representation.setId(policy.getId());

        audit(uriInfo, representation, representation.getId(), OperationType.CREATE);

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
        Policy existing = policyStore.findByName(representation.getName(), resourceServer.getId());

        if (existing != null) {
            throw new ErrorResponseException("Policy with name [" + representation.getName() + "] already exists", "Conflicting policy", Status.CONFLICT);
        }

        return policyStore.create(representation, resourceServer);
    }

    @Path("/search")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response findByName(@QueryParam("name") String name) {
        this.auth.realm().requireViewAuthorization();
        StoreFactory storeFactory = authorization.getStoreFactory();

        if (name == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        Policy model = storeFactory.getPolicyStore().findByName(name, this.resourceServer.getId());

        if (model == null) {
            return Response.status(Status.OK).build();
        }

        return Response.ok(toRepresentation(model, authorization)).build();
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

        if (type != null && !"".equals(type.trim())) {
            search.put("type", new String[] {type});
        }

        StoreFactory storeFactory = authorization.getStoreFactory();
        PolicyStore policyStore = storeFactory.getPolicyStore();

        if (resource != null || scope != null) {
            List<Policy> policies = new ArrayList<>();

            if (resource != null && !"".equals(resource.trim())) {
                HashMap<String, String[]> resourceSearch = new HashMap<>();

                resourceSearch.put("name", new String[]{resource});

                storeFactory.getResourceStore().findByResourceServer(resourceSearch, resourceServer.getId(), -1, 1).forEach(resource1 -> {
                    policies.addAll(policyStore.findByResource(resource1.getId(), resourceServer.getId()));
                    if (resource1.getType() != null) {
                        policies.addAll(policyStore.findByResourceType(resource1.getType(), resourceServer.getId()));
                    }
                });
            }

            if (scope != null && !"".equals(scope.trim())) {
                HashMap<String, String[]> scopeSearch = new HashMap<>();

                scopeSearch.put("name", new String[]{scope});

                storeFactory.getScopeStore().findByResourceServer(scopeSearch, resourceServer.getId(), -1, 1).forEach(scope1 -> {
                    policies.addAll(policyStore.findByScopeIds(Arrays.asList(scope1.getId()), resourceServer.getId()));
                });
            }

            if (policies.isEmpty()) {
                return Response.ok(Collections.emptyList()).build();
            }

            new ArrayList<>(policies).forEach(policy -> findAssociatedPolicies(policy, policies));

            search.put("id", policies.stream().map(Policy::getId).toArray(String[]::new));
        }

        if (permission != null) {
            search.put("permission", new String[] {permission.toString()});
        }

        return Response.ok(
                doSearch(firstResult, maxResult, search))
                .build();
    }

    protected AbstractPolicyRepresentation toRepresentation(Policy model, AuthorizationProvider authorization) {
        return ModelToRepresentation.toRepresentation(model, PolicyRepresentation.class, authorization);
    }

    protected List<Object> doSearch(Integer firstResult, Integer maxResult, Map<String, String[]> filters) {
        PolicyStore policyStore = authorization.getStoreFactory().getPolicyStore();
        return policyStore.findByResourceServer(filters, resourceServer.getId(), firstResult != null ? firstResult : -1, maxResult != null ? maxResult : Constants.DEFAULT_MAX_RESULTS).stream()
                .map(policy -> toRepresentation(policy, authorization))
                .collect(Collectors.toList());
    }

    @Path("providers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response findPolicyProviders() {
        this.auth.realm().requireViewAuthorization();
        return Response.ok(
                authorization.getProviderFactories().stream()
                        .map(provider -> {
                            PolicyProviderRepresentation representation = new PolicyProviderRepresentation();

                            representation.setName(provider.getName());
                            representation.setGroup(provider.getGroup());
                            representation.setType(provider.getId());

                            return representation;
                        })
                        .collect(Collectors.toList()))
                .build();
    }

    @Path("evaluate")
    public PolicyEvaluationService getPolicyEvaluateResource() {
        this.auth.realm().requireViewAuthorization();
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

    private void findAssociatedPolicies(Policy policy, List<Policy> policies) {
        policy.getAssociatedPolicies().forEach(associated -> {
            policies.add(associated);
            findAssociatedPolicies(associated, policies);
        });
    }

    private void audit(@Context UriInfo uriInfo, AbstractPolicyRepresentation resource, String id, OperationType operation) {
        if (authorization.getRealm().isAdminEventsEnabled()) {
            if (id != null) {
                adminEvent.operation(operation).resourcePath(uriInfo, id).representation(resource).success();
            } else {
                adminEvent.operation(operation).resourcePath(uriInfo).representation(resource).success();
            }
        }
    }
}
