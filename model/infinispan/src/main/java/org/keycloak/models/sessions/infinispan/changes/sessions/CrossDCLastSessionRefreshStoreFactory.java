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

import org.infinispan.Cache;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CrossDCLastSessionRefreshStoreFactory extends AbstractLastSessionRefreshStoreFactory {

    // Name of periodic tasks to send events to the other DCs
    public static final String LSR_PERIODIC_TASK_NAME = "lastSessionRefreshes";
    public static final String LSR_OFFLINE_PERIODIC_TASK_NAME = "lastSessionRefreshes-offline";


    public CrossDCLastSessionRefreshStore createAndInit(KeycloakSession kcSession, Cache<String, SessionEntityWrapper<UserSessionEntity>> cache, boolean offline) {
        return createAndInit(kcSession, cache, DEFAULT_TIMER_INTERVAL_MS, DEFAULT_MAX_INTERVAL_BETWEEN_MESSAGES_SECONDS, DEFAULT_MAX_COUNT, offline);
    }


    public CrossDCLastSessionRefreshStore createAndInit(KeycloakSession kcSession, Cache<String, SessionEntityWrapper<UserSessionEntity>> cache,
                                                        long timerIntervalMs, int maxIntervalBetweenMessagesSeconds, int maxCount, boolean offline) {
        String eventKey = offline ? LSR_OFFLINE_PERIODIC_TASK_NAME :  LSR_PERIODIC_TASK_NAME;
        CrossDCLastSessionRefreshStore store = createStoreInstance(maxIntervalBetweenMessagesSeconds, maxCount, eventKey);

        // Register listener
        ClusterProvider cluster = kcSession.getProvider(ClusterProvider.class);
        cluster.registerListener(eventKey, new CrossDCLastSessionRefreshListener(kcSession, cache, offline));

        // Setup periodic timer check
        setupPeriodicTimer(kcSession, store, timerIntervalMs, eventKey);

        return store;
    }


    protected CrossDCLastSessionRefreshStore createStoreInstance(int maxIntervalBetweenMessagesSeconds, int maxCount, String eventKey) {
        return new CrossDCLastSessionRefreshStore(maxIntervalBetweenMessagesSeconds, maxCount, eventKey);
    }



}
