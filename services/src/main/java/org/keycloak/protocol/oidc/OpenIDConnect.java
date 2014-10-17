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
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.ResourceAdminManager;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OpenIDConnect implements LoginProtocol {

    public static final String LOGIN_PROTOCOL = "openid-connect";
    public static final String STATE_PARAM = "state";
    public static final String SCOPE_PARAM = "scope";
    public static final String RESPONSE_TYPE_PARAM = "response_type";
    public static final String REDIRECT_URI_PARAM = "redirect_uri";
    public static final String CLIENT_ID_PARAM = "client_id";
    public static final String PROMPT_PARAM = "prompt";
    public static final String LOGIN_HINT_PARAM = "login_hint";
    private static final Logger log = Logger.getLogger(OpenIDConnect.class);

    protected KeycloakSession session;

    protected RealmModel realm;

    protected UriInfo uriInfo;

    public OpenIDConnect(KeycloakSession session, RealmModel realm, UriInfo uriInfo) {
        this.session = session;
        this.realm = realm;
        this.uriInfo = uriInfo;
    }

    public OpenIDConnect() {
    }

    @Override
    public OpenIDConnect setSession(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public OpenIDConnect setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public OpenIDConnect setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
        return this;
    }

    @Override
    public Response cancelLogin(ClientSessionModel clientSession) {
        String redirect = clientSession.getRedirectUri();
        String state = clientSession.getNote(OpenIDConnect.STATE_PARAM);
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
        String state = clientSession.getNote(OpenIDConnect.STATE_PARAM);
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
        String state = clientSession.getNote(OpenIDConnect.STATE_PARAM);
        UriBuilder redirectUri = UriBuilder.fromUri(redirect).queryParam(OAuth2Constants.ERROR, "access_denied");
        if (state != null)
            redirectUri.queryParam(OAuth2Constants.STATE, state);
        Response.ResponseBuilder location = Response.status(302).location(redirectUri.build());
        return location.build();
    }


    public Response invalidSessionError(ClientSessionModel clientSession) {
        String redirect = clientSession.getRedirectUri();
        String state = clientSession.getNote(OpenIDConnect.STATE_PARAM);
        UriBuilder redirectUri = UriBuilder.fromUri(redirect).queryParam(OAuth2Constants.ERROR, "access_denied");
        if (state != null) {
            redirectUri.queryParam(OAuth2Constants.STATE, state);
        }
        return Response.status(302).location(redirectUri.build()).build();
    }

    @Override
    public void backchannelLogout(UserSessionModel userSession, ClientSessionModel clientSession) {
        if (!(clientSession.getClient() instanceof ApplicationModel)) return;
        ApplicationModel app = (ApplicationModel)clientSession.getClient();
        ApacheHttpClient4Executor executor = ResourceAdminManager.createExecutor();

        try {
            new ResourceAdminManager().logoutClientSession(uriInfo.getRequestUri(), realm, app, clientSession, executor);
        } finally {
            executor.getHttpClient().getConnectionManager().shutdown();
        }
    }

    @Override
    public void close() {

    }
}
