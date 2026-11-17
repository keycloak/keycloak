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
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
    // Keeps the handshake thread free. A request is dropped when a reload is already running (debounce).
    private final ThreadPoolExecutor reloadExecutor = new ThreadPoolExecutor(0, 1, 10, TimeUnit.SECONDS, new SynchronousQueue<>(), r -> {
        var thread = new Thread(r, "jgroups-certificate-reload");
        thread.setDaemon(true);
        return thread;
    }, new ThreadPoolExecutor.DiscardPolicy());
    // not final to be updated for testing
    private volatile Duration rotationPeriod;
    private final KeySpec keySpec;
    private volatile JGroupsCertificate currentCertificate;

    private DatabaseJGroupsCertificateProvider(KeycloakSessionFactory sessionFactory, Duration rotationPeriod, KeySpec keySpec) {
        this.keySpec = Objects.requireNonNull(keySpec);
        this.sessionFactory = Objects.requireNonNull(sessionFactory);
        this.rotationPeriod = Objects.requireNonNull(rotationPeriod);
        keyManager = new ReloadingX509ExtendedKeyManager();
        trustManager = new ReloadingX509ExtendedTrustManager();
        currentCertificate = new JGroupsCertificate();
    }

    public static DatabaseJGroupsCertificateProvider create(KeycloakSessionFactory factory, Duration rotationPeriod, KeySpec keySpec) {
        var provider = new DatabaseJGroupsCertificateProvider(factory, rotationPeriod, keySpec);
        provider.init();
        return provider;
    }

    private void init() {
        logger.debug("Initializing JGroups mTLS certificate.");
        var cert = Retry.call(ignored -> KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, this::loadOrCreateCertificate), STARTUP_RETRIES, STARTUP_RETRY_SLEEP_MILLIS);
        useCertificate(cert);
        trustManager.setExceptionHandler(this::onTrustManagerException);
        trustManager.setAcceptedIssuersHandler(this::reloadAsync);
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

    // A peer that rejects the certificate of this node aborts the handshake before the trust manager sees its certificate.
    // Each incoming handshake therefore triggers a reload, so a node that still uses an outdated certificate picks up the new one.
    private void reloadAsync() {
        reloadExecutor.execute(() -> {
            try {
                doReload();
            } catch (RuntimeException e) {
                logger.warn("Failed to reload the JGroups certificate", e);
            }
        });
    }

    private void doReload() {
        // Read and apply under one lock, so a delayed reload cannot reinstall a certificate that a newer reload has superseded.
        lock.lock();
        try {
            KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, DatabaseJGroupsCertificateProvider::loadCertificateFromDatabase)
                    .map(JGroupsCertificate::fromJson)
                    .ifPresent(this::useCertificate);
        } finally {
            lock.unlock();
        }
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
            var x509 = certificate.getCertificate();
            logger.infof("JGroups mTLS certificate in use: key=%s, signature=%s, valid until %s, rotation every %s.",
                    describe(x509.getPublicKey()), x509.getSigAlgName(), x509.getNotAfter().toInstant(), rotationPeriod);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    // describes the key of the certificate in use, which differs from the configured key until this node has regenerated or reloaded it
    private static String describe(PublicKey publicKey) {
        if (publicKey instanceof RSAPublicKey rsa) {
            return "RSA-" + rsa.getModulus().bitLength();
        }
        if (publicKey instanceof ECPublicKey ec) {
            return "ECDSA-" + ec.getParams().getOrder().bitLength() + "bit";
        }
        return publicKey.getAlgorithm();
    }

    private JGroupsCertificate loadOrCreateCertificate(KeycloakSession session) {
        var storage = session.getProvider(ServerConfigStorageProvider.class);
        var stored = fromJson(storage.loadOrCreate(CERTIFICATE_ID, this::generateSelfSignedCertificate));
        if (matchesKeySpec(stored)) {
            return stored;
        }
        // Replace only if still mismatching, so concurrently starting nodes with the same configuration regenerate it once.
        // Running nodes reload the new certificate when the trust manager rejects the connection of this node.
        logger.infof("Stored JGroups certificate does not match the configured key (%s). Regenerating it.", keySpec.description());
        storage.replace(CERTIFICATE_ID, json -> !matchesKeySpec(fromJson(json)), this::generateSelfSignedCertificate);
        return fromJson(storage.find(CERTIFICATE_ID).orElseThrow());
    }

    private boolean matchesKeySpec(JGroupsCertificate certificate) {
        return keySpec.matches(certificate.getCertificate().getPublicKey());
    }

    private String generateSelfSignedCertificate() {
        return toJson(Utils.generateSelfSignedCertificate(rotationPeriod.multipliedBy(2L), keySpec));
    }
}
