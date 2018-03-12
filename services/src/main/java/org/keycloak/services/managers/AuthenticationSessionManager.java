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
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.sessions.StickySessionEncoderProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationSessionManager {

    public static final String AUTH_SESSION_ID = "AUTH_SESSION_ID";

    private static final Logger log = Logger.getLogger(AuthenticationSessionManager.class);

    private final KeycloakSession session;

    public AuthenticationSessionManager(KeycloakSession session) {
        this.session = session;
    }


    /**
     * Creates a fresh authentication session for the given realm . Optionally sets the browser
     * authentication session cookie {@link #AUTH_SESSION_ID} with the ID of the new session.
     * @param realm
     * @param browserCookie Set the cookie in the browser for the
     * @return
     */
    public RootAuthenticationSessionModel createAuthenticationSession(RealmModel realm, boolean browserCookie) {
        RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().createRootAuthenticationSession(realm);

        if (browserCookie) {
            setAuthSessionCookie(rootAuthSession.getId(), realm);
        }

        return rootAuthSession;
    }


    /**
     * Returns ID of current authentication session if it exists, otherwise returns {@code null}.
     * @param realm
     * @return
     */
    public String getCurrentAuthenticationSessionId(RealmModel realm) {
        return getAuthSessionCookieDecoded(realm);
    }


    /**
     * Returns current authentication session if it exists, otherwise returns {@code null}.
     * @param realm
     * @return
     */
    public AuthenticationSessionModel getCurrentAuthenticationSession(RealmModel realm, ClientModel client, String tabId) {
        String authSessionId = getAuthSessionCookieDecoded(realm);

        if (authSessionId == null) {
            return null;
        }

        return getAuthenticationSessionByIdAndClient(realm, authSessionId, client, tabId);
    }


    public void setAuthSessionCookie(String authSessionId, RealmModel realm) {
        UriInfo uriInfo = session.getContext().getUri();
        String cookiePath = AuthenticationManager.getRealmCookiePath(realm, uriInfo);

        boolean sslRequired = realm.getSslRequired().isRequired(session.getContext().getConnection());

        StickySessionEncoderProvider encoder = session.getProvider(StickySessionEncoderProvider.class);
        String encodedAuthSessionId = encoder.encodeSessionId(authSessionId);

        CookieHelper.addCookie(AUTH_SESSION_ID, encodedAuthSessionId, cookiePath, null, null, -1, sslRequired, true);

        log.debugf("Set AUTH_SESSION_ID cookie with value %s", encodedAuthSessionId);
    }


    public String getAuthSessionCookieDecoded(RealmModel realm) {
        String cookieVal = CookieHelper.getCookieValue(AUTH_SESSION_ID);

        if (cookieVal != null) {
            log.debugf("Found AUTH_SESSION_ID cookie with value %s", cookieVal);

            StickySessionEncoderProvider encoder = session.getProvider(StickySessionEncoderProvider.class);
            String decodedAuthSessionId = encoder.decodeSessionId(cookieVal);

            // Check if owner of this authentication session changed due to re-hashing (usually node failover or addition of new node)
            String reencoded = encoder.encodeSessionId(decodedAuthSessionId);
            if (!reencoded.equals(cookieVal)) {
                log.debugf("Route changed. Will update authentication session cookie");
                setAuthSessionCookie(decodedAuthSessionId, realm);
            }

            return decodedAuthSessionId;
        } else {
            log.debugf("Not found AUTH_SESSION_ID cookie");
            return null;
        }
    }


    public void removeAuthenticationSession(RealmModel realm, AuthenticationSessionModel authSession, boolean expireRestartCookie) {
        RootAuthenticationSessionModel rootAuthSession = authSession.getParentSession();

        log.debugf("Removing authSession '%s'. Expire restart cookie: %b", rootAuthSession.getId(), expireRestartCookie);
        session.authenticationSessions().removeRootAuthenticationSession(realm, rootAuthSession);

        // expire restart cookie
        if (expireRestartCookie) {
            ClientConnection clientConnection = session.getContext().getConnection();
            UriInfo uriInfo = session.getContext().getUri();
            RestartLoginCookie.expireRestartCookie(realm, clientConnection, uriInfo);
        }
    }


    // Check to see if we already have authenticationSession with same ID
    public UserSessionModel getUserSession(AuthenticationSessionModel authSession) {
        return session.sessions().getUserSession(authSession.getRealm(), authSession.getParentSession().getId());
    }


    // Don't look at cookie. Just lookup authentication session based on the ID and client. Return null if not found
    public AuthenticationSessionModel getAuthenticationSessionByIdAndClient(RealmModel realm, String authSessionId, ClientModel client, String tabId) {
        RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, authSessionId);
        return rootAuthSession==null ? null : rootAuthSession.getAuthenticationSession(client, tabId);
    }

}
