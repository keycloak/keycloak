package org.keycloak.tests.oid4vc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCIMapperEmptyConfigTest extends OID4VCIssuerTestBase {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void testEmptyMapperConfigDoesNotCauseNPE() {
        String scopeName = "empty-config-scope-" + UUID.randomUUID();
        
        ClientScopeRepresentation scope = new ClientScopeRepresentation();
        scope.setName(scopeName);
        scope.setProtocol("oid4vc");
        
        ProtocolMapperRepresentation emptyMapper = new ProtocolMapperRepresentation();
        emptyMapper.setName("empty-mapper");
        emptyMapper.setProtocol("oid4vc");
        emptyMapper.setProtocolMapper("oid4vc-subject-id-mapper");
        emptyMapper.setConfig(Map.of()); // Empty config
        
        scope.setProtocolMappers(List.of(emptyMapper));
        
        testRealm.admin().clientScopes().create(scope).close();

        runOnServer.run(session -> {
            CredentialIssuer credentialIssuer = new OID4VCIssuerWellKnownProvider(session).getIssuerMetadata();
            assertNotNull(credentialIssuer, "Credential Issuer metadata should be available");
            
            // The empty mapper should be ignored and not present in the claims
            boolean foundEmptyMapper = credentialIssuer.getCredentialsSupported().values().stream()
                    .map(config -> config.getCredentialMetadata())
                    .filter(metadata -> metadata != null && metadata.getClaims() != null)
                    .flatMap(metadata -> metadata.getClaims().stream())
                    .anyMatch(claim -> "empty-mapper".equals(claim.getName()) || claim.getName().isEmpty());
            
            assertFalse(foundEmptyMapper, "Mapper with empty config should not be included in metadata");
        });
    }

    @Test
    public void testUserAttributeMapperEmptyConfig() {
        String scopeName = "user-attr-empty-config-scope-" + UUID.randomUUID();

        ClientScopeRepresentation scope = new ClientScopeRepresentation();
        scope.setName(scopeName);
        scope.setProtocol("oid4vc");

        ProtocolMapperRepresentation emptyMapper = new ProtocolMapperRepresentation();
        emptyMapper.setName("user-attr-empty-mapper");
        emptyMapper.setProtocol("oid4vc");
        emptyMapper.setProtocolMapper("oid4vc-user-attribute-mapper");
        emptyMapper.setConfig(Map.of()); // Empty config

        scope.setProtocolMappers(List.of(emptyMapper));

        testRealm.admin().clientScopes().create(scope).close();

        runOnServer.run(session -> {
            CredentialIssuer credentialIssuer = new OID4VCIssuerWellKnownProvider(session).getIssuerMetadata();
            assertNotNull(credentialIssuer, "Credential Issuer metadata should be available");

            // The empty mapper should be ignored and not present in the claims
            boolean foundEmptyMapper = credentialIssuer.getCredentialsSupported().values().stream()
                    .map(config -> config.getCredentialMetadata())
                    .filter(metadata -> metadata != null && metadata.getClaims() != null)
                    .flatMap(metadata -> metadata.getClaims().stream())
                    .anyMatch(claim -> "user-attr-empty-mapper".equals(claim.getName()) || claim.getName().isEmpty());

            assertFalse(foundEmptyMapper, "User attribute mapper with empty config should not be included in metadata");
        });
    }

    @Test
    public void testStaticClaimMapperEmptyConfig() {
        String scopeName = "static-claim-empty-config-scope-" + UUID.randomUUID();

        ClientScopeRepresentation scope = new ClientScopeRepresentation();
        scope.setName(scopeName);
        scope.setProtocol("oid4vc");

        ProtocolMapperRepresentation emptyMapper = new ProtocolMapperRepresentation();
        emptyMapper.setName("static-claim-empty-mapper");
        emptyMapper.setProtocol("oid4vc");
        emptyMapper.setProtocolMapper("oid4vc-static-claim-mapper");
        emptyMapper.setConfig(Map.of()); // Empty config

        scope.setProtocolMappers(List.of(emptyMapper));

        testRealm.admin().clientScopes().create(scope).close();

        runOnServer.run(session -> {
            CredentialIssuer credentialIssuer = new OID4VCIssuerWellKnownProvider(session).getIssuerMetadata();
            assertNotNull(credentialIssuer, "Credential Issuer metadata should be available");

            // The empty mapper should be ignored and not present in the claims
            boolean foundEmptyMapper = credentialIssuer.getCredentialsSupported().values().stream()
                    .map(config -> config.getCredentialMetadata())
                    .filter(metadata -> metadata != null && metadata.getClaims() != null)
                    .flatMap(metadata -> metadata.getClaims().stream())
                    .anyMatch(claim -> "static-claim-empty-mapper".equals(claim.getName()) || claim.getName().isEmpty());

            assertFalse(foundEmptyMapper, "Static claim mapper with empty config should not be included in metadata");
        });
    }

    @Test
    public void testIssuedAtTimeClaimMapperEmptyConfig() {
        String scopeName = "iat-claim-empty-config-scope-" + UUID.randomUUID();

        ClientScopeRepresentation scope = new ClientScopeRepresentation();
        scope.setName(scopeName);
        scope.setProtocol("oid4vc");

        ProtocolMapperRepresentation emptyMapper = new ProtocolMapperRepresentation();
        emptyMapper.setName("iat-claim-empty-mapper");
        emptyMapper.setProtocol("oid4vc");
        emptyMapper.setProtocolMapper("oid4vc-issued-at-time-claim-mapper");
        emptyMapper.setConfig(Map.of()); // Empty config

        scope.setProtocolMappers(List.of(emptyMapper));

        testRealm.admin().clientScopes().create(scope).close();

        runOnServer.run(session -> {
            CredentialIssuer credentialIssuer = new OID4VCIssuerWellKnownProvider(session).getIssuerMetadata();
            assertNotNull(credentialIssuer, "Credential Issuer metadata should be available");

            // The empty mapper should be ignored and not present in the claims
            boolean foundEmptyMapper = credentialIssuer.getCredentialsSupported().values().stream()
                    .map(config -> config.getCredentialMetadata())
                    .filter(metadata -> metadata != null && metadata.getClaims() != null)
                    .flatMap(metadata -> metadata.getClaims().stream())
                    .anyMatch(claim -> "iat-claim-empty-mapper".equals(claim.getName()) || claim.getName().isEmpty());

            assertFalse(foundEmptyMapper, "IAT claim mapper with empty config should not be included in metadata");
        });
    }
}
