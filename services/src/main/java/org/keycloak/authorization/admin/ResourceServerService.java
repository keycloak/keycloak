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

import java.util.HashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.exportimport.util.ExportUtils;
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
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import java.util.Collections;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
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
        this.auth.realm().requireManageAuthorization();

        UserModel serviceAccount = this.session.users().getServiceAccount(client);

        if (serviceAccount == null) {
            throw new RuntimeException("Client does not have a service account.");
        }

        if (this.resourceServer == null) {
            this.resourceServer = RepresentationToModel.createResourceServer(client, session, true);
            createDefaultPermission(createDefaultResource(), createDefaultPolicy());
            audit(OperationType.CREATE, session.getContext().getUri(), newClient);
        }

        return resourceServer;
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response update(ResourceServerRepresentation server) {
        this.auth.realm().requireManageAuthorization();
        this.resourceServer.setAllowRemoteResourceManagement(server.isAllowRemoteResourceManagement());
        this.resourceServer.setPolicyEnforcementMode(server.getPolicyEnforcementMode());
        this.resourceServer.setDecisionStrategy(server.getDecisionStrategy());
        audit(OperationType.UPDATE, session.getContext().getUri(), false);
        return Response.noContent().build();
    }

    public void delete() {
        this.auth.realm().requireManageAuthorization();
        authorization.getStoreFactory().getResourceServerStore().delete(resourceServer.getId());
        audit(OperationType.DELETE, session.getContext().getUri(), false);
    }

    @GET
    @Produces("application/json")
    public Response findById() {
        this.auth.realm().requireViewAuthorization();
        return Response.ok(toRepresentation(this.resourceServer, this.client)).build();
    }

    @Path("/settings")
    @GET
    @Produces("application/json")
    public Response exportSettings() {
        this.auth.realm().requireManageAuthorization();
        return Response.ok(ExportUtils.exportAuthorizationSettings(session, client)).build();
    }

    @Path("/import")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importSettings(ResourceServerRepresentation rep) {
        this.auth.realm().requireManageAuthorization();

        rep.setClientId(client.getId());

        RepresentationToModel.toModel(rep, authorization);

        audit(OperationType.UPDATE, session.getContext().getUri(), false);

        return Response.noContent().build();
    }

    @Path("/resource")
    public ResourceSetService getResourceSetResource() {
        ResourceSetService resource = new ResourceSetService(this.session, this.resourceServer, this.authorization, this.auth, adminEvent);

        ResteasyProviderFactory.getInstance().injectProperties(resource);

        return resource;
    }

    @Path("/scope")
    public ScopeService getScopeResource() {
        ScopeService resource = new ScopeService(this.session, this.resourceServer, this.authorization, this.auth, adminEvent);

        ResteasyProviderFactory.getInstance().injectProperties(resource);

        return resource;
    }

    @Path("/policy")
    public PolicyService getPolicyResource() {
        PolicyService resource = new PolicyService(this.resourceServer, this.authorization, this.auth, adminEvent);

        ResteasyProviderFactory.getInstance().injectProperties(resource);

        return resource;
    }

    @Path("/permission")
    public Object getPermissionTypeResource() {
        this.auth.realm().requireViewAuthorization();
        PermissionService resource = new PermissionService(this.resourceServer, this.authorization, this.auth, adminEvent);

        ResteasyProviderFactory.getInstance().injectProperties(resource);

        return resource;
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

    private void audit(OperationType operation, UriInfo uriInfo, boolean newClient) {
        if (newClient) {
            adminEvent.resource(ResourceType.AUTHORIZATION_RESOURCE_SERVER).operation(operation).resourcePath(uriInfo, client.getId())
                    .representation(ModelToRepresentation.toRepresentation(resourceServer, client)).success();
        } else {
            adminEvent.resource(ResourceType.AUTHORIZATION_RESOURCE_SERVER).operation(operation).resourcePath(uriInfo)
                    .representation(ModelToRepresentation.toRepresentation(resourceServer, client)).success();
        }
    }
}
