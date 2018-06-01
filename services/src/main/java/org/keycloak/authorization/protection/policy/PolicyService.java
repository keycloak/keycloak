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
package org.keycloak.authorization.protection.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.authorization.UmaPermissionTypeRepresentation;
import org.keycloak.services.ErrorResponseException;

/**
 * @author <a href="mailto:federico@martel-innovate.com">Federico M. Facca</a>
 */
public class PolicyService {

    private final ResourceServer resourceServer;
    private final Identity identity;
    private final AuthorizationProvider authorization;
    private final ResourceStore resourceStore;
    private final PolicyStore policyStore;

    public PolicyService(Identity identity, ResourceServer resourceServer, AuthorizationProvider authorization) {
        this.identity = identity;
        this.resourceServer = resourceServer;
        this.authorization = authorization;
        this.resourceStore = this.authorization.getStoreFactory().getResourceStore();
        this.policyStore = this.authorization.getStoreFactory().getPolicyStore();
    }

    @POST
    @Path("{resourceId}")
    @Consumes("application/json")
    @Produces("application/json")
    public Response create(@PathParam("resourceId") String resourceId, UmaPermissionTypeRepresentation policy) {
        validateRequest(resourceId);
        validateUmaPermissionTypeRepresentation(resourceId, false, policy);
        validateScopes(resourceId, policy.getScopes());
        Policy model = RepresentationToModel.createPolicyfromUmaPermissionTypeRepresentation(policy, resourceId, resourceServer, authorization, identity);
        return Response.ok().entity(ModelToRepresentation.toUmaPermissionTypeRepresentation(model, authorization)).build();
    }

    @Path("{resourceId}/{policyId}")
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response update(@PathParam("resourceId") String resourceId, @PathParam("policyId") String policyId, UmaPermissionTypeRepresentation policy) {
        validateRequest(resourceId);
        validateUmaPermissionTypeRepresentation(resourceId, true, policy);
        validateScopes(resourceId, policy.getScopes());
        Policy model = RepresentationToModel.createPolicyfromUmaPermissionTypeRepresentation(policy, resourceId, resourceServer, authorization, identity);
        return Response.ok().entity(ModelToRepresentation.toUmaPermissionTypeRepresentation(model, authorization)).build();
    }

    @Path("{resourceId}/{policyId}")
    @DELETE
    public Response delete(@PathParam("resourceId") String resourceId, @PathParam("policyId") String policyId) {
        validateRequest(resourceId);
        Policy policy = policyStore.findById(policyId, resourceServer.getId());
        for (Policy associatedPolicy : policy.getAssociatedPolicies()) {
                   policyStore.delete(associatedPolicy.getId());
        }
        policyStore.delete(policyId);
        return Response.noContent().build();
    }

    @Path("{resourceId}/{policyId}")
    @GET
    @Produces("application/json")
    public Response findById(@PathParam("resourceId") String resourceId, @PathParam("policyId") String policyId) {
        return find(resourceId, policyId, -1, -1);
    }
    
    @Path("{resourceId}")
    @GET
    @NoCache
    @Produces("application/json")
    public Response find(@PathParam("resourceId") String resourceId,
                         @QueryParam("policyId") String policyId,
                         @QueryParam("first") Integer firstResult,
                         @QueryParam("max") Integer maxResult) {
        validateRequest(resourceId);
        
        Map<String, String[]> filters = new HashMap<>();
        
        filters.put("owner", new String[] {identity.getId()});
        
        if (resourceId != null) {
            List<Policy> policies = new ArrayList<>();
            
            if (!"".equals(resourceId.trim())) {
                policies.addAll(policyStore.findByResource(resourceId, resourceServer.getId()));
            }

            if (policies.isEmpty()) {
                return Response.ok(Collections.emptyList()).build();
            }
            if (policyId == null)
                filters.put("id", policies.stream().map(Policy::getId).toArray(String[]::new));
            else {
                Set<String> requestedPolicyIds = new HashSet<>(Arrays.asList(policyId.split("\\|", -1)));
                Set<String> allowedPolicyIds = new HashSet<>(Arrays.asList(policies.stream().map(Policy::getId).toArray(String[]::new)));
                if (!allowedPolicyIds.containsAll(requestedPolicyIds))
                    throw new ErrorResponseException("not_valid", "Some or all policyIDs [" + policyId + "] are not valid for resource [" + resourceId + "]", Response.Status.BAD_REQUEST);
                filters.put("id", policyId.split("\\|", -1));
            }
        }
        
        return  Response.ok().entity(policyStore.findByResourceServer(filters, resourceServer.getId(), firstResult != null ? firstResult : -1, maxResult != null ? maxResult : Constants.DEFAULT_MAX_RESULTS)
                    .stream()
                        .map(policy -> ModelToRepresentation.toUmaPermissionTypeRepresentation(policy, authorization))
                        .collect(Collectors.toList()))
                .build();
    }
    
