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

package org.keycloak.services.resources.admin;

import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.clientregistration.ClientRegistrationTokenUtils;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * @resource Client Initial Access
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class ClientInitialAccessResource {

    private final AdminPermissionEvaluator auth;
    private final RealmModel realm;
    private final AdminEventBuilder adminEvent;

    protected KeycloakSession session;

    public ClientInitialAccessResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.auth = auth;
        this.realm = session.getContext().getRealm();
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT_INITIAL_ACCESS_MODEL);

    }

    /**
     * Create a new initial access token.
     *
     * @param config
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENT_INITIAL_ACCESS)
    @Operation( summary = "Create a new initial access token.")
    @APIResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = ClientInitialAccessCreatePresentation.class)))
    public Object create(ClientInitialAccessCreatePresentation config) {
        auth.clients().requireManage();

        int expiration = config.getExpiration() != null ? config.getExpiration() : 0;
        int count = config.getCount() != null ? config.getCount() : 1;
        if (expiration < 0) {
            OAuth2ErrorRepresentation error = new OAuth2ErrorRepresentation("Invalid value for expiration", "The expiration time interval cannot be less than 0");
            return Response.status(400).entity(error).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
        if (count < 0) {
            OAuth2ErrorRepresentation error = new OAuth2ErrorRepresentation("Invalid value for count", "The count cannot be less than 0");
            return Response.status(400).entity(error).type(MediaType.APPLICATION_JSON_TYPE).build();
        }

        ClientInitialAccessModel clientInitialAccessModel = session.realms().createClientInitialAccessModel(realm, expiration, count);

        adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), clientInitialAccessModel.getId()).representation(config).success();

        ClientInitialAccessPresentation rep = wrap(clientInitialAccessModel);

        String token = ClientRegistrationTokenUtils.createInitialAccessToken(session, realm, clientInitialAccessModel, config.getWebOrigins());
        rep.setToken(token);

        HttpResponse response = session.getContext().getHttpResponse();

        response.setStatus(Response.Status.CREATED.getStatusCode());
        response.addHeader(HttpHeaders.LOCATION, session.getContext().getUri().getAbsolutePathBuilder().path(clientInitialAccessModel.getId()).build().toString());

        return rep;
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENT_INITIAL_ACCESS)
    @Operation()
    public Stream<ClientInitialAccessPresentation> list() {
        auth.clients().requireView();

        return session.realms().listClientInitialAccessStream(realm).map(this::wrap);
    }

    @DELETE
    @Path("{id}")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENT_INITIAL_ACCESS)
    @Operation()
    public void delete(final @PathParam("id") String id) {
        auth.clients().requireManage();

        session.realms().removeClientInitialAccessModel(realm, id);
        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
    }

    private ClientInitialAccessPresentation wrap(ClientInitialAccessModel model) {
        ClientInitialAccessPresentation rep = new ClientInitialAccessPresentation();
        rep.setId(model.getId());
        rep.setTimestamp(model.getTimestamp());
        rep.setExpiration(model.getExpiration());
        rep.setCount(model.getCount());
        rep.setRemainingCount(model.getRemainingCount());
        return rep;
    }

}
