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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.keycloak.common.util.Time;
import org.keycloak.expiration.jpa.ExpirationAction;
import org.keycloak.expiration.jpa.ExpirationListener;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.configuration.ServerConfigStorageProvider;

/**
 * An {@link org.keycloak.expiration.jpa.ExpirationTask} that runs cleanup per realm.
 * <p>
 * On each run, this task fetches the list of realms, removes orphan coordination keys (from deleted realms), and then
 * iterates over each realm. Per-realm coordination via {@link ServerConfigStorageProvider} ensures that a cleanup is
 * skipped if another node ran one recently for the same realm.
 */
public class RealmAwareExpirationTask extends BaseExpirationTask {

    private static final String ROW_ID_PREFIX = "exp-realm-";

    public RealmAwareExpirationTask(KeycloakSessionFactory factory, Executor executor, ExpirationAction action, ExpirationListener listener, String entityId, int transactionTimeoutSeconds, int intervalSeconds, int maxRemoval) {
        super(factory, executor, action, listener, entityId, transactionTimeoutSeconds, intervalSeconds, maxRemoval);
    }

    @Override
    void doWork() {
        var info = fetchRealmInformation();
        cleanupOrphans(info);
        for (var realmId : info.realmIds) {
            var currentTime = Time.currentTime();
            if (!realmNeedsCleanup(realmId, currentTime)) {
                continue;
            }
            var removed = new AtomicInteger(0);
            var success = false;
            var failed = false;
            var hasMore = true;
            var start = System.nanoTime();
            try {
                do {
                    hasMore = KeycloakModelUtils.runJobInTransactionWithResult(factory, session -> action.removeExpired(session, realmId, currentTime, maxRemoval, removed::addAndGet));
                    success = true;
                } while (hasMore);
            } catch (Exception e) {
                failed = true;
                logger.warnf(e, "Exception during cleanup of expired '%s' for realm '%s'", entityId, realmId);
            } finally {
                listener.onTaskRun(realmId, computeOutcome(success, failed), removed.get(), Duration.ofNanos(System.nanoTime() - start));
            }
        }
    }

    private RealmsInfo fetchRealmInformation() {
        try {
            return KeycloakModelUtils.runJobInTransactionWithResult(factory, session -> {
                var expectedKeys = new HashSet<>();
                var realms = session.realms().getRealmsStream()
                        .map(RealmModel::getId)
                        .peek(realmId -> expectedKeys.add(rowIdForRealm(realmId)))
                        .collect(Collectors.toCollection(TreeSet::new));
                var orphanKeys = session.getProvider(ServerConfigStorageProvider.class).keys()
                        .filter(key -> key.startsWith(ROW_ID_PREFIX + entityId))
                        .filter(Predicate.not(expectedKeys::contains))
                        .toList();
                return new RealmsInfo(realms, orphanKeys);
            });
        } catch (Exception e) {
            logger.debugf(e, "Failed to fetch realm information. Skipping cleanup of expired '%s'", entityId);
            return new RealmsInfo(List.of(), List.of());
        }
    }

    private void cleanupOrphans(RealmsInfo realmsInfo) {
        if (realmsInfo.orphanKeys().isEmpty()) {
            return;
        }
        try {
            KeycloakModelUtils.runJobInTransaction(factory, session -> {
                var config = session.getProvider(ServerConfigStorageProvider.class);
                realmsInfo.orphanKeys().forEach(config::remove);
            });
        } catch (Exception e) {
            logger.debugf(e, "Failed to remove orphan keys for '%s'. Cleanup of expired entries continues", entityId);
        }
    }

    private boolean realmNeedsCleanup(String realmId, int currentTime) {
        try {
            return KeycloakModelUtils.runJobInTransactionWithResult(factory, session -> {
                var config = session.getProvider(ServerConfigStorageProvider.class);
                var key = rowIdForRealm(realmId);
                var lastCheck = config.loadOrCreate(key, "0");
                if (Long.parseLong(lastCheck) + intervalSeconds > currentTime) {
                    return false;
                }
                return config.replace(key, lastCheck, Long.toString(currentTime));
            });
        } catch (Exception e) {
            logger.debugf(e, "Unable to update realm last cleanup check. Skipping removal of expired '%s' for realm '%s'", entityId, realmId);
            return false;
        }
    }

    private String rowIdForRealm(String realmId) {
        return ROW_ID_PREFIX + entityId + "-" + realmId;
    }

    private record RealmsInfo(Collection<String> realmIds, Collection<String> orphanKeys) {
    }

}
