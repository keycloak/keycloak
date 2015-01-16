/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.protocol.oidc;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.keycloak.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.UserClaimSet;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.EventsManager;
import org.keycloak.services.resources.Cors;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * @author pedroigor
 */
public class UserInfoService {

    @Context
    private HttpRequest request;

    @Context
    private HttpResponse response;

    @Context
    private KeycloakSession session;

    @Context
    private ClientConnection clientConnection;

    private final TokenManager tokenManager;
    private final AppAuthManager appAuthManager;
    private final OpenIDConnectService openIdConnectService;
    private final RealmModel realmModel;

    public UserInfoService(OpenIDConnectService openIDConnectService) {
        this.realmModel = openIDConnectService.getRealm();

        if (this.realmModel == null) {
            throw new RuntimeException("Null realm.");
        }

        this.tokenManager = openIDConnectService.getTokenManager();

        if (this.tokenManager == null) {
            throw new RuntimeException("Null token manager.");
        }

        this.openIdConnectService = openIDConnectService;
        this.appAuthManager = new AppAuthManager();
    }

    @Path("/")
    @OPTIONS
    @Produces(MediaType.APPLICATION_JSON)
    public Response issueUserInfoPreflight() {
        return Cors.add(this.request, Response.ok()).auth().preflight().build();
    }

    @Path("/")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response issueUserInfoGet(@Context final HttpHeaders headers) {
        String accessToken = this.appAuthManager.extractAuthorizationHeaderToken(headers);
        return issueUserInfo(accessToken);
    }

    @Path("/")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response issueUserInfoPost(@FormParam("access_token") String accessToken) {
        return issueUserInfo(accessToken);
    }

    private Response issueUserInfo(String token) {
        try {
            EventBuilder event = new EventsManager(this.realmModel, this.session, this.clientConnection).createEventBuilder()
                    .event(EventType.USER_INFO_REQUEST)
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN);

            Response validationResponse = this.openIdConnectService.validateAccessToken(token);

            if (!AccessToken.class.isInstance(validationResponse.getEntity())) {
                event.error(EventType.USER_INFO_REQUEST.name());
                return Response.fromResponse(validationResponse).status(Status.FORBIDDEN).build();
            }

            AccessToken accessToken = (AccessToken) validationResponse.getEntity();
            UserSessionModel userSession = session.sessions().getUserSession(realmModel, accessToken.getSessionState());
            ClientModel clientModel = realmModel.findClient(accessToken.getIssuedFor());
            UserModel userModel = userSession.getUser();
            UserClaimSet userInfo = new UserClaimSet();

            this.tokenManager.initClaims(userInfo, clientModel, userModel);

            event
                .detail(Details.USERNAME, userModel.getUsername())
                .client(clientModel)
                .session(userSession)
                .user(userModel)
                .success();

            return Cors.add(request, Response.ok(userInfo)).auth().allowedOrigins(accessToken).build();
        } catch (Exception e) {
            throw new UnauthorizedException("Could not retrieve user info.", e);
        }
    }

}
