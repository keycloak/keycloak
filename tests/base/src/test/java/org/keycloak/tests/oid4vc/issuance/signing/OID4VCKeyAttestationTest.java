package org.keycloak.tests.oid4vc.issuance.signing;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.TrustManagerFactory;

import org.keycloak.OID4VCConstants.KeyAttestationResistanceLevels;
import org.keycloak.VCFormat;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.issuance.VCIssuanceContext;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.issuance.keybinding.AttestationKeyResolver;
import org.keycloak.protocol.oid4vc.issuance.keybinding.AttestationProofValidator;
import org.keycloak.protocol.oid4vc.issuance.keybinding.AttestationProofValidatorFactory;
import org.keycloak.protocol.oid4vc.issuance.keybinding.AttestationValidatorUtil;
import org.keycloak.protocol.oid4vc.issuance.keybinding.JwtProofValidator;
import org.keycloak.protocol.oid4vc.issuance.keybinding.StaticAttestationKeyResolver;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.KeyAttestationJwtBody;
import org.keycloak.protocol.oid4vc.model.KeyAttestationsRequired;
import org.keycloak.protocol.oid4vc.model.ProofTypesSupported;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.representations.AccessToken;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCProofTestUtils;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.keycloak.protocol.oid4vc.model.ProofType.JWT;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Migrated OID4VCKeyAttestationTest to the new test framework.
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCKeyAttestationTest extends OID4VCIssuerTestBase {

    private static final Logger LOGGER = Logger.getLogger(OID4VCKeyAttestationTest.class);

    private static final TimeProvider TIME_PROVIDER = new OID4VCIssuerTestBase.StaticTimeProvider(1000);

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    private static void setupSessionContext(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(OID4VCIssuerTestBase.VCTestRealmConfig.TEST_REALM_NAME);
        session.getContext().setRealm(realm);
    }

    @Test
    public void testValidAttestationProof() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runValidAttestationProofTest(session, cNonce);
        });
    }

    @Test
    public void testInvalidAttestationProof() {
        runOnServer.run(session -> {
            setupSessionContext(session);
            runInvalidAttestationProofTest(session);
        });
    }

    @Test
    public void testValidJwtProofWithKeyAttestation() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runValidJwtProofWithKeyAttestationTest(session, cNonce);
        });
    }

    @Test
    public void testInvalidJwtProofWithKeyAttestation() {
        runOnServer.run(session -> {
            assertThrows(VCIssuerException.class, () -> runInvalidJwtProofWithKeyAttestationTest(session),
                    "Expected VCIssuerException to be thrown");
        });
    }

    @Test
    public void testAttestationProofType() {
        runOnServer.run(session -> {
            setupSessionContext(session);

            AttestationProofValidatorFactory factory = new AttestationProofValidatorFactory();
            var validator = factory.create(session);
            assertEquals("attestation", validator.getProofType(),
                    "The proof type should be 'attestation'.");
        });
    }

    @Test
    public void testInvalidAttestationSignature() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            try {
                runInvalidAttestationSignatureTest(session, cNonce);
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        });
    }

    @Test
    public void testMissingRequiredAttestationClaims() {
        runOnServer.run(session -> {
            setupSessionContext(session);
            runMissingRequiredAttestationClaimsTest(session);
        });
    }

    @Test
    public void testAttestationWithMultipleAttestedKeys() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runAttestationWithMultipleAttestedKeys(session, cNonce);
        });
    }

    @Test
    public void testAttestationWithX5cCertificateChain() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runAttestationWithX5cCertificateChain(session, cNonce);
        });
    }

    @Test
    public void testAttestationWithInvalidResistanceLevels() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            VCIssuerException e = assertThrows(VCIssuerException.class,
                    () -> runAttestationWithInvalidResistanceLevels(session, cNonce));
            assertTrue(e.getMessage().contains("key_storage") && e.getMessage().contains("INVALID_LEVEL"),
                    "Expected error about invalid level but got: " + e.getMessage());
        });
    }

    @Test
    public void testAttestationWithMissingAttestedKeys() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runAttestationWithMissingAttestedKeys(session, cNonce);
        });
    }

    @Test
    public void testAttestationWithValidResistanceLevels() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runAttestationWithValidResistanceLevels(session, cNonce);
        });
    }

    private String getCNonce() {
        return oauth.oid4vc().nonceRequest().send().getNonce();
    }

    private static KeyWrapper getECKey(String keyId) {
        return OID4VCProofTestUtils.newEcSigningKey(keyId);
    }

    private static VCIssuanceContext createVCIssuanceContext(KeycloakSession session) {
        VCIssuanceContext context = new VCIssuanceContext();
        KeyAttestationsRequired keyAttestationsRequired = new KeyAttestationsRequired();
        keyAttestationsRequired.setKeyStorage(List.of(KeyAttestationResistanceLevels.HIGH,
                KeyAttestationResistanceLevels.MODERATE));
        SupportedCredentialConfiguration config = new SupportedCredentialConfiguration()
                .setFormat(VCFormat.SD_JWT_VC)
                .setVct("https://credentials.example.com/test-credential")
                .setCryptographicBindingMethodsSupported(List.of("jwk"))
                .setProofTypesSupported(ProofTypesSupported.parse(session, keyAttestationsRequired, List.of("ES256")));

        context.setCredentialConfig(config)
                .setCredentialRequest(new CredentialRequest());
        return context;
    }

    private static String createValidAttestationJwt(KeycloakSession session,
                                                    KeyWrapper attestationKey,
                                                    JWK proofJwk,
                                                    String cNonce) {
        return createValidAttestationJwt(session, attestationKey, List.of(proofJwk), cNonce,
                AttestationValidatorUtil.ATTESTATION_JWT_TYP);
    }

    private static String createValidAttestationJwt(KeycloakSession session,
                                                    KeyWrapper attestationKey,
                                                    List<JWK> proofJwks,
                                                    String cNonce,
                                                    String typ) {
        if (AttestationValidatorUtil.ATTESTATION_JWT_TYP.equals(typ)) {
            return OID4VCProofTestUtils.generateAttestationProof(
                    attestationKey,
                    cNonce,
                    proofJwks,
                    List.of(KeyAttestationResistanceLevels.HIGH),
                    List.of(KeyAttestationResistanceLevels.HIGH),
                    null
            );
        }

        // Keep support for non-default typ variants used by dedicated compatibility tests.
        KeyAttestationJwtBody payload = new KeyAttestationJwtBody();
        payload.setIat((long) TIME_PROVIDER.currentTimeSeconds());
        payload.setNonce(cNonce);
        payload.setAttestedKeys(proofJwks);
        payload.setKeyStorage(List.of(KeyAttestationResistanceLevels.HIGH));
        payload.setUserAuthentication(List.of(KeyAttestationResistanceLevels.HIGH));

        return new JWSBuilder()
                .type(typ)
                .kid(attestationKey.getKid())
                .jsonContent(payload)
                .sign(new ECDSASignatureSignerContext(attestationKey));
    }

    private static String generateJwtProofWithKeyAttestation(KeycloakSession session,
                                                             KeyWrapper proofKey,
                                                             String attestationJwt,
                                                             String cNonce) {
        try {
            JWK proofJwk = JWKBuilder.create().ec(proofKey.getPublicKey());
            proofJwk.setKeyId(proofKey.getKid());
            proofJwk.setAlgorithm(proofKey.getAlgorithm());

            AccessToken token = new AccessToken();
            String credentialIssuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
            token.addAudience(credentialIssuer);
            token.setNonce(cNonce);
            token.issuedNow();

            Map<String, Object> header = new HashMap<>();
            header.put("alg", proofKey.getAlgorithm());
            header.put("typ", JwtProofValidator.PROOF_JWT_TYP);
            header.put("jwk", proofJwk);
            header.put("key_attestation", attestationJwt);

            return new JWSBuilder() {
                @Override
                protected String encodeHeader(String sigAlgName) {
                    try {
                        return Base64Url.encode(JsonSerialization.writeValueAsBytes(header));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to encode header", e);
                    }
                }
            }.jsonContent(token).sign(new ECDSASignatureSignerContext(proofKey));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT proof with key attestation", e);
        }
    }

    private static KeyAttestationJwtBody createAttestationPayload(JWK proofJwk, String cNonce) {
        KeyAttestationJwtBody payload = new KeyAttestationJwtBody();
        payload.setIat((long) TIME_PROVIDER.currentTimeSeconds());
        payload.setNonce(cNonce);
        payload.setAttestedKeys(List.of(proofJwk));
        payload.setKeyStorage(List.of(KeyAttestationResistanceLevels.HIGH));
        payload.setUserAuthentication(List.of(KeyAttestationResistanceLevels.HIGH));
        return payload;
    }

    private static void runAttestationWithValidResistanceLevels(KeycloakSession session, String cNonce) throws VCIssuerException {
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
            payload.setKeyStorage(List.of(
                    KeyAttestationResistanceLevels.HIGH,
                    KeyAttestationResistanceLevels.MODERATE
            ));
            payload.setUserAuthentication(List.of(
                    KeyAttestationResistanceLevels.ENHANCED_BASIC,
                    KeyAttestationResistanceLevels.BASIC
            ));

            String attestationJwt = OID4VCProofTestUtils.generateAttestationProof(
                    attestationKey,
                    cNonce,
                    List.of(proofJwk),
                    List.of(
                            KeyAttestationResistanceLevels.HIGH,
                            KeyAttestationResistanceLevels.MODERATE
                    ),
                    List.of(
                            KeyAttestationResistanceLevels.ENHANCED_BASIC,
                            KeyAttestationResistanceLevels.BASIC
                    ),
                    null
            );

            VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
            KeyAttestationsRequired attestationRequirements = new KeyAttestationsRequired();
            attestationRequirements.setKeyStorage(List.of(
                    KeyAttestationResistanceLevels.HIGH,
                    KeyAttestationResistanceLevels.MODERATE,
                    KeyAttestationResistanceLevels.ENHANCED_BASIC
            ));
            attestationRequirements.setUserAuthentication(List.of(
                    KeyAttestationResistanceLevels.BASIC,
                    KeyAttestationResistanceLevels.ENHANCED_BASIC
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

            assertNotNull(attestedKeys, "Attested keys should not be null");
            assertEquals(1, attestedKeys.size(), "Should contain exactly one attested key");
            assertEquals(proofKey.getKid(), attestedKeys.get(0).getKeyId(),
                    "Attested key ID should match proof key ID");
        } catch (Exception e) {
            LOGGER.error("Validation failed with valid resistance levels", e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    private static void runValidAttestationProofTest(KeycloakSession session, String cNonce) {
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

        assertNotNull(attestedKeys, "Attested keys should not be null");
        assertEquals(1, attestedKeys.size(), "Should contain exactly one attested key");
        assertEquals(proofKey.getKid(), attestedKeys.get(0).getKeyId(),
                "Attested key ID should match proof key ID");
    }

    private static void runInvalidAttestationProofTest(KeycloakSession session) {
        KeyWrapper attestationKey = getECKey("attestationKey");
        String invalidAttestationJwt = "invalid.jwt.token";

        VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
        vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setAttestation(List.of(invalidAttestationJwt)));

        AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(
                Map.of(attestationKey.getKid(), JWKBuilder.create().ec(attestationKey.getPublicKey())));
        AttestationProofValidator validator = new AttestationProofValidator(session, keyResolver);

        assertThrows(VCIssuerException.class, () -> validator.validateProof(vcIssuanceContext),
                "Expected VCIssuerException to be thrown");
    }

    private static void runValidJwtProofWithKeyAttestationTest(KeycloakSession session, String cNonce) {
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
    }

    private static void runInvalidJwtProofWithKeyAttestationTest(KeycloakSession session) throws VCIssuerException {
        KeyWrapper attestationKey = getECKey("attestationKey");
        String invalidJwtProof = "invalid.jwt.token";

        VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
        vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setJwt(List.of(invalidJwtProof)));

        AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(
                Map.of(attestationKey.getKid(), JWKBuilder.create().ec(attestationKey.getPublicKey())));
        JwtProofValidator validator = new JwtProofValidator(session, keyResolver);

        validator.validateProof(vcIssuanceContext);
    }

    private static void runInvalidAttestationSignatureTest(KeycloakSession session, String cNonce) throws Exception {
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

        AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(
                Map.of(attestationKey.getKid(), JWKBuilder.create().ec(attestationKey.getPublicKey())));
        AttestationProofValidator validator = new AttestationProofValidator(session, keyResolver);

        assertThrows(VCIssuerException.class, () -> validator.validateProof(vcIssuanceContext),
                "Expected VCIssuerException to be thrown");
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

        AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(
                Map.of(attestationKey.getKid(), JWKBuilder.create().ec(attestationKey.getPublicKey())));
        AttestationProofValidator validator = new AttestationProofValidator(session, keyResolver);

        assertThrows(VCIssuerException.class, () -> validator.validateProof(vcIssuanceContext),
                "Expected VCIssuerException to be thrown");
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

            String attestationJwt = OID4VCProofTestUtils.generateAttestationProof(
                    attestationKey,
                    cNonce,
                    List.of(proofJwk1, proofJwk2),
                    List.of(KeyAttestationResistanceLevels.HIGH),
                    List.of(KeyAttestationResistanceLevels.HIGH),
                    null
            );

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
            payload.setKeyStorage(List.of(KeyAttestationResistanceLevels.HIGH));
            payload.setUserAuthentication(List.of(KeyAttestationResistanceLevels.HIGH));

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

            assertNotNull(attestedKeys, "Attested keys should not be null");
            assertEquals(1, attestedKeys.size(), "Should contain exactly one attested key");
            assertEquals(proofKey.getKid(), attestedKeys.get(0).getKeyId(),
                    "Attested key ID should match proof key ID");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void runAttestationWithInvalidResistanceLevels(KeycloakSession session, String cNonce) throws VCIssuerException {
        try {
            KeyWrapper attestationKey = getECKey("attestationKey");
            KeyWrapper proofKey = getECKey("proofKey");

            JWK proofJwk = JWKBuilder.create().ec(proofKey.getPublicKey());
            proofJwk.setKeyId(proofKey.getKid());
            proofJwk.setAlgorithm(proofKey.getAlgorithm());

            String attestationJwt = OID4VCProofTestUtils.generateAttestationProof(
                    attestationKey,
                    cNonce,
                    List.of(proofJwk),
                    List.of("INVALID_LEVEL"),
                    null,
                    null
            );

            VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
            vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setAttestation(List.of(attestationJwt)));

            AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(
                    Map.of(attestationKey.getKid(), JWKBuilder.create().ec(attestationKey.getPublicKey()))
            );

            new AttestationProofValidator(session, keyResolver).validateProof(vcIssuanceContext);
            fail("Expected VCIssuerException for invalid resistance level");
        } catch (VCIssuerException e) {
            throw e;
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
            assertEquals("key_storage is required but was missing.", e.getMessage(),
                    "Expected error about missing keys but got: " + e.getMessage());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
}
