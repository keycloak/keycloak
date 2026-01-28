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

import org.keycloak.representations.idm.RoleRepresentation;

/**
 * Sometimes its easier to just interact with roles by their ID instead of container/role-name
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RoleByIdResource {

    @Path("{role-id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    RoleRepresentation getRole(final @PathParam("role-id") String id);

    @Path("{role-id}")
    @DELETE
    void deleteRole(final @PathParam("role-id") String id);

    @Path("{role-id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void updateRole(final @PathParam("role-id") String id, RoleRepresentation rep);

    @Path("{role-id}/composites")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    void addComposites(final @PathParam("role-id") String id, List<RoleRepresentation> roles);

    @Path("{role-id}/composites")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Set<RoleRepresentation> getRoleComposites(@PathParam("role-id") String id);

    @Path("{role-id}/composites")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Set<RoleRepresentation> searchRoleComposites(@PathParam("role-id") String id,
                                                 @QueryParam("search") String search,
                                                 @QueryParam("first") Integer first,
                                                 @QueryParam("max") Integer max);

    @Path("{role-id}/composites/realm")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Set<RoleRepresentation> getRealmRoleComposites(@PathParam("role-id") String id);

    @Path("{role-id}/composites/clients/{clientUuid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Set<RoleRepresentation> getClientRoleComposites(@PathParam("role-id") String id, @PathParam("clientUuid") String clientUuid);

    @Path("{role-id}/composites")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    void deleteComposites(final @PathParam("role-id") String id, List<RoleRepresentation> roles);

}
