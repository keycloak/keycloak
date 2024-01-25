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

package org.keycloak.protocol.oid4vc.issuance.signing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.AsymmetricSignatureVerifierContext;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.ServerECDSASignatureVerifierContext;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;

import java.security.PublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class JwtSigningServiceTest extends SigningServiceTest {

    @BeforeEach
    public void setup() {
        CryptoIntegration.init(this.getClass().getClassLoader());
    }

    @DisplayName("If an unsupported algorithm is provided, the JWT Sigining Service should not be instantiated.")
    @Test
    public void testUnsupportedAlgorithm() {
        assertThrows(SigningServiceException.class,
                () -> new JwtSigningService(
                        getMockSession(getRsaKey("my-rsa-key")),
                        "my-rsa-key",
                        "did:web:test.org",
                        "unsupported-algorithm",
                        new StaticTimeProvider(1000)),
                "The service should not be instantiated, when an unsupported algorithm is provided.");
    }

    @DisplayName("If no key is provided, the JWT Sigining Service should not be instantiated.")
    @Test
    public void testFailIfNoKey() {
        assertThrows(SigningServiceException.class,
                () -> new JwtSigningService(
                        getMockSession(getRsaKey("my-rsa-key")),
                        "no-such-key",
                        Algorithm.RS256,
                        "did:web:test.org",
                        new StaticTimeProvider(1000)),
                "The service should not be instantiated, when no sigining key is provided.");
    }

    @DisplayName("The provided credentials should be successfully signed as a JWT-VC.")
    @ParameterizedTest
    @MethodSource("getKeys")
    public void testSignJwtCredential(KeyWrapper keyWrapper, String algorithm, Map<String, Object> claims) throws Exception {

        JwtSigningService jwtSigningService = new JwtSigningService(
                getMockSession(keyWrapper),
                keyWrapper.getKid(),
                algorithm,
                "did:web:test.org",
                new StaticTimeProvider(1000));

        var testCredential = getTestCredential(claims);

        String jwtCredential = jwtSigningService.signCredential(testCredential);

        SignatureVerifierContext verifierContext = switch (algorithm) {
            case Algorithm.ES256 -> new ServerECDSASignatureVerifierContext(keyWrapper);
            case Algorithm.RS256 -> new AsymmetricSignatureVerifierContext(keyWrapper);
            default -> fail("Algorithm not supported.");
        };

        var verifier = TokenVerifier
                .create(jwtCredential, JsonWebToken.class)
                .verifierContext(verifierContext);
        verifier.publicKey((PublicKey) keyWrapper.getPublicKey());
        try {
            verifier.verify();
        } catch (VerificationException e) {
            fail("The credential should successfully be verified.", e);
        }

        var theToken = verifier.getToken();

        assertEquals(TEST_EXPIRATION_DATE.toInstant().getEpochSecond(), theToken.getExp(), "JWT claim in JWT encoded VC or VP MUST be used to set the value of the “expirationDate” of the VC");
        assertEquals(TEST_ISSUANCE_DATE.toInstant().getEpochSecond(), theToken.getNbf(), "VC Data Model v1.1 specifies that “issuanceDate” property MUST be represented as an nbf JWT claim, and not iat JWT claim.");
        assertEquals(TEST_DID.toString(), theToken.getIssuer(), "The issuer should be set in the token.");
        assertEquals(testCredential.getId().toString(), theToken.getId(), "The credential ID should be set as the token ID.");
        Optional.ofNullable(testCredential.getCredentialSubject().getClaims().get("id")).ifPresent(id -> assertEquals(id.toString(), theToken.getSubject(), "If the credentials subject id is set, it should be set as the token subject."));

        assertNotNull(theToken.getOtherClaims().get("vc"), "The credentials should be included at the vc-claim.");
        var credential = new ObjectMapper().convertValue(theToken.getOtherClaims().get("vc"), VerifiableCredential.class);
        assertEquals(credential.getType(), TEST_TYPES, "The types should be included");
        assertEquals(credential.getIssuer(), TEST_DID, "The issuer should be included");
        assertEquals(credential.getExpirationDate(), TEST_EXPIRATION_DATE, "The expiration date should be included");
        assertEquals(credential.getIssuanceDate(), TEST_ISSUANCE_DATE, "The issuance date should be included");

        var subject = credential.getCredentialSubject();
        claims.forEach((k, v) -> assertEquals(v, subject.getClaims().get(k), String.format("All additional claims should be set - %s is incorrect", k)));

    }

    public static Stream<Arguments> getKeys() {
        return Stream.of(
                Arguments.of(
                        getRsaKey("my-rsa-key"),
                        Algorithm.RS256,
                        Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                "test", "test",
                                "arrayClaim", List.of("a", "b", "c"))),
                Arguments.of(
                        getECKey("my-ec-key"),
                        Algorithm.ES256,
                        Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                "test", "test",
                                "arrayClaim", List.of("a", "b", "c"))),
                Arguments.of(
                        getECKey("my-ec-key"),
                        Algorithm.ES256,
                        Map.of())
        );
    }


} 