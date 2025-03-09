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
import org.infinispan.notifications.cachemanagerlistener.CacheManagerNotifier;
import org.infinispan.notifications.cachemanagerlistener.annotation.Merged;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.concurrent.BlockingManager;
import org.jboss.logging.Logger;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.configuration.ServerConfigStorageProvider;

import static org.keycloak.infinispan.module.certificates.JGroupsCertificate.toJson;

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
    public static final String CERTIFICATE_ID = "crt_jgroups";
    private static final String JGROUPS_SUBJECT = "jgroups";
    private static final Duration RETRY_WAIT_TIME = Duration.ofMinutes(1);
    private static final Duration BOOT_PERIOD = Duration.ofMillis(200);

    private final KeycloakSessionFactory sessionFactory;
    private final JGroupsCertificateHolder certificateHolder;
    private volatile long rotationSeconds;
    private final AutoCloseableLock lock;
    private ScheduledFuture<?> scheduledFuture;
    private ScheduledFuture<?> bootFuture;

    @Inject
    EmbeddedCacheManager cacheManager;

    @Inject
    CacheManagerNotifier notifier;

    @ComponentName(KnownComponentNames.EXPIRATION_SCHEDULED_EXECUTOR)
    @Inject
    ScheduledExecutorService scheduledExecutorService;

    @Inject
    BlockingManager blockingManager;

    public CertificateReloadManager(KeycloakSessionFactory sessionFactory, JGroupsCertificateHolder certificateHolder, int rotationDays) {
        this.sessionFactory = sessionFactory;
        this.certificateHolder = certificateHolder;
        this.rotationSeconds = TimeUnit.DAYS.toSeconds(rotationDays);
        lock = new AutoCloseableLock(new ReentrantLock());
    }

    @Override
    @Start
    public void start() {
        logger.info("Starting JGroups certificate reload manager");
        notifier.addListener(this);
        scheduleNextRotation();
        certificateHolder.setExceptionHandler(this::onInvalidCertificate);

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
            KeycloakModelUtils.runJobInTransaction(sessionFactory, this::replaceCertificateInTransaction);
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
            var maybeCrt = KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, CertificateReloadManager::loadCertificateInTransaction);
            if (maybeCrt.isEmpty()) {
                return;
            }
            var crt = JGroupsCertificate.fromJson(maybeCrt.get());
            certificateHolder.useCertificate(crt);
        } catch (GeneralSecurityException | IOException e) {
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
    public JGroupsCertificate currentCertificate() {
        return certificateHolder.getCertificateInUse();
    }

    // testing purpose
    public void setRotationSeconds(long seconds) {
        this.rotationSeconds = seconds;
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
        logger.info("[Boot] reloading certificate.");
        lock.lock();
        try (lock) {
            var maybeCrt = KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, CertificateReloadManager::loadCertificateInTransaction);
            if (maybeCrt.isEmpty()) {
                return;
            }
            var crt = JGroupsCertificate.fromJson(maybeCrt.get());
            certificateHolder.useCertificate(crt);
        } catch (GeneralSecurityException | IOException e) {
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
            var crt = certificateHolder.getCertificateInUse();
            var delay = delayUntilNextRotation(crt.getCertificate().getNotBefore().toInstant(), crt.getCertificate().getNotAfter().toInstant());
            logger.debugf("Next rotation in %s", delay);
            if (delay.isZero()) {
                blockingManager.runBlocking(this::rotateCertificate, "rotate");
                return;
            }
            scheduledFuture = scheduledExecutorService.schedule(() -> blockingManager.runBlocking(this::rotateCertificate, "rotate"), delay.toSeconds(), TimeUnit.SECONDS);
        }
    }

    private void replaceCertificateInTransaction(KeycloakSession session) {
        var storage = session.getProvider(ServerConfigStorageProvider.class);
        var holder = certificateHolder.getCertificateInUse();
        storage.replace(CERTIFICATE_ID, holder::isSameAlias, () -> generateSelfSignedCertificate(rotationSeconds * 2L));
    }

    private static Optional<String> loadCertificateInTransaction(KeycloakSession session) {
        return session.getProvider(ServerConfigStorageProvider.class).find(CERTIFICATE_ID);
    }

    private Duration delayUntilNextRotation(Instant certificateStartInstant, Instant certificateEndInstant) {
        var rotationInstant = certificateStartInstant.plus(Duration.ofSeconds(rotationSeconds));

        // Avoid the current certificate to expire if the old duration was shorter than the new duration
        var rotationInstantOldCertificate = certificateStartInstant.plus(Duration.between(certificateStartInstant, certificateEndInstant).dividedBy(2));
        if (rotationInstantOldCertificate.isBefore(rotationInstant)) {
            rotationInstant = rotationInstantOldCertificate;
        }

        var secondsLeft = Instant.ofEpochSecond(Time.currentTime()).until(rotationInstant, ChronoUnit.SECONDS);
        return secondsLeft > 0 ? Duration.ofSeconds(secondsLeft) : Duration.ZERO;
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

    public static String generateSelfSignedCertificate(long validForSeconds) {
        var endDate = Date.from(Instant.now().plus(validForSeconds, ChronoUnit.SECONDS));
        var keyPair = KeyUtils.generateRsaKeyPair(2048);
        var certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, JGROUPS_SUBJECT, BigInteger.valueOf(System.currentTimeMillis()), endDate);

        logger.debugf("Created JGroups certificate. Valid until %s", certificate.getNotAfter());

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
