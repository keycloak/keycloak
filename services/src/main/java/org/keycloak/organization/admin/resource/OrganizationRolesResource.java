package org.keycloak.organization.admin.resource;

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

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.RoleRepresentation;
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
public class OrganizationRolesResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private final OrganizationProvider organizationProvider;
    private final OrganizationModel organization;
    private final AdminEventBuilder adminEvent;

    public OrganizationRolesResource(KeycloakSession session, OrganizationModel organization, AdminEventBuilder adminEvent) {
        this.realm = session == null ? null : session.getContext().getRealm();
        this.session = session;
        this.organizationProvider = session == null ? null : session.getProvider(OrganizationProvider.class);
        this.organization = organization;
        this.adminEvent = adminEvent.resource(ResourceType.REALM_ROLE); // використовуємо існуючий тип
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Create a new organization role",
            description = "Creates a new role scoped to this organization.")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Created"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response createRole(RoleRepresentation rep) {
        if (rep == null || ObjectUtil.isBlank(rep.getName())) {
            throw ErrorResponse.error("Role name is missing", Response.Status.BAD_REQUEST);
        }

        try {
            RoleModel role = organizationProvider.createRole(organization, rep.getName());

            if (rep.getDescription() != null) {
                role.setDescription(rep.getDescription());
            }

            rep.setId(role.getId());

            adminEvent.operation(OperationType.CREATE)
                    .resourcePath(session.getContext().getUri())
                    .representation(rep)
                    .success();

            URI uri = session.getContext().getUri().getAbsolutePathBuilder()
                    .path(role.getId()).build();
            return Response.created(uri).build();

        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists("Role with the given name already exists.");
        }
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Get organization roles",
            description = "Returns all roles defined for this organization.")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK")
    })
    public List<RoleRepresentation> getRoles(@QueryParam("search") String search,
                                             @QueryParam("first") Integer first,
                                             @QueryParam("max") Integer max) {
        Stream<RoleModel> roles;

        if (search != null && !search.isBlank()) {
            roles = organizationProvider.searchRolesByName(organization, search.trim(), first, max);
        } else {
            roles = organizationProvider.getRoles(organization, first, max);
        }

        return roles.map(r -> {
            RoleRepresentation rep = ModelToRepresentation.toRepresentation(r);
            rep.setName(r.getFirstAttribute("org.role.name"));
            return rep;
        }).collect(Collectors.toList());
    }

    @Path("{role-id}")
    public OrganizationRoleResource getRoleById(@PathParam("role-id") String id) {
        RoleModel role = organizationProvider.getRoleById(organization, id);

        if (role == null) {
            throw new NotFoundException("Role does not exist");
        }

        return new OrganizationRoleResource(session, organizationProvider, organization, role, adminEvent);
    }
}
