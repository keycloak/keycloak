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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.rule.CryptoInitRule;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        SdJws<JsonNode> sdJws = createSdJws(createPayload(), testSettings.holderSigContext, "jwt");
        sdJws.verifySignature(testSettings.holderVerifierContext);
    }

    @Test
    public void testVerifySignature_WrongPublicKey() {
        SdJws<JsonNode> sdJws = createSdJws(createPayload(), testSettings.holderSigContext, "jwt");
        assertThrows(VerificationException.class, () -> sdJws.verifySignature(testSettings.issuerVerifierContext));
    }

    @Test
    public void testPayloadJwsConstruction() {
        SdJws<JsonNode> sdJws = createSdJws(createPayload());
        assertNotNull(sdJws.getPayload());
    }

    @Test(expected = IllegalStateException.class)
    public void testUnsignedJwsConstruction() {
        SdJws<JsonNode> sdJws = createSdJws(createPayload());
        sdJws.toJws();
    }

    @Test
    public void testSignedJwsConstruction() {
        SdJws<JsonNode> sdJws = createSdJws(createPayload(), testSettings.holderSigContext, "jwt");
        assertNotNull(sdJws.toJws());
    }



    @Test
    public void testVerifyIssClaim_Negative() {
        List<String> allowedIssuers = Arrays.asList(new String[]{"issuer1@sdjwt.com", "issuer2@sdjwt.com"});
        JsonNode payload = createPayload();
        ((ObjectNode) payload).put("iss", "unknown-issuer@sdjwt.com");
        SdJws<JsonNode> sdJws = createSdJws(payload);
        VerificationException exception = assertThrows(VerificationException.class, () -> sdJws.verifyIssClaim(allowedIssuers));
        assertEquals("Unknown 'iss' claim value: unknown-issuer@sdjwt.com", exception.getMessage());
    }

    @Test
    public void testVerifyIssClaim_Positive() throws VerificationException {
        List<String> allowedIssuers = Arrays.asList(new String[]{"issuer1@sdjwt.com", "issuer2@sdjwt.com"});
        JsonNode payload = createPayload();
        ((ObjectNode) payload).put("iss", "issuer1@sdjwt.com");
        SdJws<JsonNode> sdJws = createSdJws(payload);
        sdJws.verifyIssClaim(allowedIssuers);
    }

    @Test
    public void testVerifyVctClaim_Negative() {
        JsonNode payload = createPayload();
        ((ObjectNode) payload).put("vct", "IdentityCredential");
        SdJws<JsonNode> sdJws = createSdJws(payload);
        VerificationException exception = assertThrows(VerificationException.class, () -> sdJws.verifyVctClaim(Collections.singletonList("PassportCredential")));
        assertEquals("Unknown 'vct' claim value: IdentityCredential", exception.getMessage());
    }

    @Test
    public void testVerifyVctClaim_Positive() throws VerificationException {
        JsonNode payload = createPayload();
        ((ObjectNode) payload).put("vct", "IdentityCredential");
        SdJws<JsonNode> sdJws = createSdJws(payload);
        sdJws.verifyVctClaim(Collections.singletonList("IdentityCredential"));
    }

    private SdJws<JsonNode> createSdJws(JsonNode payload) {
        return new SdJws<JsonNode>(payload) {

            @Override
            protected String readClaim(JsonNode payload, String claimName) throws VerificationException {
                return SdJwtUtils.readClaim(payload, claimName);
            }

            @Override
            protected JsonNode readPayload(JWSInput jwsInput) {
                try {
                    return SdJwtUtils.mapper.readTree(jwsInput.getContent());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private SdJws<JsonNode> createSdJws(JsonNode payload, SignatureSignerContext signer, String jwsType) {
        return new SdJws<JsonNode>(payload, signer, jwsType) {

            @Override
            protected String readClaim(JsonNode payload, String claimName) throws VerificationException {
                return SdJwtUtils.readClaim(payload, claimName);
            }

            @Override
            protected JsonNode readPayload(JWSInput jwsInput) {
                try {
                    return SdJwtUtils.mapper.readTree(jwsInput.getContent());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
