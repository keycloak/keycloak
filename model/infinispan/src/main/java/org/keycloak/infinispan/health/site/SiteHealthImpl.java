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

package org.keycloak.infinispan.health.site;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.keycloak.infinispan.health.ClusterHealth;
import org.keycloak.models.KeycloakSessionFactory;

import jdk.jfr.EventType;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.jboss.logging.Logger;

/**
 * Cross-site STONITH health check implementation.
 * <p>
 * Uses a database-backed state machine ({@link SiteStorage}) and the Infinispan management API
 * ({@link InfinispanManagement}) to detect site failures and coordinate failover between two sites. The state machine
 * follows the lifecycle {@code HEALTHY → SUSPECTING → UNHEALTHY → RECOVERING → HEALTHY}, with a one-round delay in the
 * {@code SUSPECTING} state to avoid reacting to transient failures.
 */
public class SiteHealthImpl implements ClusterHealth {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
    private static final EventType JFR_HEALTH_CHECK = EventType.getEventType(SiteHealthCheckEvent.class);
    private static final EventType JFR_STATE_CHANGE = EventType.getEventType(SiteStateChangeEvent.class);

    private final InfinispanManagement management;
    private final SiteStorage storage;
    private final Executor executor;
    private final Consumer<Status> statusConsumer;
    private final AtomicBoolean inProgress = new AtomicBoolean(false);
    private volatile boolean healthy = false;
    private volatile SiteState transitionState;

    public SiteHealthImpl(SiteStorage storage, InfinispanManagement management, Executor executor, Consumer<Status> statusConsumer) {
        this.management = Objects.requireNonNull(management);
        this.storage = Objects.requireNonNull(storage);
        this.executor = Objects.requireNonNull(executor);
        this.statusConsumer = Objects.requireNonNullElse(statusConsumer, status -> {
        });
    }

