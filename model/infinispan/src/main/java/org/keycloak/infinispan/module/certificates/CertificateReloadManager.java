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

package org.keycloak.infinispan.module.certificates;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

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
import org.infinispan.notifications.cachemanagerlistener.annotation.Merged;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.concurrent.BlockingManager;
import org.jboss.logging.Logger;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.configuration.ServerConfigStorageProvider;

import static org.keycloak.infinispan.module.certificates.JGroupsCertificate.toJson;

@Scope(Scopes.GLOBAL)
@Listener
public class CertificateReloadManager implements Lifecycle {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
    public static final String CERTIFICATE_ID = "crt_jgroups";
    private static final String JGROUPS_SUBJECT = "jgroups";

    private final KeycloakSessionFactory sessionFactory;
    private final JGroupsCertificateHolder certificateHolder;
    private final int rotationDays;
    private final AutoCloseableLock lock;
    private ScheduledFuture<?> scheduledFuture;

    @Inject
    EmbeddedCacheManager cacheManager;

    @ComponentName(KnownComponentNames.TIMEOUT_SCHEDULE_EXECUTOR)
    @Inject
    ScheduledExecutorService scheduledExecutorService;

    @Inject
    BlockingManager blockingManager;

    public CertificateReloadManager(KeycloakSessionFactory sessionFactory, JGroupsCertificateHolder certificateHolder, int rotationDays) {
        this.sessionFactory = sessionFactory;
        this.certificateHolder = certificateHolder;
        this.rotationDays = rotationDays;
        lock = new AutoCloseableLock(new ReentrantLock());
    }

    @Override
    @Start
    public void start() {
        logger.info("Starting certificate reloading");
        scheduleNextRotation();
    }

    @Override
    @Stop
    public void stop() {
        logger.info("Stopping certificate reloading");
        lock.lock();
        try (lock) {
            if (scheduledFuture == null) {
                return;
            }
            scheduledFuture.cancel(true);
        }
    }

    public void rotateCertificate() {
        logger.info("Rotate Certificate");
        lock.lock();
        try (lock) {
            KeycloakModelUtils.runJobInTransaction(sessionFactory, this::replaceCertificateInTransaction);
            //async notify all nodes
            cacheManager.executor()
                    .allNodeSubmission()
                    .submitConsumer(ReloadCertificateFunction.getInstance(), this::onCertificateReloadResponse);
        }
    }

    public void reloadCertificate() {
        logger.info("Reload Certificate");
        lock.lock();
        try (lock) {
            var maybeCrt = KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, this::loadCertificateInTransaction);
            if (maybeCrt.isEmpty()) {
                return;
            }
            var crt = JGroupsCertificate.fromJson(maybeCrt.get());
            certificateHolder.useCertificate(crt);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            scheduleNextRotation();
        }
    }

    @ViewChanged
    @Merged
    public void onViewChange(ViewChangedEvent event) {
        scheduleNextRotation();
    }

    private void onCertificateReloadResponse(Address address, Void unused, Throwable throwable) {
        // TODO better handling. retry?
        if (throwable != null) {
            logger.warnf(throwable, "Node %s failed to reload JGroups MTLS certificate.", address);
        }
    }

    private void scheduleNextRotation() {
        lock.lock();
        try (lock) {
            if (!cacheManager.isCoordinator()) {
                return;
            }
            var crt = certificateHolder.getCertificateInUse();
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
            var delay = delayUntilNextRotation(crt.getCertificate().getNotBefore().toInstant());
            logger.infof("Next rotation in %s", delay);
            if (delay.isZero()) {
                blockingManager.runBlocking(this::rotateCertificate, "jgrp-crt-rotate");
                return;
            }
            scheduledFuture = scheduledExecutorService.schedule(() -> blockingManager.runBlocking(this::rotateCertificate, "jgrp-crt-rotate"), delay.toSeconds(), TimeUnit.SECONDS);
        }
    }

    private void replaceCertificateInTransaction(KeycloakSession session) {
        var storage = session.getProvider(ServerConfigStorageProvider.class);
        var holder = certificateHolder.getCertificateInUse();
        storage.replace(CERTIFICATE_ID, holder::isSameAlias, () -> generateSelfSignedCertificate(rotationDays * 2));
    }

    private Optional<String> loadCertificateInTransaction(KeycloakSession session) {
        return session.getProvider(ServerConfigStorageProvider.class).find(CERTIFICATE_ID);
    }

    private Duration delayUntilNextRotation(Instant certificateStartInstant) {
        var rotationInstant = certificateStartInstant.plus(Duration.ofDays(rotationDays));
        var hoursLeft = certificateStartInstant.until(rotationInstant, ChronoUnit.HOURS);
        return hoursLeft > 0 ? Duration.ofHours(hoursLeft) : Duration.ZERO;
    }

    public static String generateSelfSignedCertificate(int validForDays) {
        var endDate = Date.from(Instant.now().plus(validForDays, ChronoUnit.DAYS));;
        var keyPair = KeyUtils.generateRsaKeyPair(2048);
        var certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, JGROUPS_SUBJECT, BigInteger.ONE, endDate);
        logger.infof("Created JGroups certificate. Valid until %s", certificate.getNotAfter());
        var entity = new JGroupsCertificate();
        entity.setCertificate(certificate);
        entity.setKeyPair(keyPair);
        entity.setAlias(UUID.randomUUID().toString());
        return toJson(entity);
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
