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
package org.keycloak.protocol.oidc;

import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.protocol.oidc.utils.OIDCRedirectUriBuilder;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.ResourceAdminManager;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCLoginProtocol implements LoginProtocol {

    public static final String LOGIN_PROTOCOL = "openid-connect";
    public static final String STATE_PARAM = "state";
    public static final String LOGOUT_STATE_PARAM = "OIDC_LOGOUT_STATE_PARAM";
    public static final String SCOPE_PARAM = "scope";
    public static final String CODE_PARAM = "code";
    public static final String RESPONSE_TYPE_PARAM = "response_type";
    public static final String GRANT_TYPE_PARAM = "grant_type";
    public static final String REDIRECT_URI_PARAM = "redirect_uri";
    public static final String CLIENT_ID_PARAM = "client_id";
    public static final String NONCE_PARAM = "nonce";
    public static final String PROMPT_PARAM = "prompt";
    public static final String LOGIN_HINT_PARAM = "login_hint";
    public static final String LOGOUT_REDIRECT_URI = "OIDC_LOGOUT_REDIRECT_URI";
    public static final String ISSUER = "iss";

    public static final String RESPONSE_MODE_PARAM = "response_mode";

    private static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    protected KeycloakSession session;

    protected RealmModel realm;

    protected UriInfo uriInfo;

    protected HttpHeaders headers;

    protected EventBuilder event;

    protected OIDCResponseType responseType;
    protected OIDCResponseMode responseMode;

    public OIDCLoginProtocol(KeycloakSession session, RealmModel realm, UriInfo uriInfo, HttpHeaders headers, EventBuilder event) {
        this.session = session;
        this.realm = realm;
        this.uriInfo = uriInfo;
        this.headers = headers;
        this.event = event;
    }

    public OIDCLoginProtocol() {

    }

    private void setupResponseTypeAndMode(ClientSessionModel clientSession) {
        String responseType = clientSession.getNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        String responseMode = clientSession.getNote(OIDCLoginProtocol.RESPONSE_MODE_PARAM);
        this.responseType = OIDCResponseType.parse(responseType);
        this.responseMode = OIDCResponseMode.parse(responseMode, this.responseType);
        this.event.detail(Details.RESPONSE_TYPE, responseType);
        this.event.detail(Details.RESPONSE_MODE, this.responseMode.toString().toLowerCase());
    }

    @Override
    public OIDCLoginProtocol setSession(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public OIDCLoginProtocol setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public OIDCLoginProtocol setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
        return this;
    }

    @Override
    public OIDCLoginProtocol setHttpHeaders(HttpHeaders headers) {
        this.headers = headers;
        return this;
    }

    @Override
    public OIDCLoginProtocol setEventBuilder(EventBuilder event) {
        this.event = event;
        return this;
    }


    @Override
    public Response authenticated(UserSessionModel userSession, ClientSessionCode accessCode) {
        ClientSessionModel clientSession = accessCode.getClientSession();
        setupResponseTypeAndMode(clientSession);

        String redirect = clientSession.getRedirectUri();
        OIDCRedirectUriBuilder redirectUri = OIDCRedirectUriBuilder.fromUri(redirect, responseMode);
        String state = clientSession.getNote(OIDCLoginProtocol.STATE_PARAM);
        logger.debugv("redirectAccessCode: state: {0}", state);
        if (state != null)
            redirectUri.addParam(OAuth2Constants.STATE, state);

        // Standard or hybrid flow
        if (responseType.hasResponseType(OIDCResponseType.CODE)) {
            accessCode.setAction(ClientSessionModel.Action.CODE_TO_TOKEN.name());
            redirectUri.addParam(OAuth2Constants.CODE, accessCode.getCode());
        }

        // Implicit or hybrid flow
        if (responseType.isImplicitOrHybridFlow()) {
            TokenManager tokenManager = new TokenManager();
            AccessTokenResponse res = tokenManager.responseBuilder(realm, clientSession.getClient(), event, session, userSession, clientSession)
                    .generateAccessToken()
                    .generateIDToken()
                    .build();

            if (responseType.hasResponseType(OIDCResponseType.ID_TOKEN)) {
                redirectUri.addParam("id_token", res.getIdToken());
            }

            if (responseType.hasResponseType(OIDCResponseType.TOKEN)) {
                redirectUri.addParam("access_token", res.getToken());
                redirectUri.addParam("token_type", res.getTokenType());
                redirectUri.addParam("session_state", res.getSessionState());
                redirectUri.addParam("expires_in", String.valueOf(res.getExpiresIn()));
            }

            redirectUri.addParam("not-before-policy", String.valueOf(res.getNotBeforePolicy()));
        }

        return redirectUri.build();
    }


    @Override
    public Response sendError(ClientSessionModel clientSession, Error error) {
        setupResponseTypeAndMode(clientSession);

        String redirect = clientSession.getRedirectUri();
        String state = clientSession.getNote(OIDCLoginProtocol.STATE_PARAM);
        OIDCRedirectUriBuilder redirectUri = OIDCRedirectUriBuilder.fromUri(redirect, responseMode).addParam(OAuth2Constants.ERROR, translateError(error));
        if (state != null)
            redirectUri.addParam(OAuth2Constants.STATE, state);
        session.sessions().removeClientSession(realm, clientSession);
        RestartLoginCookie.expireRestartCookie(realm, session.getContext().getConnection(), uriInfo);
        return redirectUri.build();
    }

    private String translateError(Error error) {
        switch (error) {
            case CANCELLED_BY_USER:
            case CONSENT_DENIED:
                return "access_denied";
            case PASSIVE_INTERACTION_REQUIRED:
                return "interaction_required";
            case PASSIVE_LOGIN_REQUIRED:
                return "login_required";
            default:
                logger.untranslatedProtocol(error.name());
                return "access_denied";
        }
    }

    @Override
    public void backchannelLogout(UserSessionModel userSession, ClientSessionModel clientSession) {
        ClientModel client = clientSession.getClient();
        new ResourceAdminManager(session).logoutClientSession(uriInfo.getRequestUri(), realm, client, clientSession);
    }

    @Override
    public Response frontchannelLogout(UserSessionModel userSession, ClientSessionModel clientSession) {
        // todo oidc redirect support
        throw new RuntimeException("NOT IMPLEMENTED");
    }

    @Override
    public Response finishLogout(UserSessionModel userSession) {
        String redirectUri = userSession.getNote(OIDCLoginProtocol.LOGOUT_REDIRECT_URI);
        String state = userSession.getNote(OIDCLoginProtocol.LOGOUT_STATE_PARAM);
        event.event(EventType.LOGOUT);
        if (redirectUri != null) {
            event.detail(Details.REDIRECT_URI, redirectUri);
        }
        event.user(userSession.getUser()).session(userSession).success();

        if (redirectUri != null) {
            UriBuilder uriBuilder = UriBuilder.fromUri(redirectUri);
            if (state != null)
                uriBuilder.queryParam(STATE_PARAM, state);
            return Response.status(302).location(uriBuilder.build()).build();
        } else {
            return Response.ok().build();
        }
    }

    @Override
    public void close() {

    }
}
