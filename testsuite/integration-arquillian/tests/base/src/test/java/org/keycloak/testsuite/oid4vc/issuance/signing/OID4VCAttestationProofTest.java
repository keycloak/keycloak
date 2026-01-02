package org.keycloak.testsuite.oid4vc.issuance.signing;

import java.io.IOException;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.OID4VCConstants;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.VCIssuanceContext;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.issuance.keybinding.AttestationProofValidator;
import org.keycloak.protocol.oid4vc.issuance.keybinding.AttestationProofValidatorFactory;
import org.keycloak.protocol.oid4vc.issuance.keybinding.AttestationValidatorUtil;
import org.keycloak.protocol.oid4vc.issuance.keybinding.ProofValidator;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.KeyAttestationJwtBody;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class for verifying Attestation Proof functionality with trusted keys configuration.
 * Tests both component-based and realm attribute-based configuration.
 */
public class OID4VCAttestationProofTest extends OID4VCIssuerEndpointTest {

    private static final Logger LOGGER = Logger.getLogger(OID4VCAttestationProofTest.class);

    @Test
    public void testAttestationProofWithRealmAttributeTrustedKeys() {
        String cNonce = getCNonce();
        testingClient.server(TEST_REALM_NAME).run(session -> {
            runAttestationProofWithRealmAttributeTrustedKeys(session, cNonce);
        });
    }

    @Test
    public void testAttestationProofAcceptsLegacyTyp() {
        String cNonce = getCNonce();
        testingClient.server(TEST_REALM_NAME).run(session -> {
            try {
                KeyWrapper attestationKey = createECKey("legacyAttestationKey");
                KeyWrapper proofKey = createECKey("legacyProofKey");

                JWK proofJwk = createJWK(proofKey);
                String attestationJwt = createValidAttestationJwt(
                        session,
                        attestationKey,
                        List.of(proofJwk),
                        cNonce,
                        AttestationValidatorUtil.LEGACY_ATTESTATION_JWT_TYP);

                configureTrustedKeysInRealm(session, List.of(createJWK(attestationKey)));

                VCIssuanceContext vcIssuanceContext = createVCIssuanceContextWithAttestationProof(session, attestationJwt);

                AttestationProofValidatorFactory factory = new AttestationProofValidatorFactory();
                AttestationProofValidator validator = (AttestationProofValidator) factory.create(session);

                validateProofAndAssert(validator, vcIssuanceContext, proofKey);
            } catch (Exception e) {
                LOGGER.error("Legacy typ test failed with exception", e);
                fail("Legacy typ attestation proof should be accepted: " + e.getMessage());
            }
        });
    }

    @Test
    public void testAttestationProofExtractsAttestedKeysFromPayload() {
        String cNonce = getCNonce();
        testingClient.server(TEST_REALM_NAME).run(session -> {
            runAttestationProofExtractsAttestedKeysFromPayload(session, cNonce);
        });
    }

    @Test
    public void testAttestationProofWithInvalidTrustedKey() {
        String cNonce = getCNonce();
        testingClient.server(TEST_REALM_NAME).run(session -> {
            try {
                runAttestationProofWithInvalidTrustedKey(session, cNonce);
                fail("Expected VCIssuerException to be thrown");
            } catch (VCIssuerException e) {
                assertTrue("Expected error about key not found in trusted key registry but got: " + e.getMessage(),
                        e.getMessage().contains("not found in trusted key registry"));
            }
        });
    }

