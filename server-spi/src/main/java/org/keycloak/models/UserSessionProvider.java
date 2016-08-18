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

    ClientSessionModel createClientSession(RealmModel realm, ClientModel client);
    ClientSessionModel getClientSession(RealmModel realm, String id);
    ClientSessionModel getClientSession(String id);

    UserSessionModel createUserSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId);
    UserSessionModel getUserSession(RealmModel realm, String id);
    List<UserSessionModel> getUserSessions(RealmModel realm, UserModel user);
    List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client);
    List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client, int firstResult, int maxResults);
    List<UserSessionModel> getUserSessionByBrokerUserId(RealmModel realm, String brokerUserId);
    UserSessionModel getUserSessionByBrokerSessionId(RealmModel realm, String brokerSessionId);

    long getActiveUserSessions(RealmModel realm, ClientModel client);
    void removeUserSession(RealmModel realm, UserSessionModel session);
    void removeUserSessions(RealmModel realm, UserModel user);

    // Implementation should propagate removal of expired userSessions to userSessionPersister too
    void removeExpired(RealmModel realm);
    void removeUserSessions(RealmModel realm);
    void removeClientSession(RealmModel realm, ClientSessionModel clientSession);

    UserLoginFailureModel getUserLoginFailure(RealmModel realm, String userId);
    UserLoginFailureModel addUserLoginFailure(RealmModel realm, String userId);
    void removeUserLoginFailure(RealmModel realm, String userId);
    void removeAllUserLoginFailures(RealmModel realm);

    void onRealmRemoved(RealmModel realm);
    void onClientRemoved(RealmModel realm, ClientModel client);
    void onUserRemoved(RealmModel realm, UserModel user);

    UserSessionModel createOfflineUserSession(UserSessionModel userSession);
    UserSessionModel getOfflineUserSession(RealmModel realm, String userSessionId);

    // Removes the attached clientSessions as well
    void removeOfflineUserSession(RealmModel realm, UserSessionModel userSession);

    ClientSessionModel createOfflineClientSession(ClientSessionModel clientSession);
    ClientSessionModel getOfflineClientSession(RealmModel realm, String clientSessionId);
    List<ClientSessionModel> getOfflineClientSessions(RealmModel realm, UserModel user);

    // Don't remove userSession even if it's last userSession
    void removeOfflineClientSession(RealmModel realm, String clientSessionId);

    long getOfflineSessionsCount(RealmModel realm, ClientModel client);
    List<UserSessionModel> getOfflineUserSessions(RealmModel realm, ClientModel client, int first, int max);

    // Triggered by persister during pre-load
    UserSessionModel importUserSession(UserSessionModel persistentUserSession, boolean offline);
    ClientSessionModel importClientSession(ClientSessionModel persistentClientSession, boolean offline);

    ClientInitialAccessModel createClientInitialAccessModel(RealmModel realm, int expiration, int count);
    ClientInitialAccessModel getClientInitialAccessModel(RealmModel realm, String id);
    void removeClientInitialAccessModel(RealmModel realm, String id);
    List<ClientInitialAccessModel> listClientInitialAccess(RealmModel realm);

    ClientRegistrationTrustedHostModel createClientRegistrationTrustedHostModel(RealmModel realm, String hostName, int count);
    ClientRegistrationTrustedHostModel getClientRegistrationTrustedHostModel(RealmModel realm, String hostName);
    void removeClientRegistrationTrustedHostModel(RealmModel realm, String hostName);
    List<ClientRegistrationTrustedHostModel> listClientRegistrationTrustedHosts(RealmModel realm);

    void close();

}
