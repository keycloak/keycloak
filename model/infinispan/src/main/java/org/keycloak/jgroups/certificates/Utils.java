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

package org.keycloak.jgroups.certificates;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.KeystoreUtil;

import org.jboss.logging.Logger;

public final class Utils {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
    private static final String JGROUPS_SUBJECT = "jgroups";
    private static final char[] KEY_PASSWORD = "jgroups-password".toCharArray();

    private Utils() {
    }

    public static JGroupsCertificate generateSelfSignedCertificate(Duration validity) {
        var endDate = Date.from(Instant.now().plus(validity));
        var keyPair = KeyUtils.generateRsaKeyPair(2048);
        var certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, JGROUPS_SUBJECT, BigInteger.valueOf(System.currentTimeMillis()), endDate);

        logger.debugf("Created JGroups certificate. Valid until %s", certificate.getNotAfter());

        var entity = new JGroupsCertificate();
        entity.setCertificate(certificate);
        entity.setKeyPair(keyPair);
        entity.setAlias(UUID.randomUUID().toString());
        entity.setGeneratedMillis(System.currentTimeMillis());
        return entity;
    }

    public static X509ExtendedKeyManager createKeyManager(JGroupsCertificate certificate) throws GeneralSecurityException, IOException {
        var ks = getKeyStore();
        ks.setKeyEntry(certificate.getAlias(), certificate.getPrivateKey(), KEY_PASSWORD, new java.security.cert.Certificate[]{certificate.getCertificate()});
        var kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, KEY_PASSWORD);
        for (KeyManager km : kmf.getKeyManagers()) {
            if (km instanceof X509ExtendedKeyManager) {
                return (X509ExtendedKeyManager) km;
            }
        }
        throw new GeneralSecurityException("Could not obtain an X509ExtendedKeyManager");
    }

    public static X509ExtendedTrustManager createTrustManager(JGroupsCertificate certificate) throws GeneralSecurityException, IOException {
        var ks = getKeyStore();
        ks.setCertificateEntry(certificate.getAlias(), certificate.getCertificate());
        var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509ExtendedTrustManager) {
                return (X509ExtendedTrustManager) tm;
            }
        }
        throw new GeneralSecurityException("Could not obtain an X509TrustManager");
    }

    private static KeyStore getKeyStore() throws KeyStoreException, NoSuchProviderException, CertificateException, IOException, NoSuchAlgorithmException {
        KeystoreUtil.KeystoreFormat keystoreFormat = CryptoIntegration.getProvider().getSupportedKeyStoreTypes().findFirst().orElseThrow(() -> new RuntimeException("No supported keystore types found"));
        var ks = CryptoIntegration.getProvider().getKeyStore(keystoreFormat);
        ks.load(null, null);
        return ks;
    }
}
