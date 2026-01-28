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
package org.keycloak.services.resources.admin;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.authentication.ClientAuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormAuthenticator;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.deployment.DeployedConfigurationsManager;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.utils.Base32;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RequiredActionConfigInfoRepresentation;
import org.keycloak.representations.idm.RequiredActionConfigRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.userprofile.ValidationException;
import org.keycloak.utils.CredentialHelper;
import org.keycloak.utils.RequiredActionHelper;
import org.keycloak.utils.ReservedCharValidator;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

/**
 * @resource Authentication Management
 * @author Bill Burke
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class AuthenticationManagementResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    protected static final Logger logger = Logger.getLogger(AuthenticationManagementResource.class);

    public AuthenticationManagementResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.AUTH_FLOW);
    }

    /**
     * Get form providers
     *
     * Returns a stream of form providers.
     * @return
     */
    @Path("/form-providers")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation(summary = "Get form providers Returns a stream of form providers.")
    public Stream<Map<String, Object>> getFormProviders() {
        auth.realm().requireViewRealm();

        return buildProviderMetadata(session.getKeycloakSessionFactory().getProviderFactoriesStream(FormAuthenticator.class));
    }

    /**
     * Get authenticator providers
     *
     * Returns a stream of authenticator providers.
     * @return
     */
    @Path("/authenticator-providers")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Get authenticator providers Returns a stream of authenticator providers.")
    public Stream<Map<String, Object>> getAuthenticatorProviders() {
        auth.realm().requireViewRealm();

        return buildProviderMetadata(session.getKeycloakSessionFactory().getProviderFactoriesStream(Authenticator.class));
    }

    /**
     * Get client authenticator providers
     *
     * Returns a stream of client authenticator providers.
     * @return
     */
    @Path("/client-authenticator-providers")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Get client authenticator providers Returns a stream of client authenticator providers.")
    public Stream<Map<String, Object>> getClientAuthenticatorProviders() {
        auth.realm().requireViewClientAuthenticatorProviders();
        Stream<ProviderFactory> factories =  session.getKeycloakSessionFactory().getProviderFactoriesStream(ClientAuthenticator.class);

        return factories.map(factory -> {
            Map<String, Object> data = new HashMap<>();
            buildProviderMetadataHelper(data, factory);
            data.put("supportsSecret", ((ClientAuthenticatorFactory) factory).supportsSecret());
            return data;
        });
    }

    private void buildProviderMetadataHelper(Map<String, Object> data, ProviderFactory factory) {
        data.put("id", factory.getId());
        ConfigurableAuthenticatorFactory configured = (ConfigurableAuthenticatorFactory) factory;
        data.put("description", configured.getHelpText());
        data.put("displayName", configured.getDisplayType());
    }

    public Stream<Map<String, Object>> buildProviderMetadata(Stream<ProviderFactory> factories) {
        return factories.map(factory -> {
            Map<String, Object> data = new HashMap<>();
            buildProviderMetadataHelper(data, factory);
            return data;
        });
    }

    /**
     * Get form action providers
     *
     * Returns a stream of form action providers.
     * @return
     */
    @Path("/form-action-providers")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Get form action providers Returns a stream of form action providers."
    )
    public Stream<Map<String, Object>> getFormActionProviders() {
        auth.realm().requireViewRealm();

        return buildProviderMetadata(session.getKeycloakSessionFactory().getProviderFactoriesStream(FormAction.class));
    }


    /**
     * Get authentication flows
     *
     * Returns a stream of authentication flows.
     * @return
     */
    @Path("/flows")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Get authentication flows Returns a stream of authentication flows.")
    public Stream<AuthenticationFlowRepresentation> getFlows() {
        auth.realm().requireViewAuthenticationFlows();

        return realm.getAuthenticationFlowsStream()
                .filter(flow -> flow.isTopLevel() && !Objects.equals(flow.getAlias(), DefaultAuthenticationFlows.SAML_ECP_FLOW))
                .map(flow -> ModelToRepresentation.toRepresentation(session, realm, flow));
    }

    /**
     * Create a new authentication flow
     *
     * @param flow Authentication flow representation
     * @return
     */
    @Path("/flows")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Create a new authentication flow")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Created"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response createFlow(@Parameter( description = "Authentication flow representation") AuthenticationFlowRepresentation flow) {
        auth.realm().requireManageRealm();

        if (flow.getAlias() == null || flow.getAlias().isEmpty()) {
            throw ErrorResponse.exists("Failed to create flow with empty alias name");
        }

        if (realm.getFlowByAlias(flow.getAlias()) != null) {
            throw ErrorResponse.exists("Flow " + flow.getAlias() + " already exists");
        }

        //adding an empty string to avoid NPE
        if(Objects.isNull(flow.getDescription())) {
            flow.setDescription("");
        }

        ReservedCharValidator.validate(flow.getAlias());

        AuthenticationFlowModel createdModel = realm.addAuthenticationFlow(RepresentationToModel.toModel(flow));

        flow.setId(createdModel.getId());
        adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), createdModel.getId()).representation(flow).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(flow.getId()).build()).build();
    }

    /**
     * Get authentication flow for id
     *
     * @param id Flow id
     * @return
     */
    @Path("/flows/{id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Get authentication flow for id")
    public AuthenticationFlowRepresentation getFlow(@Parameter(description = "Flow id") @PathParam("id") String id) {
        auth.realm().requireViewRealm();

        AuthenticationFlowModel flow = realm.getAuthenticationFlowById(id);
        if (flow == null) {
            throw new NotFoundException("Could not find flow with id");
        }
        return ModelToRepresentation.toRepresentation(session, realm, flow);
    }

    /**
     * Update an authentication flow
     *
     * @param id The flow id
     * @param flow Authentication flow representation
     * @return
     */
    @Path("/flows/{id}")
    @PUT
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Update an authentication flow")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public void updateFlow(@PathParam("id") String id, AuthenticationFlowRepresentation flow) {
        auth.realm().requireManageRealm();

        AuthenticationFlowRepresentation existingFlow = getFlow(id);

        if (flow.getAlias() == null || flow.getAlias().isEmpty()) {
            throw ErrorResponse.exists("Failed to update flow with empty alias name");
        }

        ReservedCharValidator.validate(flow.getAlias());

        //check if updating a correct flow
        AuthenticationFlowModel checkFlow = realm.getAuthenticationFlowById(id);
        if (checkFlow == null) {
            session.getTransactionManager().setRollbackOnly();
            throw new NotFoundException("Illegal execution");
        }

        //if a different flow with the same name does already exist, throw an exception
        if (realm.getFlowByAlias(flow.getAlias()) != null && !checkFlow.getAlias().equals(flow.getAlias())) {
            throw ErrorResponse.exists("Flow alias name already exists");
        }

        //if the name changed
        if (checkFlow.getAlias() != null && !checkFlow.getAlias().equals(flow.getAlias())) {
            checkFlow.setAlias(flow.getAlias());
        } else if (checkFlow.getAlias() == null && flow.getAlias() != null) {
            checkFlow.setAlias(flow.getAlias());
	}

        //check if the description changed
        if (checkFlow.getDescription() != null && !checkFlow.getDescription().equals(flow.getDescription())) {
            checkFlow.setDescription(flow.getDescription());
        } else if (checkFlow.getDescription() == null && flow.getDescription() != null) {
            checkFlow.setDescription(flow.getDescription());
	}

        //update the flow
        flow.setId(existingFlow.getId());
        realm.updateAuthenticationFlow(RepresentationToModel.toModel(flow));
        adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(flow).success();
    }

    /**
     * Delete an authentication flow
     *
     * @param id Flow id
     */
    @Path("/flows/{id}")
    @DELETE
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Delete an authentication flow")
    @APIResponse(responseCode = "204", description = "No Content")
    public void deleteFlow(@Parameter(description = "Flow id") @PathParam("id") String id) {
        auth.realm().requireManageRealm();

        AuthenticationFlowModel flow = realm.getAuthenticationFlowById(id);
        if (flow == null) {
            throw new NotFoundException("Flow not found");
        }

        KeycloakModelUtils.deepDeleteAuthenticationFlow(session, realm, flow,
                () -> {}, // allow deleting even with missing references
                () -> {
                    throw new BadRequestException("Can't delete built in flow");
                }
        );

        // Use just one event for top-level flow. Using separate events won't work properly for flows of depth 2 or bigger
        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Copy existing authentication flow under a new name
     *
     * The new name is given as 'newName' attribute of the passed JSON object
     *
     * @param flowAlias Name of the existing authentication flow
     * @param data JSON containing 'newName' attribute
     * @return
     */
    @Path("/flows/{flowAlias}/copy")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Copy existing authentication flow under a new name The new name is given as 'newName' attribute of the passed JSON object")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Created"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response copy(@Parameter(description="name of the existing authentication flow") @PathParam("flowAlias") String flowAlias, Map<String, String> data) {
        auth.realm().requireManageRealm();

        String newName = data.get("newName");
        if (realm.getFlowByAlias(newName) != null) {
            throw ErrorResponse.exists("New flow alias name already exists");
        }

        AuthenticationFlowModel flow = realm.getFlowByAlias(flowAlias);
        if (flow == null) {
            logger.debugf("flow not found: %s", flowAlias);
            throw new NotFoundException("Flow not found");
        }

        AuthenticationFlowModel copy = copyFlow(session, realm, flow, newName);

        data.put("id", copy.getId());
        adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri()).representation(data).success();

        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(copy.getId()).build()).build();
    }

    public static AuthenticationFlowModel copyFlow(KeycloakSession session, RealmModel realm, AuthenticationFlowModel flow, String newName) {
        AuthenticationFlowModel copy = new AuthenticationFlowModel();
        copy.setAlias(newName);
        copy.setDescription(flow.getDescription());
        copy.setProviderId(flow.getProviderId());
        copy.setBuiltIn(false);
        copy.setTopLevel(flow.isTopLevel());
        copy = realm.addAuthenticationFlow(copy);
        copy(session, realm, newName, flow, copy);
        return copy;
    }

    public static void copy(KeycloakSession session, RealmModel realm, String newName, AuthenticationFlowModel from, AuthenticationFlowModel to) {
        realm.getAuthenticationExecutionsStream(from.getId()).forEachOrdered(execution -> {
            if (execution.isAuthenticatorFlow()) {
                AuthenticationFlowModel subFlow = realm.getAuthenticationFlowById(execution.getFlowId());
                AuthenticationFlowModel copy = new AuthenticationFlowModel();
                copy.setAlias(newName + " " + subFlow.getAlias());
                copy.setDescription(subFlow.getDescription());
                copy.setProviderId(subFlow.getProviderId());
                copy.setBuiltIn(false);
                copy.setTopLevel(false);
                copy = realm.addAuthenticationFlow(copy);
                execution.setFlowId(copy.getId());
                copy(session, realm, newName, subFlow, copy);
            }

            if (execution.getAuthenticatorConfig() != null) {
                DeployedConfigurationsManager configManager = new DeployedConfigurationsManager(session);
                AuthenticatorConfigModel config = configManager.getAuthenticatorConfig(realm, execution.getAuthenticatorConfig());

                if (config == null) {
                    logger.debugf("Authentication execution configuration with id [%s] not found", execution.getAuthenticatorConfig());
                    throw new IllegalStateException("Authentication execution configuration not found");
                }

                if (configManager.getDeployedAuthenticatorConfig(execution.getAuthenticatorConfig()) != null) {
                    // Shared configuration of deployed provider
                    execution.setAuthenticatorConfig(config.getId());
                } else {
                    config.setId(null);

                    if (config.getAlias() != null) {
                        config.setAlias(newName + " " + config.getAlias());
                        if (configManager.getAuthenticatorConfigByAlias(realm, config.getAlias()) != null) {
                            logger.warnf("Authentication execution configuration [%s] already exists", config.getAlias());
                            throw new IllegalStateException("Authentication execution configuration " + config.getAlias() + " already exists.");
                        }
                    }

                    AuthenticatorConfigModel newConfig = realm.addAuthenticatorConfig(config);

                    execution.setAuthenticatorConfig(newConfig.getId());
                }
            }

            execution.setId(null);
            execution.setParentFlow(to.getId());
            realm.addAuthenticatorExecution(execution);
        });
    }

    /**
     * Add new flow with new execution to existing flow.
     *
     * This method creates two entities under the covers. Firstly the new authentication flow entity, which will be a subflow of existing parent flow, which was referenced by the path of this endpoint.
     * Secondly the authentication execution entity, which will be added under the parent flow. This execution entity provides the binding between the parent flow and the new subflow. The new execution
     * contains the "parentFlow" with the ID of the parent flow and the "flowId" with the ID of the newly created flow
     *
     * @param flowAlias Alias of parent authentication flow
     * @param data New authentication flow / execution JSON data containing 'alias', 'type', 'provider', 'priority', and 'description' attributes
     * @return The response with the "Location" header pointing to the newly created flow (not the newly created execution!)
     */
    @Path("/flows/{flowAlias}/executions/flow")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Add new flow with new execution to existing flow")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Created"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response addExecutionFlow(@Parameter(description = "Alias of parent authentication flow") @PathParam("flowAlias") String flowAlias, @Parameter(description = "New authentication flow / execution JSON data containing 'alias', 'type', 'provider', 'priority', and 'description' attributes") Map<String, Object> data) {
        auth.realm().requireManageRealm();

        AuthenticationFlowModel parentFlow = realm.getFlowByAlias(flowAlias);
        if (parentFlow == null) {
            throw ErrorResponse.error("Parent flow doesn't exist", Response.Status.BAD_REQUEST);
        }
        if (parentFlow.isBuiltIn()) {
            throw new BadRequestException("It is illegal to add sub-flow to a built in flow");
        }
        String alias = (String) data.get("alias");
        String type = (String) data.get("type");
        String provider = (String) data.get("provider");
        int priority = data.containsKey("priority") ? (Integer) data.get("priority") : getNextPriority(parentFlow);

        //Make sure that the description to avoid NullPointerException
        String description = Objects.isNull(data.get("description")) ? "" : (String) data.get("description");


        AuthenticationFlowModel newFlow = realm.getFlowByAlias(alias);
        if (newFlow != null) {
            throw ErrorResponse.exists("New flow alias name already exists");
        }
        newFlow = new AuthenticationFlowModel();
        newFlow.setAlias(alias);
        newFlow.setDescription(description);
        newFlow.setProviderId(type);
        newFlow = realm.addAuthenticationFlow(newFlow);
        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(parentFlow.getId());
        execution.setFlowId(newFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);
        execution.setAuthenticatorFlow(true);
        if (type.equals("form-flow")) {
            execution.setAuthenticator(provider);
        }
        execution.setPriority(priority);
        execution = realm.addAuthenticatorExecution(execution);

        data.put("id", execution.getId());
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.AUTH_EXECUTION_FLOW).resourcePath(session.getContext().getUri()).representation(data).success();

        String addExecutionPathSegment = UriBuilder.fromMethod(AuthenticationManagementResource.class, "addExecutionFlow").build(parentFlow.getAlias()).getPath();
        return Response.created(session.getContext().getUri().getBaseUriBuilder().path(session.getContext().getUri().getPath().replace(addExecutionPathSegment, "")).path("flows").path(newFlow.getId()).build()).build();
    }

    private int getNextPriority(AuthenticationFlowModel parentFlow) {
        List<AuthenticationExecutionModel> executions = realm.getAuthenticationExecutionsStream(parentFlow.getId())
                .collect(Collectors.toList());
        return executions.isEmpty() ? 0 : executions.get(executions.size() - 1).getPriority() + 1;
    }

    /**
     * Add new authentication execution to a flow
     *
     * @param flowAlias Alias of parent flow
     * @param data New execution JSON data containing 'provider' and 'priority' (optional) attribute
     * @return
     */
    @Path("/flows/{flowAlias}/executions/execution")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary="Add new authentication execution to a flow")
    @APIResponse(responseCode = "201", description = "Created")
    public Response addExecutionToFlow(@Parameter(description = "Alias of parent flow") @PathParam("flowAlias") String flowAlias, @Parameter(description = "New execution JSON data containing 'provider' and 'priority' (optional) attribute") Map<String, Object> data) {
        auth.realm().requireManageRealm();

        AuthenticationFlowModel parentFlow = realm.getFlowByAlias(flowAlias);
        if (parentFlow == null) {
            throw new BadRequestException("Parent flow doesn't exist");
        }
        if (parentFlow.isBuiltIn()) {
            throw new BadRequestException("It is illegal to add execution to a built in flow");
        }
        String provider = (String) data.get("provider");
        int priority = data.containsKey("priority") ? (Integer) data.get("priority") : getNextPriority(parentFlow);

        // make sure provider is one of the registered providers
        ProviderFactory f = getProviderFactory( parentFlow, provider);

        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(parentFlow.getId());

        ConfigurableAuthenticatorFactory conf = (ConfigurableAuthenticatorFactory) f;
        if (conf.getRequirementChoices().length == 1)
            execution.setRequirement(conf.getRequirementChoices()[0]);
        else
            execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);

        execution.setAuthenticatorFlow(false);
        execution.setAuthenticator(provider);
        execution.setPriority(priority);

        execution = realm.addAuthenticatorExecution(execution);

        checkConfigForDeployedProvider(f, execution);

        data.put("id", execution.getId());
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.AUTH_EXECUTION).resourcePath(session.getContext().getUri()).representation(data).success();

        String addExecutionPathSegment = UriBuilder.fromMethod(AuthenticationManagementResource.class, "addExecutionToFlow").build(parentFlow.getAlias()).getPath();
        return Response.created(session.getContext().getUri().getBaseUriBuilder().path(session.getContext().getUri().getPath().replace(addExecutionPathSegment, "")).path("executions").path(execution.getId()).build()).build();
    }

    private ProviderFactory getProviderFactory(AuthenticationFlowModel parentFlow, String provider) {
        ProviderFactory f = null;
        if (parentFlow.getProviderId().equals(AuthenticationFlow.CLIENT_FLOW)) {
            f = session.getKeycloakSessionFactory().getProviderFactory(ClientAuthenticator.class, provider);
        } else if (parentFlow.getProviderId().equals(AuthenticationFlow.FORM_FLOW)) {
            f = session.getKeycloakSessionFactory().getProviderFactory(FormAction.class, provider);
        } else {
            f = session.getKeycloakSessionFactory().getProviderFactory(Authenticator.class, provider);
        }
        if (f == null) {
            throw new BadRequestException("No authentication provider found for id: " + provider);
        }
        return f;
    }


    private void checkConfigForDeployedProvider(ProviderFactory f, AuthenticationExecutionModel execution) {
        if (f instanceof ConfiguredProvider) {
            ConfiguredProvider internalProviderFactory = (ConfiguredProvider) f;
            AuthenticatorConfigModel config = internalProviderFactory.getConfig();

            if (config != null) {
                // use a default configuration if the factory defines one
                // Assumption is that this is registered in DeployedConfigurationsProvider
                // useful for internal providers that already provide a built-in configuration
                logger.tracef("Updating execution of provider '%s' with shared configuration.", execution.getAuthenticator());
                execution.setAuthenticatorConfig(config.getId());
                realm.updateAuthenticatorExecution(execution);
            }
        }
    }

    /**
     * Get authentication executions for a flow
     *
     * @param flowAlias Flow alias
     * @return The list of executions
     */
    @Path("/flows/{flowAlias}/executions")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Get authentication executions for a flow")
    public List<AuthenticationExecutionInfoRepresentation> getExecutions(@Parameter(description = "Flow alias") @PathParam("flowAlias") String flowAlias) {
        auth.realm().requireViewRealm();

        AuthenticationFlowModel flow = realm.getFlowByAlias(flowAlias);
        if (flow == null) {
            logger.debugf("flow not found: %s", flowAlias);
            throw new NotFoundException("Flow not found");
        }
        List<AuthenticationExecutionInfoRepresentation> result = new LinkedList<>();

        int level = 0;

        recurseExecutions(flow, result, level);
        return result;
    }

    private String getAuthenticationConfig(String flowAlias, AuthenticationExecutionModel model) {
        if (model.getAuthenticatorConfig() == null) {
            return null;
        }
        AuthenticatorConfigModel config = new DeployedConfigurationsManager(session).getAuthenticatorConfig(realm, model.getAuthenticatorConfig());
        if (config == null) {
            logger.warnf("Authenticator configuration '%s' is missing for execution '%s' (%s) in flow '%s'",
                    model.getAuthenticatorConfig(), model.getId(), model.getAuthenticator(), flowAlias);
            return null;
        }
        return config.getId();
    }

    public void recurseExecutions(AuthenticationFlowModel flow, List<AuthenticationExecutionInfoRepresentation> result, int level) {
        AtomicInteger index = new AtomicInteger(0);
        realm.getAuthenticationExecutionsStream(flow.getId()).forEachOrdered(execution -> {
            AuthenticationExecutionInfoRepresentation rep = new AuthenticationExecutionInfoRepresentation();
            rep.setLevel(level);
            rep.setIndex(index.getAndIncrement());
            rep.setRequirementChoices(new LinkedList<>());
            rep.setPriority(execution.getPriority());
            if (execution.isAuthenticatorFlow()) {
                AuthenticationFlowModel flowRef = realm.getAuthenticationFlowById(execution.getFlowId());
                if (AuthenticationFlow.BASIC_FLOW.equals(flowRef.getProviderId())) {
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.REQUIRED.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.ALTERNATIVE.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.DISABLED.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.CONDITIONAL.name());
                } else if (AuthenticationFlow.FORM_FLOW.equals(flowRef.getProviderId())) {
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.REQUIRED.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.DISABLED.name());
                    rep.setProviderId(execution.getAuthenticator());
                    rep.setAuthenticationConfig(getAuthenticationConfig(flow.getAlias(), execution));
                } else if (AuthenticationFlow.CLIENT_FLOW.equals(flowRef.getProviderId())) {
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.ALTERNATIVE.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.REQUIRED.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.DISABLED.name());
                }
                rep.setDisplayName(flowRef.getAlias());
                rep.setDescription(flowRef.getDescription());
                rep.setConfigurable(false);
                rep.setId(execution.getId());
                rep.setAuthenticationFlow(execution.isAuthenticatorFlow());
                rep.setRequirement(execution.getRequirement().name());
                rep.setFlowId(execution.getFlowId());
                result.add(rep);
                recurseExecutions(flowRef, result, level + 1);
            } else {
                String providerId = execution.getAuthenticator();
                ConfigurableAuthenticatorFactory factory = CredentialHelper.getConfigurableAuthenticatorFactory(session, providerId);
                if (factory == null) {
                    logger.warnf("Cannot find authentication provider implementation with provider ID '%s'", providerId);
                    throw new NotFoundException("Could not find authenticator provider");
                }
                rep.setDisplayName(factory.getDisplayType());
                rep.setConfigurable(factory.isConfigurable());
                for (AuthenticationExecutionModel.Requirement choice : factory.getRequirementChoices()) {
                    rep.getRequirementChoices().add(choice.name());
                }
                rep.setId(execution.getId());

                if (factory.isConfigurable()) {
                    String authenticatorConfigId = execution.getAuthenticatorConfig();
                    if(authenticatorConfigId != null) {
                        AuthenticatorConfigModel authenticatorConfig = new DeployedConfigurationsManager(session).getAuthenticatorConfig(realm, authenticatorConfigId);

                        if (authenticatorConfig != null) {
                            rep.setAlias(authenticatorConfig.getAlias());
                        }
                    }
                }

                rep.setRequirement(execution.getRequirement().name());

                providerId = execution.getAuthenticator();

                // encode the provider id in case the provider is a script deployed to the server to make sure it can be used as path parameters without break the URL syntax
                if (providerId.startsWith("script-")) {
                    providerId = Base32.encode(providerId.getBytes());
                }

                rep.setProviderId(providerId);
                rep.setAuthenticationConfig(getAuthenticationConfig(flow.getAlias(), execution));
                result.add(rep);
            }
        });
    }

    /**
     * Update authentication executions of a Flow
     * @param flowAlias Flow alias
     * @param rep AuthenticationExecutionInfoRepresentation
     * @return
     */
    @Path("/flows/{flowAlias}/executions")
    @PUT
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Update authentication executions of a Flow")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public void updateExecutions(@Parameter(description = "Flow alias") @PathParam("flowAlias") String flowAlias, @Parameter(description = "AuthenticationExecutionInfoRepresentation") AuthenticationExecutionInfoRepresentation rep) {
        auth.realm().requireManageRealm();

        AuthenticationFlowModel flow = realm.getFlowByAlias(flowAlias);
        if (flow == null) {
            logger.debugf("flow not found: %s", flowAlias);
            throw new NotFoundException("flow not found");
        }

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(rep.getId());
        if (model == null) {
            session.getTransactionManager().setRollbackOnly();
            throw new NotFoundException("Illegal execution");

        }
        boolean updateExecution = false;
        if (model.getPriority() != rep.getPriority()) {
            model.setPriority(rep.getPriority());
            updateExecution = true;
        }
        if (!model.getRequirement().name().equals(rep.getRequirement())) {
            model.setRequirement(AuthenticationExecutionModel.Requirement.valueOf(rep.getRequirement()));
            updateExecution = true;
        }
        if (updateExecution) {
            realm.updateAuthenticatorExecution(model);
            adminEvent.operation(OperationType.UPDATE).resource(ResourceType.AUTH_EXECUTION).resourcePath(session.getContext().getUri()).representation(rep).success();
            return;
        }

        //executions can't have name and description updated
        if (rep.getAuthenticationFlow() == null) {
            return;
        }

        //check if updating a correct flow
        AuthenticationFlowModel checkFlow = realm.getAuthenticationFlowById(rep.getFlowId());
        if (checkFlow == null) {
            session.getTransactionManager().setRollbackOnly();
            throw new NotFoundException("Illegal execution");
        }

        //if a different flow with the same name does already exist, throw an exception
        if (realm.getFlowByAlias(rep.getDisplayName()) != null && !checkFlow.getAlias().equals(rep.getDisplayName())) {
            throw ErrorResponse.exists("Flow alias name already exists");
        }

        //if the name changed
        if (!checkFlow.getAlias().equals(rep.getDisplayName())) {
            checkFlow.setAlias(rep.getDisplayName());
        }

        // check if description is null and set an empty String to avoid NPE
        if (Objects.isNull(checkFlow.getDescription())) {
            checkFlow.setDescription("");
        }

        // check if the description changed
        if (!checkFlow.getDescription().equals(rep.getDescription())) {
            checkFlow.setDescription(rep.getDescription());
        }

        //update the flow
        realm.updateAuthenticationFlow(checkFlow);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.AUTH_EXECUTION).resourcePath(session.getContext().getUri()).representation(rep).success();
    }

    /**
     * Get Single Execution
     * @param executionId The execution id
     * @return
     */
    @Path("/executions/{executionId}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Get Single Execution")
    public AuthenticationExecutionRepresentation getExecution(final @PathParam("executionId") String executionId) {
    	//http://localhost:8080/auth/admin/realms/master/authentication/executions/cf26211b-9e68-4788-b754-1afd02e59d7f
        auth.realm().requireViewRealm();

        final Optional<AuthenticationExecutionModel> model = Optional.ofNullable(realm.getAuthenticationExecutionById(executionId));
        if (!model.isPresent()) {
            logger.debugf("Could not find execution by Id: %s", executionId);
            throw new NotFoundException("Illegal execution");
        }

        return ModelToRepresentation.toRepresentation(model.get());
    }

    /**
     * Add new authentication execution
     *
     * @param execution JSON model describing authentication execution
     * @return
     */
    @Path("/executions")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Add new authentication execution")
    @APIResponse(responseCode = "201", description = "Created")
    public Response addExecution(@Parameter(description = "JSON model describing authentication execution") AuthenticationExecutionRepresentation execution) {
        auth.realm().requireManageRealm();

        AuthenticationExecutionModel model = RepresentationToModel.toModel(session, realm, execution);
        AuthenticationFlowModel parentFlow = getParentFlow(model);
        if (parentFlow.isBuiltIn()) {
            throw new BadRequestException("It is illegal to add execution to a built in flow");
        }
        int priority = execution.getPriority() != null ? execution.getPriority() : getNextPriority(parentFlow);
        model.setPriority(priority);
        model = realm.addAuthenticatorExecution(model);

        if (!execution.isAuthenticatorFlow()) {
            ProviderFactory f = getProviderFactory(parentFlow, execution.getAuthenticator());
            checkConfigForDeployedProvider(f, model);
        }

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.AUTH_EXECUTION).resourcePath(session.getContext().getUri(), model.getId()).representation(execution).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(model.getId()).build()).build();
    }

    public AuthenticationFlowModel getParentFlow(AuthenticationExecutionModel model) {
        if (model.getParentFlow() == null) {
            throw new BadRequestException("parent flow not set on new execution");
        }
        AuthenticationFlowModel parentFlow = realm.getAuthenticationFlowById(model.getParentFlow());
        if (parentFlow == null) {
            throw new BadRequestException("execution parent flow does not exist");

        }
        return parentFlow;
    }


    /**
     * Raise execution's priority
     *
     * @param execution Execution id
     */
    @Path("/executions/{executionId}/raise-priority")
    @POST
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Raise execution's priority")
    @APIResponse(responseCode = "204", description = "No Content")
    public void raisePriority(@Parameter(description = "Execution id") @PathParam("executionId") String execution) {
        auth.realm().requireManageRealm();

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(execution);
        if (model == null) {
            session.getTransactionManager().setRollbackOnly();
            throw new NotFoundException("Illegal execution");

        }
        AuthenticationFlowModel parentFlow = getParentFlow(model);
        if (parentFlow.isBuiltIn()) {
            throw new BadRequestException("It is illegal to modify execution in a built in flow");
        }

        AuthenticationExecutionModel previous = null;
        for (AuthenticationExecutionModel exe : realm.getAuthenticationExecutionsStream(parentFlow.getId()).collect(Collectors.toList())) {
            if (exe.getId().equals(model.getId())) {
                break;
            }
            previous = exe;

        }
        if (previous == null) return;
        int tmp = previous.getPriority();
        previous.setPriority(model.getPriority());
        realm.updateAuthenticatorExecution(previous);
        model.setPriority(tmp);
        realm.updateAuthenticatorExecution(model);

        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.AUTH_EXECUTION).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Lower execution's priority
     *
     * @param execution Execution id
     */
    @Path("/executions/{executionId}/lower-priority")
    @POST
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Lower execution's priority")
    @APIResponse(responseCode = "204", description = "No Content")
    public void lowerPriority(@Parameter( description = "Execution id") @PathParam("executionId") String execution) {
        auth.realm().requireManageRealm();

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(execution);
        if (model == null) {
            session.getTransactionManager().setRollbackOnly();
            throw new NotFoundException("Illegal execution");

        }
        AuthenticationFlowModel parentFlow = getParentFlow(model);
        if (parentFlow.isBuiltIn()) {
            throw new BadRequestException("It is illegal to modify execution in a built in flow");
        }
        List<AuthenticationExecutionModel> executions = realm.getAuthenticationExecutionsStream(parentFlow.getId()).collect(Collectors.toList());
        int i;
        for (i = 0; i < executions.size(); i++) {
            if (executions.get(i).getId().equals(model.getId())) {
                break;
            }
        }
        if (i + 1 >= executions.size()) return;
        AuthenticationExecutionModel next = executions.get(i + 1);
        int tmp = model.getPriority();
        model.setPriority(next.getPriority());
        realm.updateAuthenticatorExecution(model);
        next.setPriority(tmp);
        realm.updateAuthenticatorExecution(next);

        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.AUTH_EXECUTION).resourcePath(session.getContext().getUri()).success();
    }


    /**
     * Delete execution
     *
     * @param execution Execution id
     */
    @Path("/executions/{executionId}")
    @DELETE
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Delete execution")
    @APIResponse(responseCode = "204", description = "No Content")
    public void removeExecution(@Parameter(description = "Execution id") @PathParam("executionId") String execution) {
        auth.realm().requireManageRealm();

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(execution);
        if (model == null) {
            session.getTransactionManager().setRollbackOnly();
            throw new NotFoundException("Illegal execution");
        }

        AuthenticationFlowModel parentFlow = getParentFlow(model);
        if (parentFlow.isBuiltIn()) {
            throw new BadRequestException("It is illegal to remove execution from a built in flow");
        }

        KeycloakModelUtils.deepDeleteAuthenticationExecutor(session, realm, model,
                () -> {}, // allow deleting even with missing references
                () -> {
                    throw new BadRequestException("It is illegal to remove execution from a built in flow");
                }
        );

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.AUTH_EXECUTION).resourcePath(session.getContext().getUri()).success();
    }


    /**
     * Update execution with new configuration
     *
     * @param execution Execution id
     * @param json JSON with new configuration
     * @return
     */
    @Path("/executions/{executionId}/config")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Update execution with new configuration")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Created"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response newExecutionConfig(@Parameter(description = "Execution id") @PathParam("executionId") String execution, @Parameter(description = "JSON with new configuration") AuthenticatorConfigRepresentation json) {
        auth.realm().requireManageRealm();

        if (json.getAlias() == null || json.getAlias().isEmpty()) {
            throw ErrorResponse.exists("Failed to create authentication execution configuration with empty alias name");
        }

        ReservedCharValidator.validate(json.getAlias());

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(execution);
        if (model == null) {
            session.getTransactionManager().setRollbackOnly();
            throw new NotFoundException("Illegal execution");
        }

        // retrieve the previous configuration if assigned
        AuthenticatorConfigModel prevConfig = null;
        if (model.getAuthenticatorConfig() != null) {
            prevConfig = realm.getAuthenticatorConfigById(model.getAuthenticatorConfig());
        }

        AuthenticatorConfigModel otherConfig = realm.getAuthenticatorConfigByAlias(json.getAlias());
        if (otherConfig != null && (prevConfig == null || !prevConfig.getId().equals(otherConfig.getId()))) {
            throw ErrorResponse.exists("Authentication execution configuration " + json.getAlias() + " already exists");
        }

        // remove the previous config
        if (prevConfig != null) {
            realm.removeAuthenticatorConfig(prevConfig);
        }

        AuthenticatorConfigModel config = RepresentationToModel.toModel(json);
        config = realm.addAuthenticatorConfig(config);
        model.setAuthenticatorConfig(config.getId());
        realm.updateAuthenticatorExecution(model);

        json.setId(config.getId());
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.AUTHENTICATOR_CONFIG).resourcePath(session.getContext().getUri()).representation(json).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(config.getId()).build()).build();
    }

    /**
     * Get execution's configuration
     *
     * @param execution Execution id
     * @param id Configuration id
     * @return
     * @deprecated Use rather {@link #getAuthenticatorConfig(String)}
     */
    @Path("/executions/{executionId}/config/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Get execution's configuration", deprecated = true)
    @Deprecated
    public AuthenticatorConfigRepresentation getAuthenticatorConfig(@Parameter(description = "Execution id") @PathParam("executionId") String execution, @Parameter(description = "Configuration id") @PathParam("id") String id) {
        auth.realm().requireViewRealm();

        AuthenticatorConfigModel config = new DeployedConfigurationsManager(session).getAuthenticatorConfig(realm, id);
        if (config == null) {
            throw new NotFoundException("Could not find authenticator config");

        }
        return ModelToRepresentation.toRepresentation(config);
    }

    /**
     * Get unregistered required actions
     *
     * Returns a stream of unregistered required actions.
     * @return
     */
    @Path("unregistered-required-actions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Get unregistered required actions Returns a stream of unregistered required actions.")
    public Stream<Map<String, String>> getUnregisteredRequiredActions() {
        auth.realm().requireViewRealm();

        Set<String> providerIds = realm.getRequiredActionProvidersStream()
                .map(RequiredActionProviderModel::getProviderId).collect(Collectors.toSet());

        return session.getKeycloakSessionFactory().getProviderFactoriesStream(RequiredActionProvider.class)
                .filter(factory -> !providerIds.contains(factory.getId()))
                .map(factory -> {
                    RequiredActionFactory r = (RequiredActionFactory) factory;
                    Map<String, String> m = new HashMap<>();
                    m.put("name", r.getDisplayText());
                    m.put("providerId", r.getId());
                    return m;
                });
    }

    /**
     * Register a new required actions
     *
     * @param data JSON containing 'providerId', and 'name' attributes.
     */
    @Path("register-required-action")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Register a new required actions")
    @APIResponse(responseCode = "204", description = "No Content")
    public void registerRequiredAction(@Parameter(description = "JSON containing 'providerId', and 'name' attributes.") Map<String, String> data) {
        auth.realm().requireManageRealm();

        String providerId = data.get("providerId");

        if (providerId == null || session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, providerId) == null) {
            throw new BadRequestException("Required Action Provider with given providerId not found");
        }

        String name = data.get("name");
        RequiredActionProviderModel requiredAction = new RequiredActionProviderModel();
        requiredAction.setAlias(providerId);
        requiredAction.setName(name);
        requiredAction.setProviderId(providerId);
        requiredAction.setDefaultAction(false);
        requiredAction.setPriority(getNextRequiredActionPriority());
        requiredAction.setEnabled(true);
        requiredAction = realm.addRequiredActionProvider(requiredAction);

        data.put("id", requiredAction.getId());
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.REQUIRED_ACTION).resourcePath(session.getContext().getUri()).representation(data).success();
    }

    private int getNextRequiredActionPriority() {
        List<RequiredActionProviderModel> actions = realm.getRequiredActionProvidersStream().collect(Collectors.toList());
        return actions.isEmpty() ? 0 : actions.get(actions.size() - 1).getPriority() + 1;
    }


    /**
     * Get required actions
     *
     * Returns a stream of required actions.
     * @return
     */
    @Path("required-actions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Get required actions Returns a stream of required actions.")
    public Stream<RequiredActionProviderRepresentation> getRequiredActions() {
        auth.realm().requireViewRequiredActions();

        return realm.getRequiredActionProvidersStream().map(AuthenticationManagementResource::toRepresentation);
    }

    public static RequiredActionProviderRepresentation toRepresentation(RequiredActionProviderModel model) {
        RequiredActionProviderRepresentation rep = new RequiredActionProviderRepresentation();
        rep.setAlias(model.getAlias());
        rep.setProviderId(model.getProviderId());
        rep.setName(model.getName());
        rep.setDefaultAction(model.isDefaultAction());
        rep.setPriority(model.getPriority());
        rep.setEnabled(model.isEnabled());
        rep.setConfig(model.getConfig());
        return rep;
    }

    /**
     * Get required action for alias
     * @param alias Alias of required action
     * @return The required action representation
     */
    @Path("required-actions/{alias}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Get required action for alias")
    public RequiredActionProviderRepresentation getRequiredAction(@Parameter(description = "Alias of required action") @PathParam("alias") String alias) {
        auth.realm().requireViewRealm();

        RequiredActionProviderModel model = realm.getRequiredActionProviderByAlias(alias);
        if (model == null) {
            throw new NotFoundException("Failed to find required action");
        }
        return toRepresentation(model);
    }


    /**
     * Update required action
     *
     * @param alias Alias of required action
     * @param rep JSON describing new state of required action
     */
    @Path("required-actions/{alias}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Update required action")
    @APIResponse(responseCode = "204", description = "No Content")
    public void updateRequiredAction(@Parameter(description = "Alias of required action") @PathParam("alias") String alias, @Parameter(description = "JSON describing new state of required action") RequiredActionProviderRepresentation rep) {
        auth.realm().requireManageRealm();

        RequiredActionProviderModel model = realm.getRequiredActionProviderByAlias(alias);
        if (model == null) {
            throw new NotFoundException("Failed to find required action");
        }
        RequiredActionProviderModel update = new RequiredActionProviderModel();
        update.setId(model.getId());
        update.setName(rep.getName());
        update.setAlias(rep.getAlias());
        update.setProviderId(model.getProviderId());
        update.setDefaultAction(rep.isDefaultAction());
        update.setPriority(rep.getPriority());
        update.setEnabled(rep.isEnabled());
        update.setConfig(rep.getConfig());
        realm.updateRequiredActionProvider(update);

        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.REQUIRED_ACTION).resourcePath(session.getContext().getUri()).representation(rep).success();
    }

    /**
     * Delete required action
     * @param alias Alias of required action
     */
    @Path("required-actions/{alias}")
    @DELETE
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Delete required action")
    @APIResponse(responseCode = "204", description = "No Content")
    public void removeRequiredAction(@Parameter(description = "Alias of required action") @PathParam("alias") String alias) {
        auth.realm().requireManageRealm();

        RequiredActionProviderModel model = realm.getRequiredActionProviderByAlias(alias);
        if (model == null) {
            throw new NotFoundException("Failed to find required action.");
        }
        realm.removeRequiredActionProvider(model);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.REQUIRED_ACTION).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Raise required action's priority
     *
     * @param alias Alias of required action
     */
    @Path("required-actions/{alias}/raise-priority")
    @POST
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Raise required action's priority")
    @APIResponse(responseCode = "204", description = "No Content")
    public void raiseRequiredActionPriority(@Parameter(description = "Alias of required action") @PathParam("alias") String alias) {
        auth.realm().requireManageRealm();

        RequiredActionProviderModel model = realm.getRequiredActionProviderByAlias(alias);
        if (model == null) {
            throw new NotFoundException("Failed to find required action.");
        }

        RequiredActionProviderModel previous = null;
        for (RequiredActionProviderModel action : realm.getRequiredActionProvidersStream().collect(Collectors.toList())) {
            if (action.getId().equals(model.getId())) {
                break;
            }
            previous = action;
        }
        if (previous == null) return;
        int tmp = previous.getPriority();
        previous.setPriority(model.getPriority());
        realm.updateRequiredActionProvider(previous);
        model.setPriority(tmp);
        realm.updateRequiredActionProvider(model);

        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.REQUIRED_ACTION).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Lower required action's priority
     *
     * @param alias Alias of required action
     */
    @Path("/required-actions/{alias}/lower-priority")
    @POST
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Lower required action's priority")
    @APIResponse(responseCode = "204", description = "No Content")
    public void lowerRequiredActionPriority(@Parameter(description = "Alias of required action") @PathParam("alias") String alias) {
        auth.realm().requireManageRealm();

        RequiredActionProviderModel model = realm.getRequiredActionProviderByAlias(alias);
        if (model == null) {
            throw new NotFoundException("Failed to find required action.");
        }

        List<RequiredActionProviderModel> actions = realm.getRequiredActionProvidersStream().collect(Collectors.toList());
        int i;
        for (i = 0; i < actions.size(); i++) {
            if (actions.get(i).getId().equals(model.getId())) {
                break;
            }
        }
        if (i + 1 >= actions.size()) return;
        RequiredActionProviderModel next = actions.get(i + 1);
        int tmp = model.getPriority();
        model.setPriority(next.getPriority());
        realm.updateRequiredActionProvider(model);
        next.setPriority(tmp);
        realm.updateRequiredActionProvider(next);

        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.REQUIRED_ACTION).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Get required actions provider's configuration description
     * @param alias The alias of the required action
     * @return The required action configuration representation
     */
    @Path("required-actions/{alias}/config-description")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Get RequiredAction provider configuration description")
    public RequiredActionConfigInfoRepresentation getRequiredActionConfigDescription(@Parameter(description = "Alias of required action")  @PathParam("alias") String alias) {
        auth.realm().requireViewRealm();

        RequiredActionFactory factory = RequiredActionHelper.lookupConfigurableRequiredActionFactory(session, alias);
        if (factory == null) {
            throw new NotFoundException("Could not find configurable RequiredAction provider");
        }

        RequiredActionConfigInfoRepresentation rep = new RequiredActionConfigInfoRepresentation();
        rep.setProperties(new LinkedList<>());
        List<ProviderConfigProperty> configProperties = Optional.ofNullable(factory.getConfigMetadata()).orElse(Collections.emptyList());
        for (ProviderConfigProperty prop : configProperties) {
            ConfigPropertyRepresentation propRep = getConfigPropertyRep(prop);
            rep.getProperties().add(propRep);
        }
        return rep;
    }

    /**
     * Get the configuration of the RequiredAction provider in the current Realm.
     * @param alias Provider id
     * @return The required action configuration representation
     */
    @Path("required-actions/{alias}/config")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Get RequiredAction configuration")
    public RequiredActionConfigRepresentation getRequiredActionConfig(@Parameter(description = "Alias of required action")  @PathParam("alias") String alias) {
        auth.realm().requireViewRealm();

        RequiredActionFactory factory = RequiredActionHelper.lookupConfigurableRequiredActionFactory(session, alias);
        if (factory == null) {
            throw new BadRequestException("RequiredAction is not configurable");
        }

        RequiredActionConfigModel config = realm.getRequiredActionConfigByAlias(alias);
        if (config == null) {
            throw new NotFoundException("Could not find RequiredAction config");
        }

        return ModelToRepresentation.toRepresentation(config);
    }

    /**
     * Remove the configuration from the RequiredAction provider in the current Realm.
     * @param alias Provider id
     */
    @Path("required-actions/{alias}/config")
    @DELETE
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Delete RequiredAction configuration")
    @APIResponse(responseCode = "204", description = "No Content")
    public void removeRequiredActionConfig(@Parameter(description = "Alias of required action")  @PathParam("alias") String alias) {
        auth.realm().requireManageRealm();

        RequiredActionFactory factory = RequiredActionHelper.lookupConfigurableRequiredActionFactory(session, alias);
        if (factory == null) {
            throw new BadRequestException("RequiredAction is not configurable");
        }

        RequiredActionConfigModel config = realm.getRequiredActionConfigByAlias(alias);
        if (config == null) {
            throw new NotFoundException("Could not find RequiredAction config");
        }
        realm.removeRequiredActionProviderConfig(config);

        adminEvent.operation(OperationType.DELETE) //
                .resource(ResourceType.REQUIRED_ACTION_CONFIG) //
                .resourcePath(session.getContext().getUri()) //
                .success();
    }

    /**
     * Update the configuration of the RequiredAction provider in the current Realm.
     * @param alias provider id
     * @param rep JSON describing new state of RequiredAction configuration
     */
    @Path("required-actions/{alias}/config")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Update RequiredAction configuration")
    @APIResponse(responseCode = "204", description = "No Content")
    public void updateRequiredActionConfig(@Parameter(description = "Alias of required action")  @PathParam("alias") String alias, @Parameter(description = "JSON describing new state of required action configuration") RequiredActionConfigRepresentation rep) {
        auth.realm().requireManageRealm();

        RequiredActionFactory factory = RequiredActionHelper.lookupConfigurableRequiredActionFactory(session, alias);
        if (factory == null) {
            throw new BadRequestException("RequiredAction is not configurable");
        }

        RequiredActionConfigModel exists = realm.getRequiredActionConfigByAlias(alias);
        if (exists == null) {
            throw new NotFoundException("Could not find RequiredAction config");
        }

        exists.setConfig(RepresentationToModel.removeEmptyString(rep.getConfig()));
        try {
            realm.updateRequiredActionConfig(exists);
            adminEvent.operation(OperationType.UPDATE) //
                    .resource(ResourceType.REQUIRED_ACTION_CONFIG) //
                    .resourcePath(session.getContext().getUri()) //
                    .representation(rep) //
                    .success();
        } catch (ValidationException ve) {
            List<ErrorRepresentation> errorReps = ve.getErrors().stream().map(err -> new ErrorRepresentation(err.getAttribute(), err.getMessage(), err.getMessageParameters())).toList();
            throw ErrorResponse.errors(errorReps, Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Get authenticator provider's configuration description
     * @param providerId The authenticator provider id
     * @return The authenticator configuration representation
     */
    @Path("config-description/{providerId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Get authenticator provider's configuration description")
    public AuthenticatorConfigInfoRepresentation getAuthenticatorConfigDescription(@PathParam("providerId") String providerId) {
        auth.realm().requireViewRealm();

        ConfigurableAuthenticatorFactory factory = CredentialHelper.getConfigurableAuthenticatorFactory(session, providerId);

        if (factory == null) {
            providerId = new String(Base32.decode(providerId));
            factory = CredentialHelper.getConfigurableAuthenticatorFactory(session, providerId);
        }

        if (factory == null) {
            throw new NotFoundException("Could not find authenticator provider");
        }

        AuthenticatorConfigInfoRepresentation rep = new AuthenticatorConfigInfoRepresentation();
        rep.setProviderId(providerId);
        rep.setName(factory.getDisplayType());
        rep.setHelpText(factory.getHelpText());
        rep.setProperties(new LinkedList<>());
        List<ProviderConfigProperty> configProperties = Optional.ofNullable(factory.getConfigProperties()).orElse(Collections.emptyList());
        for (ProviderConfigProperty prop : configProperties) {
            ConfigPropertyRepresentation propRep = getConfigPropertyRep(prop);
            rep.getProperties().add(propRep);
        }
        return rep;
    }

    private ConfigPropertyRepresentation getConfigPropertyRep(ProviderConfigProperty prop) {
        return ModelToRepresentation.toRepresentation(prop);
    }

    /**
     * Get configuration descriptions for all clients
     * @return
     */
    @Path("per-client-config-description")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Get configuration descriptions for all clients")
    public Map<String, List<ConfigPropertyRepresentation>> getPerClientConfigDescription() {
        auth.realm().requireViewClientAuthenticatorProviders();

        return session.getKeycloakSessionFactory().getProviderFactoriesStream(ClientAuthenticator.class)
                .collect(Collectors.toMap(
                        ProviderFactory::getId,
                        factory -> {
                            ClientAuthenticatorFactory clientAuthFactory = (ClientAuthenticatorFactory)
                                    CredentialHelper.getConfigurableAuthenticatorFactory(session, factory.getId());
                            return clientAuthFactory.getConfigPropertiesPerClient().stream()
                                    .map(this::getConfigPropertyRep).collect(Collectors.toList());
                        }));
    }

    /**
     * Create new authenticator configuration
     * @param rep JSON describing new authenticator configuration
     * @return
     * @deprecated Use {@link #newExecutionConfig(String, AuthenticatorConfigRepresentation)} instead
     */
    @Path("config")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Create new authenticator configuration", deprecated = true)
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Created"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    @Deprecated
    public Response createAuthenticatorConfig(@Parameter(description = "JSON describing new authenticator configuration") AuthenticatorConfigRepresentation rep) {
        auth.realm().requireManageRealm();

        if (rep.getAlias() == null || rep.getAlias().isEmpty()) {
            throw ErrorResponse.exists("Failed to create authentication execution configuration with empty alias name");
        }

        if (realm.getAuthenticatorConfigByAlias(rep.getAlias()) != null) {
            throw ErrorResponse.exists("Authentication execution configuration " + rep.getAlias() + " already exists");
        }

        ReservedCharValidator.validate(rep.getAlias());

        AuthenticatorConfigModel config = realm.addAuthenticatorConfig(RepresentationToModel.toModel(rep));
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.AUTHENTICATOR_CONFIG).resourcePath(session.getContext().getUri(), config.getId()).representation(rep).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(config.getId()).build()).build();
    }

    /**
     * Get authenticator configuration
     * @param id Configuration id
     * @return The authenticator configuration representation
     */
    @Path("config/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Get authenticator configuration")
    public AuthenticatorConfigRepresentation getAuthenticatorConfig(@Parameter(description = "Configuration id") @PathParam("id") String id) {
        auth.realm().requireViewRealm();

        AuthenticatorConfigModel config = new DeployedConfigurationsManager(session).getAuthenticatorConfig(realm, id);
        if (config == null) {
            throw new NotFoundException("Could not find authenticator config");

        }
        return ModelToRepresentation.toRepresentation(config);
    }

    /**
     * Delete authenticator configuration
     * @param id Configuration id
     */
    @Path("config/{id}")
    @DELETE
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Delete authenticator configuration")
    @APIResponse(responseCode = "204", description = "No Content")
    public void removeAuthenticatorConfig(@Parameter(description = "Configuration id") @PathParam("id") String id) {
        auth.realm().requireManageRealm();

        AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(id);
        if (config == null) {
            throw new NotFoundException("Could not find authenticator config");

        }
        realm.getAuthenticationFlowsStream().forEach(flow -> realm.getAuthenticationExecutionsStream(flow.getId())
                .filter(exe -> Objects.equals(id, exe.getAuthenticatorConfig()))
                .forEachOrdered(exe -> {
                    exe.setAuthenticatorConfig(null);
                    realm.updateAuthenticatorExecution(exe);
                }));

        realm.removeAuthenticatorConfig(config);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.AUTHENTICATOR_CONFIG).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Update authenticator configuration
     * @param id Configuration id
     * @param rep JSON describing new state of authenticator configuration
     */
    @Path("config/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.AUTHENTICATION_MANAGEMENT)
    @Operation( summary = "Update authenticator configuration")
    @APIResponse(responseCode = "204", description = "No Content")
    public void updateAuthenticatorConfig(@Parameter(description = "Configuration id") @PathParam("id") String id, @Parameter(description = "JSON describing new state of authenticator configuration") AuthenticatorConfigRepresentation rep) {
        auth.realm().requireManageRealm();

        ReservedCharValidator.validate(rep.getAlias());
        if (new DeployedConfigurationsManager(session).getDeployedAuthenticatorConfig(id) != null) {
            throw new BadRequestException("Authenticator config is read-only");
        }

        AuthenticatorConfigModel exists = realm.getAuthenticatorConfigById(id);
        if (exists == null) {
            throw new NotFoundException("Could not find authenticator config");
        }

        exists.setAlias(rep.getAlias());
        exists.setConfig(RepresentationToModel.removeEmptyString(rep.getConfig()));
        realm.updateAuthenticatorConfig(exists);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.AUTHENTICATOR_CONFIG).resourcePath(session.getContext().getUri()).representation(rep).success();
    }
}
