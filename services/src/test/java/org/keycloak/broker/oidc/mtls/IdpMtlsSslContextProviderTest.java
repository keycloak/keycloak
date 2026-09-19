package org.keycloak.broker.oidc.mtls;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.crypto.KeyWrapper;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class IdpMtlsSslContextProviderTest {

    @BeforeClass
    public static void initCrypto() {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
    }

    private static KeyWrapper keyWithCert(String cn) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        X509Certificate cert = CertificateUtils.generateV1SelfSignedCertificate(kp, cn);
        KeyWrapper key = new KeyWrapper();
        key.setPrivateKey(kp.getPrivate());
        key.setPublicKey(kp.getPublic());
        key.setCertificate(cert);
        key.setCertificateChain(List.<X509Certificate>of(cert));
        return key;
    }

    @Test
    public void buildsSslContextFromKeyWrapperWithChain() throws Exception {
        KeyWrapper key = keyWithCert("CN=idp-client");
        SSLContext ctx = IdpMtlsSslContextProvider.buildSslContext(key, null);
        assertNotNull(ctx);
        assertNotNull(ctx.getSocketFactory());
    }

    @Test
    public void buildsSslContextFromKeyWrapperWithSingleCert() throws Exception {
        KeyWrapper key = keyWithCert("CN=idp-client-2");
        key.setCertificateChain(null); // force the single-cert fallback path
        SSLContext ctx = IdpMtlsSslContextProvider.buildSslContext(key, null);
        assertNotNull(ctx);
    }

    @Test
    public void rejectsNonPrivateKey() throws Exception {
        // Build a KeyWrapper whose privateKey field holds a plain Key (not a PrivateKey).
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        X509Certificate cert = CertificateUtils.generateV1SelfSignedCertificate(kp, "CN=bad");

        // An anonymous Key implementation that is NOT a PrivateKey.
        Key nonPrivate = new Key() {
            public String getAlgorithm() { return "RAW"; }
            public String getFormat() { return "RAW"; }
            public byte[] getEncoded() { return new byte[0]; }
        };

        KeyWrapper key = new KeyWrapper();
        key.setPrivateKey(nonPrivate);
        key.setCertificate(cert);

        assertThrows(IllegalStateException.class,
                () -> IdpMtlsSslContextProvider.buildSslContext(key, null));
    }

    /**
     * Full end-to-end handshake: a local HTTPS server that <em>requires</em> client authentication
     * must actually receive the client certificate that {@link IdpMtlsSslContextProvider#buildKeyManagers}
     * selects and presents. This exercises the runtime path where key material is supplied as
     * {@link KeyManager}s rather than a pre-built {@link SSLContext}, and pairs it with an all-trusting
     * trust manager to reproduce the {@code disable-trust-manager} client configuration (no truststore,
     * hostname/cert validation relaxed) while still enforcing mTLS on the server side.
     */
    @Test
    public void presentsSelectedClientCertificateOnHandshake() throws Exception {
        // Server identity (separate from the client key material).
        KeyWrapper serverKey = keyWithCert("CN=localhost");
        // Client key material selected/presented by the provider under test.
        KeyWrapper clientKey = keyWithCert("CN=idp-client-handshake");

        SSLServerSocket serverSocket = startClientAuthServer(serverKey);
        int port = serverSocket.getLocalPort();
        BlockingQueue<Object> presented = new ArrayBlockingQueue<>(1);

        Thread server = new Thread(() -> {
            try (SSLSocket accepted = (SSLSocket) serverSocket.accept()) {
                // Force the handshake and read the client certificate the server received.
                accepted.startHandshake();
                Certificate[] peer = accepted.getSession().getPeerCertificates();
                presented.offer(peer);
                // Drain the single byte the client writes so the handshake fully settles.
                InputStream in = accepted.getInputStream();
                //noinspection ResultOfMethodCallIgnored
                in.read();
            } catch (Exception e) {
                presented.offer(e);
            }
        });
        server.setDaemon(true);
        server.start();

        try {
            // Client side: real provider-built key managers + all-trusting trust manager (disable-trust-manager path).
            KeyManager[] keyManagers = IdpMtlsSslContextProvider.buildKeyManagers(clientKey);
            SSLContext clientCtx = SSLContext.getInstance("TLS");
            clientCtx.init(keyManagers, new TrustManager[] { ALL_TRUSTING }, new SecureRandom());

            try (SSLSocket clientSocket =
                         (SSLSocket) clientCtx.getSocketFactory()
                                 .createSocket(InetAddress.getLoopbackAddress(), port)) {
                clientSocket.startHandshake();
                OutputStream out = clientSocket.getOutputStream();
                out.write(1);
                out.flush();
            }

            Object result = presented.poll(15, TimeUnit.SECONDS);
            assertNotNull("Server did not complete the mTLS handshake in time", result);
            if (result instanceof Exception) {
                throw new AssertionError("Server-side handshake failed", (Exception) result);
            }

            Certificate[] peerChain = (Certificate[]) result;
            assertNotNull("Server received no client certificate", peerChain);
            assertTrue("Server received an empty client certificate chain", peerChain.length >= 1);
            X509Certificate presentedCert = (X509Certificate) peerChain[0];
            // The certificate presented must be exactly the one carried by the client KeyWrapper.
            assertEquals(clientKey.getCertificate().getSubjectX500Principal(),
                    presentedCert.getSubjectX500Principal());
            assertArrayEquals(clientKey.getCertificate().getEncoded(), presentedCert.getEncoded());
        } finally {
            serverSocket.close();
        }
    }

    private static SSLServerSocket startClientAuthServer(KeyWrapper serverKey) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setKeyEntry("server", serverKey.getPrivateKey(), new char[0],
                new X509Certificate[] { serverKey.getCertificate() });

        javax.net.ssl.KeyManagerFactory kmf =
                javax.net.ssl.KeyManagerFactory.getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, new char[0]);

        SSLContext serverCtx = SSLContext.getInstance("TLS");
        // Trust any client certificate: the assertion is that a cert is presented and matches, not that a CA validates it.
        serverCtx.init(kmf.getKeyManagers(), new TrustManager[] { ALL_TRUSTING }, new SecureRandom());

        SSLServerSocketFactory factory = serverCtx.getServerSocketFactory();
        SSLServerSocket serverSocket =
                (SSLServerSocket) factory.createServerSocket(0, 1, InetAddress.getLoopbackAddress());
        serverSocket.setNeedClientAuth(true); // require mTLS
        return serverSocket;
    }

    private static final X509TrustManager ALL_TRUSTING = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };
}
