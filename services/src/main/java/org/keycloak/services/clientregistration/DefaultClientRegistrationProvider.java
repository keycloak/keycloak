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

package org.keycloak.services.clientregistration;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultClientRegistrationProvider extends AbstractClientRegistrationProvider {

    public DefaultClientRegistrationProvider(KeycloakSession session) {
        super(session);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDefault(ClientRepresentation client) {
        DefaultClientRegistrationContext context = new DefaultClientRegistrationContext(session, client, this);
        client = create(context);
        validateClient(client, true);
        URI uri = session.getContext().getUri().getAbsolutePathBuilder().path(client.getClientId()).build();
        return Response.created(uri).entity(client).build();
    }

    @GET
    @Path("{clientId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDefault(@PathParam("clientId") String clientId) {
        ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);
        ClientRepresentation clientRepresentation = get(client);
        return Response.ok(clientRepresentation).build();
    }

    @PUT
    @Path("{clientId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDefault(@PathParam("clientId") String clientId, ClientRepresentation client) {
        DefaultClientRegistrationContext context = new DefaultClientRegistrationContext(session, client, this);
        client = update(clientId, context);
        validateClient(client, false);
        return Response.ok(client).build();
    }

    @DELETE
    @Path("{clientId}")
    public void deleteDefault(@PathParam("clientId") String clientId) {
        delete(clientId);
    }
}
