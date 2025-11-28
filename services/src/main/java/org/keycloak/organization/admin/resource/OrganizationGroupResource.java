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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
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

import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;


@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class OrganizationGroupResource {

    private final KeycloakSession session;
    private final OrganizationProvider organizationProvider;
    private final OrganizationModel organization;
    private final GroupModel group;
    private final AdminEventBuilder adminEvent;

    public OrganizationGroupResource(KeycloakSession session, OrganizationProvider organizationProvider, OrganizationModel organization, GroupModel group, AdminEventBuilder adminEvent) {
        this.session = session;
        this.organizationProvider = organizationProvider;
        this.organization = organization;
        this.group = group;
        this.adminEvent = adminEvent;
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Get organization group representation")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK")
    })
    public GroupRepresentation getGroup() {
        GroupRepresentation rep = ModelToRepresentation.groupToBriefRepresentation(group);
        // todo path
        rep.setPath("");
        return rep;
    }

    @DELETE
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Delete the organization group",
        description = "Deletes the organization group and all its subgroups")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void deleteGroup() {
        // todo org cache - listen to removal event and invalidate corresponding org in the cache?
        session.groups().removeGroup(session.getContext().getRealm(), group);
        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Update organization group",
        description = "Updates the organization group's name, description, and attributes. Subgroups are not affected.")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response updateGroup(GroupRepresentation rep) {
        try {
            String groupName = rep.getName();

            if (ObjectUtil.isBlank(groupName)) {
                throw ErrorResponse.error("Group name is missing", Response.Status.BAD_REQUEST);
            }

            if (rep.getId() != null && !group.getId().equals(rep.getId())) {
                throw ErrorResponse.error("Invalid group id", Response.Status.BAD_REQUEST);
            }

            // name, todo path
            if (!Objects.equals(groupName, group.getName())) {
                group.setName(groupName);
            }

            // description
            if (!Objects.equals(rep.getDescription(), group.getDescription())) {
                group.setDescription(rep.getDescription());
            }

            //attributes
            if (rep.getAttributes() != null) {
                Set<String> attrsToRemove = new HashSet<>(group.getAttributes().keySet());
                attrsToRemove.removeAll(rep.getAttributes().keySet());
                for (Map.Entry<String, List<String>> attr : rep.getAttributes().entrySet()) {
                    group.setAttribute(attr.getKey(), attr.getValue());
                }

                for (String attr : attrsToRemove) {
                    group.removeAttribute(attr);
                }
            }

            adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(rep).success();
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists("Sibling group with the given name already exists.");
        }
    }

    @GET
    @Path("children")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Get subgroups of this organization group",
        description = "Returns a paginated stream of subgroups that belong to this organization group")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK")
    })
    public Stream<GroupRepresentation> getSubGroups(
            @Parameter(description = "A String representing either an exact group name or a partial name") @QueryParam("search") String search,
            @Parameter(description = "Boolean which defines whether the params \"search\" must match exactly or not") @QueryParam("exact") Boolean exact,
            @Parameter(description = "The position of the first result to be returned (pagination offset).") @QueryParam("first") @DefaultValue("0") Integer first,
            @Parameter(description = "The maximum number of results that are to be returned. Defaults to 10") @QueryParam("max") @DefaultValue("10") Integer max) {

        return group.getSubGroupsStream(search, exact, first, max).map(groupModel -> {
            GroupRepresentation rep = ModelToRepresentation.groupToBriefRepresentation(groupModel);
            // todo path
            rep.setPath("");
            return rep;
        });
    }

    @POST
    @Path("children")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Create or move a subgroup",
        description = "Creates a new subgroup under this organization group. " +
                "If the group representation includes an ID, moves the existing group to be a child of this group. " +
                "If no ID is provided, creates a new subgroup.")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Created"),
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Not Found"),
            @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response addSubGroup(GroupRepresentation rep) {
        String groupName = rep.getName();
        if (ObjectUtil.isBlank(groupName)) {
            throw ErrorResponse.error("Group name is missing", Response.Status.BAD_REQUEST);
        }

        try {
            RealmModel realm = session.getContext().getRealm();
            Response.ResponseBuilder builder = Response.status(204);
            GroupModel child;

            if (rep.getId() != null) {
                // MOVE existing group to this parent
                child = session.groups().getGroupById(realm, rep.getId());
                if (child == null) {
                    throw new NotFoundException("Could not find child by id");
                }

                // Validate it's an organization group
                if (!GroupModel.Type.ORGANIZATION.equals(child.getType())) {
                    throw ErrorResponse.error("Can only move organization groups", Response.Status.BAD_REQUEST);
                }

                // Validate it belongs to the same organization
                OrganizationModel childOrg = child.getOrganization();
                if (childOrg == null || !childOrg.getId().equals(organization.getId())) {
                    throw ErrorResponse.error("Group does not belong to this organization", Response.Status.BAD_REQUEST);
                }

                // Move the group if it's not already a child of this group
                if (!Objects.equals(child.getParentId(), group.getId())) {
                    session.groups().moveGroup(realm, child, group);
                }
                adminEvent.operation(OperationType.UPDATE);

            } else {
                // CREATE new subgroup
                child = organizationProvider.createGroup(organization, groupName, group);
                builder.status(201);
                rep.setId(child.getId());
                adminEvent.operation(OperationType.CREATE);
            }

            adminEvent.resourcePath(session.getContext().getUri()).representation(rep).success();
            GroupRepresentation childRep = ModelToRepresentation.toGroupHierarchy(child, true);
            return builder.type(MediaType.APPLICATION_JSON_TYPE).entity(childRep).build();

        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists("Sibling group with the given name already exists");
        } catch (ModelException me) {
            throw ErrorResponse.error(me.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    @GET
    @NoCache
    @Path("members")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Get members of this organization group",
        description = "Returns a paginated list of organization members that belong to this group")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK")
    })
    public Stream<MemberRepresentation> getMembers(@Parameter(description = "Pagination offset") @QueryParam("first") Integer firstResult,
                                                   @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults,
                                                   @Parameter(description = "Only return basic information (only guaranteed to return id, username, created, first and last name, email, enabled state, email verification state, federation link, and access. Note that it means that namely user attributes, required actions, and not before are not returned.)")
                                                   @QueryParam("briefRepresentation") Boolean briefRepresentation) {

        RealmModel realm = session.getContext().getRealm();
        return session.users().getGroupMembersStream(realm, group, firstResult, maxResults)
                .map(user -> toMemberRepresentation(realm, user, briefRepresentation));
    }

    private MemberRepresentation toMemberRepresentation(RealmModel realm, UserModel user, Boolean briefRepresentation) {
        UserRepresentation userRep = Boolean.TRUE.equals(briefRepresentation)
                ? ModelToRepresentation.toBriefRepresentation(user)
                : ModelToRepresentation.toRepresentation(session, realm, user);

        MemberRepresentation memberRep = new MemberRepresentation(userRep);
        memberRep.setMembershipType(
            organizationProvider.isManagedMember(organization, user)
                ? MembershipType.MANAGED
                : MembershipType.UNMANAGED
        );

        return memberRep;
    }

    @PUT
    @Path("members/{userId}")
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Add a user to this organization group",
        description = "Adds an organization member to this group. The user must be a member of the organization.")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request - User is not a member of the organization"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found - User does not exist"),
        @APIResponse(responseCode = "409", description = "Conflict - User is already a member of the group")
    })
    public void joinGroup(@PathParam("userId") String userId) {
        UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);

        if (user == null) {
            throw ErrorResponse.error("User does not exist", Response.Status.NOT_FOUND);
        }

        if (!organizationProvider.isMember(organization, user)) {
            throw ErrorResponse.error("User is not member of the organization", Response.Status.BAD_REQUEST);
        }

        if (user.isMemberOf(group)) {
            throw ErrorResponse.error("User is already a member of the group", Response.Status.CONFLICT);
        }

        try {
            user.joinGroup(group);
            adminEvent.operation(OperationType.CREATE)
                    .resource(ResourceType.ORGANIZATION_GROUP_MEMBERSHIP)
                    .representation(ModelToRepresentation.groupToBriefRepresentation(group))
                    .resourcePath(session.getContext().getUri())
                    .detail(UserModel.USERNAME, user.getUsername())
                    .detail(UserModel.EMAIL, user.getEmail())
                    .success();
        } catch (ModelException me) {
            throw ErrorResponse.error(me.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    @DELETE
    @Path("members/{userId}")
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Remove a user from this organization group",
        description = "Removes a user from this organization group. The user remains a member of the organization.")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found - User does not exist")
    })
    public void leaveGroup(@PathParam("userId") String userId) {
        UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);

        if (user == null) {
            throw ErrorResponse.error("User does not exist", Response.Status.NOT_FOUND);
        }

        if (user.isMemberOf(group)) {
            try {
                user.leaveGroup(group);
                adminEvent.operation(OperationType.DELETE)
                        .resource(ResourceType.ORGANIZATION_GROUP_MEMBERSHIP)
                        .representation(ModelToRepresentation.groupToBriefRepresentation(group))
                        .resourcePath(session.getContext().getUri())
                        .detail(UserModel.USERNAME, user.getUsername())
                        .detail(UserModel.EMAIL, user.getEmail())
                        .success();
            } catch (ModelException me) {
                throw ErrorResponse.error(me.getMessage(), Response.Status.BAD_REQUEST);
            }
        }
    }
}
