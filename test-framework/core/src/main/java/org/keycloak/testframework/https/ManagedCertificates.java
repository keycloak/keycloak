package org.keycloak.testframework.https;

import org.jboss.logging.Logger;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.crypto.def.DefaultCryptoProvider;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class ManagedCertificates {

    private static final Logger LOGGER = Logger.getLogger(ManagedCertificates.class);

    private final CryptoProvider cryptoProvider;

    private KeyStore keyStore;
    private X509Certificate certificate;
    private KeyPair keyPair;

    private final static Path KEYSTORE_FILE_PATH = Paths.get(System.getProperty("java.io.tmpdir"), "kc-server-testing.keystore");
    public final static char[] KEYSTORE_PASSWORD = "password".toCharArray();
    public final static char[] PRVKEY_PASSWORD = "password".toCharArray();

    public final static String CERT_ENTRY = "cert";
    public final static String PRV_KEY_ENTRY = "prvKey";


    public ManagedCertificates() throws ManagedCertificatesException {
        cryptoProvider = new DefaultCryptoProvider();
        if (!CryptoIntegration.isInitialised()) {
            CryptoIntegration.setProvider(cryptoProvider);
        }
        initKeyStore();
    }

    public Path getKeycloakServerKeyStorePath() {
        return KEYSTORE_FILE_PATH;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    private void initKeyStore() throws ManagedCertificatesException {
        try {
            keyStore = cryptoProvider.getKeyStore(KeystoreUtil.KeystoreFormat.JKS);

            if (Files.exists(KEYSTORE_FILE_PATH)) {
                loadKeyStoreContents();
            } else {
                generateKeyStoreContents();
            }
        } catch (Exception e) {
            throw new ManagedCertificatesException(e);
        }
    }

    private void loadKeyStoreContents() throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, IOException, CertificateException {
        LOGGER.debugv("Existing KeyStore file found in {0}", KEYSTORE_FILE_PATH);
        try (FileInputStream fis = new FileInputStream(KEYSTORE_FILE_PATH.toFile())) {
            keyStore.load(fis, KEYSTORE_PASSWORD);
            // if this is failing delete the existing keystore, or implement a fallback mechanism to generate a new one instead
        }

        certificate = (X509Certificate) keyStore.getCertificate(CERT_ENTRY);
        PublicKey pubKey = certificate.getPublicKey();
        PrivateKey prvKey = (PrivateKey) keyStore.getKey(PRV_KEY_ENTRY, PRVKEY_PASSWORD);
        keyPair = new KeyPair(pubKey, prvKey);
    }

    private void generateKeyStoreContents() throws NoSuchAlgorithmException, NoSuchProviderException, CertificateException, IOException, KeyStoreException, Exception {
        LOGGER.debugv("Generating KeyStore file in {0}", KEYSTORE_FILE_PATH);
        keyStore.load(null);
        generateKeyPair();
        createKeycloakServerCertificate();

        keyStore.setCertificateEntry(CERT_ENTRY, certificate);
        keyStore.setKeyEntry(PRV_KEY_ENTRY, keyPair.getPrivate(), PRVKEY_PASSWORD, new X509Certificate[]{certificate});

        // store the generated keystore in a temp folder
        try (FileOutputStream fos = new FileOutputStream(KEYSTORE_FILE_PATH.toFile())) {
            keyStore.store(fos, KEYSTORE_PASSWORD);
        }
    }

    private void generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        keyPair = cryptoProvider.getKeyPairGen("RSA").generateKeyPair();
    }

    private void createKeycloakServerCertificate() throws Exception {
        // generate a v1 certificate
        X509Certificate caCert = cryptoProvider.getCertificateUtils().generateV1SelfSignedCertificate(keyPair, "localhost");

        // generate a v3 certificate
        certificate = cryptoProvider.getCertificateUtils().generateV3Certificate(keyPair, keyPair.getPrivate(), caCert, "localhost");
    }
}
