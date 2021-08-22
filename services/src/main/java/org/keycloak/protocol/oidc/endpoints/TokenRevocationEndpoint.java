/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import java.util.Objects;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.TokenRevocationStoreProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.TokenRevokeContext;
import org.keycloak.services.managers.UserSessionCrossDCManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.util.TokenUtil;

/**
 * @author <a href="mailto:yoshiyuki.tabata.jy@hitachi.com">Yoshiyuki Tabata</a>
 */
public class TokenRevocationEndpoint {
    private static final String PARAM_TOKEN = "token";

    @Context
    private KeycloakSession session;

    @Context
    private HttpRequest request;

    @Context
    private HttpHeaders headers;

    @Context
    private ClientConnection clientConnection;

    private MultivaluedMap<String, String> formParams;
    private ClientModel client;
    private RealmModel realm;
    private EventBuilder event;
    private Cors cors;
    private AccessToken token;
    private UserModel user;

    public TokenRevocationEndpoint(RealmModel realm, EventBuilder event) {
        this.realm = realm;
        this.event = event;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response revoke() {
        event.event(EventType.REVOKE_GRANT);

        cors = Cors.add(request).auth().allowedMethods("POST").auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);

        checkSsl();
        checkRealm();
        checkClient();

        formParams = request.getDecodedFormParameters();

        checkParameterDuplicated(formParams);

        try {
            session.clientPolicy().triggerOnEvent(new TokenRevokeContext(formParams));
        } catch (ClientPolicyException cpe) {
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), cpe.getErrorStatus());
        }

        checkToken();
        checkIssuedFor();
        checkUser();

        if (TokenUtil.TOKEN_TYPE_REFRESH.equals(token.getType()) || TokenUtil.TOKEN_TYPE_OFFLINE.equals(token.getType())) {
            revokeClient();
            event.detail(Details.REVOKED_CLIENT, client.getClientId());
        } else {
            revokeAccessToken();
            event.detail(Details.TOKEN_ID, token.getId());
        }

        event.success();

        session.getProvider(SecurityHeadersProvider.class).options().allowEmptyContentType();
        return cors.builder(Response.ok()).build();
    }

    @OPTIONS
    public Response preflight() {
        return Cors.add(request, Response.ok()).auth().preflight().allowedMethods("POST", "OPTIONS").build();
    }

    private void checkSsl() {
        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https")
            && realm.getSslRequired().isRequired(clientConnection)) {
            throw new CorsErrorResponseException(cors.allowAllOrigins(), OAuthErrorException.INVALID_REQUEST, "HTTPS required",
                Response.Status.FORBIDDEN);
        }
    }

    private void checkRealm() {
        if (!realm.isEnabled()) {
            throw new CorsErrorResponseException(cors.allowAllOrigins(), "access_denied", "Realm not enabled",
                Response.Status.FORBIDDEN);
        }
    }

    private void checkClient() {
        AuthorizeClientUtil.ClientAuthResult clientAuth = AuthorizeClientUtil.authorizeClient(session, event, cors);
        client = clientAuth.getClient();

        event.client(client);

        cors.allowedOrigins(session, client);

        if (client.isBearerOnly()) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Bearer-only not allowed",
                Response.Status.BAD_REQUEST);
        }
    }

    private void checkToken() {
        String encodedToken = formParams.getFirst(PARAM_TOKEN);

        if (encodedToken == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Token not provided",
                Response.Status.BAD_REQUEST);
        }

        token = session.tokens().decode(encodedToken, AccessToken.class);

        if (token == null) {
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.OK);
        }

        if (!(TokenUtil.TOKEN_TYPE_REFRESH.equals(token.getType()) || TokenUtil.TOKEN_TYPE_OFFLINE.equals(token.getType()) || TokenUtil.TOKEN_TYPE_BEARER.equals(token.getType()))) {
            event.error(Errors.INVALID_TOKEN_TYPE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.UNSUPPORTED_TOKEN_TYPE, "Unsupported token type",
                Response.Status.BAD_REQUEST);
        }
    }

    private void checkIssuedFor() {
        String issuedFor = token.getIssuedFor();
        if (issuedFor == null) {
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.OK);
        }

        if (!client.getClientId().equals(issuedFor)) {
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Unmatching clients",
                Response.Status.BAD_REQUEST);
        }
    }

    private void checkUser() {
        if (token.getSessionState() == null) {
            user = TokenManager.lookupUserFromStatelessToken(session, realm, token);
        } else {
            UserSessionModel userSession = new UserSessionCrossDCManager(session).getUserSessionWithClient(realm,
                    token.getSessionState(), false, client.getId());

            if (userSession == null) {
                userSession = new UserSessionCrossDCManager(session).getUserSessionWithClient(realm, token.getSessionState(), true,
                        client.getId());

                if (userSession == null) {
                    event.error(Errors.USER_SESSION_NOT_FOUND);
                    throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid token",
                            Response.Status.OK);
                }
            }

            user = userSession.getUser();
        }

        if (user == null) {
            event.error(Errors.USER_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.OK);
        }

        event.user(user);
    }

    private void checkParameterDuplicated(MultivaluedMap<String, String> formParams) {
        for (String key : formParams.keySet()) {
            if (formParams.get(key).size() != 1) {
                throw new CorsErrorResponseException(cors, Errors.INVALID_REQUEST, "duplicated parameter", Response.Status.BAD_REQUEST);
            }
        }
    }

    private void revokeClient() {
        session.users().revokeConsentForClient(realm, user.getId(), client.getId());
        if (TokenUtil.TOKEN_TYPE_OFFLINE.equals(token.getType())) {
            new UserSessionManager(session).revokeOfflineToken(user, client);
        }
        session.sessions().getUserSessionsStream(realm, user)
                .map(userSession -> userSession.getAuthenticatedClientSessionByClient(client.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()) // collect to avoid concurrent modification as dettachClientSession removes the user sessions.
                .forEach(clientSession -> {
                    UserSessionModel userSession = clientSession.getUserSession();
                    TokenManager.dettachClientSession(clientSession);

                    if (userSession != null) {
                        // TODO: Might need optimization to prevent loading client sessions from cache in getAuthenticatedClientSessions()
                        if (userSession.getAuthenticatedClientSessions().isEmpty()) {
                            session.sessions().removeUserSession(realm, userSession);
                        }
                    }
                });
    }

    private void revokeAccessToken() {
        TokenRevocationStoreProvider revocationStore = session.getProvider(TokenRevocationStoreProvider.class);
        int currentTime = Time.currentTime();
        long lifespanInSecs = Math.max(token.getExp() - currentTime, 10);
        revocationStore.putRevokedToken(token.getId(), lifespanInSecs);
    }
}
