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

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.RequiredActionConfigInfoRepresentation;
import org.keycloak.representations.idm.RequiredActionConfigRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;

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
    Response copy(@PathParam("flowAlias") String flowAlias, Map<String, Object> data);

    @Path("/flows/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void updateFlow(@PathParam("id") String id, AuthenticationFlowRepresentation flow);

    @Path("/flows/{flowAlias}/executions/flow")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    void addExecutionFlow(@PathParam("flowAlias") String flowAlias, Map<String, Object> data);

    @Path("/flows/{flowAlias}/executions/execution")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    void addExecution(@PathParam("flowAlias") String flowAlias, Map<String, Object> data);

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

    /**
     * Returns configuration description of the specified required action
     *
     * @since Keycloak server 25
     * @param alias Alias of the required action, which configuration description will be returned
     * @return Configuration description of the required action
     * @throws jakarta.ws.rs.NotFoundException if the required action of specified alias is not found
     */
    @Path("required-actions/{alias}/config-description")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    RequiredActionConfigInfoRepresentation getRequiredActionConfigDescription(@PathParam("alias") String alias);

    /**
     * Returns configuration of the specified required action
     *
     * @since Keycloak server 25
     * @param alias Alias of the required action, which configuration will be returned
     * @return Configuration of the required action
     * @throws jakarta.ws.rs.BadRequestException if required action not configurable
     * @throws jakarta.ws.rs.NotFoundException if the required action configuration of specified alias is not found
     */
    @Path("required-actions/{alias}/config")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    RequiredActionConfigRepresentation getRequiredActionConfig(@PathParam("alias") String alias);

    /**
     * Delete configuration of the specified required action
     *
     * @since Keycloak server 25
     * @param alias Alias of the required action, which will be removed
     * @throws jakarta.ws.rs.BadRequestException if required action not configurable
     * @throws jakarta.ws.rs.NotFoundException if the required action configuration of specified alias is not found
     */
    @Path("required-actions/{alias}/config")
    @DELETE
    void removeRequiredActionConfig(@PathParam("alias") String alias);

    /**
     * Update configuration of the required action
     *
     * @since Keycloak server 25
     * @param alias Alias of the required action, which will be updated
     * @param rep JSON representation of the required action
     * @throws jakarta.ws.rs.BadRequestException if required action not configurable or given configuration is incorrect
     * @throws jakarta.ws.rs.NotFoundException if the required action configuration of specified alias is not found
     */
    @Path("required-actions/{alias}/config")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void updateRequiredActionConfig(@PathParam("alias") String alias, RequiredActionConfigRepresentation rep);

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
