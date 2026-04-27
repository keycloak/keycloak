/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.changes.sessions;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.SessionTimeoutHelper;

import org.jboss.logging.Logger;

/**
 * The store is supposed to do periodic bulk update of lastSessionRefresh times of all userSessions, which were refreshed during some period
 * of time. The updates are sent to UserSessionPersisterProvider (DB)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PersisterLastSessionRefreshStore extends AbstractLastSessionRefreshStore {

    protected static final Logger logger = Logger.getLogger(PersisterLastSessionRefreshStore.class);

    private final boolean offline;

    protected PersisterLastSessionRefreshStore(int maxIntervalBetweenMessagesSeconds, int maxCount, boolean offline) {
        super(maxIntervalBetweenMessagesSeconds, maxCount);
        this.offline = offline;
    }


    protected void sendMessage(KeycloakSession kcSession, Map<String, SessionData> refreshesToSend) {
        Map<String, Set<String>> sessionIdsByRealm =
                refreshesToSend.entrySet().stream().collect(
                        Collectors.groupingBy(entry -> entry.getValue().realmId(),
                                Collectors.mapping(Map.Entry::getKey, Collectors.toSet())));

        // Update DB with a bit lower value than current time to ensure 'revokeRefreshToken' will work correctly taking server
        int lastSessionRefresh = Time.currentTime() - SessionTimeoutHelper.PERIODIC_TASK_INTERVAL_SECONDS;

        if (logger.isDebugEnabled()) {
            logger.debugf("Updating %d userSessions with lastSessionRefresh: %d", refreshesToSend.size(), lastSessionRefresh);
        }

        // Separate transaction for each bulk update request to avoid deadlocks
        for (Map.Entry<String, Set<String>> entry : sessionIdsByRealm.entrySet()) {
            KeycloakModelUtils.runJobInTransaction(kcSession.getKeycloakSessionFactory(), (kcSession2) -> {
                UserSessionPersisterProvider persister = kcSession2.getProvider(UserSessionPersisterProvider.class);
                RealmModel realm = kcSession2.realms().getRealm(entry.getKey());

                // If realm is null, it means that realm was deleted in the meantime. UserSessions were already deleted as well (callback for realm deletion)
                if (realm != null) {
                    Set<String> userSessionIds = new TreeSet<>(entry.getValue());

                    persister.updateLastSessionRefreshes(realm, lastSessionRefresh, userSessionIds, offline);
                }
            });
        }
    }
}
