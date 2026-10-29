package org.keycloak.quarkus.runtime.integration.tls;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.truststore.TruststoreProviderReloadEvent;
import org.keycloak.truststore.TruststoreReloadEvent;

import io.quarkus.tls.TrustStoreAndTrustOptions;
import io.vertx.core.Vertx;
import io.vertx.core.net.TrustOptions;
import org.jboss.logging.Logger;

import static org.keycloak.truststore.TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY;
import static org.keycloak.truststore.TruststoreBuilder.setAndGetSystemTruststore;

public final class SystemTruststoreReload {

    private record SystemTruststoreSource(String[] paths, boolean includeDefault, String dataDir, KeystoreUtil.TruststoreFormat preferredType) {
    }

    public record SystemTruststoreSourceAndKeystore(String[] paths, boolean includeDefault, String dataDir,
                                                    KeystoreUtil.TruststoreFormat preferredType, KeyStore systemTruststore) {
    }

    record SystemTrustOptions(TrustManagerFactory trustManagerFactory) implements TrustOptions {

        @Override
        public TrustOptions copy() {
            return this;
        }

        @Override
        public TrustManagerFactory getTrustManagerFactory(Vertx vertx) {
            return trustManagerFactory;
        }

        @Override
        public Function<String, TrustManager[]> trustManagerMapper(Vertx vertx) {
            return serverName -> trustManagerFactory.getTrustManagers();
        }
    }

    public static final String TLS_BUCKET_NAME = "keycloak-system-truststore";

    public static final String TRUST_STORE_TYPE = "keycloak-system-truststore";

    private static final Logger LOGGER = Logger.getLogger(SystemTruststoreReload.class);

    private final AtomicLong RELOAD_COUNT = new AtomicLong();

    private final SystemTruststoreSource systemTruststoreSource;

    private volatile TrustStoreAndTrustOptions systemTruststore;

    private volatile String lastSourceFingerprint;

    public SystemTruststoreReload() {
        this.systemTruststoreSource = null;
    }

    public SystemTruststoreReload(SystemTruststoreSourceAndKeystore r) {
        this.systemTruststoreSource = new SystemTruststoreSource(r.paths, r.includeDefault, r.dataDir, r.preferredType);
        this.systemTruststore = toTrustStoreAndTrustOptions(r.systemTruststore);
        this.lastSourceFingerprint = computeSourceFingerprint(this.systemTruststoreSource);
    }

    // for testing purposes only, not intended for public use
    public long reloadCount() {
        return RELOAD_COUNT.get();
    }

    TrustStoreAndTrustOptions getSystemTruststore() {
        return systemTruststore;
    }

    private synchronized boolean reloadIfChanged() {
        if (systemTruststoreSource == null) {
            return false;
        }
        try {
            if (!reloadSystemTruststoreIfChanged()) {
                LOGGER.debug("System truststore sources unchanged; nothing to reload");
                return false;
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to reload the system truststore; keeping the previously loaded truststore", e);
            return false;
        }
        long count = RELOAD_COUNT.updateAndGet(current -> {
            if (current == Long.MAX_VALUE) {
                LOGGER.debug("Resetting system truststore reload count to 1");
                return 1L;
            }
            return current + 1;
        });
        LOGGER.debugf("System truststore re-merged (reloadCount=%d)", count);
        return true;
    }

    /**
     * @return {@code true} if the system truststore was reloaded and its consumers were notified
     */
    boolean notifyConsumers(KeycloakSessionFactory sessionFactory) {
        if (!reloadIfChanged()) {
            return false;
        }
        long count = RELOAD_COUNT.get();
        LOGGER.debugf("Refreshing system truststore consumers after reload (reloadCount=%d)", count);
        try {
            try (var session = sessionFactory.create()) {
                sessionFactory.publish(TruststoreProviderReloadEvent.get());
                sessionFactory.publish(new TruststoreReloadEvent(session));
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to refresh a system truststore consumer after reload", e);
        }
        return true;
    }

    private boolean reloadSystemTruststoreIfChanged() {
        String current = computeSourceFingerprint(systemTruststoreSource);
        if (current.equals(lastSourceFingerprint)) {
            LOGGER.debugf("System truststore sources unchanged (fingerprint %s); skipping re-merge",
                    fingerprintPrefix(current));
            return false;
        }
        LOGGER.debugf("System truststore sources changed (%s -> %s); re-merging",
                fingerprintPrefix(lastSourceFingerprint), fingerprintPrefix(current));
        // Reuse the fingerprint just computed to detect the change, so a reload does not fingerprint the sources
        // a second time.
        rebuildAndPublish(current);
        return true;
    }

    private void rebuildAndPublish(String sourceFingerprint) {
        systemTruststore = toTrustStoreAndTrustOptions(setAndGetSystemTruststore(systemTruststoreSource.paths,
                systemTruststoreSource.includeDefault, systemTruststoreSource.dataDir, systemTruststoreSource.preferredType));
        lastSourceFingerprint = sourceFingerprint;
    }

    private static TrustStoreAndTrustOptions toTrustStoreAndTrustOptions(KeyStore keyStore) {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            return new TrustStoreAndTrustOptions(keyStore, new SystemTrustOptions(trustManagerFactory));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    // Fingerprint the reload SOURCES (truststore-paths files, files inside directory sources, and the default
    // cacerts) by content, so an unchanged reload interval can be skipped. Never fingerprint the generated
    // output store: re-saving a PKCS12 produces new bytes each time and would self-trigger.
    private static String computeSourceFingerprint(SystemTruststoreSource source) {
        SortedMap<String, String> entries = new TreeMap<>();
        for (String path : source.paths()) {
            addFingerprintEntries(new File(path), entries);
        }
        if (source.includeDefault()) {
            String defaultTrustStore = System.getProperty(SYSTEM_TRUSTSTORE_KEY + ".orig");
            if (defaultTrustStore != null) {
                addFingerprintEntries(new File(defaultTrustStore), entries);
            }
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(entry.getValue().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String fingerprintPrefix(String fingerprint) {
        if (fingerprint == null) {
            return "none";
        }
        return fingerprint.length() <= 12 ? fingerprint : fingerprint.substring(0, 12);
    }

    private static void addFingerprintEntries(File file, SortedMap<String, String> entries) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    addFingerprintEntries(child, entries);
                }
            }
        } else if (file.isFile()) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(Files.readAllBytes(file.toPath()));
                entries.put(file.getAbsolutePath(), HexFormat.of().formatHex(digest.digest()));
            } catch (IOException | NoSuchAlgorithmException e) {
                // unreadable or vanished mid-scan: omit it, so its absence is reflected in the fingerprint
            }
        }
    }
}
