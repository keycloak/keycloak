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

import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

public interface UsersResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> search(@QueryParam("username") String username,
                                           @QueryParam("firstName") String firstName,
                                           @QueryParam("lastName") String lastName,
                                             @QueryParam("email") String email,
                                             @QueryParam("first") Integer firstResult,
                                             @QueryParam("max") Integer maxResults);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> search(@QueryParam("search") String search,
                                           @QueryParam("first") Integer firstResult,
                                           @QueryParam("max") Integer maxResults);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(UserRepresentation userRepresentation);

    @Path("count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Integer count();

    @Path("{id}")
    UserResource get(@PathParam("id") String id);

    @Path("{id}")
    @DELETE
    Response delete(@PathParam("id") String id);
}
