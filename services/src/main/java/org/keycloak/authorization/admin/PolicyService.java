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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.admin.util.Models;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.representations.idm.authorization.PolicyProviderRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.services.resources.admin.RealmAuth;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.HashSet;
import java.util.stream.Collectors;

import static org.keycloak.authorization.admin.util.Models.toRepresentation;

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
        Policy policy = Models.toModel(representation, this.resourceServer, authorization);

        updateResources(policy, authorization);
        updateAssociatedPolicies(policy);
        updateScopes(policy, authorization);

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

        policy.setName(representation.getName());
        policy.setDescription(representation.getDescription());
        policy.setConfig(representation.getConfig());
        policy.setDecisionStrategy(representation.getDecisionStrategy());
        policy.setLogic(representation.getLogic());

        updateResources(policy, authorization);
        updateAssociatedPolicies(policy);
        updateScopes(policy, authorization);

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
            dependentPolicy.removeAssociatedPolicy(policy);
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

    @GET
    @Produces("application/json")
    @NoCache
    public Response findAll() {
        this.auth.requireView();
        StoreFactory storeFactory = authorization.getStoreFactory();
        return Response.ok(
                storeFactory.getPolicyStore().findByResourceServer(resourceServer.getId()).stream()
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

    private void updateScopes(Policy policy, AuthorizationProvider authorization) {
        String scopes = policy.getConfig().get("scopes");
        if (scopes != null) {
            String[] scopeIds;

            try {
                scopeIds = new ObjectMapper().readValue(scopes, String[].class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            StoreFactory storeFactory = authorization.getStoreFactory();
            PolicyStore policyStore = storeFactory.getPolicyStore();

            for (String scopeId : scopeIds) {
                boolean hasScope = false;

                for (Scope scopeModel : new HashSet<Scope>(policy.getScopes())) {
                    if (scopeModel.getId().equals(scopeId)) {
                        hasScope = true;
                    }
                }
                if (!hasScope) {
                    policy.addScope(storeFactory.getScopeStore().findById(scopeId));
                }
            }

            for (Scope scopeModel : new HashSet<Scope>(policy.getScopes())) {
                boolean hasScope = false;

                for (String scopeId : scopeIds) {
                    if (scopeModel.getId().equals(scopeId)) {
                        hasScope = true;
                    }
                }
                if (!hasScope) {
                    policy.removeScope(scopeModel);
                }
            }
        }
    }

    private void updateAssociatedPolicies(Policy policy) {
        String policies = policy.getConfig().get("applyPolicies");

        if (policies != null) {
            String[] policyIds;

            try {
                policyIds = new ObjectMapper().readValue(policies, String[].class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            StoreFactory storeFactory = authorization.getStoreFactory();
            PolicyStore policyStore = storeFactory.getPolicyStore();

            for (String policyId : policyIds) {
                boolean hasPolicy = false;

                for (Policy policyModel : new HashSet<Policy>(policy.getAssociatedPolicies())) {
                    if (policyModel.getId().equals(policyId) || policyModel.getName().equals(policyId)) {
                        hasPolicy = true;
                    }
                }


                if (!hasPolicy) {
                    Policy associatedPolicy = policyStore.findById(policyId);

                    if (associatedPolicy == null) {
                        associatedPolicy = policyStore.findByName(policyId, this.resourceServer.getId());
                    }

                    policy.addAssociatedPolicy(associatedPolicy);
                }
            }

            for (Policy policyModel : new HashSet<Policy>(policy.getAssociatedPolicies())) {
                boolean hasPolicy = false;

                for (String policyId : policyIds) {
                    if (policyModel.getId().equals(policyId) || policyModel.getName().equals(policyId)) {
                        hasPolicy = true;
                    }
                }
                if (!hasPolicy) {
                    policy.removeAssociatedPolicy(policyModel);;
                }
            }
        }
    }

    private void updateResources(Policy policy, AuthorizationProvider authorization) {
        String resources = policy.getConfig().get("resources");
        if (resources != null) {
            String[] resourceIds;

            try {
                resourceIds = new ObjectMapper().readValue(resources, String[].class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            StoreFactory storeFactory = authorization.getStoreFactory();

            for (String resourceId : resourceIds) {
                boolean hasResource = false;
                for (Resource resourceModel : new HashSet<Resource>(policy.getResources())) {
                    if (resourceModel.getId().equals(resourceId)) {
                        hasResource = true;
                    }
                }
                if (!hasResource && !"".equals(resourceId)) {
                    policy.addResource(storeFactory.getResourceStore().findById(resourceId));
                }
            }

            for (Resource resourceModel : new HashSet<Resource>(policy.getResources())) {
                boolean hasResource = false;

                for (String resourceId : resourceIds) {
                    if (resourceModel.getId().equals(resourceId)) {
                        hasResource = true;
                    }
                }

                if (!hasResource) {
                    policy.removeResource(resourceModel);
                }
            }
        }
    }
}
