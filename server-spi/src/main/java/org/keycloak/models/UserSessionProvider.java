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

package org.keycloak.models;

import org.keycloak.provider.Provider;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserSessionProvider extends Provider {

    /**
     * Returns currently used Keycloak session.
     * @return {@link KeycloakSession}
     */
    KeycloakSession getKeycloakSession();

    AuthenticatedClientSessionModel createClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession);
    
    /**
     * @deprecated Use {@link #getClientSession(UserSessionModel, ClientModel, String, boolean)} instead.
     */
    default AuthenticatedClientSessionModel getClientSession(UserSessionModel userSession, ClientModel client, UUID clientSessionId, boolean offline) {
        return getClientSession(userSession, client, clientSessionId == null ? null : clientSessionId.toString(), offline);
    }
    AuthenticatedClientSessionModel getClientSession(UserSessionModel userSession, ClientModel client, String clientSessionId, boolean offline);

    /**
     * @deprecated Use {@link #createUserSession(String, RealmModel, UserModel, String, String, String, boolean, String, String, UserSessionModel.SessionPersistenceState)} instead.
     */
    default UserSessionModel createUserSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        return createUserSession(null, realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId,
                brokerUserId, UserSessionModel.SessionPersistenceState.PERSISTENT);
    }

    /**
     * Creates a new user session with the given parameters.
     *
     * @param id identifier. Is generated if {@code null}
     * @param realm the realm
     * @param user user associated with the created user session
     * @param loginUsername
     * @param ipAddress
     * @param authMethod
     * @param rememberMe
     * @param brokerSessionId
     * @param brokerUserId
     * @param persistenceState
     * @return Model of the created user session
     */
    UserSessionModel createUserSession(String id, RealmModel realm, UserModel user, String loginUsername, String ipAddress,
                                       String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId, UserSessionModel.SessionPersistenceState persistenceState);

    UserSessionModel getUserSession(RealmModel realm, String id);

    /**
     * Obtains the online user sessions associated with the specified user.
     *
     * @param realm a reference to the realm.
     * @param user the user whose sessions are being searched.
     * @return a non-null {@link Stream} of online user sessions.
     */
    Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, UserModel user);

    /**
     * Obtains the online user sessions associated with the specified client.
     *
     * @param realm a reference to the realm.
     * @param client the client whose user sessions are being searched.
     * @return a non-null {@link Stream} of online user sessions.
     */
    Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, ClientModel client);

    /**
     * Obtains the online user sessions associated with the specified client, starting from the {@code firstResult} and containing
     * at most {@code maxResults}.
     *
     * @param realm a reference to the realm.
     * @param client the client whose user sessions are being searched.
     * @param firstResult first result to return. Ignored if negative or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of online user sessions.
     */
    Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, ClientModel client, Integer firstResult, Integer maxResults);

    /**
     * Obtains the online user sessions associated with the user that matches the specified {@code brokerUserId}.
     *
     * @param realm a reference to the realm.
     * @param brokerUserId the id of the broker user whose sessions are being searched.
     * @return a non-null {@link Stream} of online user sessions.
     */
    Stream<UserSessionModel> getUserSessionByBrokerUserIdStream(RealmModel realm, String brokerUserId);

    UserSessionModel getUserSessionByBrokerSessionId(RealmModel realm, String brokerSessionId);

    /**
     * Return userSession of specified ID as long as the predicate passes. Otherwise, returns {@code null}.
     * If predicate doesn't pass, implementation can do some best-effort actions to try to have predicate passing (e.g. download userSession from other DC)
     */
    UserSessionModel getUserSessionWithPredicate(RealmModel realm, String id, boolean offline, Predicate<UserSessionModel> predicate);

    long getActiveUserSessions(RealmModel realm, ClientModel client);

    /**
     * Returns a summary of client sessions key is client.getId()
     *
     * @param realm
     * @param offline
     * @return
     */
    Map<String, Long> getActiveClientSessionStats(RealmModel realm, boolean offline);

    /** This will remove attached ClientLoginSessionModels too **/
    void removeUserSession(RealmModel realm, UserSessionModel session);
    void removeUserSessions(RealmModel realm, UserModel user);

    /**
     * Remove expired user sessions and client sessions in all the realms
     */
    void removeAllExpired();

    /**
     * Removes expired user sessions owned by this realm from this provider.
     * If this `UserSessionProvider` uses `UserSessionPersister`, the removal of the expired
     * {@link UserSessionModel user sessions} is also propagated to relevant `UserSessionPersister`.
     *
     * @param realm {@link RealmModel} Realm where all the expired user sessions to be removed from.
     */
    void removeExpired(RealmModel realm);

    void removeUserSessions(RealmModel realm);

    void onRealmRemoved(RealmModel realm);
    void onClientRemoved(RealmModel realm, ClientModel client);

    /** Newly created userSession won't contain attached AuthenticatedClientSessions **/
    UserSessionModel createOfflineUserSession(UserSessionModel userSession);
    UserSessionModel getOfflineUserSession(RealmModel realm, String userSessionId);

    /** Removes the attached clientSessions as well **/
    void removeOfflineUserSession(RealmModel realm, UserSessionModel userSession);

    /** Will automatically attach newly created offline client session to the offlineUserSession **/
    AuthenticatedClientSessionModel createOfflineClientSession(AuthenticatedClientSessionModel clientSession, UserSessionModel offlineUserSession);

    /**
     * Obtains the offline user sessions associated with the specified user.
     *
     * @param realm a reference to the realm.
     * @param user the user whose offline sessions are being searched.
     * @return a non-null {@link Stream} of offline user sessions.
     */
    Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, UserModel user);

    /**
     * Obtains the offline user sessions associated with the user that matches the specified {@code brokerUserId}.
     *
     * @param realm a reference to the realm.
     * @param brokerUserId the id of the broker user whose sessions are being searched.
     * @return a non-null {@link Stream} of offline user sessions.
     */
    Stream<UserSessionModel> getOfflineUserSessionByBrokerUserIdStream(RealmModel realm, String brokerUserId);

    long getOfflineSessionsCount(RealmModel realm, ClientModel client);

    /**
     * Obtains the offline user sessions associated with the specified client, starting from the {@code firstResult} and
     * containing at most {@code maxResults}.
     *
     * @param realm a reference to the realm.
     * @param client the client whose user sessions are being searched.
     * @param firstResult first result to return. Ignored if negative or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of offline user sessions.
     */
    Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, ClientModel client, Integer firstResult, Integer maxResults);

    /** Triggered by persister during pre-load. It imports authenticatedClientSessions too.
     *
     * @deprecated Deprecated as offline session preloading was removed in KC25. This method will be removed in KC27.
     */
    @Deprecated(forRemoval = true)
    void importUserSessions(Collection<UserSessionModel> persistentUserSessions, boolean offline);

    void close();

    int getStartupTime(RealmModel realm);
}
