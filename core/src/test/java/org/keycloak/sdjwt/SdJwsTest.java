/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.sdjwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.keycloak.common.VerificationException;
import org.keycloak.rule.CryptoInitRule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public abstract class SdJwsTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    static TestSettings testSettings = TestSettings.getInstance();

    private JsonNode createPayload() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("sub", "test");
        node.put("exp", Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond());
        node.put("name", "Test User");
        return node;
    }

    @Test
    public void testVerifySignature_Positive() throws Exception {
        SdJws sdJws = new SdJws(createPayload(), testSettings.holderSigContext, "jwt") {
        };
        sdJws.verifySignature(testSettings.holderVerifierContext);
    }

    @Test
    public void testVerifySignature_WrongPublicKey() {
        SdJws sdJws = new SdJws(createPayload(), testSettings.holderSigContext, "jwt") {
        };
        assertThrows(VerificationException.class, () -> sdJws.verifySignature(testSettings.issuerVerifierContext));
    }

    @Test
    public void testPayloadJwsConstruction() {
        SdJws sdJws = new SdJws(createPayload()) {
        };
        assertNotNull(sdJws.getPayload());
    }

    @Test(expected = IllegalStateException.class)
    public void testUnsignedJwsConstruction() {
        SdJws sdJws = new SdJws(createPayload()) {
        };
        sdJws.toJws();
    }

    @Test
    public void testSignedJwsConstruction() {
        SdJws sdJws = new SdJws(createPayload(), testSettings.holderSigContext, "jwt") {
        };
        assertNotNull(sdJws.toJws());
    }



    @Test
    public void testVerifyIssClaim_Negative() {
        List<String> allowedIssuers = Arrays.asList(new String[]{"issuer1@sdjwt.com", "issuer2@sdjwt.com"});
        JsonNode payload = createPayload();
        ((ObjectNode) payload).put("iss", "unknown-issuer@sdjwt.com");
        SdJws sdJws = new SdJws(payload) {};
        VerificationException exception = assertThrows(VerificationException.class, () -> sdJws.verifyIssClaim(allowedIssuers));
        assertEquals("Unknown 'iss' claim value: unknown-issuer@sdjwt.com", exception.getMessage());
    }

    @Test
    public void testVerifyIssClaim_Positive() throws VerificationException {
        List<String> allowedIssuers = Arrays.asList(new String[]{"issuer1@sdjwt.com", "issuer2@sdjwt.com"});
        JsonNode payload = createPayload();
        ((ObjectNode) payload).put("iss", "issuer1@sdjwt.com");
        SdJws sdJws = new SdJws(payload) {};
        sdJws.verifyIssClaim(allowedIssuers);
    }

    @Test
    public void testVerifyVctClaim_Negative() {
        JsonNode payload = createPayload();
        ((ObjectNode) payload).put("vct", "IdentityCredential");
        SdJws sdJws = new SdJws(payload) {};
        VerificationException exception = assertThrows(VerificationException.class, () -> sdJws.verifyVctClaim(Collections.singletonList("PassportCredential")));
        assertEquals("Unknown 'vct' claim value: IdentityCredential", exception.getMessage());
    }

    @Test
    public void testVerifyVctClaim_Positive() throws VerificationException {
        JsonNode payload = createPayload();
        ((ObjectNode) payload).put("vct", "IdentityCredential");
        SdJws sdJws = new SdJws(payload) {};
        sdJws.verifyVctClaim(Collections.singletonList("IdentityCredential"));
    }
}
