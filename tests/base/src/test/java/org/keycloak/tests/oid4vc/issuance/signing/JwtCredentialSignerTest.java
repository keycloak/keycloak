package org.keycloak.tests.oid4vc.issuance.signing;

import java.security.PublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
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
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class JwtCredentialSignerTest extends OID4VCTest {

    @InjectRunOnServer
    private RunOnServerClient runOnServer;

    @Test
    public void testUnsupportedCredentialBody() {
        try {
            withCausePropagation(() -> runOnServer.run(session -> new JwtCredentialSigner(session).signCredential(
                    new LDCredentialBody(getTestCredential(Map.of())),
                    new CredentialBuildConfig()
            )));
            fail("Expected CredentialSignerException");
        } catch (Throwable t) {
            assertTrue(t instanceof CredentialSignerException);
        }
    }

    @Test
    public void testUnsupportedAlgorithm() {
        try {
            withCausePropagation(() -> runOnServer.run(session ->
                    testSignJwtCredential(session, getKeyIdFromSession(session), "unsupported-algorithm", Map.of())));
            fail("Expected CredentialSignerException");
        } catch (Throwable t) {
            assertTrue(t instanceof CredentialSignerException);
        }
    }

    @Test
    public void testFailIfNoKey() {
        try {
            withCausePropagation(() -> runOnServer.run(session ->
                    testSignJwtCredential(session, "no-such-key", Algorithm.RS256, Map.of())));
            fail("Expected CredentialSignerException");
        } catch (Throwable t) {
            assertTrue(t instanceof CredentialSignerException);
        }
    }

    @Test
    public void testRsaSignedCredentialWithOutIssuanceDate() {
        runOnServer.run(session ->
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
        runOnServer.run(session ->
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
        runOnServer.run(session ->
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
                new OID4VCIssuerTestBase.StaticTimeProvider(1000),
                session
        );

        CredentialBody credentialBody = builder.buildCredentialBody(testCredential, credentialBuildConfig);
        String jwtCredential = jwtCredentialSigner.signCredential(credentialBody, credentialBuildConfig);

        KeyWrapper keyWrapper = getKeyFromSession(session);
        SignatureVerifierContext verifierContext;
        switch (algorithm) {
            case Algorithm.ES256:
                verifierContext = new ServerECDSASignatureVerifierContext(keyWrapper);
                break;
            case Algorithm.RS256:
                verifierContext = new AsymmetricSignatureVerifierContext(keyWrapper);
                break;
            default:
                fail("Algorithm not supported.");
                return;
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

            assertEquals(TEST_EXPIRATION_DATE.getEpochSecond(), theToken.getExp().longValue(),
                    "JWT claim in JWT encoded VC or VP MUST be used to set the value of the expirationDate of the VC");
            if (claims.containsKey("issuanceDate")) {
                assertEquals(((Instant) claims.get("issuanceDate")).getEpochSecond(), theToken.getNbf().longValue(),
                        "VC Data Model v1.1 specifies that issuanceDate property MUST be represented as nbf");
            } else {
                assertEquals(1000L, theToken.getNbf().longValue(),
                        "If issuanceDate is absent, nbf should default to test time provider");
            }
            assertEquals(TEST_DID.toString(), theToken.getIssuer(), "The issuer should be set in the token.");
            assertEquals(testCredential.getId().toString(), theToken.getId(), "The credential ID should be set as token ID.");
            Optional.ofNullable(testCredential.getCredentialSubject().getClaims().get("id"))
                    .ifPresent(id -> assertEquals(id.toString(), theToken.getSubject()));

            assertNotNull(theToken.getOtherClaims().get("vc"), "The credentials should be included at the vc-claim.");
            VerifiableCredential credential = JsonSerialization.mapper.convertValue(theToken.getOtherClaims().get("vc"), VerifiableCredential.class);
            assertEquals(TEST_TYPES, credential.getType(), "The types should be included");
            assertEquals(TEST_DID, credential.getIssuer(), "The issuer should be included");
            assertEquals(TEST_EXPIRATION_DATE, credential.getExpirationDate(), "The expiration date should be included");
            if (claims.containsKey("issuanceDate")) {
                assertEquals(claims.get("issuanceDate"), credential.getIssuanceDate(), "The issuance date should be included");
            }

            CredentialSubject subject = credential.getCredentialSubject();
            claims.entrySet().stream()
                    .filter(e -> !e.getKey().equals("issuanceDate"))
                    .forEach(e -> assertEquals(e.getValue(), subject.getClaims().get(e.getKey()),
                            String.format("All additional claims should be set - %s is incorrect", e.getKey())));
        } catch (VerificationException e) {
            fail("Was not able to get the token from the verifier.");
        }
    }
}
