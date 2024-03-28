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

package org.keycloak.testsuite.oid4vc.issuance.signing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.AsymmetricSignatureVerifierContext;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.ServerECDSASignatureVerifierContext;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.signing.JwtSigningService;
import org.keycloak.protocol.oid4vc.issuance.signing.SdJwtSigningService;
import org.keycloak.protocol.oid4vc.issuance.signing.SigningServiceException;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.sdjwt.SdJwtUtils;
import org.keycloak.testsuite.runonserver.RunOnServerException;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SdJwtSigningServiceTest extends OID4VCTest {

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static KeyWrapper rsaKey = getRsaKey();

    // If an unsupported algorithm is provided, the JWT Sigining Service should not be instantiated.
    @Test(expected = SigningServiceException.class)
    public void testUnsupportedAlgorithm() throws Throwable {
        try {
            getTestingClient()
                    .server(TEST_REALM_NAME)
                    .run(session ->
                            new SdJwtSigningService(
                                    session,
                                    new ObjectMapper(),
                                    getKeyFromSession(session).getKid(),
                                    "unsupported-algorithm",
                                    "JWT",
                                    "sha-256",
                                    "did:web:test.org",
                                    0,
                                    List.of(),
                                    new StaticTimeProvider(1000),
                                    Optional.empty()));
        } catch (RunOnServerException ros) {
            throw ros.getCause();
        }
    }

    // If no key is provided, the JWT Sigining Service should not be instantiated.
    @Test(expected = SigningServiceException.class)
    public void testFailIfNoKey() throws Throwable {
        try {
            getTestingClient()
                    .server(TEST_REALM_NAME)
                    .run(session ->
                            new JwtSigningService(
                                    session,
                                    "no-such-key",
                                    Algorithm.RS256,
                                    "JWT",
                                    "did:web:test.org",
                                    new StaticTimeProvider(1000)));
        } catch (RunOnServerException ros) {
            throw ros.getCause();
        }
    }

    @Test
    public void testRsaSignedCredentialWithClaims() {
        getTestingClient()
                .server(TEST_REALM_NAME)
                .run(session ->
                        testSignSDJwtCredential(
                                session,
                                Optional.empty(),
                                Algorithm.RS256,
                                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                        "test", "test",
                                        "arrayClaim", List.of("a", "b", "c")),
                                0,
                                List.of()));
    }

    @Test
    public void testRsaSignedCredentialWithVisibleClaims() {
        getTestingClient()
                .server(TEST_REALM_NAME)
                .run(session ->
                        testSignSDJwtCredential(
                                session,
                                Optional.empty(),
                                Algorithm.RS256,
                                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                        "test", "test",
                                        "arrayClaim", List.of("a", "b", "c")),
                                0,
                                List.of("test")));
    }


    @Test
    public void testRsaSignedCredentialWithClaimsAndDecoys() {
        getTestingClient()
                .server(TEST_REALM_NAME)
                .run(session ->
                        testSignSDJwtCredential(
                                session,
                                Optional.empty(),
                                Algorithm.RS256,
                                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                        "test", "test",
                                        "arrayClaim", List.of("a", "b", "c")),
                                6,
                                List.of()));
    }

    @Test
    public void testRsaSignedCredentialWithKeyId() {
        getTestingClient()
                .server(TEST_REALM_NAME)
                .run(session ->
                        testSignSDJwtCredential(
                                session,
                                Optional.of("did:web:test.org#key-id"),
                                Algorithm.RS256,
                                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                        "test", "test",
                                        "arrayClaim", List.of("a", "b", "c")),
                                0,
                                List.of()));
    }

    @Test
    public void testRsaSignedCredentialWithoutAdditionalClaims() {
        getTestingClient()
                .server(TEST_REALM_NAME)
                .run(session ->
                        testSignSDJwtCredential(
                                session,
                                Optional.empty(),
                                Algorithm.RS256,
                                Map.of(),
                                0,
                                List.of()));
    }

    public static void testSignSDJwtCredential(KeycloakSession session, Optional<String> keyId, String
            algorithm, Map<String, Object> claims, int decoys, List<String> visibleClaims) {
        KeyWrapper keyWrapper = getKeyFromSession(session);

        SdJwtSigningService signingService = new SdJwtSigningService(
                session,
                new ObjectMapper(),
                keyWrapper.getKid(),
                algorithm,
                "vc+sd-jwt",
                "sha-256",
                "did:web:test.org",
                decoys,
                visibleClaims,
                new StaticTimeProvider(1000),
                keyId);

        VerifiableCredential testCredential = getTestCredential(claims);

        String sdJwt = signingService.signCredential(testCredential);
        SignatureVerifierContext verifierContext = null;
        switch (algorithm) {
            case Algorithm.ES256: {
                verifierContext = new ServerECDSASignatureVerifierContext(keyWrapper);
                break;
            }
            case Algorithm.RS256: {
                verifierContext = new AsymmetricSignatureVerifierContext(keyWrapper);
                break;
            }
            default: {
                fail("Algorithm not supported.");
            }
        }
        // the sd-jwt is dot-concatenated header.payload.signature~disclosure1~___~disclosureN
        // we first split the disclosuers
        String[] splittedSdToken = sdJwt.split("~");
        // and then split the actual token part
        String[] splittedToken = splittedSdToken[0].split("\\.");

        String jwt = new StringJoiner(".")
                // header
                .add(splittedToken[0])
                // payload
                .add(splittedToken[1])
                // signature
                .add(splittedToken[2])
                .toString();
        TokenVerifier<JsonWebToken> verifier = TokenVerifier
                .create(jwt, JsonWebToken.class)
                .verifierContext(verifierContext);
        verifier.publicKey((PublicKey) keyWrapper.getPublicKey());
        try {
            verifier.verify();
        } catch (VerificationException e) {
            fail("The credential should successfully be verified.");
        }
        try {
            JsonWebToken theToken = verifier.getToken();

            assertEquals("The issuer should be set in the token.", TEST_DID.toString(), theToken.getIssuer());
            assertEquals("The credential ID should be set as the token ID.", testCredential.getId().toString(), theToken.getId());
            assertEquals("The type should be included", TEST_TYPES.get(0), theToken.getOtherClaims().get("vct"));

            assertEquals("The nbf date should be included", TEST_ISSUANCE_DATE.toInstant().getEpochSecond(), theToken.getNbf().longValue());

            List<String> sds = (List<String>) theToken.getOtherClaims().get("_sd");
            if (sds != null && !sds.isEmpty()){
                assertEquals("The algorithm should be included", "sha-256", theToken.getOtherClaims().get("_sd_alg"));
            }
            List<String> disclosed = Arrays.asList(splittedSdToken).subList(1, splittedSdToken.length);
            int numSds = sds != null ? sds.size() : 0;
            assertEquals("All undisclosed claims and decoys should be provided.", disclosed.size() + decoys, numSds);
            verifyDisclosures(sds, disclosed);

            visibleClaims
                    .forEach(vc -> assertTrue("The visible claims should be present within the token.",theToken.getOtherClaims().containsKey(vc)));
        } catch (VerificationException e) {
            fail("Was not able to extract the token.");
        }
    }

    private static void verifyDisclosures(List<String> undisclosed, List<String> disclosedList) {
        disclosedList.stream()
                .map(disclosed -> new String(Base64.getUrlDecoder().decode(disclosed)))
                .map(disclosedString -> {
                    try {
                        return OBJECT_MAPPER.readValue(disclosedString, List.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(dl -> new DisclosedClaim((String) dl.get(0), (String) dl.get(1), dl.get(2)))
                .forEach(dc -> assertTrue("Every disclosure claim should be provided in the undisclosures.", undisclosed.contains(dc.getHash())));

    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        if (testRealm.getComponents() != null) {
            testRealm.getComponents().add("org.keycloak.keys.KeyProvider", getRsaKeyProvider(rsaKey));
        } else {
            testRealm.setComponents(new MultivaluedHashMap<>(
                    Map.of("org.keycloak.keys.KeyProvider", List.of(getRsaKeyProvider(rsaKey)))));
        }
    }

    static class DisclosedClaim {
        private final String salt;
        private final String key;
        private final Object value;
        private final String hash;

        DisclosedClaim(String salt, String key, Object value) {
            this.salt = salt;
            this.key = key;
            this.value = value;
            this.hash = createHash(salt, key, value);
        }

        private String createHash(String salt, String key, Object value) {
            try {
                return SdJwtUtils.encodeNoPad(
                        HashUtils.hash("sha-256",
                                SdJwtUtils.encodeNoPad(
                                        SdJwtUtils.printJsonArray(List.of(salt, key, value).toArray()).getBytes()).getBytes()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        }

        public String getSalt() {
            return salt;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        public String getHash() {
            return hash;
        }
    }

}
