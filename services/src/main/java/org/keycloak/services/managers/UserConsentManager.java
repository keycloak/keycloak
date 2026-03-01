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

import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.light.LightweightUserAdapter;

import static org.keycloak.models.light.LightweightUserAdapter.isLightweightUser;

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
        boolean revokedConsent = revokeConsentForClient(session, realm, user, client.getId());
        boolean revokedOfflineToken = new UserSessionManager(session).revokeOfflineToken(user, client);

        if (revokedConsent) {
            // Logout clientSessions for this user and client
            AuthenticationManager.backchannelLogoutUserFromClient(session, realm, user, client, session.getContext().getUri(), session.getContext().getRequestHeaders());
        }

        return revokedConsent || revokedOfflineToken;
    }

    /**
     * Add user consent for the user.
     *
     * @param realm a reference to the realm
     * @param user user. Must not be {@code null}
     * @param consent all details corresponding to the granted consent
     *
     * @throws ModelException If there is no user with userId
     */
    public static void addConsent(KeycloakSession session, RealmModel realm, UserModel user, UserConsentModel consent) {
        if (isLightweightUser(user)) {
            LightweightUserAdapter lua = (LightweightUserAdapter) user;
            lua.addConsent(consent);
        } else {
            session.users().addConsent(realm, user.getId(), consent);
        }
    }

    /**
     * Returns UserConsentModel given by a user for the client with clientInternalId
     *
     * @param realm a reference to the realm
     * @param user user. Must not be {@code null}
     * @param clientInternalId id of the client
     * @return consent given by the user to the client or {@code null} if no consent or user exists
     *
     * @throws ModelException when there are more consents fulfilling specified parameters
     */
    public static UserConsentModel getConsentByClient(KeycloakSession session, RealmModel realm, UserModel user, String clientInternalId) {
        if (isLightweightUser(user)) {
            LightweightUserAdapter lua = (LightweightUserAdapter) user;
            return lua.getConsentByClient(clientInternalId);
        } else {
            return session.users().getConsentByClient(realm, user.getId(), clientInternalId);
        }
    }

    /**
     * Obtains the consents associated with the user
     *
     * @param realm a reference to the realm.
     * @param user user. Must not be {@code null}
     * @return a non-null {@link Stream} of consents associated with the user.
     */
    public static Stream<UserConsentModel> getConsentsStream(KeycloakSession session, RealmModel realm, UserModel user) {
        if (isLightweightUser(user)) {
            LightweightUserAdapter lua = (LightweightUserAdapter) user;
            return lua.getConsentsStream();
        } else {
            return session.users().getConsentsStream(realm, user.getId());
        }
    }

    /**
     * Update client scopes in the stored user consent
     *
     * @param realm a reference to the realm
     * @param user user. Must not be {@code null}
     * @param consent new details of the user consent
     *
     * @throws ModelException when consent doesn't exist for the userId
     */
    public static void updateConsent(KeycloakSession session, RealmModel realm, UserModel user, UserConsentModel consent) {
        if (isLightweightUser(user)) {
            LightweightUserAdapter lua = (LightweightUserAdapter) user;
            lua.updateConsent(consent);
        } else {
            session.users().updateConsent(realm, user.getId(), consent);
        }
    }

    /**
     * Remove a user consent given by the user and client id
     *
     * @param realm a reference to the realm
     * @param user user. Must not be {@code null}
     * @param clientInternalId id of the client
     * @return {@code true} if the consent was removed, {@code false} otherwise
     *
     * TODO: Make this method return Boolean so that store can return "I don't know" answer, this can be used for example in async stores
     */
    public static boolean revokeConsentForClient(KeycloakSession session, RealmModel realm, UserModel user, String clientInternalId) {
        if (isLightweightUser(user)) {
            LightweightUserAdapter lua = (LightweightUserAdapter) user;
            return lua.revokeConsentForClient(clientInternalId);
        } else {
            return session.users().revokeConsentForClient(realm, user.getId(), clientInternalId);
        }
    }

}
