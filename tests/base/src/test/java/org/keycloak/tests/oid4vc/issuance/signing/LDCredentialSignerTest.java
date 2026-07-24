package org.keycloak.tests.oid4vc.issuance.signing;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.JwtCredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.LDCredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.LDCredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.signing.CredentialSignerException;
import org.keycloak.protocol.oid4vc.issuance.signing.LDCredentialSigner;
import org.keycloak.protocol.oid4vc.issuance.signing.vcdm.Ed255192018Suite;
import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oid4vc.model.vcdm.LdProof;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// LDP_VC format is intentionally disabled to keep scope limited to supported formats.
@Disabled("LDP_VC format is currently not supported and providers are disabled.")
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class LDCredentialSignerTest extends OID4VCIssuerTestBase {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @BeforeEach
    public void setup() {
        CryptoIntegration.init(this.getClass().getClassLoader());
    }

    @TestSetup
    public void configureTestRealm() {
        super.configureTestRealm();
        ComponentsResource components = testRealm.admin().components();
        components.add(getEdDSAKeyProvider()).close();
    }

    @Test
    public void testUnsupportedCredentialBody() throws Throwable {
        runOnServer.run(session ->
                assertThrows(
                        CredentialSignerException.class,
                        () -> new LDCredentialSigner(session, new StaticTimeProvider(1000))
                                .signCredential(
                                        new JwtCredentialBody(new JWSBuilder().jsonContent(Map.of())),
                                        new CredentialBuildConfig()
                                )
                )
        );
    }

    // If an unsupported algorithm is provided, signing should reliably fail.
    @Test
    public void testUnsupportedLdpType() throws Throwable {
        runOnServer.run(session ->
                assertThrows(
                        CredentialSignerException.class,
                        () -> testSignLdCredential(
                                session,
                                getKeyIdFromSession(session),
                                Map.of(),
                                null,
                                "UnsupportedSignatureType")
                )
        );
    }

    // If an unknown key is provided, signing should reliably fail.
    @Test
    public void testFailIfNoKey() throws Throwable {
        runOnServer.run(session ->
                assertThrows(
                        CredentialSignerException.class,
                        () -> testSignLdCredential(
                                session,
                                "no-such-key",
                                Map.of(),
                                null,
                                Ed255192018Suite.PROOF_TYPE)
                )
        );
    }

    // The provided credentials should be successfully signed as a JWT-VC.
    @Test
    public void testLdpSignedCredentialWithOutIssuanceDate() {
        runOnServer.run(session ->
                testSignLdCredential(
                        session,
                        getKeyIdFromSession(session),
                        Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                "test", "test",
                                "arrayClaim", List.of("a", "b", "c")),
                        null,
                        Ed255192018Suite.PROOF_TYPE)
        );
    }

    @Test
    public void testLdpSignedCredentialWithIssuanceDate() {
        runOnServer.run(session ->
                testSignLdCredential(
                        session,
                        getKeyIdFromSession(session),
                        Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                "test", "test",
                                "arrayClaim", List.of("a", "b", "c"),
                                "issuanceDate", Instant.ofEpochSecond(10)),
                        null,
                        Ed255192018Suite.PROOF_TYPE)
        );
    }

    @Test
    public void testLdpSignedCredentialWithCustomKid() {
        runOnServer.run(session ->
                testSignLdCredential(
                        session,
                        getKeyIdFromSession(session),
                        Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                "test", "test",
                                "arrayClaim", List.of("a", "b", "c"),
                                "issuanceDate", Instant.ofEpochSecond(10)),
                        "did:web:test.org#the-key-id",
                        Ed255192018Suite.PROOF_TYPE)
        );
    }

    @Test
    public void testLdpSignedCredentialWithoutAdditionalClaims() {
        runOnServer.run(session ->
                testSignLdCredential(
                        session,
                        getKeyIdFromSession(session),
                        Map.of(),
                        null,
                        Ed255192018Suite.PROOF_TYPE)
        );
    }

    public static void testSignLdCredential(
            KeycloakSession session, String signingKeyId, Map<String, Object> claims,
            String overrideKeyId, String ldpProofType) {
        CredentialBuildConfig credentialBuildConfig = new CredentialBuildConfig()
                .setCredentialIssuer(TEST_ISSUER_DID)
                .setTokenJwsType("JWT")
                .setSigningKeyId(signingKeyId)
                .setSigningAlgorithm("EdDSA")
                .setOverrideKeyId(overrideKeyId)
                .setLdpProofType(ldpProofType);

        LDCredentialSigner ldCredentialSigner = new LDCredentialSigner(
                session, new StaticTimeProvider(1000));

        VerifiableCredential testCredential = getTestCredential(claims);
        LDCredentialBody ldCredentialBody = new LDCredentialBuilder()
                .buildCredentialBody(testCredential, credentialBuildConfig);

        VerifiableCredential verifiableCredential = ldCredentialSigner
                .signCredential(ldCredentialBody, credentialBuildConfig);

        assertEquals(TEST_TYPES, verifiableCredential.getType(), "The types should be included");
        assertEquals(TEST_ISSUER_DID, String.valueOf(verifiableCredential.getIssuer()), "The issuer should be included");
        assertNotNull(verifiableCredential.getContext(), "The context needs to be set.");
        assertEquals(TEST_EXPIRATION_DATE, verifiableCredential.getExpirationDate(), "The expiration date should be included");
        if (claims.containsKey("issuanceDate")) {
            assertEquals(claims.get("issuanceDate"), verifiableCredential.getIssuanceDate(), "The issuance date should be included");
        }

        CredentialSubject subject = verifiableCredential.getCredentialSubject();
        claims.entrySet().stream()
                .filter(e -> !e.getKey().equals("issuanceDate"))
                .forEach(e -> assertEquals(e.getValue(), subject.getClaims().get(e.getKey()), String.format("All additional claims should be set - %s is incorrect", e.getKey())));

        assertNotNull(verifiableCredential.getAdditionalProperties().get("proof"), "The credential should contain a signed proof.");

        LdProof ldProof = (LdProof) verifiableCredential.getAdditionalProperties().get("proof");
        KeyWrapper keyWrapper = getKeyFromSession(session);
        String expectedKid = Optional.ofNullable(overrideKeyId).orElse(keyWrapper.getKid());
        assertEquals(expectedKid, ldProof.getVerificationMethod(), "The verification method should be set to the key id.");
    }

    private ComponentRepresentation getEdDSAKeyProvider() {
        ComponentRepresentation component = new ComponentRepresentation();
        component.setProviderType(KeyProvider.class.getName());
        component.setName("eddsa-generated");
        component.setId(UUID.randomUUID().toString());
        component.setProviderId("eddsa-generated");
        component.setConfig(new MultivaluedHashMap<>(
                Map.of("eddsaEllipticCurveKey", List.of("Ed25519"))
        ));
        return component;
    }
}
