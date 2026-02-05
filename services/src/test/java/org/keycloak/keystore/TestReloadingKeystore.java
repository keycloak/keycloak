/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.keystore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;

import org.keycloak.common.util.PemUtils;
import org.keycloak.rule.CryptoInitRule;

import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestReloadingKeystore {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void testLoadJks() throws Exception {
        Path ksPath = Paths.get(TestReloadingKeystore.class.getResource("keycloak.jks").toURI());
        KeyStore gotKs = ReloadingKeyStore.Builder.fromKeyStoreFile("JKS", ksPath, "secret").getKeyStore();
        assertNotNull(gotKs);
        assertEquals(Arrays.asList("localhost"), Collections.list(gotKs.aliases()));
    }

    @Test
    public void testLoadP12() throws Exception {
        Path ksPath = Paths.get(TestReloadingKeystore.class.getResource("keycloak.p12").toURI());
        KeyStore gotKs = ReloadingKeyStore.Builder.fromKeyStoreFile("JKS", ksPath, "secret").getKeyStore();
        assertNotNull(gotKs);
        assertEquals(Arrays.asList("localhost"), Collections.list(gotKs.aliases()));
    }

    @Test
    public void testLoadPemWithRsaKey() throws Exception {
        Path certPath = Paths.get(TestReloadingKeystore.class.getResource("rsa-leaf.pem").toURI());
        Path keyPath = Paths.get(TestReloadingKeystore.class.getResource("rsa-leaf-key.pem").toURI());
        Path caPath = Paths.get(TestReloadingKeystore.class.getResource("root-ca.pem").toURI());

        KeyStore gotKs = ReloadingKeyStore.Builder.fromPem(certPath, keyPath).getKeyStore();
        assertNotNull(gotKs);
        assertEquals("CN=rsa-leaf", ((X509Certificate) gotKs.getCertificate("0000")).getSubjectX500Principal().getName());

        KeyStore gotTs = ReloadingKeyStore.Builder.fromPem(caPath).getKeyStore();
        assertNotNull(gotTs);
        assertEquals("CN=root-ca", ((X509Certificate) gotTs.getCertificate("0000")).getSubjectX500Principal().getName());
    }

    @Test
    public void testLoadPemWithEcKey() throws Exception {
        Path certPath = Paths.get(TestReloadingKeystore.class.getResource("ec-leaf.pem").toURI());
        Path keyPath = Paths.get(TestReloadingKeystore.class.getResource("ec-leaf-key.pem").toURI());
        Path caPath = Paths.get(TestReloadingKeystore.class.getResource("root-ca.pem").toURI());

        KeyStore gotKs = ReloadingKeyStore.Builder.fromPem(certPath, keyPath).getKeyStore();
        assertNotNull(gotKs);
        assertEquals("CN=ec-leaf",
                ((X509Certificate) gotKs.getCertificate("0000")).getSubjectX500Principal().getName());

        KeyStore gotTs = ReloadingKeyStore.Builder.fromPem(caPath).getKeyStore();
        assertNotNull(gotTs);
        assertEquals("CN=root-ca",
                ((X509Certificate) gotTs.getCertificate("0000")).getSubjectX500Principal().getName());
    }

    @Test
    public void testLoadPemBundle() throws Exception {
        Path certPath = Paths.get(TestReloadingKeystore.class.getResource("rsa-leaf.pem").toURI());
        String bundle = new String(Files.readAllBytes(certPath));

        X509Certificate[] certs = PemUtils.decodeCertificates(bundle);

        assertEquals(2, certs.length);
        assertEquals("CN=rsa-leaf", certs[0].getSubjectX500Principal().getName());
        assertEquals("CN=intermediate-ca", certs[1].getSubjectX500Principal().getName());
    }

}
