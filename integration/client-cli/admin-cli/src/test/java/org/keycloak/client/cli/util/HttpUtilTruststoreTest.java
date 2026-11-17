package org.keycloak.client.cli.util;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class HttpUtilTruststoreTest {

    private static final String PASSWORD = "changeit";

    private Path testDirectory;
    private HttpsServer server;

    @Before
    public void createTestDirectory() throws Exception {
        testDirectory = Files.createTempDirectory(Path.of("target"), "kcadm-truststore-");
    }

    @After
    public void cleanup() throws Exception {
        HttpUtil.clearHttpClient();
        if (server != null) {
            server.stop(0);
        }
        if (testDirectory != null) {
            try (var paths = Files.walk(testDirectory)) {
                paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    }

    @Test
    public void rejectsSelfSignedServerCertificateAbsentFromConfiguredTruststore() throws Exception {
        Path attackerKeyStore = generateKeyStore("attacker", "localhost", true);
        Path unrelatedTrustStore = generateKeyStore("trusted", "unrelated.invalid", false);
        server = startServer(attackerKeyStore);

        HttpUtil.setTruststore(unrelatedTrustStore.toFile(), PASSWORD);
        try (InputStream response = HttpUtil.doGet(
                "https://localhost:" + server.getAddress().getPort() + "/", "text/plain", null)) {
            fail("untrusted self-signed server certificate should have been rejected");
        } catch (RuntimeException e) {
            // doGet wraps IOException; verify the root cause is an SSL handshake failure
            assertThat("expected SSLHandshakeException but got: " + e.getCause(), e.getCause(), instanceOf(SSLHandshakeException.class));
        }
    }

    @Test
    public void acceptsSelfSignedServerCertificatePresentInConfiguredTruststore() throws Exception {
        Path serverKeyStore = generateKeyStore("server", "localhost", true);
        server = startServer(serverKeyStore);

        HttpUtil.setTruststore(serverKeyStore.toFile(), PASSWORD);
        try (InputStream response = HttpUtil.doGet(
                "https://localhost:" + server.getAddress().getPort() + "/", "text/plain", null)) {
            assertThat("trusted server response was not read",
                    new String(response.readAllBytes(), StandardCharsets.UTF_8), equalTo("server response"));
        }
    }

    @Test
    public void acceptsCaSignedServerCertificateWhenOnlyCaIsInTruststore() throws Exception {
        // Generate CA keypair (self-signed)
        Path caKeyStore = testDirectory.resolve("ca.p12");
        runKeytool("-genkeypair", "-alias", "ca", "-keyalg", "RSA",
                "-dname", "CN=Test CA", "-validity", "30", "-storetype", "PKCS12",
                "-keystore", caKeyStore.toString(), "-storepass", PASSWORD, "-keypass", PASSWORD,
                "-ext", "BasicConstraints=ca:true,pathlen:0", "-noprompt");

        // Generate server keypair
        Path serverKeyStore = testDirectory.resolve("ca-server.p12");
        runKeytool("-genkeypair", "-alias", "server", "-keyalg", "RSA",
                "-dname", "CN=localhost", "-validity", "30", "-storetype", "PKCS12",
                "-keystore", serverKeyStore.toString(), "-storepass", PASSWORD, "-keypass", PASSWORD,
                "-noprompt");

        // Generate CSR from server
        Path csr = testDirectory.resolve("server.csr");
        runKeytool("-certreq", "-alias", "server",
                "-keystore", serverKeyStore.toString(), "-storepass", PASSWORD,
                "-file", csr.toString(),
                "-ext", "SAN=dns:localhost");

        // CA signs the CSR → signed cert
        Path signedCert = testDirectory.resolve("server-signed.crt");
        runKeytool("-gencert", "-alias", "ca",
                "-keystore", caKeyStore.toString(), "-storepass", PASSWORD,
                "-infile", csr.toString(), "-outfile", signedCert.toString(),
                "-validity", "30", "-rfc",
                "-ext", "SAN=dns:localhost");

        // Export CA cert
        Path caCert = testDirectory.resolve("ca.crt");
        runKeytool("-exportcert", "-alias", "ca",
                "-keystore", caKeyStore.toString(), "-storepass", PASSWORD,
                "-file", caCert.toString(), "-rfc");

        // Import CA cert then signed server cert into server keystore to build a chain
        runKeytool("-importcert", "-alias", "ca", "-trustcacerts",
                "-keystore", serverKeyStore.toString(), "-storepass", PASSWORD,
                "-file", caCert.toString(), "-noprompt");
        runKeytool("-importcert", "-alias", "server",
                "-keystore", serverKeyStore.toString(), "-storepass", PASSWORD,
                "-file", signedCert.toString(), "-noprompt");

        // Truststore contains only the CA cert — not the server cert directly
        Path trustStore = testDirectory.resolve("ca-truststore.p12");
        runKeytool("-importcert", "-alias", "ca",
                "-keystore", trustStore.toString(), "-storepass", PASSWORD,
                "-file", caCert.toString(), "-noprompt");

        server = startServer(serverKeyStore);

        HttpUtil.setTruststore(trustStore.toFile(), PASSWORD);
        try (InputStream response = HttpUtil.doGet(
                "https://localhost:" + server.getAddress().getPort() + "/", "text/plain", null)) {
            assertThat("CA-signed server cert was not accepted when CA is in truststore",
                    new String(response.readAllBytes(), StandardCharsets.UTF_8), equalTo("server response"));
        }
    }

    @Test
    public void explicitInsecureModeStillAcceptsSelfSignedServerCertificate() throws Exception {
        Path serverKeyStore = generateKeyStore("insecure-server", "localhost", true);
        server = startServer(serverKeyStore);

        HttpUtil.setSkipCertificateValidation();
        try (InputStream response = HttpUtil.doGet(
                "https://localhost:" + server.getAddress().getPort() + "/", "text/plain", null)) {
            assertThat("explicit insecure mode did not accept the self-signed server",
                    new String(response.readAllBytes(), StandardCharsets.UTF_8), equalTo("server response"));
        }
    }

    private Path generateKeyStore(String alias, String commonName, boolean localhostSan) throws Exception {
        Path keyStore = testDirectory.resolve(alias + ".p12");
        if (localhostSan) {
            runKeytool("-genkeypair", "-alias", alias, "-keyalg", "RSA",
                    "-dname", "CN=" + commonName, "-validity", "30", "-storetype", "PKCS12",
                    "-keystore", keyStore.toString(), "-storepass", PASSWORD, "-keypass", PASSWORD,
                    "-ext", "SAN=dns:localhost", "-noprompt");
        } else {
            runKeytool("-genkeypair", "-alias", alias, "-keyalg", "RSA",
                    "-dname", "CN=" + commonName, "-validity", "30", "-storetype", "PKCS12",
                    "-keystore", keyStore.toString(), "-storepass", PASSWORD, "-keypass", PASSWORD,
                    "-noprompt");
        }
        return keyStore;
    }

    private void runKeytool(String... args) throws Exception {
        String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        List<String> command = new java.util.ArrayList<>();
        command.add(keytool);
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat("keytool failed: " + output, process.waitFor(), equalTo(0));
    }

    private HttpsServer startServer(Path keyStorePath) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(keyStorePath)) {
            keyStore.load(input, PASSWORD.toCharArray());
        }
        KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagers.init(keyStore, PASSWORD.toCharArray());
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers.getKeyManagers(), null, null);

        HttpsServer httpsServer = HttpsServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(context));
        httpsServer.createContext("/", exchange -> {
            byte[] body = "server response".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        httpsServer.start();
        return httpsServer;
    }
}
