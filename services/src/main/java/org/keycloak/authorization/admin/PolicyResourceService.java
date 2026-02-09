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
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
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
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.util.JsonSerialization;

import org.jboss.resteasy.reactive.NoCache;

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
            this.auth.realm().requireManageAuthorization(resourceServer);
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
            this.auth.realm().requireManageAuthorization(resourceServer);
        }

        if (policy == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        StoreFactory storeFactory = authorization.getStoreFactory();
        PolicyStore policyStore = storeFactory.getPolicyStore();
        PolicyProviderFactory resource = getProviderFactory(policy.getType());

        //to be able to access all lazy loaded fields it's needed to create representation before it's deleted
        AbstractPolicyRepresentation policyRep = toRepresentation(policy, authorization);

        if (resource != null) {
            resource.onRemove(policy, authorization);
        }

        policyStore.delete(policy.getId());

        audit(policyRep, OperationType.DELETE);

        return Response.noContent().build();
    }

    @GET
    @Produces("application/json")
    @NoCache
    public Response findById(@QueryParam("fields") String fields) {
        if (auth != null) {
            this.auth.realm().requireViewAuthorization(resourceServer);
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
            this.auth.realm().requireViewAuthorization(resourceServer);
        }

        if (policy == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        List<Policy> policies = authorization.getStoreFactory().getPolicyStore().findDependentPolicies(resourceServer, policy.getId());

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
            this.auth.realm().requireViewAuthorization(resourceServer);
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
            this.auth.realm().requireViewAuthorization(resourceServer);
        }

        if (policy == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        KeycloakSession session = authorization.getKeycloakSession();

        return Response.ok(policy.getResources().stream().map(resource -> {
            ResourceRepresentation representation = new ResourceRepresentation();

            representation.setId(resource.getId());
            representation.setName(resource.getName());
            representation.setDisplayName(AdminPermissionsSchema.SCHEMA.getResourceName(session, policy, resource));

            return representation;
        }).collect(Collectors.toList())).build();
    }

    @Path("/associatedPolicies")
    @GET
    @Produces("application/json")
    @NoCache
    public Response getAssociatedPolicies() {
        if (auth != null) {
            this.auth.realm().requireViewAuthorization(resourceServer);
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
