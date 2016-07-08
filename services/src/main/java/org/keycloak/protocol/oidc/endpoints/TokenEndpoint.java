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

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.Urls;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TokenEndpoint {

    // Flag if code was already exchanged for token
    private static final String CODE_EXCHANGED = "CODE_EXCHANGED";

    private static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;
    private MultivaluedMap<String, String> formParams;
    private ClientModel client;
    private Map<String, String> clientAuthAttributes;

    private enum Action {
        AUTHORIZATION_CODE, REFRESH_TOKEN, PASSWORD, CLIENT_CREDENTIALS
    }

    @Context
    private KeycloakSession session;

    @Context
    private HttpRequest request;

    @Context
    private HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    @Context
    private ClientConnection clientConnection;

    private final TokenManager tokenManager;
    private final RealmModel realm;
    private final EventBuilder event;

    private Action action;

    private String grantType;

    public TokenEndpoint(TokenManager tokenManager, RealmModel realm, EventBuilder event) {
        this.tokenManager = tokenManager;
        this.realm = realm;
        this.event = event;
    }

    @POST
    public Response build() {
        formParams = request.getDecodedFormParameters();
        grantType = formParams.getFirst(OIDCLoginProtocol.GRANT_TYPE_PARAM);

        checkSsl();
        checkRealm();
        checkGrantType();
        checkClient();

        switch (action) {
            case AUTHORIZATION_CODE:
                return buildAuthorizationCodeAccessTokenResponse();
            case REFRESH_TOKEN:
                return buildRefreshToken();
            case PASSWORD:
                return buildResourceOwnerPasswordCredentialsGrant();
            case CLIENT_CREDENTIALS:
                return buildClientCredentialsGrant();
        }

        throw new RuntimeException("Unknown action " + action);
    }

    @Path("introspect")
    public Object introspect() {
        TokenIntrospectionEndpoint tokenIntrospectionEndpoint = new TokenIntrospectionEndpoint(this.realm, this.event);

        ResteasyProviderFactory.getInstance().injectProperties(tokenIntrospectionEndpoint);

        return tokenIntrospectionEndpoint;
    }

    @OPTIONS
    public Response preflight() {
        if (logger.isDebugEnabled()) {
            logger.debugv("CORS preflight from: {0}", headers.getRequestHeaders().getFirst("Origin"));
        }
        return Cors.add(request, Response.ok()).auth().preflight().build();
    }

    private void checkSsl() {
        if (!uriInfo.getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

    private void checkRealm() {
        if (!realm.isEnabled()) {
            throw new ErrorResponseException("access_denied", "Realm not enabled", Response.Status.FORBIDDEN);
        }
    }

    private void checkClient() {
        AuthorizeClientUtil.ClientAuthResult clientAuth = AuthorizeClientUtil.authorizeClient(session, event);
        client = clientAuth.getClient();
        clientAuthAttributes = clientAuth.getClientAuthAttributes();

        if (client.isBearerOnly()) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_CLIENT, "Bearer-only not allowed", Response.Status.BAD_REQUEST);
        }
    }

    private void checkGrantType() {
        if (grantType == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Missing form parameter: " + OIDCLoginProtocol.GRANT_TYPE_PARAM, Response.Status.BAD_REQUEST);
        }

        if (grantType.equals(OAuth2Constants.AUTHORIZATION_CODE)) {
            event.event(EventType.CODE_TO_TOKEN);
            action = Action.AUTHORIZATION_CODE;
        } else if (grantType.equals(OAuth2Constants.REFRESH_TOKEN)) {
            event.event(EventType.REFRESH_TOKEN);
            action = Action.REFRESH_TOKEN;
        } else if (grantType.equals(OAuth2Constants.PASSWORD)) {
            event.event(EventType.LOGIN);
            action = Action.PASSWORD;
        } else if (grantType.equals(OAuth2Constants.CLIENT_CREDENTIALS)) {
            event.event(EventType.CLIENT_LOGIN);
            action = Action.CLIENT_CREDENTIALS;
        } else {
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "Invalid " + OIDCLoginProtocol.GRANT_TYPE_PARAM, Response.Status.BAD_REQUEST);
        }

        event.detail(Details.GRANT_TYPE, grantType);
    }

    public Response buildAuthorizationCodeAccessTokenResponse() {
        String code = formParams.getFirst(OAuth2Constants.CODE);
        if (code == null) {
            event.error(Errors.INVALID_CODE);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Missing parameter: " + OAuth2Constants.CODE, Response.Status.BAD_REQUEST);
        }

        ClientSessionCode accessCode = ClientSessionCode.parse(code, session, realm);
        if (accessCode == null) {
            String[] parts = code.split("\\.");
            if (parts.length == 2) {
                event.detail(Details.CODE_ID, parts[1]);
            }
            event.error(Errors.INVALID_CODE);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Code not found", Response.Status.BAD_REQUEST);
        }

        ClientSessionModel clientSession = accessCode.getClientSession();
        event.detail(Details.CODE_ID, clientSession.getId());

        String codeExchanged = clientSession.getNote(CODE_EXCHANGED);
        if (codeExchanged != null && Boolean.parseBoolean(codeExchanged)) {
            session.sessions().removeClientSession(realm, clientSession);

            event.error(Errors.INVALID_CODE);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Code used already", Response.Status.BAD_REQUEST);
        }

        if (!accessCode.isValid(ClientSessionModel.Action.CODE_TO_TOKEN.name(), ClientSessionCode.ActionType.CLIENT)) {
            event.error(Errors.INVALID_CODE);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Code is expired", Response.Status.BAD_REQUEST);
        }

        accessCode.setAction(null);
        clientSession.setNote(CODE_EXCHANGED, "true");
        UserSessionModel userSession = clientSession.getUserSession();

        if (userSession == null) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "User session not found", Response.Status.BAD_REQUEST);
        }

        UserModel user = userSession.getUser();
        if (user == null) {
            event.error(Errors.USER_NOT_FOUND);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "User not found", Response.Status.BAD_REQUEST);
        }
        if (!user.isEnabled()) {
            event.error(Errors.USER_DISABLED);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "User disabled", Response.Status.BAD_REQUEST);
        }

        event.user(userSession.getUser());
        event.session(userSession.getId());

        String redirectUri = clientSession.getNote(OIDCLoginProtocol.REDIRECT_URI_PARAM);
        if (redirectUri != null && !redirectUri.equals(formParams.getFirst(OAuth2Constants.REDIRECT_URI))) {
            event.error(Errors.INVALID_CODE);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Incorrect redirect_uri", Response.Status.BAD_REQUEST);
        }

        if (!client.getClientId().equals(clientSession.getClient().getClientId())) {
            event.error(Errors.INVALID_CODE);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Auth error", Response.Status.BAD_REQUEST);
        }

        if (!client.isStandardFlowEnabled()) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Client not allowed to exchange code", Response.Status.BAD_REQUEST);
        }

        if (!AuthenticationManager.isSessionValid(realm, userSession)) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Session not active", Response.Status.BAD_REQUEST);
        }

        updateClientSession(clientSession);
        updateUserSessionFromClientAuth(userSession);

        AccessToken token = tokenManager.createClientAccessToken(session, accessCode.getRequestedRoles(), realm, client, user, userSession, clientSession);

        AccessTokenResponse res = tokenManager.responseBuilder(realm, client, event, session, userSession, clientSession)
                .accessToken(token)
                .generateIDToken()
                .generateRefreshToken().build();

        event.success();

        return Cors.add(request, Response.ok(res).type(MediaType.APPLICATION_JSON_TYPE)).auth().allowedOrigins(client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    public Response buildRefreshToken() {
        String refreshToken = formParams.getFirst(OAuth2Constants.REFRESH_TOKEN);
        if (refreshToken == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "No refresh token", Response.Status.BAD_REQUEST);
        }

        AccessTokenResponse res;
        try {
            TokenManager.RefreshResult result = tokenManager.refreshAccessToken(session, uriInfo, clientConnection, realm, client, refreshToken, event, headers);
            res = result.getResponse();

            if (!result.isOfflineToken()) {
                UserSessionModel userSession = session.sessions().getUserSession(realm, res.getSessionState());
                updateClientSessions(userSession.getClientSessions());
                updateUserSessionFromClientAuth(userSession);
            }

        } catch (OAuthErrorException e) {
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(e.getError(), e.getDescription(), Response.Status.BAD_REQUEST);
        }

        event.success();

        return Cors.add(request, Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).auth().allowedOrigins(client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    private void updateClientSession(ClientSessionModel clientSession) {

        if(clientSession == null) {
            logger.clientSessionNull();
            return;
        }

        String adapterSessionId = formParams.getFirst(AdapterConstants.CLIENT_SESSION_STATE);
        if (adapterSessionId != null) {
            String adapterSessionHost = formParams.getFirst(AdapterConstants.CLIENT_SESSION_HOST);
            logger.debugf("Adapter Session '%s' saved in ClientSession for client '%s'. Host is '%s'", adapterSessionId, client.getClientId(), adapterSessionHost);

            event.detail(AdapterConstants.CLIENT_SESSION_STATE, adapterSessionId);
            clientSession.setNote(AdapterConstants.CLIENT_SESSION_STATE, adapterSessionId);
            event.detail(AdapterConstants.CLIENT_SESSION_HOST, adapterSessionHost);
            clientSession.setNote(AdapterConstants.CLIENT_SESSION_HOST, adapterSessionHost);
        }
    }

    private void updateClientSessions(List<ClientSessionModel> clientSessions) {
        if(clientSessions == null) {
            logger.clientSessionNull();
            return;
        }
        for (ClientSessionModel clientSession : clientSessions) {
            if(clientSession == null) {
                logger.clientSessionNull();
                continue;
            }
            if(clientSession.getClient() == null) {
                logger.clientModelNull();
                continue;
            }
            if(client.getId().equals(clientSession.getClient().getId())) {
                updateClientSession(clientSession);
            }
        }
    }

    private void updateUserSessionFromClientAuth(UserSessionModel userSession) {
        for (Map.Entry<String, String> attr : clientAuthAttributes.entrySet()) {
            userSession.setNote(attr.getKey(), attr.getValue());
        }
    }

    public Response buildResourceOwnerPasswordCredentialsGrant() {
        event.detail(Details.AUTH_METHOD, "oauth_credentials");

        if (!client.isDirectAccessGrantsEnabled()) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Client not allowed for direct access grants", Response.Status.BAD_REQUEST);
        }

        if (client.isConsentRequired()) {
            event.error(Errors.CONSENT_DENIED);
            throw new ErrorResponseException(OAuthErrorException.INVALID_CLIENT, "Client requires user consent", Response.Status.BAD_REQUEST);
        }
        String scope = formParams.getFirst(OAuth2Constants.SCOPE);

        UserSessionProvider sessions = session.sessions();
        ClientSessionModel clientSession = sessions.createClientSession(realm, client);
        clientSession.setAuthMethod(OIDCLoginProtocol.LOGIN_PROTOCOL);
        clientSession.setAction(ClientSessionModel.Action.AUTHENTICATE.name());
        clientSession.setNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()));
        clientSession.setNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

        AuthenticationFlowModel flow = realm.getDirectGrantFlow();
        String flowId = flow.getId();
        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setClientSession(clientSession)
                .setFlowId(flowId)
                .setConnection(clientConnection)
                .setEventBuilder(event)
                .setRealm(realm)
                .setSession(session)
                .setUriInfo(uriInfo)
                .setRequest(request);
        Response challenge = processor.authenticateOnly();
        if (challenge != null) return challenge;
        processor.evaluateRequiredActionTriggers();
        UserModel user = clientSession.getAuthenticatedUser();
        if (user.getRequiredActions() != null && user.getRequiredActions().size() > 0) {
            event.error(Errors.RESOLVE_REQUIRED_ACTIONS);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Account is not fully set up", Response.Status.BAD_REQUEST);

        }
        processor.attachSession();
        UserSessionModel userSession = processor.getUserSession();
        updateUserSessionFromClientAuth(userSession);

        AccessTokenResponse res = tokenManager.responseBuilder(realm, client, event, session, userSession, clientSession)
                .generateAccessToken()
                .generateRefreshToken()
                .generateIDToken()
                .build();


        event.success();

        return Cors.add(request, Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).auth().allowedOrigins(client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    public Response buildClientCredentialsGrant() {
        if (client.isBearerOnly()) {
            event.error(Errors.INVALID_CLIENT);
            throw new ErrorResponseException(OAuthErrorException.UNAUTHORIZED_CLIENT, "Bearer-only client not allowed to retrieve service account", Response.Status.UNAUTHORIZED);
        }
        if (client.isPublicClient()) {
            event.error(Errors.INVALID_CLIENT);
            throw new ErrorResponseException(OAuthErrorException.UNAUTHORIZED_CLIENT, "Public client not allowed to retrieve service account", Response.Status.UNAUTHORIZED);
        }
        if (!client.isServiceAccountsEnabled()) {
            event.error(Errors.INVALID_CLIENT);
            throw new ErrorResponseException(OAuthErrorException.UNAUTHORIZED_CLIENT, "Client not enabled to retrieve service account", Response.Status.UNAUTHORIZED);
        }

        UserModel clientUser = session.users().getServiceAccount(client);

        if (clientUser == null || client.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, ServiceAccountConstants.CLIENT_ID_PROTOCOL_MAPPER) == null) {
            // May need to handle bootstrap here as well
            logger.debugf("Service account user for client '%s' not found or default protocol mapper for service account not found. Creating now", client.getClientId());
            new ClientManager(new RealmManager(session)).enableServiceAccount(client);
            clientUser = session.users().getServiceAccount(client);
        }

        String clientUsername = clientUser.getUsername();
        event.detail(Details.USERNAME, clientUsername);
        event.user(clientUser);

        if (!clientUser.isEnabled()) {
            event.error(Errors.USER_DISABLED);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "User '" + clientUsername + "' disabled", Response.Status.UNAUTHORIZED);
        }

        String scope = formParams.getFirst(OAuth2Constants.SCOPE);

        UserSessionProvider sessions = session.sessions();

        ClientSessionModel clientSession = sessions.createClientSession(realm, client);
        clientSession.setAuthMethod(OIDCLoginProtocol.LOGIN_PROTOCOL);
        clientSession.setNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()));
        clientSession.setNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

        UserSessionModel userSession = sessions.createUserSession(realm, clientUser, clientUsername, clientConnection.getRemoteAddr(), ServiceAccountConstants.CLIENT_AUTH, false, null, null);
        event.session(userSession);

        TokenManager.attachClientSession(userSession, clientSession);

        // Notes about client details
        userSession.setNote(ServiceAccountConstants.CLIENT_ID, client.getClientId());
        userSession.setNote(ServiceAccountConstants.CLIENT_HOST, clientConnection.getRemoteHost());
        userSession.setNote(ServiceAccountConstants.CLIENT_ADDRESS, clientConnection.getRemoteAddr());

        updateUserSessionFromClientAuth(userSession);

        AccessTokenResponse res = tokenManager.responseBuilder(realm, client, event, session, userSession, clientSession)
                .generateAccessToken()
                .generateRefreshToken()
                .generateIDToken()
                .build();

        event.success();

        return Cors.add(request, Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).auth().allowedOrigins(client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

}
