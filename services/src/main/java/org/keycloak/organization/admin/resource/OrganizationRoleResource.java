/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.organization.admin.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelIllegalStateException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.organization.validation.OrganizationsValidation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.RoleResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.utils.StringUtil;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class OrganizationRoleResource extends RoleResource {

    private static final Logger logger = Logger.getLogger(OrganizationRoleResource.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final OrganizationModel organization;
    private final RoleModel role;
    private final AdminEventBuilder adminEvent;
    private final AdminPermissionEvaluator auth;

    public OrganizationRoleResource(KeycloakSession session, OrganizationModel organization, RoleModel role, AdminEventBuilder adminEvent, AdminPermissionEvaluator auth) {
        super(session.getContext().getRealm());
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.organization = organization;
        this.role = role;
        this.adminEvent = adminEvent.resource(ResourceType.ORGANIZATION_ROLE);
        this.auth = auth;
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Get a specific organization role's representation")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = RoleRepresentation.class))),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public RoleRepresentation getRole() {
        auth.roles().requireView(role);
        return getRole(role);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Update an organization role")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response updateRole(RoleRepresentation rep) {
        auth.roles().requireManage(role);
        if (rep == null) {
            throw new BadRequestException("role has no name");
        }

        try {
            updateRole(rep, role, realm, session);
            adminEvent.resource(ResourceType.ORGANIZATION_ROLE)
                    .operation(OperationType.UPDATE)
                    .resourcePath(session.getContext().getUri())
                    .representation(rep)
                    .success();
            return Response.noContent().build();
        } catch (ModelDuplicateException mde) {
            throw ErrorResponse.exists("Role with name " + rep.getName() + " already exists");
        }
    }

    @DELETE
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Delete an organization role")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Response deleteRole() {
        rejectDefaultRoleRemoval();
        auth.roles().requireManage(role);

        RoleRepresentation representation = ModelToRepresentation.toBriefRepresentation(role);
        deleteRole(role);
        adminEvent.resource(ResourceType.ORGANIZATION_ROLE)
                .operation(OperationType.DELETE)
                .resourcePath(session.getContext().getUri())
                .representation(representation)
                .success();

        return Response.noContent().build();
    }

    @POST
    @Path("composites")
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Make the organization role a composite role by associating child roles")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void addComposites(List<RoleRepresentation> roles) {
        auth.roles().requireManage(role);

        List<RoleModel> composites = resolveCompositeRoles(roles, true);
        composites.forEach(role::addCompositeRole);

        if (!roles.isEmpty()) {
            adminEvent.resource(ResourceType.ORGANIZATION_ROLE)
                    .operation(OperationType.CREATE)
                    .resourcePath(session.getContext().getUri())
                    .representation(roles)
                    .success();
        }
    }

    @GET
    @Path("composites")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Get an organization role's composite roles")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = RoleRepresentation.class, type = SchemaType.ARRAY))),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<RoleRepresentation> getRoleComposites(@QueryParam("search") String search,
                                                        @QueryParam("first") Integer first,
                                                        @QueryParam("max") Integer max) {
        auth.roles().requireView(role);
        if (search == null && first == null && max == null) {
            return role.getCompositesStream().map(ModelToRepresentation::toBriefRepresentation);
        }
        return role.getCompositesStream(search, first, max).map(ModelToRepresentation::toBriefRepresentation);
    }

    @GET
    @Path("composites/realm")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Get realm-level roles in the organization role's composite")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = RoleRepresentation.class, type = SchemaType.ARRAY))),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<RoleRepresentation> getRealmRoleComposites() {
        auth.roles().requireView(role);
        return getRealmRoleComposites(role);
    }

    @GET
    @Path("composites/clients/{clientUuid}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Get client-level roles for the client in the organization role's composite")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = RoleRepresentation.class, type = SchemaType.ARRAY))),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Not Found")
    })
    public Stream<RoleRepresentation> getClientRoleComposites(@Parameter(description = "client id (not clientId!)") @PathParam("clientUuid") String clientUuid) {
        auth.roles().requireView(role);
        ClientModel client = realm.getClientById(clientUuid);
        if (client == null) {
            throw new NotFoundException("Could not find client");
        }
        return getClientRoleComposites(client, role);
    }

    @DELETE
    @Path("composites")
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Remove roles from the organization role's composite")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void deleteComposites(List<RoleRepresentation> roles) {
        auth.roles().requireManage(role);

        List<RoleModel> composites = resolveCompositeRoles(roles, false);
        composites.forEach(role::removeCompositeRole);

        adminEvent.resource(ResourceType.ORGANIZATION_ROLE)
                .operation(OperationType.DELETE)
                .resourcePath(session.getContext().getUri())
                .representation(roles)
                .success();
    }

    @GET
    @Path("users")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Returns users that have the specified organization role")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UserRepresentation.class, type = SchemaType.ARRAY))),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<UserRepresentation> getUsersInRole(@Parameter(description = "Boolean which defines whether brief representations are returned") @QueryParam("briefRepresentation") Boolean briefRepresentation,
                                                     @Parameter(description = "First result to return") @QueryParam("first") Integer firstResult,
                                                     @Parameter(description = "Maximum number of results to return") @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STR) Integer maxResults) {
        auth.roles().requireView(role);
        auth.users().requireQuery();

        if (!realm.isAdminPermissionsEnabled() && !auth.users().canView()) {
            return Stream.empty();
        }

        boolean briefRep = Boolean.TRUE.equals(briefRepresentation);
        int first = firstResult == null ? 0 : firstResult;
        int max = maxResults == null ? Constants.DEFAULT_MAX_RESULTS : maxResults;

        return session.users().getRoleMembersStream(realm, role, first, max)
                .filter(organization::isMember)
                .map(user -> ModelToRepresentation.toRepresentation(session, user, briefRep));
    }

    @POST
    @Path("users")
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Assign the organization role to organization members")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void addUserRoleMappings(List<UserRepresentation> users) {
        updateUserRoleMappings(users, true, UserModel::grantRole, OperationType.CREATE);
    }

    @DELETE
    @Path("users")
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Remove the organization role from users")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void deleteUserRoleMappings(List<UserRepresentation> users) {
        updateUserRoleMappings(users, false, UserModel::deleteRoleMapping, OperationType.DELETE);
    }

    private List<RoleModel> resolveCompositeRoles(List<RoleRepresentation> roles, boolean validateComposite) {
        if (roles == null) {
            throw new BadRequestException("roles are required");
        }

        List<RoleModel> composites = new ArrayList<>();
        for (RoleRepresentation rep : roles) {
            if (rep == null || rep.getId() == null) {
                throw new NotFoundException("Could not find composite role");
            }

            RoleModel composite = realm.getRoleById(rep.getId());
            if (composite == null) {
                throw new NotFoundException("Could not find composite role");
            }

            auth.roles().requireMapComposite(composite);
            if (validateComposite) {
                try {
                    OrganizationsValidation.validateOrganizationRoleComposite(role, composite);
                } catch (ModelException me) {
                    throw new BadRequestException(me.getMessage(), me);
                }
            }
            composites.add(composite);
        }
        return composites;
    }

    private void updateUserRoleMappings(List<UserRepresentation> users, boolean validateMembership, BiConsumer<UserModel, RoleModel> mapping, OperationType operationType) {
        if (users == null) {
            throw new BadRequestException("users are required");
        }

        auth.roles().requireMapRole(role);
        try {
            for (UserRepresentation userRepresentation : users) {
                UserModel user = getUser(userRepresentation);
                auth.users().requireManage(user);
                if (validateMembership) {
                    OrganizationsValidation.validateOrganizationRoleMapping(user, role);
                }
                mapping.accept(user, role);
            }
        } catch (ModelIllegalStateException mise) {
            logger.error(mise.getMessage(), mise);
            throw ErrorResponse.error(mise.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        } catch (ModelException | ReadOnlyException me) {
            logger.warn(me.getMessage(), me);
            throw new ErrorResponseException("invalid_request", me.getMessage(), Response.Status.BAD_REQUEST);
        }

        if (!users.isEmpty()) {
            adminEvent.resource(ResourceType.ORGANIZATION_ROLE_MAPPING)
                    .operation(operationType)
                    .resourcePath(session.getContext().getUri())
                    .representation(users)
                    .success();
        }
    }

    private UserModel getUser(UserRepresentation userRepresentation) {
        if (userRepresentation == null || StringUtil.isBlank(userRepresentation.getId())) {
            throw new BadRequestException("user id is required");
        }

        UserModel user = session.users().getUserById(realm, userRepresentation.getId());
        if (user == null) {
            throw auth.users().canQuery() ? new NotFoundException("User not found") : new ForbiddenException();
        }
        return user;
    }

    private void rejectDefaultRoleRemoval() {
        RoleModel defaultRole = organization.getDefaultRole();
        if (defaultRole != null && Objects.equals(defaultRole.getId(), role.getId())) {
            throw ErrorResponse.error(role.getName() + " is default role of the organization and cannot be removed.",
                    Response.Status.BAD_REQUEST);
        }
    }
}
