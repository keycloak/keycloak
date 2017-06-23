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

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserSessionProvider extends Provider {

    AuthenticatedClientSessionModel createClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession);

    UserSessionModel createUserSession(String id, RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId);
    UserSessionModel getUserSession(RealmModel realm, String id);
    List<UserSessionModel> getUserSessions(RealmModel realm, UserModel user);
    List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client);
    List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client, int firstResult, int maxResults);
    List<UserSessionModel> getUserSessionByBrokerUserId(RealmModel realm, String brokerUserId);
    UserSessionModel getUserSessionByBrokerSessionId(RealmModel realm, String brokerSessionId);

    long getActiveUserSessions(RealmModel realm, ClientModel client);

    /** This will remove attached ClientLoginSessionModels too **/
    void removeUserSession(RealmModel realm, UserSessionModel session);
    void removeUserSessions(RealmModel realm, UserModel user);

    /** Implementation should propagate removal of expired userSessions to userSessionPersister too **/
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

    /** Triggered by persister during pre-load. It optionally imports authenticatedClientSessions too if requested. Otherwise the imported UserSession will have empty list of AuthenticationSessionModel **/
    UserSessionModel importUserSession(UserSessionModel persistentUserSession, boolean offline, boolean importAuthenticatedClientSessions);

    void close();

}
