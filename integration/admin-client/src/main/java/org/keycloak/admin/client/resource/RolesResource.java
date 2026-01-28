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
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.representations.idm.RoleRepresentation;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface RolesResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<RoleRepresentation> list();
    
    /**
     * @param briefRepresentation if false, return roles with their attributes
     * @return A list containing all roles.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<RoleRepresentation> list(@QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation);
    
    /**
     * Get roles by pagination params.
     * @param search max number of occurrences
     * @param first index of the first element
     * @param max max number of occurrences
     * @return A list containing the slice of all roles.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<RoleRepresentation> list(@QueryParam("first") Integer firstResult,
                                  @QueryParam("max") Integer maxResults);
    
    /**
     * Get roles by pagination params.
     * @param first index of the first element
     * @param max max number of occurrences
     * @param briefRepresentation if false, return roles with their attributes
     * @return A list containing the slice of all roles.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<RoleRepresentation> list(@QueryParam("first") Integer firstResult,
                                  @QueryParam("max") Integer maxResults,
                                  @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation);
    
    /**
     * Get roles by pagination params.
     * @param search max number of occurrences
     * @param briefRepresentation if false, return roles with their attributes
     * @return A list containing the slice of all roles.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<RoleRepresentation> list(@QueryParam("search") @DefaultValue("") String search,
                                  @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation);
    
    /**
     * Get roles by pagination params.
     * @param search max number of occurrences
     * @param first index of the first element
     * @param max max number of occurrences
     * @return A list containing the slice of all roles.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<RoleRepresentation> list(@QueryParam("search") @DefaultValue("") String search,
                                  @QueryParam("first") Integer firstResult,
                                  @QueryParam("max") Integer maxResults);
    
    /**
     * Get roles by pagination params.
     * @param search max number of occurrences
     * @param first index of the first element
     * @param max max number of occurrences
     * @param briefRepresentation if false, return roles with their attributes
     * @return A list containing the slice of all roles.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<RoleRepresentation> list(@QueryParam("search") @DefaultValue("") String search,
                                  @QueryParam("first") Integer firstResult,
                                  @QueryParam("max") Integer maxResults,
                                  @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    void create(RoleRepresentation roleRepresentation);

    @Path("{roleName}")
    RoleResource get(@PathParam("roleName") String roleName);

    @Path("{role-name}")
    @DELETE
    void deleteRole(final @PathParam("role-name") String roleName);

}
