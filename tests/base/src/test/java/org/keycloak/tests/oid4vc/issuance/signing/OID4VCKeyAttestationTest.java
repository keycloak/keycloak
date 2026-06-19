package org.keycloak.tests.oid4vc.issuance.signing;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.keycloak.OID4VCConstants.KeyAttestationResistanceLevels;
import org.keycloak.VCFormat;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderConfig;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderFactory;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.config.TruststoreOptions;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.def.DefaultCryptoProvider;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.ClientModel;
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
import org.keycloak.protocol.oid4vc.issuance.keybinding.JwtProofValidatorFactory;
import org.keycloak.protocol.oid4vc.issuance.keybinding.StaticAttestationKeyResolver;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.KeyAttestationJwtBody;
import org.keycloak.protocol.oid4vc.model.KeyAttestationsRequired;
import org.keycloak.protocol.oid4vc.model.ProofTypesSupported;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.SupportedProofTypeData;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCProofTestUtils;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.keycloak.protocol.oid4vc.model.ProofType.ATTESTATION;
import static org.keycloak.protocol.oid4vc.model.ProofType.JWT;
import static org.keycloak.protocol.oidc.utils.JWKSServerUtils.toJwk;
import static org.keycloak.tests.oid4vc.OID4VCProofTestUtils.toJwks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Migrated OID4VCKeyAttestationTest to the new test framework.
 */
@KeycloakIntegrationTest(config = OID4VCKeyAttestationTest.ServerConfig.class)
public class OID4VCKeyAttestationTest extends OID4VCIssuerTestBase {

    private static final Logger LOGGER = Logger.getLogger(OID4VCKeyAttestationTest.class);

