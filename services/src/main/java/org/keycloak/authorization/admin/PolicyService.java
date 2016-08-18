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
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.authorization.PolicyProviderRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;
import static org.keycloak.models.utils.RepresentationToModel.toModel;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyService {

    private final ResourceServer resourceServer;
    private final AuthorizationProvider authorization;
    private final RealmAuth auth;

    public PolicyService(ResourceServer resourceServer, AuthorizationProvider authorization, RealmAuth auth) {
        this.resourceServer = resourceServer;
        this.authorization = authorization;
        this.auth = auth;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @NoCache
    public Response create(PolicyRepresentation representation) {
        this.auth.requireManage();
        Policy policy = toModel(representation, this.resourceServer, authorization);
        PolicyProviderAdminService resource = getPolicyProviderAdminResource(policy.getType(), authorization);

        if (resource != null) {
            try {
                resource.onCreate(policy);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        representation.setId(policy.getId());

        return Response.status(Status.CREATED).entity(representation).build();
    }

    @Path("{id}")
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @NoCache
    public Response update(@PathParam("id") String id, PolicyRepresentation representation) {
        this.auth.requireManage();
        representation.setId(id);
        StoreFactory storeFactory = authorization.getStoreFactory();
        Policy policy = storeFactory.getPolicyStore().findById(representation.getId());

        if (policy == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        policy = toModel(representation, resourceServer, authorization);

        PolicyProviderAdminService resource = getPolicyProviderAdminResource(policy.getType(), authorization);

        if (resource != null) {
            try {
                resource.onUpdate(policy);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return Response.status(Status.CREATED).build();
    }

    @Path("{id}")
    @DELETE
    public Response delete(@PathParam("id") String id) {
        this.auth.requireManage();
        StoreFactory storeFactory = authorization.getStoreFactory();
        PolicyStore policyStore = storeFactory.getPolicyStore();
        Policy policy = policyStore.findById(id);

        if (policy == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        PolicyProviderAdminService resource = getPolicyProviderAdminResource(policy.getType(), authorization);

        if (resource != null) {
            try {
                resource.onRemove(policy);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        policyStore.findDependentPolicies(id).forEach(dependentPolicy -> {
            if (dependentPolicy.getAssociatedPolicies().size() == 1) {
                policyStore.delete(dependentPolicy.getId());
            } else {
                dependentPolicy.removeAssociatedPolicy(policy);
            }
        });

        policyStore.delete(policy.getId());

        return Response.noContent().build();
    }

    @Path("{id}")
    @GET
    @Produces("application/json")
    @NoCache
    public Response findById(@PathParam("id") String id) {
        this.auth.requireView();
        StoreFactory storeFactory = authorization.getStoreFactory();
        Policy model = storeFactory.getPolicyStore().findById(id);

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(toRepresentation(model, authorization)).build();
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

        Policy model = storeFactory.getPolicyStore().findByName(name, this.resourceServer.getId());

        if (model == null) {
            return Response.status(Status.OK).build();
        }

        return Response.ok(toRepresentation(model, authorization)).build();
    }

    @GET
    @Produces("application/json")
    @NoCache
    public Response findAll(@QueryParam("name") String name,
                            @QueryParam("type") String type,
                            @QueryParam("resource") String resource,
                            @QueryParam("permission") Boolean permission,
                            @QueryParam("first") Integer firstResult,
                            @QueryParam("max") Integer maxResult) {
        this.auth.requireView();

        Map<String, String[]> search = new HashMap<>();

        if (name != null && !"".equals(name.trim())) {
            search.put("name", new String[] {name});
        }

        if (type != null && !"".equals(type.trim())) {
            search.put("type", new String[] {type});
        }

        StoreFactory storeFactory = authorization.getStoreFactory();

        if (resource != null && !"".equals(resource.trim())) {
            List<Policy> policies = new ArrayList<>();
            HashMap<String, String[]> resourceSearch = new HashMap<>();

            resourceSearch.put("name", new String[] {resource});

            storeFactory.getResourceStore().findByResourceServer(resourceSearch, resourceServer.getId(), -1, -1).forEach(resource1 -> {
                ResourceRepresentation resourceRepresentation = ModelToRepresentation.toRepresentation(resource1, resourceServer, authorization);
                resourceRepresentation.getPolicies().forEach(policyRepresentation -> {
                    Policy associated = storeFactory.getPolicyStore().findById(policyRepresentation.getId());
                    policies.add(associated);
                    findAssociatedPolicies(associated, policies);
                });
            });

            if (policies.isEmpty()) {
                return Response.ok(Collections.emptyList()).build();
            }

            search.put("id", policies.stream().map(Policy::getId).toArray(String[]::new));
        }

        if (permission != null) {
            search.put("permission", new String[] {permission.toString()});
        }

        return Response.ok(
                storeFactory.getPolicyStore().findByResourceServer(search, resourceServer.getId(), firstResult != null ? firstResult : -1, maxResult != null ? maxResult : -1).stream()
                        .map(policy -> toRepresentation(policy, authorization))
                        .collect(Collectors.toList()))
                .build();
    }

    @Path("providers")
    @GET
    @Produces("application/json")
    @NoCache
    public Response findPolicyProviders() {
        this.auth.requireView();
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
        this.auth.requireView();
        PolicyEvaluationService resource = new PolicyEvaluationService(this.resourceServer, this.authorization);

        ResteasyProviderFactory.getInstance().injectProperties(resource);

        return resource;
    }

    @Path("{policyType}")
    public Object getPolicyTypeResource(@PathParam("policyType") String policyType) {
        this.auth.requireView();
        return getPolicyProviderAdminResource(policyType, this.authorization);
    }

    private PolicyProviderAdminService getPolicyProviderAdminResource(String policyType, AuthorizationProvider authorization) {
        PolicyProviderFactory providerFactory = authorization.getProviderFactory(policyType);

        if (providerFactory != null) {
            return providerFactory.getAdminResource(this.resourceServer);
        }

        return null;
    }

    private void findAssociatedPolicies(Policy policy, List<Policy> policies) {
        policy.getAssociatedPolicies().forEach(associated -> {
            policies.add(associated);
            findAssociatedPolicies(associated, policies);
        });
    }
}
