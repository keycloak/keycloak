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

package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public interface AuthenticationManagementResource {

    @GET
    @Path("/form-providers")
    @Produces(MediaType.APPLICATION_JSON)
    List<Map<String, Object>> getFormProviders();

    @Path("/authenticator-providers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<Map<String, Object>> getAuthenticatorProviders();

    @Path("/client-authenticator-providers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<Map<String, Object>> getClientAuthenticatorProviders();

    @Path("/form-action-providers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<Map<String, Object>> getFormActionProviders();

    @Path("/flows")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<AuthenticationFlowRepresentation> getFlows();

    @Path("/flows")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response createFlow(AuthenticationFlowRepresentation model);

    @Path("/flows/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    AuthenticationFlowRepresentation getFlow(@PathParam("id") String id);

    @Path("/flows/{id}")
    @DELETE
    void deleteFlow(@PathParam("id") String id);

    @Path("/flows/{flowAlias}/copy")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response copy(@PathParam("flowAlias") String flowAlias, Map<String, String> data);

    @Path("/flows/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void updateFlow(@PathParam("id") String id, AuthenticationFlowRepresentation flow);

    @Path("/flows/{flowAlias}/executions/flow")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    void addExecutionFlow(@PathParam("flowAlias") String flowAlias, Map<String, String> data);

    @Path("/flows/{flowAlias}/executions/execution")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    void addExecution(@PathParam("flowAlias") String flowAlias, Map<String, String> data);

    @Path("/flows/{flowAlias}/executions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<AuthenticationExecutionInfoRepresentation> getExecutions(@PathParam("flowAlias") String flowAlias);

    @Path("/flows/{flowAlias}/executions")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void updateExecutions(@PathParam("flowAlias") String flowAlias, AuthenticationExecutionInfoRepresentation rep);

    @Path("/executions")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response addExecution(AuthenticationExecutionRepresentation model);

    @Path("/executions/{executionId}")
	@GET
    @Produces(MediaType.APPLICATION_JSON)
    AuthenticationExecutionRepresentation getExecution(final @PathParam("executionId") String executionId);

    @Path("/executions/{executionId}/raise-priority")
    @POST
    void raisePriority(@PathParam("executionId") String execution);

    @Path("/executions/{executionId}/lower-priority")
    @POST
    void lowerPriority(@PathParam("executionId") String execution);

    @Path("/executions/{executionId}")
    @DELETE
    void removeExecution(@PathParam("executionId") String execution);

    @Path("/executions/{executionId}/config")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response newExecutionConfig(@PathParam("executionId") String executionId, AuthenticatorConfigRepresentation config);

    @Path("unregistered-required-actions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<RequiredActionProviderSimpleRepresentation> getUnregisteredRequiredActions();

    @Path("register-required-action")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    void registerRequiredAction(RequiredActionProviderSimpleRepresentation action);

    @Path("required-actions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<RequiredActionProviderRepresentation> getRequiredActions();

    @Path("required-actions/{alias}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    RequiredActionProviderRepresentation getRequiredAction(@PathParam("alias") String alias);

    @Path("required-actions/{alias}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void updateRequiredAction(@PathParam("alias") String alias, RequiredActionProviderRepresentation rep);

    @Path("required-actions/{alias}")
    @DELETE
    void removeRequiredAction(@PathParam("alias") String alias);

    @Path("required-actions/{alias}/raise-priority")
    @POST
    void raiseRequiredActionPriority(@PathParam("alias") String alias);

    @Path("required-actions/{alias}/lower-priority")
    @POST
    void lowerRequiredActionPriority(@PathParam("alias") String alias);

    @Path("config-description/{providerId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    AuthenticatorConfigInfoRepresentation getAuthenticatorConfigDescription(@PathParam("providerId") String providerId);

    @Path("per-client-config-description")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, List<ConfigPropertyRepresentation>> getPerClientConfigDescription();

    @Path("config/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    AuthenticatorConfigRepresentation getAuthenticatorConfig(@PathParam("id") String id);

    @Path("config/{id}")
    @DELETE
    void removeAuthenticatorConfig(@PathParam("id") String id);

    @Path("config/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void updateAuthenticatorConfig(@PathParam("id") String id, AuthenticatorConfigRepresentation config);
}
