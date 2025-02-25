/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.infinispan.module.certificates;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Objects;

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
        var km = createKeyManager(null, certificate);
        var tm = createTrustManager(null, certificate);
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
        var km = createKeyManager(this.certificate, certificate);
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

    private static X509ExtendedKeyManager createKeyManager(JGroupsCertificate oldCertificate, JGroupsCertificate newCertificate) throws GeneralSecurityException, IOException {
        var ks = CryptoIntegration.getProvider().getKeyStore(KeystoreUtil.KeystoreFormat.JKS);
        ks.load(null, null);
        if (oldCertificate != null) {
            addKeyEntry(ks, oldCertificate);
        }
        addKeyEntry(ks, newCertificate);
        var kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, KEY_PASSWORD);
        for (KeyManager km : kmf.getKeyManagers()) {
            if (km instanceof X509ExtendedKeyManager) {
                return (X509ExtendedKeyManager) km;
            }
        }
        throw new GeneralSecurityException("Could not obtain an X509ExtendedKeyManager");
    }

    private static void addKeyEntry(KeyStore store, JGroupsCertificate certificate) throws KeyStoreException {
        store.setKeyEntry(certificate.getAlias(), certificate.getPrivateKey(), KEY_PASSWORD, new java.security.cert.Certificate[]{certificate.getCertificate()});
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
