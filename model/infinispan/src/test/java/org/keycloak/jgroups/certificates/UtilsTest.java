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

import java.time.Duration;

import org.keycloak.common.crypto.CryptoIntegration;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class UtilsTest {

    @BeforeClass
    public static void initCrypto() {
        CryptoIntegration.init(UtilsTest.class.getClassLoader());
    }

    @Test
    public void testRsaCertificate() throws Exception {
        var cert = Utils.generateSelfSignedCertificate(Duration.ofDays(1), new KeySpec.Rsa(3072));

        var publicKey = (java.security.interfaces.RSAPublicKey) cert.getCertificate().getPublicKey();
        Assert.assertEquals(3072, publicKey.getModulus().bitLength());
        // RSASSA-PSS
        Assert.assertEquals("1.2.840.113549.1.1.10", cert.getCertificate().getSigAlgOID());
        cert.getCertificate().verify(publicKey);
        Assert.assertNotNull(Utils.createKeyManager(cert));
        Assert.assertNotNull(Utils.createTrustManager(cert));
    }

    @Test
    public void testEcdsaCertificate() throws Exception {
        var cert = Utils.generateSelfSignedCertificate(Duration.ofDays(1), new KeySpec.Ecdsa("secp384r1"));

        var publicKey = (java.security.interfaces.ECPublicKey) cert.getCertificate().getPublicKey();
        Assert.assertEquals(384, publicKey.getParams().getOrder().bitLength());
        cert.getCertificate().verify(publicKey);
        Assert.assertEquals("EC", cert.getPrivateKey().getAlgorithm());
        Assert.assertNotNull(Utils.createKeyManager(cert));
        Assert.assertNotNull(Utils.createTrustManager(cert));
    }

    @Test
    public void testLoadPrivateKeyStoredByPreviousVersion() throws Exception {
        var cert = Utils.generateSelfSignedCertificate(Duration.ofDays(1), new KeySpec.Rsa(2048));
        var original = cert.getPrivateKey();
        // previous versions stored the RSA private key with PemUtils
        cert.setPrivateKeyPem(org.keycloak.common.util.PemUtils.encodeKey(original));

        Assert.assertEquals(original, cert.getPrivateKey());
        Assert.assertNotNull(Utils.createKeyManager(cert));
    }

    @Test
    public void testKeySpecValidation() {
        Assert.assertEquals(new KeySpec.Rsa(3072), KeySpec.of("rsa", null, null));
        Assert.assertEquals(new KeySpec.Ecdsa("secp256r1"), KeySpec.of("ECDSA", null, null));
        Assert.assertThrows(IllegalArgumentException.class, () -> KeySpec.of("RSA", 1024, null));
        Assert.assertThrows(IllegalArgumentException.class, () -> KeySpec.of("ECDSA", null, "secp192r1"));
        Assert.assertThrows(IllegalArgumentException.class, () -> KeySpec.of("DSA", null, null));
    }

    @Test
    public void testKeySpecMatches() {
        var rsa3072 = Utils.generateSelfSignedCertificate(Duration.ofDays(1), new KeySpec.Rsa(3072)).getCertificate().getPublicKey();
        var rsa2048 = Utils.generateSelfSignedCertificate(Duration.ofDays(1), new KeySpec.Rsa(2048)).getCertificate().getPublicKey();
        var p256 = Utils.generateSelfSignedCertificate(Duration.ofDays(1), new KeySpec.Ecdsa("secp256r1")).getCertificate().getPublicKey();

        Assert.assertTrue(new KeySpec.Rsa(3072).matches(rsa3072));
        Assert.assertFalse(new KeySpec.Rsa(3072).matches(rsa2048));
        Assert.assertFalse(new KeySpec.Rsa(3072).matches(p256));
        Assert.assertTrue(new KeySpec.Ecdsa("secp256r1").matches(p256));
        Assert.assertFalse(new KeySpec.Ecdsa("secp384r1").matches(p256));
        Assert.assertFalse(new KeySpec.Ecdsa("secp256r1").matches(rsa3072));
    }
}
