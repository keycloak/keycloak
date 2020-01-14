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

import org.jboss.resteasy.annotations.cache.NoCache;
import javax.ws.rs.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @resource Groups
 * @author Bill Burke
 */
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
     * Get group hierarchy.  Only name and ids are returned.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<GroupRepresentation> getGroups(@QueryParam("search") String search,
                                               @QueryParam("first") Integer firstResult,
                                               @QueryParam("max") Integer maxResults,
                                               @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation) {
        auth.groups().requireList();

        List<GroupRepresentation> results;

        if (Objects.nonNull(search)) {
            results = ModelToRepresentation.searchForGroupByName(realm, !briefRepresentation, search.trim(), firstResult, maxResults);
        } else if(Objects.nonNull(firstResult) && Objects.nonNull(maxResults)) {
            results = ModelToRepresentation.toGroupHierarchy(realm, !briefRepresentation, firstResult, maxResults);
        } else {
            results = ModelToRepresentation.toGroupHierarchy(realm, !briefRepresentation);
        }

        return results;
    }

    /**
     * Does not expand hierarchy.  Subgroups will not be set.
     *
     * @param id
     * @return
     */
    @Path("{id}")
    public GroupResource getGroupById(@PathParam("id") String id) {
        GroupModel group = realm.getGroupById(id);
        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }
        GroupResource resource =  new GroupResource(realm, group, session, this.auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
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
    public Map<String, Long> getGroupCount(@QueryParam("search") String search,
                                           @QueryParam("top") @DefaultValue("false") boolean onlyTopGroups) {
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
    public Response addTopLevelGroup(GroupRepresentation rep) {
        auth.groups().requireManage();

        List<GroupRepresentation> search = ModelToRepresentation.searchForGroupByName(realm, false, rep.getName(), 0, 1);
        if (search != null && !search.isEmpty() && Objects.equals(search.get(0).getName(), rep.getName())) {
            return ErrorResponse.exists("Top level group named '" + rep.getName() + "' already exists.");
        }

        GroupModel child;
        Response.ResponseBuilder builder = Response.status(204);
        if (rep.getId() != null) {
            child = realm.getGroupById(rep.getId());
            if (child == null) {
                throw new NotFoundException("Could not find child by id");
            }
            adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri());
        } else {
            child = realm.createGroup(rep.getName());
            GroupResource.updateGroup(rep, child);
            URI uri = session.getContext().getUri().getAbsolutePathBuilder()
                    .path(child.getId()).build();
            builder.status(201).location(uri);

            rep.setId(child.getId());
            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), child.getId());
        }
        realm.moveGroup(child, null);

        adminEvent.representation(rep).success();
        return builder.build();
    }
}
