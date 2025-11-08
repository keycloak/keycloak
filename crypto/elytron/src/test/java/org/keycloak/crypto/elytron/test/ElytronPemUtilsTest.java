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

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.PemUtilsProvider;
import org.keycloak.rule.CryptoInitRule;

import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ElytronPemUtilsTest {

    @ClassRule
    public static CryptoInitRule cinit = new CryptoInitRule();

    @Test
        public void testGenerateThumbprintSha1() throws NoSuchAlgorithmException {
        String[] test = new String[] {"abcdefg"};
        String encoded = org.keycloak.common.util.PemUtils.generateThumbprint(test, "SHA-1");
        assertEquals(27, encoded.length());
    }

    @Test
    public void testGenerateThumbprintSha256() throws NoSuchAlgorithmException {
        String[] test = new String[] {"abcdefg"};
        String encoded = org.keycloak.common.util.PemUtils.generateThumbprint(test, "SHA-256");
        assertEquals(43, encoded.length());
    }


    @Test
    public void testenocdedecode() throws NoSuchAlgorithmException, NoSuchProviderException {
        PemUtilsProvider pemutil = CryptoIntegration.getProvider().getPemUtils();

        KeyPair keypair = CryptoIntegration.getProvider().getKeyPairGen("RSA").generateKeyPair();
        String pem = pemutil.encodeKey(keypair.getPrivate());

        Object decodekey = pemutil.decodePrivateKey(pem);

    }

    @Test
    public void testtrkey() {
        String key = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKtWsK5O0CtuBpnMvWG+HTG0vmZzujQ2o9WdheQu+BzCILcGMsbDW0YQaglpcO5JpGWWhubnckGGPHfdQ2/7nP9QwbiTK0FbGF41UqcvoaCqU1psxoV88s8IXyQCAqeyLv00yj6foqdJjxh5SZ5z+na+M7Y2OxIBVxYRAxWEnfUvAgMBAAECgYB+Y7yBWHIHF2qXGYi6CVvPxtyNBuFcktHYShLyeBNeY3VujYv3QzSZQpJ1zuoXXQuARMHOovyNiVAhu357pMfx9wSkoKNSXKrQx/+9Vt9lI1pXJxjXedPOjbuI/JZAcrk0u4nOfXG/HGtR5cjoDZYWkYQEtsePCnHlZAb0D7axwQJBAO92f00Tvkc9NU/EGqwR3bPXRMqSX0JnG7XRBvLeJBCZYsQn0s2bLdpy8qsTeAyJg1ZvrEc8qIio5HVqzsvbhpMCQQC3K9A6UK+vmQCNWqsQpdqWPRPN7CPB67FzSmyS8CtMjY6jTvSHrkamggotz2N/5QDr1xG2q7A/3dpkq1bTpTx1AkAXZjjiSz+Yrn57IOqKTeSgIjTypoLwdirbBWXsbZCQnqxsBogu1y8P3ZOg6/IbJ4TR+W+YNnExiW9pmdpDSVxJAkEAplTq6YmLf/F4RuQmox94tyUPbtcYQWg942uZ3HSrXQDOng18kBj5nwpHJAJHYEQb6g2K0E5n5hcX0oKkfdx2YQJAcSKAmFiD7KQ6+vVqJlQwVPvYdTSOeZB7YVV6S4b4slS3ZObsa0yNMWgal/QnCtW5k3f185gCWj6dOLGB5btfxg==";

        PemUtilsProvider pemutil = CryptoIntegration.getProvider().getPemUtils();

        pemutil.decodePrivateKey(key);
    }
}
