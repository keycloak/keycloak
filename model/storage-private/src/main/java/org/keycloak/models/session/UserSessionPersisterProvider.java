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

package org.keycloak.models.session;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface UserSessionPersisterProvider extends Provider {

    // Persist just userSession. Not it's clientSessions
    void createUserSession(UserSessionModel userSession, boolean offline);

    // Assuming that corresponding userSession is already persisted
    void createClientSession(AuthenticatedClientSessionModel clientSession, boolean offline);

    // Called during logout (for online session) or during periodic expiration. It will remove all corresponding clientSessions too
    void removeUserSession(String userSessionId, boolean offline);

    // Called during revoke. It will remove userSession too if this was last clientSession attached to it
    void removeClientSession(String userSessionId, String clientUUID, boolean offline);

    void onRealmRemoved(RealmModel realm);
    void onClientRemoved(RealmModel realm, ClientModel client);
    void onUserRemoved(RealmModel realm, UserModel user);

    // Bulk update of lastSessionRefresh of all specified userSessions to the given value.
    void updateLastSessionRefreshes(RealmModel realm, int lastSessionRefresh, Collection<String> userSessionIds, boolean offline);

    // Remove userSessions and clientSessions, which are expired
    void removeExpired(RealmModel realm);

    /**
     * Loads the user session with the given userSessionId.
     * @param userSessionId
     * @param offline
     * @return
     */
    UserSessionModel loadUserSession(RealmModel realm, String userSessionId, boolean offline);

    /**
     * Loads the user sessions for the given {@link UserModel} in the given {@link RealmModel} if present.
     * @param realm
     * @param user
     * @param offline
     * @param firstResult
     * @param maxResults
     * @return
     */
    Stream<UserSessionModel> loadUserSessionsStream(RealmModel realm, UserModel user, boolean offline, Integer firstResult, Integer maxResults);

    /**
     * Loads the user sessions for the given {@link ClientModel} in the given {@link RealmModel} if present.
     *
     * @param realm
     * @param client
     * @param offline
     * @param firstResult
     * @param maxResults
     * @return
     */
    Stream<UserSessionModel> loadUserSessionsStream(RealmModel realm, ClientModel client, boolean offline, Integer firstResult, Integer maxResults);

    default UserSessionModel loadUserSessionsStreamByBrokerSessionId(RealmModel realm, String brokerSessionId, boolean offline) {
        throw new IllegalStateException("not implemented");
    }

    /**
     * Called during startup. For each userSession, it loads also clientSessions.
     * @param firstResult {@code Integer} Index of the first desired user session. Ignored if negative or {@code null}.
     * @param maxResults {@code Integer} Maximum number of returned user sessions. Ignored if negative or {@code null}.
     * @param offline {@code boolean} Flag to include offline sessions.
     * @param lastUserSessionId {@code String} Id of the user session. It will return only user sessions with id's lexicographically greater than this.
     * it will compare the id in dictionary order and takes only those created later.
     * @return Stream of {@link UserSessionModel}. Never returns {@code null}.
     */
    Stream<UserSessionModel> loadUserSessionsStream(Integer firstResult, Integer maxResults, boolean offline,
                                                    String lastUserSessionId);

    /**
     * Loads client session from the db by provided user session and client.
     * @param realm RealmModel Realm for the associated client session.
     * @param client ClientModel Client used for the creation of client session.
     * @param userSession UserSessionModel User session for the associated client session.
     * @param offline boolean Flag that indicates the client session should be online/offline.
     * @return Client session according the provided criteria or {@code null} if not found.
     */
    AuthenticatedClientSessionModel loadClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession, boolean offline);

    /**
     * Retrieves the count of user sessions for all realms.
     *
     * @param offline
     * @return
     *
     */
    int getUserSessionsCount(boolean offline);

    /**
     * Retrieves the count of user client-sessions for the given client
     *
     * @param realm
     * @param clientModel
     * @param offline
     * @return
     */
    int getUserSessionsCount(RealmModel realm, ClientModel clientModel, boolean offline);

    /**
     * Returns a {@link Map} containing the number of user-sessions aggregated by client id for the given realm.
     * @param realm
     * @param offline
     * @return the count {@link Map} with clientId as key and session count as value
     */
    Map<String, Long> getUserSessionsCountsByClients(RealmModel realm, boolean offline);

    /**
     * Remove the online user sessions for this realm.
     */
    default void removeUserSessions(RealmModel realm, boolean offline) {
        throw new IllegalArgumentException("not supported");
        // TODO: remove default implementation
    }

    /**
     * Removes all user sessions from the specified realm.
     */
    default void removeUserSessions(RealmModel realm) {
        removeUserSessions(realm, true);
        removeUserSessions(realm, false);
    }

}
