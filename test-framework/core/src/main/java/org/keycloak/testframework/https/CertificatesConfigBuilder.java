package org.keycloak.testframework.https;

import org.keycloak.common.util.KeystoreUtil;

public class CertificatesConfigBuilder {

    private KeystoreUtil.KeystoreFormat keystoreFormat = KeystoreUtil.KeystoreFormat.JKS;
    private boolean tlsEnabled = false;
    private boolean mTlsEnabled = false;

    public CertificatesConfigBuilder() {
    }

    public CertificatesConfigBuilder keystoreFormat(KeystoreUtil.KeystoreFormat keystoreFormat) {
        this.keystoreFormat = keystoreFormat;
        return this;
    }

    public KeystoreUtil.KeystoreFormat getKeystoreFormat() {
        return this.keystoreFormat;
    }

    public CertificatesConfigBuilder tlsEnabled(boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
        return this;
    }

    public boolean isTlsEnabled() {
        return tlsEnabled || mTlsEnabled;
    }

    public CertificatesConfigBuilder mTlsEnabled(boolean mTlsEnabled) {
        this.mTlsEnabled = mTlsEnabled;
        return this;
    }

    public boolean isMTlsEnabled() {
        return mTlsEnabled;
    }
}
