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

package org.keycloak.authentication.jpa;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.SessionExpiration;
import org.keycloak.storage.configuration.ServerConfigStorageProvider;

import org.jboss.logging.Logger;

/**
 * Periodic task that removes expired authentication sessions, one realm at a time. Uses
 * {@link ServerConfigStorageProvider} as a coordination point: if another node is already cleaning up a realm, this
 * node skips it and moves on to the next realm in the list.
 */
final class ExpirationTask implements Runnable {

    private final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
    private static final String ROW_ID_PREFIX = "exp-auth-session-";

    private final int intervalSeconds;
    private final int transactionTimeoutSeconds;
    private final Executor executor;
    private final KeycloakSessionFactory factory;
    private final Monitoring monitoring;
    private final AtomicBoolean inProgress;

    ExpirationTask(KeycloakSessionFactory factory, Executor executor, Monitoring monitoring, int intervalSeconds,
                   int transactionTimeoutSeconds) {
        this.executor = Objects.requireNonNull(executor);
        this.monitoring = Objects.requireNonNull(monitoring);
        this.factory = Objects.requireNonNull(factory);
        this.intervalSeconds = intervalSeconds;
        this.transactionTimeoutSeconds = Math.min(intervalSeconds, transactionTimeoutSeconds);
        this.inProgress = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        // triggered by the timer background task and it delays other tasks if it takes too long
        executor.execute(this::removeExpired);
    }

    private void removeExpired() {
        if (!inProgress.compareAndSet(false, true)) {
            logger.debug("Skipping authentication session expiration task. Already in progress");
            return;
        }
        var start = System.nanoTime();
        try {
            var realms = fetchRealmInformation();
            cleanupOrphans(realms);
            var currentTime = Time.currentTime();
            realms.realmIds().stream()
                    .filter(this::realmNeedsCleanup)
                    .forEach(realmId -> removeExpiredForRealm(realmId, currentTime));
        } catch (Exception e) {
            // no exception expected here and running in a background thread... that's why it is warning
            logger.warn("Exception during cleanup of expired authentication sessions", e);
        } finally {
            monitoring.onTaskCompleted(Duration.ofNanos(System.nanoTime() - start));
            inProgress.set(false);
        }
    }

    private void removeExpiredForRealm(String realmId, int currentTime) {
        var start = System.nanoTime();
        var removed = new AtomicInteger(0);
        var realmName = new AtomicReference<String>();
        boolean success = false;
        try {
            KeycloakModelUtils.runJobInTransactionWithTimeout(factory, session -> {
                var realm = session.realms().getRealm(realmId);
                if (realm == null) {
                    return;
                }
                realmName.set(realm.getName());
                session.getContext().setRealm(realm);
                var lifespan = SessionExpiration.getAuthSessionLifespan(realm);
                var olderTimestamp = currentTime - lifespan;
                var updated = session.getProvider(JpaConnectionProvider.class)
                        .getEntityManager()
                        .createNamedQuery("deleteExpiredRootAuthSessionByRealm")
                        .setParameter("realmId", realmId)
                        .setParameter("timestamp", olderTimestamp)
                        .executeUpdate();
                removed.set(updated);
                logger.debugf("Removed %s expired authentication sessions in realm '%s', with timestamp older than %d", updated, realmId, (Object) olderTimestamp);
            }, transactionTimeoutSeconds);
            success = true;
        } catch (Exception e) {
            logger.debugf(e, "An exception occurred while removing expired authentication sessions in realm '%s'", realmId);
        } finally {
            if (realmName.get() != null) {
                monitoring.onTaskCompletedForRealm(realmName.get(), success, removed.get(), Duration.ofNanos(System.nanoTime() - start));
            }
        }

    }

    private RealmsInfo fetchRealmInformation() {
        try {
            return KeycloakModelUtils.runJobInTransactionWithResult(factory, session -> {
                // cleanup orphan keys, in case a realm is removed
                // this contains the expected keys in the server config table
                var expectedKeys = new HashSet<>();
                var realms = session.realms().getRealmsStream()
                        .map(RealmModel::getId)
                        .peek(realmId -> expectedKeys.add(rowIdForRealm(realmId)))
                        .collect(Collectors.toCollection(TreeSet::new));
                var orphanKeys = session.getProvider(ServerConfigStorageProvider.class).keys()
                        .filter(key -> key.startsWith(ROW_ID_PREFIX))
                        .filter(Predicate.not(expectedKeys::contains))
                        .toList();
                return new RealmsInfo(realms, orphanKeys);
            });
        } catch (Exception e) {
            logger.debug("An exception occurred fetching information about the existing realms. It cancels the cleanup of expired authentication sessions", e);
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
            logger.debug("An exception occurred removing orphan keys from server configuration table. It does not abort the cleanup of expired authentication sessions", e);
        }
    }

    private boolean realmNeedsCleanup(String realmId) {
        try {
            return KeycloakModelUtils.runJobInTransactionWithResult(factory, session -> {
                var currentTime = Time.currentTime();
                var config = session.getProvider(ServerConfigStorageProvider.class);
                var key = rowIdForRealm(realmId);
                var lastCheck = config.loadOrCreate(key, "0");
                if (Long.parseLong(lastCheck) + intervalSeconds > currentTime) {
                    // some task recently run on this realm, skip it
                    return false;
                }
                return config.replace(key, lastCheck, Long.toString(currentTime));
            });
        } catch (Exception e) {
            logger.debugf(e, "Unable to update realm last cleanup check. Skipping removal of expired authentication sessions for realm '%s'", realmId);
            return false;
        }
    }

    private static String rowIdForRealm(String realmId) {
        return ROW_ID_PREFIX + realmId;
    }

    private record RealmsInfo(Collection<String> realmIds, Collection<String> orphanKeys) {
    }

    public interface Monitoring {
        void onTaskCompleted(Duration duration);

        void onTaskCompletedForRealm(String realmName, boolean success, int removedCount, Duration duration);
    }

}
