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

package org.keycloak.broker.oidc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.List;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ClientCertificatesTest {

    private static KeyPair keyPair;

    @BeforeClass
    public static void init() throws Exception {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    private static KeyWrapper key(KeyStatus status, long priority, String subject) {
        KeyWrapper key = new KeyWrapper();
        key.setProviderId("provider-1");
        key.setStatus(status);
        key.setProviderPriority(priority);
        key.setUse(KeyUse.SIG);
        key.setPrivateKey(keyPair.getPrivate());
        key.setCertificate(CertificateUtils.generateV1SelfSignedCertificate(keyPair, subject));
        return key;
    }

    private static String subjectOf(KeyWrapper key) {
        return key.getCertificate().getSubjectX500Principal().getName();
    }

    @Test
    public void keyWithPrivateKeyAndCertificateIsUsable() {
        assertNull(ClientCertificates.unusableReason(key(KeyStatus.ACTIVE, 0, "client")));
    }

    @Test
    public void missingKeyIsUnusable() {
        assertEquals("has no key", ClientCertificates.unusableReason(null));
    }

    @Test
    public void keyWithoutPrivateKeyIsUnusable() {
        KeyWrapper key = key(KeyStatus.ACTIVE, 0, "client");
        key.setPrivateKey(null);
        assertEquals("has no private key", ClientCertificates.unusableReason(key));
    }

    @Test
    public void keyWithoutCertificateIsUnusable() {
        KeyWrapper key = key(KeyStatus.ACTIVE, 0, "client");
        key.setCertificate(null);
        assertEquals("has no certificate", ClientCertificates.unusableReason(key));
    }

    @Test
    public void encryptionOnlyKeyIsUnusable() {
        KeyWrapper key = key(KeyStatus.ACTIVE, 0, "client");
        key.setUse(KeyUse.ENC);
        assertEquals("is not a signing key", ClientCertificates.unusableReason(key));
    }

    @Test
    public void activeKeyIsPreferredToPassiveKey() {
        // the passive key has the higher priority, but the active one wins
        assertEquals("CN=active", subjectOf(ClientCertificates.select(List.of(
                key(KeyStatus.PASSIVE, 200, "passive"),
                key(KeyStatus.ACTIVE, 100, "active")))));
    }

    @Test
    public void enabledKeyIsPreferredToDisabledKey() {
        assertEquals("CN=passive", subjectOf(ClientCertificates.select(List.of(
                key(KeyStatus.DISABLED, 300, "disabled"),
                key(KeyStatus.PASSIVE, 100, "passive")))));
    }

    @Test
    public void highestPriorityComesFirstAmongKeysWithTheSameStatus() {
        assertEquals("CN=high", subjectOf(ClientCertificates.select(List.of(
                key(KeyStatus.ACTIVE, 100, "low"),
                key(KeyStatus.ACTIVE, 200, "high"),
                key(KeyStatus.ACTIVE, 150, "middle")))));
    }

    @Test
    public void usableKeyIsPreferredToKeyWithoutPrivateKey() {
        KeyWrapper withoutPrivateKey = key(KeyStatus.ACTIVE, 200, "no-private-key");
        withoutPrivateKey.setPrivateKey(null);

        assertEquals("CN=usable", subjectOf(ClientCertificates.select(List.of(withoutPrivateKey,
                key(KeyStatus.ACTIVE, 100, "usable")))));
    }

    @Test
    public void providerWithoutKeysSelectsNothing() {
        assertNull(ClientCertificates.select(List.of()));
    }

    @Test
    public void certificateChainStartsWithTheCertificateOfTheKey() {
        KeyWrapper key = key(KeyStatus.ACTIVE, 0, "client");
        assertEquals(1, ClientCertificates.certificateChain(key).length);

        X509Certificate issuer = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "issuer");
        key.setCertificateChain(List.of(key.getCertificate(), issuer));
        X509Certificate[] chain = ClientCertificates.certificateChain(key);
        assertEquals(2, chain.length);
        assertSame(key.getCertificate(), chain[0]);
    }

    @Test
    public void idIsScopedByTheRealm() {
        KeyWrapper key = key(KeyStatus.ACTIVE, 0, "client");
        assertNotEquals(ClientCertificates.of("realm-a", key).getId(), ClientCertificates.of("realm-b", key).getId());
    }
}
