/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Set;

import org.keycloak.common.crypto.CryptoIntegration;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class IdentityAlgorithmTest {
    @BeforeClass
    public static void initialize() {
        CryptoIntegration.init(IdentityAlgorithmTest.class.getClassLoader());
    }

    @Test
    public void startupRegeneratesOnlyOnMismatchAndSupportsRollback() throws Exception {
        var storage = new MemoryStorage();
        String lastAlias = null;
        for (String algorithm : new String[]{"RSA", "ML-DSA-65", "RSA"}) {
            java.util.function.Supplier<String> generator = () -> JGroupsCertificate.toJson(
                    Utils.generateSelfSignedCertificate(Duration.ofDays(1), algorithm));
            var first = DatabaseJGroupsCertificateProvider.loadOrCreateCertificate(storage, algorithm, generator);
            org.junit.Assert.assertNotEquals(lastAlias, first.getAlias());
            assertEquals(algorithm, DatabaseJGroupsCertificateProvider.identityAlgorithm(first));
            var second = DatabaseJGroupsCertificateProvider.loadOrCreateCertificate(storage, algorithm,
                    () -> { throw new AssertionError("Matching identity must be reused"); });
            assertEquals(first.getAlias(), second.getAlias());
            lastAlias = first.getAlias();
        }
    }

    private static class MemoryStorage implements org.keycloak.storage.configuration.ServerConfigStorageProvider {
        private String value;
        public synchronized java.util.Optional<String> find(String key) { return java.util.Optional.ofNullable(value); }
        public synchronized void store(String key, String value) { this.value = value; }
        public synchronized void remove(String key) { value = null; }
        public synchronized String loadOrCreate(String key, java.util.function.Supplier<String> generator) {
            if (value == null) { value = generator.get(); }
            return value;
        }
        public synchronized boolean replace(String key, java.util.function.Predicate<String> predicate,
                java.util.function.Supplier<String> generator) {
            if (value != null && predicate.test(value)) { value = generator.get(); return true; }
            return false;
        }
        public java.util.stream.Stream<String> keys() { return java.util.stream.Stream.of("crt_jgroups"); }
        public void close() { }
    }

    @Test
    public void generationAndDatabaseRoundTrip() throws Exception {
        for (String algorithm : Set.of("RSA", "ML-DSA-65")) {
            var original = Utils.generateSelfSignedCertificate(Duration.ofDays(1), algorithm);
            var identity = JGroupsCertificate.fromJson(JGroupsCertificate.toJson(original));
            assertEquals(algorithm, DatabaseJGroupsCertificateProvider.identityAlgorithm(identity));
            assertEquals(original.getPrivateKey(), identity.getPrivateKey());
            identity.getCertificate().verify(identity.getCertificate().getPublicKey());
            Utils.createKeyManager(identity);
        }
    }

    @Test
    public void trustFollowsTheReloadedDatabaseIdentity() throws Exception {
        var rsa = Utils.generateSelfSignedCertificate(Duration.ofDays(1), "RSA");
        var pq = Utils.generateSelfSignedCertificate(Duration.ofDays(1), "ML-DSA-65");
        var manager = new ReloadingX509ExtendedTrustManager();
        manager.reload(Utils.createTrustManager(rsa));
        manager.checkClientTrusted(new X509Certificate[]{rsa.getCertificate()}, "RSA");
        assertThrows(CertificateException.class, () -> manager.checkClientTrusted(new X509Certificate[]{pq.getCertificate()}, "ML-DSA-65"));
        manager.reload(Utils.createTrustManager(pq));
        manager.checkClientTrusted(new X509Certificate[]{pq.getCertificate()}, "ML-DSA-65");
        assertThrows(CertificateException.class, () -> manager.checkClientTrusted(new X509Certificate[]{rsa.getCertificate()}, "RSA"));
        manager.reload(Utils.createTrustManager(rsa));
        manager.checkClientTrusted(new X509Certificate[]{rsa.getCertificate()}, "RSA");
    }
}
