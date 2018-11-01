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

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserSessionProvider extends Provider {

    AuthenticatedClientSessionModel createClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession);
    AuthenticatedClientSessionModel getClientSession(UserSessionModel userSession, ClientModel client, UUID clientSessionId, boolean offline);

    UserSessionModel createUserSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId);
    UserSessionModel createUserSession(String id, RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId);
    UserSessionModel getUserSession(RealmModel realm, String id);
    List<UserSessionModel> getUserSessions(RealmModel realm, UserModel user);
    List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client);
    List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client, int firstResult, int maxResults);
    List<UserSessionModel> getUserSessionByBrokerUserId(RealmModel realm, String brokerUserId);
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

    /** Implementation doesn't need to propagate removal of expired userSessions to userSessionPersister. Cleanup on persister will be called separately **/
    void removeExpired(RealmModel realm);
    void removeUserSessions(RealmModel realm);

    UserLoginFailureModel getUserLoginFailure(RealmModel realm, String userId);
    UserLoginFailureModel addUserLoginFailure(RealmModel realm, String userId);
    void removeUserLoginFailure(RealmModel realm, String userId);
    void removeAllUserLoginFailures(RealmModel realm);

    void onRealmRemoved(RealmModel realm);
    void onClientRemoved(RealmModel realm, ClientModel client);

    /** Newly created userSession won't contain attached AuthenticatedClientSessions **/
    UserSessionModel createOfflineUserSession(UserSessionModel userSession);
    UserSessionModel getOfflineUserSession(RealmModel realm, String userSessionId);

    /** Removes the attached clientSessions as well **/
    void removeOfflineUserSession(RealmModel realm, UserSessionModel userSession);

    /** Will automatically attach newly created offline client session to the offlineUserSession **/
    AuthenticatedClientSessionModel createOfflineClientSession(AuthenticatedClientSessionModel clientSession, UserSessionModel offlineUserSession);
    List<UserSessionModel> getOfflineUserSessions(RealmModel realm, UserModel user);

    long getOfflineSessionsCount(RealmModel realm, ClientModel client);
    List<UserSessionModel> getOfflineUserSessions(RealmModel realm, ClientModel client, int first, int max);

    /** Triggered by persister during pre-load. It imports authenticatedClientSessions too **/
    void importUserSessions(Collection<UserSessionModel> persistentUserSessions, boolean offline);

    void close();

}
