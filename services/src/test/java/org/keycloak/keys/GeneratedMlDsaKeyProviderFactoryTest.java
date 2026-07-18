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

package org.keycloak.keys;

import java.security.KeyPair;
import java.util.Base64;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.crypto.Algorithm;
import org.keycloak.rule.CryptoInitRule;

import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GeneratedMlDsaKeyProviderFactoryTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void determinesExactAlgorithmFromPublicKey() throws Exception {
        for (String algorithm : new String[] { Algorithm.ML_DSA_44, Algorithm.ML_DSA_65, Algorithm.ML_DSA_87 }) {
            KeyPair keyPair = CryptoIntegration.getProvider().getKeyPairGen(algorithm).generateKeyPair();
            String encodedPublicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

            assertEquals(algorithm, GeneratedMlDsaKeyProviderFactory.getAlgorithmFromPublicKey(encodedPublicKey));
        }
    }
}
