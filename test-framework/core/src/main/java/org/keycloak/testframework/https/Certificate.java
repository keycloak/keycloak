package org.keycloak.testframework.https;

import java.nio.file.Path;
import java.security.KeyStore;

public class Certificate {

    private final Path keystorePath;
    private final Path truststorePath;

    private KeyStore keystore;
    private KeyStore truststore;
    private String keyEntry;
    private String certificateEntry;

    public Certificate(Path keystorePath, Path truststorePath) {
        this.keystorePath = keystorePath;
        this.truststorePath = truststorePath;
    }

    public Path getKeystorePath() {
        return keystorePath;
    }

    public Path getTruststorePath() {
        return truststorePath;
    }

    public void setKeystore(KeyStore keystore) {
        this.keystore = keystore;
    }

    public KeyStore getKeystore() {
        return keystore;
    }

    public void setTruststore(KeyStore truststore) {
        this.truststore = truststore;
    }

    public KeyStore getTruststore() {
        return truststore;
    }

    public void setKeyEntry(String keyEntry) {
        this.keyEntry = keyEntry;
    }

    public String getKeyEntry() {
        return keyEntry;
    }

    public void setCertificateEntry(String certificateEntry) {
        this.certificateEntry = certificateEntry;
    }

    public String getCertificateEntry() {
        return certificateEntry;
    }
}
