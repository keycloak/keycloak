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

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.Provider;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * Called during startup. For each userSession, it loads also clientSessions
     * @deprecated Use {@link #loadUserSessionsStream(Integer, Integer, boolean, Integer, String) loadUserSessionsStream} instead.
     */
    @Deprecated
    default List<UserSessionModel> loadUserSessions(int firstResult, int maxResults, boolean offline, int lastCreatedOn, String lastUserSessionId) {
        return loadUserSessionsStream(firstResult, maxResults, offline, lastCreatedOn, lastUserSessionId).collect(Collectors.toList());
    }

    /**
     * Called during startup. For each userSession, it loads also clientSessions.
     * @param firstResult {@code Integer} Index of the first desired user session. Ignored if negative or {@code null}.
     * @param maxResults {@code Integer} Maximum number of returned user sessions. Ignored if negative or {@code null}.
     * @param offline {@code boolean} Flag to include offline sessions.
     * @param lastCreatedOn {@code Integer} Timestamp when the user session was created. It will return only user sessions created later.
     * @param lastUserSessionId {@code String} Id of the user session. In case of equal {@code lastCreatedOn}
     * it will compare the id in dictionary order and takes only those created later.
     * @return Stream of {@link UserSessionModel}. Never returns {@code null}.
     */
    Stream<UserSessionModel> loadUserSessionsStream(Integer firstResult, Integer maxResults, boolean offline,
                                                    Integer lastCreatedOn, String lastUserSessionId);

    int getUserSessionsCount(boolean offline);

}
