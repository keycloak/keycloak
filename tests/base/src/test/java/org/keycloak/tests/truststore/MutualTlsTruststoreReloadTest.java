package org.keycloak.tests.truststore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
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
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;

import jakarta.enterprise.inject.spi.CDI;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.common.util.PemUtils;
import org.keycloak.config.HealthOptions;
import org.keycloak.config.HttpOptions;
import org.keycloak.config.TruststoreOptions;
import org.keycloak.crypto.def.DefaultCryptoProvider;
import org.keycloak.quarkus.runtime.integration.tls.SystemTruststoreReload;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.truststore.TruststoreBuilder;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Without a dedicated HTTPS truststore, the HTTPS and management interfaces validate client certificates against the
 * system truststore, so a system truststore reload must change which client certificates they accept.
 */
@KeycloakIntegrationTest(config = MutualTlsTruststoreReloadTest.ServerConfig.class)
public class MutualTlsTruststoreReloadTest {

    private static final Path TRUSTSTORE_FILE = Path.of(System.getProperty("java.io.tmpdir"), "kc-it-mtls-system-truststore.pem");
    private static final Duration RELOAD_TIMEOUT = Duration.ofSeconds(20);

    @InjectCertificates(config = TlsConfig.class)
    ManagedCertificates certificates;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests.truststore")
    RunOnServerClient runOnServer;

    @Test
    void httpsAndManagementInterfacesFollowSystemTruststoreReload() throws Exception {
        URL https = keycloakUrls.getBaseUrl();
        URL management = URI.create(keycloakUrls.getMetric()).toURL();
        ClientIdentity initial = ClientIdentities.INITIAL;
        ClientIdentity rotated = ClientIdentity.generate("mTLS Truststore Reload IT rotated client");

        assertTrue(acceptsClientCertificate(https, initial), "HTTPS must accept the client trusted at startup");
        assertFalse(acceptsClientCertificate(https, rotated), "HTTPS must reject the client before it is trusted");
        assertTrue(acceptsClientCertificate(management, initial), "management must accept the client trusted at startup");
        assertFalse(acceptsClientCertificate(management, rotated), "management must reject the client before it is trusted");

        long reloadsBefore = reloadCount();
        writeTrustedClient(rotated);

        Awaitility.await("system truststore is reloaded with the rotated client instead of the initial one")
                .atMost(RELOAD_TIMEOUT).pollInterval(Duration.ofMillis(500))
                .until(() -> {
                    Set<String> subjects = systemTruststoreSubjects();
                    return reloadCount() > reloadsBefore && subjects.contains(rotated.subject())
                            && !subjects.contains(initial.subject());
                });

        Awaitility.await("HTTPS interface trusts the rotated client and no longer the initial one")
                .atMost(RELOAD_TIMEOUT).pollInterval(Duration.ofMillis(500))
                .until(() -> acceptsClientCertificate(https, rotated) && !acceptsClientCertificate(https, initial));
        Awaitility.await("management interface trusts the rotated client and no longer the initial one")
                .atMost(RELOAD_TIMEOUT).pollInterval(Duration.ofMillis(500))
                .until(() -> acceptsClientCertificate(management, rotated) && !acceptsClientCertificate(management, initial));
    }

