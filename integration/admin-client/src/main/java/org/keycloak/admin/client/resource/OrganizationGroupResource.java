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

import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;

/**
 * Resource for managing a single organization group.
 */
public interface OrganizationGroupResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    GroupRepresentation toRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    Response update(GroupRepresentation rep);

    @DELETE
    void delete();

    @GET
    @Path("children")
    @Produces(MediaType.APPLICATION_JSON)
    List<GroupRepresentation> getSubGroups(
            @QueryParam("search") String search,
            @QueryParam("exact") Boolean exact,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max
    );

    @POST
    @Path("children")
    @Consumes(MediaType.APPLICATION_JSON)
    Response addSubGroup(GroupRepresentation rep);

    @GET
    @Path("members")
    @Produces(MediaType.APPLICATION_JSON)
    List<MemberRepresentation> getMembers(
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max,
            @QueryParam("briefRepresentation") Boolean briefRepresentation
    );

    @PUT
    @Path("members/{userId}")
    void addMember(@PathParam("userId") String userId);

    @DELETE
    @Path("members/{userId}")
    void removeMember(@PathParam("userId") String userId);
}
