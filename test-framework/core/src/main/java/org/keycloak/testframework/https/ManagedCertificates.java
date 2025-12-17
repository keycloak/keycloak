package org.keycloak.testframework.https;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.ssl.SSLContextBuilder;

public class ManagedCertificates {

    private final static Path KEYSTORES_DIR = Path.of(System.getProperty("java.io.tmpdir"));

    private static final String STORE_PASSWORD = "mysuperstrongstorepassword";
    private static final char[] STORE_PASSWORD_CHARS = STORE_PASSWORD.toCharArray();

    private final boolean tlsEnabled;
    private final boolean mTlsEnabled;
    private final KeystoreUtil.KeystoreFormat keystoreFormat;

    private final CryptoProvider cryptoProvider;

    private final Path serverKeystorePath;
    private final Path serverTruststorePath;

    private final Path clientKeystorePath;
    private KeyStore clientKeyStore;

    private final Path clientTruststorePath;
    private KeyStore clientTrustStore;

    private SSLContext clientSslContext;

    public ManagedCertificates(CertificatesConfigBuilder configBuilder) throws ManagedCertificatesException {
        if (!CryptoIntegration.isInitialised()) {
            CryptoIntegration.setProvider(new DefaultCryptoProvider());
        }
        cryptoProvider = CryptoIntegration.getProvider();

        keystoreFormat = configBuilder.getKeystoreFormat();
        tlsEnabled = configBuilder.isTlsEnabled();
        mTlsEnabled = configBuilder.isMTlsEnabled();

        serverKeystorePath = resolvePath("kc-testing-server-keystore");
        serverTruststorePath = resolvePath("kc-testing-server-truststore");

        clientKeystorePath = resolvePath("kc-testing-client-keystore");
        clientTruststorePath = resolvePath("kc-testing-client-truststore");

        if (!Files.exists(serverKeystorePath) || !Files.exists(serverTruststorePath) || !Files.exists(clientKeystorePath) || !Files.exists(clientTruststorePath)) {
            createStores();
        } else {
            clientKeyStore = load(clientKeystorePath);
            clientTrustStore = load(clientTruststorePath);
        }

        clientSslContext = tlsEnabled ? createClientSSLContext() : null;
    }

    public String getServerKeyStorePath() {
        return tlsEnabled ? serverKeystorePath.toString() : null;
    }

    public String getServerKeyStorePassword() {
        return tlsEnabled ? STORE_PASSWORD : null;
    }

    public String getServerTrustStorePath() {
        return mTlsEnabled ? serverTruststorePath.toString() : null;
    }

    public String getServerTrustStorePassword() {
        return mTlsEnabled ? STORE_PASSWORD : null;
    }

    public SSLContext getClientSSLContext() {
        return clientSslContext;
    }

    public boolean isTlsEnabled()  {
        return tlsEnabled;
    }

    public boolean isMTlsEnabled()  {
        return mTlsEnabled;
    }


    private SSLContext createClientSSLContext() {
        try {
            SSLContextBuilder sslContextBuilder = SSLContextBuilder.create()
                    .loadTrustMaterial(clientTrustStore, TrustAllStrategy.INSTANCE);

            if (mTlsEnabled) {
                sslContextBuilder.loadKeyMaterial(clientKeyStore, STORE_PASSWORD_CHARS);
            }

            return sslContextBuilder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createStores() {
        try {
            KeyPair serverKeyPair = generateKeyPair();
            X509Certificate serverCertificate = generateX509CertificateCertificate(serverKeyPair, "localhost");

            KeyPair clientKeyPair = generateKeyPair();
            X509Certificate clientCertificate = generateX509CertificateCertificate(clientKeyPair, "myclient");

            KeyStore serverKeyStore = cryptoProvider.getKeyStore(keystoreFormat);
            serverKeyStore.load(null, STORE_PASSWORD_CHARS);
            serverKeyStore.setKeyEntry("server-key", serverKeyPair.getPrivate(), STORE_PASSWORD_CHARS, new X509Certificate[] { serverCertificate });
            save(serverKeyStore, serverKeystorePath);

            KeyStore serverTrustStore = cryptoProvider.getKeyStore(keystoreFormat);
            serverTrustStore.load(null, STORE_PASSWORD_CHARS);
            serverTrustStore.setCertificateEntry("myclient-certificate", clientCertificate);
            save(serverTrustStore, serverTruststorePath);

            clientKeyStore = cryptoProvider.getKeyStore(keystoreFormat);
            clientKeyStore.load(null, STORE_PASSWORD_CHARS);
            clientKeyStore.setKeyEntry("client-key", clientKeyPair.getPrivate(), STORE_PASSWORD_CHARS, new X509Certificate[] { clientCertificate });
            save(clientKeyStore, clientKeystorePath);

            clientTrustStore = cryptoProvider.getKeyStore(keystoreFormat);
            clientTrustStore.load(null, STORE_PASSWORD_CHARS);
            clientTrustStore.setCertificateEntry("server-certificate", serverCertificate);
            save(clientTrustStore, clientTruststorePath);
        } catch (Exception e) {
            throw new ManagedCertificatesException(e);
        }
    }

    private Path resolvePath(String fileName) {
        return KEYSTORES_DIR.resolve(fileName + "." + keystoreFormat.getPrimaryExtension());
    }

    private void save(KeyStore store, Path storePath) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        try (FileOutputStream fos = new FileOutputStream(storePath.toFile())) {
            store.store(fos, STORE_PASSWORD_CHARS);
        }
    }

    private KeyStore load(Path keyStorePath) {
        try (FileInputStream fis = new FileInputStream(keyStorePath.toFile())) {
            KeyStore keyStore = cryptoProvider.getKeyStore(keystoreFormat);
            keyStore.load(fis, STORE_PASSWORD_CHARS);
            return keyStore;
        } catch (Exception e) {
            throw new ManagedCertificatesException(e);
        }
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
