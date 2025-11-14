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

package org.keycloak.testsuite.oauth;

import org.junit.Test;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.testsuite.util.KeyUtils;

public class ClientAuthMLDSASignedJWTTest extends AbstractClientAuthSignedJWTTest {

    @Test
    public void testCodeToTokenRequestSuccessMLDSA65usingJwksUri() throws Exception {
        testCodeToTokenRequestSuccess(Algorithm.MLDSA65, null, true);
    } // TODO HTTP-Error 401

    @Test
    public void testCodeToTokenRequestSuccessMLDSA65usingJwks() throws Exception {
        testCodeToTokenRequestSuccess(Algorithm.MLDSA65, null, false);
    } // TODO HTTP-Error 401

    @Test
    public void testUploadCertificatePemMLDSA65() throws Exception {
        testUploadCertificatePEM(KeyUtils.generateMLDSAKey(Algorithm.MLDSA65), Algorithm.MLDSA65, null);
    } // TODO HTTP-Error 500

    @Test
    public void testUploadPublicKeyPemMLDSA65() throws Exception {
        testUploadPublicKeyPem(KeyUtils.generateMLDSAKey(Algorithm.MLDSA65), Algorithm.MLDSA65, null);
    } // TODO HTTP-Error 500

    @Override
    protected String getKeyAlgorithmFromJwaAlgorithm(String jwaAlgorithm, String curve) {
        if (!JavaAlgorithm.isMldsaJavaAlgorithm(jwaAlgorithm)) {
            throw new RuntimeException("Unsupported signature algorithm: " + jwaAlgorithm);
        }
        if (curve != null && !curve.isEmpty()) {
            throw new RuntimeException("Unsupported signature curve: " + curve);
        }
        return jwaAlgorithm;
    }
}
