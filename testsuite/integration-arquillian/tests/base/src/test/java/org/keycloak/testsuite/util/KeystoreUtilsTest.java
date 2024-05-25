/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.util;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.JavaAlgorithm;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.security.KeyStore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 *
 */
public class KeystoreUtilsTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    @Ignore
    public void testGenerateKeystore() throws Exception {
        // 1. Generate a Secret Key
        KeyGenerator keyGen = KeyGenerator.getInstance(JavaAlgorithm.getJavaAlgorithm(Algorithm.HS256));
        keyGen.init(128);
        SecretKey secretKey = keyGen.generateKey();

        // 2. Prepare Parameters
        KeystoreUtil.KeystoreFormat keystoreType = KeystoreUtil.KeystoreFormat.BCFKS; // Or choose your desired format
        String subject = "keycloak";
        String keystorePassword = "test_password";
        String keyPassword = "test_key_password";

        // 3. Generate Keystore
        KeystoreUtils.KeystoreInfo keystoreInfo = KeystoreUtils.generateKeystore(folder, keystoreType, subject, keystorePassword, keyPassword, secretKey);
        assertNotNull("Keystore file should be created", keystoreInfo.getKeystoreFile());
        assertTrue("Keystore file should exist", keystoreInfo.getKeystoreFile().exists());

        // 4. Verify Keystore Contents
        KeyStore loadedKeyStore = CryptoIntegration.getProvider().getKeyStore(keystoreType);
        loadedKeyStore.load(new FileInputStream(keystoreInfo.getKeystoreFile()), keystorePassword.toCharArray());
        KeyStore.SecretKeyEntry loadedEntry = (KeyStore.SecretKeyEntry) loadedKeyStore.getEntry(subject, new KeyStore.PasswordProtection(keyPassword.toCharArray()));

        // 5. Assertions
        assertNotNull("Loaded key entry should not be null", loadedEntry);
        assertEquals("Secret keys should match", secretKey, loadedEntry.getSecretKey());
    }
}
