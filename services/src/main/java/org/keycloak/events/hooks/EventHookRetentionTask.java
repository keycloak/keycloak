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

package org.keycloak.events.hooks;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.timer.ScheduledTask;

import static org.keycloak.common.util.Time.currentTimeMillis;
import static org.keycloak.models.utils.KeycloakModelUtils.runJobInTransaction;

public class EventHookRetentionTask implements ScheduledTask {

    static final long DEFAULT_RETENTION_MILLIS = java.util.concurrent.TimeUnit.DAYS.toMillis(30);

    private final KeycloakSessionFactory sessionFactory;
    private final long retentionMillis;

    public EventHookRetentionTask(KeycloakSessionFactory sessionFactory, long retentionMillis) {
        this.sessionFactory = sessionFactory;
        this.retentionMillis = retentionMillis;
    }

    @Override
    public void run(KeycloakSession session) {
        long olderThan = currentTimeMillis() - retentionMillis;
        runJobInTransaction(sessionFactory, targetSession ->
                targetSession.getProvider(EventHookStoreProvider.class).clearExpiredMessagesAndLogs(olderThan));
    }
}