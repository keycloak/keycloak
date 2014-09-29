/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.protocol.oidc;

import org.jboss.logging.Logger;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.services.resources.flows.Flows;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OAuthFlows implements LoginProtocol {

    public static final String LOGIN_PAGE_PROTOCOL = "openid-connect";
    public static final String STATE_PARAM = "state";
    public static final String SCOPE_PARAM = "scope";
    public static final String RESPONSE_TYPE_PARAM = "response_type";
    public static final String REDIRECT_URI_PARAM = "redirect_uri";
    public static final String CLIENT_ID_PARAM = "client_id";
    public static final String PROMPT_PARAM = "prompt";
    public static final String LOGIN_HINT_PARAM = "login_hint";
    private static final Logger log = Logger.getLogger(OAuthFlows.class);

    protected KeycloakSession session;

    protected RealmModel realm;

    protected HttpRequest request;

    protected UriInfo uriInfo;

    protected ClientConnection clientConnection;

    public OAuthFlows(KeycloakSession session, RealmModel realm, HttpRequest request, UriInfo uriInfo,
               ClientConnection clientConnection) {
        this.session = session;
        this.realm = realm;
        this.request = request;
        this.uriInfo = uriInfo;
        this.clientConnection = clientConnection;
    }

    public OAuthFlows() {
    }

    @Override
    public OAuthFlows setSession(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public OAuthFlows setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public OAuthFlows setRequest(HttpRequest request) {
        this.request = request;
        return this;
    }

    @Override
    public OAuthFlows setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
        return this;
    }

    @Override
    public OAuthFlows setClientConnection(ClientConnection clientConnection) {
        this.clientConnection = clientConnection;
        return this;
    }

    @Override
    public Response cancelLogin(ClientSessionModel clientSession) {
        String redirect = clientSession.getRedirectUri();
        String state = clientSession.getNote(OAuthFlows.STATE_PARAM);
        UriBuilder redirectUri = UriBuilder.fromUri(redirect).queryParam(OAuth2Constants.ERROR, "access_denied");
        if (state != null) {
            redirectUri.queryParam(OAuth2Constants.STATE, state);
        }
        return Response.status(302).location(redirectUri.build()).build();
    }

    @Override
    public Response authenticated(UserSessionModel userSession, ClientSessionCode accessCode) {
        ClientSessionModel clientSession = accessCode.getClientSession();
        String redirect = clientSession.getRedirectUri();
        String state = clientSession.getNote(OAuthFlows.STATE_PARAM);
        accessCode.setAction(ClientSessionModel.Action.CODE_TO_TOKEN);
        UriBuilder redirectUri = UriBuilder.fromUri(redirect).queryParam(OAuth2Constants.CODE, accessCode.getCode());
        log.debugv("redirectAccessCode: state: {0}", state);
        if (state != null)
            redirectUri.queryParam(OAuth2Constants.STATE, state);
        Response.ResponseBuilder location = Response.status(302).location(redirectUri.build());

        return location.build();
    }

    public Response consentDenied(ClientSessionModel clientSession) {
        String redirect = clientSession.getRedirectUri();
        String state = clientSession.getNote(OAuthFlows.STATE_PARAM);
        UriBuilder redirectUri = UriBuilder.fromUri(redirect).queryParam(OAuth2Constants.ERROR, "access_denied");
        if (state != null)
            redirectUri.queryParam(OAuth2Constants.STATE, state);
        Response.ResponseBuilder location = Response.status(302).location(redirectUri.build());
        return location.build();
    }


    public Response invalidSessionError(ClientSessionModel clientSession) {
        String redirect = clientSession.getRedirectUri();
        String state = clientSession.getNote(OAuthFlows.STATE_PARAM);
        UriBuilder redirectUri = UriBuilder.fromUri(redirect).queryParam(OAuth2Constants.ERROR, "access_denied");
        if (state != null) {
            redirectUri.queryParam(OAuth2Constants.STATE, state);
        }
        return Response.status(302).location(redirectUri.build()).build();
    }

    @Override
    public void close() {

    }
}
