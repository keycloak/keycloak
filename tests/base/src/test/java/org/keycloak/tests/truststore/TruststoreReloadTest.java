package org.keycloak.tests.truststore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.common.util.PemUtils;
import org.keycloak.config.TruststoreOptions;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.services.x509.NginxProxySslClientCertificateLookupFactory;
import org.keycloak.services.x509.NginxProxySslClientCertificateLookupFactory.ReloadableX509ClientCertificateLookup;
import org.keycloak.services.x509.X509ClientCertificateLookup;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.truststore.TruststoreBuilder;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.PfxOptions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = TruststoreReloadTest.ServerConfig.class)
public class TruststoreReloadTest {

    private static final Path TRUSTSTORE_FILE = Path.of(System.getProperty("java.io.tmpdir"), "kc-it-system-truststore.pem");
    // Additional truststore-paths sources exercising the PKCS12, directory and mixed PEM+PKCS12 cases.
    private static final Path NO_MAC_PKCS12_FILE = Path.of(System.getProperty("java.io.tmpdir"), "kc-it-system-truststore-nomac.p12");
    private static final Path EMPTY_MAC_PKCS12_FILE = Path.of(System.getProperty("java.io.tmpdir"), "kc-it-system-truststore-emptymac.p12");
    private static final Path TRUSTSTORE_DIR = Path.of(System.getProperty("java.io.tmpdir"), "kc-it-system-truststore-dir");
    private static final Path DIR_PEM_FILE = TRUSTSTORE_DIR.resolve("dir-ca.pem");
    private static final Path DIR_PKCS12_FILE = TRUSTSTORE_DIR.resolve("dir-ca.p12");
    private static final Path MIXED_PEM_FILE = Path.of(System.getProperty("java.io.tmpdir"), "kc-it-system-truststore-mixed.pem");
    private static final Path MIXED_PKCS12_FILE = Path.of(System.getProperty("java.io.tmpdir"), "kc-it-system-truststore-mixed.p12");

    private static final byte[] STARTUP_TRUSTED_CERTIFICATE = readResource("org/keycloak/tests/ssl/smtp-server.pem");
    private static final byte[] STARTUP_TRUSTED_KEYSTORE = readResource("org/keycloak/tests/ssl/smtp-server.p12");
    private static final String STARTUP_TRUSTED_KEYSTORE_PASSWORD = "changeit";
    private static final String FRESH_KEYSTORE_PASSWORD = "password";
    private static final AtomicInteger CA_SEQUENCE = new AtomicInteger();

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests.truststore")
    RunOnServerClient runOnServer;

    private static Vertx vertx;

