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
package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.HttpRequest;
import javax.ws.rs.NotAuthorizedException;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.ForbiddenException;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Providers;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientsManagementService {

    private static final Logger logger = Logger.getLogger(ClientsManagementService.class);

    private RealmModel realm;

    private EventBuilder event;

    @Context
    private HttpRequest request;

    @Context
    protected HttpHeaders headers;

    @Context
    private ClientConnection clientConnection;

    @Context
    protected Providers providers;

    @Context
    protected KeycloakSession session;

    public ClientsManagementService(RealmModel realm, EventBuilder event) {
        this.realm = realm;
        this.event = event;
    }

    public static UriBuilder clientsManagementBaseUrl(UriBuilder baseUriBuilder) {
        return baseUriBuilder.path(RealmsResource.class).path(RealmsResource.class, "getClientsManagementService");
    }

    public static UriBuilder registerNodeUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = clientsManagementBaseUrl(baseUriBuilder);
        return uriBuilder.path(ClientsManagementService.class, "registerNode");
    }

    public static UriBuilder unregisterNodeUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = clientsManagementBaseUrl(baseUriBuilder);
        return uriBuilder.path(ClientsManagementService.class, "unregisterNode");
    }

    /**
     * URL invoked by adapter to register new client cluster node. Each application cluster node will invoke this URL once it joins cluster
     *
     * @param authorizationHeader
     * @param formData
     * @return
     */
    @Path("register-node")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerNode(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader, final MultivaluedMap<String, String> formData) {
        if (!checkSsl()) {
            throw new ForbiddenException("HTTPS required");
        }

        event.event(EventType.REGISTER_NODE);

        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            throw new NotAuthorizedException("Realm not enabled");
        }

        ClientModel client = authorizeClient();
        String nodeHost = getClientClusterHost(formData);

        event.client(client).detail(Details.NODE_HOST, nodeHost);
        logger.debugf("Registering cluster host '%s' for client '%s'", nodeHost, client.getClientId());

        try {
            client.registerNode(nodeHost, Time.currentTime());
        } catch (RuntimeException e) {
            event.error(e.getMessage());
            throw e;
        }

        event.success();

        return Response.noContent().build();
    }


    /**
     * URL invoked by adapter to register new client cluster node. Each application cluster node will invoke this URL once it joins cluster
     *
     * @param authorizationHeader
     * @param formData
     * @return
     */
    @Path("unregister-node")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response unregisterNode(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader, final MultivaluedMap<String, String> formData) {
        if (!checkSsl()) {
            throw new ForbiddenException("HTTPS required");
        }

        event.event(EventType.UNREGISTER_NODE);

        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            throw new NotAuthorizedException("Realm not enabled");
        }

        ClientModel client = authorizeClient();
        String nodeHost = getClientClusterHost(formData);

        event.client(client).detail(Details.NODE_HOST, nodeHost);
        logger.debugf("Unregistering cluster host '%s' for client '%s'", nodeHost, client.getClientId());

        client.unregisterNode(nodeHost);

        event.success();

        return Response.noContent().build();
    }

    protected ClientModel authorizeClient() {
        ClientModel client = AuthorizeClientUtil.authorizeClient(session, event, null).getClient();

        if (client.isPublicClient()) {
            OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(OAuthErrorException.INVALID_CLIENT, "Public clients not allowed");
            event.error(Errors.INVALID_CLIENT);
            throw new BadRequestException("Public clients not allowed", javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST).entity(errorRep).type(MediaType.APPLICATION_JSON_TYPE).build());
        }

        return client;
    }

    protected String getClientClusterHost(MultivaluedMap<String, String> formData) {
        String clientClusterHost = formData.getFirst(AdapterConstants.CLIENT_CLUSTER_HOST);
        if (clientClusterHost == null || clientClusterHost.length() == 0) {
            OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation( OAuthErrorException.INVALID_REQUEST, "Client cluster host not specified");
            event.error(Errors.INVALID_CODE);
            throw new BadRequestException("Cluster host not specified", javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST).entity(errorRep).type(MediaType.APPLICATION_JSON_TYPE).build());
        }

        return clientClusterHost;
    }



    private boolean checkSsl() {
        if (session.getContext().getUri().getBaseUri().getScheme().equals("https")) {
            return true;
        } else {
            return !realm.getSslRequired().isRequired(clientConnection);
        }
    }
}
