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

package org.keycloak.testsuite.util;

import org.junit.Assume;
import org.junit.rules.TemporaryFolder;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.common.util.PemUtils;
import org.keycloak.representations.idm.CertificateRepresentation;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeystoreUtils {

    public static String[] getSupportedKeystoreTypes() {
        String supportedKeystoreTypes = System.getProperty("auth.server.supported.keystore.types");
        if (supportedKeystoreTypes == null || supportedKeystoreTypes.trim().isEmpty()) {
            fail("Property 'auth.server.supported.keystore.types' not set");
        }
        return supportedKeystoreTypes.split(",");
    }

    public static KeystoreUtil.KeystoreFormat getPreferredKeystoreType() {
        return Enum.valueOf(KeystoreUtil.KeystoreFormat.class, getSupportedKeystoreTypes()[0]);
    }

    public static void assumeKeystoreTypeSupported(KeystoreUtil.KeystoreFormat keystoreType) {
        String[] supportedKeystoreTypes = KeystoreUtils.getSupportedKeystoreTypes();
        Assume.assumeTrue("Keystore type '" + keystoreType + "' not supported. Supported keystore types: " + Arrays.asList(supportedKeystoreTypes),
                Stream.of(supportedKeystoreTypes)
                        .anyMatch(type -> type.equals(keystoreType.toString())));
    }

    public static KeystoreInfo generateKeystore(TemporaryFolder folder, KeystoreUtil.KeystoreFormat keystoreType, String subject, String keystorePassword, String keyPassword, KeyPair keyPair) throws Exception {
        String fileName = "keystore." + keystoreType.getPrimaryExtension();

        X509Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, subject);

        KeyStore keyStore = CryptoIntegration.getProvider().getKeyStore(keystoreType);
        keyStore.load(null, null);

        Certificate[] chain = {certificate};
        keyStore.setKeyEntry(subject, keyPair.getPrivate(), keyPassword.trim().toCharArray(), chain);

        File file = folder.newFile(fileName);
        keyStore.store(new FileOutputStream(file), keystorePassword.trim().toCharArray());

        CertificateRepresentation certRep = new CertificateRepresentation();
        certRep.setPrivateKey(PemUtils.encodeKey(keyPair.getPrivate()));
        certRep.setPublicKey(PemUtils.encodeKey(keyPair.getPublic()));
        certRep.setCertificate(PemUtils.encodeCertificate(certificate));
        return new KeystoreInfo(certRep, file);
    }

    public static KeystoreInfo generateKeystore(TemporaryFolder folder, KeystoreUtil.KeystoreFormat keystoreType, String subject, String keystorePassword, String keyPassword) throws Exception {
        return generateKeystore(folder, keystoreType, subject, keystorePassword, keyPassword, KeyUtils.generateRsaKeyPair(2048));
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
