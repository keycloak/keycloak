package org.keycloak.testframework.https;

import org.keycloak.common.util.KeystoreUtil;

public class CertificatesConfigBuilder {

    private KeystoreUtil.KeystoreFormat keystoreFormat = KeystoreUtil.KeystoreFormat.JKS;
    private boolean tlsEnabled = false;
    private boolean mTlsEnabled = false;

    public CertificatesConfigBuilder() {
    }

    /**
     * Use the specified keystore format
     *
     * @param keystoreFormat the keystore format to use
     * @return
     */
    public CertificatesConfigBuilder keystoreFormat(KeystoreUtil.KeystoreFormat keystoreFormat) {
        this.keystoreFormat = keystoreFormat;
        return this;
    }

    public KeystoreUtil.KeystoreFormat getKeystoreFormat() {
        return this.keystoreFormat;
    }

    /**
     * Enable TLS
     *
     * @param tlsEnabled <code>true</code> if tls should be enabled
     * @return
     */
    public CertificatesConfigBuilder tlsEnabled(boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
        return this;
    }

    public boolean isTlsEnabled() {
        return tlsEnabled || mTlsEnabled;
    }

    /**
     * Enable mTLS authentication between Keycloak and clients
     *
     * @param mTlsEnabled <code>true</code> if mTLS should be enabled
     * @return
     */
    public CertificatesConfigBuilder mTlsEnabled(boolean mTlsEnabled) {
        this.mTlsEnabled = mTlsEnabled;
        return this;
    }

    public boolean isMTlsEnabled() {
        return mTlsEnabled;
    }
}
