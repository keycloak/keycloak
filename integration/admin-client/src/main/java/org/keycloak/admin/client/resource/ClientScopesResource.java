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
import org.keycloak.representations.idm.ClientScopeRepresentation;

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
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface ClientScopesResource {

    @Path("{id}")
    ClientScopeResource get(@PathParam("id") String id);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(ClientScopeRepresentation clientScopeRepresentation);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<ClientScopeRepresentation> findAll();


    /**
     * Generate new client scope for specified service client. The "Frontend" clients, who will use this client scope, will be able to
     * send their access token to authenticate against specified service client
     *
     * @param clientId Client ID of service client (typically bearer-only client)
     * @return
     */
    @Path("generate-audience-client-scope")
    @POST
    @NoCache
    Response generateAudienceClientScope(final @QueryParam("clientId") String clientId);


}
