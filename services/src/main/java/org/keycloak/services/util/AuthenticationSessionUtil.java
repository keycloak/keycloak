/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.util;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import static org.keycloak.services.Constants.ASSOCIATED_AUTH_SESSION_ID;
import static org.keycloak.services.Constants.USER_SESSION_ID;

public class AuthenticationSessionUtil {

    public static AuthenticationSessionModel createAuthenticationSession(final KeycloakSession session, final RealmModel realm,
                                                                         final ClientModel client, final UserSessionModel userSession) {
        RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().createRootAuthenticationSession(realm);
        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);
        userSession.setNote(ASSOCIATED_AUTH_SESSION_ID + authSession.getTabId(), rootAuthSession.getId());
        authSession.setAuthNote(USER_SESSION_ID, userSession.getId());
        return authSession;
    }

    public static String getEventCode(final AuthenticationSessionModel authSession) {
        String associatedUserSession = authSession.getAuthNote(USER_SESSION_ID);
        return associatedUserSession != null ? associatedUserSession : authSession.getParentSession().getId();
    }
}
