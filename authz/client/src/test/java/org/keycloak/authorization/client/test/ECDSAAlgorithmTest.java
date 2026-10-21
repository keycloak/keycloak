/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authorization.client.test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;

import org.keycloak.authorization.client.util.crypto.AuthzClientCryptoProvider;
import org.keycloak.crypto.ECDSAAlgorithm;
import org.keycloak.crypto.JavaAlgorithm;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
public class ECDSAAlgorithmTest {

    private final KeyPair keyPair;

    public ECDSAAlgorithmTest() throws Exception {
        keyPair = KeyPairGenerator.getInstance("EC").genKeyPair();
    }


    private void test(ECDSAAlgorithm algorithm) throws Exception {
        AuthzClientCryptoProvider prov = new AuthzClientCryptoProvider();
        byte[] data = "Something to sign".getBytes(StandardCharsets.UTF_8);
        Signature signature = Signature.getInstance(JavaAlgorithm.getJavaAlgorithm(algorithm.name()));
        signature.initSign(keyPair.getPrivate());
        signature.update(data);
        byte[] sign = signature.sign();
        byte[] rsConcat = prov.getEcdsaCryptoProvider().asn1derToConcatenatedRS(sign, algorithm.getSignatureLength());
        byte[] asn1Des = prov.getEcdsaCryptoProvider().concatenatedRSToASN1DER(rsConcat, algorithm.getSignatureLength());
        byte[] rsConcat2 = prov.getEcdsaCryptoProvider().asn1derToConcatenatedRS(asn1Des, algorithm.getSignatureLength());
        Assert.assertArrayEquals(rsConcat, rsConcat2);
    }

    @Test
    public void testES256() throws Exception {
        test(ECDSAAlgorithm.ES256);
    }

    @Test
    public void testES384() throws Exception {
        test(ECDSAAlgorithm.ES384);
    }

    @Test
    public void testES512() throws Exception {
        test(ECDSAAlgorithm.ES512);
    }
}
