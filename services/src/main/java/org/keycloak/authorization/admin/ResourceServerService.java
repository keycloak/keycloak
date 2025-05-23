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

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;

import java.util.Collections;
import java.util.HashMap;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class ResourceServerService {

    private final AuthorizationProvider authorization;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final KeycloakSession session;
    private ResourceServer resourceServer;
    private final ClientModel client;

    public ResourceServerService(AuthorizationProvider authorization, ResourceServer resourceServer, ClientModel client, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.authorization = authorization;
        this.session = authorization.getKeycloakSession();
        this.client = client;
        this.resourceServer = resourceServer;
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    public ResourceServer create(boolean newClient) {
        AdminPermissionsSchema.SCHEMA.throwExceptionIfAdminPermissionClient(session, client.getId());

        this.auth.realm().requireManageAuthorization(resourceServer);

        UserModel serviceAccount = this.session.users().getServiceAccount(client);

        if (serviceAccount == null) {
            throw new RuntimeException("Client does not have a service account.");
        }

        if (this.resourceServer == null) {
            this.resourceServer = RepresentationToModel.createResourceServer(client, session, true);
            createDefaultPermission(createDefaultResource(), createDefaultPolicy());
            audit(ModelToRepresentation.toRepresentation(resourceServer, client), OperationType.CREATE, session.getContext().getUri(), newClient);
        }

        return resourceServer;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "204", description = "No Content")
    public Response update(ResourceServerRepresentation server) {
        AdminPermissionsSchema.SCHEMA.throwExceptionIfAdminPermissionClient(session, client.getId());

        this.auth.realm().requireManageAuthorization(resourceServer);
        this.resourceServer.setAllowRemoteResourceManagement(server.isAllowRemoteResourceManagement());
        this.resourceServer.setPolicyEnforcementMode(server.getPolicyEnforcementMode());
        this.resourceServer.setDecisionStrategy(server.getDecisionStrategy());
        audit(ModelToRepresentation.toRepresentation(resourceServer, client), OperationType.UPDATE, session.getContext().getUri(), false);
        return Response.noContent().build();
    }

    public void delete() {
        AdminPermissionsSchema.SCHEMA.throwExceptionIfAdminPermissionClient(session, client.getId());

        this.auth.realm().requireManageAuthorization(resourceServer);
        //need to create representation before the object is deleted to be able to get lazy loaded fields
        ResourceServerRepresentation rep = ModelToRepresentation.toRepresentation(resourceServer, client);
        authorization.getStoreFactory().getResourceServerStore().delete(client);
        audit(rep, OperationType.DELETE, session.getContext().getUri(), false);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResourceServerRepresentation findById() {
        this.auth.realm().requireViewAuthorization(resourceServer);
        return toRepresentation(this.resourceServer, this.client);
    }

    @Path("/settings")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResourceServerRepresentation exportSettings() {
        AdminPermissionsSchema.SCHEMA.throwExceptionIfAdminPermissionClient(session, client.getId());
        this.auth.realm().requireManageAuthorization(resourceServer);
        return ModelToRepresentation.toResourceServerRepresentation(session, client);
    }

    @Path("/import")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "204", description = "No Content")
    public Response importSettings(ResourceServerRepresentation rep) {
        AdminPermissionsSchema.SCHEMA.throwExceptionIfAdminPermissionClient(session, client.getId());
        this.auth.realm().requireManageAuthorization(resourceServer);

        rep.setClientId(client.getId());

        resourceServer = RepresentationToModel.toModel(rep, authorization, client);

        audit(ModelToRepresentation.toRepresentation(resourceServer, client), OperationType.UPDATE, session.getContext().getUri(), false);

        return Response.noContent().build();
    }

    @Path("/resource")
    public ResourceSetService getResourceSetResource() {
        return new ResourceSetService(this.session, this.resourceServer, this.authorization, this.auth, adminEvent);
    }

    @Path("/scope")
    public ScopeService getScopeResource() {
        return new ScopeService(this.session, this.resourceServer, this.authorization, this.auth, adminEvent);
    }

    @Path("/policy")
    public PolicyService getPolicyResource() {
        return new PolicyService(this.resourceServer, this.authorization, this.auth, adminEvent);
    }

    @Path("/permission")
    public PermissionService getPermissionTypeResource() {
        this.auth.realm().requireViewAuthorization(resourceServer);
        return new PermissionService(this.resourceServer, this.authorization, this.auth, adminEvent);
    }

    private void createDefaultPermission(ResourceRepresentation resource, PolicyRepresentation policy) {
        ResourcePermissionRepresentation defaultPermission = new ResourcePermissionRepresentation();

        defaultPermission.setName("Default Permission");
        defaultPermission.setDescription("A permission that applies to the default resource type");
        defaultPermission.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        defaultPermission.setLogic(Logic.POSITIVE);

        defaultPermission.setResourceType(resource.getType());
        defaultPermission.addPolicy(policy.getName());

        getPolicyResource().create(defaultPermission);
    }

    private PolicyRepresentation createDefaultPolicy() {
        PolicyRepresentation defaultPolicy = new PolicyRepresentation();

        defaultPolicy.setName("Default Policy");
        defaultPolicy.setDescription("A policy that grants access only for users within this realm");
        defaultPolicy.setType("js");
        defaultPolicy.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        defaultPolicy.setLogic(Logic.POSITIVE);

        HashMap<String, String> defaultPolicyConfig = new HashMap<>();

        defaultPolicyConfig.put("code", "// by default, grants any permission associated with this policy\n$evaluation.grant();\n");

        defaultPolicy.setConfig(defaultPolicyConfig);
        
        session.setAttribute("ALLOW_CREATE_POLICY", true);

        getPolicyResource().create(defaultPolicy);

        return defaultPolicy;
    }

    private ResourceRepresentation createDefaultResource() {
        ResourceRepresentation defaultResource = new ResourceRepresentation();

        defaultResource.setName("Default Resource");
        defaultResource.setUris(Collections.singleton("/*"));
        defaultResource.setType("urn:" + this.client.getClientId() + ":resources:default");

        getResourceSetResource().create(defaultResource);
        return defaultResource;
    }

    private void audit(ResourceServerRepresentation rep, OperationType operation, UriInfo uriInfo, boolean newClient) {
        if (newClient) {
            adminEvent.resource(ResourceType.AUTHORIZATION_RESOURCE_SERVER).operation(operation).resourcePath(uriInfo, client.getId())
                    .representation(rep).success();
        } else {
            adminEvent.resource(ResourceType.AUTHORIZATION_RESOURCE_SERVER).operation(operation).resourcePath(uriInfo)
                    .representation(rep).success();
        }
    }
}
