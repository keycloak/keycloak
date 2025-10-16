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
import java.security.UnrecoverableKeyException;
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

    private Certificate server;
    private Certificate client;
    private final char[] password;
    private final boolean tlsEnabled;
    private final boolean mTlsEnabled;
    private final KeystoreUtil.KeystoreFormat keystoreFormat;

    private final static Path KEYSTORES_DIR = Path.of(System.getProperty("java.io.tmpdir"));

    public ManagedCertificates(CertificatesConfigBuilder configBuilder) throws ManagedCertificatesException {
        if (!CryptoIntegration.isInitialised()) {
            CryptoIntegration.setProvider(new DefaultCryptoProvider());
        }
        cryptoProvider = CryptoIntegration.getProvider();

        keystoreFormat = configBuilder.getKeystoreFormat();
        tlsEnabled = configBuilder.isTlsEnabled();
        mTlsEnabled = configBuilder.isMTlsEnabled();
        password = keystoreFormat == KeystoreUtil.KeystoreFormat.JKS ? "password".toCharArray() : "passwordpassword".toCharArray();

        initServerAndClientCertificateObjects();

        initKeyAndTrustStores();
    }

    public void initServerAndClientCertificateObjects() {
        Path serverKeystorePath = resolvePath("kc-testing-server-keystore");
        Path serverTruststorePath = resolvePath("kc-testing-server-truststore");
        server = new Certificate(serverKeystorePath, serverTruststorePath);
        server.setKeyEntry("server-private-key");
        server.setCertificateEntry("server-certificate");

        if (mTlsEnabled) {
            Path clientKeystorePath = resolvePath("kc-testing-client-keystore");
            Path clientTruststorePath = resolvePath("kc-testing-client-truststore");
            client = new Certificate(clientKeystorePath, clientTruststorePath);
            client.setKeyEntry("client-private-key");
            client.setCertificateEntry("client-certificate");
        }
    }

    private Path resolvePath(String fileName) {
        return KEYSTORES_DIR.resolve(fileName + "." + keystoreFormat.getPrimaryExtension());
    }

    public SSLContext getClientSSLContext() {
        try {
            if (mTlsEnabled) {
                return SSLContextBuilder.create()
                        .loadTrustMaterial(client.getTruststorePath().toFile(), password)
                        .loadKeyMaterial(client.getKeystore(), password)
                        .build();
            } else {
                return SSLContextBuilder.create()
                        .loadTrustMaterial(server.getTruststore(), null)
                        .build();
            }
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new ManagedCertificatesException(e);
        } catch (UnrecoverableKeyException | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getServerKeyStorePath() {
        return server.getKeystorePath().toString();
    }

    public String getClientTrustStorePath() {
        return server.getTruststorePath().toString();
    }

    public String getKeyAndTrustStorePassword() {
        return String.valueOf(password);
    }

    public KeyStore getServerTrustStore() {
        if (mTlsEnabled) {
            return client.getTruststore();
        } else {
            return server.getTruststore();
        }
    }

    public String getServerCertificateEntry() {
        return server.getCertificateEntry();
    }

    public X509Certificate getServerCertificate() {
        try {
            if (mTlsEnabled) {
                return (X509Certificate) client.getKeystore().getCertificate(server.getCertificateEntry());
            } else {
                return (X509Certificate) server.getKeystore().getCertificate(server.getCertificateEntry());
            }
        } catch (KeyStoreException e) {
            throw new ManagedCertificatesException(e);
        }
    }

    public boolean isTlsEnabled()  {
        return tlsEnabled;
    }

    public boolean isMTlsEnabled()  {
        return mTlsEnabled;
    }

    private void initKeyAndTrustStores() throws ManagedCertificatesException {
        try {
            server.setKeystore(cryptoProvider.getKeyStore(keystoreFormat));
            server.setTruststore(cryptoProvider.getKeyStore(keystoreFormat));

            if (mTlsEnabled) {
                client.setKeystore(cryptoProvider.getKeyStore(keystoreFormat));
                client.setTruststore(cryptoProvider.getKeyStore(keystoreFormat));
            }

            if (Files.exists(server.getKeystorePath()) && Files.exists(server.getTruststorePath())
                    && Files.exists(client.getKeystorePath()) && Files.exists(client.getTruststorePath())) {
                LOGGER.debugv("Existing Server KeyStore files found in {0}", KEYSTORES_DIR);

                if (mTlsEnabled) {
                    loadKeyStore(client.getKeystore(), client.getKeystorePath(), password);
                    loadKeyStore(client.getTruststore(), client.getTruststorePath(), password);
                }

                loadKeyStore(server.getKeystore(), server.getKeystorePath(), password);
                loadKeyStore(server.getTruststore(), server.getTruststorePath(), password);
            } else {
                LOGGER.debugv("Generating Server KeyStore files in {0}", KEYSTORES_DIR);

                if (mTlsEnabled) {
                    generateKeystoreAndTruststore(client.getKeystore(), server.getTruststore(), client.getKeyEntry(), client.getCertificateEntry(), "localhost");
                    storeKeyOrTrustStore(client.getKeystore(), client.getKeystorePath());

                    generateKeystoreAndTruststore(server.getKeystore(), client.getTruststore(), server.getKeyEntry(), server.getCertificateEntry(), "localhost");
                    storeKeyOrTrustStore(client.getTruststore(), client.getTruststorePath());
                } else {
                    generateKeystoreAndTruststore(server.getKeystore(), server.getTruststore(), server.getKeyEntry(), server.getCertificateEntry(), "localhost");
                }
                storeKeyOrTrustStore(server.getKeystore(), server.getKeystorePath());
                storeKeyOrTrustStore(server.getTruststore(), server.getTruststorePath());
            }
        } catch (Exception e) {
            throw new ManagedCertificatesException(e);
        }
    }

    private void storeKeyOrTrustStore(KeyStore store, Path storePath) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        try (FileOutputStream fos = new FileOutputStream(storePath.toFile())) {
            store.store(fos, password);
        }
    }

    private void loadKeyStore(KeyStore keyStore, Path keyStorePath, char[] keyStorePasswd) throws NoSuchAlgorithmException, IOException, CertificateException {
        try (FileInputStream fis = new FileInputStream(keyStorePath.toFile())) {
            keyStore.load(fis, keyStorePasswd);
        }
    }

    private void generateKeystoreAndTruststore(KeyStore keyStore, KeyStore trustStore, String privateKey, String certificateEntry, String subject) throws NoSuchAlgorithmException, NoSuchProviderException, CertificateException, IOException, KeyStoreException, Exception {
        keyStore.load(null);
        trustStore.load(null);

        KeyPair keyPair = generateKeyPair();
        X509Certificate cert = generateX509CertificateCertificate(keyPair, subject);

        keyStore.setCertificateEntry(certificateEntry, cert);
        trustStore.setCertificateEntry(certificateEntry, cert);
        keyStore.setKeyEntry(privateKey, keyPair.getPrivate(), password, new X509Certificate[]{cert});
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
