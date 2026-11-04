package org.keycloak.authentication.authenticators.x509;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.crypto.KeyType;
import org.keycloak.rule.CryptoInitRule;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * The size limit and the timeouts for plain LDAP connections are covered by the integration tests in
 * {@code org.keycloak.tests.x509}, this test covers what cannot be easily reproduced there.
 */
public class CrlLdapSocketFactoryTest {

    private static final int TIMEOUT = 5_000;

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    private static final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "ldap-test-server");
        thread.setDaemon(true);
        return thread;
    });

    @AfterClass
    public static void shutdownExecutor() {
        executor.shutdownNow();
    }

    @Test
    public void bindingIsLimitedToTheCurrentThreadAndOperation() throws Exception {
        CrlLdapSocketFactory factory = new CrlLdapSocketFactory(1000, TIMEOUT, TIMEOUT, null);
        try (CrlLdapSocketFactory.Binding binding = factory.bind()) {
            Assert.assertSame(factory, CrlLdapSocketFactory.getDefault());
            // other threads never get the factory, so they cannot create sockets with these or without limits
            ExecutionException e = Assert.assertThrows(ExecutionException.class,
                    () -> executor.submit(CrlLdapSocketFactory::getDefault).get());
            Assert.assertTrue(e.getCause() instanceof IllegalStateException);
        }
        // the binding does not outlive the operation, for example on a pooled request thread
        Assert.assertThrows(IllegalStateException.class, CrlLdapSocketFactory::getDefault);
    }

    @Test
    public void limitsBytesReadFromTlsConnection() throws Exception {
        SSLContext sslContext = selfSignedSslContext();
        try (ServerSocket server = sslContext.getServerSocketFactory().createServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            respondWith(server, new byte[20_000]);
            // the limit applies to the bytes on the wire, so the TLS handshake counts as well
            CrlLdapSocketFactory factory = new CrlLdapSocketFactory(8000, TIMEOUT, TIMEOUT, sslContext.getSocketFactory());
            try (Socket socket = factory.createSocket(server.getInetAddress().getHostAddress(), server.getLocalPort())) {
                Assert.assertTrue(socket instanceof SSLSocket);
                int[] bytesRead = new int[1];
                Assert.assertThrows(IOException.class, () -> readAll(socket.getInputStream(), bytesRead));
                Assert.assertTrue("Read " + bytesRead[0] + " bytes", bytesRead[0] < 20_000);
                // the reason is recorded, because JNDI would only report that the connection was closed
                Assert.assertTrue(String.valueOf(factory.getFailure()), factory.getFailure().getMessage().contains("max bytes to be consumed being 8000"));
            }
        }
    }

    private static void respondWith(ServerSocket server, byte[] data) {
        executor.submit(() -> {
            try (Socket socket = server.accept()) {
                socket.getOutputStream().write(data);
                socket.getOutputStream().flush();
            } catch (IOException e) {
                // the client aborted the connection
            }
            return null;
        });
    }

    private static void readAll(InputStream in, int[] bytesRead) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            bytesRead[0] += read;
        }
    }

    private static SSLContext selfSignedSslContext() throws Exception {
        KeyPairGenerator kpg = CryptoIntegration.getProvider().getKeyPairGen(KeyType.RSA);
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();
        X509Certificate certificate = CryptoIntegration.getProvider().getCertificateUtils()
                .createServicesTestCertificate("CN=localhost", new Date(), new Date(System.currentTimeMillis() + 60_000L), keyPair);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("key", keyPair.getPrivate(), new char[0], new X509Certificate[] { certificate });
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, new char[0]);

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("cert", certificate);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        return sslContext;
    }
}
