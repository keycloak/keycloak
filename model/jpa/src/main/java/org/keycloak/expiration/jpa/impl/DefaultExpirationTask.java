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

package org.keycloak.expiration.jpa.impl;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.common.util.Time;
import org.keycloak.expiration.jpa.ExpirationAction;
import org.keycloak.expiration.jpa.ExpirationListener;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.configuration.ServerConfigStorageProvider;

/**
 * An {@link org.keycloak.expiration.jpa.ExpirationTask} that runs a single global cleanup (no per-realm iteration).
 * <p>
 * Uses {@link ServerConfigStorageProvider} to coordinate across cluster nodes: a cleanup is skipped if another node
 * ran one recently (within the configured interval).
 */
public class DefaultExpirationTask extends BaseExpirationTask {

    private static final String ROW_ID_PREFIX = "exp-";

    public DefaultExpirationTask(KeycloakSessionFactory factory, Executor executor, ExpirationAction action, ExpirationListener listener, String entityId, int transactionTimeoutSeconds, int intervalSeconds, int maxRemoval) {
        super(factory, executor, action, listener, entityId, transactionTimeoutSeconds, intervalSeconds, maxRemoval);
    }

    @Override
    void doWork() {
        if (!needsCleanup()) {
            return;
        }

        var removed = new AtomicInteger(0);
        var success = false;
        var failed = false;
        var hasMore = true;
        var start = System.nanoTime();
        var currentTime = Time.currentTime();
        try {
            do {
                hasMore = KeycloakModelUtils.runJobInTransactionWithResult(factory, session -> action.removeExpired(session, null, currentTime, maxRemoval, removed::addAndGet));
                success = true;
            } while (hasMore);
        } catch (RuntimeException e) {
            failed = true;
            throw e;
        } finally {
            listener.onTaskRun(null, computeOutcome(success, failed), removed.get(), Duration.ofNanos(System.nanoTime() - start));
        }
    }

    private boolean needsCleanup() {
        try {
            return KeycloakModelUtils.runJobInTransactionWithResult(factory, session -> {
                var currentTime = Time.currentTime();
                var config = session.getProvider(ServerConfigStorageProvider.class);
                var key = ROW_ID_PREFIX + entityId;
                var lastCheck = config.loadOrCreate(key, "0");
                if (Long.parseLong(lastCheck) + intervalSeconds > currentTime) {
                    return false;
                }
                return config.replace(key, lastCheck, Long.toString(currentTime));
            });
        } catch (Exception e) {
            logger.debugf(e, "Unable to update last cleanup check. Skipping removal of expired '%s'", entityId);
            return false;
        }
    }
}
