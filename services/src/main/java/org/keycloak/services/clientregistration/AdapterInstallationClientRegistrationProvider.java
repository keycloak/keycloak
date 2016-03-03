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

import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AdapterInstallationClientRegistrationProvider implements ClientRegistrationProvider {

    private KeycloakSession session;
    private EventBuilder event;
    private ClientRegistrationAuth auth;

    public AdapterInstallationClientRegistrationProvider(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @Path("{clientId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("clientId") String clientId) {
        event.event(EventType.CLIENT_INFO);

        ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);

        if (auth.isAuthenticated()) {
            auth.requireView(client);
        } else {
            authenticateClient(client);
        }

        ClientManager clientManager = new ClientManager(new RealmManager(session));
        Object rep = clientManager.toInstallationRepresentation(session.getContext().getRealm(), client, session.getContext().getAuthServerUrl());

        event.client(client.getClientId()).success();
        return Response.ok(rep).build();
    }

    @Override
    public void setAuth(ClientRegistrationAuth auth) {
        this.auth = auth;
    }

    @Override
    public void setEvent(EventBuilder event) {
        this.event = event;
    }

    @Override
    public void close() {
    }

    private void authenticateClient(ClientModel client) {
        if (client.isPublicClient()) {
            return;
        }

        AuthenticationProcessor processor = AuthorizeClientUtil.getAuthenticationProcessor(session, event);

        Response response = processor.authenticateClient();
        if (response != null) {
            event.client(client.getClientId()).error(Errors.NOT_ALLOWED);
            throw new ForbiddenException();
        }

        ClientModel authClient = processor.getClient();
        if (client == null) {
            event.client(client.getClientId()).error(Errors.NOT_ALLOWED);
            throw new ForbiddenException();
        }

        if (!authClient.getClientId().equals(client.getClientId())) {
            event.client(client.getClientId()).error(Errors.NOT_ALLOWED);
            throw new ForbiddenException();
        }
    }

}
