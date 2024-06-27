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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.signing.LDSigningService;
import org.keycloak.protocol.oid4vc.issuance.signing.SigningServiceException;
import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oid4vc.model.vcdm.LdProof;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.runonserver.RunOnServerException;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LDSigningServiceTest extends OID4VCTest {

    @Before
    public void setup() {
        CryptoIntegration.init(this.getClass().getClassLoader());
    }

    // If an unsupported algorithm is provided, the JWT Signing Service should not be instantiated.
    @Test(expected = SigningServiceException.class)
    public void testUnsupportedLdpType() throws Throwable {
        try {
            getTestingClient()
                    .server(TEST_REALM_NAME)
                    .run(session ->
                            new LDSigningService(
                                    session,
                                    getKeyFromSession(session).getKid(),
                                    "EdDSA",
                                    "UnsupportedSignatureType",
                                    new ObjectMapper(),
                                    new StaticTimeProvider(1000),
                                    Optional.empty()));
        } catch (RunOnServerException ros) {
            throw ros.getCause();
        }
    }

    // If no key is provided, the JWT Signing Service should not be instantiated.
    @Test(expected = SigningServiceException.class)
    public void testFailIfNoKey() throws Throwable {
        try {
            getTestingClient()
                    .server(TEST_REALM_NAME)
                    .run(session ->
                            new LDSigningService(
                                    session,
                                    "no-such-key",
                                    "EdDSA",
                                    "Ed25519Signature2018",
                                    new ObjectMapper(),
                                    new StaticTimeProvider(1000),
                                    Optional.empty()));
        } catch (RunOnServerException ros) {
            throw ros.getCause();
        }
    }

    // The provided credentials should be successfully signed as a JWT-VC.
    @Test
    public void testLdpSignedCredentialWithOutIssuanceDate() {
        getTestingClient()
                .server(TEST_REALM_NAME)
                .run(session ->
                        testSignLdCredential(
                                session,
                                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                        "test", "test",
                                        "arrayClaim", List.of("a", "b", "c")),
                                Optional.empty()));
    }

    @Test
    public void testLdpSignedCredentialWithIssuanceDate() {
        getTestingClient()
                .server(TEST_REALM_NAME)
                .run(session ->
                        testSignLdCredential(
                                session,
                                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                        "test", "test",
                                        "arrayClaim", List.of("a", "b", "c"),
                                        "issuanceDate", Date.from(Instant.ofEpochSecond(10))),
                                Optional.empty()));
    }

    @Test
    public void testLdpSignedCredentialWithCustomKid() {
        getTestingClient()
                .server(TEST_REALM_NAME)
                .run(session ->
                        testSignLdCredential(
                                session,
                                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                        "test", "test",
                                        "arrayClaim", List.of("a", "b", "c"),
                                        "issuanceDate", Date.from(Instant.ofEpochSecond(10))),
                                Optional.of("did:web:test.org#the-key-id")));
    }

    @Test
    public void testLdpSignedCredentialWithoutAdditionalClaims() {
        getTestingClient()
                .server(TEST_REALM_NAME)
                .run(session ->
                        testSignLdCredential(
                                session,
                                Map.of(),
                                Optional.empty()));
    }

    public static void testSignLdCredential(KeycloakSession session, Map<String, Object> claims, Optional<String> kid) {
        KeyWrapper keyWrapper = getKeyFromSession(session);

        LDSigningService ldSigningService = new LDSigningService(
                session,
                keyWrapper.getKid(),
                "EdDSA",
                "Ed25519Signature2018",
                new ObjectMapper(),
                new StaticTimeProvider(1000),
                kid);

        VerifiableCredential testCredential = getTestCredential(claims);

        VerifiableCredential verifiableCredential = ldSigningService.signCredential(testCredential);

        assertEquals("The types should be included", TEST_TYPES, verifiableCredential.getType());
        assertEquals("The issuer should be included", TEST_DID, verifiableCredential.getIssuer());
        assertNotNull("The context needs to be set.", verifiableCredential.getContext());
        assertEquals("The expiration date should be included", TEST_EXPIRATION_DATE, verifiableCredential.getExpirationDate());
        if (claims.containsKey("issuanceDate")) {
            assertEquals("The issuance date should be included", claims.get("issuanceDate"), verifiableCredential.getIssuanceDate());
        }

        CredentialSubject subject = verifiableCredential.getCredentialSubject();
        claims.entrySet().stream()
                .filter(e -> !e.getKey().equals("issuanceDate"))
                .forEach(e -> assertEquals(String.format("All additional claims should be set - %s is incorrect", e.getKey()), e.getValue(), subject.getClaims().get(e.getKey())));

        assertNotNull("The credential should contain a signed proof.", verifiableCredential.getAdditionalProperties().get("proof"));

        LdProof ldProof = (LdProof) verifiableCredential.getAdditionalProperties().get("proof");
        String expectedKid = kid.orElse(keyWrapper.getKid());
        assertEquals("The verification method should be set to the key id.", expectedKid, ldProof.getVerificationMethod());

    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        if (testRealm.getComponents() != null) {
            testRealm.getComponents().add("org.keycloak.keys.KeyProvider", getEdDSAKeyProvider());
        } else {
            testRealm.setComponents(new MultivaluedHashMap<>(
                    Map.of("org.keycloak.keys.KeyProvider", List.of(getEdDSAKeyProvider()))));
        }
    }
}
