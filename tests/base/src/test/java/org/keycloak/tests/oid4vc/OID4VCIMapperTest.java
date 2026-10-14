package org.keycloak.tests.oid4vc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.keycloak.VCFormat;
import org.keycloak.models.UserModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCMapper;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.ssf.subject.DidSubjectId;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_EXP;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_IAT;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_JTI;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_SUB;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_SUBJECT_ID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for OID4VC protocol mappers, covering:
 * <ul>
 *   <li>Functional verification — each mapper correctly populates its claim in the issuer metadata.</li>
 *   <li>Empty-config handling — mappers with missing required config are silently ignored.</li>
 * </ul>
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCIMapperTest extends OID4VCIssuerTestBase {

    // ---- Functional tests ----

    @Test
    public void testSubjectIdMapper() {
        ProtocolMapperRepresentation mapper = ProtocolMapperUtils.getSubjectIdMapper("did", "did");
        mapper.setConfig(new HashMap<>(mapper.getConfig()));
        mapper.getConfig().put(CredentialScopeModel.VC_INCLUDE_IN_METADATA, "true");
        assertMapperIsFunctional("subject-id-mapper", mapper, "did");
    }

    @Test
    public void testUserAttributeMapper() {
        ProtocolMapperRepresentation mapper = ProtocolMapperUtils.getUserAttributeMapper("email", "email");
        mapper.setConfig(new HashMap<>(mapper.getConfig()));
        mapper.getConfig().put(CredentialScopeModel.VC_INCLUDE_IN_METADATA, "true");
        assertMapperIsFunctional("user-attr-mapper", mapper, "email");
    }

    @Test
    public void testStaticClaimMapper() {
        ProtocolMapperRepresentation mapper = ProtocolMapperUtils.getStaticClaimMapper("test-value");
        assertMapperIsFunctional("static-claim-mapper", mapper, "scope-name");
    }

    @Test
    public void testIssuedAtTimeClaimMapper() {
        ProtocolMapperRepresentation mapper = ProtocolMapperUtils.getIssuedAtTimeMapper("iat", null, "COMPUTE");
        mapper.setConfig(new HashMap<>(mapper.getConfig()));
        mapper.getConfig().put(CredentialScopeModel.VC_INCLUDE_IN_METADATA, "true");
        assertMapperIsFunctional("iat-mapper", mapper, "iat");
    }

    @Test
    public void testTargetRoleMapper() {
        String roleName = "test-role-" + UUID.randomUUID();
        RoleRepresentation role = new RoleRepresentation();
        role.setName(roleName);
        testRealm.admin().clients().get(client.getId()).roles().create(role);

        ProtocolMapperRepresentation mapper = ProtocolMapperUtils.getRoleMapper(client.getClientId());
        mapper.setConfig(new HashMap<>(mapper.getConfig()));
        mapper.getConfig().put(CredentialScopeModel.VC_INCLUDE_IN_METADATA, "true");
        assertMapperIsFunctional("role-mapper", mapper, "roles");
    }

    @Test
    public void testGeneratedIdMapper() {
        ProtocolMapperRepresentation mapper = ProtocolMapperUtils.getJtiGeneratedIdMapper();
        mapper.setConfig(new HashMap<>(mapper.getConfig()));
        mapper.getConfig().put(CredentialScopeModel.VC_INCLUDE_IN_METADATA, "true");
        assertMapperIsFunctional("generated-id-mapper", mapper, "jti");
    }

    @Test
    public void testContextMapper() {
        ProtocolMapperRepresentation mapper = ProtocolMapperUtils.getProtocolMapper(
                "context-mapper", "oid4vc-context-mapper",
                Map.of("context", "https://www.w3.org/2018/credentials/v1", CredentialScopeModel.VC_INCLUDE_IN_METADATA, "true"));
        assertMapperIsFunctional("context-mapper", mapper, "context");
    }

    @Test
    public void testTypeMapper() {
        ProtocolMapperRepresentation mapper = ProtocolMapperUtils.getProtocolMapper(
                "type-mapper", "oid4vc-vc-type-mapper",
                Map.of("vcTypeProperty", "VerifiableCredential", CredentialScopeModel.VC_INCLUDE_IN_METADATA, "true"));
        assertMapperIsFunctional("type-mapper", mapper, "type");
    }

    // ---- Empty-config tests (verifies mappers with missing required config are silently ignored) ----

    @Test
    public void testSubjectIdMapperEmptyConfig() {
        assertMapperIsIgnored("oid4vc-subject-id-mapper", "empty-subject-id-mapper");
    }

    @Test
    public void testUserAttributeMapperEmptyConfig() {
        assertMapperIsIgnored("oid4vc-user-attribute-mapper", "user-attr-empty-mapper");
    }

    @Test
    public void testStaticClaimMapperEmptyConfig() {
        assertMapperIsIgnored("oid4vc-static-claim-mapper", "static-claim-empty-mapper");
    }

    @Test
    public void testIssuedAtTimeClaimMapperEmptyConfig() {
        assertMapperIsIgnored("oid4vc-issued-at-time-claim-mapper", "iat-claim-empty-mapper");
    }

    @Test
    public void testTargetRoleMapperEmptyConfig() {
        assertMapperIsIgnored("oid4vc-target-role-mapper", "role-empty-mapper");
    }

    // ---- Protected-claim tests (mappers must not map to reserved, issuer-controlled claims) ----

    @Test
    public void testUserAttributeMapperCannotMapToReservedClaim() {
        ProtocolMapperRepresentation mapper1 = ProtocolMapperUtils.getUserAttributeMapper(CLAIM_NAME_EXP, "some-user-attribute");
        assertReservedClaimMapperIsRejected(mapper1, CLAIM_NAME_EXP);

        // The user-attribute mapper interprets dotted claim names as nested paths, so "cnf.jwk" emits a top-level
        // "cnf" claim. Validation must guard the actual top-level path segment, not the literal claim name.
        ProtocolMapperRepresentation mapper2 = ProtocolMapperUtils.getUserAttributeMapper("cnf.jwk", "some-user-attribute");
        assertReservedClaimMapperIsRejected(mapper2, "cnf.jwk");

        // The SD-JWT builder emits a top-level "id" claim as "sub" (see SdJwtCredentialBuilder), so targeting "id"
        // is an indirect write to the reserved "sub" claim and must be guarded too.
        ProtocolMapperRepresentation mapper3 = ProtocolMapperUtils.getUserAttributeMapper(CLAIM_NAME_SUBJECT_ID, "some-user-attribute");
        assertReservedClaimMapperIsRejected(mapper3, CLAIM_NAME_SUBJECT_ID);
    }

    @Test
    public void testSubjectIdMapperCannotMapToReservedClaim() {
        ProtocolMapperRepresentation mapper = ProtocolMapperUtils.getSubjectIdMapper(CLAIM_NAME_JTI, DidSubjectId.DID);
        assertReservedClaimMapperIsRejected(mapper, CLAIM_NAME_JTI);
    }

    @Test
    public void testTargetRoleMapperCannotMapToReservedClaim() {
        ProtocolMapperRepresentation mapper = ProtocolMapperUtils.getRoleMapper(client.getClientId());
        mapper.setConfig(new HashMap<>(mapper.getConfig()));
        mapper.getConfig().put(OID4VCMapper.CLAIM_NAME, CLAIM_NAME_EXP);
        assertReservedClaimMapperIsRejected(mapper, CLAIM_NAME_EXP);
    }

    @Test
    public void testIssuerControlledMappersMayUseReservedClaims() {
        assertReservedClaimMapperIsAccepted(ProtocolMapperUtils.getJtiGeneratedIdMapper());
        assertReservedClaimMapperIsAccepted(ProtocolMapperUtils.getIssuedAtTimeMapper(CLAIM_NAME_IAT, null, "COMPUTE"));
    }

    @Test
    public void testStaticClaimMapperMayMapToReservedClaim() {
        // Static claim values are admin-configured (issuer-controlled), so a static mapper may deliberately target
        // any reserved claim.
        ProtocolMapperRepresentation mapper = ProtocolMapperUtils.getStaticClaimMapper("some-value");
        mapper.setConfig(new HashMap<>(mapper.getConfig()));
        mapper.getConfig().put(OID4VCMapper.CLAIM_NAME, CLAIM_NAME_EXP);
        assertReservedClaimMapperIsAccepted(mapper);
    }

    @Test
    public void testSubjectIdMapperMayWriteSubClaim() {
        // The subject-id mapper is the trusted writer of 'sub' (via its 'id' alias), so it must be accepted for both
        // 'sub' and 'id' even though it maps user-controlled data.
        assertReservedClaimMapperIsAccepted(ProtocolMapperUtils.getSubjectIdMapper(CLAIM_NAME_SUB, UserModel.USERNAME));
        assertReservedClaimMapperIsAccepted(ProtocolMapperUtils.getSubjectIdMapper(CLAIM_NAME_SUBJECT_ID, UserModel.USERNAME));
    }

    @Test
    public void testJwtVcScopeCannotMapToReservedClaimName() {
        // For JWT VC, mapper claims live under credentialSubject, but mapping a user-controlled value to a reserved
        // claim name is still treated as a misconfiguration and must be rejected.
        ProtocolMapperRepresentation mapper = ProtocolMapperUtils.getUserAttributeMapper(CLAIM_NAME_EXP, "some-user-attribute");
        assertReservedClaimMapperIsRejected(mapper, CLAIM_NAME_EXP, VCFormat.JWT_VC);
    }

    @Test
    public void testReservedClaimMapperNotAdvertisedInMetadata() {
        // A reserved-claim mapper that slips in through the scope-create/import path (which skips validateConfig)
        // must not be advertised in issuer metadata, matching the issuance-time guard that silently drops it.
        assertMapperIsIgnored(
                "bypass-reserved-claim-scope-" + UUID.randomUUID(),
                ProtocolMapperUtils.getUserAttributeMapper(CLAIM_NAME_EXP, "some-user-attribute"),
                "A reserved-claim mapper that bypassed validateConfig must not be advertised in issuer metadata");
    }

    // ---- Helpers ----

    /**
     * Creates a proper credential scope with the given mapper, attaches it to the OID4VCI test client,
     * then asserts that the expected claim name appears in the issuer metadata.
     */
    private void assertMapperIsFunctional(String mapperName, ProtocolMapperRepresentation mapper, String expectedClaimName) {
        mapper.setName(mapperName);
        String scopeId = createCredentialScope(mapperName + "-scope-" + UUID.randomUUID(), List.of(mapper));

        // Attach scope as optional to the OID4VCI client so it appears in issuer metadata
        testRealm.admin().clients().get(client.getId()).addOptionalClientScope(scopeId);

        CredentialIssuer credentialIssuer = oauth.oid4vc().doIssuerMetadataRequest().getMetadata();
        assertNotNull(credentialIssuer, "Credential Issuer metadata must not be null");

        boolean found = credentialIssuer.getCredentialsSupported().values().stream()
                .map(SupportedCredentialConfiguration::getCredentialMetadata)
                .filter(meta -> meta != null && meta.getClaims() != null)
                .flatMap(meta -> meta.getClaims().stream())
                .filter(claim -> claim.getPath() != null)
                .map(claim -> String.join(".", claim.getPath()))
                .anyMatch(expectedClaimName::equals);

        assertTrue(found,
                "Mapper '" + mapperName + "' should expose claim '" + expectedClaimName + "' in issuer metadata");
    }

    /**
     * Creates a credential scope with an empty-config mapper of the given type, attaches it to the OID4VCI client,
     * then asserts that the mapper produces no claims in the issuer metadata.
     */
    private void assertMapperIsIgnored(String mapperType, String mapperName) {
        assertMapperIsIgnored(
                mapperName + "-scope-" + UUID.randomUUID(),
                ProtocolMapperUtils.getProtocolMapper(mapperName, mapperType, Collections.emptyMap()),
                "Mapper '" + mapperName + "' of type '" + mapperType + "' with empty config must not produce claims in metadata");
    }

    /**
     * Creates a credential scope with the given mapper, attaches it to the OID4VCI client, then asserts that the
     * mapper produces no claims in the issuer metadata.
     */
    private void assertMapperIsIgnored(String scopeName, ProtocolMapperRepresentation mapper, String failureMessage) {
        String scopeId = createCredentialScope(scopeName, List.of(mapper));

        testRealm.admin().clients().get(client.getId()).addOptionalClientScope(scopeId);

        CredentialIssuer credentialIssuer = oauth.oid4vc().doIssuerMetadataRequest().getMetadata();
        assertNotNull(credentialIssuer, "Credential Issuer metadata must not be null");

        boolean foundClaim = credentialIssuer.getCredentialsSupported().values().stream()
                .filter(cfg -> scopeName.equals(cfg.getId()))
                .map(SupportedCredentialConfiguration::getCredentialMetadata)
                .filter(meta -> meta != null && meta.getClaims() != null)
                .flatMap(meta -> meta.getClaims().stream())
                .anyMatch(claim -> claim.getPath() != null && !claim.getPath().isEmpty());

        assertFalse(foundClaim, failureMessage);
    }

    private void assertReservedClaimMapperIsAccepted(ProtocolMapperRepresentation mapper) {
        String scopeId = createCredentialScope("accepted-claim-scope-" + UUID.randomUUID(), List.of());

        try (Response response = testRealm.admin().clientScopes().get(scopeId)
                .getProtocolMappers().createMapper(mapper)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(),
                    "Creating a mapper that targets an issuer-controlled (but not user-controlled) claim must be accepted");
        }
    }

    private void assertReservedClaimMapperIsRejected(ProtocolMapperRepresentation mapper, String expectedClaimName) {
        assertReservedClaimMapperIsRejected(mapper, expectedClaimName, VCFormat.SD_JWT_VC);
    }

    private void assertReservedClaimMapperIsRejected(ProtocolMapperRepresentation mapper, String expectedClaimName, String format) {
        String scopeId = createCredentialScope("reserved-claim-scope-" + UUID.randomUUID(), List.of(), format);

        // Create the scope without the mapper, then add the mapper directly through the protocol-mappers
        // admin endpoint, which runs ProtocolMapper.validateConfig.
        try (Response response = testRealm.admin().clientScopes().get(scopeId)
                .getProtocolMappers().createMapper(mapper)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(),
                    "Creating a mapper that targets a reserved claim must be rejected");
            OAuth2ErrorRepresentation error = response.readEntity(OAuth2ErrorRepresentation.class);
            assertNotNull(error, "The rejection response must carry an error representation");
            assertNotNull(error.getError(), "The rejection must carry an error code");
            assertTrue(error.getErrorDescription() != null
                            && error.getErrorDescription().contains(
                                    String.format("Claim name %s is reserved and must not be used by this OID4VC mapper.", expectedClaimName)),
                    "Rejection should report the reserved claim, but was: " + error.getErrorDescription());
        }
    }

    /**
     * Creates a credential scope with the given protocol mappers, registers its cleanup, and returns its
     * server-assigned id. The scope uses the default (SD-JWT) format.
     */
    private String createCredentialScope(String scopeName, List<ProtocolMapperRepresentation> mappers) {
        return createCredentialScope(scopeName, mappers, VCFormat.SD_JWT_VC);
    }

    /**
     * Creates a credential scope of the given format with the given protocol mappers, registers its cleanup, and
     * returns its server-assigned id.
     */
    private String createCredentialScope(String scopeName, List<ProtocolMapperRepresentation> mappers, String format) {
        CredentialScopeRepresentation scope = new CredentialScopeRepresentation(scopeName)
                .setIncludeInTokenScope(true)
                .setCredentialConfigurationId(scopeName + "-config-id")
                .setCredentialIdentifier(scopeName);

        if (format != null) {
            scope.setFormat(format);
        }

        scope.setProtocolMappers(mappers);

        String scopeId;
        try (Response response = testRealm.admin().clientScopes().create(scope)) {
            scopeId = ApiUtil.getCreatedId(response);
        }
        // Scope deletion also removes the optional-scope attachment from the client
        testRealm.cleanup().add(r -> r.clientScopes().get(scopeId).remove());
        return scopeId;
    }
}
