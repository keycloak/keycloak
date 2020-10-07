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

import java.io.IOException;
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
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.OAuthErrorException;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.admin.PermissionService;
import org.keycloak.authorization.admin.PolicyTypeResourceService;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.authorization.UmaPermissionRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:federico@martel-innovate.com">Federico M. Facca</a>
 */
public class UserManagedPermissionService {

    private final ResourceServer resourceServer;
    private final Identity identity;
    private final AuthorizationProvider authorization;
    private final PermissionService delegate;

    public UserManagedPermissionService(KeycloakIdentity identity, ResourceServer resourceServer, AuthorizationProvider authorization, AdminEventBuilder eventBuilder) {
        this.identity = identity;
        this.resourceServer = resourceServer;
        this.authorization = authorization;
        delegate = new PermissionService(resourceServer, authorization, null, eventBuilder);
        ResteasyProviderFactory.getInstance().injectProperties(delegate);
    }

    @POST
    @Path("{resourceId}")
    @Consumes("application/json")
    @Produces("application/json")
    public Response create(@PathParam("resourceId") String resourceId, UmaPermissionRepresentation representation) {
        if (representation.getId() != null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Newly created uma policies should not have an id", Response.Status.BAD_REQUEST);
        }

        checkRequest(resourceId, representation);

        representation.addResource(resourceId);
        representation.setOwner(identity.getId());

        return findById(delegate.create(representation).getId());
    }

    @Path("{policyId}")
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response update(@PathParam("policyId") String policyId, String payload) {
        UmaPermissionRepresentation representation;

        try {
            representation = JsonSerialization.readValue(payload, UmaPermissionRepresentation.class);
        } catch (IOException e) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Failed to parse representation", Status.BAD_REQUEST);
        }

        checkRequest(getAssociatedResourceId(policyId), representation);

        return PolicyTypeResourceService.class.cast(delegate.getResource(policyId)).update(payload);
    }

    @Path("{policyId}")
    @DELETE
    public Response delete(@PathParam("policyId") String policyId) {
        checkRequest(getAssociatedResourceId(policyId), null);
        PolicyTypeResourceService.class.cast(delegate.getResource(policyId)).delete();
        return Response.noContent().build();
    }

    @Path("{policyId}")
    @GET
    @Produces("application/json")
    public Response findById(@PathParam("policyId") String policyId) {
        checkRequest(getAssociatedResourceId(policyId), null);
        return PolicyTypeResourceService.class.cast(delegate.getResource(policyId)).findById(null);
    }

    @GET
    @NoCache
    @Produces("application/json")
    public Response find(@QueryParam("name") String name,
                         @QueryParam("resource") String resource,
                         @QueryParam("scope") String scope,
                         @QueryParam("first") Integer firstResult,
                         @QueryParam("max") Integer maxResult) {
        return  delegate.findAll(null, name, "uma", resource, scope, true, identity.getId(), null, firstResult, maxResult);
    }

    private Policy getPolicy(@PathParam("policyId") String policyId) {
        Policy existing = authorization.getStoreFactory().getPolicyStore().findById(policyId, resourceServer.getId());

        if (existing == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Policy with [" + policyId + "] does not exist", Status.NOT_FOUND);
        }

        return existing;
    }

    private void checkRequest(String resourceId, UmaPermissionRepresentation representation) {
        ResourceStore resourceStore = this.authorization.getStoreFactory().getResourceStore();
        Resource resource = resourceStore.findById(resourceId, resourceServer.getId());

        if (resource == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Resource [" + resourceId + "] cannot be found", Response.Status.BAD_REQUEST);
        }

        if (!resource.getOwner().equals(identity.getId())) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Only resource owner can access policies for resource [" + resourceId + "]", Status.BAD_REQUEST);
        }

        if (!resource.isOwnerManagedAccess()) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Only resources with owner managed accessed can have policies", Status.BAD_REQUEST);
        }

        if (!resourceServer.isAllowRemoteResourceManagement()) {
            throw new ErrorResponseException(OAuthErrorException.REQUEST_NOT_SUPPORTED, "Remote Resource Management not enabled on resource server [" + resourceServer.getId() + "]", Status.FORBIDDEN);
        }

        if (representation != null) {
            Set<String> resourceScopes = resource.getScopes().stream().map(scope -> scope.getName()).collect(Collectors.toSet());
            Set<String> scopes = representation.getScopes();

            if (scopes == null || scopes.isEmpty()) {
                scopes = resourceScopes;
                representation.setScopes(scopes);
            }

            if (!resourceScopes.containsAll(scopes)) {
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Some of the scopes [" + scopes + "] are not valid for resource [" + resourceId + "]", Response.Status.BAD_REQUEST);
            }

            if (representation.getCondition() != null) {
                if (!Profile.isFeatureEnabled(Profile.Feature.UPLOAD_SCRIPTS)) {
                    throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Script upload not supported", Status.BAD_REQUEST);
                }
            }
        }
    }

    private String getAssociatedResourceId(String policyId) {
        return getPolicy(policyId).getResources().iterator().next().getId();
    }
}
