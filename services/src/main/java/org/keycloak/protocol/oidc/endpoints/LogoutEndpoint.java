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
import org.keycloak.common.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.ErrorPage;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LogoutEndpoint {
    protected static ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    @Context
    private KeycloakSession session;

    @Context
    private ClientConnection clientConnection;

    @Context
    private HttpRequest request;

    @Context
    private HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    private TokenManager tokenManager;
    private RealmModel realm;
    private EventBuilder event;

    public LogoutEndpoint(TokenManager tokenManager, RealmModel realm, EventBuilder event) {
        this.tokenManager = tokenManager;
        this.realm = realm;
        this.event = event;
    }

    /**
     * Logout user session.  User must be logged in via a session cookie.
     *
     * @param redirectUri
     * @return
     */
    @GET
    @NoCache
    public Response logout(@QueryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM) String redirectUri, // deprecated
                           @QueryParam("id_token_hint") String encodedIdToken,
                           @QueryParam("post_logout_redirect_uri") String postLogoutRedirectUri,
                           @QueryParam("state") String state) {
        String redirect = postLogoutRedirectUri != null ? postLogoutRedirectUri : redirectUri;

        if (redirect != null) {
            String validatedUri = RedirectUtils.verifyRealmRedirectUri(uriInfo, redirect, realm);
            if (validatedUri == null) {
                event.event(EventType.LOGOUT);
                event.detail(Details.REDIRECT_URI, redirect);
                event.error(Errors.INVALID_REDIRECT_URI);
                return ErrorPage.error(session, Messages.INVALID_REDIRECT_URI);
            }
            redirect = validatedUri;
        }

        UserSessionModel userSession = null;
        boolean error = false;
        if (encodedIdToken != null) {
            try {
                IDToken idToken = tokenManager.verifyIDToken(realm, encodedIdToken);
                userSession = session.sessions().getUserSession(realm, idToken.getSessionState());
                if (userSession == null) {
                    error = true;
                }
            } catch (OAuthErrorException e) {
                error = true;
            }
            if (error) {
                event.event(EventType.LOGOUT);
                event.error(Errors.INVALID_TOKEN);
                return ErrorPage.error(session, Messages.SESSION_NOT_ACTIVE);
            }
        }

        // authenticate identity cookie, but ignore an access token timeout as we're logging out anyways.
        AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(session, realm, false);
        if (authResult != null) {
            userSession = userSession != null ? userSession : authResult.getSession();
            if (redirect != null) userSession.setNote(OIDCLoginProtocol.LOGOUT_REDIRECT_URI, redirect);
            if (state != null) userSession.setNote(OIDCLoginProtocol.LOGOUT_STATE_PARAM, state);
            userSession.setNote(AuthenticationManager.KEYCLOAK_LOGOUT_PROTOCOL, OIDCLoginProtocol.LOGIN_PROTOCOL);
            logger.debug("Initiating OIDC browser logout");
            Response response =  AuthenticationManager.browserLogout(session, realm, authResult.getSession(), uriInfo, clientConnection, headers);
            logger.debug("finishing OIDC browser logout");
            return response;
        } else if (userSession != null) { // non browser logout
            event.event(EventType.LOGOUT);
            AuthenticationManager.backchannelLogout(session, realm, userSession, uriInfo, clientConnection, headers, true);
            event.user(userSession.getUser()).session(userSession).success();
        }

        if (redirect != null) {
            UriBuilder uriBuilder = UriBuilder.fromUri(redirect);
            if (state != null) uriBuilder.queryParam(OIDCLoginProtocol.STATE_PARAM, state);
            return Response.status(302).location(uriBuilder.build()).build();
        } else {
            return Response.ok().build();
        }
    }

    /**
     * Logout a session via a non-browser invocation.  Similar signature to refresh token except there is no grant_type.
     * You must pass in the refresh token and
     * authenticate the client if it is not public.
     *
     * If the client is a confidential client
     * you must include the client-id and secret in an Basic Auth Authorization header.
     *
     * If the client is a public client, then you must include a "client_id" form parameter.
     *
     * returns 204 if successful, 400 if not with a json error response.
     *
     * @param authorizationHeader
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response logoutToken(final @HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        MultivaluedMap<String, String> form = request.getDecodedFormParameters();
        checkSsl();

        event.event(EventType.LOGOUT);

        ClientModel client = authorizeClient();
        String refreshToken = form.getFirst(OAuth2Constants.REFRESH_TOKEN);
        if (refreshToken == null) {
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "No refresh token", Response.Status.BAD_REQUEST);
        }
        try {
            RefreshToken token = tokenManager.verifyRefreshToken(realm, refreshToken, false);
            UserSessionModel userSessionModel = session.sessions().getUserSession(realm, token.getSessionState());
            if (userSessionModel != null) {
                logout(userSessionModel);
            }
        } catch (OAuthErrorException e) {
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(e.getError(), e.getDescription(), Response.Status.BAD_REQUEST);
        }
        return Cors.add(request, Response.noContent()).auth().allowedOrigins(client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    private void logout(UserSessionModel userSession) {
        AuthenticationManager.backchannelLogout(session, realm, userSession, uriInfo, clientConnection, headers, true);
        event.user(userSession.getUser()).session(userSession).success();
    }

    private ClientModel authorizeClient() {
        ClientModel client = AuthorizeClientUtil.authorizeClient(session, event).getClient();

        if (client.isBearerOnly()) {
            throw new ErrorResponseException("invalid_client", "Bearer-only not allowed", Response.Status.BAD_REQUEST);
        }

        return client;
    }

    private void checkSsl() {
        if (!uriInfo.getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new ErrorResponseException("invalid_request", "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

}