    @BeforeAll
    static void prepareVertx() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    static void closeVertx() throws Exception {
        vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    @BeforeEach
    void resetSystemTruststore() throws IOException {
        Files.write(TRUSTSTORE_FILE, STARTUP_TRUSTED_CERTIFICATE);
        awaitReloaded(() -> generatedTruststoreFileContains(startupTrustedSubject()));
    }

    @Test
    void outboundHttpClientPicksUpRotatedCaAfterReload() throws Exception {
        try (TlsPeer trusted = startTrustedPeer(); TlsPeer rotated = startPeerWithFreshCa()) {
            assertTrue(httpClientTrusts(url(trusted)), "startup-trusted peer must be trusted");
            assertFalse(httpClientTrusts(url(rotated)), "fresh peer must not be trusted before reload");

            rotateSystemTruststoreTo(rotated.certificateAuthority);
            awaitReloaded(() -> httpClientTrusts(url(rotated)) && !httpClientTrusts(url(trusted)));
        }
    }

    @Test
    void reloadDoesNotAbortInFlightRequestAndItCompletesAfterRelease() throws Exception {
        // Reload is proactive (old certs still valid): a request already in flight on the previous truststore
        // must keep running and complete; only new requests switch to the new truststore. The peer withholds
        // its response until we release it, so the reload provably happens while the request is still open.
        try (TlsPeer trusted = startHeldTrustedPeer(); TlsPeer rotated = startPeerWithFreshCa()) {
            assertFalse(httpClientTrusts(url(rotated)), "fresh peer must not be trusted before reload");

            startInFlightHttpRequest(url(trusted));
            // The peer has received the request (connection open) but is withholding the response.
            Awaitility.await().atMost(Duration.ofSeconds(10)).until(trusted::hasReceivedRequest);
            assertFalse(inFlightHttpRequestFinished(), "request must still be in flight before the reload");

            rotateSystemTruststoreTo(rotated.certificateAuthority);
            awaitReloaded(() -> httpClientTrusts(url(rotated)));

            // The reload happened while the request was still open — it must NOT have been aborted.
            assertFalse(inFlightHttpRequestFinished(),
                    "a system-truststore reload must not abort an in-flight request");

            trusted.release();
            assertTrue(inFlightHttpRequestSucceeded(), "the released in-flight request must complete successfully");
        }
    }

    @Test
    void ldapsSocketFactoryPicksUpRotatedCaAfterReload() throws Exception {
        try (TlsPeer trusted = startTrustedPeer(); TlsPeer rotated = startPeerWithFreshCa()) {
            assertTrue(ldapsSocketFactoryTrusts(trusted.port()), "startup-trusted peer must be trusted");
            assertFalse(ldapsSocketFactoryTrusts(rotated.port()), "fresh peer must not be trusted before reload");

            rotateSystemTruststoreTo(rotated.certificateAuthority);
            awaitReloaded(() -> ldapsSocketFactoryTrusts(rotated.port()));
            awaitReloaded(() -> !ldapsSocketFactoryTrusts(trusted.port()));
        }
    }

    @Test
    void nginxLookupPicksUpRotatedCaAfterReload() throws Exception {
        X509Certificate rotatedCa = generateCertificateAuthority();
        String rotatedSubject = rotatedCa.getSubjectX500Principal().getName();

        assertTrue(nginxLookupTrusts(startupTrustedSubject()), "startup-trusted ca must be trusted");
        assertFalse(nginxLookupTrusts(rotatedSubject), "fresh ca must not be trusted before reload");

        rotateSystemTruststoreTo(rotatedCa);
        awaitReloaded(() -> nginxLookupTrusts(rotatedSubject));
        awaitReloaded(() -> !nginxLookupTrusts(startupTrustedSubject()));
    }

    @Test
    void generatedTruststoreFileOnDiskIsRewrittenAfterReload() throws Exception {
        X509Certificate rotatedCa = generateCertificateAuthority();
        String rotatedSubject = rotatedCa.getSubjectX500Principal().getName();

        assertTrue(generatedTruststoreFileContains(startupTrustedSubject()),
                "startup ca must be persisted in the generated truststore file");
        assertFalse(generatedTruststoreFileContains(rotatedSubject),
                "fresh ca must not be in the generated truststore file before reload");

        rotateSystemTruststoreTo(rotatedCa);
        awaitReloaded(() -> generatedTruststoreFileContains(rotatedSubject)
                && !generatedTruststoreFileContains(startupTrustedSubject()));
    }

    @Test
    void noMacPkcs12TruststorePathPicksUpRotatedCaAfterReload() throws Exception {
        // A PKCS12 truststore-paths file with no MAC (KeyStore#store with a null password).
        try (TlsPeer initial = startPeerWithFreshCa(); TlsPeer rotated = startPeerWithFreshCa()) {
            writeNoMacPkcs12(NO_MAC_PKCS12_FILE, initial.certificateAuthority);
            awaitReloaded(() -> httpClientTrusts(url(initial)));
            assertFalse(httpClientTrusts(url(rotated)), "fresh peer must not be trusted before rotation");

            writeNoMacPkcs12(NO_MAC_PKCS12_FILE, rotated.certificateAuthority);
            awaitReloaded(() -> httpClientTrusts(url(rotated)) && !httpClientTrusts(url(initial)));
        }
    }

    @Test
    void emptyPasswordMacPkcs12TruststorePathPicksUpRotatedCaAfterReload() throws Exception {
        // A PKCS12 truststore-paths file with an empty-password MAC (KeyStore#store with "".toCharArray()).
        try (TlsPeer initial = startPeerWithFreshCa(); TlsPeer rotated = startPeerWithFreshCa()) {
            writeEmptyPasswordMacPkcs12(EMPTY_MAC_PKCS12_FILE, initial.certificateAuthority);
            awaitReloaded(() -> httpClientTrusts(url(initial)));
            assertFalse(httpClientTrusts(url(rotated)), "fresh peer must not be trusted before rotation");

            writeEmptyPasswordMacPkcs12(EMPTY_MAC_PKCS12_FILE, rotated.certificateAuthority);
            awaitReloaded(() -> httpClientTrusts(url(rotated)) && !httpClientTrusts(url(initial)));
        }
    }

    @Test
    void directoryTruststorePathPicksUpRotatedCaAfterReload() throws Exception {
        // The truststore-path is a directory holding both a PEM and a PKCS12 file: a reload must re-scan the
        // directory and pick up rotations in both files while dropping the previous certificates.
        try (TlsPeer initialPem = startPeerWithFreshCa(); TlsPeer initialPkcs12 = startPeerWithFreshCa();
                TlsPeer rotatedPem = startPeerWithFreshCa(); TlsPeer rotatedPkcs12 = startPeerWithFreshCa()) {
            writeCertificatesToPem(DIR_PEM_FILE, initialPem.certificateAuthority);
            writeNoMacPkcs12(DIR_PKCS12_FILE, initialPkcs12.certificateAuthority);
            awaitReloaded(() -> httpClientTrusts(url(initialPem)));
            awaitReloaded(() -> httpClientTrusts(url(initialPkcs12)));
            assertFalse(httpClientTrusts(url(rotatedPem)), "fresh PEM peer must not be trusted before rotation");
            assertFalse(httpClientTrusts(url(rotatedPkcs12)), "fresh PKCS12 peer must not be trusted before rotation");

            writeCertificatesToPem(DIR_PEM_FILE, rotatedPem.certificateAuthority);
            writeNoMacPkcs12(DIR_PKCS12_FILE, rotatedPkcs12.certificateAuthority);
            awaitReloaded(() -> httpClientTrusts(url(rotatedPem)));
            awaitReloaded(() -> httpClientTrusts(url(rotatedPkcs12)));
            awaitReloaded(() -> !httpClientTrusts(url(initialPem)));
            awaitReloaded(() -> !httpClientTrusts(url(initialPkcs12)));
        }
    }

    @Test
    void mixedPemAndPkcs12TruststorePathsPickUpRotatedCaAfterReload() throws Exception {
        // truststore-paths lists a PEM file and a PKCS12 file side by side: a reload must pick up rotations in
        // both entries while dropping the previous certificates.
        try (TlsPeer initialPem = startPeerWithFreshCa(); TlsPeer initialPkcs12 = startPeerWithFreshCa();
                TlsPeer rotatedPem = startPeerWithFreshCa(); TlsPeer rotatedPkcs12 = startPeerWithFreshCa()) {
            writeCertificatesToPem(MIXED_PEM_FILE, initialPem.certificateAuthority);
            writeNoMacPkcs12(MIXED_PKCS12_FILE, initialPkcs12.certificateAuthority);
            awaitReloaded(() -> httpClientTrusts(url(initialPem)) && httpClientTrusts(url(initialPkcs12)));
            assertFalse(httpClientTrusts(url(rotatedPem)), "fresh PEM peer must not be trusted before rotation");
            assertFalse(httpClientTrusts(url(rotatedPkcs12)), "fresh PKCS12 peer must not be trusted before rotation");

            writeCertificatesToPem(MIXED_PEM_FILE, rotatedPem.certificateAuthority);
            writeNoMacPkcs12(MIXED_PKCS12_FILE, rotatedPkcs12.certificateAuthority);
            awaitReloaded(() -> httpClientTrusts(url(rotatedPem)));
            awaitReloaded(() -> httpClientTrusts(url(rotatedPkcs12)));
            awaitReloaded(() -> !httpClientTrusts(url(initialPem)));
            awaitReloaded(() -> !httpClientTrusts(url(initialPkcs12)));
        }
    }

    private boolean httpClientTrusts(String url) {
        return runOnServer.fetch(session -> {
            try {
                return "ok".equals(session.getProvider(HttpClientProvider.class).getString(url));
            } catch (Exception untrusted) {
                return Boolean.FALSE;
            }
        }, Boolean.class);
    }

    private void startInFlightHttpRequest(String url) {
        runOnServer.fetch(session -> {
            InFlightHttpRequest.start(session.getProvider(HttpClientProvider.class), url);
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private boolean inFlightHttpRequestFinished() {
        return runOnServer.fetch(session -> InFlightHttpRequest.isFinished(), Boolean.class);
    }

    private boolean inFlightHttpRequestSucceeded() {
        return runOnServer.fetch(session -> InFlightHttpRequest.awaitSuccess(), Boolean.class);
    }

    private boolean ldapsSocketFactoryTrusts(int port) {
        return runOnServer.fetch(session -> {
            try {
                // this factory is currently used by the LDAPContextManager
                javax.net.ssl.SSLSocket socket = (javax.net.ssl.SSLSocket) org.keycloak.truststore.SSLSocketFactory
                        .getDefault().createSocket("localhost", port);
                javax.net.ssl.SSLParameters parameters = socket.getSSLParameters();
                parameters.setEndpointIdentificationAlgorithm(null);
                socket.setSSLParameters(parameters);
                socket.startHandshake();
                socket.close();
                return Boolean.TRUE;
            } catch (Exception untrusted) {
                return Boolean.FALSE;
            }
        }, Boolean.class);
    }

    private boolean nginxLookupTrusts(String subject) {
        return runOnServer.fetch(session -> {
            try {
                NginxProxySslClientCertificateLookupFactory factory = (NginxProxySslClientCertificateLookupFactory) session
                        .getKeycloakSessionFactory().getProviderFactory(X509ClientCertificateLookup.class, "nginx");
                ReloadableX509ClientCertificateLookup lookup = (ReloadableX509ClientCertificateLookup) factory.create(session);
                Set<String> subjects = new HashSet<>();
                lookup.getIntermediateCerts().forEach(cert -> subjects.add(cert.getSubjectX500Principal().getName()));
                lookup.getTrustedRootCerts().forEach(cert -> subjects.add(cert.getSubjectX500Principal().getName()));
                return subjects.contains(subject);
            } catch (Exception e) {
                return Boolean.FALSE;
            }
        }, Boolean.class);
    }

    private boolean generatedTruststoreFileContains(String subject) {
        return runOnServer.fetch(session -> {
            try {
                KeyStore keyStore = KeystoreUtil.loadKeyStore(
                        System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY),
                        System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY),
                        System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY));
                for (Enumeration<String> aliases = keyStore.aliases(); aliases.hasMoreElements();) {
                    Certificate certificate = keyStore.getCertificate(aliases.nextElement());
                    if (certificate instanceof X509Certificate x509Certificate
                            && x509Certificate.getSubjectX500Principal().getName().equals(subject)) {
                        return Boolean.TRUE;
                    }
                }
                return Boolean.FALSE;
            } catch (Exception e) {
                return Boolean.FALSE;
            }
        }, Boolean.class);
    }

    private void awaitReloaded(Callable<Boolean> reloaded) {
        Awaitility.await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(500)).until(reloaded);
    }

