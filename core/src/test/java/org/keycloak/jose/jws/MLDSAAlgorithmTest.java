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

package org.keycloak.jose.jws;

import java.nio.charset.StandardCharsets;

import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MLDSAAlgorithmTest {

    private static final String[] ALGORITHMS = {
            org.keycloak.crypto.Algorithm.ML_DSA_44,
            org.keycloak.crypto.Algorithm.ML_DSA_65,
            org.keycloak.crypto.Algorithm.ML_DSA_87
    };

    @Test
    public void parseMLDSAProtectedHeaders() throws Exception {
        for (String algorithm : ALGORITHMS) {
            String header = Base64Url.encode(("{\"alg\":\"" + algorithm + "\"}").getBytes(StandardCharsets.UTF_8));
            JWSInput input = new JWSInput(header + ".e30.");

            assertEquals(algorithm, input.getHeader().getRawAlgorithm());
        }
    }

    @Test
    public void mapMLDSAToJcaAndAkp() {
        for (String algorithm : ALGORITHMS) {
            assertEquals(algorithm, JavaAlgorithm.getJavaAlgorithm(algorithm));
            assertEquals(KeyType.AKP, JavaAlgorithm.getKeyType(algorithm));
        }
    }

    @Test
    public void mapMLDSAToShake256() {
        for (String algorithm : ALGORITHMS) {
            assertEquals(JavaAlgorithm.SHAKE256, JavaAlgorithm.getJavaAlgorithmForHash(algorithm));
        }
    }
}
