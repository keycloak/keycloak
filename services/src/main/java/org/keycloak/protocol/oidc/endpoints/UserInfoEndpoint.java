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
package org.keycloak.protocol.oidc.endpoints;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.common.ClientConnection;
import org.keycloak.OAuthErrorException;
import org.keycloak.RSATokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.Urls;

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
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pedroigor
 */
public class UserInfoEndpoint {

    @Context
    private HttpRequest request;

    @Context
    private HttpResponse response;

    @Context
    private UriInfo uriInfo;

    @Context
    private KeycloakSession session;

    @Context
    private ClientConnection clientConnection;

    private final TokenManager tokenManager;
    private final AppAuthManager appAuthManager;
    private final RealmModel realm;

    public UserInfoEndpoint(TokenManager tokenManager, RealmModel realm) {
        this.realm = realm;
        this.tokenManager = tokenManager;
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

    private Response issueUserInfo(String tokenString) {
        EventBuilder event = new EventBuilder(realm, session, clientConnection)
                .event(EventType.USER_INFO_REQUEST)
                .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN);

        AccessToken token = null;
        try {
            token = RSATokenVerifier.verifyToken(tokenString, realm.getPublicKey(), Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()), true, true);
        } catch (VerificationException e) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Token invalid: " + e.getMessage(), Status.FORBIDDEN);
        }

        UserSessionModel userSession = session.sessions().getUserSession(realm, token.getSessionState());
        ClientSessionModel clientSession = session.sessions().getClientSession(token.getClientSession());
        if (userSession == null || clientSession == null || !AuthenticationManager.isSessionValid(realm, userSession)) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Token invalid", Status.FORBIDDEN);
        }

        ClientModel clientModel = realm.getClientByClientId(token.getIssuedFor());
        UserModel userModel = userSession.getUser();
        AccessToken userInfo = new AccessToken();
        tokenManager.transformAccessToken(session, userInfo, realm, clientModel, userModel, userSession, clientSession);

        event
            .detail(Details.USERNAME, userModel.getUsername())
            .client(clientModel)
            .session(userSession)
            .user(userModel)
            .success();

        Map<String, Object> claims = new HashMap<String, Object>();
        claims.putAll(userInfo.getOtherClaims());
        claims.put("sub", userModel.getId());
        return Cors.add(request, Response.ok(claims)).auth().allowedOrigins(token).build();
    }

}
