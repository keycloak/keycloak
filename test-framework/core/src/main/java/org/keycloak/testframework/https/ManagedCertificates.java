package org.keycloak.testframework.https;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.crypto.def.DefaultCryptoProvider;

import org.apache.http.ssl.SSLContextBuilder;
import org.jboss.logging.Logger;

public class ManagedCertificates {

    private static final Logger LOGGER = Logger.getLogger(ManagedCertificates.class);

    private final CryptoProvider cryptoProvider;

    private KeyStore serverKeyStore;
    private KeyStore clientsTrustStore;
    private final Path serverKeystorePath;
    private final Path clientsTruststorePath;
    private final char[] password;

    private final static Path KEYSTORES_DIR = Path.of(System.getProperty("java.io.tmpdir"));
    private final static String PRV_KEY_ENTRY = "prvKey";
    public final static String CERT_ENTRY = "cert";


    public ManagedCertificates(CertificatesConfigBuilder configBuilder) throws ManagedCertificatesException {
        if (!CryptoIntegration.isInitialised()) {
            CryptoIntegration.setProvider(new DefaultCryptoProvider());
        }
        cryptoProvider = CryptoIntegration.getProvider();

        serverKeystorePath = KEYSTORES_DIR.resolve("kc-testing-server-keystore" + "." + configBuilder.getKeystoreFormat().getPrimaryExtension());
        clientsTruststorePath = KEYSTORES_DIR.resolve("kc-testing-clients-truststore" + "." + configBuilder.getKeystoreFormat().getPrimaryExtension());

        password = configBuilder.getKeystoreFormat() == KeystoreUtil.KeystoreFormat.JKS ? "password".toCharArray() : "passwordpassword".toCharArray();

        initServerCerts(configBuilder.getKeystoreFormat());
    }

    public String getKeycloakServerKeyStorePath() {
        return serverKeystorePath.toString();
    }

    public String getKeycloakServerKeyStorePassword() {
        return String.valueOf(password);
    }

    public KeyStore getClientTrustStore() {
        return clientsTrustStore;
    }

    public X509Certificate getKeycloakServerCertificate() {
        try {
            return (X509Certificate) serverKeyStore.getCertificate(CERT_ENTRY);
        } catch (KeyStoreException e) {
            throw new ManagedCertificatesException(e);
        }
    }

    public SSLContext getClientSSLContext() {
        try {
            return SSLContextBuilder.create()
                    .loadTrustMaterial(clientsTrustStore, null)
                    .build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new ManagedCertificatesException(e);
        }
    }

    private void initServerCerts(KeystoreUtil.KeystoreFormat keystoreFormat) throws ManagedCertificatesException {
        try {
            serverKeyStore = cryptoProvider.getKeyStore(keystoreFormat);
            clientsTrustStore = cryptoProvider.getKeyStore(keystoreFormat);

            if (Files.exists(serverKeystorePath) && Files.exists(clientsTruststorePath)) {
                LOGGER.debugv("Existing Server KeyStore files found in {0}", KEYSTORES_DIR);

                loadKeyStore(serverKeyStore, serverKeystorePath, password);
                loadKeyStore(clientsTrustStore, clientsTruststorePath, password);
            } else {
                LOGGER.debugv("Generating Server KeyStore files in {0}", KEYSTORES_DIR);

                generateKeystoreAndTruststore(serverKeyStore, clientsTrustStore, "localhost");
                // store the generated keystore and truststore in a temp folder
                try (FileOutputStream fos = new FileOutputStream(serverKeystorePath.toFile())) {
                    serverKeyStore.store(fos, password);
                }
                try (FileOutputStream fos = new FileOutputStream(clientsTruststorePath.toFile())) {
                    clientsTrustStore.store(fos, password);
                }
            }
        } catch (Exception e) {
            throw new ManagedCertificatesException(e);
        }
    }

    private void loadKeyStore(KeyStore keyStore, Path keyStorePath, char[] keyStorePasswd) throws NoSuchAlgorithmException, IOException, CertificateException {
        try (FileInputStream fis = new FileInputStream(keyStorePath.toFile())) {
            keyStore.load(fis, keyStorePasswd);
        }
    }

    private void generateKeystoreAndTruststore(KeyStore keyStore, KeyStore trustStore, String subject) throws NoSuchAlgorithmException, NoSuchProviderException, CertificateException, IOException, KeyStoreException, Exception {
        keyStore.load(null);
        trustStore.load(null);

        KeyPair keyPair = generateKeyPair();
        X509Certificate cert = generateX509CertificateCertificate(keyPair, subject);

        keyStore.setCertificateEntry(CERT_ENTRY, cert);
        trustStore.setCertificateEntry(CERT_ENTRY, cert);
        keyStore.setKeyEntry(PRV_KEY_ENTRY, keyPair.getPrivate(), password, new X509Certificate[]{cert});
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        return cryptoProvider.getKeyPairGen("RSA").generateKeyPair();
    }

    private X509Certificate generateX509CertificateCertificate(KeyPair keyPair, String subject) throws Exception {
        // generate a v1 root certificate authority certificate
        X509Certificate caCert = cryptoProvider.getCertificateUtils().generateV1SelfSignedCertificate(keyPair, subject);

        // generate a v3 certificate chain
        return cryptoProvider.getCertificateUtils().generateV3Certificate(keyPair, keyPair.getPrivate(), caCert, subject);
    }
}