    private static void rotateSystemTruststoreTo(X509Certificate certificateAuthority) throws IOException {
        Files.writeString(TRUSTSTORE_FILE, certificatePem(certificateAuthority));
    }

    private static void writeCertificatesToPem(Path file, X509Certificate... certificates) throws IOException {
        StringBuilder pem = new StringBuilder();
        for (X509Certificate certificate : certificates) {
            pem.append(certificatePem(certificate));
        }
        Files.writeString(file, pem.toString());
    }

    // No-MAC PKCS12: KeyStore.getInstance("PKCS12"); load(null, null); setCertificateEntry; store(fos, null).
    private static void writeNoMacPkcs12(Path file, X509Certificate... certificates) throws Exception {
        writePkcs12(file, null, certificates);
    }

    // Empty-password-MAC PKCS12: identical, but stored with "".toCharArray() so the store carries an empty MAC.
    private static void writeEmptyPasswordMacPkcs12(Path file, X509Certificate... certificates) throws Exception {
        writePkcs12(file, "".toCharArray(), certificates);
    }

    private static void writePkcs12(Path file, char[] storePassword, X509Certificate... certificates) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        for (X509Certificate certificate : certificates) {
            keyStore.setCertificateEntry(certificate.getSubjectX500Principal().getName(), certificate);
        }
        try (var out = Files.newOutputStream(file)) {
            keyStore.store(out, storePassword);
        }
    }

    private static String url(TlsPeer peer) {
        return "https://localhost:" + peer.port() + "/";
    }

    private TlsPeer startTrustedPeer() throws Exception {
        HttpServer server = startHttpsPeer(new PfxOptions()
                .setValue(Buffer.buffer(STARTUP_TRUSTED_KEYSTORE))
                .setPassword(STARTUP_TRUSTED_KEYSTORE_PASSWORD));
        return new TlsPeer(server, null);
    }

    private TlsPeer startHeldTrustedPeer() throws Exception {
        AtomicReference<HttpServerResponse> heldResponse = new AtomicReference<>();
        HttpServer server = vertx
                .createHttpServer(new HttpServerOptions().setSsl(true).setKeyCertOptions(new PfxOptions()
                        .setValue(Buffer.buffer(STARTUP_TRUSTED_KEYSTORE))
                        .setPassword(STARTUP_TRUSTED_KEYSTORE_PASSWORD)))
                .requestHandler(request -> heldResponse.set(request.response())); // capture, answer only on release()
        server.listen(0, "localhost").toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS);
        return new TlsPeer(server, null, heldResponse);
    }

    private TlsPeer startPeerWithFreshCa() throws Exception {
        KeyPair caKeyPair = generateKeyPair();
        X509Certificate certificateAuthority = selfSignedCertificate(caKeyPair);
        KeyPair serverKeyPair = generateKeyPair();
        X509Certificate serverCertificate = CryptoIntegration.getProvider().getCertificateUtils()
                .generateV3Certificate(serverKeyPair, caKeyPair.getPrivate(), certificateAuthority, "localhost");
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("server", serverKeyPair.getPrivate(), FRESH_KEYSTORE_PASSWORD.toCharArray(),
                new Certificate[] { serverCertificate, certificateAuthority });
        ByteArrayOutputStream keyStoreBytes = new ByteArrayOutputStream();
        keyStore.store(keyStoreBytes, FRESH_KEYSTORE_PASSWORD.toCharArray());
        HttpServer server = startHttpsPeer(new PfxOptions()
                .setValue(Buffer.buffer(keyStoreBytes.toByteArray()))
                .setPassword(FRESH_KEYSTORE_PASSWORD));
        return new TlsPeer(server, certificateAuthority);
    }

    private HttpServer startHttpsPeer(KeyCertOptions keyCertOptions) throws Exception {
        HttpServer server = vertx
                .createHttpServer(new HttpServerOptions().setSsl(true).setKeyCertOptions(keyCertOptions))
                .requestHandler(request -> request.response().end("ok"));
        return server.listen(0, "localhost").toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS);
    }

    private static X509Certificate generateCertificateAuthority() throws Exception {
        return selfSignedCertificate(generateKeyPair());
    }

    private static String startupTrustedSubject() {
        return PemUtils.decodeCertificate(new String(STARTUP_TRUSTED_CERTIFICATE, StandardCharsets.UTF_8))
                .getSubjectX500Principal().getName();
    }

    private static X509Certificate selfSignedCertificate(KeyPair caKeyPair) {
        return CryptoIntegration.getProvider().getCertificateUtils()
                .generateV1SelfSignedCertificate(caKeyPair, "Truststore Reload IT CA " + CA_SEQUENCE.incrementAndGet());
    }

    private static String certificatePem(X509Certificate certificate) {
        return PemUtils.addCertificateBeginEnd(PemUtils.encodeCertificate(certificate)) + "\n";
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static byte[] readResource(String resource) {
        try (InputStream stream = TruststoreReloadTest.class.getClassLoader().getResourceAsStream(resource)) {
            // stream can be null when this class is reloaded by TestClassLoader, in which case these resources are not
            // needed anyway
            return stream == null ? null : stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Runs an outbound request on the server and holds its Future across runOnServer calls so the test can
    // start it, trigger a reload, then verify it still completed. Lives in the run-on-server package.
    private static final class InFlightHttpRequest {

        private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "kc-it-inflight-request");
            thread.setDaemon(true);
            return thread;
        });
        private static volatile Future<String> future;

        static void start(HttpClientProvider provider, String url) {
            future = EXECUTOR.submit(() -> provider.getString(url));
        }

        static boolean isFinished() {
            return future != null && future.isDone();
        }

        static boolean awaitSuccess() {
            try {
                return "ok".equals(future.get(15, TimeUnit.SECONDS));
            } catch (Exception aborted) {
                return Boolean.FALSE;
            }
        }
    }

    private record TlsPeer(HttpServer server, X509Certificate certificateAuthority,
                           AtomicReference<HttpServerResponse> heldResponse) implements AutoCloseable {

            private TlsPeer(HttpServer server, X509Certificate certificateAuthority) {
                this(server, certificateAuthority, null);
            }

        private int port() {
                return server.actualPort();
            }

            private boolean hasReceivedRequest() {
                return heldResponse != null && heldResponse.get() != null;
            }

            private void release() {
                HttpServerResponse response = heldResponse == null ? null : heldResponse.get();
                if (response != null) {
                    response.end("ok");
                }
            }

            @Override
            public void close() throws Exception {
                server.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
            }
        }

    static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            prepareTruststorePathsSourceFiles();
            // truststore-paths is a list: a PEM file, a no-MAC PKCS12, an empty-password-MAC PKCS12, a
            // directory holding PEM+PKCS12, and a mixed PEM+PKCS12 pair - all reloaded on the same period.
            String paths = String.join(",",
                    TRUSTSTORE_FILE.toString(),
                    NO_MAC_PKCS12_FILE.toString(),
                    EMPTY_MAC_PKCS12_FILE.toString(),
                    TRUSTSTORE_DIR.toString(),
                    MIXED_PEM_FILE.toString(),
                    MIXED_PKCS12_FILE.toString());
            return config
                    .option(TruststoreOptions.TRUSTSTORE_PATHS.getKey(), paths)
                    .option(TruststoreOptions.TRUSTSTORE_PATHS_RELOAD_PERIOD.getKey(), "2s")
                    .option(TruststoreOptions.HOSTNAME_VERIFICATION_POLICY.getKey(), "ANY")
                    // Keep the outbound socket read-timeout well above the reload window so a held in-flight
                    // request does not time out on its own during the test.
                    .option("spi-connections-http-client-default-socket-timeout-millis", "60000");
        }
    }

    private static void prepareTruststorePathsSourceFiles() {
        try {
            Files.write(TRUSTSTORE_FILE, STARTUP_TRUSTED_CERTIFICATE);
            // Every truststore-paths source must exist and be loadable when the server boots; seed the
            // additional PKCS12/PEM/directory sources empty so the reload tests can populate them later.
            Files.createDirectories(TRUSTSTORE_DIR);
            writeCertificatesToPem(DIR_PEM_FILE);
            writeCertificatesToPem(MIXED_PEM_FILE);
            writeNoMacPkcs12(NO_MAC_PKCS12_FILE);
            writeNoMacPkcs12(DIR_PKCS12_FILE);
            writeNoMacPkcs12(MIXED_PKCS12_FILE);
            writeEmptyPasswordMacPkcs12(EMPTY_MAC_PKCS12_FILE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
