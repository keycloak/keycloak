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
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.JsonSerialization;

import org.junit.Assert;
import org.junit.Test;

public class JWSHeaderTest {

    @Test
    public void knownAlgorithm() throws Exception {
        JWSHeader header = JsonSerialization.readValue("{\"alg\":\"ES256\",\"typ\":\"JWT\"}", JWSHeader.class);
        Assert.assertEquals(Algorithm.ES256, header.getAlgorithm());
        Assert.assertEquals("ES256", header.getRawAlgorithm());
        Assert.assertEquals("{\"alg\":\"ES256\",\"typ\":\"JWT\"}", JsonSerialization.writeValueAsString(header));
    }

    @Test
    public void customAlgorithm() throws Exception {
        JWSHeader header = JsonSerialization.readValue("{\"alg\":\"CUSTOM256\",\"typ\":\"JWT\",\"kid\":\"k1\"}", JWSHeader.class);
        Assert.assertNull(header.getAlgorithm());
        Assert.assertEquals("CUSTOM256", header.getRawAlgorithm());
        Assert.assertEquals("k1", header.getKeyId());
        Assert.assertEquals("{\"alg\":\"CUSTOM256\",\"typ\":\"JWT\",\"kid\":\"k1\"}", JsonSerialization.writeValueAsString(header));
    }

    @Test
    public void customAlgorithmInToken() throws Exception {
        String token = encode("{\"alg\":\"CUSTOM256\",\"typ\":\"JWT\"}") + "." + encode("{\"sub\":\"user\"}") + "." + encode("signature");

        JWSInput input = new JWSInput(token);
        Assert.assertEquals("CUSTOM256", input.getHeader().getRawAlgorithm());

        TokenVerifier<AccessToken> verifier = TokenVerifier.create(token, AccessToken.class)
                .publicKey(KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic());
        Assert.assertEquals("user", verifier.getToken().getSubject());
        VerificationException e = Assert.assertThrows(VerificationException.class, verifier::verifySignature);
        Assert.assertEquals("Unknown or unsupported token algorithm", e.getMessage());
    }

    private static String encode(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
}
