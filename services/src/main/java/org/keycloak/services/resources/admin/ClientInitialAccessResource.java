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

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.services.clientregistration.ClientRegistrationTokenUtils;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;

/**
 * @resource Client Initial Access
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientInitialAccessResource {

    private final AdminPermissionEvaluator auth;
    private final RealmModel realm;
    private final AdminEventBuilder adminEvent;

    @Context
    protected KeycloakSession session;

    public ClientInitialAccessResource(RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
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
    public ClientInitialAccessPresentation create(ClientInitialAccessCreatePresentation config, @Context final HttpServletResponse response) {
        auth.clients().requireManage();

        int expiration = config.getExpiration() != null ? config.getExpiration() : 0;
        int count = config.getCount() != null ? config.getCount() : 1;

        ClientInitialAccessModel clientInitialAccessModel = session.realms().createClientInitialAccessModel(realm, expiration, count);

        adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), clientInitialAccessModel.getId()).representation(config).success();

        ClientInitialAccessPresentation rep = wrap(clientInitialAccessModel);

        String token = ClientRegistrationTokenUtils.createInitialAccessToken(session, realm, clientInitialAccessModel);
        rep.setToken(token);

        response.setStatus(Response.Status.CREATED.getStatusCode());
        response.setHeader(HttpHeaders.LOCATION, session.getContext().getUri().getAbsolutePathBuilder().path(clientInitialAccessModel.getId()).build().toString());

        return rep;
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ClientInitialAccessPresentation> list() {
        auth.clients().requireView();

        List<ClientInitialAccessModel> models = session.realms().listClientInitialAccess(realm);
        List<ClientInitialAccessPresentation> reps = new LinkedList<>();
        for (ClientInitialAccessModel m : models) {
            ClientInitialAccessPresentation r = wrap(m);
            reps.add(r);
        }
        return reps;
    }

    @DELETE
    @Path("{id}")
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
