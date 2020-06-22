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

import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ManagementPermissionReference;
import org.keycloak.representations.idm.ManagementPermissionRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Set;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface RoleResource {

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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    RoleRepresentation toRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void update(RoleRepresentation roleRepresentation);

    @DELETE
    void remove();

    @GET
    @Path("composites")
    @Produces(MediaType.APPLICATION_JSON)
    Set<RoleRepresentation> getRoleComposites();

    @GET
    @Path("composites/realm")
    @Produces(MediaType.APPLICATION_JSON)
    Set<RoleRepresentation> getRealmRoleComposites();

    @GET
    @Path("composites/clients/{clientUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    Set<RoleRepresentation> getClientRoleComposites(@PathParam("clientUuid") String clientUuid);

    @POST
    @Path("composites")
    @Consumes(MediaType.APPLICATION_JSON)
    void addComposites(List<RoleRepresentation> rolesToAdd);

    @DELETE
    @Path("composites")
    @Consumes(MediaType.APPLICATION_JSON)
    void deleteComposites(List<RoleRepresentation> rolesToRemove);

    /**
     * Get role members
     * <p/>
     * Returns users that have the given role
     *
     * @return a list of users with the given role
     */
    @GET
    @Path("users")
    @Produces(MediaType.APPLICATION_JSON)
    Set<UserRepresentation> getRoleUserMembers();

    /**
     * Get role members
     * <p/>
     * Returns users that have the given role, paginated according to the query parameters
     *
     * @param firstResult Pagination offset
     * @param maxResults  Pagination size
     * @return a list of users with the given role
     */
    @GET
    @Path("users")
    @Produces(MediaType.APPLICATION_JSON)
    Set<UserRepresentation> getRoleUserMembers(@QueryParam("first") Integer firstResult,
                                               @QueryParam("max") Integer maxResults);
    
    /**
     * Get role groups
     * <p/>
     * Returns groups that have the given role
     *
     * @return a list of groups with the given role
     */
    @GET
    @Path("groups")
    @Produces(MediaType.APPLICATION_JSON)
    Set<GroupRepresentation> getRoleGroupMembers();

    /**
     * Get role groups
     * <p/>
     * Returns groups that have the given role, paginated according to the query parameters
     *
     * @param firstResult Pagination offset
     * @param maxResults  Pagination size
     * @return a list of groups with the given role
     */
    @GET
    @Path("groups")
    @Produces(MediaType.APPLICATION_JSON)
    Set<GroupRepresentation> getRoleGroupMembers(@QueryParam("first") Integer firstResult,
                                               @QueryParam("max") Integer maxResults);
}
