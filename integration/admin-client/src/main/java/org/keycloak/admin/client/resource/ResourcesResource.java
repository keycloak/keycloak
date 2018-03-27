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

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface ResourcesResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response create(ResourceRepresentation resource);

    @Path("{id}")
    ResourceResource resource(@PathParam("id") String id);

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    List<ResourceRepresentation> find(@QueryParam("name") String name,
                  @QueryParam("uri") String uri,
                  @QueryParam("owner") String owner,
                  @QueryParam("type") String type,
                  @QueryParam("scope") String scope,
                  @QueryParam("first") Integer firstResult,
                  @QueryParam("max") Integer maxResult);

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    List<ResourceRepresentation> findByName(@QueryParam("name") String name);

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    List<ResourceRepresentation> findByName(@QueryParam("name") String name, @QueryParam("owner") String owner);

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    List<ResourceRepresentation> resources();
}
