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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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

    UserSessionModel createUserSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId);

    UserSessionModel createUserSession(String id, RealmModel realm, UserModel user, String loginUsername, String ipAddress,
                                       String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId, UserSessionModel.SessionPersistenceState persistenceState);

    UserSessionModel getUserSession(RealmModel realm, String id);

    /**
     * @deprecated Use {@link #getUserSessionsStream(RealmModel, ClientModel) getUserSessionsStream} instead.
     */
    @Deprecated
    default List<UserSessionModel> getUserSessions(RealmModel realm, UserModel user) {
        return this.getUserSessionsStream(realm, user).collect(Collectors.toList());
    }

    /**
     * Obtains the online user sessions associated with the specified user.
     *
     * @param realm a reference to the realm.
     * @param user the user whose sessions are being searched.
     * @return a non-null {@link Stream} of online user sessions.
     */
    Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, UserModel user);

    /**
     * @deprecated Use {@link #getUserSessionsStream(RealmModel, ClientModel) getUserSessionsStream} instead.
     */
    @Deprecated
    default List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client) {
        return this.getUserSessionsStream(realm, client).collect(Collectors.toList());
    }

    /**
     * Obtains the online user sessions associated with the specified client.
     *
     * @param realm a reference to the realm.
     * @param client the client whose user sessions are being searched.
     * @return a non-null {@link Stream} of online user sessions.
     */
    Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, ClientModel client);

    /**
     * @deprecated Use {@link #getUserSessionsStream(RealmModel, ClientModel, Integer, Integer) getUserSessionsStream} instead.
     */
    @Deprecated
    default List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client, int firstResult, int maxResults) {
        return this.getUserSessionsStream(realm, client, firstResult, maxResults).collect(Collectors.toList());
    }

    /**
     * Obtains the online user sessions associated with the specified client, starting from the {@code firstResult} and containing
     * at most {@code maxResults}.
     *
     * @param realm a reference tot he realm.
     * @param client the client whose user sessions are being searched.
     * @param firstResult first result to return. Ignored if negative or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of online user sessions.
     */
    Stream<UserSessionModel> getUserSessionsStream(RealmModel realm, ClientModel client, Integer firstResult, Integer maxResults);

    /**
     * @deprecated Use {@link #getUserSessionByBrokerUserIdStream(RealmModel, String) getUserSessionByBrokerUserIdStream}
     * instead.
     */
    @Deprecated
    default List<UserSessionModel> getUserSessionByBrokerUserId(RealmModel realm, String brokerUserId) {
        return this.getUserSessionByBrokerUserIdStream(realm, brokerUserId).collect(Collectors.toList());
    }

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
     * Return userSession of specified ID as long as the predicate passes. Otherwise returns {@code null}.
     * If predicate doesn't pass, implementation can do some best-effort actions to try have predicate passing (eg. download userSession from other DC)
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

    /**
     * @deprecated Use {@link UserLoginFailureProvider#getUserLoginFailure(RealmModel, String) getUserLoginFailure} instead.
     */
    @Deprecated
    default UserLoginFailureModel getUserLoginFailure(RealmModel realm, String userId) {
        return getKeycloakSession().loginFailures().getUserLoginFailure(realm, userId);
    }

    /**
     * @deprecated Use {@link UserLoginFailureProvider#addUserLoginFailure(RealmModel, String) addUserLoginFailure} instead.
     */
    @Deprecated
    default UserLoginFailureModel addUserLoginFailure(RealmModel realm, String userId) {
        return getKeycloakSession().loginFailures().addUserLoginFailure(realm, userId);
    }

    /**
     * @deprecated Use {@link UserLoginFailureProvider#removeUserLoginFailure(RealmModel, String) removeUserLoginFailure} instead.
     */
    @Deprecated
    default void removeUserLoginFailure(RealmModel realm, String userId) {
        getKeycloakSession().loginFailures().removeUserLoginFailure(realm, userId);
    }

    /**
     * @deprecated Use {@link UserLoginFailureProvider#removeAllUserLoginFailures(RealmModel) removeAllUserLoginFailures} instead.
     */
    @Deprecated
    default void removeAllUserLoginFailures(RealmModel realm) {
        getKeycloakSession().loginFailures().removeAllUserLoginFailures(realm);
    }

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
     * @deprecated Use {@link #getOfflineUserSessionsStream(RealmModel, UserModel) getOfflineUserSessionsStream} instead.
     */
    @Deprecated
    default List<UserSessionModel> getOfflineUserSessions(RealmModel realm, UserModel user) {
        return this.getOfflineUserSessionsStream(realm, user).collect(Collectors.toList());
    }

    /**
     * Obtains the offline user sessions associated with the specified user.
     *
     * @param realm a reference to the realm.
     * @param user the user whose offline sessions are being searched.
     * @return a non-null {@link Stream} of offline user sessions.
     */
    Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, UserModel user);

    UserSessionModel getOfflineUserSessionByBrokerSessionId(RealmModel realm, String brokerSessionId);

    /**
     * @deprecated Use {@link #getOfflineUserSessionByBrokerUserIdStream(RealmModel, String) getOfflineUserSessionByBrokerUserIdStream}
     * instead.
     */
    @Deprecated
    default List<UserSessionModel> getOfflineUserSessionByBrokerUserId(RealmModel realm, String brokerUserId) {
        return this.getOfflineUserSessionByBrokerUserIdStream(realm, brokerUserId).collect(Collectors.toList());
    }

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
     * @deprecated use {@link #getOfflineUserSessionsStream(RealmModel, ClientModel, Integer, Integer) getOfflineUserSessionsStream}
     * instead.
     */
    @Deprecated
    default List<UserSessionModel> getOfflineUserSessions(RealmModel realm, ClientModel client, int first, int max) {
        return this.getOfflineUserSessionsStream(realm, client, first, max).collect(Collectors.toList());
    }

    /**
     * Obtains the offline user sessions associated with the specified client, starting from the {@code firstResult} and
     * containing at most {@code maxResults}.
     *
     * @param realm a reference tot he realm.
     * @param client the client whose user sessions are being searched.
     * @param firstResult first result to return. Ignored if negative or {@code null}.
     * @param maxResults maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of offline user sessions.
     */
    Stream<UserSessionModel> getOfflineUserSessionsStream(RealmModel realm, ClientModel client, Integer firstResult, Integer maxResults);

    /** Triggered by persister during pre-load. It imports authenticatedClientSessions too **/
    void importUserSessions(Collection<UserSessionModel> persistentUserSessions, boolean offline);

    void close();

    int getStartupTime(RealmModel realm);
}
