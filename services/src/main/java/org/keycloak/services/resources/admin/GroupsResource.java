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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.GroupPermissionEvaluator;
import org.keycloak.utils.GroupUtils;
import org.keycloak.utils.SearchQueryUtils;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;

/**
 * @resource Groups
 * @author Bill Burke
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class GroupsResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    public GroupsResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.GROUP);

    }

    /**
     * Get group hierarchy.  Only {@code name} and {@code id} are returned.  {@code subGroups} are only returned when using the {@code search} or {@code q} parameter.
     * If none of these parameters is provided, the top-level groups are returned without `{@code subGroups} being filled.
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation( summary = "Get group hierarchy.  Only `name` and `id` are returned.  `subGroups` are only returned when using the `search` or `q` parameter. If none of these parameters is provided, the top-level groups are returned without `subGroups` being filled.")
    public Stream<GroupRepresentation> getGroups(@QueryParam("search") String search,
                                                 @QueryParam("q") String searchQuery,
                                                 @QueryParam("exact") @DefaultValue("false") Boolean exact,
                                                 @QueryParam("first") Integer firstResult,
                                                 @QueryParam("max") Integer maxResults,
                                                 @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation,
                                                 @QueryParam("populateHierarchy") @DefaultValue("true") boolean populateHierarchy,
                                                 @Parameter(description = "Boolean which defines whether to return the count of subgroups for each group (default: true") @QueryParam("subGroupsCount") @DefaultValue("true") Boolean subGroupsCount) {
        GroupPermissionEvaluator groupsEvaluator = auth.groups();
        groupsEvaluator.requireList();

        Stream<GroupModel> stream;
        if (Objects.nonNull(searchQuery)) {
            Map<String, String> attributes = SearchQueryUtils.getFields(searchQuery);
            stream = session.groups().searchGroupsByAttributes(realm, attributes, firstResult, maxResults);
        } else if (Objects.nonNull(search)) {
            stream = session.groups().searchForGroupByNameStream(realm, search.trim(), exact, firstResult, maxResults);
        } else {
            stream = session.groups().getTopLevelGroupsStream(realm, firstResult, maxResults);
        }

        if (populateHierarchy) {
            return GroupUtils.populateGroupHierarchyFromSubGroups(session, realm, stream, !briefRepresentation, groupsEvaluator, subGroupsCount);
        }

        if (!AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm)) {
            stream = stream.filter(groupsEvaluator::canView);
        }

        return stream.map(g -> {
            GroupRepresentation rep = GroupUtils.toRepresentation(groupsEvaluator, g, !briefRepresentation);

            if (subGroupsCount) {
                return GroupUtils.populateSubGroupCount(g, rep);
            }

            return rep;
        });
    }

    /**
     * Does not expand hierarchy.  Subgroups will not be set.
     *
     * @param id
     * @return
     */
    @Path("{group-id}")
    @Operation( summary = "Get group details. Does not expand hierarchy.  Subgroups will not be set.")
    public GroupResource getGroupById(@PathParam("group-id") String id) {
        GroupModel group = realm.getGroupById(id);

        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }

        if (!Organizations.canManageOrganizationGroup(session, group)) {
            throw ErrorResponse.error("Cannot manage organization related group via non Organization API.", Status.BAD_REQUEST);
        }

        return new GroupResource(realm, group, session, this.auth, adminEvent);
    }

    /**
     * Returns the groups counts.
     *
     * @return
     */
    @GET
    @NoCache
    @Path("count")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation( summary = "Returns the groups counts.")
    public Map<String, Long> getGroupCount(@QueryParam("search") String search,
                                           @QueryParam("top") @DefaultValue("false") boolean onlyTopGroups) {
        GroupPermissionEvaluator groupsEvaluator = auth.groups();
        groupsEvaluator.requireList();
        Long results;
        Map<String, Long> map = new HashMap<>();
        if (Objects.nonNull(search)) {
            results = realm.getGroupsCountByNameContaining(search);
        } else {
            results = realm.getGroupsCount(onlyTopGroups);
        }
        map.put("count", results);
        return map;
    }

    /**
     * create or add a top level realm groupSet or create child.  This will update the group and set the parent if it exists.  Create it and set the parent
     * if the group doesn't exist.
     *
     * @param rep
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Created"),
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation( summary = "create or add a top level realm groupSet or create child.",
        description = "This will update the group and set the parent if it exists. Create it and set the parent if the group doesn’t exist.")
    public Response addTopLevelGroup(GroupRepresentation rep) {
        auth.groups().requireManage();

        GroupModel child;
        Response.ResponseBuilder builder = Response.status(204);
        String groupName = rep.getName();

        if (ObjectUtil.isBlank(groupName)) {
            throw ErrorResponse.error("Group name is missing", Response.Status.BAD_REQUEST);
        }

        try {
            if (rep.getId() != null) {
                child = realm.getGroupById(rep.getId());
                if (child == null) {
                    throw new NotFoundException("Could not find child by id");
                }
                if (child.getParentId() != null) {
                    realm.moveGroup(child, null);
                }
                adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri());
            } else {
                child = realm.createGroup(groupName);
                GroupResource.updateGroup(rep, child, realm, session);
                URI uri = session.getContext().getUri().getAbsolutePathBuilder()
                        .path(child.getId()).build();
                builder.status(201).location(uri);

                rep.setId(child.getId());
                adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), child.getId());
            }
        } catch (ModelDuplicateException mde) {
            throw ErrorResponse.exists("Top level group named '" + groupName + "' already exists.");
        }

        adminEvent.representation(rep).success();
        return builder.build();
    }
}
