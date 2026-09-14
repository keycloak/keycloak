package org.keycloak.tests.oid4vc.issuance.credentialbuilder;

import java.security.KeyPairGenerator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.OID4VCConstants;
import org.keycloak.VCFormat;
import org.keycloak.common.VerificationException;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.SdJwtCredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.SdJwtCredentialBuilder;
import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.IssuerSignedJwtVerificationOpts;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.issuance.signing.OID4VCTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_CNF;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_ISSUER;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_JWK;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_SD;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_SD_HASH_ALGORITHM;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_VCT;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class SdJwtCredentialBuilderTest extends CredentialBuilderTest {

    @Test
    public void shouldBuildSdJwtCredentialSuccessfully() throws Exception {
        testSignSDJwtCredential(
                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                        "test", "test",
                        "arrayClaim", List.of("a", "b", "c")),
                0,
                List.of()
        );
    }

    @Test
    public void buildSdJwtCredential_WithDecoys() throws Exception {
        testSignSDJwtCredential(
                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                        "test", "test",
                        "arrayClaim", List.of("a", "b", "c")),
                6,
                List.of()
        );
    }

    @Test
    public void buildSdJwtCredential_WithVisibleClaims() throws Exception {
        testSignSDJwtCredential(
                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                        "test", "test",
                        "arrayClaim", List.of("a", "b", "c")),
                6,
                List.of("test")
        );
    }

    @Test
    public void buildSdJwtCredential_WithNoClaims() throws Exception {
        testSignSDJwtCredential(
                Map.of(),
                0,
                List.of()
        );
    }

    @Test
    public void buildSdJwtCredential_DisclosesArrayElementsIndividually() throws Exception {
        CredentialBuildConfig credentialBuildConfig = new CredentialBuildConfig()
                .setCredentialIssuer(TEST_ISSUER_DID)
                .setCredentialType("https://credentials.example.com/test-credential")
                .setTokenJwsType(VCFormat.SD_JWT_VC)
                .setHashAlgorithm(OID4VCConstants.SD_HASH_DEFAULT_ALGORITHM)
                .setNumberOfDecoys(0)
                .setSdJwtVisibleClaims(List.of());

        VerifiableCredential testCredential = getTestCredential(
                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                        "roles", List.of("admin", "auditor", "user")));

        SdJwtCredentialBody sdJwtCredentialBody = new SdJwtCredentialBuilder()
                .buildCredentialBody(testCredential, credentialBuildConfig);
        SdJwtVP sdJwt = SdJwtVP.of(sdJwtCredentialBody.sign(exampleSigner()));

        // The claim name stays visible while its elements are undisclosed one by one
        JsonNode rolesNode = sdJwt.getIssuerSignedJWT().getPayload().get("roles");
        assertNotNull(rolesNode, "Array claim name must stay visible for per-element disclosure");
        assertEquals(3, rolesNode.size(), "Each array element must be undisclosed separately");

        Map<String, JsonNode> elementDisclosures = sdJwt.getDisclosures().entrySet().stream()
                .map(e -> Map.entry(e.getKey(), decodeDisclosure(e.getValue())))
                .filter(e -> e.getValue().size() == 2)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertEquals(Set.of("admin", "auditor", "user"),
                elementDisclosures.values().stream().map(node -> node.get(1).asText()).collect(Collectors.toSet()),
                "Every element must have its own disclosure");

        // A holder can selectively present a single element
        String adminDigest = elementDisclosures.entrySet().stream()
                .filter(e -> "admin".equals(e.getValue().get(1).asText()))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElseThrow();
        SdJwtVP presentation = SdJwtVP.of(sdJwt.present(List.of(adminDigest), false, null, null));
        JsonNode disclosedPayload = presentation.getSdJwtVerificationContext()
                .verifyIssuance(List.of(exampleVerifier()),
                        IssuerSignedJwtVerificationOpts.builder()
                                .withIatCheck(true)
                                .withNbfCheck(true)
                                .withExpCheck(true)
                                .build(),
                        null);

        assertEquals(1, disclosedPayload.get("roles").size(), "Only the selected element must be disclosed");
        assertEquals("admin", disclosedPayload.get("roles").get(0).asText());
    }

    @Test
    public void buildSdJwtCredential_DisclosesSetElementsIndividually() throws Exception {
        // Regression test: OID4VCTargetRoleMapper stores roles as a HashSet, not a List.
        // Array-ness must be determined from the serialized JSON value, not the Java type.
        CredentialBuildConfig credentialBuildConfig = new CredentialBuildConfig()
                .setCredentialIssuer(TEST_ISSUER_DID)
                .setCredentialType("https://credentials.example.com/test-credential")
                .setTokenJwsType(VCFormat.SD_JWT_VC)
                .setHashAlgorithm(OID4VCConstants.SD_HASH_DEFAULT_ALGORITHM)
                .setNumberOfDecoys(0)
                .setSdJwtVisibleClaims(List.of());

        Set<String> rolesSet = new HashSet<>(List.of("admin", "auditor", "user"));
        VerifiableCredential testCredential = getTestCredential(
                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                        "roles", rolesSet));

        SdJwtCredentialBody sdJwtCredentialBody = new SdJwtCredentialBuilder()
                .buildCredentialBody(testCredential, credentialBuildConfig);
        SdJwtVP sdJwt = SdJwtVP.of(sdJwtCredentialBody.sign(exampleSigner()));

        // The claim name stays visible while its elements are undisclosed one by one
        JsonNode rolesNode = sdJwt.getIssuerSignedJWT().getPayload().get("roles");
        assertNotNull(rolesNode, "Set-valued claim name must stay visible for per-element disclosure");
        assertTrue(rolesNode.isArray(), "Set-valued claim must be serialized as a JSON array");
        assertEquals(3, rolesNode.size(), "Each set element must be undisclosed separately");

        Map<String, JsonNode> elementDisclosures = sdJwt.getDisclosures().entrySet().stream()
                .map(e -> Map.entry(e.getKey(), decodeDisclosure(e.getValue())))
                .filter(e -> e.getValue().size() == 2)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertEquals(rolesSet,
                elementDisclosures.values().stream().map(node -> node.get(1).asText()).collect(Collectors.toSet()),
                "Every set element must have its own disclosure");
    }

    static Stream<Integer> decoyCountProvider() {
        return Stream.of(0, 1, 5);
    }

    @ParameterizedTest
    @MethodSource("decoyCountProvider")
    public void shouldBindHolderKeyWithCnfClaim(int decoys) throws Exception {
        CredentialBuildConfig credentialBuildConfig = new CredentialBuildConfig()
                .setCredentialIssuer(TEST_ISSUER_DID)
                .setCredentialType("https://credentials.example.com/test-credential")
                .setTokenJwsType(VCFormat.SD_JWT_VC)
                .setHashAlgorithm(OID4VCConstants.SD_HASH_DEFAULT_ALGORITHM)
                .setNumberOfDecoys(decoys)
                .setSdJwtVisibleClaims(List.of(CLAIM_NAME_CNF));

        VerifiableCredential testCredential = getTestCredential(
                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()), "test", "value"));

        SdJwtCredentialBody sdJwtCredentialBody = new SdJwtCredentialBuilder()
                .buildCredentialBody(testCredential, credentialBuildConfig);

        var holderKeyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        JWK holderJwk = JWKBuilder.create().kid("holder-key-1").ec(holderKeyPair.getPublic());
        sdJwtCredentialBody.addKeyBinding(holderJwk);

        String sdJwtString = sdJwtCredentialBody.sign(exampleSigner());
        SdJwtVP sdJwt = SdJwtVP.of(sdJwtString);
        IssuerSignedJWT jwt = sdJwt.getIssuerSignedJWT();

        JsonNode cnfNode = jwt.getPayload().get(CLAIM_NAME_CNF);
        assertNotNull(cnfNode, "The cnf claim must be present in the SD-JWT payload (decoys=" + decoys + ")");

        JsonNode jwkNode = cnfNode.get(CLAIM_NAME_JWK);
        assertNotNull(jwkNode, "The cnf claim must contain a jwk field");
        assertEquals("holder-key-1", jwkNode.get("kid").asText(),
                "The bound JWK must have the holder's key ID");
        assertEquals("EC", jwkNode.get("kty").asText(),
                "The bound JWK must have the correct key type");
    }

    public void testSignSDJwtCredential(Map<String, Object> claims, int decoys, List<String> visibleClaims)
            throws VerificationException {
        CredentialBuildConfig credentialBuildConfig = new CredentialBuildConfig()
                .setCredentialIssuer(TEST_ISSUER_DID)
                .setCredentialType("https://credentials.example.com/test-credential")
                .setTokenJwsType(VCFormat.SD_JWT_VC)
                .setHashAlgorithm(OID4VCConstants.SD_HASH_DEFAULT_ALGORITHM)
                .setNumberOfDecoys(decoys)
                .setSdJwtVisibleClaims(visibleClaims);

        VerifiableCredential testCredential = getTestCredential(claims);
        SdJwtCredentialBody sdJwtCredentialBody = new SdJwtCredentialBuilder()
                .buildCredentialBody(testCredential, credentialBuildConfig);

        String sdJwtString = sdJwtCredentialBody.sign(exampleSigner());
        SdJwtVP sdJwt = SdJwtVP.of(sdJwtString);

        IssuerSignedJWT jwt = sdJwt.getIssuerSignedJWT();

        assertEquals(TEST_ISSUER_DID,
                jwt.getPayload().get(CLAIM_NAME_ISSUER).asText(),
                "The issuer should be set in the token.");

        assertEquals(credentialBuildConfig.getCredentialType(),
                jwt.getPayload().get(CLAIM_NAME_VCT).asText(),
                "The type should be included");

        assertEquals(credentialBuildConfig.getTokenJwsType(),
                jwt.getJwsHeader().getType(),
                "The JWS token type should be included");

        ArrayNode sdArrayNode = (ArrayNode) jwt.getPayload().get(CLAIM_NAME_SD);
        if (sdArrayNode != null) {
            assertEquals(credentialBuildConfig.getHashAlgorithm().toLowerCase(),
                    jwt.getPayload().get(CLAIM_NAME_SD_HASH_ALGORITHM).asText(),
                    "The algorithm should be included and lowercase");
        }

        List<String> disclosed = sdJwt.getDisclosures().values().stream().toList();
        // The _sd array holds one digest per undisclosed claim and per decoy; disclosures
        // of array elements are anchored inside the visible arrays instead.
        long undisclosedClaims = disclosed.stream()
                .map(OID4VCTest::decodeDisclosure)
                .filter(node -> node.size() == 3)
                .count();
        assertEquals(undisclosedClaims + (decoys == 0 ? SdJwt.DEFAULT_NUMBER_OF_DECOYS : decoys),
                sdArrayNode == null ? 0 : sdArrayNode.size(),
                "All undisclosed claims and decoys should be provided.");

        visibleClaims.forEach(vc ->
                assertTrue(jwt.getPayload().has(vc),
                        "The visible claims should be present within the token.")
        );

        sdJwt.getSdJwtVerificationContext()
                .verifyIssuance(List.of(exampleVerifier()),
                        IssuerSignedJwtVerificationOpts.builder()
                                .withIatCheck(true)
                                .withNbfCheck(true)
                                .withExpCheck(true)
                                .build(),
                        null);
    }
}
