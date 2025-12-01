/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testframework.crypto;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.stream.Stream;
import javax.crypto.SecretKey;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.Time;
import org.keycloak.representations.idm.CertificateRepresentation;

import org.junit.jupiter.api.Assumptions;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CryptoKeyStore {

    private final CryptoHelper cryptoHelper;

    CryptoKeyStore(CryptoHelper cryptoHelper) {
        this.cryptoHelper = cryptoHelper;
    }

    public KeystoreUtil.KeystoreFormat getPreferredKeystoreType() {
        return Enum.valueOf(KeystoreUtil.KeystoreFormat.class, cryptoHelper.getExpectedSupportedKeyStoreTypes()[0]);
    }

    public void assumeKeystoreTypeSupported(KeystoreUtil.KeystoreFormat keystoreType) {
        String[] supportedKeystoreTypes = cryptoHelper.getExpectedSupportedKeyStoreTypes();
        Assumptions.assumeTrue(Stream.of(supportedKeystoreTypes).anyMatch(type -> type.equals(keystoreType.toString())),
                "Keystore type '" + keystoreType + "' not supported. Supported keystore types: " + Arrays.asList(supportedKeystoreTypes));
    }

    public KeystoreInfo generateKeystore(File folder, KeystoreUtil.KeystoreFormat keystoreType, String subject, String keystorePassword, String keyPassword) throws Exception {
        return generateKeystore(folder, keystoreType, subject, keystorePassword, keyPassword, KeyUtils.generateRsaKeyPair(2048));
    }

    public KeystoreInfo generateKeystore(File folder, KeystoreUtil.KeystoreFormat keystoreType, String subject, String keystorePassword, String keyPassword, KeyPair keyPair) throws Exception {
        X509Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, subject);
        return generateKeystore(folder, keystoreType, subject, keystorePassword, keyPassword, keyPair.getPrivate(), certificate);
    }

    public KeystoreInfo generateKeystore(File folder, KeystoreUtil.KeystoreFormat keystoreType,
            String subject, String keystorePassword, String keyPassword, PrivateKey privKey, Certificate certificate) throws Exception {
        KeyStore keyStore = CryptoIntegration.getProvider().getKeyStore(keystoreType);
        keyStore.load(null, null);
        Certificate[] chain = {certificate};
        keyStore.setKeyEntry(subject, privKey, keyPassword.trim().toCharArray(), chain);

        File file = saveKeystore(folder, keystoreType, keyStore, keystorePassword);

        CertificateRepresentation certRep = new CertificateRepresentation();
        certRep.setPrivateKey(PemUtils.encodeKey(privKey));
        certRep.setPublicKey(PemUtils.encodeKey(certificate.getPublicKey()));
        certRep.setCertificate(PemUtils.encodeCertificate(certificate));
        return new KeystoreInfo(certRep, file);
    }

    public KeystoreInfo generateKeystore(File folder, KeystoreUtil.KeystoreFormat keystoreType, String alias,
            String keystorePassword, String keyPassword, SecretKey secretKey) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(keystoreType.name());
        keyStore.load(null, null);

        KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
        KeyStore.ProtectionParameter protection = new KeyStore.PasswordProtection(keyPassword.trim().toCharArray());
        keyStore.setEntry(alias, secretKeyEntry, protection);

        File file = saveKeystore(folder, keystoreType, keyStore, keystorePassword);

        return new KeystoreInfo(null, file);
    }

    private File saveKeystore(File folder, KeystoreUtil.KeystoreFormat keystoreType, KeyStore keyStore, String keystorePassword) throws Exception {
        String fileName = "keystore-" + Time.currentTimeMillis() + "." + keystoreType.getPrimaryExtension();
        File file = new File(folder, fileName);
        if (file.exists()) {
            throw new RuntimeException("Keystore file already exists: " + file.getAbsolutePath());
        }
        FileOutputStream fos = new FileOutputStream(file);
        keyStore.store(fos, keystorePassword.trim().toCharArray());
        fos.close();
        return file;
    }

}
