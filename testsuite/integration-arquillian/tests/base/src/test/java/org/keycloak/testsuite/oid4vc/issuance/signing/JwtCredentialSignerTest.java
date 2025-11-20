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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.AsymmetricSignatureVerifierContext;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.ServerECDSASignatureVerifierContext;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.JwtCredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.LDCredentialBody;
import org.keycloak.protocol.oid4vc.issuance.signing.CredentialSignerException;
import org.keycloak.protocol.oid4vc.issuance.signing.JwtCredentialSigner;
import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.runonserver.RunOnServerException;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


public class JwtCredentialSignerTest extends OID4VCTest {

    private static final Logger LOGGER = Logger.getLogger(JwtCredentialSignerTest.class);

    private static final KeyWrapper rsaKey = getRsaKey();

    @Before
    public void setup() {
        CryptoIntegration.init(this.getClass().getClassLoader());
    }

    @Test(expected = CredentialSignerException.class)
    public void testUnsupportedCredentialBody() throws Throwable {
        try {
            getTestingClient()
                    .server(TEST_REALM_NAME)
                    .run(session -> new JwtCredentialSigner(session).signCredential(
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
                            testSignJwtCredential(
                                    session,
                                    getKeyIdFromSession(session),
                                    "unsupported-algorithm",
                                    Map.of())
                    );
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
                            testSignJwtCredential(
                                    session,
                                    "no-such-key",
                                    Algorithm.RS256,
                                    Map.of()));
        } catch (RunOnServerException ros) {
            throw ros.getCause();
        }
    }

    // The provided credentials should be successfully signed as a JWT-VC.
    @Test
    public void testRsaSignedCredentialWithOutIssuanceDate() {
        getTestingClient()
                .server(TEST_REALM_NAME)
                .run(session ->
                        testSignJwtCredential(
                                session,
                                getKeyIdFromSession(session),
                                Algorithm.RS256,
                                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                        "test", "test",
                                        "arrayClaim", List.of("a", "b", "c"))));
    }

    @Test
    public void testRsaSignedCredentialWithIssuanceDate() {
        getTestingClient()
                .server(TEST_REALM_NAME)
                .run(session ->
                        testSignJwtCredential(
                                session,
                                getKeyIdFromSession(session),
                                Algorithm.RS256,
                                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                        "test", "test",
                                        "arrayClaim", List.of("a", "b", "c"),
                                        "issuanceDate", Instant.ofEpochSecond(10))));
    }

    @Test
    public void testRsaSignedCredentialWithoutAdditionalClaims() {
        getTestingClient()
                .server(TEST_REALM_NAME)
                .run(session ->
                        testSignJwtCredential(
                                session,
                                getKeyIdFromSession(session),
                                Algorithm.RS256,
                                Map.of()));
    }


    public static void testSignJwtCredential(
            KeycloakSession session, String signingKeyId, String algorithm, Map<String, Object> claims) {
        CredentialBuildConfig credentialBuildConfig = new CredentialBuildConfig()
                .setCredentialIssuer(TEST_DID.toString())
                .setTokenJwsType("JWT")
                .setSigningKeyId(signingKeyId)
                .setSigningAlgorithm(algorithm);

        JwtCredentialSigner jwtCredentialSigner = new JwtCredentialSigner(session);

        VerifiableCredential testCredential = getTestCredential(claims);
        JwtCredentialBuilder builder = new JwtCredentialBuilder(
                new StaticTimeProvider(1000)
        );

        CredentialBody credentialBody = builder.buildCredentialBody(
                testCredential,
                credentialBuildConfig
        );

        String jwtCredential = jwtCredentialSigner.signCredential(credentialBody, credentialBuildConfig);

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

        TokenVerifier<JsonWebToken> verifier = TokenVerifier
                .create(jwtCredential, JsonWebToken.class)
                .verifierContext(verifierContext);
        verifier.publicKey((PublicKey) keyWrapper.getPublicKey());
        try {
            verifier.verify();
        } catch (VerificationException e) {
            fail("The credential should successfully be verified.");
        }

        try {
            JsonWebToken theToken = verifier.getToken();

            assertEquals("JWT claim in JWT encoded VC or VP MUST be used to set the value of the “expirationDate” of the VC", TEST_EXPIRATION_DATE.getEpochSecond(), theToken.getExp().longValue());
            if (claims.containsKey("issuanceDate")) {
                assertEquals("VC Data Model v1.1 specifies that “issuanceDate” property MUST be represented as an nbf JWT claim, and not iat JWT claim.", ((Instant) claims.get("issuanceDate")).getEpochSecond(), theToken.getNbf().longValue());
            } else {
                // if not specific date is set, check against "currentTime"
                assertEquals("VC Data Model v1.1 specifies that “issuanceDate” property MUST be represented as an nbf JWT claim, and not iat JWT claim.", TEST_ISSUANCE_DATE.getEpochSecond(), theToken.getNbf().longValue());
            }
            assertEquals("The issuer should be set in the token.", TEST_DID.toString(), theToken.getIssuer());
            assertEquals("The credential ID should be set as the token ID.", testCredential.getId().toString(), theToken.getId());
            Optional.ofNullable(testCredential.getCredentialSubject().getClaims().get("id")).ifPresent(id -> assertEquals("If the credentials subject id is set, it should be set as the token subject.", id.toString(), theToken.getSubject()));

            assertNotNull("The credentials should be included at the vc-claim.", theToken.getOtherClaims().get("vc"));
            VerifiableCredential credential = JsonSerialization.mapper.convertValue(theToken.getOtherClaims().get("vc"), VerifiableCredential.class);
            assertEquals("The types should be included", TEST_TYPES, credential.getType());
            assertEquals("The issuer should be included", TEST_DID, credential.getIssuer());
            assertEquals("The expiration date should be included", TEST_EXPIRATION_DATE, credential.getExpirationDate());
            if (claims.containsKey("issuanceDate")) {
                assertEquals("The issuance date should be included", claims.get("issuanceDate"), credential.getIssuanceDate());
            }

            CredentialSubject subject = credential.getCredentialSubject();
            claims.entrySet().stream()
                    .filter(e -> !e.getKey().equals("issuanceDate"))
                    .forEach(e -> assertEquals(String.format("All additional claims should be set - %s is incorrect", e.getKey()), e.getValue(), subject.getClaims().get(e.getKey())));
        } catch (VerificationException e) {
            fail("Was not able to get the token from the verifier.");
        }
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
}
