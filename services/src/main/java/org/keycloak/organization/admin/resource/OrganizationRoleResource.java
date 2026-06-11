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

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;

@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class OrganizationRoleResource {

    private final KeycloakSession session;
    private final OrganizationProvider organizationProvider;
    private final OrganizationModel organization;
    private final RoleModel role;
    private final AdminEventBuilder adminEvent;

    public OrganizationRoleResource(KeycloakSession session, OrganizationProvider organizationProvider,
                                    OrganizationModel organization, RoleModel role, AdminEventBuilder adminEvent) {
        this.session = session;
        this.organizationProvider = organizationProvider;
        this.organization = organization;
        this.role = role;
        this.adminEvent = adminEvent;
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Get organization role")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK")
    })
    public RoleRepresentation getRole() {
        RoleRepresentation rep = ModelToRepresentation.toRepresentation(role);
        rep.setName(role.getFirstAttribute("org.role.name"));
        return rep;
    }

    @DELETE
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Delete organization role")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void deleteRole() {
        organizationProvider.removeRole(organization, role);
        adminEvent.operation(OperationType.DELETE)
                .resourcePath(session.getContext().getUri())
                .success();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Update organization role")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response updateRole(RoleRepresentation rep) {
        if (rep == null || ObjectUtil.isBlank(rep.getName())) {
            throw ErrorResponse.error("Role name is missing", Response.Status.BAD_REQUEST);
        }

        try {
            String internalName = "org." + organization.getId() + "." + rep.getName();
            role.setName(internalName);
            role.setSingleAttribute("org.role.name", rep.getName());
            if (rep.getDescription() != null) {
                role.setDescription(rep.getDescription());
            }

            adminEvent.operation(OperationType.UPDATE)
                    .resourcePath(session.getContext().getUri())
                    .representation(rep)
                    .success();

            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists("Role with the given name already exists.");
        }
    }

    @GET
    @Path("users")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Get members with this organization role",
            description = "Returns organization members that have been assigned this role.")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK")
    })
    public List<UserRepresentation> getUsersInRole() {
        return organizationProvider.getMembersWithRole(organization, role)
                .map(user -> ModelToRepresentation.toRepresentation(session, session.getContext().getRealm(), user))
                .collect(Collectors.toList());
    }

    @PUT
    @Path("users/{userId}")
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Assign role to organization member")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void assignRoleToUser(@PathParam("userId") String userId) {
        UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);

        if (user == null) {
            throw ErrorResponse.error("User does not exist", Response.Status.NOT_FOUND);
        }

        if (!organizationProvider.isMember(organization, user)) {
            throw ErrorResponse.error("User is not a member of the organization", Response.Status.BAD_REQUEST);
        }

        organizationProvider.grantRole(organization, user, role);

        adminEvent.operation(OperationType.CREATE)
                .resourcePath(session.getContext().getUri())
                .success();
    }

    @DELETE
    @Path("users/{userId}")
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Remove role from organization member")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void removeRoleFromUser(@PathParam("userId") String userId) {
        UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);

        if (user == null) {
            throw ErrorResponse.error("User does not exist", Response.Status.NOT_FOUND);
        }

        organizationProvider.revokeRole(organization, user, role);

        adminEvent.operation(OperationType.DELETE)
                .resourcePath(session.getContext().getUri())
                .success();
    }
}
