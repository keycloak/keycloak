package org.keycloak.tests.truststore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.inject.spi.CDI;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.common.util.PemUtils;
import org.keycloak.config.TruststoreOptions;
import org.keycloak.quarkus.runtime.integration.tls.SystemTruststoreReload;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.truststore.TruststoreBuilder;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = SystemTruststoreChangeDetectionTest.ServerConfig.class)
public class SystemTruststoreChangeDetectionTest {

    private static final Path TMP = Path.of(System.getProperty("java.io.tmpdir"));
    private static final Path CHANGEDET_PEM = TMP.resolve("kc-it-changedet.pem");
    private static final Path CHANGEDET_NOMAC_P12 = TMP.resolve("kc-it-changedet-nomac.p12");
    private static final Path CHANGEDET_EMPTYMAC_P12 = TMP.resolve("kc-it-changedet-emptymac.p12");
    private static final Path CHANGEDET_DIR = TMP.resolve("kc-it-changedet-dir");
    private static final Path CHANGEDET_DIR_BASELINE = CHANGEDET_DIR.resolve("dir-a.pem");
    private static final Path CHANGEDET_DIR_EXTRA = CHANGEDET_DIR.resolve("dir-b.pem");
    private static final Path CHANGEDET_MIXED_PEM = TMP.resolve("kc-it-changedet-mixed.pem");
    private static final Path CHANGEDET_MIXED_P12 = TMP.resolve("kc-it-changedet-mixed.p12");
    private static final Duration RELOAD_PERIOD = Duration.ofSeconds(2);
    // "Skipped" must hold across at least ~3 reload periods.
    private static final Duration IDLE_WINDOW = RELOAD_PERIOD.multipliedBy(3);
    private static final Duration RELOAD_TIMEOUT = Duration.ofSeconds(20);
    private static final AtomicInteger CA_SEQUENCE = new AtomicInteger();

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests.truststore")
    RunOnServerClient runOnServer;

    private String pemBaselineSubject;
    private String noMacBaselineSubject;
    private String emptyMacBaselineSubject;
    private String mixedPemBaselineSubject;
    private String mixedP12BaselineSubject;
    private String dirBaselineSubject;

    @BeforeEach
    void establishSettledBaseline() throws Exception {
        pemBaselineSubject = writePem(CHANGEDET_PEM, generateCa());
        noMacBaselineSubject = writeNoMacPkcs12(CHANGEDET_NOMAC_P12, generateCa());
        emptyMacBaselineSubject = writeEmptyPasswordMacPkcs12(CHANGEDET_EMPTYMAC_P12, generateCa());
        mixedPemBaselineSubject = writePem(CHANGEDET_MIXED_PEM, generateCa());
        mixedP12BaselineSubject = writeNoMacPkcs12(CHANGEDET_MIXED_P12, generateCa());
        Files.deleteIfExists(CHANGEDET_DIR_EXTRA);
        dirBaselineSubject = writePem(CHANGEDET_DIR_BASELINE, generateCa());
        List<String> allBaselines = List.of(pemBaselineSubject, noMacBaselineSubject, emptyMacBaselineSubject,
                mixedPemBaselineSubject, mixedP12BaselineSubject, dirBaselineSubject);
        Awaitility.await("system truststore settles on the fresh baselines")
                .atMost(RELOAD_TIMEOUT).pollInterval(Duration.ofMillis(500))
                .until(() -> generatedStoreSubjects().containsAll(allBaselines));
    }

    @Test
    void pemSourceChangeTriggersReload() throws Exception {
        X509Certificate rotated = generateCa();
        assertRotationDetected(() -> writePem(CHANGEDET_PEM, rotated),
                List.of(subjectOf(rotated)), List.of(pemBaselineSubject));
    }

    @Test
    void noMacPkcs12SourceChangeTriggersReload() throws Exception {
        X509Certificate rotated = generateCa();
        assertRotationDetected(() -> writeNoMacPkcs12(CHANGEDET_NOMAC_P12, rotated),
                List.of(subjectOf(rotated)), List.of(noMacBaselineSubject));
    }

    @Test
    void emptyPasswordMacPkcs12SourceChangeTriggersReload() throws Exception {
        X509Certificate rotated = generateCa();
        assertRotationDetected(() -> writeEmptyPasswordMacPkcs12(CHANGEDET_EMPTYMAC_P12, rotated),
                List.of(subjectOf(rotated)), List.of(emptyMacBaselineSubject));
    }