    @Test
    public void testAttestationProofValidatorFactoryConfiguration() {
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AttestationProofValidatorFactory factory = new AttestationProofValidatorFactory();

            assertEquals("Factory ID should be 'attestation'",
                    "attestation", factory.getId());

            ProofValidator validator = factory.create(session);
            assertNotNull("Factory should create validator", validator);
            assertEquals("Validator proof type should be 'attestation'",
                    "attestation", validator.getProofType());
        });
    }

    @Test
    public void testAttestationProofWithMultipleTrustedKeys() {
        String cNonce = getCNonce();
        testingClient.server(TEST_REALM_NAME).run(session -> {
            runAttestationProofWithMultipleTrustedKeys(session, cNonce);
        });
    }

    @Test
    public void testCredentialIssuanceWithAttestationProof() {
        final String scopeName = jwtTypeCredentialClientScope.getName();
        String configIdFromScope = jwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
        final String credConfigId = configIdFromScope != null ? configIdFromScope : scopeName;
        String token = getBearerToken(oauth, client, scopeName);
        String cNonce = getCNonce();

        testingClient.server(TEST_REALM_NAME).run(session -> {
            try {
                // Configure trusted keys via realm attribute
                KeyWrapper attestationKey = createECKey("attestationKey");
                JWK attestationJwk = createJWK(attestationKey);
                configureTrustedKeysInRealm(session, List.of(attestationJwk));

                // Create attestation JWT
                KeyWrapper proofKey = createECKey("proofKey");
                JWK proofJwk = createJWK(proofKey);
                String attestationJwt = createValidAttestationJwt(session, attestationKey, proofJwk, cNonce);

                // Create credential request with attestation proof
                AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);

                Proofs proofs = new Proofs().setAttestation(List.of(attestationJwt));

                CredentialRequest request = new CredentialRequest()
                        .setCredentialConfigurationId(credConfigId)
                        .setProofs(proofs);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);

                String requestPayload = JsonSerialization.writeValueAsString(request);

                Response response = endpoint.requestCredential(requestPayload);
                assertEquals("Response status should be OK", Response.Status.OK.getStatusCode(), response.getStatus());

                CredentialResponse credentialResponse = JsonSerialization.mapper
                        .convertValue(response.getEntity(), CredentialResponse.class);
                assertNotNull("Credential response should not be null", credentialResponse);
                assertNotNull("Credentials array should not be null", credentialResponse.getCredentials());
                assertEquals("Should return 1 credential", 1, credentialResponse.getCredentials().size());

                // Validate the credential
                Object credentialObj = credentialResponse.getCredentials().get(0).getCredential();
                assertNotNull("Credential should not be null", credentialObj);
                assertTrue("Credential should be a string", credentialObj instanceof String);

                String credentialString = (String) credentialObj;
                JsonWebToken jsonWebToken;
                try {
                    jsonWebToken = TokenVerifier.create(credentialString, JsonWebToken.class).getToken();
                } catch (VerificationException e) {
                    fail("Failed to verify JWT credential: " + e.getMessage());
                    return;
                }

                assertNotNull("A valid credential JWT should be returned", jsonWebToken);
                assertNotNull("The credentials should include the vc claim", jsonWebToken.getOtherClaims().get("vc"));

                VerifiableCredential vc = JsonSerialization.mapper.convertValue(
                        jsonWebToken.getOtherClaims().get("vc"), VerifiableCredential.class);
                assertNotNull("VerifiableCredential should not be null", vc);
                assertNotNull("Credential subject should not be null", vc.getCredentialSubject());
            } catch (Exception e) {
                LOGGER.error("Test failed with exception", e);
                fail("Test should not throw exception: " + e.getMessage());
            }
        });
    }

    /**
     * Creates and configures an EC key with the given key ID.
     */
    private static KeyWrapper createECKey(String keyId) {
        KeyWrapper key = getECKey(keyId);
        key.setKid(keyId);
        key.setAlgorithm("ES256");
        return key;
    }

    /**
     * Creates a JWK from a KeyWrapper.
     */
    private static JWK createJWK(KeyWrapper keyWrapper) {
        JWK jwk = JWKBuilder.create().ec(keyWrapper.getPublicKey());
        jwk.setKeyId(keyWrapper.getKid());
        jwk.setAlgorithm(keyWrapper.getAlgorithm());
        return jwk;
    }

    /**
     * Configures trusted keys in realm attribute.
     */
    private static void configureTrustedKeysInRealm(KeycloakSession session, List<JWK> trustedKeys) throws IOException {
        RealmModel realm = session.getContext().getRealm();
        String trustedKeysJson = JsonSerialization.writeValueAsString(trustedKeys);
        realm.setAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR, trustedKeysJson);
    }

    /**
     * Creates a VCIssuanceContext with an attestation proof.
     */
    private static VCIssuanceContext createVCIssuanceContextWithAttestationProof(KeycloakSession session, String attestationJwt) {
        VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
        vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setAttestation(List.of(attestationJwt)));
        return vcIssuanceContext;
    }

    /**
     * Validates proof and returns attested keys, with common assertions.
     */
    private static List<JWK> validateProofAndAssert(AttestationProofValidator validator,
                                                    VCIssuanceContext vcIssuanceContext,
                                                    KeyWrapper expectedProofKey) {
        List<JWK> attestedKeys = validator.validateProof(vcIssuanceContext);
        assertNotNull("Attested keys should not be null", attestedKeys);
        assertEquals("Should contain exactly one attested key", 1, attestedKeys.size());
        assertEquals("Attested key ID should match proof key ID",
                expectedProofKey.getKid(), attestedKeys.get(0).getKeyId());
        return attestedKeys;
    }

    private static void runAttestationProofWithRealmAttributeTrustedKeys(KeycloakSession session, String cNonce) {
        try {
            KeyWrapper attestationKey = createECKey("attestationKey");
            KeyWrapper proofKey = createECKey("proofKey");

            // Create attestation JWT
            JWK proofJwk = createJWK(proofKey);
            String attestationJwt = createValidAttestationJwt(session, attestationKey, proofJwk, cNonce);

            // Configure trusted keys via realm attribute
            JWK attestationJwk = createJWK(attestationKey);
            configureTrustedKeysInRealm(session, List.of(attestationJwk));

            // Create VCIssuanceContext with attestation proof
            VCIssuanceContext vcIssuanceContext = createVCIssuanceContextWithAttestationProof(session, attestationJwt);

            // Create validator using factory (should load from realm attribute)
            AttestationProofValidatorFactory factory = new AttestationProofValidatorFactory();
            AttestationProofValidator validator = (AttestationProofValidator) factory.create(session);

            // Validate proof
            validateProofAndAssert(validator, vcIssuanceContext, proofKey);
        } catch (Exception e) {
            LOGGER.error("Test failed with exception", e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Tests that the validator correctly extracts attested_keys from the attestation payload
     * after verifying the attestation JWT signature with a trusted key.
     */
    private static void runAttestationProofExtractsAttestedKeysFromPayload(KeycloakSession session, String cNonce) {
        try {
            KeyWrapper attestationKey = createECKey("attestationKey");
            KeyWrapper proofKey = createECKey("proofKey");

            // Create attestation JWT with attested_keys in payload
            JWK proofJwk = createJWK(proofKey);

            KeyAttestationJwtBody payload = new KeyAttestationJwtBody();
            payload.setIat((long) TIME_PROVIDER.currentTimeSeconds());
            payload.setNonce(cNonce);
            payload.setAttestedKeys(List.of(proofJwk));
            payload.setKeyStorage(List.of(OID4VCConstants.KeyAttestationResistanceLevels.HIGH));
            payload.setUserAuthentication(List.of(OID4VCConstants.KeyAttestationResistanceLevels.HIGH));

            String attestationJwt = new JWSBuilder()
                    .type(AttestationValidatorUtil.ATTESTATION_JWT_TYP)
                    .kid(attestationKey.getKid())
                    .jsonContent(payload)
                    .sign(new ECDSASignatureSignerContext(attestationKey));

            // Configure trusted key for verifying the attestation signature
            // According to spec, the signature must verify with a trusted key from the header
            JWK attestationJwk = createJWK(attestationKey);
            configureTrustedKeysInRealm(session, List.of(attestationJwk));

            // Create VCIssuanceContext with attestation proof
            VCIssuanceContext vcIssuanceContext = createVCIssuanceContextWithAttestationProof(session, attestationJwt);

            // Create validator with trusted keys configured
            AttestationProofValidatorFactory factory = new AttestationProofValidatorFactory();
            AttestationProofValidator validator = (AttestationProofValidator) factory.create(session);

            // Validate proof - should verify signature with trusted key and extract attested_keys from payload
            validateProofAndAssert(validator, vcIssuanceContext, proofKey);
        } catch (Exception e) {
            LOGGER.error("Test failed with exception", e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    private static void runAttestationProofWithInvalidTrustedKey(KeycloakSession session, String cNonce) throws IOException {
        KeyWrapper attestationKey = createECKey("attestationKey");
        KeyWrapper proofKey = createECKey("proofKey");
        KeyWrapper unrelatedKey = createECKey("unrelatedKey");

        // Create attestation JWT
        JWK proofJwk = createJWK(proofKey);
        String attestationJwt = createValidAttestationJwt(session, attestationKey, proofJwk, cNonce);

        // Configure trusted keys with wrong key (unrelatedKey instead of attestationKey)
        JWK unrelatedJwk = createJWK(unrelatedKey);
        configureTrustedKeysInRealm(session, List.of(unrelatedJwk));

        try {
            // Create VCIssuanceContext with attestation proof
            VCIssuanceContext vcIssuanceContext = createVCIssuanceContextWithAttestationProof(session, attestationJwt);

            // Create validator using factory
            AttestationProofValidatorFactory factory = new AttestationProofValidatorFactory();
            AttestationProofValidator validator = (AttestationProofValidator) factory.create(session);

            // Validate proof - should fail because trusted key doesn't match
            validator.validateProof(vcIssuanceContext);
        } catch (VCIssuerException e) {
            throw e;
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    private static void runAttestationProofWithMultipleTrustedKeys(KeycloakSession session, String cNonce) {
        try {
            KeyWrapper attestationKey1 = createECKey("attestationKey1");
            KeyWrapper attestationKey2 = createECKey("attestationKey2");
            KeyWrapper proofKey = createECKey("proofKey");

            // Create attestation JWT with first attestation key
            JWK proofJwk = createJWK(proofKey);
            String attestationJwt = createValidAttestationJwt(session, attestationKey1, proofJwk, cNonce);

            // Configure multiple trusted keys via realm attribute
            JWK attestationJwk1 = createJWK(attestationKey1);
            JWK attestationJwk2 = createJWK(attestationKey2);
            configureTrustedKeysInRealm(session, List.of(attestationJwk1, attestationJwk2));

            // Create VCIssuanceContext with attestation proof
            VCIssuanceContext vcIssuanceContext = createVCIssuanceContextWithAttestationProof(session, attestationJwt);

            // Create validator using factory
            AttestationProofValidatorFactory factory = new AttestationProofValidatorFactory();
            AttestationProofValidator validator = (AttestationProofValidator) factory.create(session);

            // Validate proof - should succeed because attestationKey1 is in trusted keys
            validateProofAndAssert(validator, vcIssuanceContext, proofKey);
        } catch (Exception e) {
            LOGGER.error("Test failed with exception", e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }
}
