/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.TrustManagerFactory;

import org.keycloak.common.util.CertificateUtils;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.VCIssuanceContext;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.issuance.keybinding.AttestationKeyResolver;
import org.keycloak.protocol.oid4vc.issuance.keybinding.AttestationProofValidator;
import org.keycloak.protocol.oid4vc.issuance.keybinding.AttestationProofValidatorFactory;
import org.keycloak.protocol.oid4vc.issuance.keybinding.AttestationValidatorUtil;
import org.keycloak.protocol.oid4vc.issuance.keybinding.JwtProofValidator;
import org.keycloak.protocol.oid4vc.issuance.keybinding.ProofValidator;
import org.keycloak.protocol.oid4vc.issuance.keybinding.StaticAttestationKeyResolver;
import org.keycloak.protocol.oid4vc.model.ISO18045ResistanceLevel;
import org.keycloak.protocol.oid4vc.model.KeyAttestationJwtBody;
import org.keycloak.protocol.oid4vc.model.KeyAttestationsRequired;
import org.keycloak.protocol.oid4vc.model.Proofs;

import org.jboss.logging.Logger;
import org.junit.Test;

import static org.keycloak.protocol.oid4vc.model.ProofType.JWT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Bertrand Ogen
 *
 * Test class for verifying Key Attestation
 */

public class OID4VCKeyAttestationTest extends OID4VCIssuerEndpointTest {

    private static final Logger LOGGER = Logger.getLogger(OID4VCKeyAttestationTest.class);

    @Test
    public void testValidAttestationProof() {
        String cNonce = getCNonce();
        testingClient.server(TEST_REALM_NAME).run(session -> {
            runValidAttestationProofTest(session, cNonce);
        });
    }

    @Test
    public void testInvalidAttestationProof() {
        testingClient.server(TEST_REALM_NAME).run(OID4VCKeyAttestationTest::runInvalidAttestationProofTest);
    }

    @Test
    public void testValidJwtProofWithKeyAttestation() {
        String cNonce = getCNonce();
        testingClient.server(TEST_REALM_NAME).run(session -> {
            runValidJwtProofWithKeyAttestationTest(session, cNonce);
        });
    }


    @Test
    public void testInvalidJwtProofWithKeyAttestation() {
        testingClient.server(TEST_REALM_NAME).run(session -> {
            try {
                runInvalidJwtProofWithKeyAttestationTest(session);
                fail("Expected VCIssuerException to be thrown");
            } catch (VCIssuerException e) {
                assertTrue(e.getMessage().contains("Could not validate JWT proof"));
            }
        });
    }

