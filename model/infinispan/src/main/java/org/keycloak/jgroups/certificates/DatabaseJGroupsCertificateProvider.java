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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.keycloak.common.util.Retry;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.spi.infinispan.JGroupsCertificateProvider;
import org.keycloak.storage.configuration.ServerConfigStorageProvider;

import org.jboss.logging.Logger;

import static org.keycloak.jgroups.certificates.JGroupsCertificate.fromJson;
import static org.keycloak.jgroups.certificates.JGroupsCertificate.toJson;

/**
 * A {@link JGroupsCertificateProvider} implementation that stores the certificates in the database.
 * <p>
 * The generated certificate is self-signed, and the database is used to share the certificate amongst the Keycloak
 * instances in the cluster. This implementation supports rotation and reloading of the certificate. The rotation can
 * happen at any time, or by a periodic task, or by sysadmin request.
 */
public class DatabaseJGroupsCertificateProvider implements JGroupsCertificateProvider {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
    public static final String CERTIFICATE_ID = "crt_jgroups";
    private static final int STARTUP_RETRIES = 5;
    private static final int STARTUP_RETRY_SLEEP_MILLIS = 500;

    private final KeycloakSessionFactory sessionFactory;
    private final ReloadingX509ExtendedKeyManager keyManager;
    private final ReloadingX509ExtendedTrustManager trustManager;
    private final Lock lock = new ReentrantLock();
    // not final to be updated for testing
    private volatile Duration rotationPeriod;
    private volatile JGroupsCertificate currentCertificate;

    private DatabaseJGroupsCertificateProvider(KeycloakSessionFactory sessionFactory, Duration rotationPeriod) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory);
        this.rotationPeriod = Objects.requireNonNull(rotationPeriod);
        keyManager = new ReloadingX509ExtendedKeyManager();
        trustManager = new ReloadingX509ExtendedTrustManager();
        currentCertificate = new JGroupsCertificate();
    }

    public static DatabaseJGroupsCertificateProvider create(KeycloakSessionFactory factory, Duration rotationPeriod) {
        var provider = new DatabaseJGroupsCertificateProvider(factory, rotationPeriod);
        provider.init();
        return provider;
    }

    private void init() {
        logger.debug("Initializing JGroups mTLS certificate.");
        var cert = Retry.call(ignored -> KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, this::loadOrCreateCertificate), STARTUP_RETRIES, STARTUP_RETRY_SLEEP_MILLIS);
        useCertificate(cert);
        trustManager.setExceptionHandler(this::onTrustManagerException);
    }

    @Override
    public void rotateCertificate() {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, this::replaceCertificateFromDatabase);
    }

    @Override
    public void reloadCertificate() {
        doReload();
    }

    @Override
    public Duration nextRotation() {
        var cert = currentCertificate;
        return delayUntilNextRotation(Instant.ofEpochMilli(cert.getGeneratedMillis()), cert.getCertificate().getNotAfter().toInstant());
    }

    @Override
    public boolean supportRotateAndReload() {
        return true;
    }

    @Override
    public KeyManager keyManager() {
        return keyManager;
    }

    @Override
    public TrustManager trustManager() {
        return trustManager;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void setRotationPeriod(Duration rotationPeriod) {
        this.rotationPeriod = Objects.requireNonNull(rotationPeriod);
    }

    public Duration getRotationPeriod() {
        return rotationPeriod;
    }

    public JGroupsCertificate getCurrentCertificate() {
        return currentCertificate;
    }

    private void onTrustManagerException() {
        doReload();
    }

    private void doReload() {
        KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, DatabaseJGroupsCertificateProvider::loadCertificateFromDatabase)
                .map(JGroupsCertificate::fromJson)
                .ifPresent(this::useCertificate);
    }

    private Duration delayUntilNextRotation(Instant certificateStartInstant, Instant certificateEndInstant) {
        // Avoid the current certificate to expire if the old duration was shorter than the new duration.
        // Compute the rotation instant based on the certificate start and end instants.
        var rotationInstant = certificateStartInstant.plus(Duration.between(certificateStartInstant, certificateEndInstant).dividedBy(2));

        // Compute rotation instant based on the configured rotationPeriod.
        var configuredRotation = certificateStartInstant.plus(rotationPeriod);

        // Pick the smaller one.
        // Most of the time they are the same, but we need to cover the case where the user modifies the rotation period to a short interval.
        var secondsLeft = configuredRotation.isBefore(rotationInstant) ?
                Instant.ofEpochSecond(Time.currentTime()).until(configuredRotation, ChronoUnit.SECONDS) :
                Instant.ofEpochSecond(Time.currentTime()).until(rotationInstant, ChronoUnit.SECONDS);
        return secondsLeft > 0 ? Duration.ofSeconds(secondsLeft) : Duration.ZERO;
    }

    private static Optional<String> loadCertificateFromDatabase(KeycloakSession session) {
        return session.getProvider(ServerConfigStorageProvider.class).find(CERTIFICATE_ID);
    }

    private void replaceCertificateFromDatabase(KeycloakSession session) {
        var storage = session.getProvider(ServerConfigStorageProvider.class);
        storage.replace(CERTIFICATE_ID, currentCertificate::isSameAlias, this::generateSelfSignedCertificate);
    }

    private void useCertificate(JGroupsCertificate certificate) {
        lock.lock();
        try {
            if (Objects.equals(currentCertificate.getAlias(), certificate.getAlias())) {
                return;
            }
            var km = Utils.createKeyManager(certificate);
            var tm = Utils.createTrustManager(certificate);
            currentCertificate = certificate;
            keyManager.reload(km);
            trustManager.reload(tm);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private JGroupsCertificate loadOrCreateCertificate(KeycloakSession session) {
        var storage = session.getProvider(ServerConfigStorageProvider.class);
        return fromJson(storage.loadOrCreate(CERTIFICATE_ID, this::generateSelfSignedCertificate));
    }

    private String generateSelfSignedCertificate() {
        return toJson(Utils.generateSelfSignedCertificate(rotationPeriod.multipliedBy(2L)));
    }
}
