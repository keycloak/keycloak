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
import java.util.Optional;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.ClientRepresentation;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface ClientsResource {

    @Path("{id}")
    ClientResource get(@PathParam("id") String id);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(ClientRepresentation clientRepresentation);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<ClientRepresentation> findAll();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<ClientRepresentation> findAll(@QueryParam("viewableOnly") boolean viewableOnly);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<ClientRepresentation> findAll(@QueryParam("clientId") String clientId,
                                                 @QueryParam("viewableOnly") Boolean viewableOnly,
                                                 @QueryParam("search") Boolean search,
                                                 @QueryParam("first") Integer firstResult,
                                                 @QueryParam("max") Integer maxResults);

    /** @deprecated use {@link #findClientByClientId(String)} */
    @Deprecated
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<ClientRepresentation> findByClientId(@QueryParam("clientId") String clientId);

    default Optional<ClientRepresentation> findClientByClientId(String clientId) {
        List<ClientRepresentation> list = findByClientId(clientId);
        if (list.isEmpty()) {
            return Optional.<ClientRepresentation>empty();
        }
        return Optional.of(list.get(0));
    }

    default ClientResource getByClientId(String id) {
        return get(findClientByClientId(id).map(ClientRepresentation::getId)
                .orElseThrow(() -> new NotFoundException("client not found " + id)));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<ClientRepresentation> query(@QueryParam("q") String searchQuery);

    @Path("{id}")
    @DELETE
    Response delete(@PathParam("id") String id);

}
