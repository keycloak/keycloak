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
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationRoleModel;
import org.keycloak.representations.idm.OrganizationRoleRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.utils.StringUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST resource for managing organization roles.
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class OrganizationRolesResource {

    private final KeycloakSession session;
    private final OrganizationModel organization;
    private final AdminEventBuilder adminEvent;

    public OrganizationRolesResource(KeycloakSession session, OrganizationModel organization, AdminEventBuilder adminEvent) {
        this.session = session;
        this.organization = organization;
        this.adminEvent = adminEvent.resource(ResourceType.ORGANIZATION_ROLE);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a new organization role")
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Organization role created",
                content = @Content(schema = @Schema(implementation = OrganizationRoleRepresentation.class))),
        @APIResponse(responseCode = "400", description = "Invalid input"),
        @APIResponse(responseCode = "409", description = "Role with same name already exists")
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public Response createRole(OrganizationRoleRepresentation rep) {
        if (rep == null || StringUtil.isBlank(rep.getName())) {
            throw ErrorResponse.error("Role name is required", Response.Status.BAD_REQUEST);
        }

        try {
            OrganizationRoleModel role = organization.addRole(rep.getId(), rep.getName());
            
            if (rep.getDescription() != null) {
                role.setDescription(rep.getDescription());
            }

            adminEvent.operation(OperationType.CREATE)
                    .resourcePath(role.getId())
                    .representation(rep)
                    .success();

            OrganizationRoleRepresentation created = toRepresentation(role);
            return Response.status(Response.Status.CREATED)
                    .entity(created)
                    .location(session.getContext().getUri().getAbsolutePathBuilder().path(role.getId()).build())
                    .build();
        } catch (ModelValidationException e) {
            throw ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Operation(summary = "Get organization roles")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "List of organization roles",
                content = @Content(schema = @Schema(implementation = OrganizationRoleRepresentation.class)))
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public List<OrganizationRoleRepresentation> getRoles(
            @QueryParam("search") String search,
            @QueryParam("first") @DefaultValue("0") Integer first,
            @QueryParam("max") @DefaultValue("100") Integer max) {
        
        if (!StringUtil.isBlank(search)) {
            return organization.searchForRolesStream(search, first, max)
                    .map(this::toRepresentation)
                    .collect(Collectors.toList());
        } else {
            return organization.getRolesStream(first, max)
                    .map(this::toRepresentation)
                    .collect(Collectors.toList());
        }
    }

    @Path("{roleId}")
    public OrganizationRoleResource role(@PathParam("roleId") String roleId) {
        OrganizationRoleModel role = organization.getRoleById(roleId);
        if (role == null) {
            throw ErrorResponse.error("Role not found", Response.Status.NOT_FOUND);
        }
        return new OrganizationRoleResource(session, organization, role, adminEvent);
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
