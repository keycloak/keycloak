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

import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.ServerCookie.SameSiteAttributeValue;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.sessions.StickySessionEncoderProvider;

import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.keycloak.utils.LockObjectsForModification.lockUserSessionsForModification;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationSessionManager {

    public static final String AUTH_SESSION_ID = "AUTH_SESSION_ID";

    public static final int AUTH_SESSION_COOKIE_LIMIT = 3;

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


    public RootAuthenticationSessionModel getCurrentRootAuthenticationSession(RealmModel realm) {
        List<String> authSessionCookies = getAuthSessionCookies(realm);

        return authSessionCookies.stream().map(oldEncodedId -> {
            AuthSessionId authSessionId = decodeAuthSessionId(oldEncodedId);
            String sessionId = authSessionId.getDecodedId();

            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, sessionId);

            if (rootAuthSession != null) {
                reencodeAuthSessionCookie(oldEncodedId, authSessionId, realm);
                return rootAuthSession;
            }

            return null;
        }).filter(authSession -> Objects.nonNull(authSession)).findFirst().orElse(null);
    }


    public UserSessionModel getUserSessionFromAuthCookie(RealmModel realm) {
        List<String> authSessionCookies = getAuthSessionCookies(realm);

        return authSessionCookies.stream().map(oldEncodedId -> {
            AuthSessionId authSessionId = decodeAuthSessionId(oldEncodedId);
            String sessionId = authSessionId.getDecodedId();

            UserSessionModel userSession = lockUserSessionsForModification(session, () -> session.sessions().getUserSession(realm, sessionId));

            if (userSession != null) {
                reencodeAuthSessionCookie(oldEncodedId, authSessionId, realm);
                return userSession;
            }

            return null;
        }).filter(authSession -> Objects.nonNull(authSession)).findFirst().orElse(null);
    }


    /**
     * Returns current authentication session if it exists, otherwise returns {@code null}.
     * @param realm
     * @return
     */
    public AuthenticationSessionModel getCurrentAuthenticationSession(RealmModel realm, ClientModel client, String tabId) {
        List<String> authSessionCookies = getAuthSessionCookies(realm);

        return authSessionCookies.stream().map(oldEncodedId -> {
            AuthSessionId authSessionId = decodeAuthSessionId(oldEncodedId);
            String sessionId = authSessionId.getDecodedId();

            AuthenticationSessionModel authSession = getAuthenticationSessionByIdAndClient(realm, sessionId, client, tabId);

            if (authSession != null) {
                reencodeAuthSessionCookie(oldEncodedId, authSessionId, realm);
                return authSession;
            }

            return null;
        }).filter(authSession -> Objects.nonNull(authSession)).findFirst().orElse(null);
    }


    /**
     * @param authSessionId decoded authSessionId (without route info attached)
     * @param realm
     */
    public void setAuthSessionCookie(String authSessionId, RealmModel realm) {
        UriInfo uriInfo = session.getContext().getUri();
        String cookiePath = AuthenticationManager.getRealmCookiePath(realm, uriInfo);

        boolean sslRequired = realm.getSslRequired().isRequired(session.getContext().getConnection());

        StickySessionEncoderProvider encoder = session.getProvider(StickySessionEncoderProvider.class);
        String encodedAuthSessionId = encoder.encodeSessionId(authSessionId);

        CookieHelper.addCookie(AUTH_SESSION_ID, encodedAuthSessionId, cookiePath, null, null, -1, sslRequired, true, SameSiteAttributeValue.NONE, session);

        log.debugf("Set AUTH_SESSION_ID cookie with value %s", encodedAuthSessionId);
    }


    /**
     *
     * @param encodedAuthSessionId encoded ID with attached route in cluster environment (EG. "5e161e00-d426-4ea6-98e9-52eb9844e2d7.node1" )
     * @return object with decoded and actually encoded authSessionId
     */
    AuthSessionId decodeAuthSessionId(String encodedAuthSessionId) {
        log.debugf("Found AUTH_SESSION_ID cookie with value %s", encodedAuthSessionId);
        StickySessionEncoderProvider encoder = session.getProvider(StickySessionEncoderProvider.class);
        String decodedAuthSessionId = encoder.decodeSessionId(encodedAuthSessionId);
        String reencoded = encoder.encodeSessionId(decodedAuthSessionId);

        return new AuthSessionId(decodedAuthSessionId, reencoded);
    }


    void reencodeAuthSessionCookie(String oldEncodedAuthSessionId, AuthSessionId newAuthSessionId, RealmModel realm) {
        if (!oldEncodedAuthSessionId.equals(newAuthSessionId.getEncodedId())) {
            log.debugf("Route changed. Will update authentication session cookie. Old: '%s', New: '%s'", oldEncodedAuthSessionId,
                    newAuthSessionId.getEncodedId());
            setAuthSessionCookie(newAuthSessionId.getDecodedId(), realm);
        }
    }


    /**
     * @param realm
     * @return list of the values of AUTH_SESSION_ID cookies. It is assumed that values could be encoded with route added (EG. "5e161e00-d426-4ea6-98e9-52eb9844e2d7.node1" )
     */
    List<String> getAuthSessionCookies(RealmModel realm) {
        Set<String> cookiesVal = CookieHelper.getCookieValue(session, AUTH_SESSION_ID);

        if (cookiesVal.size() > 1) {
            AuthenticationManager.expireOldAuthSessionCookie(realm, session.getContext().getUri(), session);
        }

        List<String> authSessionIds = cookiesVal.stream().limit(AUTH_SESSION_COOKIE_LIMIT).collect(Collectors.toList());

        if (authSessionIds.isEmpty()) {
            log.debugf("Not found AUTH_SESSION_ID cookie");
        }

        return authSessionIds.stream().filter(new Predicate<String>() {
            @Override
            public boolean test(String id) {
                StickySessionEncoderProvider encoder = session.getProvider(StickySessionEncoderProvider.class);
                // in case the id is encoded with a route when running in a cluster
                String decodedId = encoder.decodeSessionId(cookiesVal.iterator().next());
                // we can't blindly trust the cookie and assume it is valid and referencing a valid root auth session
                // but make sure the root authentication session actually exists
                // without this check there is a risk of resolving user sessions from invalid root authentication sessions as they share the same id
                return session.authenticationSessions().getRootAuthenticationSession(realm, decodedId) != null;
            }
        }).collect(Collectors.toList());
    }


    public void removeAuthenticationSession(RealmModel realm, AuthenticationSessionModel authSession, boolean expireRestartCookie) {
        RootAuthenticationSessionModel rootAuthSession = authSession.getParentSession();

        log.debugf("Removing authSession '%s'. Expire restart cookie: %b", rootAuthSession.getId(), expireRestartCookie);
        session.authenticationSessions().removeRootAuthenticationSession(realm, rootAuthSession);

        // expire restart cookie
        if (expireRestartCookie) {
            UriInfo uriInfo = session.getContext().getUri();
            RestartLoginCookie.expireRestartCookie(realm, uriInfo, session);
        }
    }


    // Check to see if we already have authenticationSession with same ID
    public UserSessionModel getUserSession(AuthenticationSessionModel authSession) {
        return lockUserSessionsForModification(session, () -> session.sessions().getUserSession(authSession.getRealm(), authSession.getParentSession().getId()));
    }


    // Don't look at cookie. Just lookup authentication session based on the ID and client. Return null if not found
    public AuthenticationSessionModel getAuthenticationSessionByIdAndClient(RealmModel realm, String authSessionId, ClientModel client, String tabId) {
        RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, authSessionId);
        return rootAuthSession==null ? null : rootAuthSession.getAuthenticationSession(client, tabId);
    }
}
