package org.keycloak.broker.oidc.mtls;

import org.keycloak.crypto.KeyWrapper;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Builds an SSLContext that presents the IdP's client certificate (from a realm key) as the
 * TLS key material for tls_client_auth. Trust material is supplied by the caller (the global
 * Keycloak truststore); null means the JVM default trust managers are used.
 */
public final class IdpMtlsSslContextProvider {

    private static final char[] EMPTY = new char[0];

    private IdpMtlsSslContextProvider() {
    }

    public static SSLContext buildSslContext(KeyWrapper key, TrustManager[] trustManagers) throws Exception {
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

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), trustManagers, new SecureRandom());
        return ctx;
    }
}
