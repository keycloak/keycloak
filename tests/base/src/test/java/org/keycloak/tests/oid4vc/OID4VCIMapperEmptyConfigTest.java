package org.keycloak.tests.oid4vc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCIMapperEmptyConfigTest extends OID4VCIssuerTestBase {

    @Test
    public void testEmptyMapperConfigDoesNotCauseNPE() {
        assertMapperIsIgnored("oid4vc-subject-id-mapper", "empty-mapper");
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

    private void assertMapperIsIgnored(String mapperType, String mapperName) {
        String scopeName = mapperName + "-scope-" + UUID.randomUUID();

        ClientScopeRepresentation scope = new ClientScopeRepresentation();
        scope.setName(scopeName);
        scope.setProtocol("oid4vc");

        ProtocolMapperRepresentation emptyMapper = new ProtocolMapperRepresentation();
        emptyMapper.setName(mapperName);
        emptyMapper.setProtocol("oid4vc");
        emptyMapper.setProtocolMapper(mapperType);
        emptyMapper.setConfig(Map.of()); // Empty config

        scope.setProtocolMappers(List.of(emptyMapper));

        testRealm.admin().clientScopes().create(scope).close();

        CredentialIssuer credentialIssuer = oauth.oid4vc().doIssuerMetadataRequest().getMetadata();
        assertNotNull(credentialIssuer, "Credential Issuer metadata should be available");

        // The empty mapper should be ignored and not present in the claims
        boolean foundEmptyMapper = credentialIssuer.getCredentialsSupported().values().stream()
                .map(SupportedCredentialConfiguration::getCredentialMetadata)
                .filter(metadata -> metadata != null && metadata.getClaims() != null)
                .flatMap(metadata -> metadata.getClaims().stream())
                .anyMatch(claim -> mapperName.equals(claim.getName()) || (claim.getName() != null && claim.getName().isEmpty()));

        assertFalse(foundEmptyMapper, "Mapper " + mapperName + " of type " + mapperType + " with empty config should not be included in metadata");
    }
}
