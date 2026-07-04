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
package org.keycloak.admin.client.resource;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * @since Keycloak server 26.7.
 */
public interface OrganizationRoleResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    RoleRepresentation toRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    Response update(RoleRepresentation roleRepresentation);

    @DELETE
    Response remove();

    @GET
    @Path("composites")
    @Produces(MediaType.APPLICATION_JSON)
    Set<RoleRepresentation> getRoleComposites();

    @GET
    @Path("composites")
    @Produces(MediaType.APPLICATION_JSON)
    Set<RoleRepresentation> searchRoleComposites(@QueryParam("search") String search,
                                                 @QueryParam("first") Integer first,
                                                 @QueryParam("max") Integer max);

    @GET
    @Path("composites/available")
    @Produces(MediaType.APPLICATION_JSON)
    List<RoleRepresentation> getAvailableRoleComposites(@QueryParam("source") String source,
                                                         @QueryParam("search") String search,
                                                         @QueryParam("first") Integer first,
                                                         @QueryParam("max") Integer max);

    @GET
    @Path("composites/effective")
    @Produces(MediaType.APPLICATION_JSON)
    List<RoleRepresentation> getEffectiveRoleComposites(@QueryParam("search") String search,
                                                         @QueryParam("first") Integer first,
                                                         @QueryParam("max") Integer max);

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

    @GET
    @Path("users")
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> getUserMembers();

    @GET
    @Path("users")
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> getUserMembers(@QueryParam("first") Integer firstResult,
                                             @QueryParam("max") Integer maxResults);

    @GET
    @Path("users")
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> getUserMembers(@QueryParam("briefRepresentation") Boolean briefRepresentation,
                                             @QueryParam("first") Integer firstResult,
                                             @QueryParam("max") Integer maxResults);

    @GET
    @Path("users/available")
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> getAvailableUserMembers(@QueryParam("search") String search,
                                                      @QueryParam("exact") Boolean exact,
                                                      @QueryParam("briefRepresentation") Boolean briefRepresentation,
                                                      @QueryParam("first") Integer firstResult,
                                                      @QueryParam("max") Integer maxResults);

    @POST
    @Path("users")
    @Consumes(MediaType.APPLICATION_JSON)
    void addUserMembers(List<UserRepresentation> users);

    @DELETE
    @Path("users")
    @Consumes(MediaType.APPLICATION_JSON)
    void deleteUserMembers(List<UserRepresentation> users);
}
