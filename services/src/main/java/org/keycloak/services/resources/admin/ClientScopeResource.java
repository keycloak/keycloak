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

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelIllegalStateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;


/**
 * Base resource class for managing one particular client of a realm.
 *
 * @resource Client Scopes
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class ClientScopeResource {
    protected static final Logger logger = Logger.getLogger(ClientScopeResource.class);
    protected RealmModel realm;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;
    protected ClientScopeModel clientScope;
    protected KeycloakSession session;
    protected static Pattern dynamicScreenPattern = Pattern.compile("[^\\s\\*]*\\*{1}[^\\s\\*]*");
    protected final static Pattern scopeNamePattern = Pattern.compile("[\\x21\\x23-\\x5B\\x5D-\\x7E]+");

    public ClientScopeResource(RealmModel realm, AdminPermissionEvaluator auth, ClientScopeModel clientScope, KeycloakSession session, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        this.clientScope = clientScope;
        this.session = session;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT_SCOPE);
    }

    @Path("protocol-mappers")
    public ProtocolMappersResource getProtocolMappers() {
        AdminPermissionEvaluator.RequirePermissionCheck manageCheck = () -> auth.clients().requireManage(clientScope);
        AdminPermissionEvaluator.RequirePermissionCheck viewCheck = () -> auth.clients().requireView(clientScope);
        return new ProtocolMappersResource(session, clientScope, auth, adminEvent, manageCheck, viewCheck);
    }

    /**
     * Base path for managing the role scope mappings for the client scope
     *
     * @return
     */
    @Path("scope-mappings")
    public ScopeMappedResource getScopeMappedResource() {
        AdminPermissionEvaluator.RequirePermissionCheck manageCheck = () -> auth.clients().requireManage(clientScope);
        AdminPermissionEvaluator.RequirePermissionCheck viewCheck = () -> auth.clients().requireView(clientScope);
        return new ScopeMappedResource(realm, auth, clientScope, session, adminEvent, manageCheck, viewCheck);
    }

    /**
     * Update the client scope
     * @param rep
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENT_SCOPES)
    @Operation(summary = "Update the client scope")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response update(final ClientScopeRepresentation rep) {
        auth.clients().requireManageClientScopes();
        validateDynamicScopeUpdate(rep);
        try {
            LoginProtocolFactory loginProtocolFactory = //
                    (LoginProtocolFactory) session.getKeycloakSessionFactory().getProviderFactory(LoginProtocol.class,
                                                                                                  clientScope.getProtocol());
            Optional.ofNullable(loginProtocolFactory).ifPresent(lp -> lp.addClientScopeDefaults(rep));
            RepresentationToModel.updateClientScope(rep, clientScope);
            adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(rep).success();

            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().commit();
            }
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists("Client Scope " + rep.getName() + " already exists");
        }
    }

    /**
     * Get representation of the client scope
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENT_SCOPES)
    @Operation(summary = "Get representation of the client scope")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = ClientScopeRepresentation.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public ClientScopeRepresentation getClientScope() {
        auth.clients().requireView(clientScope);

        return ModelToRepresentation.toRepresentation(clientScope);
    }

    /**
     * Delete the client scope
     */
    @DELETE
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENT_SCOPES)
    @Operation(summary = "Delete the client scope")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "500", description = "Internal Server Error")
    })
    public Response deleteClientScope() {
        auth.clients().requireManage(clientScope);
        long clientScopesCount =  Arrays.stream(realm.getClientScopesStream().toArray()).count();
        if (clientScopesCount > 1) {
            try {
                realm.removeClientScope(clientScope.getId());
                adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
                return Response.noContent().build();
            } catch (ModelIllegalStateException e) {
                logger.error(e.getMessage(), e);
                throw ErrorResponse.error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
            } catch (ModelException me) {
                throw ErrorResponse.error(me.getMessage(), Response.Status.BAD_REQUEST);
            }
        } else {
            throw ErrorResponse.error("You are not allowed to delete all the client scopes.", Response.Status.FORBIDDEN);
        }
    }

    /**
     * Performs some validation based on attributes combinations and format.
     * Validations differ based on whether the DYNAMIC_SCOPES feature is enabled or not
     * @param clientScope
     * @throws ErrorResponseException
     */
    public static void validateDynamicClientScope(ClientScopeRepresentation clientScope) throws ErrorResponseException {
        if (clientScope.getAttributes() == null) {
            return;
        }
        boolean isDynamic = Boolean.parseBoolean(clientScope.getAttributes().get(ClientScopeModel.IS_DYNAMIC_SCOPE));
        String regexp = clientScope.getAttributes().get(ClientScopeModel.DYNAMIC_SCOPE_REGEXP);
        if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            // if the scope is dynamic but the regexp is empty, it's not considered valid
            if (isDynamic && StringUtil.isNullOrEmpty(regexp)) {
                throw ErrorResponse.error("Dynamic scope regexp must not be null or empty", Response.Status.BAD_REQUEST);
            }
            // Always validate the dynamic scope regexp to avoid inserting a wrong value even when the feature is disabled
            if (!StringUtil.isNullOrEmpty(regexp) && !dynamicScreenPattern.matcher(regexp).matches()) {
                throw ErrorResponse.error(String.format("Invalid format for the Dynamic Scope regexp %1s", regexp), Response.Status.BAD_REQUEST);
            }
        } else {
            // if the value is not null or empty we won't accept the request as the feature is disabled
            Optional.ofNullable(regexp).ifPresent(s -> {
                if (!s.isEmpty()) {
                    throw ErrorResponse.error(String.format("Unexpected value \"%1s\" for attribute %2s in ClientScope",
                            regexp, ClientScopeModel.DYNAMIC_SCOPE_REGEXP), Response.Status.BAD_REQUEST);
                }
            });
            // If isDynamic is true, we won't accept the request as the feature is disabled
            if (isDynamic) {
                throw ErrorResponse.error(String.format("Unexpected value \"%1s\" for attribute %2s in ClientScope",
                        isDynamic, ClientScopeModel.IS_DYNAMIC_SCOPE), Response.Status.BAD_REQUEST);
            }
        }
    }

    public static void validateClientScopeName(String name) throws ErrorResponseException {
        if (!scopeNamePattern.matcher(name).matches()) {
            String message = String.format("Unexpected name \"%s\" for ClientScope", name);
            throw ErrorResponse.error(message, Response.Status.BAD_REQUEST);
        }
    }

    public static void validateClientScopeProtocol(KeycloakSession session, String protocol)
            throws ErrorResponseException {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        Set<String> acceptedProtocols = sessionFactory.getProviderFactoriesStream(LoginProtocol.class)
                                                      .map(type -> (LoginProtocolFactory) type)
                                                      .map(LoginProtocolFactory::getId)
                                                      .collect(Collectors.toSet());
        // the OID4VC protocol is not registered to prevent it from being displayed in the client-details ui
        acceptedProtocols.add(OID4VCLoginProtocolFactory.PROTOCOL_ID);

        if (protocol == null || !acceptedProtocols.contains(protocol)) {
            throw ErrorResponse.error("Unexpected protocol", Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Makes sure that an update that makes a Client Scope Dynamic is rejected if the Client Scope is assigned to a client
     * as a default scope.
     * @param rep the {@link ClientScopeRepresentation} with the changes from the frontend.
     */
    public void validateDynamicScopeUpdate(ClientScopeRepresentation rep) {
        validateClientScopeName(rep.getName());

        // Only check this if the representation has been sent to make it dynamic
        if (rep.getAttributes() != null && rep.getAttributes().getOrDefault(ClientScopeModel.IS_DYNAMIC_SCOPE, "false").equalsIgnoreCase("true")) {
            Optional<String> scopeModelOpt = realm.getClientsStream()
                    .flatMap(clientModel -> clientModel.getClientScopes(true).values().stream())
                    .map(ClientScopeModel::getId)
                    .filter(scopeId -> scopeId.equalsIgnoreCase(this.clientScope.getId()))
                    .findAny();
            // if it's present, it means that a client has this scope assigned as a default scope, so this scope can't be made dynamic
            if (scopeModelOpt.isPresent()) {
                throw ErrorResponse.error("This Client Scope can't be made dynamic as it's assigned to a Client as a Default Scope",
                        Response.Status.BAD_REQUEST);
            }
        }
        // after the previous validation, run the usual Dynamic Scope validations.
        validateDynamicClientScope(rep);
    }
}
