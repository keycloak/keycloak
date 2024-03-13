/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.keycloak.organization.OrganizationRepresentation;
import org.keycloak.utils.StringUtil;

@Path("/")
public class OrganizationAdminResource {

    public OrganizationAdminResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(OrganizationRepresentation organizaiton) {
        return Response.status(Status.CREATED).build();
    }

    @Path("{id}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") String id) {
        if (StringUtil.isBlank(id)) {
            throw new BadRequestException();
        }
        return Response.noContent().build();
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<OrganizationRepresentation> get(@PathParam("id") String id) {
        OrganizationRepresentation organization = new OrganizationRepresentation();

        organization.setId("1");
        organization.setName("acme");

        return Stream.of(organization);
    }
}
