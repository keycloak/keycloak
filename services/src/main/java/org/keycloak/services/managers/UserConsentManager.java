/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.services.managers;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserConsentManager {

    /**
     * Revoke consent of given user to given client
     *
     * @param session
     * @param client
     * @param user
     * @return true if either consent or offlineToken was revoked
     */
    public static boolean revokeConsentToClient(KeycloakSession session, ClientModel client, UserModel user) {
        RealmModel realm = session.getContext().getRealm();
        boolean revokedConsent = session.users().revokeConsentForClient(realm, user.getId(), client.getId());
        boolean revokedOfflineToken = new UserSessionManager(session).revokeOfflineToken(user, client);

        if (revokedConsent) {
            // Logout clientSessions for this user and client
            AuthenticationManager.backchannelLogoutUserFromClient(session, realm, user, client, session.getContext().getUri(), session.getContext().getRequestHeaders());
        }

        return revokedConsent || revokedOfflineToken;
    }
}