    public static SiteHealthImpl create(KeycloakSessionFactory factory, RemoteCacheManager remoteCacheManager, Executor executor, Consumer<Status> statusConsumer) {
        return new SiteHealthImpl(new JpaSiteStorage(factory), RestInfinispanManagement.fromRemoteCacheManager(remoteCacheManager), executor, statusConsumer);
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public boolean isSiteActive() {
        return healthy;
    }

    @Override
    public void triggerClusterHealthCheck() {
        executor.execute(() -> {
            try {
                checkHealth();
            } catch (ExecutionException | RuntimeException e) {
                healthy = false;
                logger.warn("Failed to check cross-site state. Marking instance as unhealthy", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public void close() {
        try {
            management.close();
        } catch (Exception e) {
            logger.debug("Exception while closing the Infinispan connection. Ignored.", e);
        }
    }

    private void checkHealth() throws ExecutionException, InterruptedException {
        if (!inProgress.compareAndSet(false, true)) {
            logger.debug("A thread is running. Ignoring health check request");
            return;
        }
        try {
            var sites = management.siteStatus();
            var connection = management.siteConnection();

            if (connection.localSite() == null) {
                logger.debug("Cross-site disabled in Infinispan cluster.");
                healthy = true;
                return;
            }
            if (sites.isEmpty()) {
                logger.debug("No caches are replicated to the remove sites. Is this a mistake?");
                healthy = true;
                return;
            }

            var siteState = storage.get();
            statusConsumer.accept(siteState.status());
            SiteHealthCheckEvent event = null;
            if (JFR_HEALTH_CHECK.isEnabled()) {
                // a recording is progress, let's track it.
                event = new SiteHealthCheckEvent();
                event.begin();
            }
            try {
                computeState(siteState, sites, connection);
            } finally {
                if (event != null) {
                    event.end();
                    if (event.shouldCommit()) {
                        // returns true if the duration is within the configured threshold (if configured)
                        event.set(sites, connection, siteState);
                        event.commit();
                    }
                }
            }
        } finally {
            inProgress.set(false);
        }
    }

    private void computeState(SiteState siteState, Map<String, String> sites, SiteConnection connection) {
        switch (siteState.status()) {
            case HEALTHY -> onHealthy(siteState, sites, connection);
            case RECOVERING -> onRecovering(siteState, sites, connection);
            case UNHEALTHY -> onUnhealthy(siteState, sites, connection);
            case SUSPECTING -> onSuspecting(siteState, sites, connection);
        }
    }

    private void onSuspecting(SiteState siteState, Map<String, String> sites, SiteConnection connection) {
        logger.debugf("Checking 'suspecting' state. State=%s, Connection=%s", siteState, connection);
        healthy = true;
        if (!Objects.equals(siteState.activeSite(), connection.localSite())) {
            tryStealSuspect(siteState, connection);
            return;
        }

        if (!Objects.equals(siteState, transitionState)) {
            logger.debugf("Current state does not match our last state, skipping. Last=%s, Current=%s", transitionState, siteState);
            transitionState = siteState;
            return;
        }

        if (connection.onlineSites().containsAll(sites.keySet())) {
            logger.debugf("All sites are online (in the view). Setting 'healthy' state");
            if (advanceState(siteState, SiteState.healthy())) {
                transitionState = null;
            }
            return;
        }

        logger.warnf("Some site(s) is(are) not reachable. Setting 'unhealthy' state");
        if (advanceState(siteState, SiteState.unhealthy(connection.localSite()))) {
            // db updated - disconnect all sites
            takeOffline(sites.keySet());
            transitionState = null;
        }
    }

    private void onUnhealthy(SiteState siteState, Map<String, String> sites, SiteConnection connection) {
        logger.debugf("Checking 'unhealthy' state. State=%s, Connection=%s", siteState, connection);
        if (!Objects.equals(connection.localSite(), siteState.activeSite())) {
            logger.debug("Not the active site; mark unhealthy");
            healthy = false;
            transitionState = null;
            return;
        }
        logger.debug("Active site; keep healthy");
        healthy = true;
        var allOnline = connection.onlineSites().containsAll(sites.keySet());
        if (allOnline && sites.values().stream().allMatch("online"::equals)) {
            logger.debug("All sites are online, setting 'recovering' state");
            var recovering = SiteState.recovering(connection.localSite());
            if (advanceState(siteState, recovering)) {
                transitionState = recovering;
            }
            return;
        }

        logger.debugf("Some site(s) is(are) not reachable. Keeping 'unhealthy' state");
        if (!allOnline && sites.values().stream().allMatch("offline"::equals)) {
            takeOffline(sites.keySet());
        }
    }

    private void onRecovering(SiteState siteState, Map<String, String> sites, SiteConnection connection) {
        logger.debugf("Checking 'recovering' state. State=%s, Connection=%s", siteState, connection);
        if (!Objects.equals(connection.localSite(), siteState.activeSite())) {
            // only active site can recover
            // we can get stuck here if it fails, and we don't know what does the other site sees
            logger.debug("Not the active site; keep unhealthy");
            healthy = false;
            return;
        }
        logger.debug("Active site; keep healthy");
        healthy = true;
        if (!Objects.equals(siteState, transitionState)) {
            logger.debugf("Current state does not match our last state, skipping. Last=%s, Current=%s", transitionState, siteState);
            transitionState = siteState;
            return;
        }
        if (connection.onlineSites().containsAll(sites.keySet()) && sites.values().stream().allMatch("online"::equals)) {
            logger.debug("All sites are online, setting 'healthy' state");
            if (advanceState(siteState, SiteState.healthy())) {
                transitionState = null;
            }
        } else {
            // connection is broken or cache state is not online (needs admin to change it)
            logger.debugf("Some site(s) is(are) not reachable. Setting 'unhealthy' state");
            if (advanceState(siteState, SiteState.unhealthy(connection.localSite()))) {
                transitionState = null;
            }
        }
    }

    private void onHealthy(SiteState siteState, Map<String, String> sites, SiteConnection connection) {
        logger.debugf("Checking 'healthy' state. State=%s, Connection=%s", siteState, connection);
        healthy = true;
        if (connection.onlineSites().containsAll(sites.keySet())) {
            logger.debug("All sites are online, keeping 'healthy' state");
            return;
        }
        logger.debugf("Some site(s) is(are) not reachable. Setting 'suspecting' state");
        var newState = SiteState.suspecting(connection.localSite());
        if (advanceState(siteState, newState)) {
            transitionState = newState;
        }
    }

    private void tryStealSuspect(SiteState siteState, SiteConnection connection) {
        if (!Objects.equals(siteState, transitionState)) {
            // first round, wait to see if active site makes progress
            transitionState = siteState;
            return;
        }
        // active site didn't progress (may have crashed), steal the suspect state
        logger.debug("Stealing 'suspecting' state from other site. No progress has been made yet.");
        var stolen = SiteState.suspecting(connection.localSite());
        if (advanceState(siteState, stolen)) {
            transitionState = stolen;
        }
    }

    private void takeOffline(Collection<String> sites) {
        // do not throw an exception, it will change the health to false
        try {
            management.disconnect(sites);
        } catch (ExecutionException e) {
            logger.debugf(e, "Failed to take sites offline: %s", sites);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean advanceState(SiteState expectedState, SiteState newState) {
        // do not throw an exception, it will change the health to false
        SiteStateChangeEvent event = null;
        if (JFR_STATE_CHANGE.isEnabled()) {
            // a recording is progress, let's track it.
            event = new SiteStateChangeEvent();
            event.begin();
        }
        var success = false;
        try {
            success = storage.compareAndSet(expectedState, newState);
        } catch (Exception e) {
            logger.debugf(e, "Failed to advance state %s to state %s", expectedState, newState);
        } finally {
            if (event != null) {
                event.end();
                if (event.shouldCommit()) {
                    event.set(expectedState, newState, success);
                    event.commit();
                }
            }
        }
        return success;
    }

}
