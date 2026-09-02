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
package org.keycloak.crypto.def.test;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Collection;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.crypto.def.BCECDSACryptoProvider;
import org.keycloak.rule.CryptoInitRule;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class BCECDSACryptoProviderTest {


    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"secp256r1"}, {"secp384r1"}, {"secp521r1"}
        });
    }

    private String curve;

    public BCECDSACryptoProviderTest(String curve) {
        this.curve = curve;
    }

    @Test
    public void getPublicFromPrivate() {
        KeyPair testKey = generateECKey(curve);

        BCECDSACryptoProvider bcecdsaCryptoProvider = new BCECDSACryptoProvider();
        assertEquals("The derived key should be equal to the originally generated one.",
                testKey.getPublic(),
                bcecdsaCryptoProvider.getPublicFromPrivate((ECPrivateKey) testKey.getPrivate()));

    }

    public static KeyPair generateECKey(String curve) {

        try {
            KeyPairGenerator kpg = CryptoIntegration.getProvider().getKeyPairGen("ECDSA");
            ECGenParameterSpec parameterSpec = new ECGenParameterSpec(curve);
            kpg.initialize(parameterSpec);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }
}