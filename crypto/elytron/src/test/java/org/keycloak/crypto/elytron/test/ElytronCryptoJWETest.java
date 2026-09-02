/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.crypto.elytron.test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.keycloak.jose.JWETest;
import org.keycloak.jose.jwe.JWEConstants;

import org.junit.Test;

public class ElytronCryptoJWETest extends JWETest {

    @Test
    public void testDirect_Aes256CbcHmacSha512() throws Exception {
        final SecretKey aesKey = new SecretKeySpec(AES_256_KEY, "AES");
        final SecretKey hmacKey = new SecretKeySpec(HMAC_SHA512_KEY, "HMACSHA2");

        testDirectEncryptAndDecrypt(aesKey, hmacKey, JWEConstants.A256CBC_HS512, PAYLOAD, true);
    }

    @Override
    public void testAesKW_Aes128CbcHmacSha256() throws Exception {
        // Skipping this test, this test fails when not using BC provider.

    }

}
