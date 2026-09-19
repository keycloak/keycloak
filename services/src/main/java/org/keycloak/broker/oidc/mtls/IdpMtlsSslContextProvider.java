package org.keycloak.broker.oidc.mtls;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.keycloak.crypto.KeyWrapper;

/**
 * Builds the TLS key material that presents the IdP's client certificate (from a realm key) for
 * tls_client_auth. Callers can obtain either the raw {@link KeyManager}s (so they survive an
 * outbound HTTP client configured with {@code disable-trust-manager}) or a fully built
 * {@link SSLContext}. Trust material is supplied by the caller (the global Keycloak truststore);
 * null means the JVM default trust managers are used.
 */
public final class IdpMtlsSslContextProvider {

    private static final char[] EMPTY = new char[0];

    private IdpMtlsSslContextProvider() {
    }

    /**
     * Builds the {@link KeyManager}s that present the IdP client certificate for mTLS.
     */
    public static KeyManager[] buildKeyManagers(KeyWrapper key) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);

        List<X509Certificate> chain = key.getCertificateChain();
        X509Certificate[] certs;
        if (chain != null && !chain.isEmpty()) {
            certs = chain.toArray(new X509Certificate[0]);
        } else {
            certs = new X509Certificate[] { key.getCertificate() };
        }
        if (!(key.getPrivateKey() instanceof PrivateKey)) {
            throw new IllegalStateException(
                    "Realm key does not contain a usable private key for mTLS client authentication.");
        }
        ks.setKeyEntry("idp-client", (PrivateKey) key.getPrivateKey(), EMPTY, certs);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, EMPTY);
        return kmf.getKeyManagers();
    }

    public static SSLContext buildSslContext(KeyWrapper key, TrustManager[] trustManagers) throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(buildKeyManagers(key), trustManagers, new SecureRandom());
        return ctx;
    }
}
