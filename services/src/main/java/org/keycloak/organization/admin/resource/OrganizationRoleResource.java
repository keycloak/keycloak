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
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationRoleModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.OrganizationRoleRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * REST resource for managing a specific organization role.
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class OrganizationRoleResource {

    private final KeycloakSession session;
    private final OrganizationModel organization;
    private final OrganizationRoleModel role;
    private final AdminEventBuilder adminEvent;

    public OrganizationRoleResource(KeycloakSession session, OrganizationModel organization, 
                                   OrganizationRoleModel role, AdminEventBuilder adminEvent) {
        this.session = session;
        this.organization = organization;
        this.role = role;
        this.adminEvent = adminEvent.resource(ResourceType.ORGANIZATION_ROLE);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Operation(summary = "Get organization role")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Organization role details",
                content = @Content(schema = @Schema(implementation = OrganizationRoleRepresentation.class))),
        @APIResponse(responseCode = "404", description = "Role not found")
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public OrganizationRoleRepresentation getRole() {
        return toRepresentation(role);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update organization role")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Organization role updated",
                content = @Content(schema = @Schema(implementation = OrganizationRoleRepresentation.class))),
        @APIResponse(responseCode = "400", description = "Invalid input"),
        @APIResponse(responseCode = "404", description = "Role not found")
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public OrganizationRoleRepresentation updateRole(OrganizationRoleRepresentation rep) {
        if (rep == null) {
            throw ErrorResponse.error("Role representation is required", Response.Status.BAD_REQUEST);
        }

        try {
            if (rep.getName() != null && !rep.getName().equals(role.getName())) {
                // Check if new name is already taken
                if (organization.getRole(rep.getName()) != null) {
                    throw ErrorResponse.error("Role with name '" + rep.getName() + "' already exists", 
                            Response.Status.BAD_REQUEST);
                }
                role.setName(rep.getName());
            }

            if (rep.getDescription() != null) {
                role.setDescription(rep.getDescription());
            }

            adminEvent.operation(OperationType.UPDATE)
                    .resourcePath(role.getId())
                    .representation(rep)
                    .success();

            return toRepresentation(role);
        } catch (ModelValidationException e) {
            throw ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    @DELETE
    @Operation(summary = "Delete organization role")
    @APIResponses({
        @APIResponse(responseCode = "204", description = "Organization role deleted"),
        @APIResponse(responseCode = "404", description = "Role not found")
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public Response deleteRole() {
        boolean removed = organization.removeRole(role);
        if (removed) {
            adminEvent.operation(OperationType.DELETE)
                    .resourcePath(role.getId())
                    .success();
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("members")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Operation(summary = "Get users who have this organization role")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "List of users with this role",
                content = @Content(schema = @Schema(implementation = UserRepresentation.class)))
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public List<UserRepresentation> getRoleMembers() {
        return organization.getRoleMembersStream(role)
                .map(this::toUserRepresentation)
                .collect(Collectors.toList());
    }

    @POST
    @Path("members/{userId}")
    @Operation(summary = "Grant organization role to user")
    @APIResponses({
        @APIResponse(responseCode = "204", description = "Role granted to user"),
        @APIResponse(responseCode = "400", description = "Invalid input"),
        @APIResponse(responseCode = "404", description = "User not found")
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public Response grantRoleToUser(@PathParam("userId") String userId) {
        UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);
        if (user == null) {
            throw ErrorResponse.error("User not found", Response.Status.NOT_FOUND);
        }

        try {
            organization.grantRole(user, role);
            
            adminEvent.operation(OperationType.CREATE)
                    .resourcePath(role.getId() + "/members/" + userId)
                    .success();
            
            return Response.noContent().build();
        } catch (ModelValidationException e) {
            throw ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    @DELETE
    @Path("members/{userId}")
    @Operation(summary = "Revoke organization role from user")
    @APIResponses({
        @APIResponse(responseCode = "204", description = "Role revoked from user"),
        @APIResponse(responseCode = "404", description = "User not found")
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public Response revokeRoleFromUser(@PathParam("userId") String userId) {
        UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);
        if (user == null) {
            throw ErrorResponse.error("User not found", Response.Status.NOT_FOUND);
        }

        organization.revokeRole(user, role);
        
        adminEvent.operation(OperationType.DELETE)
                .resourcePath(role.getId() + "/members/" + userId)
                .success();
        
        return Response.noContent().build();
    }

    @GET
    @Path("composites")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Operation(summary = "Get composite roles for this organization role")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "List of composite roles",
                content = @Content(schema = @Schema(implementation = RoleRepresentation.class)))
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public List<RoleRepresentation> getComposites() {
        return role.getCompositesStream()
                .map(ModelToRepresentation::toBriefRepresentation)
                .collect(Collectors.toList());
    }

    @GET
    @Path("composites/realm")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Operation(summary = "Get realm-level roles that are composites of this organization role")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "List of realm composite roles",
                content = @Content(schema = @Schema(implementation = RoleRepresentation.class)))
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public List<RoleRepresentation> getRealmComposites() {
        return role.getCompositesStream()
                .filter(r -> !r.isClientRole())
                .map(ModelToRepresentation::toBriefRepresentation)
                .collect(Collectors.toList());
    }

    @GET
    @Path("composites/clients/{clientId}")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Operation(summary = "Get client-level roles that are composites of this organization role")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "List of client composite roles",
                content = @Content(schema = @Schema(implementation = RoleRepresentation.class)))
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public List<RoleRepresentation> getClientComposites(@PathParam("clientId") String clientId) {
        ClientModel client = session.getContext().getRealm().getClientById(clientId);
        if (client == null) {
            throw ErrorResponse.error("Client not found", Response.Status.NOT_FOUND);
        }
        
        return role.getCompositesStream()
                .filter(r -> r.isClientRole() && r.getContainer().equals(client))
                .map(ModelToRepresentation::toBriefRepresentation)
                .collect(Collectors.toList());
    }

    @POST
    @Path("composites")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Add composite roles to this organization role")
    @APIResponses({
        @APIResponse(responseCode = "204", description = "Composite roles added"),
        @APIResponse(responseCode = "400", description = "Invalid input"),
        @APIResponse(responseCode = "404", description = "Role not found")
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public Response addComposites(List<RoleRepresentation> roles) {
        if (roles == null || roles.isEmpty()) {
            return Response.noContent().build();
        }

        try {
            RealmModel realm = session.getContext().getRealm();
            for (RoleRepresentation roleRep : roles) {
                RoleModel targetRole = null;
                
                if (roleRep.getId() != null) {
                    targetRole = realm.getRoleById(roleRep.getId());
                    if (targetRole == null) {
                        // Try to find as client role
                        targetRole = realm.getClientsStream()
                                .map(client -> client.getRole(roleRep.getId()))
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElse(null);
                    }
                } else if (roleRep.getName() != null) {
                    if (roleRep.getContainerId() != null) {
                        // Client role
                        ClientModel client = realm.getClientById(roleRep.getContainerId());
                        if (client != null) {
                            targetRole = client.getRole(roleRep.getName());
                        }
                    } else {
                        // Realm role
                        targetRole = realm.getRole(roleRep.getName());
                    }
                }

                if (targetRole == null) {
                    throw ErrorResponse.error("Role not found: " + roleRep.getName(), Response.Status.NOT_FOUND);
                }

                role.addCompositeRole(targetRole);
            }

            adminEvent.operation(OperationType.CREATE)
                    .resourcePath(role.getId() + "/composites")
                    .representation(roles)
                    .success();

            return Response.noContent().build();
        } catch (ModelValidationException e) {
            throw ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    @DELETE
    @Path("composites")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Remove composite roles from this organization role")
    @APIResponses({
        @APIResponse(responseCode = "204", description = "Composite roles removed"),
        @APIResponse(responseCode = "400", description = "Invalid input"),
        @APIResponse(responseCode = "404", description = "Role not found")
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    public Response removeComposites(List<RoleRepresentation> roles) {
        if (roles == null || roles.isEmpty()) {
            return Response.noContent().build();
        }

        try {
            RealmModel realm = session.getContext().getRealm();
            for (RoleRepresentation roleRep : roles) {
                RoleModel targetRole = null;
                
                if (roleRep.getId() != null) {
                    targetRole = realm.getRoleById(roleRep.getId());
                    if (targetRole == null) {
                        // Try to find as client role
                        targetRole = realm.getClientsStream()
                                .map(client -> client.getRole(roleRep.getId()))
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElse(null);
                    }
                } else if (roleRep.getName() != null) {
                    if (roleRep.getContainerId() != null) {
                        // Client role
                        ClientModel client = realm.getClientById(roleRep.getContainerId());
                        if (client != null) {
                            targetRole = client.getRole(roleRep.getName());
                        }
                    } else {
                        // Realm role
                        targetRole = realm.getRole(roleRep.getName());
                    }
                }

                if (targetRole != null) {
                    role.removeCompositeRole(targetRole);
                }
            }

            adminEvent.operation(OperationType.DELETE)
                    .resourcePath(role.getId() + "/composites")
                    .representation(roles)
                    .success();

            return Response.noContent().build();
        } catch (ModelValidationException e) {
            throw ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    private OrganizationRoleRepresentation toRepresentation(OrganizationRoleModel role) {
        OrganizationRoleRepresentation rep = new OrganizationRoleRepresentation();
        rep.setId(role.getId());
        rep.setName(role.getName());
        rep.setDescription(role.getDescription());
        rep.setComposite(role.isComposite());
        return rep;
    }

    private UserRepresentation toUserRepresentation(UserModel user) {
        UserRepresentation rep = new UserRepresentation();
        rep.setId(user.getId());
        rep.setUsername(user.getUsername());
        rep.setEmail(user.getEmail());
        rep.setFirstName(user.getFirstName());
        rep.setLastName(user.getLastName());
        rep.setEnabled(user.isEnabled());
        return rep;
    }
}
