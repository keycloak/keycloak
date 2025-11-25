/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationRoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.OrganizationRoleRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * REST resource for managing organization role mappings for users
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class OrganizationRoleMappingResource {

    private final KeycloakSession session;
    private final OrganizationModel organization;
    private final UserModel user;
    private final AdminEventBuilder adminEvent;

    public OrganizationRoleMappingResource(KeycloakSession session, OrganizationModel organization, 
                                          UserModel user, AdminEventBuilder adminEvent) {
        this.session = session;
        this.organization = organization;
        this.user = user;
        this.adminEvent = adminEvent.resource(ResourceType.ORGANIZATION_ROLE);
    }

    /**
     * Get organization role mappings for the user
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Operation(summary = "Get organization role mappings for the user")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "List of organization roles",
                content = @Content(schema = @Schema(implementation = OrganizationRoleRepresentation.class)))
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public List<OrganizationRoleRepresentation> getUserRoleMappings() {
        return organization.getUserRolesStream(user)
                .map(this::toRepresentation)
                .collect(Collectors.toList());
    }

    /**
     * Get effective organization role mappings (including composite roles)
     */
    @GET
    @Path("composite")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Operation(summary = "Get effective organization role mappings for the user")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "List of effective organization roles",
                content = @Content(schema = @Schema(implementation = OrganizationRoleRepresentation.class)))
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public List<OrganizationRoleRepresentation> getCompositeUserRoleMappings() {
        Stream<OrganizationRoleModel> directRoles = organization.getUserRolesStream(user);
        Stream<OrganizationRoleModel> compositeRoles = directRoles
                .flatMap(role -> role.getCompositesStream())
                .filter(role -> role instanceof OrganizationRoleModel)
                .map(role -> (OrganizationRoleModel) role);
        
        return Stream.concat(
                organization.getUserRolesStream(user),
                compositeRoles
        ).distinct()
                .map(this::toRepresentation)
                .collect(Collectors.toList());
    }

    /**
     * Get available organization roles that can be mapped to the user
     */
    @GET
    @Path("available")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Operation(summary = "Get available organization roles for the user")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "List of available organization roles",
                content = @Content(schema = @Schema(implementation = OrganizationRoleRepresentation.class)))
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public List<OrganizationRoleRepresentation> getAvailableUserRoleMappings() {
        List<OrganizationRoleModel> userRoles = organization.getUserRolesStream(user)
                .collect(Collectors.toList());
        
        return organization.getRolesStream()
                .filter(role -> !userRoles.contains(role))
                .map(this::toRepresentation)
                .collect(Collectors.toList());
    }

    /**
     * Add organization role mappings to the user
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Add organization role mappings to the user")
    @APIResponses({
        @APIResponse(responseCode = "204", description = "Roles assigned"),
        @APIResponse(responseCode = "400", description = "Invalid input"),
        @APIResponse(responseCode = "404", description = "Role not found")
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public Response addUserRoleMappings(List<OrganizationRoleRepresentation> roles) {
        if (roles == null || roles.isEmpty()) {
            return Response.noContent().build();
        }

        for (OrganizationRoleRepresentation roleRep : roles) {
            OrganizationRoleModel role = null;
            if (roleRep.getId() != null) {
                role = organization.getRoleById(roleRep.getId());
            } else if (roleRep.getName() != null) {
                role = organization.getRole(roleRep.getName());
            }

            if (role == null) {
                throw ErrorResponse.error("Organization role not found", Response.Status.NOT_FOUND);
            }

            organization.grantRole(user, role);
        }

        adminEvent.operation(OperationType.CREATE)
                .resourcePath(user.getId() + "/roles")
                .representation(roles)
                .success();

        return Response.noContent().build();
    }

    /**
     * Delete organization role mappings from the user
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Remove organization role mappings from the user")
    @APIResponses({
        @APIResponse(responseCode = "204", description = "Roles removed"),
        @APIResponse(responseCode = "400", description = "Invalid input"),
        @APIResponse(responseCode = "404", description = "Role not found")
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public Response deleteUserRoleMappings(List<OrganizationRoleRepresentation> roles) {
        if (roles == null || roles.isEmpty()) {
            return Response.noContent().build();
        }

        for (OrganizationRoleRepresentation roleRep : roles) {
            OrganizationRoleModel role = null;
            if (roleRep.getId() != null) {
                role = organization.getRoleById(roleRep.getId());
            } else if (roleRep.getName() != null) {
                role = organization.getRole(roleRep.getName());
            }

            if (role != null) {
                organization.revokeRole(user, role);
            }
        }

        adminEvent.operation(OperationType.DELETE)
                .resourcePath(user.getId() + "/roles")
                .representation(roles)
                .success();

        return Response.noContent().build();
    }

    private OrganizationRoleRepresentation toRepresentation(OrganizationRoleModel role) {
        OrganizationRoleRepresentation rep = new OrganizationRoleRepresentation();
        rep.setId(role.getId());
        rep.setName(role.getName());
        rep.setDescription(role.getDescription());
        rep.setComposite(role.isComposite());
        return rep;
    }
}
