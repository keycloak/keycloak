/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jose;

import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.HMACProvider;
import org.keycloak.rule.CryptoInitRule;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class HmacTest {

    private final Logger logger = Logger.getLogger(getClass().getName());

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void testHmacSignaturesWithRandomSecretKey() throws Exception {
        SecretKey secretKey = new SecretKeySpec(UUID.randomUUID().toString().getBytes(), "HmacSHA256");
        testHMACSignAndVerify(secretKey, "testHmacSignaturesWithRandomSecretKey");
    }

    @Test
    public void testHmacSignaturesWithShortSecretKey() throws Exception {
        SecretKey secretKey = new SecretKeySpec("secret".getBytes(), "HmacSHA256");
        testHMACSignAndVerify(secretKey, "testHmacSignaturesWithShortSecretKey");
    }

    protected void testHMACSignAndVerify(SecretKey secretKey, String test) throws Exception {
        String encoded = new JWSBuilder().content("12345678901234567890".getBytes())
                .hmac256(secretKey);
        logger.infof("%s: Length of encoded content: %d, Length of secret key: %d", test, encoded.length(), secretKey.getEncoded().length);
        JWSInput input = new JWSInput(encoded);
        Assert.assertTrue(HMACProvider.verify(input, secretKey));
    }

}
