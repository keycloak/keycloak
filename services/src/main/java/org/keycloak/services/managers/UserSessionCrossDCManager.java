/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;

import static org.keycloak.services.managers.AuthenticationManager.authenticateIdentityCookie;
import static org.keycloak.utils.LockObjectsForModification.lockUserSessionsForModification;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserSessionCrossDCManager {

    private static final Logger logger = Logger.getLogger(UserSessionCrossDCManager.class);

    private final KeycloakSession kcSession;

    public UserSessionCrossDCManager(KeycloakSession session) {
        this.kcSession = session;
    }


    // get userSession if it has "authenticatedClientSession" of specified client attached to it. Otherwise download it from remoteCache
    public UserSessionModel getUserSessionWithClient(RealmModel realm, String id, boolean offline, String clientUUID) {
        return kcSession.sessions().getUserSessionWithPredicate(realm, id, offline, userSession -> userSession.getAuthenticatedClientSessionByClient(clientUUID) != null);
    }


    // get userSession if it has "authenticatedClientSession" of specified client attached to it. Otherwise download it from remoteCache
    // TODO Probably remove this method once AuthenticatedClientSession.getAction is removed and information is moved to OAuth code JWT instead
    public UserSessionModel getUserSessionWithClient(RealmModel realm, String id, String clientUUID) {

        return kcSession.sessions().getUserSessionWithPredicate(realm, id, false, (UserSessionModel userSession) -> {

            AuthenticatedClientSessionModel authSessions = userSession.getAuthenticatedClientSessionByClient(clientUUID);
            return authSessions != null;

        });
    }


    // Just check if userSession also exists on remoteCache. It can happen that logout happened on 2nd DC and userSession is already removed on remoteCache and this DC wasn't yet notified
    public UserSessionModel getUserSessionIfExistsRemotely(AuthenticationSessionManager asm, RealmModel realm) {
        List<String> sessionCookies = asm.getAuthSessionCookies(realm);

        if (sessionCookies.isEmpty()) {
            // ideally, we should not rely on auth session id to retrieve user sessions
            // in case the auth session was removed, we fall back to the identity cookie
            // we are here doing the user session lookup twice, however the second lookup is going to make sure the
            // session exists in remote caches
            AuthenticationManager.AuthResult authResult = lockUserSessionsForModification(kcSession, () -> authenticateIdentityCookie(kcSession, realm, true));

            if (authResult != null && authResult.getSession() != null) {
                sessionCookies = Collections.singletonList(authResult.getSession().getId());
            }
        }

        return sessionCookies.stream().map(oldEncodedId -> {
            AuthSessionId authSessionId = asm.decodeAuthSessionId(oldEncodedId);
            String sessionId = authSessionId.getDecodedId();

            // This will remove userSession "locally" if it doesn't exist on remoteCache
            lockUserSessionsForModification(kcSession, () -> kcSession.sessions().getUserSessionWithPredicate(realm, sessionId, false, (UserSessionModel userSession2) -> userSession2 == null));

            UserSessionModel userSession = lockUserSessionsForModification(kcSession, () -> kcSession.sessions().getUserSession(realm, sessionId));

            if (userSession != null) {
                asm.reencodeAuthSessionCookie(oldEncodedId, authSessionId, realm);
                return userSession;
            }

            return null;
        }).filter(userSession -> Objects.nonNull(userSession)).findFirst().orElse(null);
    }
}
