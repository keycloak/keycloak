/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jgroups.certificates;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.spi.infinispan.JGroupsCertificateProvider;

import org.infinispan.commons.api.Lifecycle;
import org.infinispan.factories.KnownComponentNames;
import org.infinispan.factories.annotations.ComponentName;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.factories.annotations.Stop;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.CacheManagerNotifier;
import org.infinispan.notifications.cachemanagerlistener.annotation.Merged;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.concurrent.BlockingManager;
import org.jboss.logging.Logger;

/**
 * Class to handle JGroups certificate reloading for encryption (mTLS).
 * <p>
 * This class is attached to Infinispan lifecycle, and it starts/stops together with the {@link EmbeddedCacheManager}.
 * <p>
 * It provides two public methods, {@link #rotateCertificate()} to force a certificate rotation without waiting for the
 * configured period, and {@link #reloadCertificate()} to force a certificate reloading from storage and schedule the
 * next rotation.
 * <p>
 * When the timer expires, only the cluster coordinator generates a new certificate. It notifies the other cluster
 * members that a new certificate is available in storage. Both the key and trust stores keep a hold of the old and the
 * new certificates.
 * <p>
 * Last, but not least, it listens to topology changes and, if the coordinator crashes, the new re-elected coordinator
 * will continue to perform its duties to rotate the certificate.
 */
@Scope(Scopes.GLOBAL)
@Listener
public class CertificateReloadManager implements Lifecycle {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
    private static final Duration RETRY_WAIT_TIME = Duration.ofMinutes(1);
    private static final Duration BOOT_PERIOD = Duration.ofMillis(500);

    private final KeycloakSessionFactory sessionFactory;
    private final AutoCloseableLock lock;
    private ScheduledFuture<?> scheduledFuture;
    private ScheduledFuture<?> bootFuture;

    @Inject EmbeddedCacheManager cacheManager;
    @Inject CacheManagerNotifier notifier;
    @Inject BlockingManager blockingManager;
    @ComponentName(KnownComponentNames.EXPIRATION_SCHEDULED_EXECUTOR)
    @Inject ScheduledExecutorService scheduledExecutorService;

    public CertificateReloadManager(KeycloakSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        lock = new AutoCloseableLock(new ReentrantLock());
    }

    @Override
    @Start
    public void start() {
        logger.info("Starting JGroups certificate reload manager");
        notifier.addListener(this);
        scheduleNextRotation();

        lock.lock();
        try(lock) {
            // It is invoked before JGroups starts; it schedules a fast pace reload of the certificate.
            // It is canceled when it gets a view from JGroups.
            // This is here to prevent the case when a node joins during a rotation process.
            bootFuture = scheduledExecutorService.scheduleAtFixedRate(() -> blockingManager.runBlocking(this::bootReload, "boot-reload"), BOOT_PERIOD.toMillis(), BOOT_PERIOD.toMillis(), TimeUnit.MILLISECONDS);
        }

    }

    @Override
    @Stop
    public void stop() {
        logger.info("Stopping JGroups certificate reload manager");
        notifier.removeListener(this);
        lock.lock();
        try (lock) {
            if (scheduledFuture == null) {
                return;
            }
            scheduledFuture.cancel(true);
        }
    }

    /**
     * Creates and reload a new certificate.
     */
    public void rotateCertificate() {
        logger.info("Rotating JGroups certificate");
        lock.lock();
        try (lock) {
            KeycloakModelUtils.runJobInTransaction(sessionFactory, CertificateReloadManager::replaceCertificateInTransaction);
            sendReloadNotification();
        } catch (RuntimeException e) {
            logger.warn("Failed to rotate JGroups certificate", e);
            retry(this::rotateCertificate, "retry-rotate");
        }
    }

    /**
     * Reloads the certificate from storage.
     */
    public void reloadCertificate() {
        logger.info("Reloading JGroups Certificate");
        lock.lock();
        try (lock) {
            if (bootFuture != null) {
                bootFuture.cancel(true);
                bootFuture = null;
            }
            KeycloakModelUtils.runJobInTransaction(sessionFactory, CertificateReloadManager::loadCertificateInTransaction);
        } catch (RuntimeException e) {
            logger.warn("Failed to reload JGroups certificate", e);
            retry(this::reloadCertificate, "retry-reload");
        } finally {
            scheduleNextRotation();
        }
    }

    @ViewChanged
    @Merged
    public void onViewChanged(ViewChangedEvent event) {
        logger.debug("On view changed");
        // probably a waste to reload, but if we have a partition, we reload the most recent certificate stored.
        reloadCertificate();
    }

    // testing purpose
    public boolean isCoordinator() {
        return cacheManager.isCoordinator();
    }

    // testing purpose
    public boolean hasRotationTask() {
        lock.lock();
        try (lock) {
            return scheduledFuture != null;
        }
    }

    private void bootReload() {
        logger.debug("[Boot] reloading certificate.");
        lock.lock();
        try (lock) {
            KeycloakModelUtils.runJobInTransaction(sessionFactory, CertificateReloadManager::loadCertificateInTransaction);
        } catch (RuntimeException e) {
            logger.warn("Exception on boot reload cycle. Ignoring it.", e);
        }
    }

    private void onInvalidCertificate() {
        logger.info("On certificate exception");
        blockingManager.runBlocking(this::reloadCertificate, "invalid-certificate");
    }

    private void onCertificateReloadResponse(Address address, Void unused, Throwable throwable) {
        if (throwable != null) {
            logger.warnf(throwable, "Node %s failed to handle JGroups certificate reload notification.", address);
            retry(() -> sendReloadNotification(address), "retry-notification");
        }
    }

    private void scheduleNextRotation() {
        lock.lock();
        try (lock) {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
            if (!isCoordinator()) {
                return;
            }

            var delay = KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, CertificateReloadManager::nextRotationDelay);
            logger.debugf("Next rotation in %s", delay);
            if (delay.isZero()) {
                blockingManager.runBlocking(this::rotateCertificate, "rotate");
                return;
            }
            scheduledFuture = scheduledExecutorService.schedule(() -> blockingManager.runBlocking(this::rotateCertificate, "rotate"), delay.toSeconds(), TimeUnit.SECONDS);
        }
    }

    private static void replaceCertificateInTransaction(KeycloakSession session) {
        session.getProvider(JGroupsCertificateProvider.class).rotateCertificate();
    }

    private static void loadCertificateInTransaction(KeycloakSession session) {
        session.getProvider(JGroupsCertificateProvider.class).reloadCertificate();
    }

    private static Duration nextRotationDelay(KeycloakSession session) {
        return session.getProvider(JGroupsCertificateProvider.class).nextRotation();
    }

    private void sendReloadNotification() {
        cacheManager.executor()
                .allNodeSubmission()
                .submitConsumer(ReloadCertificateFunction.getInstance(), this::onCertificateReloadResponse);
    }

    private void sendReloadNotification(Address destination) {
        cacheManager.executor()
                .filterTargets(destination::equals)
                .submitConsumer(ReloadCertificateFunction.getInstance(), this::onCertificateReloadResponse);
    }

    private void retry(Runnable runnable, String traceId) {
        scheduledExecutorService.schedule(() -> blockingManager.runBlocking(runnable, traceId), RETRY_WAIT_TIME.toSeconds(), TimeUnit.SECONDS);
    }

    private record AutoCloseableLock(ReentrantLock innerLock) implements AutoCloseable {

        public void lock() {
            innerLock.lock();
        }

        @Override
        public void close() {
            innerLock.unlock();
        }
    }

}
