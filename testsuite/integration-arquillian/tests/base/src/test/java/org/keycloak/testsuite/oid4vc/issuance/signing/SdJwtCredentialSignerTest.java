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

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

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
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.LDCredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.SdJwtCredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.SdJwtCredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.signing.CredentialSignerException;
import org.keycloak.protocol.oid4vc.issuance.signing.SdJwtCredentialSigner;
import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.sdjwt.SdJwtUtils;
import org.keycloak.testsuite.runonserver.RunOnServerException;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_SD;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_SD_HASH_ALGORITHM;
import static org.keycloak.OID4VCConstants.SDJWT_DELIMITER;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SdJwtCredentialSignerTest extends OID4VCTest {

    private static final KeyWrapper rsaKey = getRsaKey();

    @Test(expected = CredentialSignerException.class)
    public void testUnsupportedCredentialBody() throws Throwable {
        try {
            getTestingClient()
                    .server(TEST_REALM_NAME)
                    .run(session -> new SdJwtCredentialSigner(session).signCredential(
                            new LDCredentialBody(getTestCredential(Map.of())),
                            new CredentialBuildConfig()
                    ));
        } catch (RunOnServerException ros) {
            throw ros.getCause();
        }
    }

    // If an unsupported algorithm is provided, signing should reliably fail.
    @Test(expected = CredentialSignerException.class)
    public void testUnsupportedAlgorithm() throws Throwable {
        try {
            getTestingClient()
                    .server(TEST_REALM_NAME)
                    .run(session ->
                            testSignSDJwtCredential(
                                    session,
                                    getKeyIdFromSession(session),
                                    null,
                                    "unsupported-algorithm",
                                    Map.of(),
                                    0,
                                    List.of()));
        } catch (RunOnServerException ros) {
            throw ros.getCause();
        }
    }

    // If an unknown key is provided, signing should reliably fail.
    @Test(expected = CredentialSignerException.class)
    public void testFailIfNoKey() throws Throwable {
        try {
            getTestingClient()
                    .server(TEST_REALM_NAME)
                    .run(session ->
                            testSignSDJwtCredential(
                                    session,
                                    "no-such-key",
                                    null,
                                    Algorithm.RS256,
                                    Map.of(),
                                    0,
                                    List.of()));
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
                                getKeyIdFromSession(session),
                                null,
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
                                getKeyIdFromSession(session),
                                null,
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
                                getKeyIdFromSession(session),
                                null,
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
                                getKeyIdFromSession(session),
                                "did:web:test.org#key-id",
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
                                getKeyIdFromSession(session),
                                null,
                                Algorithm.RS256,
                                Map.of(),
                                0,
                                List.of()));
    }

    public static void testSignSDJwtCredential(KeycloakSession session, String signingKeyId, String overrideKeyId, String
            algorithm, Map<String, Object> claims, int decoys, List<String> visibleClaims) {
        CredentialBuildConfig credentialBuildConfig = new CredentialBuildConfig()
                .setCredentialIssuer(TEST_DID.toString())
                .setCredentialType("https://credentials.example.com/test-credential")
                .setTokenJwsType("example+sd-jwt")
                .setHashAlgorithm("sha-256")
                .setNumberOfDecoys(decoys)
                .setSdJwtVisibleClaims(visibleClaims)
                .setSigningKeyId(signingKeyId)
                .setSigningAlgorithm(algorithm)
                .setOverrideKeyId(overrideKeyId);

        SdJwtCredentialSigner sdJwtCredentialSigner = new SdJwtCredentialSigner(session);

        VerifiableCredential testCredential = getTestCredential(claims);
        SdJwtCredentialBody sdJwtCredentialBody = new SdJwtCredentialBuilder()
                .buildCredentialBody(testCredential, credentialBuildConfig);

        String sdJwt = sdJwtCredentialSigner.signCredential(sdJwtCredentialBody, credentialBuildConfig);

        KeyWrapper keyWrapper = getKeyFromSession(session);
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
        String[] splittedSdToken = sdJwt.split(SDJWT_DELIMITER);
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
            assertEquals("The type should be included", "https://credentials.example.com/test-credential", theToken.getOtherClaims().get("vct"));
            List<String> sds = (List<String>) theToken.getOtherClaims().get(CLAIM_NAME_SD);
            if (sds != null && !sds.isEmpty()) {
                assertEquals("The algorithm should be included", "sha-256", theToken.getOtherClaims().get(CLAIM_NAME_SD_HASH_ALGORITHM));
            }
            List<String> disclosed = Arrays.asList(splittedSdToken).subList(1, splittedSdToken.length);
            int numSds = sds != null ? sds.size() : 0;
            assertEquals("All undisclosed claims and decoys should be provided.", disclosed.size() + decoys, numSds);
            verifyDisclosures(sds, disclosed);

            visibleClaims
                    .forEach(vc -> assertTrue("The visible claims should be present within the token.", theToken.getOtherClaims().containsKey(vc)));
        } catch (VerificationException e) {
            fail("Was not able to extract the token.");
        }
    }

    private static void verifyDisclosures(List<String> undisclosed, List<String> disclosedList) {
        disclosedList.stream()
                .map(disclosed -> new String(Base64.getUrlDecoder().decode(disclosed)))
                .map(disclosedString -> {
                    try {
                        return JsonSerialization.mapper.readValue(disclosedString, List.class);
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
