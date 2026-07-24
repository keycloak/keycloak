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

package org.keycloak.admin.client.resource;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ManagementPermissionReference;
import org.keycloak.representations.idm.ManagementPermissionRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface GroupResource {

    /**
     * Enables or disables the fine grain permissions feature.
     * Returns the updated status of the server in the
     * {@link ManagementPermissionReference}.
     *
     * @param status status request to apply
     * @return permission reference indicating the updated status
     */
    @PUT
    @Path("/management/permissions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ManagementPermissionReference setPermissions(ManagementPermissionRepresentation status);

    /**
     * Returns indicator if the fine grain permissions are enabled or not.
     *
     * @return current representation of the permissions feature
     */
    @GET
    @Path("/management/permissions")
    @Produces(MediaType.APPLICATION_JSON)
    ManagementPermissionReference getPermissions();

    /**
     * Does not expand hierarchy.  Subgroups will not be set.
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    GroupRepresentation toRepresentation();

    /**
     * Update group
     *
     * @param rep
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void update(GroupRepresentation rep);

    @DELETE
    void remove();

    /**
     * Get the paginated list of subgroups belonging to this group.
     *
     * @param first the position of the first result to be returned.
     * @param max the maximum number of results that are to be returned.
     * @param briefRepresentation if {@code true}, each returned subgroup representation will only contain basic information
     *                           (id, name, path, and parentId). If {@code false}, the complete representations of the subgroups
     *                            are returned (include role mappings and attributes).
     */
    @GET
    @Path("children")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    List<GroupRepresentation> getSubGroups(@QueryParam("first") Integer first, @QueryParam("max") Integer max, @QueryParam("briefRepresentation") Boolean briefRepresentation);

    /**
     * Get the paginated list of subgroups belonging to this group.
     *
     * @param first the position of the first result to be returned.
     * @param max the maximum number of results that are to be returned.
     * @param briefRepresentation if {@code true}, each returned subgroup representation will only contain basic information
     *                           (id, name, path, and parentId). If {@code false}, the complete representations of the subgroups
     *                            are returned (include role mappings and attributes).
     * @param subGroupsCount if {@code true}, the count of subgroups is returned for each subgroup. Defaults to true. Parameter supported since Keycloak 26.3. For older versions, it is always true.
     */
    @GET
    @Path("children")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    List<GroupRepresentation> getSubGroups(@QueryParam("first") Integer first, @QueryParam("max") Integer max,
                                           @QueryParam("briefRepresentation") Boolean briefRepresentation,
                                           @QueryParam("subGroupsCount") Boolean subGroupsCount);

    /**
     * Get the paginated list of subgroups belonging to this group, filtered according to the specified parameters.
     *
     * @param search a {@code String} representing either an exact group name or a partial name. If empty or {@code null}
     *              then all subgroups of this group are returned. Parameter available since Keycloak server 25. Will be ignored on older Keycloak versions with the default value null.
     * @param exact if {@code true}, the subgroups will be searched using exact match for the {@code search} param. If false
     *              or {@code null}, the method returns all subgroups that partially match the specified name. Parameter available since Keycloak server 25. Will be ignored on older Keycloak versions with the default value null.
     * @param first the position of the first result to be returned.
     * @param max the maximum number of results that are to be returned.
     * @param briefRepresentation if {@code true}, each returned subgroup representation will only contain basic information
     *                           (id, name, path, and parentId). If {@code false}, the complete representations of the subgroups
     *                            are returned (including role mappings and attributes).
     */
    @GET
    @Path("children")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    List<GroupRepresentation> getSubGroups(
            @QueryParam("search") String search,
            @QueryParam("exact") Boolean exact,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max,
            @QueryParam("briefRepresentation") Boolean briefRepresentation);

    /**
     * Get the paginated list of subgroups belonging to this group, filtered according to the specified parameters.
     *
     * @param search a {@code String} representing either an exact group name or a partial name. If empty or {@code null}
     *              then all subgroups of this group are returned. Parameter available since Keycloak server 25. Will be ignored on older Keycloak versions with the default value null.
     * @param exact if {@code true}, the subgroups will be searched using exact match for the {@code search} param. If false
     *              or {@code null}, the method returns all subgroups that partially match the specified name. Parameter available since Keycloak server 25. Will be ignored on older Keycloak versions with the default value null.
     * @param first the position of the first result to be returned.
     * @param max the maximum number of results that are to be returned.
     * @param briefRepresentation if {@code true}, each returned subgroup representation will only contain basic information
     *                           (id, name, path, and parentId). If {@code false}, the complete representations of the subgroups
     *                            are returned (including role mappings and attributes).
     * @param subGroupsCount if {@code true}, the count of subgroups is returned for each subgroup. Defaults to true. Parameter supported since Keycloak 26.3. For older versions, it is always true.
     */
    @GET
    @Path("children")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    List<GroupRepresentation> getSubGroups(
            @QueryParam("search") String search,
            @QueryParam("exact") Boolean exact,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max,
            @QueryParam("briefRepresentation") Boolean briefRepresentation,
            @QueryParam("subGroupsCount") Boolean subGroupsCount);

    /**
     * Set or create child.  This will just set the parent if it exists.  Create it and set the parent
     * if the group doesn't exist.
     *
     * @param rep
     */
    @POST
    @Path("children")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Response subGroup(GroupRepresentation rep);


    @Path("role-mappings")
    RoleMappingResource roles();

    /**
     * Get users
     * <p/>
     * Returns a list of all users in group.
     *
     * @return  Returns a max size of 100 users
     */
    @GET
    @Path("/members")
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> members();

    /**
     * Get users
     * <p/>
     * Returns a list of users, filtered according to query parameters
     *
     * @param firstResult Pagination offset
     * @param maxResults  Pagination size
     * @return
     */
    @GET
    @Path("/members")
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> members(@QueryParam("first") Integer firstResult,
                                     @QueryParam("max") Integer maxResults);

    /**
     * Get users
     * <p/>
     * Returns a list of users, filtered according to query parameters
     *
     * @param firstResult Pagination offset
     * @param maxResults  Pagination size
     * @param briefRepresentation Only return basic information (only guaranteed to return id, username, created, first and last name,
     *      email, enabled state, email verification state, federation link, and access.
     *      Note that it means that namely user attributes, required actions, and not before are not returned.)
     * @return
     */
    @GET
    @Path("/members")
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> members(@QueryParam("first") Integer firstResult,
                                     @QueryParam("max") Integer maxResults,
                                     @QueryParam("briefRepresentation") Boolean briefRepresentation);
}
