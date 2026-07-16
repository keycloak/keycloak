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

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
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
import org.jboss.resteasy.reactive.NoCache;

@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class OrganizationRolesResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final OrganizationModel organization;
    private final AdminEventBuilder adminEvent;
    private final AdminPermissionEvaluator auth;

    public OrganizationRolesResource(KeycloakSession session, OrganizationModel organization, AdminEventBuilder adminEvent, AdminPermissionEvaluator auth) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.organization = organization;
        this.adminEvent = adminEvent.resource(ResourceType.ORGANIZATION_ROLE);
        this.auth = auth;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Creates a new organization role")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Created"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response createRole(RoleRepresentation rep) {
        auth.roles().requireManage(organization);
        if (rep == null || StringUtil.isBlank(rep.getName())) {
            throw new BadRequestException("role has no name");
        }

        try {
            RoleModel role = organization.addRole(rep.getName());
            role.setDescription(rep.getDescription());
            setAttributes(role, rep.getAttributes());
            rep.setId(role.getId());

            URI uri = session.getContext().getUri().getAbsolutePathBuilder().path(role.getId()).build();
            adminEvent.resource(ResourceType.ORGANIZATION_ROLE)
                    .operation(OperationType.CREATE)
                    .resourcePath(session.getContext().getUri(), role.getId())
                    .representation(rep)
                    .success();

            return Response.created(uri).build();
        } catch (ModelDuplicateException mde) {
            throw ErrorResponse.exists("Role with name " + rep.getName() + " already exists");
        }
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Get all organization roles")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = RoleRepresentation.class, type = SchemaType.ARRAY))),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<RoleRepresentation> getRoles(@Parameter(description = "A String representing a role name or description") @QueryParam("search") @DefaultValue("") String search,
                                               @Parameter(description = "The position of the first result to be processed") @QueryParam("first") Integer firstResult,
                                               @Parameter(description = "The maximum number of results to be returned") @QueryParam("max") Integer maxResults,
                                               @Parameter(description = "If false, return roles with their attributes") @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation) {
        auth.roles().requireList(organization);

        Stream<RoleModel> roles;

        if (StringUtil.isNotBlank(search)) {
            roles = organization.searchForRolesStream(search, firstResult, maxResults);
        } else if (!Objects.isNull(firstResult) && !Objects.isNull(maxResults)) {
            roles = organization.getRolesStream(firstResult, maxResults);
        } else {
            roles = organization.getRolesStream();
        }

        Function<RoleModel, RoleRepresentation> toRepresentation = briefRepresentation ?
                ModelToRepresentation::toBriefRepresentation :
                ModelToRepresentation::toRepresentation;
        return roles.map(role -> {
            RoleRepresentation representation = toRepresentation.apply(role);
            representation.setAccess(auth.roles().getAccess(role));
            return representation;
        });
    }

    @GET
    @NoCache
    @Path("count")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Returns the organization roles count")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public long getRoleCount(@Parameter(description = "A String representing a role name or description") @QueryParam("search") String search) {
        auth.roles().requireList(organization);
        return session.roles().getOrganizationRolesCount(organization, search);
    }

    @Path("default")
    public OrganizationRoleResource getDefaultRole() {
        RoleModel defaultRole = organization.getDefaultRole();
        if (defaultRole == null) {
            throw new NotFoundException("Could not find default organization role");
        }
        return new OrganizationRoleResource(session, organization, defaultRole, adminEvent, auth);
    }

    @Path("{role-id}")
    public OrganizationRoleResource getRole(@Parameter(description = "id of role") @PathParam("role-id") String roleId) {
        return new OrganizationRoleResource(session, organization, getRoleModel(roleId), adminEvent, auth);
    }

    private RoleModel getRoleModel(String roleId) {
        if (StringUtil.isBlank(roleId)) {
            throw new BadRequestException("role id cannot be null");
        }

        RoleModel role = session.roles().getRoleById(organization, roleId);
        if (role == null) {
            throw new NotFoundException("Could not find organization role");
        }
        return role;
    }

    private void setAttributes(RoleModel role, Map<String, List<String>> attributes) {
        if (attributes != null) {
            attributes.forEach(role::setAttribute);
        }
    }
}
