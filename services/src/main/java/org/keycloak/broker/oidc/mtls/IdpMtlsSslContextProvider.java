package org.keycloak.broker.oidc.mtls;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;

import org.keycloak.crypto.KeyWrapper;

/**
 * Builds the TLS key material that presents the IdP's client certificate (from a realm key) for
 * tls_client_auth. Callers can obtain either the raw {@link KeyManager}s (so they survive an
 * outbound HTTP client configured with {@code disable-trust-manager}) or a fully built
 * {@link SSLContext}. Trust material is supplied by the caller (the global Keycloak truststore);
 * null means the JVM default trust managers are used.
 */
public final class IdpMtlsSslContextProvider {

    private static final String ALIAS = "idp-client";

    private IdpMtlsSslContextProvider() {
    }

    /**
     * Builds the {@link KeyManager}s that present the IdP client certificate for mTLS.
     */
    public static KeyManager[] buildKeyManagers(KeyWrapper key) throws Exception {
        List<X509Certificate> chain = key.getCertificateChain();
        X509Certificate[] certs;
        if (chain != null && !chain.isEmpty()) {
            certs = chain.toArray(new X509Certificate[0]);
        } else if (key.getCertificate() != null) {
            certs = new X509Certificate[] { key.getCertificate() };
        } else {
            // Without an explicit check we would hand a null/empty chain to the key manager and the mTLS
            // handshake would silently present no certificate. This is a public utility, so fail loudly with a
            // clear message about the missing certificate material instead.
            throw new IllegalStateException(
                    "Realm key does not contain a certificate or certificate chain for mTLS client authentication.");
        }
        if (!(key.getPrivateKey() instanceof PrivateKey)) {
            throw new IllegalStateException(
                    "Realm key does not contain a usable private key for mTLS client authentication.");
        }
        // Present the private key + certificate chain directly via an X509ExtendedKeyManager rather than
        // round-tripping through a KeyStore. KeyStore#setKeyEntry requires an extractable/encodable key, which
        // rejects non-extractable PrivateKeys from a PKCS#11/HSM token (getEncoded()/getFormat() return null)
        // even though they are perfectly usable for a TLS handshake. The resolver already accepts such keys at
        // configuration time, so the runtime path must accept them too.
        return new KeyManager[] { new SingleIdentityKeyManager((PrivateKey) key.getPrivateKey(), certs) };
    }

    public static SSLContext buildSslContext(KeyWrapper key, TrustManager[] trustManagers) throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(buildKeyManagers(key), trustManagers, new SecureRandom());
        return ctx;
    }

    /**
     * An {@link X509ExtendedKeyManager} that always presents the one client identity it was built from,
     * regardless of the peer's requested key types or accepted issuers. There is exactly one IdP client
     * certificate, so selection is unconditional. Only the client-side callbacks return the identity; the
     * server-side callbacks return null because this is never a server identity.
     */
    private static final class SingleIdentityKeyManager extends X509ExtendedKeyManager {

        private final PrivateKey privateKey;
        private final X509Certificate[] chain;

        private SingleIdentityKeyManager(PrivateKey privateKey, X509Certificate[] chain) {
            this.privateKey = privateKey;
            this.chain = chain;
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
            // The X509ExtendedKeyManager default returns null here (it does not fall back to the Socket
            // variant), so an SSLEngine-based client would present no certificate without this override.
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
        public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
            return null;
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return ALIAS.equals(alias) ? chain : null;
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return ALIAS.equals(alias) ? privateKey : null;
        }
    }
}
