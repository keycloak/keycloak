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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

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
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.utils.SearchQueryUtils;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
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

    public OrganizationGroupsResource(KeycloakSession session, OrganizationModel organization, AdminEventBuilder adminEvent) {
        this.realm = session == null ? null : session.getContext().getRealm();
        this.session = session;
        this.organizationProvider = session == null ? null : session.getProvider(OrganizationProvider.class);
        this.organization = organization;
        this.adminEvent = adminEvent.resource(ResourceType.ORGANIZATION_GROUP);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Creates a new top-level group in the organization",
        description = "Adds a new group as a top-level group to the organization. " +
                "If a group already exists or group with the same name already exists, an error response is returned.")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Created"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response addTopLevelGroup(GroupRepresentation rep) {
        try {
            // currently we do not support adding existing group
            if (rep.getId() != null) {
                if (session.groups().getGroupById(realm, rep.getId()) != null) {
                    throw ErrorResponse.exists("Group with the given id already exists");
                }
            }

            // name
            String groupName = rep.getName();

            if (ObjectUtil.isBlank(groupName)) {
                throw ErrorResponse.error("Group name is missing", Response.Status.BAD_REQUEST);
            }

            // create new org group
            GroupModel group = organizationProvider.createGroup(organization, groupName, null);

            // set description and attributes
            if (rep.getDescription() != null) {
                group.setDescription(group.getDescription());
            }
            if (rep.getAttributes() != null) {
                rep.getAttributes().forEach(group::setAttribute);
            }

            rep.setId(group.getId());

            adminEvent.operation(OperationType.CREATE)
                    .resourcePath(session.getContext().getUri())
                    .representation(rep)
                    .success();

            URI uri = session.getContext().getUri().getAbsolutePathBuilder()
                    .path(group.getId()).build();
            return Response.created(uri).build();

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
        @APIResponse(responseCode = "200", description = "OK")
    })
    public Stream<GroupRepresentation> getGroups(@QueryParam("search") String search,
                                                 @QueryParam("q") String searchQuery,
                                                 @QueryParam("exact") @DefaultValue("false") Boolean exact,
                                                 @QueryParam("first") Integer first,
                                                 @QueryParam("max") Integer max) {
        Stream<GroupModel> groups;
        if (Objects.nonNull(searchQuery)) {
            Map<String, String> attributes = SearchQueryUtils.getFields(searchQuery);
            groups = organizationProvider.searchGroupsByAttributes(organization, attributes, first, max);
        } else if (Objects.nonNull(search)) {
            groups = organizationProvider.searchGroupsByName(organization, search.trim(), exact, first, max);
        } else {
            groups = organizationProvider.getTopLevelGroups(organization, first, max);
        }
        return groups.map(ModelToRepresentation::groupToBriefRepresentation);
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
    public GroupRepresentation getGroupByPath(@PathParam("path") String path) {
        // todo path
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Path("{group-id}")
    public OrganizationGroupResource getGroupById(@PathParam("group-id") String id) {
        GroupModel group = realm.getGroupById(id);

        if (group == null) {
            throw ErrorResponse.error("Group does not exist", Response.Status.NOT_FOUND);
        }

        if (!Organizations.isOrganizationGroup(group) ||
                !Objects.equals(group.getOrganization().getId(), organization.getId())) {
            throw ErrorResponse.error("Group does not belong to the organization", Status.BAD_REQUEST);
        }

        return new OrganizationGroupResource(session, organizationProvider, organization, group, adminEvent);
    }
}
