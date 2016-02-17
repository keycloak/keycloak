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

package org.keycloak.models.sessions.infinispan.initializer;

import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflineUserSessionLoader implements SessionLoader {

    private static final Logger log = Logger.getLogger(OfflineUserSessionLoader.class);

    @Override
    public void init(KeycloakSession session) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);

        // TODO: check if update of timestamps in persister can be skipped entirely
        int clusterStartupTime = session.getProvider(ClusterProvider.class).getClusterStartupTime();

        log.debugf("Clearing detached sessions from persistent storage and updating timestamps to %d", clusterStartupTime);

        persister.clearDetachedUserSessions();
        persister.updateAllTimestamps(clusterStartupTime);
    }

    @Override
    public int getSessionsCount(KeycloakSession session) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        return persister.getUserSessionsCount(true);
    }

    @Override
    public boolean loadSessions(KeycloakSession session, int first, int max) {
        if (log.isTraceEnabled()) {
            log.tracef("Loading sessions - first: %d, max: %d", first, max);
        }

        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        List<UserSessionModel> sessions = persister.loadUserSessions(first, max, true);

        for (UserSessionModel persistentSession : sessions) {

            // Save to memory/infinispan
            UserSessionModel offlineUserSession = session.sessions().importUserSession(persistentSession, true);

            for (ClientSessionModel persistentClientSession : persistentSession.getClientSessions()) {
                ClientSessionModel offlineClientSession = session.sessions().importClientSession(persistentClientSession, true);
                offlineClientSession.setUserSession(offlineUserSession);
            }
        }

        return true;
    }


}
