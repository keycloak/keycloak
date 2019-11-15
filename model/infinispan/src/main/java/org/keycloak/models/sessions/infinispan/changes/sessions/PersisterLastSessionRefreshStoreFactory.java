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

import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PersisterLastSessionRefreshStoreFactory extends AbstractLastSessionRefreshStoreFactory {

    // Name of periodic task to update DB with lastSessionRefreshes
    public static final String DB_LSR_PERIODIC_TASK_NAME = "db-last-session-refresh";

    public PersisterLastSessionRefreshStore createAndInit(KeycloakSession kcSession, boolean offline) {
        return createAndInit(kcSession, DEFAULT_TIMER_INTERVAL_MS, DEFAULT_MAX_INTERVAL_BETWEEN_MESSAGES_SECONDS, DEFAULT_MAX_COUNT, offline);
    }


    private PersisterLastSessionRefreshStore createAndInit(KeycloakSession kcSession,
                                                          long timerIntervalMs, int maxIntervalBetweenMessagesSeconds, int maxCount, boolean offline) {
        PersisterLastSessionRefreshStore store = createStoreInstance(maxIntervalBetweenMessagesSeconds, maxCount, offline);

        // Setup periodic timer check
        setupPeriodicTimer(kcSession, store, timerIntervalMs, DB_LSR_PERIODIC_TASK_NAME);

        return store;
    }


    protected PersisterLastSessionRefreshStore createStoreInstance(int maxIntervalBetweenMessagesSeconds, int maxCount, boolean offline) {
        return new PersisterLastSessionRefreshStore(maxIntervalBetweenMessagesSeconds, maxCount, offline);
    }
}