    @Test
    void directorySourceAddReplaceRemoveTriggersReload() throws Exception {
        X509Certificate added = generateCa();
        X509Certificate replacement = generateCa();

        // ADD a new file to the directory: the extra CA must appear, the baseline must remain.
        assertRotationDetected(() -> writePem(CHANGEDET_DIR_EXTRA, added),
                List.of(subjectOf(added), dirBaselineSubject), List.of());

        // REPLACE the baseline file's content: the replacement must appear, the old baseline must go, the
        // added file must remain.
        assertRotationDetected(() -> writePem(CHANGEDET_DIR_BASELINE, replacement),
                List.of(subjectOf(replacement), subjectOf(added)), List.of(dirBaselineSubject));

        // REMOVE the added file: it must disappear even though no remaining file's content changed. This
        // stresses that the fingerprint tracks the directory's file SET, not just the bytes of known files.
        assertRotationDetected(() -> Files.delete(CHANGEDET_DIR_EXTRA),
                List.of(subjectOf(replacement)), List.of(subjectOf(added)));
    }

    @Test
    void mixedPemSourceChangeTriggersReload() throws Exception {
        X509Certificate rotated = generateCa();
        // The PEM half rotates; the untouched PKCS12 half must still be present.
        assertRotationDetected(() -> writePem(CHANGEDET_MIXED_PEM, rotated),
                List.of(subjectOf(rotated), mixedP12BaselineSubject), List.of(mixedPemBaselineSubject));
    }

    @Test
    void mixedPkcs12SourceChangeTriggersReload() throws Exception {
        X509Certificate rotated = generateCa();
        // The PKCS12 half rotates; the untouched PEM half must still be present.
        assertRotationDetected(() -> writeNoMacPkcs12(CHANGEDET_MIXED_P12, rotated),
                List.of(subjectOf(rotated), mixedPemBaselineSubject), List.of(mixedP12BaselineSubject));
    }

    @Test
    void assertIdleDoesNotReload() {
        long before = reloadCount();
        Awaitility.await("reload counter stays flat while all sources are stable")
                .pollInterval(Duration.ofMillis(250))
                .during(IDLE_WINDOW)
                .atMost(IDLE_WINDOW.plus(Duration.ofSeconds(8)))
                .until(() -> reloadCount() == before);
    }

    @Test
    void corruptSourceDuringReloadKeepsLastGoodTruststoreAndRecovers() throws Exception {
        long before = reloadCount();
        writeFileAtomically(CHANGEDET_NOMAC_P12, "this is not a valid keystore".getBytes(StandardCharsets.UTF_8));
        Awaitility.await("a failed reload does not advance the counter and keeps the previously loaded truststore")
                .during(IDLE_WINDOW)
                .atMost(IDLE_WINDOW.plus(Duration.ofSeconds(8)))
                .pollInterval(Duration.ofMillis(250))
                .until(() -> reloadCount() == before
                        && generatedStoreSubjects().containsAll(List.of(pemBaselineSubject, noMacBaselineSubject)));

        X509Certificate recovered = generateCa();
        writeNoMacPkcs12(CHANGEDET_NOMAC_P12, recovered);
        Awaitility.await("a valid rotation after a failure resumes reloading and is picked up")
                .atMost(RELOAD_TIMEOUT).pollInterval(Duration.ofMillis(500))
                .until(() -> reloadCount() > before && generatedStoreSubjects().contains(subjectOf(recovered)));
    }

    private void assertRotationDetected(IoAction mutation, List<String> mustAppear, List<String> mustDisappear)
            throws Exception {
        long before = reloadCount();
        mutation.run();
        Awaitility.await("source change is detected and the merged store is rewritten")
                .atMost(RELOAD_TIMEOUT).pollInterval(Duration.ofMillis(500))
                .until(() -> {
                    if (reloadCount() <= before) {
                        return false;
                    }
                    Set<String> subjects = generatedStoreSubjects();
                    return subjects.containsAll(mustAppear) && Collections.disjoint(subjects, mustDisappear);
                });
    }

    private long reloadCount() {
        Long count = runOnServer.fetch(session -> CDI.current().select(SystemTruststoreReload.class).get().reloadCount(), Long.class);
        return count == null ? 0L : count;
    }

