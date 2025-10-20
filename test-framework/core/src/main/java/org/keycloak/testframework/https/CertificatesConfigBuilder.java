package org.keycloak.testframework.https;

import org.keycloak.common.util.KeystoreUtil;

public class CertificatesConfigBuilder {

    private CryptoProviderOption cryptoProviderOption = CryptoProviderOption.DEFAULT;
    private KeystoreUtil.KeystoreFormat keystoreFormat = KeystoreUtil.KeystoreFormat.JKS;
    private String fileFormatSuffix = ".jks";
    private char[] keystorePassword = "password".toCharArray();
    private char[] truststorePassword = "password".toCharArray();

    public CertificatesConfigBuilder () {
    }

    public CertificatesConfigBuilder cryptoProviderOption(CryptoProviderOption option) {
        this.cryptoProviderOption = option;
        return this;
    }

    public CryptoProviderOption getCryptoProviderOption() {
        return this.cryptoProviderOption;
    }

    public CertificatesConfigBuilder keystoreFormat(KeystoreUtil.KeystoreFormat keystoreFormat) {
        this.keystoreFormat = keystoreFormat;
        fileFormatSuffix(keystoreFormat.toString().toLowerCase());
        return this;
    }

    public KeystoreUtil.KeystoreFormat getKeystoreFormat() {
        return this.keystoreFormat;
    }

    private void fileFormatSuffix(String fileFormatSuffix) {
        this.fileFormatSuffix = "." + fileFormatSuffix;
    }

    public String getFileFormatSuffix() {
        return this.fileFormatSuffix;
    }

    public CertificatesConfigBuilder keystorePassword(char[] keystorePassword) {
        this.keystorePassword = keystorePassword;
        return this;
    }

    public char[] getKeystorePassword() {
        return this.keystorePassword;
    }

    public CertificatesConfigBuilder truststorePassword(char[] truststorePassword) {
        this.truststorePassword = truststorePassword;
        return this;
    }

    public char[] getTruststorePassword() {
        return this.truststorePassword;
    }
}
