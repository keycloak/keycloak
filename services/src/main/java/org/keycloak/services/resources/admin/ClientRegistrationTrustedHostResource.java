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

import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientRegistrationTrustedHostModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ClientRegistrationTrustedHostRepresentation;
import org.keycloak.services.ErrorResponse;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientRegistrationTrustedHostResource {

    private final RealmAuth auth;
    private final RealmModel realm;
    private final AdminEventBuilder adminEvent;

    @Context
    protected KeycloakSession session;

    @Context
    protected UriInfo uriInfo;

    public ClientRegistrationTrustedHostResource(RealmModel realm, RealmAuth auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT_REGISTRATION_TRUSTED_HOST_MODEL);

        auth.init(RealmAuth.Resource.CLIENT);
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
    public Response create(ClientRegistrationTrustedHostRepresentation config) {
        auth.requireManage();

        if (config.getHostName() == null) {
            return ErrorResponse.error("hostName not provided in config", Response.Status.BAD_REQUEST);
        }

        int count = config.getCount() != null ? config.getCount() : 1;

        try {
            ClientRegistrationTrustedHostModel clientRegTrustedHostModel = session.sessions().createClientRegistrationTrustedHostModel(realm, config.getHostName(), count);

            adminEvent.operation(OperationType.CREATE).resourcePath(uriInfo, clientRegTrustedHostModel.getHostName()).representation(config).success();

            return Response.created(uriInfo.getAbsolutePathBuilder().path(clientRegTrustedHostModel.getHostName()).build()).build();
        } catch (ModelDuplicateException mde) {
            return ErrorResponse.exists(mde.getMessage());
        }
    }

    /**
     * Update a new initial access token.
     *
     * @param config
     * @return
     */
    @PUT
    @Path("{hostname}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(final @PathParam("hostname") String hostName, ClientRegistrationTrustedHostRepresentation config) {
        auth.requireManage();

        if (config.getHostName() == null || !hostName.equals(config.getHostName())) {
            return ErrorResponse.error("hostName not provided in config or not compatible", Response.Status.BAD_REQUEST);
        }

        if (config.getCount() == null) {
            return ErrorResponse.error("count needs to be available", Response.Status.BAD_REQUEST);
        }

        if (config.getRemainingCount() != null && config.getRemainingCount() > config.getCount()) {
            return ErrorResponse.error("remainingCount can't be bigger than count", Response.Status.BAD_REQUEST);
        }

        ClientRegistrationTrustedHostModel hostModel = session.sessions().getClientRegistrationTrustedHostModel(realm, config.getHostName());
        if (hostModel == null) {
            return ErrorResponse.error("hostName record not found", Response.Status.NOT_FOUND);
        }

        hostModel.setCount(config.getCount());
        hostModel.setRemainingCount(config.getRemainingCount());

        adminEvent.operation(OperationType.UPDATE).resourcePath(uriInfo).representation(config).success();
        return Response.noContent().build();
    }

    /**
     * Get an initial access token.
     *
     * @param hostName
     * @return
     */
    @GET
    @Path("{hostname}")
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRegistrationTrustedHostRepresentation getConfig(final @PathParam("hostname") String hostName) {
        auth.requireView();

        ClientRegistrationTrustedHostModel hostModel = session.sessions().getClientRegistrationTrustedHostModel(realm, hostName);
        if (hostModel == null) {
            throw new NotFoundException("hostName record not found");
        }

        return wrap(hostModel);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ClientRegistrationTrustedHostRepresentation> list() {
        auth.requireView();

        List<ClientRegistrationTrustedHostModel> models = session.sessions().listClientRegistrationTrustedHosts(realm);
        List<ClientRegistrationTrustedHostRepresentation> reps = new LinkedList<>();
        for (ClientRegistrationTrustedHostModel m : models) {
            ClientRegistrationTrustedHostRepresentation r = wrap(m);
            reps.add(r);
        }
        return reps;
    }

    @DELETE
    @Path("{hostname}")
    public void delete(final @PathParam("hostname") String hostName) {
        auth.requireManage();

        session.sessions().removeClientRegistrationTrustedHostModel(realm, hostName);
        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();
    }

    private ClientRegistrationTrustedHostRepresentation wrap(ClientRegistrationTrustedHostModel model) {
        return ClientRegistrationTrustedHostRepresentation.create(model.getHostName(), model.getCount(), model.getRemainingCount());
    }
}
