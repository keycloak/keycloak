package org.keycloak.infinispan.module.certificates;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Objects;

import org.jboss.logging.Logger;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.KeystoreUtil;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;

//TODO move to SPI https://github.com/keycloak/keycloak/issues/37325

/**
 * Holds the JGroups certificate and updates the {@link X509ExtendedKeyManager} and {@link X509ExtendedTrustManager}
 * used by the TLS/SSL sockets.
 */
public class JGroupsCertificateHolder {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
    private static final char[] KEY_PASSWORD = "jgroups-password".toCharArray();

    private volatile JGroupsCertificate certificate;
    private final ReloadingX509ExtendedKeyManager keyManager;
    private final ReloadingX509ExtendedTrustManager trustManager;


    private JGroupsCertificateHolder(ReloadingX509ExtendedKeyManager keyManager, ReloadingX509ExtendedTrustManager trustManager, JGroupsCertificate certificate) {
        this.keyManager = keyManager;
        this.trustManager = trustManager;
        this.certificate = certificate;
    }

    public static JGroupsCertificateHolder create(JGroupsCertificate certificate) throws GeneralSecurityException, IOException {
        Objects.requireNonNull(certificate);
        var km = createKeyManager(certificate);
        var tm = createTrustManager(null, certificate);
        var crt =certificate.getCertificate();
        logger.debugf("Using JGroups certificate (serial: %s). Valid until %s", crt.getSerialNumber(), crt.getNotAfter());
        return new JGroupsCertificateHolder(new ReloadingX509ExtendedKeyManager(km), new ReloadingX509ExtendedTrustManager(tm), certificate);
    }

    public JGroupsCertificate getCertificateInUse() {
        return certificate;
    }

    public void useCertificate(JGroupsCertificate certificate) throws GeneralSecurityException, IOException {
        Objects.requireNonNull(certificate);
        if (Objects.equals(certificate.getAlias(), this.certificate.getAlias())) {
            return;
        }
        var crt =certificate.getCertificate();
        logger.debugf("Using JGroups certificate (serial: %s). Valid until %s", crt.getSerialNumber(), crt.getNotAfter());
        if (this.certificate != null) {
            crt = this.certificate.getCertificate();
            logger.debugf("Old JGroups certificate (serial: %s). Valid until %s", crt.getSerialNumber(), crt.getNotAfter());
        }
        var km = createKeyManager(certificate);
        var tm = createTrustManager(this.certificate, certificate);
        keyManager.reload(km);
        trustManager.reload(tm);
        this.certificate = certificate;
    }

    public X509ExtendedKeyManager keyManager() {
        return keyManager;
    }

    public X509ExtendedTrustManager trustManager() {
        return trustManager;
    }

    public void setExceptionHandler(Runnable runnable) {
        trustManager.setExceptionHandler(runnable);
    }

    private static X509ExtendedKeyManager createKeyManager(JGroupsCertificate newCertificate) throws GeneralSecurityException, IOException {
        var ks = CryptoIntegration.getProvider().getKeyStore(KeystoreUtil.KeystoreFormat.JKS);
        ks.load(null, null);
        ks.setKeyEntry(newCertificate.getAlias(), newCertificate.getPrivateKey(), KEY_PASSWORD, new java.security.cert.Certificate[]{newCertificate.getCertificate()});
        var kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, KEY_PASSWORD);
        for (KeyManager km : kmf.getKeyManagers()) {
            if (km instanceof X509ExtendedKeyManager) {
                return (X509ExtendedKeyManager) km;
            }
        }
        throw new GeneralSecurityException("Could not obtain an X509ExtendedKeyManager");
    }

    private static X509ExtendedTrustManager createTrustManager(JGroupsCertificate oldCertificate, JGroupsCertificate newCertificate) throws GeneralSecurityException, IOException {
        var ks = CryptoIntegration.getProvider().getKeyStore(KeystoreUtil.KeystoreFormat.JKS);
        ks.load(null, null);
        if (oldCertificate != null) {
            addCertificateEntry(ks, oldCertificate);
        }
        addCertificateEntry(ks, newCertificate);
        var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509ExtendedTrustManager) {
                return (X509ExtendedTrustManager) tm;
            }
        }
        throw new GeneralSecurityException("Could not obtain an X509TrustManager");
    }

    private static void addCertificateEntry(KeyStore store, JGroupsCertificate certificate) throws KeyStoreException {
        store.setCertificateEntry(certificate.getAlias(), certificate.getCertificate());
    }
}
