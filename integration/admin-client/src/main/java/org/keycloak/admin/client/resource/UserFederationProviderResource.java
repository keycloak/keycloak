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
import java.util.Map;

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
import javax.ws.rs.core.Response;

import org.keycloak.representations.idm.UserFederationMapperRepresentation;
import org.keycloak.representations.idm.UserFederationMapperTypeRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.representations.idm.UserFederationSyncResultRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface UserFederationProviderResource {

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void update(UserFederationProviderRepresentation rep);


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    UserFederationProviderRepresentation toRepresentation();


    @DELETE
    void remove();


    @POST
    @Path("sync")
    @Produces(MediaType.APPLICATION_JSON)
    UserFederationSyncResultRepresentation syncUsers(@QueryParam("action") String action);


    @GET
    @Path("mapper-types")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, UserFederationMapperTypeRepresentation> getMapperTypes();


    @GET
    @Path("mappers")
    @Produces(MediaType.APPLICATION_JSON)
    List<UserFederationMapperRepresentation> getMappers();


    @POST
    @Path("mappers")
    @Consumes(MediaType.APPLICATION_JSON)
    Response addMapper(UserFederationMapperRepresentation mapper);


    @GET
    @Path("mappers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    UserFederationMapperRepresentation getMapperById(@PathParam("id") String id);


    @PUT
    @Path("mappers/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    void updateMapper(@PathParam("id") String id, UserFederationMapperRepresentation rep);


    @DELETE
    @Path("mappers/{id}")
    void removeMapper(@PathParam("id") String id);


    @POST
    @Path("mappers/{id}/sync")
    @Produces(MediaType.APPLICATION_JSON)
    UserFederationSyncResultRepresentation syncMapperData(@PathParam("id") String mapperId, @QueryParam("direction") String direction);
}