    @Test
    public void testAttestationProofType() {
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AttestationProofValidatorFactory factory = new AttestationProofValidatorFactory();
            ProofValidator validator = factory.create(session);
            assertEquals("The proof type should be 'attestation'.",
                    "attestation", validator.getProofType());
        });
    }

    @Test
    public void testInvalidAttestationSignature() {
        String cNonce = getCNonce();
        testingClient.server(TEST_REALM_NAME).run(session -> {
            try {
                runInvalidAttestationSignatureTest(session, cNonce);
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        });
    }

    @Test
    public void testMissingRequiredAttestationClaims() {
        testingClient.server(TEST_REALM_NAME).run(OID4VCKeyAttestationTest::runMissingRequiredAttestationClaimsTest);
    }

    @Test
    public void testAttestationWithMultipleAttestedKeys() {
        String cNonce = getCNonce();
        testingClient.server(TEST_REALM_NAME).run(session -> {
            runAttestationWithMultipleAttestedKeys(session, cNonce);
        });
    }

    @Test
    public void testAttestationWithX5cCertificateChain() {
        String cNonce = getCNonce();
        testingClient.server(TEST_REALM_NAME).run(session -> {
            try {
                runAttestationWithX5cCertificateChain(session, cNonce);
            } catch (VCIssuerException e) {
                assertTrue("Expected error about invalid level but got: " + e.getMessage(),
                        e.getMessage().contains("key_storage") ||
                                e.getMessage().contains("INVALID_LEVEL"));
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        });
    }

    @Test
    public void testAttestationWithInvalidResistanceLevels() {
        String cNonce = getCNonce();
        testingClient.server(TEST_REALM_NAME).run(session -> {
            try {
                runAttestationWithInvalidResistanceLevels(session, cNonce);
            } catch (VCIssuerException e) {
                assertTrue("Expected error about invalid level but got: " + e.getMessage(),
                        e.getMessage().contains("key_storage") ||
                                e.getMessage().contains("INVALID_LEVEL"));
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        });
    }

    @Test
    public void testAttestationWithMissingAttestedKeys() {
        String cNonce = getCNonce();
        testingClient.server(TEST_REALM_NAME).run(session -> {
            runAttestationWithMissingAttestedKeys(session, cNonce);
        });
    }

    @Test
    public void testAttestationWithValidResistanceLevels() {
        String cNonce = getCNonce();
        testingClient.server(TEST_REALM_NAME).run(session -> {
            try {
                runAttestationWithValidResistanceLevels(session, cNonce);
            } catch (VCIssuerException e) {
                assertTrue("Expected error about invalid level but got: " + e.getMessage(),
                        e.getMessage().contains("key_storage") ||
                                e.getMessage().contains("INVALID_LEVEL"));
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        });
    }

    private static void runAttestationWithValidResistanceLevels(KeycloakSession session, String cNonce) {
        try {
            KeyWrapper attestationKey = getECKey("attestationKey");
            KeyWrapper proofKey = getECKey("proofKey");

            JWK proofJwk = JWKBuilder.create().ec(proofKey.getPublicKey());
            proofJwk.setKeyId(proofKey.getKid());
            proofJwk.setAlgorithm(proofKey.getAlgorithm());

            // Create a complete payload
            KeyAttestationJwtBody payload = new KeyAttestationJwtBody();
            payload.setIat((long) TIME_PROVIDER.currentTimeSeconds());
            payload.setNonce(cNonce);
            payload.setAttestedKeys(List.of(proofJwk));
            payload.setKeyStorage(List.of(
                    ISO18045ResistanceLevel.HIGH.getValue(),
                    ISO18045ResistanceLevel.MODERATE.getValue()
            ));
            payload.setUserAuthentication(List.of(
                    ISO18045ResistanceLevel.ENHANCED_BASIC.getValue(),
                    ISO18045ResistanceLevel.BASIC.getValue()
            ));

            String attestationJwt = new JWSBuilder()
                    .type(AttestationValidatorUtil.ATTESTATION_JWT_TYP)
                    .kid(attestationKey.getKid())
                    .jsonContent(payload)
                    .sign(new ECDSASignatureSignerContext(attestationKey));

            VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
            // Set attestation requirements
            KeyAttestationsRequired attestationRequirements = new KeyAttestationsRequired();
            attestationRequirements.setKeyStorage(List.of(
                    ISO18045ResistanceLevel.HIGH,
                    ISO18045ResistanceLevel.MODERATE,
                    ISO18045ResistanceLevel.ENHANCED_BASIC
            ));
            attestationRequirements.setUserAuthentication(List.of(
                    ISO18045ResistanceLevel.BASIC,
                    ISO18045ResistanceLevel.ENHANCED_BASIC
            ));

            vcIssuanceContext.getCredentialConfig()
                    .getProofTypesSupported()
                    .getSupportedProofTypes()
                    .get(JWT)
                    .setKeyAttestationsRequired(attestationRequirements);

            vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setAttestation(List.of(attestationJwt)));

            AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(
                    Map.of(attestationKey.getKid(), JWKBuilder.create().ec(attestationKey.getPublicKey()))
            );

            AttestationProofValidator validator = new AttestationProofValidator(session, keyResolver);
            List<JWK> attestedKeys = validator.validateProof(vcIssuanceContext);

            assertNotNull("Attested keys should not be null", attestedKeys);
            assertEquals("Should contain exactly one attested key", 1, attestedKeys.size());
            assertEquals("Attested key ID should match proof key ID",
                    proofKey.getKid(),
                    attestedKeys.get(0).getKeyId());
        } catch (Exception e) {
            LOGGER.error("Validation failed with valid resistance levels", e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    private static void runValidAttestationProofTest(KeycloakSession session, String cNonce) {
        try {
            KeyWrapper attestationKey = getECKey("attestationKey");
            KeyWrapper proofKey = getECKey("proofKey");

            attestationKey.setKid("attestationKey");
            attestationKey.setAlgorithm("ES256");
            proofKey.setKid("proofKey");
            proofKey.setAlgorithm("ES256");

            JWK proofJwk = JWKBuilder.create().ec(proofKey.getPublicKey());
            proofJwk.setKeyId(proofKey.getKid());
            proofJwk.setAlgorithm(proofKey.getAlgorithm());


            String attestationJwt = createValidAttestationJwt(session, attestationKey, proofJwk, cNonce);
            String jwtProof = generateJwtProofWithKeyAttestation(session, proofKey, attestationJwt, cNonce);
            VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
            vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setJwt(List.of(jwtProof)));

            JWK attestationJwk = JWKBuilder.create().ec(attestationKey.getPublicKey());
            attestationJwk.setKeyId(attestationKey.getKid());

            AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(
                    Map.of(attestationKey.getKid(), attestationJwk)
            );

            JwtProofValidator validator = new JwtProofValidator(session, keyResolver);
            List<JWK> attestedKeys = validator.validateProof(vcIssuanceContext);

            assertNotNull("Attested keys should not be null", attestedKeys);
            assertEquals("Should contain exactly one attested key", 1, attestedKeys.size());
            assertEquals("Attested key ID should match proof key ID",
                    proofKey.getKid(),
                    attestedKeys.get(0).getKeyId());

        } catch (Exception e) {
            LOGGER.error("Validation failed", e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    private static void runInvalidAttestationProofTest(KeycloakSession session) {
        KeyWrapper attestationKey = getECKey("attestationKey");
        String invalidAttestationJwt = "invalid.jwt.token";

        VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
        vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setAttestation(List.of(invalidAttestationJwt)));

        AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(Map.of(attestationKey.getKid(), JWKBuilder.create().ec(attestationKey.getPublicKey())));
        AttestationProofValidator validator = new AttestationProofValidator(session, keyResolver);

        try {
            validator.validateProof(vcIssuanceContext);
            fail("Expected VCIssuerException to be thrown");
        } catch (VCIssuerException e) {
            // Expected exception
        }
    }

    private static void runValidJwtProofWithKeyAttestationTest(KeycloakSession session, String cNonce) {
        try {
            KeyWrapper attestationKey = getECKey("attestationKey");
            KeyWrapper proofKey = getECKey("proofKey");
            JWK proofJwk = JWKBuilder.create().ec(proofKey.getPublicKey());

            String attestationJwt = createValidAttestationJwt(session, attestationKey, proofJwk, cNonce);
            String jwtProof = generateJwtProofWithKeyAttestation(session, proofKey, attestationJwt, cNonce);

            VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
            vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setJwt(List.of(jwtProof)));

            AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(
                    Map.of(attestationKey.getKid(), JWKBuilder.create().ec(attestationKey.getPublicKey()))
            );
            JwtProofValidator validator = new JwtProofValidator(session, keyResolver);

            List<JWK> attestedKeys = validator.validateProof(vcIssuanceContext);
            assertNotNull(attestedKeys);
            assertFalse(attestedKeys.isEmpty());
        } catch (Exception e) {
            LOGGER.error("Validation failed unexpectedly", e);
            fail("Unexpected exception in valid JWT proof test: " + e.getMessage());
        }
    }

    private static void runInvalidJwtProofWithKeyAttestationTest(KeycloakSession session) {
        KeyWrapper attestationKey = getECKey("attestationKey");
        String invalidJwtProof = "invalid.jwt.token";

        VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
        vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setJwt(List.of(invalidJwtProof)));

        AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(Map.of(attestationKey.getKid(), JWKBuilder.create().ec(attestationKey.getPublicKey())));
        JwtProofValidator validator = new JwtProofValidator(session, keyResolver);

        validator.validateProof(vcIssuanceContext);
    }

    private static void runInvalidAttestationSignatureTest(KeycloakSession session, String cNonce) {
        KeyWrapper attestationKey = getECKey("attestationKey");
        KeyWrapper proofKey = getECKey("proofKey");
        JWK proofJwk = JWKBuilder.create().ec(proofKey.getPublicKey());

        KeyWrapper unrelatedKey = getECKey("unrelatedKey");
        String invalidAttestationJwt = new JWSBuilder()
                .type(AttestationValidatorUtil.ATTESTATION_JWT_TYP)
                .jwk(JWKBuilder.create().ec(attestationKey.getPublicKey()))
                .jsonContent(createAttestationPayload(proofJwk, cNonce))
                .sign(new ECDSASignatureSignerContext(unrelatedKey));

        VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
        vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setAttestation(List.of(invalidAttestationJwt)));

        AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(Map.of(attestationKey.getKid(), JWKBuilder.create().ec(attestationKey.getPublicKey())));
        AttestationProofValidator validator = new AttestationProofValidator(session, keyResolver);

        try {
            validator.validateProof(vcIssuanceContext);
            fail("Expected VCIssuerException to be thrown");
        } catch (VCIssuerException e) {
            assertTrue("Expected VCIssuerException to be thrown", true);
        }
    }

    private static void runMissingRequiredAttestationClaimsTest(KeycloakSession session) {
        KeyWrapper attestationKey = getECKey("attestationKey");

        Map<String, Object> incompletePayload = new HashMap<>();
        incompletePayload.put("iat", TIME_PROVIDER.currentTimeSeconds());

        String invalidAttestationJwt = new JWSBuilder()
                .type(AttestationValidatorUtil.ATTESTATION_JWT_TYP)
                .jwk(JWKBuilder.create().ec(attestationKey.getPublicKey()))
                .jsonContent(incompletePayload)
                .sign(new ECDSASignatureSignerContext(attestationKey));

        VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
        vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setAttestation(List.of(invalidAttestationJwt)));

        AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(Map.of(attestationKey.getKid(), JWKBuilder.create().ec(attestationKey.getPublicKey())));
        AttestationProofValidator validator = new AttestationProofValidator(session, keyResolver);

        try {
            validator.validateProof(vcIssuanceContext);
            fail("Expected VCIssuerException to be thrown");
        } catch (VCIssuerException e) {
            assertTrue("Expected VCIssuerException to be thrown", true);
        }
    }

    private static void runAttestationWithMultipleAttestedKeys(KeycloakSession session, String cNonce) {
        try {
            KeyWrapper attestationKey = getECKey("attestationKey");
            KeyWrapper proofKey1 = getECKey("proofKey1");
            KeyWrapper proofKey2 = getECKey("proofKey2");

            JWK proofJwk1 = JWKBuilder.create().ec(proofKey1.getPublicKey());
            proofJwk1.setKeyId(proofKey1.getKid());
            proofJwk1.setAlgorithm(proofKey1.getAlgorithm());

            JWK proofJwk2 = JWKBuilder.create().ec(proofKey2.getPublicKey());
            proofJwk2.setKeyId(proofKey2.getKid());
            proofJwk2.setAlgorithm(proofKey2.getAlgorithm());

            // Create a proper payload with attested keys
            KeyAttestationJwtBody payload = new KeyAttestationJwtBody();
            payload.setIat((long) TIME_PROVIDER.currentTimeSeconds());
            payload.setNonce(cNonce);
            payload.setAttestedKeys(List.of(proofJwk1, proofJwk2));
            payload.setKeyStorage(List.of(ISO18045ResistanceLevel.HIGH.getValue()));
            payload.setUserAuthentication(List.of(ISO18045ResistanceLevel.HIGH.getValue()));

            String attestationJwt = new JWSBuilder()
                    .type(AttestationValidatorUtil.ATTESTATION_JWT_TYP)
                    .kid(attestationKey.getKid())
                    .jsonContent(payload)
                    .sign(new ECDSASignatureSignerContext(attestationKey));

            VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
            vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setAttestation(List.of(attestationJwt)));

            AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(
                    Map.of(attestationKey.getKid(), JWKBuilder.create().ec(attestationKey.getPublicKey()))
            );

            AttestationProofValidator validator = new AttestationProofValidator(session, keyResolver);
            List<JWK> attestedKeys = validator.validateProof(vcIssuanceContext);

            assertEquals(2, attestedKeys.size());
            assertTrue(attestedKeys.stream().anyMatch(k -> k.getKeyId().equals(proofJwk1.getKeyId())));
            assertTrue(attestedKeys.stream().anyMatch(k -> k.getKeyId().equals(proofJwk2.getKeyId())));
        } catch (Exception e) {
            LOGGER.error("Validation failed with exception: " + e.getMessage(), e);
            fail("Unexpected exception in test: " + e.getMessage());
        }
    }

    private static void runAttestationWithX5cCertificateChain(KeycloakSession session, String cNonce) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            keyGen.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair keyPair = keyGen.generateKeyPair();

            X509Certificate cert = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "Test Certificate");
            Logger.getLogger(OID4VCKeyAttestationTest.class).info("Generated certificate: " + cert.toString());

            KeyWrapper signerKey = new KeyWrapper();
            signerKey.setPrivateKey(keyPair.getPrivate());
            signerKey.setPublicKey(keyPair.getPublic());
            signerKey.setAlgorithm("ES256");
            signerKey.setType(KeyType.EC);
            signerKey.setKid("test-cert-key");

            KeyWrapper proofKey = getECKey("proofKey");
            JWK proofJwk = JWKBuilder.create().ec(proofKey.getPublicKey());
            proofJwk.setKeyId(proofKey.getKid());
            proofJwk.setAlgorithm(proofKey.getAlgorithm());

            KeyAttestationJwtBody payload = new KeyAttestationJwtBody();
            payload.setNonce(cNonce);
            payload.setIat((long) TIME_PROVIDER.currentTimeSeconds());
            payload.setAttestedKeys(List.of(proofJwk));
            payload.setKeyStorage(List.of(ISO18045ResistanceLevel.HIGH.getValue()));
            payload.setUserAuthentication(List.of(ISO18045ResistanceLevel.HIGH.getValue()));

            String attestationJwt = new JWSBuilder()
                    .type(AttestationValidatorUtil.ATTESTATION_JWT_TYP)
                    .kid(signerKey.getKid())
                    .x5c(List.of(cert))
                    .jsonContent(payload)
                    .sign(new ECDSASignatureSignerContext(signerKey));

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null);
            ks.setCertificateEntry("test-cert", cert);
            tmf.init(ks);

            JWK certJwk = JWKBuilder.create().ec(signerKey.getPublicKey());
            certJwk.setKeyId(signerKey.getKid());
            AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(
                    Map.of(signerKey.getKid(), certJwk)
            );

            VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
            vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setAttestation(List.of(attestationJwt)));

            AttestationProofValidator validator = new AttestationProofValidator(session, keyResolver);
            List<JWK> attestedKeys = validator.validateProof(vcIssuanceContext);

            assertNotNull("Attested keys should not be null", attestedKeys);
            assertEquals("Should contain exactly one attested key", 1, attestedKeys.size());
            assertEquals("Attested key ID should match proof key ID", proofKey.getKid(), attestedKeys.get(0).getKeyId());
        } catch (Exception e) {
            LOGGER.error("Certificate chain validation failed", e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    private static void runAttestationWithInvalidResistanceLevels(KeycloakSession session, String cNonce) {
        try {
            KeyWrapper attestationKey = getECKey("attestationKey");
            KeyWrapper proofKey = getECKey("proofKey");

            JWK proofJwk = JWKBuilder.create().ec(proofKey.getPublicKey());
            proofJwk.setKeyId(proofKey.getKid());
            proofJwk.setAlgorithm(proofKey.getAlgorithm());

            KeyAttestationJwtBody payload = new KeyAttestationJwtBody();
            payload.setIat((long) TIME_PROVIDER.currentTimeSeconds());
            payload.setNonce(cNonce);
            payload.setAttestedKeys(List.of(proofJwk));
            payload.setKeyStorage(List.of("INVALID_LEVEL"));

            String attestationJwt = new JWSBuilder()
                    .type(AttestationValidatorUtil.ATTESTATION_JWT_TYP)
                    .kid(attestationKey.getKid())
                    .jsonContent(payload)
                    .sign(new ECDSASignatureSignerContext(attestationKey));

            VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
            vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setAttestation(List.of(attestationJwt)));

            AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(
                    Map.of(attestationKey.getKid(), JWKBuilder.create().ec(attestationKey.getPublicKey()))
            );

            new AttestationProofValidator(session, keyResolver).validateProof(vcIssuanceContext);
            fail("Expected VCIssuerException for invalid resistance level");
        } catch (VCIssuerException e) {
            assertTrue("Expected error about invalid level but got: " + e.getMessage(),
                    e.getMessage().contains("key_storage") && e.getMessage().contains("INVALID_LEVEL"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    private static void runAttestationWithMissingAttestedKeys(KeycloakSession session, String cNonce) {
        try {
            KeyWrapper attestationKey = getECKey("attestationKey");

            KeyAttestationJwtBody payload = new KeyAttestationJwtBody();
            payload.setIat((long) TIME_PROVIDER.currentTimeSeconds());
            payload.setNonce(cNonce);
            // Intentionally omit attested_keys

            String attestationJwt = new JWSBuilder()
                    .type(AttestationValidatorUtil.ATTESTATION_JWT_TYP)
                    .kid(attestationKey.getKid())
                    .jsonContent(payload)
                    .sign(new ECDSASignatureSignerContext(attestationKey));

            VCIssuanceContext context = createVCIssuanceContext(session);
            context.getCredentialRequest().setProofs(new Proofs().setAttestation(List.of(attestationJwt)));

            AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(
                    Map.of(attestationKey.getKid(), JWKBuilder.create().ec(attestationKey.getPublicKey()))
            );

            AttestationProofValidator validator = new AttestationProofValidator(session, keyResolver);
            validator.validateProof(context);
            fail("Expected VCIssuerException for missing attested_keys");
        } catch (VCIssuerException e) {
            assertTrue("Expected error about missing keys but got: " + e.getMessage(),
                    e.getMessage().contains("attested_keys"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
}
