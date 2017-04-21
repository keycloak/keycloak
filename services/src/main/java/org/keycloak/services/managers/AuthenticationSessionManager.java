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

package org.keycloak.services.managers;

import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationSessionManager {

    private static final String AUTH_SESSION_ID = "AUTH_SESSION_ID";

    private static final Logger log = Logger.getLogger(AuthenticationSessionManager.class);

    private final KeycloakSession session;

    public AuthenticationSessionManager(KeycloakSession session) {
        this.session = session;
    }


    public AuthenticationSessionModel createAuthenticationSession(RealmModel realm, ClientModel client, boolean browserCookie) {
        AuthenticationSessionModel authSession = session.authenticationSessions().createAuthenticationSession(realm, client);

        if (browserCookie) {
            setAuthSessionCookie(authSession.getId(), realm);
        }

        return authSession;
    }


    public String getCurrentAuthenticationSessionId(RealmModel realm) {
        return getAuthSessionCookie();
    }


    public AuthenticationSessionModel getCurrentAuthenticationSession(RealmModel realm) {
        String authSessionId = getAuthSessionCookie();
        return authSessionId==null ? null : session.authenticationSessions().getAuthenticationSession(realm, authSessionId);
    }


    public void setAuthSessionCookie(String authSessionId, RealmModel realm) {
        UriInfo uriInfo = session.getContext().getUri();
        String cookiePath = AuthenticationManager.getRealmCookiePath(realm, uriInfo);

        boolean sslRequired = realm.getSslRequired().isRequired(session.getContext().getConnection());
        CookieHelper.addCookie(AUTH_SESSION_ID, authSessionId, cookiePath, null, null, -1, sslRequired, true);

        log.debugf("Set AUTH_SESSION_ID cookie with value %s", authSessionId);
    }


    public String getAuthSessionCookie() {
        String cookieVal = CookieHelper.getCookieValue(AUTH_SESSION_ID);

        if (cookieVal != null) {
            log.debugf("Found AUTH_SESSION_ID cookie with value %s", cookieVal);
        } else {
            log.debugf("Not found AUTH_SESSION_ID cookie");
        }

        return cookieVal;
    }


    public void removeAuthenticationSession(RealmModel realm, AuthenticationSessionModel authSession, boolean expireRestartCookie) {
        log.debugf("Removing authSession '%s'. Expire restart cookie: %b", authSession.getId(), expireRestartCookie);
        session.authenticationSessions().removeAuthenticationSession(realm, authSession);

        // expire restart cookie
        if (expireRestartCookie) {
            ClientConnection clientConnection = session.getContext().getConnection();
            UriInfo uriInfo = session.getContext().getUri();
            RestartLoginCookie.expireRestartCookie(realm, clientConnection, uriInfo);
        }
    }


    // Check to see if we already have authenticationSession with same ID
    public UserSessionModel getUserSession(AuthenticationSessionModel authSession) {
        return session.sessions().getUserSession(authSession.getRealm(), authSession.getId());
    }

}
