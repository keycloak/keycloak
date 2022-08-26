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

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.ServicesLogger;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserSessionManager {

    private static final Logger logger = Logger.getLogger(UserSessionManager.class);

    private final KeycloakSession kcSession;

    public UserSessionManager(KeycloakSession session) {
        this.kcSession = session;
    }

    public void createOrUpdateOfflineSession(AuthenticatedClientSessionModel clientSession, UserSessionModel userSession) {
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
        AuthenticatedClientSessionModel offlineClientSession = offlineUserSession.getAuthenticatedClientSessionByClient(clientSession.getClient().getId());
        if (offlineClientSession == null) {
            createOfflineClientSession(user, clientSession, offlineUserSession);
        }
    }


    public UserSessionModel findOfflineUserSession(RealmModel realm, String userSessionId) {
        return kcSession.sessions().getOfflineUserSession(realm, userSessionId);
    }

    public Set<ClientModel> findClientsWithOfflineToken(RealmModel realm, UserModel user) {
        return kcSession.sessions().getOfflineUserSessionsStream(realm, user)
                .flatMap(userSession -> userSession.getAuthenticatedClientSessions().keySet().stream())
                .map(clientUUID -> realm.getClientById(clientUUID))
                .collect(Collectors.toSet());
    }

    @Deprecated
    public List<UserSessionModel> findOfflineSessions(RealmModel realm, UserModel user) {
        return this.findOfflineSessionsStream(realm, user).collect(Collectors.toList());
    }

    public Stream<UserSessionModel> findOfflineSessionsStream(RealmModel realm, UserModel user) {
        return kcSession.sessions().getOfflineUserSessionsStream(realm, user);
    }

    public boolean revokeOfflineToken(UserModel user, ClientModel client) {
        RealmModel realm = client.getRealm();

        AtomicBoolean anyRemoved = new AtomicBoolean(false);
        kcSession.sessions().getOfflineUserSessionsStream(realm, user).collect(Collectors.toList())
                .forEach(userSession -> {
                    AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
                    if (clientSession != null) {
                        if (logger.isTraceEnabled()) {
                            logger.tracef("Removing existing offline token for user '%s' and client '%s' .",
                                    user.getUsername(), client.getClientId());
                        }

                        clientSession.detachFromUserSession();
                        checkOfflineUserSessionHasClientSessions(realm, user, userSession);
                        anyRemoved.set(true);
                    }
                });

        return anyRemoved.get();
    }

    public void revokeOfflineUserSession(UserSessionModel userSession) {
        if (logger.isTraceEnabled()) {
            logger.tracef("Removing offline user session '%s' for user '%s' ", userSession.getId(), userSession.getLoginUsername());
        }
        kcSession.sessions().removeOfflineUserSession(userSession.getRealm(), userSession);
    }

    public boolean isOfflineTokenAllowed(ClientSessionContext clientSessionCtx) {
        RoleModel offlineAccessRole = clientSessionCtx.getClientSession().getRealm().getRole(Constants.OFFLINE_ACCESS_ROLE);
        if (offlineAccessRole == null) {
            ServicesLogger.LOGGER.roleNotInRealm(Constants.OFFLINE_ACCESS_ROLE);
            return false;
        }

        // Check if offline_access is allowed here. Even through composite roles
        return clientSessionCtx.getRolesStream().collect(Collectors.toSet()).contains(offlineAccessRole);
    }

    private UserSessionModel createOfflineUserSession(UserModel user, UserSessionModel userSession) {
        if (logger.isTraceEnabled()) {
            logger.tracef("Creating new offline user session. UserSessionID: '%s' , Username: '%s'", userSession.getId(), user.getUsername());
        }

        UserSessionModel offlineUserSession = kcSession.sessions().createOfflineUserSession(userSession);
        return offlineUserSession;
    }

    private void createOfflineClientSession(UserModel user, AuthenticatedClientSessionModel clientSession, UserSessionModel offlineUserSession) {
        if (logger.isTraceEnabled()) {
            logger.tracef("Creating new offline token client session. ClientSessionId: '%s', UserSessionID: '%s' , Username: '%s', Client: '%s'" ,
                    clientSession.getId(), offlineUserSession.getId(), user.getUsername(), clientSession.getClient().getClientId());
        }

        kcSession.sessions().createOfflineClientSession(clientSession, offlineUserSession);
    }

    // Check if userSession has any offline clientSessions attached to it. Remove userSession if not
    private void checkOfflineUserSessionHasClientSessions(RealmModel realm, UserModel user, UserSessionModel userSession) {
        // TODO: Might need optimization to prevent loading client sessions from cache
        if (! userSession.getAuthenticatedClientSessions().isEmpty()) {
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.tracef("Removing offline userSession for user %s as it doesn't have any client sessions attached. UserSessionID: %s", user.getUsername(), userSession.getId());
        }
        kcSession.sessions().removeOfflineUserSession(realm, userSession);
    }
}
