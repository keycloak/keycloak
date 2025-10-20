package org.keycloak.tests.utils.admin;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.representations.idm.CertificateRepresentation;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class GenerateKeystoreForTestUtil {

    private final static Path KEYSTORES_DIR = Path.of(System.getProperty("java.io.tmpdir"));

    public static KeystoreInfo generateKeystore(org.keycloak.common.util.KeystoreUtil.KeystoreFormat keystoreType, String subject, String keystorePassword, String keyPassword) throws Exception {
        return generateKeystore(keystoreType, subject, keystorePassword, keyPassword, KeyUtils.generateRsaKeyPair(2048));
    }

    public static KeystoreInfo generateKeystore(org.keycloak.common.util.KeystoreUtil.KeystoreFormat keystoreType, String subject, String keystorePassword, String keyPassword, KeyPair keyPair) throws Exception {
        X509Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, subject);
        return generateKeystore(keystoreType, subject, keystorePassword, keyPassword, keyPair.getPrivate(), certificate);
    }

    public static KeystoreInfo generateKeystore(org.keycloak.common.util.KeystoreUtil.KeystoreFormat keystoreType,
                                                String subject, String keystorePassword, String keyPassword, PrivateKey privKey, Certificate certificate) throws Exception {
        String fileName = "keystore." + keystoreType.getPrimaryExtension();

        KeyStore keyStore = CryptoIntegration.getProvider().getKeyStore(keystoreType);
        keyStore.load(null, null);
        Certificate[] chain = {certificate};
        keyStore.setKeyEntry(subject, privKey, keyPassword.trim().toCharArray(), chain);

        File file = KEYSTORES_DIR.resolve(fileName).toFile();
        keyStore.store(new FileOutputStream(file), keystorePassword.trim().toCharArray());

        CertificateRepresentation certRep = new CertificateRepresentation();
        certRep.setPrivateKey(PemUtils.encodeKey(privKey));
        certRep.setPublicKey(PemUtils.encodeKey(certificate.getPublicKey()));
        certRep.setCertificate(PemUtils.encodeCertificate(certificate));
        return new KeystoreInfo(certRep, file);
    }

    public static class KeystoreInfo {
        private final CertificateRepresentation certificateInfo;
        private final File keystoreFile;

        private KeystoreInfo(CertificateRepresentation certificateInfo, File keystoreFile) {
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
}
