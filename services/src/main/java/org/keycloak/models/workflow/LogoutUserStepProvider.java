/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.workflow;

import java.util.List;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.services.managers.AuthenticationManager;

import org.jboss.logging.Logger;

public class LogoutUserStepProvider implements WorkflowStepProvider {

    public static final String CONFIG_INCLUDE_OFFLINE_SESSIONS = "include-offline-sessions";

    private final Logger log = Logger.getLogger(LogoutUserStepProvider.class);

    private final KeycloakSession session;
    private final ComponentModel stepModel;

    public LogoutUserStepProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.stepModel = model;
    }

    @Override
    public void close() {
    }

    @Override
    public void run(WorkflowExecutionContext context) {
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, context.getResourceId());

        if (user == null) {
            return;
        }

        log.debugv("Logging out user {0} ({1})", user.getUsername(), user.getId());

        if (!LightweightUserAdapter.isLightweightUser(user)) {
            session.users().setNotBeforeForUser(realm, user, Time.currentTime());
        }

        // offline sessions go first, as logging them out also removes the matching online sessions
        if (stepModel.get(CONFIG_INCLUDE_OFFLINE_SESSIONS, false)) {
            logout(realm, session.sessions().getOfflineUserSessionsStream(realm, user).toList());
        }

        logout(realm, session.sessions().getUserSessionsStream(realm, user).toList());
    }

    private void logout(RealmModel realm, List<UserSessionModel> userSessions) {
        // steps run without an active request, so there is no request URI or headers to pass
        ClientConnection connection = session.getContext().getConnection();
        userSessions.forEach(userSession -> AuthenticationManager.backchannelLogout(session, realm, userSession,
                null, connection, null, true));
    }
}