    private static final TimeProvider TIME_PROVIDER = new OID4VCIssuerTestBase.StaticTimeProvider(1000);
    private static final X5cTestCertificateChain X5C_TEST_CERTIFICATE_CHAIN = X5cTestCertificateChain.create();

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    public static class ServerConfig extends OID4VCIssuerTestBase.VCTestServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return super.configure(config)
                    .option(TruststoreOptions.TRUSTSTORE_PATHS.getKey(), X5C_TEST_CERTIFICATE_CHAIN.caCertificatePath());
        }
    }

    private record X5cTestCertificateChain(String caCertificatePem, String leafCertificatePem,
                                           String leafPrivateKeyPem, String caCertificatePath) {

        private static X5cTestCertificateChain create() {
            try {
                if (!CryptoIntegration.isInitialised()) {
                    CryptoIntegration.setProvider(new DefaultCryptoProvider());
                }

                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
                keyGen.initialize(new ECGenParameterSpec("secp256r1"));
                KeyPair caKeyPair = keyGen.generateKeyPair();
                KeyPair leafKeyPair = keyGen.generateKeyPair();

                X509Certificate caCertificate = CertificateUtils.generateV1SelfSignedCertificate(caKeyPair,
                        "OID4VC Test CA");
                X509Certificate leafCertificate = CertificateUtils.generateV3Certificate(leafKeyPair,
                        caKeyPair.getPrivate(), caCertificate, "OID4VC Test Leaf");

                String caCertificatePem = PemUtils.addCertificateBeginEnd(PemUtils.encodeCertificate(caCertificate));
                String leafCertificatePem = PemUtils.addCertificateBeginEnd(PemUtils.encodeCertificate(leafCertificate));
                String leafPrivateKeyPem = Base64.encodeBytes(leafKeyPair.getPrivate().getEncoded());
                Path caCertificatePath = Files.createTempFile("oid4vc-x5c-test-ca", ".pem");
                Files.writeString(caCertificatePath, caCertificatePem, StandardCharsets.UTF_8);
                caCertificatePath.toFile().deleteOnExit();

                return new X5cTestCertificateChain(caCertificatePem, leafCertificatePem, leafPrivateKeyPem,
                        caCertificatePath.toString());
            } catch (Exception e) {
                throw new RuntimeException("Failed to create x5c test certificate chain", e);
            }
        }
    }

    private static void setupSessionContext(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(OID4VCIssuerTestBase.VCTestRealmConfig.TEST_REALM_NAME);
        session.getContext().setRealm(realm);

        ClientModel client = session.clients().getClientByClientId(realm, OID4VCI_PUBLIC_CLIENT_ID);
        session.getContext().setClient(client);
    }

    @AfterEach
    public void cleanup() {
        runOnServer.run(session -> {
            // Remove all trusted keys config
            session.identityProviders().remove(OID4VCI_ATTESTER_DEFAULT_TRUST_IDP_ALIAS);
        });
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
    public void testValidKidJwtProofWithKeyAttestation() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runValidKidJwtProofWithKeyAttestationTest(session, cNonce);
        });
    }

    @Test
    public void testJwtProofWithKeyAttestationMustContainProofKey() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runJwtProofWithKeyAttestationMustContainProofKeyTest(session, cNonce);
        });
    }

    @Test
    public void testJwtProofWithJwkAndKidHeadersIsRejected() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runJwtProofWithJwkAndKidHeadersIsRejectedTest(session, cNonce);
        });
    }

    @Test
    public void testValidX5cJwtProofWithoutAttestation() {
        String cNonce = getCNonce();
        String leafCertificatePem = X5C_TEST_CERTIFICATE_CHAIN.leafCertificatePem();
        String leafPrivateKeyPem = X5C_TEST_CERTIFICATE_CHAIN.leafPrivateKeyPem();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runValidX5cJwtProofWithoutAttestationTest(session, cNonce, leafCertificatePem, leafPrivateKeyPem);
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
    public void testJwtProofValidatorFactoryProofType() {
        runOnServer.run(session -> {
            setupSessionContext(session);
            JwtProofValidatorFactory factory = new JwtProofValidatorFactory();
            var validator = factory.create(session);
            assertEquals(JWT, validator.getProofType(), "The proof type should be 'jwt'.");
        });
    }

    @Test
    public void testInvalidAttestationSignature() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runInvalidAttestationSignatureTest(session, cNonce);
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
    public void testAttestationWithSelfSignedX5cCertificateChainIsRejected() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runAttestationWithSelfSignedX5cCertificateChainIsRejected(session, cNonce);
        });
    }

    @Test
    public void testAttestationWithX5cCertificateChain() {
        String cNonce = getCNonce();
        String leafCertificatePem = X5C_TEST_CERTIFICATE_CHAIN.leafCertificatePem();
        String leafPrivateKeyPem = X5C_TEST_CERTIFICATE_CHAIN.leafPrivateKeyPem();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runAttestationWithX5cCertificateChain(session, cNonce, leafCertificatePem, leafPrivateKeyPem);
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

    @ParameterizedTest
    @ValueSource(strings = {
            AttestationValidatorUtil.ATTESTATION_JWT_TYP,
            AttestationValidatorUtil.LEGACY_ATTESTATION_JWT_TYP
    })
    public void testAttestationProofWithIdPConfiguredTrustedKeys(String typ) {
        String cNonce = getCNonce();

        runOnServer.run(session -> {
                setupSessionContext(session);

                // Configure trusted attestation keys
                KeyWrapper attestationKey = getECKey("attestationKey");
                KeyWrapper attestationKeyAlt1 = getECKey("attestationKeyAlt1");
                KeyWrapper attestationKeyAlt2 = getRSAKey("attestationKeyAlt2");
                String attestationJwks = JsonSerialization.valueAsString(
                        toJwks(attestationKey, attestationKeyAlt1, attestationKeyAlt2));

                RealmModel realm = session.getContext().getRealm();
                configureTrustIdentityProvider(realm, OID4VCI_ATTESTER_DEFAULT_TRUST_IDP_ALIAS,
                        DefaultTrustIdentityProviderFactory.PROVIDER_ID,
                        Map.of(DefaultTrustIdentityProviderConfig.TRUSTED_JWKS, attestationJwks));

                KeyWrapper proofKey = getECKey("proofKey");
                JWK proofKeyJwk = Objects.requireNonNull(toJwk(proofKey));
                String attestationJwt = createValidAttestationJwt(attestationKey, List.of(proofKeyJwk), cNonce, typ);
                List<JWK> attestedKeys = runAttestationProofValidationWithDefaultKeyResolver(session, attestationJwt);

                assertEquals(1, attestedKeys.size());
                assertEquals("proofKey", attestedKeys.get(0).getKeyId());

                // With attestation embedded in Jwt proof
                String jwtProof = generateJwtProofWithKeyAttestation(session, proofKey, attestationJwt, cNonce);
                attestedKeys = runJwtAttestationProofValidationWithDefaultKeyResolver(session, jwtProof);

                assertEquals(1, attestedKeys.size());
                assertEquals("proofKey", attestedKeys.get(0).getKeyId());
        });
    }

    @Test
    public void testAttestationProofWithNonMatchingIdPConfiguredTrustedKeys() {
        String cNonce = getCNonce();

        KeyWrapper attestationKey = getECKey("attestationKey");
        String attestationJwks = JsonSerialization.valueAsString(toJwks(attestationKey));

        KeyWrapper unrelatedAttestationKey = getECKey("unrelatedAttestationKey");
        KeyWrapper proofKey = getECKey("proofKey");
        String attestationJwt = createValidAttestationJwt(unrelatedAttestationKey, toJwk(proofKey), cNonce);

        runOnServer.run(session -> {
            setupSessionContext(session);
            RealmModel realm = session.getContext().getRealm();
            configureTrustIdentityProvider(realm, OID4VCI_ATTESTER_DEFAULT_TRUST_IDP_ALIAS,
                    DefaultTrustIdentityProviderFactory.PROVIDER_ID,
                    Map.of(DefaultTrustIdentityProviderConfig.TRUSTED_JWKS, attestationJwks));

            VCIssuerException e = assertThrows(VCIssuerException.class,
                    () -> runAttestationProofValidationWithDefaultKeyResolver(session, attestationJwt));

            assertEquals("Key with kid 'unrelatedAttestationKey' not found in trusted key registry", e.getMessage());
        });
    }

    @Test
    public void testJwtProofMissingIssuerForClientBoundFlowAllowed() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runJwtProofMissingIssuerForClientBoundFlowAllowedTest(session, cNonce);
        });
    }

    @Test
    public void testJwtProofWithWrongIssuerForClientBoundFlowRejected() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runJwtProofWithWrongIssuerForClientBoundFlowRejectedTest(session, cNonce);
        });
    }

    @Test
    public void testJwtProofWithIssuerInAnonymousFlowRejected() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runJwtProofWithIssuerInAnonymousFlowRejectedTest(session, cNonce);
        });
    }

    @Test
    public void testJwtProofWithMultipleAudiencesRejected() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runJwtProofWithMultipleAudiencesRejectedTest(session, cNonce);
        });
    }

    @Test
    public void testJwtProofWithFutureIatRejected() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runJwtProofWithFutureIatRejectedTest(session, cNonce);
        });
    }

    @Test
    public void testJwtProofWithExpiredExpRejected() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runJwtProofWithExpiredExpRejectedTest(session, cNonce);
        });
    }

    @Test
    public void testJwtProofWithFutureNbfRejected() {
        String cNonce = getCNonce();
        runOnServer.run(session -> {
            setupSessionContext(session);
            runJwtProofWithFutureNbfRejectedTest(session, cNonce);
        });
    }


    private String getCNonce() {
        return oauth.oid4vc().nonceRequest().send().getNonce();
    }

    private static KeyWrapper getECKey(String keyId) {
        return OID4VCProofTestUtils.createEcKeyPair(keyId);
    }

    private static KeyWrapper getRSAKey(String keyId) {
        return OID4VCProofTestUtils.createRsaKeyPair(keyId);
    }

    private static VCIssuanceContext createVCIssuanceContext(KeycloakSession session) {
        VCIssuanceContext context = new VCIssuanceContext();
        KeyAttestationsRequired keyAttestationsRequired = new KeyAttestationsRequired();
        keyAttestationsRequired.setKeyStorage(List.of(KeyAttestationResistanceLevels.HIGH,
                KeyAttestationResistanceLevels.MODERATE));
        ProofTypesSupported proofTypesSupported = ProofTypesSupported.parse(session, keyAttestationsRequired, List.of("ES256"));
        SupportedProofTypeData defaultJwtData = new SupportedProofTypeData(List.of("ES256"), keyAttestationsRequired);
        proofTypesSupported.getSupportedProofTypes().putIfAbsent(JWT, defaultJwtData);
        proofTypesSupported.getSupportedProofTypes().putIfAbsent(ATTESTATION, defaultJwtData);

        SupportedCredentialConfiguration config = new SupportedCredentialConfiguration()
                .setFormat(VCFormat.SD_JWT_VC)
                .setVct("https://credentials.example.com/test-credential")
                .setCryptographicBindingMethodsSupported(List.of("jwk"))
                .setProofTypesSupported(proofTypesSupported);

        context.setCredentialConfig(config)
                .setCredentialRequest(new CredentialRequest());
        return context;
    }

    private static String createValidAttestationJwt(KeyWrapper attestationKey,
                                                    JWK proofJwk,
                                                    String cNonce) {
        return createValidAttestationJwt(attestationKey, List.of(proofJwk), cNonce,
                AttestationValidatorUtil.ATTESTATION_JWT_TYP);
    }

    private static String createValidAttestationJwt(KeyWrapper attestationKey,
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
        long iat = System.currentTimeMillis() / 1000L;
        payload.setIat(iat);
        payload.setExp(iat + 3600);
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
        return generateJwtProofWithKeyAttestation(session, proofKey, attestationJwt, cNonce, false);
    }

    private static String generateJwtProofWithKeyAttestation(KeycloakSession session,
                                                             KeyWrapper proofKey,
                                                             String attestationJwt,
                                                             String cNonce,
                                                             boolean useKidHeader) {
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
            if (useKidHeader) {
                header.put("kid", proofKey.getKid());
            } else {
                header.put("jwk", proofJwk);
            }
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

    private static String generateJwtProofWithJwkAndKid(KeycloakSession session, KeyWrapper proofKey, String cNonce) {
        try {
            JWK proofJwk = JWKBuilder.create().ec(proofKey.getPublicKey());
            proofJwk.setKeyId(proofKey.getKid());
            proofJwk.setAlgorithm(proofKey.getAlgorithm());

            AccessToken token = new AccessToken();
            String credentialIssuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
            token.addAudience(credentialIssuer);
            token.setNonce(cNonce);
            token.issuedNow();

            return new JWSBuilder()
                    .type(JwtProofValidator.PROOF_JWT_TYP)
                    .kid(proofKey.getKid())
                    .jwk(proofJwk)
                    .jsonContent(token)
                    .sign(new ECDSASignatureSignerContext(proofKey));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT proof with both jwk and kid", e);
        }
    }

    private static String generateJwtProofWithX5c(KeycloakSession session, KeyWrapper proofKey,
            X509Certificate cert, String cNonce) {
        try {
            AccessToken token = new AccessToken();
            String credentialIssuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
            token.addAudience(credentialIssuer);
            token.setNonce(cNonce);
            token.issuedNow();

            return new JWSBuilder()
                    .type(JwtProofValidator.PROOF_JWT_TYP)
                    .x5c(List.of(cert))
                    .jsonContent(token)
                    .sign(new ECDSASignatureSignerContext(proofKey));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT proof with x5c", e);
        }
    }

    private static KeyAttestationJwtBody createAttestationPayload(JWK proofJwk, String cNonce) {
        KeyAttestationJwtBody payload = new KeyAttestationJwtBody();
        long iat = TIME_PROVIDER.currentTimeSeconds();
        payload.setIat(iat);
        payload.setExp(iat + 3600);
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

        String attestationJwt = createValidAttestationJwt(attestationKey, proofJwk, cNonce);
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
        proofJwk.setKeyId(proofKey.getKid());
        proofJwk.setAlgorithm(proofKey.getAlgorithm());

        String attestationJwt = createValidAttestationJwt(attestationKey, proofJwk, cNonce);
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

    private static void runValidKidJwtProofWithKeyAttestationTest(KeycloakSession session, String cNonce) {
        KeyWrapper attestationKey = getECKey("attestationKey");
        KeyWrapper proofKey = getECKey("proofKey");

        JWK proofJwk = JWKBuilder.create().ec(proofKey.getPublicKey());
        proofJwk.setKeyId(proofKey.getKid());
        proofJwk.setAlgorithm(proofKey.getAlgorithm());

        String attestationJwt = createValidAttestationJwt(attestationKey, proofJwk, cNonce);
        String jwtProof = generateJwtProofWithKeyAttestation(session, proofKey, attestationJwt, cNonce, true);

        VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
        vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setJwt(List.of(jwtProof)));

        AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(
                Map.of(attestationKey.getKid(), JWKBuilder.create().ec(attestationKey.getPublicKey()))
        );
        JwtProofValidator validator = new JwtProofValidator(session, keyResolver);

        List<JWK> validatedKeys = validator.validateProof(vcIssuanceContext);
        assertNotNull(validatedKeys);
        assertEquals(1, validatedKeys.size());
        assertEquals(proofKey.getKid(), validatedKeys.get(0).getKeyId());
    }

    private static void runJwtProofWithKeyAttestationMustContainProofKeyTest(KeycloakSession session, String cNonce) {
        KeyWrapper attestationKey = getECKey("attestationKey");
        KeyWrapper proofKey = getECKey("proofKey");
        KeyWrapper differentKey = getECKey("differentKey");

        JWK differentJwk = JWKBuilder.create().ec(differentKey.getPublicKey());
        differentJwk.setKeyId(differentKey.getKid());
        differentJwk.setAlgorithm(differentKey.getAlgorithm());

        String attestationJwt = createValidAttestationJwt(attestationKey, differentJwk, cNonce);
        String jwtProof = generateJwtProofWithKeyAttestation(session, proofKey, attestationJwt, cNonce);

        VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
        vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setJwt(List.of(jwtProof)));

        AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(
                Map.of(attestationKey.getKid(), JWKBuilder.create().ec(attestationKey.getPublicKey()))
        );
        JwtProofValidator validator = new JwtProofValidator(session, keyResolver);

        assertThrows(VCIssuerException.class, () -> validator.validateProof(vcIssuanceContext),
                "Expected proof key mismatch against attested_keys to fail");
    }

    private static void runJwtProofWithJwkAndKidHeadersIsRejectedTest(KeycloakSession session, String cNonce) {
        KeyWrapper attestationKey = getECKey("attestationKey");
        KeyWrapper proofKey = getECKey("proofKey");

        String jwtProof = generateJwtProofWithJwkAndKid(session, proofKey, cNonce);

        VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
        vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setJwt(List.of(jwtProof)));

        AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(
                Map.of(attestationKey.getKid(), JWKBuilder.create().ec(attestationKey.getPublicKey()))
        );
        JwtProofValidator validator = new JwtProofValidator(session, keyResolver);

        VCIssuerException e = assertThrows(VCIssuerException.class, () -> validator.validateProof(vcIssuanceContext));
        assertTrue(e.getMessage().contains("mutually exclusive"),
                "Expected mutual exclusivity validation error but got: " + e.getMessage());
    }

    private static void runValidX5cJwtProofWithoutAttestationTest(KeycloakSession session, String cNonce,
            String leafCertificatePem, String leafPrivateKeyPem) {
        try {
            X509Certificate leafCert = PemUtils.decodeCertificate(leafCertificatePem);

            KeyWrapper proofKey = new KeyWrapper();
            proofKey.setPrivateKey(PemUtils.decodePrivateKey(leafPrivateKeyPem));
            proofKey.setPublicKey(leafCert.getPublicKey());
            proofKey.setAlgorithm("ES256");
            proofKey.setType(KeyType.EC);
            // Keep kid unset for this test so header contains only x5c (mutual exclusivity with kid/jwk).
            proofKey.setKid(null);

            String jwtProof = generateJwtProofWithX5c(session, proofKey, leafCert, cNonce);

            VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
            vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setJwt(List.of(jwtProof)));

            JwtProofValidator validator = new JwtProofValidator(session, new StaticAttestationKeyResolver(Map.of()));
            List<JWK> validatedKeys = validator.validateProof(vcIssuanceContext);

            assertNotNull(validatedKeys, "Validated keys should not be null");
            assertEquals(1, validatedKeys.size(), "Expected single validated key");
        } catch (Exception e) {
            throw new RuntimeException("x5c JWT proof validation failed", e);
        }
    }

    private static void runJwtProofMissingIssuerForClientBoundFlowAllowedTest(KeycloakSession session, String cNonce) {
        String credentialIssuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
        String jwtProof = OID4VCProofTestUtils.generateJwtProofWithClaims(List.of(credentialIssuer), cNonce, null,
                null, null, null);

        VCIssuanceContext context = createVCIssuanceContext(session);
        context.setCredentialRequest(new CredentialRequest().setProofs(new Proofs().setJwt(List.of(jwtProof))));
        context.setAuthResult(new AuthenticationManager.AuthResult(null, null,
                new AccessToken().issuedFor(OID4VCIssuerTestBase.OID4VCI_CLIENT_ID), null));

        JwtProofValidator validator = new JwtProofValidator(session, new StaticAttestationKeyResolver(Map.of()));
        List<JWK> validatedKeys = validator.validateProof(context);
        assertNotNull(validatedKeys);
        assertEquals(1, validatedKeys.size());
    }

    private static void runJwtProofWithWrongIssuerForClientBoundFlowRejectedTest(KeycloakSession session, String cNonce) {
        String credentialIssuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
        String jwtProof = OID4VCProofTestUtils.generateJwtProofWithClaims(List.of(credentialIssuer), cNonce,
                "wrong-client-id", null, null, null);

        VCIssuanceContext context = createVCIssuanceContext(session);
        context.setCredentialRequest(new CredentialRequest().setProofs(new Proofs().setJwt(List.of(jwtProof))));
        context.setAuthResult(new AuthenticationManager.AuthResult(null, null,
                new AccessToken().issuedFor(OID4VCIssuerTestBase.OID4VCI_CLIENT_ID), null));

        JwtProofValidator validator = new JwtProofValidator(session, new StaticAttestationKeyResolver(Map.of()));
        assertThrows(VCIssuerException.class, () -> validator.validateProof(context));
    }

    private static void runJwtProofWithIssuerInAnonymousFlowRejectedTest(KeycloakSession session, String cNonce) {
        String credentialIssuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
        String jwtProof = OID4VCProofTestUtils.generateJwtProofWithClaims(List.of(credentialIssuer), cNonce,
                OID4VCIssuerTestBase.OID4VCI_CLIENT_ID, null, null, null);

        VCIssuanceContext context = createVCIssuanceContext(session);
        context.setCredentialRequest(new CredentialRequest().setProofs(new Proofs().setJwt(List.of(jwtProof))));
        context.setAuthResult(null);

        JwtProofValidator validator = new JwtProofValidator(session, new StaticAttestationKeyResolver(Map.of()));
        assertThrows(VCIssuerException.class, () -> validator.validateProof(context));
    }

    private static void runJwtProofWithMultipleAudiencesRejectedTest(KeycloakSession session, String cNonce) {
        String credentialIssuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
        String jwtProof = OID4VCProofTestUtils.generateJwtProofWithClaims(
                List.of(credentialIssuer, "https://unrelated.example"), cNonce, OID4VCIssuerTestBase.OID4VCI_CLIENT_ID,
                null, null, null);

        VCIssuanceContext context = createVCIssuanceContext(session);
        context.setCredentialRequest(new CredentialRequest().setProofs(new Proofs().setJwt(List.of(jwtProof))));
        context.setAuthResult(new AuthenticationManager.AuthResult(null, null,
                new AccessToken().issuedFor(OID4VCIssuerTestBase.OID4VCI_CLIENT_ID), null));

        JwtProofValidator validator = new JwtProofValidator(session, new StaticAttestationKeyResolver(Map.of()));
        assertThrows(VCIssuerException.class, () -> validator.validateProof(context));
    }

    private static void runJwtProofWithFutureIatRejectedTest(KeycloakSession session, String cNonce) {
        String credentialIssuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
        long now = System.currentTimeMillis() / 1000L;
        String jwtProof = OID4VCProofTestUtils.generateJwtProofWithClaims(List.of(credentialIssuer), cNonce,
                OID4VCIssuerTestBase.OID4VCI_CLIENT_ID, now + 120, null, null);

        VCIssuanceContext context = createVCIssuanceContext(session);
        context.setCredentialRequest(new CredentialRequest().setProofs(new Proofs().setJwt(List.of(jwtProof))));
        context.setAuthResult(new AuthenticationManager.AuthResult(null, null,
                new AccessToken().issuedFor(OID4VCIssuerTestBase.OID4VCI_CLIENT_ID), null));

        JwtProofValidator validator = new JwtProofValidator(session, new StaticAttestationKeyResolver(Map.of()));
        assertThrows(VCIssuerException.class, () -> validator.validateProof(context));
    }

    private static void runJwtProofWithExpiredExpRejectedTest(KeycloakSession session, String cNonce) {
        String credentialIssuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
        long now = System.currentTimeMillis() / 1000L;
        String jwtProof = OID4VCProofTestUtils.generateJwtProofWithClaims(List.of(credentialIssuer), cNonce,
                OID4VCIssuerTestBase.OID4VCI_CLIENT_ID, now, now - 1, null);

        VCIssuanceContext context = createVCIssuanceContext(session);
        context.setCredentialRequest(new CredentialRequest().setProofs(new Proofs().setJwt(List.of(jwtProof))));
        context.setAuthResult(new AuthenticationManager.AuthResult(null, null,
                new AccessToken().issuedFor(OID4VCIssuerTestBase.OID4VCI_CLIENT_ID), null));

        JwtProofValidator validator = new JwtProofValidator(session, new StaticAttestationKeyResolver(Map.of()));
        assertThrows(VCIssuerException.class, () -> validator.validateProof(context));
    }

    private static void runJwtProofWithFutureNbfRejectedTest(KeycloakSession session, String cNonce) {
        String credentialIssuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
        long now = System.currentTimeMillis() / 1000L;
        String jwtProof = OID4VCProofTestUtils.generateJwtProofWithClaims(List.of(credentialIssuer), cNonce,
                OID4VCIssuerTestBase.OID4VCI_CLIENT_ID, now, null, now + 120);

        VCIssuanceContext context = createVCIssuanceContext(session);
        context.setCredentialRequest(new CredentialRequest().setProofs(new Proofs().setJwt(List.of(jwtProof))));
        context.setAuthResult(new AuthenticationManager.AuthResult(null, null,
                new AccessToken().issuedFor(OID4VCIssuerTestBase.OID4VCI_CLIENT_ID), null));

        JwtProofValidator validator = new JwtProofValidator(session, new StaticAttestationKeyResolver(Map.of()));
        assertThrows(VCIssuerException.class, () -> validator.validateProof(context));
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

    private static void runAttestationWithSelfSignedX5cCertificateChainIsRejected(KeycloakSession session, String cNonce) {
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

            VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
            vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setAttestation(List.of(attestationJwt)));

            AttestationKeyResolver keyResolver = new StaticAttestationKeyResolver(Map.of());
            AttestationProofValidator validator = new AttestationProofValidator(session, keyResolver);
            assertThrows(VCIssuerException.class, () -> validator.validateProof(vcIssuanceContext),
                    "Expected self-signed x5c key attestation to be rejected");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void runAttestationWithX5cCertificateChain(KeycloakSession session, String cNonce,
            String leafCertificatePem, String leafPrivateKeyPem) {
        try {
            X509Certificate leafCert = PemUtils.decodeCertificate(leafCertificatePem);

            KeyWrapper signerKey = new KeyWrapper();
            signerKey.setPrivateKey(PemUtils.decodePrivateKey(leafPrivateKeyPem));
            signerKey.setPublicKey(leafCert.getPublicKey());
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
                    .x5c(List.of(leafCert))
                    .jsonContent(payload)
                    .sign(new ECDSASignatureSignerContext(signerKey));

            VCIssuanceContext vcIssuanceContext = createVCIssuanceContext(session);
            vcIssuanceContext.getCredentialRequest().setProofs(new Proofs().setAttestation(List.of(attestationJwt)));

            AttestationProofValidator validator = new AttestationProofValidator(session,
                    new StaticAttestationKeyResolver(Map.of()));
            List<JWK> attestedKeys = validator.validateProof(vcIssuanceContext);

            assertNotNull(attestedKeys, "Attested keys should not be null");
            assertEquals(1, attestedKeys.size(), "Should contain exactly one attested key");
            assertEquals(proofKey.getKid(), attestedKeys.get(0).getKeyId(),
                    "Attested key ID should match proof key ID");
        } catch (Exception e) {
            throw new RuntimeException("x5c key attestation trusted chain test failed", e);
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

    private static List<JWK> runAttestationProofValidationWithDefaultKeyResolver(
            KeycloakSession session, String attestationJwt) {
        VCIssuanceContext context = createVCIssuanceContext(session);
        context.getCredentialRequest().setProofs(new Proofs().setAttestation(List.of(attestationJwt)));

        AttestationProofValidator validator = new AttestationProofValidatorFactory().create(session);
        return validator.validateProof(context);
    }

    private static List<JWK> runJwtAttestationProofValidationWithDefaultKeyResolver(
            KeycloakSession session, String jwtProof) {
        VCIssuanceContext context = createVCIssuanceContext(session);
        context.getCredentialRequest().setProofs(new Proofs().setJwt(List.of(jwtProof)));

        JwtProofValidator validator = new JwtProofValidatorFactory().create(session);
        return validator.validateProof(context);
    }
}
