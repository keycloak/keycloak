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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oid4vc.model.IDTokenResponse;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.protocol.oidc.utils.OIDCRedirectUriBuilder;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.services.Urls;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;

import static org.keycloak.authentication.AuthenticationFlowError.ACCESS_DENIED;
import static org.keycloak.authentication.AuthenticationFlowError.INVALID_CREDENTIALS;
import static org.keycloak.authentication.AuthenticationFlowError.INVALID_USER;

/**
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
public class DirectPostEndpoint {

    private static final Logger logger = Logger.getLogger(DirectPostEndpoint.class);

    final RealmModel realm;
    final EventBuilder event;

    final HttpHeaders headers;
    final HttpRequest httpRequest;
    final KeycloakSession session;
    final ClientConnection clientConnection;

    static final String AUTHORIZATION_REQUEST_KEY = AuthorizationEndpointRequest.class.getName();

    // [TODO #44657] Revisit DirectPostEndpoint.authSessionHolder
    static Map<String, AuthenticationSessionModel> authSessionHolder = new ConcurrentHashMap<>();

    public DirectPostEndpoint(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
        this.realm = session.getContext().getRealm();
        this.event = event;
        this.httpRequest = session.getContext().getHttpRequest();
        this.headers = session.getContext().getRequestHeaders();
    }

    public Response process() {

        MultivaluedMap<String, String> form = httpRequest.getDecodedFormParameters();
        String signedJwt = form.getFirst("id_token");
        if (signedJwt == null)
            return Response.status(Status.BAD_REQUEST).entity("No id_token").build();

        // Process id_token (JWT validation, etc.)
        IDTokenResponse idTokenRes = new IDTokenResponse(signedJwt);
        logger.infof("Received IDTokenResponse: %s", idTokenRes.tokenJwt);

        // Verify IDToken Jwt
        try {
            idTokenRes.verify(session);
        } catch (VerificationException e) {
            throw new AuthenticationFlowException("Failed to validate IDToken: " + idTokenRes.tokenJwt, e, INVALID_CREDENTIALS);
        }

        // Restore AuthorizationRequest from session
        if (!session.singleUseObjects().contains(AUTHORIZATION_REQUEST_KEY))
            throw new IllegalStateException("No " + AUTHORIZATION_REQUEST_KEY + " in user session");

        String authRequestJson = session.singleUseObjects().remove(AUTHORIZATION_REQUEST_KEY).get("json");
        AuthorizationEndpointRequest authRequest = JsonSerialization.valueFromString(authRequestJson, AuthorizationEndpointRequest.class);

        if (!idTokenRes.clientId.equals(authRequest.getClientId()))
            throw new AuthenticationFlowException("IDToken subject miss match", INVALID_CREDENTIALS);

        OIDCResponseType responseType = OIDCResponseType.parse(authRequest.getResponseType());
        OIDCResponseMode responseMode = OIDCResponseMode.parse(authRequest.getResponseMode(), responseType);
        if (!OIDCResponseType.CODE.equals(responseType.toString()))
            throw new AuthenticationFlowException("Unexpected response_type: " + responseType, ACCESS_DENIED);

        String userDid = idTokenRes.clientId;
        UserModel userModel = session.users()
                .searchForUserByUserAttributeStream(realm, UserModel.DID, userDid)
                .findFirst()
                .orElse(null);
        if (userModel == null)
            throw new AuthenticationFlowException("Cannot identify user by: " + userDid, INVALID_USER);

        // Create the UserSession and attach it to the context
        UserSessionModel userSession = session.sessions().createUserSession(null, realm, userModel, userModel.getUsername(),
                null, "id_token", false, null,
                null, UserSessionModel.SessionPersistenceState.PERSISTENT);
        session.getContext().setUserSession(userSession);

        AuthenticationSessionModel authSession = authSessionHolder.remove(userDid);
        if (authSession == null)
            throw new IllegalStateException("No AuthenticationSession for: " + userDid);

        ClientModel clientModel = authSession.getClient();
        AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, clientModel, userSession);
        String issuer = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());
        clientSession.setNote(OIDCLoginProtocol.ISSUER, issuer);

        authSession.setAuthenticatedUser(userModel);

        String nonce = authSession.getClientNote(OIDCLoginProtocol.NONCE_PARAM);
        OAuth2Code codeData = new OAuth2Code(SecretGenerator.getInstance().generateSecureID(),
                Time.currentTime() + userSession.getRealm().getAccessCodeLifespan(),
                nonce,
                authSession.getClientNote(OAuth2Constants.SCOPE),
                authSession.getClientNote(OIDCLoginProtocol.REDIRECT_URI_PARAM),
                authSession.getClientNote(OIDCLoginProtocol.CODE_CHALLENGE_PARAM),
                authSession.getClientNote(OIDCLoginProtocol.CODE_CHALLENGE_METHOD_PARAM),
                authSession.getClientNote(OIDCLoginProtocol.DPOP_JKT),
                userSession.getId());
        String code = OAuth2CodeParser.persistCode(session, clientSession, codeData);

        String redirectUriParam = authRequest.getRedirectUriParam();
        OIDCRedirectUriBuilder redirectUri = OIDCRedirectUriBuilder.fromUri(redirectUriParam, responseMode, session, clientSession);
        redirectUri.addParam(OAuth2Constants.CODE, code);
        return redirectUri.build();
    }
}