    private boolean acceptsClientCertificate(URL url, ClientIdentity client) {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[] { new ClientCertificateKeyManager(client) }, trustServerCertificate().getTrustManagers(), null);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
        try (SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket(url.getHost(), url.getPort())) {
            SSLParameters parameters = socket.getSSLParameters();
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            socket.setSSLParameters(parameters);
            socket.setSoTimeout(10_000);
            OutputStream out = socket.getOutputStream();
            out.write(("GET / HTTP/1.1\r\nHost: " + url.getHost() + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();
            // with TLS 1.3 the server validates the client certificate after the client finished its part of the
            // handshake, so a rejected certificate only surfaces when reading the response
            String statusLine = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII)).readLine();
            return statusLine != null && statusLine.startsWith("HTTP/1.1 ");
        } catch (IOException rejected) {
            return false;
        }
    }

    private TrustManagerFactory trustServerCertificate() throws GeneralSecurityException, IOException {
        KeyStore serverKeyStore;
        try {
            serverKeyStore = KeystoreUtil.loadKeyStore(certificates.getServerKeyStorePath(),
                    certificates.getServerKeyStorePassword(), certificates.getKeystoreFormat().name());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        KeyStore serverCertificates = KeyStore.getInstance(KeyStore.getDefaultType());
        serverCertificates.load(null, null);
        for (String alias : Collections.list(serverKeyStore.aliases())) {
            Certificate certificate = serverKeyStore.getCertificate(alias);
            if (certificate != null) {
                serverCertificates.setCertificateEntry(alias, certificate);
            }
        }
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(serverCertificates);
        return trustManagerFactory;
    }

    private long reloadCount() {
        Long count = runOnServer.fetch(session -> CDI.current().select(SystemTruststoreReload.class).get().reloadCount(), Long.class);
        return count == null ? 0L : count;
    }

    private Set<String> systemTruststoreSubjects() {
        String[] subjects = runOnServer.fetch(session -> {
            List<String> found = new ArrayList<>();
            try {
                KeyStore keyStore = KeystoreUtil.loadKeyStore(
                        System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY),
                        System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY),
                        System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY),
                        System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_PROVIDER_KEY));
                for (Enumeration<String> aliases = keyStore.aliases(); aliases.hasMoreElements();) {
                    if (keyStore.getCertificate(aliases.nextElement()) instanceof X509Certificate certificate) {
                        found.add(certificate.getSubjectX500Principal().getName());
                    }
                }
            } catch (Exception ignored) {
                // store may be mid-rewrite; return what we have and let the caller retry
            }
            return found.toArray(new String[0]);
        }, String[].class);
        return new HashSet<>(Arrays.asList(subjects));
    }

    private static void writeTrustedClient(ClientIdentity client) {
        try {
            // stage next to the target and move it into place, so a reload never reads a partially written file
            Path staged = Files.createTempFile(TRUSTSTORE_FILE.getParent(), "kc-it-mtls-", ".tmp");
            try {
                Files.writeString(staged, PemUtils.addCertificateBeginEnd(PemUtils.encodeCertificate(client.certificate())) + "\n");
                try {
                    Files.move(staged, TRUSTSTORE_FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(staged, TRUSTSTORE_FILE, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(staged);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    record ClientIdentity(PrivateKey privateKey, X509Certificate certificate) {

        static ClientIdentity generate(String commonName) {
            if (!CryptoIntegration.isInitialised()) {
                CryptoIntegration.setProvider(new DefaultCryptoProvider());
            }
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair keyPair = generator.generateKeyPair();
                // a self-signed v1 certificate has no extended key usage restricting it to server authentication
                X509Certificate certificate = CryptoIntegration.getProvider().getCertificateUtils()
                        .generateV1SelfSignedCertificate(keyPair, commonName);
                return new ClientIdentity(keyPair.getPrivate(), certificate);
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }

        String subject() {
            return certificate.getSubjectX500Principal().getName();
        }
    }

    // initialized on first use in the test JVM only, the server also loads this class to run the run-on-server lambdas
    static final class ClientIdentities {
        static final ClientIdentity INITIAL = ClientIdentity.generate("mTLS Truststore Reload IT initial client");
    }

    // always presents the client certificate, even when the server does not list its issuer as acceptable, so that
    // the server has to validate it instead of the client silently connecting without one
    private static final class ClientCertificateKeyManager extends X509ExtendedKeyManager {

        private static final String ALIAS = "client";

        private final ClientIdentity client;

        private ClientCertificateKeyManager(ClientIdentity client) {
            this.client = client;
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return new String[] { ALIAS };
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            return ALIAS;
        }

        @Override
        public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
            return ALIAS;
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return null;
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            return null;
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return new X509Certificate[] { client.certificate() };
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return client.privateKey();
        }
    }

    static class TlsConfig implements CertificatesConfig {
        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            // no mTLS here: the test framework would configure a dedicated HTTPS truststore for it
            return config.tlsEnabled(true);
        }
    }

    static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            writeTrustedClient(ClientIdentities.INITIAL);
            return config
                    .option(TruststoreOptions.TRUSTSTORE_PATHS.getKey(), TRUSTSTORE_FILE.toString())
                    .option(TruststoreOptions.TRUSTSTORE_PATHS_RELOAD_PERIOD.getKey(), "2s")
                    // 'request' rather than 'required', so the test framework's own clients, which present no
                    // certificate, keep working
                    .option(HttpOptions.HTTPS_CLIENT_AUTH.getKey(), "request")
                    // only the system truststore reload may refresh the trust of the HTTPS and management interfaces
                    .option(HttpOptions.HTTPS_CERTIFICATES_RELOAD_PERIOD.getKey(), "-1")
                    // starts the management interface
                    .option(HealthOptions.HEALTH_ENABLED.getKey(), "true");
        }
    }
}
