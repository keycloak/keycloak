/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyResourceService {

    private final Policy policy;
    protected final ResourceServer resourceServer;
    protected final AuthorizationProvider authorization;
    protected final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    public PolicyResourceService(Policy policy, ResourceServer resourceServer, AuthorizationProvider authorization, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.policy = policy;
        this.resourceServer = resourceServer;
        this.authorization = authorization;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.AUTHORIZATION_POLICY);
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @NoCache
    public Response update(String payload) {
        if (auth != null) {
            this.auth.realm().requireManageAuthorization();
        }

        AbstractPolicyRepresentation representation = doCreateRepresentation(payload);

        if (policy == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        representation.setId(policy.getId());

        RepresentationToModel.toModel(representation, authorization, policy);


        audit(representation, OperationType.UPDATE);

        return Response.status(Status.CREATED).build();
    }

    @DELETE
    public Response delete() {
        if (auth != null) {
            this.auth.realm().requireManageAuthorization();
        }

        if (policy == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        StoreFactory storeFactory = authorization.getStoreFactory();
        PolicyStore policyStore = storeFactory.getPolicyStore();
        PolicyProviderFactory resource = getProviderFactory(policy.getType());

        if (resource != null) {
            resource.onRemove(policy, authorization);
        }

        policyStore.delete(policy.getId());

        audit(toRepresentation(policy, authorization), OperationType.DELETE);

        return Response.noContent().build();
    }

    @GET
    @Produces("application/json")
    @NoCache
    public Response findById(@QueryParam("fields") String fields) {
        if (auth != null) {
            this.auth.realm().requireViewAuthorization();
        }

        if (policy == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(toRepresentation(policy, fields, authorization)).build();
    }

    private AbstractPolicyRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        return toRepresentation(policy, null, authorization);
    }

    protected AbstractPolicyRepresentation toRepresentation(Policy policy, String fields, AuthorizationProvider authorization) {
        return ModelToRepresentation.toRepresentation(policy, authorization, true, false, fields != null && fields.equals("*"));
    }

    @Path("/dependentPolicies")
    @GET
    @Produces("application/json")
    @NoCache
    public Response getDependentPolicies() {
        if (auth != null) {
            this.auth.realm().requireViewAuthorization();
        }

        if (policy == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        List<Policy> policies = authorization.getStoreFactory().getPolicyStore().findDependentPolicies(policy.getId(), resourceServer.getId());

        return Response.ok(policies.stream().map(policy -> {
            PolicyRepresentation representation1 = new PolicyRepresentation();

            representation1.setId(policy.getId());
            representation1.setName(policy.getName());
            representation1.setType(policy.getType());

            return representation1;
        }).collect(Collectors.toList())).build();
    }

    @Path("/scopes")
    @GET
    @Produces("application/json")
    @NoCache
    public Response getScopes() {
        if (auth != null) {
            this.auth.realm().requireViewAuthorization();
        }

        if (policy == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(policy.getScopes().stream().map(scope -> {
            ScopeRepresentation representation = new ScopeRepresentation();

            representation.setId(scope.getId());
            representation.setName(scope.getName());

            return representation;
        }).collect(Collectors.toList())).build();
    }

    @Path("/resources")
    @GET
    @Produces("application/json")
    @NoCache
    public Response getResources() {
        if (auth != null) {
            this.auth.realm().requireViewAuthorization();
        }

        if (policy == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(policy.getResources().stream().map(resource -> {
            ResourceRepresentation representation = new ResourceRepresentation();

            representation.setId(resource.getId());
            representation.setName(resource.getName());

            return representation;
        }).collect(Collectors.toList())).build();
    }

    @Path("/associatedPolicies")
    @GET
    @Produces("application/json")
    @NoCache
    public Response getAssociatedPolicies() {
        if (auth != null) {
            this.auth.realm().requireViewAuthorization();
        }

        if (policy == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(policy.getAssociatedPolicies().stream().map(policy -> {
            PolicyRepresentation representation1 = new PolicyRepresentation();

            representation1.setId(policy.getId());
            representation1.setName(policy.getName());
            representation1.setType(policy.getType());
            representation1.setDescription(policy.getDescription());

            return representation1;
        }).collect(Collectors.toList())).build();
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

    private PolicyProviderFactory getProviderFactory(String policyType) {
        return authorization.getProviderFactory(policyType);
    }

    protected Policy getPolicy() {
        return policy;
    }

    private void audit(AbstractPolicyRepresentation policy, OperationType operation) {
        adminEvent.operation(operation).resourcePath(authorization.getKeycloakSession().getContext().getUri()).representation(policy).success();
    }
}
