package org.keycloak.connections.httpclient;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.CertificateUtils;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class HttpClientBuilderTest {

    @Test
    public void testDefaultBuilder() throws NoSuchFieldException, IllegalAccessException {
        CloseableHttpClient httpClient = new HttpClientBuilder().build();

        RequestConfig requestConfig = getRequestConfig(httpClient);

        Assert.assertEquals("Default socket timeout is -1 and can be converted by TimeUnit", -1, requestConfig.getSocketTimeout());
        Assert.assertEquals("Default connect timeout is -1 and can be converted by TimeUnit", -1, requestConfig.getConnectTimeout());
        Assert.assertEquals("Default connection request timeout is " + HttpClientBuilder.DEFAULT_CONNECTION_REQUEST_TIMEOUT_MILLIS, HttpClientBuilder.DEFAULT_CONNECTION_REQUEST_TIMEOUT_MILLIS, requestConfig.getConnectionRequestTimeout());
    }

    @Test
    public void testRepeatedApacheBuilderUse() throws Exception {
        HttpClientBuilder httpClientBuilder = new HttpClientBuilder();
        httpClientBuilder.getApacheHttpClientBuilder().setRetryHandler(new HttpRequestRetryHandler() {

            @Override
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                return true;
            }
        });
        var builder = httpClientBuilder.getApacheHttpClientBuilder(); // should not clear the retry handler

        Field retryHandler = builder.getClass().getDeclaredField("retryHandler");
        retryHandler.setAccessible(true);
        assertNotNull(retryHandler.get(builder));
    }

    @Test
    public void testTimeUnitSeconds() throws NoSuchFieldException, IllegalAccessException {
        HttpClientBuilder httpClientBuilder = new HttpClientBuilder();
        httpClientBuilder
                .socketTimeout(2, TimeUnit.SECONDS)
                .establishConnectionTimeout(1, TimeUnit.SECONDS)
                .connectionRequestTimeout(3, TimeUnit.SECONDS);
        CloseableHttpClient httpClient = httpClientBuilder.build();

        RequestConfig requestConfig = getRequestConfig(httpClient);

        Assert.assertEquals("Socket timeout is converted to milliseconds", 2000, requestConfig.getSocketTimeout());
        Assert.assertEquals("Connect timeout is converted to milliseconds", 1000, requestConfig.getConnectTimeout());
        Assert.assertEquals("Connection request timeout is converted to milliseconds", 3000, requestConfig.getConnectionRequestTimeout());
    }

    @Test
    public void testTimeUnitMilliSeconds() throws NoSuchFieldException, IllegalAccessException {
        HttpClientBuilder httpClientBuilder = new HttpClientBuilder();
        httpClientBuilder
                .socketTimeout(2000, TimeUnit.MILLISECONDS)
                .establishConnectionTimeout(1000, TimeUnit.MILLISECONDS);
        CloseableHttpClient httpClient = httpClientBuilder.build();

        RequestConfig requestConfig = getRequestConfig(httpClient);

        Assert.assertEquals("Socket timeout is still in milliseconds", 2000, requestConfig.getSocketTimeout());
        Assert.assertEquals("Connect timeout is still in milliseconds", 1000, requestConfig.getConnectTimeout());
    }

    private static RequestConfig getRequestConfig(CloseableHttpClient httpClient) throws NoSuchFieldException, IllegalAccessException {
        Field defaultConfig = httpClient.getClass().getDeclaredField("defaultConfig");
        defaultConfig.setAccessible(true);
        return (RequestConfig) defaultConfig.get(httpClient);
    }

    /**
     * Regression test: when {@code disable-trust-manager} is set, the custom client TLS key material for
     * tls_client_auth (mTLS) must still be presented. Previously the builder created a fresh SSLContext
     * with no key managers in that case, silently dropping the certificate.
     */
    @Test
    public void keyManagersSurviveDisableTrustManager() throws Exception {
        KeyManager[] keyManagers = { new NoopKeyManager() };

        HttpClientBuilder builder = new HttpClientBuilder()
                .keyManagers(keyManagers)
                .disableTrustManager();

        // The key material must not be dropped when trust management is disabled.
        assertSame("Client key managers must survive disable-trust-manager",
                keyManagers, builder.effectiveKeyManagers());

        // And the resulting context must be built successfully with that key material in place.
        SSLContext ctx = builder.resolveSslContext();
        assertNotNull("A usable SSLContext must be produced with disable-trust-manager and mTLS key material", ctx);
        assertNotNull(ctx.getSocketFactory());
    }

    /**
     * Sanity check that key managers are also honored on the normal (trust-manager-enabled) path.
     */
    @Test
    public void keyManagersAreHonoredOnDefaultPath() throws Exception {
        KeyManager[] keyManagers = { new NoopKeyManager() };

        HttpClientBuilder builder = new HttpClientBuilder().keyManagers(keyManagers);

        assertSame(keyManagers, builder.effectiveKeyManagers());

        SSLContext ctx = builder.resolveSslContext();
        assertNotNull(ctx);
        assertNotNull(ctx.getSocketFactory());
    }

    /**
     * End-to-end handshake through {@link HttpClientBuilder#resolveSslContext()} itself: with
     * {@code disable-trust-manager} enabled and mTLS key material configured, the context this method returns
     * must actually present the client certificate to a server requiring client auth. The getter-only tests
     * above ({@link #keyManagersSurviveDisableTrustManager()}) would still pass if {@code resolveSslContext()}
     * regressed to {@code init(null, ...)}; this test would not, because no certificate would be presented.
     */
    @Test
    public void resolveSslContextPresentsKeyManagersOnHandshakeWithDisableTrustManager() throws Exception {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());

        KeyPair serverKp = generateKeyPair();
        X509Certificate serverCert = CertificateUtils.generateV1SelfSignedCertificate(serverKp, "CN=localhost");
        KeyPair clientKp = generateKeyPair();
        X509Certificate clientCert = CertificateUtils.generateV1SelfSignedCertificate(clientKp, "CN=idp-client-builder");

        SSLServerSocket serverSocket = startClientAuthServer(serverKp, serverCert);
        int port = serverSocket.getLocalPort();
        BlockingQueue<Object> presented = new ArrayBlockingQueue<>(1);

        Thread server = new Thread(() -> {
            try (SSLSocket accepted = (SSLSocket) serverSocket.accept()) {
                accepted.startHandshake();
                presented.offer(accepted.getSession().getPeerCertificates());
                //noinspection ResultOfMethodCallIgnored
                accepted.getInputStream().read();
            } catch (Exception e) {
                presented.offer(e);
            }
        });
        server.setDaemon(true);
        server.start();

        try {
            // The SSLContext under test comes from the production method, not from a directly-initialized context.
            KeyManager[] keyManagers = { new SingleCertKeyManager(clientKp.getPrivate(), clientCert) };
            SSLContext clientCtx = new HttpClientBuilder()
                    .keyManagers(keyManagers)
                    .disableTrustManager()
                    .resolveSslContext();

            try (SSLSocket clientSocket = (SSLSocket) clientCtx.getSocketFactory()
                    .createSocket(InetAddress.getLoopbackAddress(), port)) {
                clientSocket.startHandshake();
                clientSocket.getOutputStream().write(1);
                clientSocket.getOutputStream().flush();
            }

            Object result = presented.poll(15, TimeUnit.SECONDS);
            assertNotNull("Server did not complete the mTLS handshake in time", result);
            if (result instanceof Exception) {
                throw new AssertionError("Server-side handshake failed", (Exception) result);
            }
            Certificate[] peerChain = (Certificate[]) result;
            assertNotNull("Server received no client certificate from resolveSslContext()", peerChain);
            Assert.assertTrue("Server received an empty client certificate chain", peerChain.length >= 1);
            assertArrayEquals("resolveSslContext() must present the configured client certificate",
                    clientCert.getEncoded(), ((X509Certificate) peerChain[0]).getEncoded());
        } finally {
            serverSocket.close();
        }
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }

    private static SSLServerSocket startClientAuthServer(KeyPair serverKp, X509Certificate serverCert) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setKeyEntry("server", serverKp.getPrivate(), new char[0], new X509Certificate[] { serverCert });
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, new char[0]);
        SSLContext serverCtx = SSLContext.getInstance("TLS");
        serverCtx.init(kmf.getKeyManagers(), new TrustManager[] { ALL_TRUSTING }, new SecureRandom());
        SSLServerSocket serverSocket = (SSLServerSocket) serverCtx.getServerSocketFactory()
                .createServerSocket(0, 1, InetAddress.getLoopbackAddress());
        serverSocket.setNeedClientAuth(true);
        return serverSocket;
    }

    private static final X509TrustManager ALL_TRUSTING = new X509TrustManager() {
        @Override public void checkClientTrusted(X509Certificate[] chain, String authType) { }
        @Override public void checkServerTrusted(X509Certificate[] chain, String authType) { }
        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    };

    /**
     * X509KeyManager that always presents a single fixed private key + certificate, so the handshake test does
     * not depend on alias-selection heuristics.
     */
    private static class SingleCertKeyManager implements X509KeyManager {
        private final PrivateKey privateKey;
        private final X509Certificate certificate;

        SingleCertKeyManager(PrivateKey privateKey, X509Certificate certificate) {
            this.privateKey = privateKey;
            this.certificate = certificate;
        }

        @Override public String[] getClientAliases(String keyType, Principal[] issuers) { return new String[] { "client" }; }
        @Override public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) { return "client"; }
        @Override public String[] getServerAliases(String keyType, Principal[] issuers) { return new String[0]; }
        @Override public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) { return null; }
        @Override public X509Certificate[] getCertificateChain(String alias) { return new X509Certificate[] { certificate }; }
        @Override public PrivateKey getPrivateKey(String alias) { return privateKey; }
    }

    /**
     * Minimal X509KeyManager placeholder used to verify the builder retains configured key material.
     */
    private static class NoopKeyManager implements X509KeyManager {
        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return new String[0];
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            return null;
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return new String[0];
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            return null;
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return new X509Certificate[0];
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return null;
        }
    }

}
