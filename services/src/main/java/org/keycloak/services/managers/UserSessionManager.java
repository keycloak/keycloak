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
package org.keycloak.services.managers;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.services.ServicesLogger;


/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserSessionManager {

    protected static ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    private final KeycloakSession kcSession;
    private final UserSessionPersisterProvider persister;

    public UserSessionManager(KeycloakSession session) {
        this.kcSession = session;
        this.persister = session.getProvider(UserSessionPersisterProvider.class);
    }

    public void createOrUpdateOfflineSession(ClientSessionModel clientSession, UserSessionModel userSession) {
        UserModel user = userSession.getUser();

        // Create and persist offline userSession if we don't have one
        UserSessionModel offlineUserSession = kcSession.sessions().getOfflineUserSession(clientSession.getRealm(), userSession.getId());
        if (offlineUserSession == null) {
            offlineUserSession = createOfflineUserSession(user, userSession);
        } else {
            // update lastSessionRefresh but don't need to persist
            offlineUserSession.setLastSessionRefresh(Time.currentTime());
        }

        // Create and persist clientSession
        ClientSessionModel offlineClientSession = kcSession.sessions().getOfflineClientSession(clientSession.getRealm(), clientSession.getId());
        if (offlineClientSession == null) {
            createOfflineClientSession(user, clientSession, offlineUserSession);
        }
    }

    // userSessionId is provided from offline token. It's used just to verify if it match the ID from clientSession representation
    public ClientSessionModel findOfflineClientSession(RealmModel realm, String clientSessionId, String userSessionId) {
        ClientSessionModel clientSession = kcSession.sessions().getOfflineClientSession(realm, clientSessionId);
        if (clientSession == null) {
            return null;
        }

        if (!userSessionId.equals(clientSession.getUserSession().getId())) {
            throw new ModelException("User session don't match. Offline client session " + clientSession.getId() + ", It's user session " + clientSession.getUserSession().getId() +
                    "  Wanted user session: " + userSessionId);
        }

        return clientSession;
    }

    public Set<ClientModel> findClientsWithOfflineToken(RealmModel realm, UserModel user) {
        List<ClientSessionModel> clientSessions = kcSession.sessions().getOfflineClientSessions(realm, user);
        Set<ClientModel> clients = new HashSet<>();
        for (ClientSessionModel clientSession : clientSessions) {
            clients.add(clientSession.getClient());
        }
        return clients;
    }

    public List<UserSessionModel> findOfflineSessions(RealmModel realm, ClientModel client, UserModel user) {
        List<ClientSessionModel> clientSessions = kcSession.sessions().getOfflineClientSessions(realm, user);
        List<UserSessionModel> userSessions = new LinkedList<>();
        for (ClientSessionModel clientSession : clientSessions) {
            userSessions.add(clientSession.getUserSession());
        }
        return userSessions;
    }

    public boolean revokeOfflineToken(UserModel user, ClientModel client) {
        RealmModel realm = client.getRealm();

        List<ClientSessionModel> clientSessions = kcSession.sessions().getOfflineClientSessions(realm, user);
        boolean anyRemoved = false;
        for (ClientSessionModel clientSession : clientSessions) {
            if (clientSession.getClient().getId().equals(client.getId())) {
                if (logger.isTraceEnabled()) {
                    logger.tracef("Removing existing offline token for user '%s' and client '%s' . ClientSessionID was '%s' .",
                            user.getUsername(), client.getClientId(), clientSession.getId());
                }

                kcSession.sessions().removeOfflineClientSession(realm, clientSession.getId());
                persister.removeClientSession(clientSession.getId(), true);
                checkOfflineUserSessionHasClientSessions(realm, user, clientSession.getUserSession(), clientSessions);
                anyRemoved = true;
            }
        }

        return anyRemoved;
    }

    public void revokeOfflineUserSession(UserSessionModel userSession) {
        if (logger.isTraceEnabled()) {
            logger.tracef("Removing offline user session '%s' for user '%s' ", userSession.getId(), userSession.getLoginUsername());
        }
        kcSession.sessions().removeOfflineUserSession(userSession.getRealm(), userSession);
        persister.removeUserSession(userSession.getId(), true);
    }

    public boolean isOfflineTokenAllowed(ClientSessionModel clientSession) {
        RoleModel offlineAccessRole = clientSession.getRealm().getRole(Constants.OFFLINE_ACCESS_ROLE);
        if (offlineAccessRole == null) {
            logger.roleNotInRealm(Constants.OFFLINE_ACCESS_ROLE);
            return false;
        }

        return clientSession.getRoles().contains(offlineAccessRole.getId());
    }

    private UserSessionModel createOfflineUserSession(UserModel user, UserSessionModel userSession) {
        if (logger.isTraceEnabled()) {
            logger.tracef("Creating new offline user session. UserSessionID: '%s' , Username: '%s'", userSession.getId(), user.getUsername());
        }

        UserSessionModel offlineUserSession = kcSession.sessions().createOfflineUserSession(userSession);
        persister.createUserSession(offlineUserSession, true);
        return offlineUserSession;
    }

    private void createOfflineClientSession(UserModel user, ClientSessionModel clientSession, UserSessionModel userSession) {
        if (logger.isTraceEnabled()) {
            logger.tracef("Creating new offline token client session. ClientSessionId: '%s', UserSessionID: '%s' , Username: '%s', Client: '%s'" ,
                    clientSession.getId(), userSession.getId(), user.getUsername(), clientSession.getClient().getClientId());
        }

        ClientSessionModel offlineClientSession = kcSession.sessions().createOfflineClientSession(clientSession);
        offlineClientSession.setUserSession(userSession);
        persister.createClientSession(clientSession, true);
    }

    // Check if userSession has any offline clientSessions attached to it. Remove userSession if not
    private void checkOfflineUserSessionHasClientSessions(RealmModel realm, UserModel user, UserSessionModel userSession, List<ClientSessionModel> clientSessions) {
        String userSessionId = userSession.getId();
        for (ClientSessionModel clientSession : clientSessions) {
            if (clientSession.getUserSession().getId().equals(userSessionId)) {
                return;
            }
        }

        if (logger.isTraceEnabled()) {
            logger.tracef("Removing offline userSession for user %s as it doesn't have any client sessions attached. UserSessionID: %s", user.getUsername(), userSessionId);
        }
        kcSession.sessions().removeOfflineUserSession(realm, userSession);
        persister.removeUserSession(userSessionId, true);
    }
}
