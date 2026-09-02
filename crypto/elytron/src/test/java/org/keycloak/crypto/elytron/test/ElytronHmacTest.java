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

import java.security.SecureRandom;
import java.util.UUID;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.keycloak.jose.HmacTest;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.HMACProvider;

import org.junit.Assert;
import org.junit.Test;


/**
 * @author <a href="mailto:david.anderson@redhat.com">David Anderson</a>
 */
public class ElytronHmacTest extends HmacTest {

    @Test
    public void testHmacSignaturesUsingKeyGen() throws Exception {
        
        KeyGenerator keygen = KeyGenerator.getInstance("HmacSHA256");
        SecureRandom random = isWindows() ? SecureRandom.getInstance("Windows-PRNG") : SecureRandom.getInstance("NativePRNG");
        random.setSeed(UUID.randomUUID().toString().getBytes());
        keygen.init(random);
        SecretKey secret = keygen.generateKey();

        String encoded = new JWSBuilder().content("12345678901234567890".getBytes())
                .hmac256(secret);
        System.out.println("length: " + encoded.length());
        JWSInput input = new JWSInput(encoded);
        Assert.assertTrue(HMACProvider.verify(input, secret));
    }
    private boolean isWindows(){
        return System.getProperty("os.name").startsWith("Windows");
    }
}
