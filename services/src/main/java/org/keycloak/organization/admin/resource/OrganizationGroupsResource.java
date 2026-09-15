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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

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
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.utils.GroupUtils;
import org.keycloak.utils.SearchQueryUtils;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;

@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class OrganizationGroupsResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private final OrganizationProvider organizationProvider;
    private final OrganizationModel organization;
    private final AdminEventBuilder adminEvent;
    private final AdminPermissionEvaluator auth;

    public OrganizationGroupsResource(KeycloakSession session, OrganizationModel organization, AdminEventBuilder adminEvent, AdminPermissionEvaluator auth) {
        this.realm = session == null ? null : session.getContext().getRealm();
        this.session = session;
        this.organizationProvider = session == null ? null : session.getProvider(OrganizationProvider.class);
        this.organization = organization;
        this.adminEvent = adminEvent.resource(ResourceType.ORGANIZATION_GROUP);
        this.auth = auth;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Creates a new top-level group or moves an existing group to top-level",
        description = "Creates a new top-level group in the organization. " +
                "If the group representation includes an ID, moves the existing organization group to be a top-level group. " +
                "If no ID is provided, creates a new top-level group.")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Created"),
        @APIResponse(responseCode = "204", description = "No Content - Group moved to top-level"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found - Group does not exist"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response addTopLevelGroup(GroupRepresentation rep) {
        auth.orgs().requireManage(organization);
        try {
            String groupName = rep.getName();

            if (ObjectUtil.isBlank(groupName)) {
                throw ErrorResponse.error("Group name is missing", Response.Status.BAD_REQUEST);
            }

            Response.ResponseBuilder builder;
            GroupModel group;

            if (rep.getId() != null) {
                // MOVE existing group to top-level
                group = session.groups().getGroupById(realm, rep.getId());
                if (group == null) {
                    throw new NotFoundException("Could not find group by id");
                }
                rejectInternalGroup(group);

                // Validate it's an organization group
                if (!GroupModel.Type.ORGANIZATION.equals(group.getType())) {
                    throw ErrorResponse.error("Can only move organization groups", Response.Status.BAD_REQUEST);
                }

                // Validate it belongs to the same organization
                if (!Organizations.isOrganizationGroup(group) ||
                        !group.getOrganization().getId().equals(organization.getId())) {
                    throw ErrorResponse.error("Group does not belong to this organization", Response.Status.BAD_REQUEST);
                }
                requireVisibleGroup(group);

                // Get internal org group (the real top-level parent for org groups)
                GroupModel internalGroup = organizationProvider.getOrganizationGroup(organization);

                // Move the group if it's not already a top-level group
                if (!Objects.equals(group.getParentId(), internalGroup.getId())) {
                    session.groups().moveGroup(realm, group, internalGroup);
                }

                builder = Response.status(204);
                adminEvent.operation(OperationType.UPDATE);

            } else {
                // CREATE new top-level org group
                group = organizationProvider.createGroup(organization, groupName, null);

                // set description and attributes
                if (rep.getDescription() != null) {
                    group.setDescription(rep.getDescription());
                }
                if (rep.getAttributes() != null) {
                    rep.getAttributes().forEach(group::setAttribute);
                }

                rep.setId(group.getId());

                URI uri = session.getContext().getUri().getAbsolutePathBuilder()
                        .path(group.getId()).build();
                builder = Response.created(uri);
                adminEvent.operation(OperationType.CREATE);
            }

            adminEvent.resourcePath(session.getContext().getUri())
                    .representation(rep)
                    .success();

            GroupRepresentation groupRep = ModelToRepresentation.groupToBriefRepresentation(group);
            return builder.type(MediaType.APPLICATION_JSON_TYPE).entity(groupRep).build();

        } catch (ModelDuplicateException mde) {
            throw ErrorResponse.exists("Group with the given name already exists.");
        } catch (ModelException me) {
            throw ErrorResponse.error(me.getMessage(), Status.BAD_REQUEST);
        }
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Get organization groups",
        description = "Returns organization groups. When `search` parameter is provided, groups are searched by name. " +
                "When `q` parameter is provided, groups are searched by attributes. " +
                "If neither parameter is provided, top-level groups are returned.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK"),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<GroupRepresentation> getGroups(@QueryParam("search") String search,
                                                 @QueryParam("q") String searchQuery,
                                                 @QueryParam("exact") @DefaultValue("false") Boolean exact,
                                                 @QueryParam("first") Integer first,
                                                 @QueryParam("max") Integer max,
                                                 @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation,
                                                 @QueryParam("populateHierarchy") @DefaultValue("false") boolean populateHierarchy,
                                                 @QueryParam("subGroupsCount") @DefaultValue("false") boolean subGroupsCount) {
        auth.orgs().requireView(organization);

        Stream<GroupModel> groups;
        if (Objects.nonNull(searchQuery)) {
            Map<String, String> attributes = SearchQueryUtils.getFields(searchQuery);
            groups = organizationProvider.searchGroupsByAttributes(organization, attributes, first, max);
        } else if (Objects.nonNull(search)) {
            groups = organizationProvider.searchGroupsByName(organization, search.trim(), exact, first, max);
        } else {
            groups = organizationProvider.getTopLevelGroups(organization, first, max);
        }

        // builds hierarchy mainly for admin UI
        if (populateHierarchy) {
            String internalGroupId = organizationProvider.getOrganizationGroup(organization).getId();
            return GroupUtils.populateGroupHierarchyFromSubGroups(session, realm, groups, !briefRepresentation, subGroupsCount, internalGroupId)
                    .map(this::decorateHierarchy);
        }

        return groups.map(group -> {
            GroupRepresentation rep = toAdminRepresentation(group, briefRepresentation);
            if (subGroupsCount) {
                rep.setSubGroupCount(group.getSubGroupsCount());
            }
            return rep;
        });
    }

    @GET
    @Path("group-by-path/{path: .*}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Get organization group by path",
        description = "Returns the organization group with the specified path")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = GroupRepresentation.class))),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Not Found")
    })
    public GroupRepresentation getGroupByPath(@PathParam("path") String path,
                                              @Parameter(description = "Whether to return a brief representation (default: true)") @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation,
                                              @Parameter(description = "Whether to return the count of subgroups (default: false)") @QueryParam("subGroupsCount") @DefaultValue("false") boolean subGroupsCount) {
        auth.orgs().requireView(organization);

        GroupModel found = KeycloakModelUtils.findGroupByPath(session, realm, organization, path);
        if (found == null) {
            throw new NotFoundException("Group path does not exist");
        }
        rejectInternalGroup(found);
        requireVisibleGroup(found);
        GroupRepresentation rep = toAdminRepresentation(found, briefRepresentation);
        if (subGroupsCount) {
            rep.setSubGroupCount(found.getSubGroupsCount());
        }
        return rep;
    }

    @Path("{group-id}")
    public OrganizationGroupResource getGroupById(@PathParam("group-id") String id) {
        auth.orgs().requireView(organization);

        GroupModel group = realm.getGroupById(id);

        if (group == null) {
            throw ErrorResponse.error("Group does not exist", Response.Status.NOT_FOUND);
        }
        rejectInternalGroup(group);

        if (!Organizations.isOrganizationGroup(group) ||
                !Objects.equals(group.getOrganization().getId(), organization.getId())) {
            throw ErrorResponse.error("Group does not belong to the organization", Status.BAD_REQUEST);
        }
        requireVisibleGroup(group);

        return new OrganizationGroupResource(session, organizationProvider, organization, group, adminEvent, auth);
    }

    private GroupRepresentation decorateHierarchy(GroupRepresentation representation) {
        GroupModel model = realm.getGroupById(representation.getId());
        if (model == null) throw new NotFoundException("Group does not exist");
        applyAccessAndVisibleOrganizationRoles(representation, model);
        representation.getSubGroups().forEach(this::decorateHierarchy);
        return representation;
    }

    private GroupRepresentation toAdminRepresentation(GroupModel group, boolean briefRepresentation) {
        GroupRepresentation representation = briefRepresentation
                ? ModelToRepresentation.groupToBriefRepresentation(group)
                : ModelToRepresentation.toRepresentation(group, true);
        applyAccessAndVisibleOrganizationRoles(representation, group);
        return representation;
    }

    private void applyAccessAndVisibleOrganizationRoles(GroupRepresentation representation, GroupModel group) {
        representation.setAccess(auth.orgs().getAccess(organization));
        if (representation.getOrganizationRoles() == null) {
            return;
        }
        Map<String, List<String>> visible = new LinkedHashMap<>();
        group.getRoleMappingsStream()
                .filter(role -> role.isType(RoleModel.Type.ORGANIZATION))
                .filter(role -> Objects.equals(organization.getId(), role.getContainerId()))
                .filter(auth.roles()::canView)
                .forEach(role -> visible.computeIfAbsent(organization.getAlias(), ignored -> new ArrayList<>())
                        .add(role.getName()));
        representation.setOrganizationRoles(visible.isEmpty() ? null : visible);
    }

    private void rejectInternalGroup(GroupModel group) {
        GroupModel internalGroup = organizationProvider.getOrganizationGroup(organization);
        if (internalGroup != null && Objects.equals(internalGroup.getId(), group.getId())) {
            throw new NotFoundException("Group does not exist");
        }
    }

    private void requireVisibleGroup(GroupModel group) {
        GroupModel root = organizationProvider.getOrganizationGroup(organization);
        Set<String> visited = new HashSet<>();
        GroupModel current = group;
        boolean visible = false;
        while (current != null && visited.add(current.getId())) {
            OrganizationModel owner = current.getOrganization();
            if (!GroupModel.Type.ORGANIZATION.equals(current.getType()) || owner == null || owner.getRealm() == null
                    || !Objects.equals(organization.getId(), owner.getId())
                    || !Objects.equals(realm.getId(), owner.getRealm().getId())) break;
            if (root != null && Objects.equals(root.getId(), current.getId()) && current.getParent() == null) {
                visible = true;
                break;
            }
            current = current.getParent();
        }
        if (!visible) throw new NotFoundException("Group does not exist");
    }
}
