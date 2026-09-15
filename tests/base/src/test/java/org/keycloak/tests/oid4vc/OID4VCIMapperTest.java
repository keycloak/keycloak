package org.keycloak.tests.oid4vc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.keycloak.VCFormat;
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
        ProtocolMapperRepresentation mapper1 = ProtocolMapperUtils.getUserAttributeMapper(CLAIM_NAME_EXP, CLAIM_NAME_EXP);
        assertReservedClaimMapperIsRejected(mapper1);

        // The user-attribute mapper interprets dotted claim names as nested paths, so "cnf.jwk" emits a top-level
        // "cnf" claim. Validation must guard the actual top-level path segment, not the literal claim name.
        ProtocolMapperRepresentation mapper2 = ProtocolMapperUtils.getUserAttributeMapper("cnf.jwk", "cnf.jwk");
        assertReservedClaimMapperIsRejected(mapper2);
    }

    @Test
    public void testSubjectIdMapperCannotMapToReservedClaim() {
        ProtocolMapperRepresentation mapper = ProtocolMapperUtils.getSubjectIdMapper(CLAIM_NAME_JTI, DidSubjectId.DID);
        assertReservedClaimMapperIsRejected(mapper);
    }

    @Test
    public void testTargetRoleMapperCannotMapToReservedClaim() {
        ProtocolMapperRepresentation mapper = ProtocolMapperUtils.getRoleMapper(client.getClientId());
        mapper.setConfig(new HashMap<>(mapper.getConfig()));
        mapper.getConfig().put(OID4VCMapper.CLAIM_NAME, CLAIM_NAME_EXP);
        assertReservedClaimMapperIsRejected(mapper);
    }

    @Test
    public void testIssuerControlledMappersMayUseReservedClaims() {
        assertReservedClaimMapperIsAccepted(ProtocolMapperUtils.getJtiGeneratedIdMapper());
        assertReservedClaimMapperIsAccepted(ProtocolMapperUtils.getIssuedAtTimeMapper(CLAIM_NAME_IAT, null, "COMPUTE"));
    }

    @Test
    public void testJwtVcScopeMayMapToReservedClaimName() {
        // For JWT VC, mapper claims live under credentialSubject rather than the SD-JWT top level, so 'exp' is a
        // legitimate data element and must not be rejected. Only SD-JWT top-level claims are protected.
        ProtocolMapperRepresentation mapper = ProtocolMapperUtils.getUserAttributeMapper(CLAIM_NAME_EXP, CLAIM_NAME_EXP);
        String scopeId = createCredentialScope("jwt-vc-reserved-claim-scope-" + UUID.randomUUID(), List.of(), VCFormat.JWT_VC);

        try (Response response = testRealm.admin().clientScopes().get(scopeId)
                .getProtocolMappers().createMapper(mapper)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(),
                    "A JWT-VC mapper targeting 'exp' must be accepted (claim lives under credentialSubject)");
        }
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
     * Creates a credential scope with an empty-config mapper, attaches it to the OID4VCI test client,
     * then asserts that the mapper's claim does <em>not</em> appear in the issuer metadata.
     */
    private void assertMapperIsIgnored(String mapperType, String mapperName) {
        ProtocolMapperRepresentation emptyMapper = ProtocolMapperUtils.getProtocolMapper(
                mapperName, mapperType, Collections.emptyMap());
        String scopeName = mapperName + "-scope-" + UUID.randomUUID();
        String scopeId = createCredentialScope(scopeName, List.of(emptyMapper));

        testRealm.admin().clients().get(client.getId()).addOptionalClientScope(scopeId);

        CredentialIssuer credentialIssuer = oauth.oid4vc().doIssuerMetadataRequest().getMetadata();
        assertNotNull(credentialIssuer, "Credential Issuer metadata must not be null");

        boolean foundEmptyMapperClaim = credentialIssuer.getCredentialsSupported().values().stream()
                .filter(cfg -> scopeName.equals(cfg.getId()))
                .map(SupportedCredentialConfiguration::getCredentialMetadata)
                .filter(meta -> meta != null && meta.getClaims() != null)
                .flatMap(meta -> meta.getClaims().stream())
                .anyMatch(claim -> claim.getPath() != null && !claim.getPath().isEmpty());

        assertFalse(foundEmptyMapperClaim,
                "Mapper '" + mapperName + "' of type '" + mapperType + "' with empty config must not produce claims in metadata");
    }

    private void assertReservedClaimMapperIsAccepted(ProtocolMapperRepresentation mapper) {
        String scopeId = createCredentialScope("accepted-claim-scope-" + UUID.randomUUID(), List.of());

        try (Response response = testRealm.admin().clientScopes().get(scopeId)
                .getProtocolMappers().createMapper(mapper)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(),
                    "Creating a mapper that targets an issuer-controlled (but not user-controlled) claim must be accepted");
        }
    }

    private void assertReservedClaimMapperIsRejected(ProtocolMapperRepresentation mapper) {
        String scopeId = createCredentialScope("reserved-claim-scope-" + UUID.randomUUID(), List.of());

        // Create the scope without the mapper, then add the mapper directly through the protocol-mappers
        // admin endpoint, which runs ProtocolMapper.validateConfig.
        try (Response response = testRealm.admin().clientScopes().get(scopeId)
                .getProtocolMappers().createMapper(mapper)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(),
                    "Creating a mapper that targets a reserved claim must be rejected");
            OAuth2ErrorRepresentation error = response.readEntity(OAuth2ErrorRepresentation.class);
            assertNotNull(error, "The rejection response must carry an error representation");
            assertTrue(error.getError() != null
                            && error.getError().contains("is reserved and must not be used by this OID4VC mapper"),
                    "Rejection should report the reserved claim, but was: " + error.getError());
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
