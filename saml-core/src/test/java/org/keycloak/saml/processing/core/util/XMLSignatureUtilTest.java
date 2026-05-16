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
package org.keycloak.saml.processing.core.util;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyName;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;

import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link XMLSignatureUtil#createKeyInfo(String, java.security.PublicKey, X509Certificate)}.
 *
 * <p>Verifies that the produced {@link KeyInfo} contains the correct child elements
 * for the supplied inputs and, in particular, that {@code KeyName} is only emitted as
 * a fallback when neither an X.509 certificate nor a public key is available. This
 * behaviour is required for interoperability with SAML SP implementations (e.g.
 * Shibboleth SP) that fail when {@code KeyName} is present alongside
 * {@code X509Data} in {@code KeyInfo}.
 */
public class XMLSignatureUtilTest {

    private static final String KEY_NAME = "test-key";

    private static KeyPair keyPair;
    private static X509Certificate certificate;

    @BeforeClass
    public static void setUp() {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
        keyPair = KeyUtils.generateRsaKeyPair(2048);
        certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "TestCert");
    }

    @Test
    public void keyInfoWithCertificateContainsOnlyX509Data() throws Exception {
        KeyInfo keyInfo = XMLSignatureUtil.createKeyInfo(KEY_NAME, keyPair.getPublic(), certificate);

        assertEquals("X509Data should be present", 1, count(keyInfo, X509Data.class));
        assertEquals("KeyName must not be emitted when an X509 certificate is supplied",
                0, count(keyInfo, KeyName.class));
        assertEquals("KeyValue must not be emitted when an X509 certificate is supplied",
                0, count(keyInfo, KeyValue.class));
    }

    @Test
    public void keyInfoWithCertificateAndNullKeyNameContainsOnlyX509Data() throws Exception {
        KeyInfo keyInfo = XMLSignatureUtil.createKeyInfo(null, keyPair.getPublic(), certificate);

        assertEquals(1, count(keyInfo, X509Data.class));
        assertEquals(0, count(keyInfo, KeyName.class));
        assertEquals(0, count(keyInfo, KeyValue.class));
    }

    @Test
    public void keyInfoWithPublicKeyOnlyContainsKeyValue() throws Exception {
        KeyInfo keyInfo = XMLSignatureUtil.createKeyInfo(KEY_NAME, keyPair.getPublic(), null);

        assertEquals("KeyValue should be present when no certificate is supplied",
                1, count(keyInfo, KeyValue.class));
        assertEquals("KeyName must not be emitted when a public key is supplied",
                0, count(keyInfo, KeyName.class));
        assertEquals(0, count(keyInfo, X509Data.class));
    }

    @Test
    public void keyInfoFallsBackToKeyNameWhenNoCertOrPublicKey() throws Exception {
        KeyInfo keyInfo = XMLSignatureUtil.createKeyInfo(KEY_NAME, null, null);

        assertEquals("KeyName must be used as a fallback when neither a certificate nor a public key is supplied",
                1, count(keyInfo, KeyName.class));
        assertEquals(0, count(keyInfo, X509Data.class));
        assertEquals(0, count(keyInfo, KeyValue.class));
    }

    private static long count(KeyInfo keyInfo, Class<? extends XMLStructure> type) {
        List<XMLStructure> content = keyInfo.getContent();
        return content.stream().filter(type::isInstance).count();
    }
}