    @GET
    @NoCache
    @Produces("application/json")
    public Response findAll() {
        List <ResourceUmaPolicy> resourcePolicyList = new ArrayList<>();
        resourceStore.findByOwner(identity.getId(), resourceServer.getId()).stream().forEach(resource -> {
            List<Policy> policies = new ArrayList<>();
            policies.addAll(policyStore.findByResource(resource.getId(), resourceServer.getId()));
            if (policies.isEmpty()) {
                resourcePolicyList.add(new ResourceUmaPolicy(resource.getId(), Collections.emptySet()));
            } else {
                Map<String, String[]> filters = new HashMap<>();
                filters.put("owner", new String[] {identity.getId()});
                filters.put("id", policies.stream().map(Policy::getId).toArray(String[]::new));
                resourcePolicyList.add(new ResourceUmaPolicy(resource.getId(), 
                policyStore.findByResourceServer(filters, resourceServer.getId(), -1, -1)
                        .stream()
                            .map(policy -> ModelToRepresentation.toUmaPermissionTypeRepresentation(policy, authorization))
                            .collect(Collectors.toSet())));
            }
        });
    
        return  Response.ok().entity(resourcePolicyList).build();
    }
    
    private boolean checkResourceOwner(Resource resource, String identity){
        return resource.getOwner().equals(identity);
    }
    
    private Resource getResource(String resourceId){
        return resourceStore.findById(resourceId, resourceServer.getId());
    }
    
    private void validateRequest(String resourceId){
        Resource resource = getResource(resourceId);
        if (resource == null)
            throw new ErrorResponseException("invalid", "Resource [" + resourceServer.getId() + "] cannot be found", Response.Status.BAD_REQUEST);
        if (!checkResourceOwner(resource,identity.getId())) 
            throw new ErrorResponseException("not_authorised", "Only resource onwer can access policies for resourcer [" + resourceId + "]", Response.Status.UNAUTHORIZED);
        if (!resource.isOwnerManagedAccess()) 
            throw new ErrorResponseException("invalid", "Only resources with owner managed accessed can have policies", Response.Status.BAD_REQUEST);
        if (!resourceServer.isAllowRemoteResourceManagement())
            throw new ErrorResponseException("invalid", "Remote Resource Management not enabled on resource server [" + resourceServer.getId() + "]", Response.Status.BAD_REQUEST);
    }
    
    private void validateUmaPermissionTypeRepresentation(String resourceId, boolean update, UmaPermissionTypeRepresentation policy){
        if (policy.getId() != null && !update) 
            throw new ErrorResponseException("invalid", "Newly created uma policies should not have an id", Response.Status.BAD_REQUEST);
    }
    
    private void validateScopes(String resource, Set<String> scopes){
        Resource r = resourceStore.findById(resource, resourceServer.getId());
        Set<String> resourceScopes = r.getScopes().stream().map(scope1 -> {
            return scope1.getName();
        }).collect(Collectors.toSet());
        if (!resourceScopes.containsAll(scopes))
            throw new ErrorResponseException("invalid", "Some of the scopes [" + scopes + "] are not valid for resource [" + resource + "]", Response.Status.BAD_REQUEST);;
    }
    
    class ResourceUmaPolicy {
        
        private String resource;
        private Set<UmaPermissionTypeRepresentation> policies;
        
        public ResourceUmaPolicy( String resource, Set<UmaPermissionTypeRepresentation> policies){
            this.resource = resource;
            this.policies = policies;
        }
        
        public String getResource(){
            return resource;
        }
        
        public Set<UmaPermissionTypeRepresentation> getPolicies(){
            return policies;
        }
        
        public void setResource(String resource){
            this.resource = resource;
        }
        
        public void setPolicies(Set<UmaPermissionTypeRepresentation> policies){
            this.policies = policies;
        }

    }

}
