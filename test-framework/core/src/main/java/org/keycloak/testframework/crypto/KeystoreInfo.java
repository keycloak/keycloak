package org.keycloak.testframework.crypto;

import java.io.File;

import org.keycloak.representations.idm.CertificateRepresentation;

public class KeystoreInfo {
    private final CertificateRepresentation certificateInfo;
    private final File keystoreFile;

    KeystoreInfo(CertificateRepresentation certificateInfo, File keystoreFile) {
        this.certificateInfo = certificateInfo;
        this.keystoreFile = keystoreFile;
    }

    public CertificateRepresentation getCertificateInfo() {
        return certificateInfo;
    }

    public File getKeystoreFile() {
        return keystoreFile;
    }
}