    private Set<String> generatedStoreSubjects() {
        String[] subjects = runOnServer.fetch(session -> {
            List<String> found = new ArrayList<>();
            try {
                KeyStore keyStore = KeystoreUtil.loadKeyStore(
                        System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY),
                        System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY),
                        System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY));
                for (Enumeration<String> aliases = keyStore.aliases(); aliases.hasMoreElements();) {
                    Certificate certificate = keyStore.getCertificate(aliases.nextElement());
                    if (certificate instanceof X509Certificate) {
                        found.add(((X509Certificate) certificate).getSubjectX500Principal().getName());
                    }
                }
            } catch (Exception ignored) {
                // store may be mid-rewrite; return what we have and let the caller retry
            }
            return found.toArray(new String[0]);
        }, String[].class);
        return new HashSet<>(Arrays.asList(subjects));
    }

    private static String writePem(Path file, X509Certificate certificate) throws Exception {
        writeFileAtomically(file, certificatePem(certificate).getBytes(StandardCharsets.UTF_8));
        return subjectOf(certificate);
    }

    // No-MAC PKCS12: KeyStore#store with a null password.
    private static String writeNoMacPkcs12(Path file, X509Certificate certificate) throws Exception {
        writeFileAtomically(file, pkcs12Bytes(null, certificate));
        return subjectOf(certificate);
    }

    // Empty-password-MAC PKCS12: KeyStore#store with "".toCharArray(), so the store carries an empty MAC.
    private static String writeEmptyPasswordMacPkcs12(Path file, X509Certificate certificate) throws Exception {
        writeFileAtomically(file, pkcs12Bytes("".toCharArray(), certificate));
        return subjectOf(certificate);
    }

    private static byte[] pkcs12Bytes(char[] storePassword, X509Certificate... certificates) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        for (X509Certificate certificate : certificates) {
            keyStore.setCertificateEntry(subjectOf(certificate), certificate);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        keyStore.store(out, storePassword);
        return out.toByteArray();
    }

    private static void writeFileAtomically(Path target, byte[] content) throws IOException {
        Files.createDirectories(target.getParent());
        // Stage in the tmp root (not inside any watched directory) then atomically move into place, so a
        // directory scan never lists a partially written file.
        Path staged = Files.createTempFile(TMP, "kc-it-changedet-", ".tmp");
        try {
            Files.write(staged, content);
            try {
                Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(staged, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(staged);
        }
    }

    private static X509Certificate generateCa() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return CryptoIntegration.getProvider().getCertificateUtils()
                .generateV1SelfSignedCertificate(keyPair, "Change Detection IT CA " + CA_SEQUENCE.incrementAndGet());
    }

    private static String subjectOf(X509Certificate certificate) {
        return certificate.getSubjectX500Principal().getName();
    }

    private static String certificatePem(X509Certificate certificate) {
        return PemUtils.addCertificateBeginEnd(PemUtils.encodeCertificate(certificate)) + "\n";
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws Exception;
    }

    static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            prepareTruststorePathsSourceFiles();
            // truststore-paths lists every source shape under test: a PEM file, a no-MAC PKCS12, an
            // empty-password-MAC PKCS12, a directory, and a side-by-side PEM+PKCS12 pair - all on one period.
            String paths = String.join(",",
                    CHANGEDET_PEM.toString(),
                    CHANGEDET_NOMAC_P12.toString(),
                    CHANGEDET_EMPTYMAC_P12.toString(),
                    CHANGEDET_DIR.toString(),
                    CHANGEDET_MIXED_PEM.toString(),
                    CHANGEDET_MIXED_P12.toString());
            return config
                    .option(TruststoreOptions.TRUSTSTORE_PATHS.getKey(), paths)
                    .option(TruststoreOptions.TRUSTSTORE_PATHS_RELOAD_PERIOD.getKey(), RELOAD_PERIOD.toSeconds() + "s");
        }
    }

    private static void prepareTruststorePathsSourceFiles() {
        try {
            Files.createDirectories(CHANGEDET_DIR);
            writeFileAtomically(CHANGEDET_PEM, new byte[0]);
            writeFileAtomically(CHANGEDET_MIXED_PEM, new byte[0]);
            writeFileAtomically(CHANGEDET_DIR_BASELINE, new byte[0]);
            writeFileAtomically(CHANGEDET_NOMAC_P12, pkcs12Bytes(null));
            writeFileAtomically(CHANGEDET_MIXED_P12, pkcs12Bytes(null));
            writeFileAtomically(CHANGEDET_EMPTYMAC_P12, pkcs12Bytes("".toCharArray()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
