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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.TokenRevokeContext;
import org.keycloak.services.clientpolicy.context.TokenRevokeResponseContext;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.util.UserSessionUtil;
import org.keycloak.util.TokenUtil;

/**
 * @author <a href="mailto:yoshiyuki.tabata.jy@hitachi.com">Yoshiyuki Tabata</a>
 */
public class TokenRevocationEndpoint {
    public static final String PARAM_TOKEN = "token";

    private final KeycloakSession session;

    private final HttpRequest request;

    private final ClientConnection clientConnection;

    private MultivaluedMap<String, String> formParams;
    private ClientModel client;
    private final RealmModel realm;
    private final EventBuilder event;
    private Cors cors;
    private AccessToken token;
    private UserModel user;

    public TokenRevocationEndpoint(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
        this.realm = session.getContext().getRealm();
        this.event = event;
        this.request = session.getContext().getHttpRequest();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response revoke() {
        event.event(EventType.REVOKE_GRANT);

        cors = Cors.builder().auth().allowedMethods("POST").auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);

        checkSsl();
        checkRealm();
        checkClient();

        formParams = request.getDecodedFormParameters();

        checkParameterDuplicated(formParams);

        try {
            session.clientPolicy().triggerOnEvent(new TokenRevokeContext(formParams));
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), cpe.getErrorStatus());
        }

        checkToken();
        checkIssuedFor();
        checkUser();

        if (TokenUtil.TOKEN_TYPE_REFRESH.equals(token.getType()) || TokenUtil.TOKEN_TYPE_OFFLINE.equals(token.getType())) {
            revokeClientSession();
            event.detail(Details.REVOKED_CLIENT, client.getClientId());
            event.session(token.getSessionId());
            event.detail(Details.REFRESH_TOKEN_ID, token.getId());
            event.detail(Details.REFRESH_TOKEN_TYPE, token.getType());
        } else {
            revokeAccessToken();
            event.detail(Details.TOKEN_ID, token.getId());
        }

        event.success();

        try {
            session.clientPolicy().triggerOnEvent(new TokenRevokeResponseContext(formParams));
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), cpe.getErrorStatus());
        }

        session.getProvider(SecurityHeadersProvider.class).options().allowEmptyContentType();
        return cors.add(Response.ok());
    }

    @OPTIONS
    public Response preflight() {
        return Cors.builder().auth().preflight().allowedMethods("POST", "OPTIONS").add(Response.ok());
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
            event.detail(Details.REASON, "Token not provided");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Token not provided",
                Response.Status.BAD_REQUEST);
        }

        token = session.tokens().decode(encodedToken, AccessToken.class);

        if (token == null) {
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.OK);
        }

        if (!(TokenUtil.TOKEN_TYPE_REFRESH.equals(token.getType()) || TokenUtil.TOKEN_TYPE_OFFLINE.equals(token.getType()) || TokenUtil.TOKEN_TYPE_BEARER.equals(token.getType())|| TokenUtil.TOKEN_TYPE_DPOP.equals(token.getType()))) {
            event.detail(Details.REASON, "Unsupported token type");
            event.error(Errors.INVALID_TOKEN_TYPE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.UNSUPPORTED_TOKEN_TYPE, "Unsupported token type",
                Response.Status.BAD_REQUEST);
        }
    }

    private void checkIssuedFor() {
        String issuedFor = token.getIssuedFor();
        if (issuedFor == null) {
            event.detail(Details.REASON, "Issued for not set");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.OK);
        }

        if (!client.getClientId().equals(issuedFor)) {
            event.detail(Details.REASON, "Unmatching clients");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Unmatching clients",
                Response.Status.BAD_REQUEST);
        }
    }

    private void checkUser() {
        UserSessionUtil.UserSessionValidationResult validationResult = UserSessionUtil.findValidSessionForAccessToken(
                session, realm, token, client, (UserSessionModel t) -> {});
        if (validationResult.getError() != null) {
            event.error(validationResult.getError());
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.OK);
        }

        user = validationResult.getUserSession().getUser();

        if (user == null) {
            event.error(Errors.USER_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.OK);
        }

        event.user(user);
    }

    private void checkParameterDuplicated(MultivaluedMap<String, String> formParams) {
        for (List<String> strings : formParams.values()) {
            if (strings.size() != 1) {
                throw new CorsErrorResponseException(cors, Errors.INVALID_REQUEST, "duplicated parameter", Response.Status.BAD_REQUEST);
            }
        }
    }

    private void revokeClientSession() {
        if (TokenUtil.TOKEN_TYPE_OFFLINE.equals(token.getType())) {
            UserSessionModel userSession = session.sessions().getOfflineUserSession(realm, token.getSessionId());
            if (userSession != null) {
                new UserSessionManager(session).removeClientFromOfflineUserSession(realm, userSession, client, user);
            }
        }
        // Always remove "online" session as well if exists to make sure that issued access-tokens are revoked as well
        UserSessionModel userSession = session.sessions().getUserSession(realm, token.getSessionId());
        if (userSession != null) {
            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
            if (clientSession != null) {
                TokenManager.dettachClientSession(clientSession);

                revokeTokenExchangeSession(userSession);

                // TODO: Might need optimization to prevent loading client sessions from cache in getAuthenticatedClientSessions()
                if (userSession.getAuthenticatedClientSessions().isEmpty()) {
                    session.sessions().removeUserSession(realm, userSession);
                }
            }
        }
    }

    private void revokeAccessToken() {
        SingleUseObjectProvider singleUseStore = session.singleUseObjects();
        int currentTime = Time.currentTime();
        long lifespanInSecs = Math.max(token.getExp() - currentTime + 1, 10);
        singleUseStore.put(token.getId() + SingleUseObjectProvider.REVOKED_KEY, lifespanInSecs, Collections.emptyMap());
        revokeTokenExchangeSession();
    }

    private void revokeTokenExchangeSession() {
        if (token.getSessionId() != null) {
            UserSessionModel userSession = session.sessions().getUserSession(realm, token.getSessionId());
            if (userSession != null) {
                revokeTokenExchangeSession(userSession);
            }
        }
    }

    private void revokeTokenExchangeSession(UserSessionModel userSession) {
        Map<String, AuthenticatedClientSessionModel> clientSessionModelMap = userSession.getAuthenticatedClientSessions();
        List<String> revokedClients = new ArrayList<>();
        clientSessionModelMap.forEach((key, clientSessionModel) -> {
            if (clientSessionModel.getNote(Constants.TOKEN_EXCHANGE_SUBJECT_CLIENT + token.getIssuedFor()) != null) {
                revokedClients.add(clientSessionModel.getClient().getClientId());
                TokenManager.dettachClientSession(clientSessionModel);
            }
        });
        if (!revokedClients.isEmpty()) {
            event.detail(Details.TOKEN_EXCHANGE_REVOKED_CLIENTS, String.join(",", revokedClients));
        }
    }
}
