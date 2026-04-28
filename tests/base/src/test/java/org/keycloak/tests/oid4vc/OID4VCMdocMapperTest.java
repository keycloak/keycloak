/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.tests.oid4vc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCContextMapper;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCMapper;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCTypeMapper;
import org.keycloak.protocol.oid4vc.model.Claim;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;

import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCMdocTestBase.VCTestServerWithMdocEnabled.class)
public class OID4VCMdocMapperTest extends OID4VCMdocTestBase {

    @Test
    public void testUserAttributeMapperUsesNamespaceAndNestedPathForMdoc() {
        ensureEcSigningKeyProvider("mdoc-mapper-issuer-key", "P-256", "ES256", 200);

        ProtocolMapperRepresentation mapper =
                ProtocolMapperUtils.getUserAttributeMapper("address.street", "address_street_address", "org.iso.18013.5.1");
        mapper.getConfig().put(CredentialScopeModel.VC_INCLUDE_IN_METADATA, "true");
        CredentialScopeRepresentation scope = createCustomMdocCredentialScope("mdoc-mapper-scope", "mdoc-mapper-config", List.of(mapper));

        CredentialIssuer credentialIssuer = oauth.oid4vc().doIssuerMetadataRequest().getMetadata();
        SupportedCredentialConfiguration supportedConfig = credentialIssuer.getCredentialsSupported().get(scope.getCredentialConfigurationId());

        assertNotNull(supportedConfig);
        assertNotNull(supportedConfig.getCredentialMetadata());
        List<List<String>> actualPaths = supportedConfig.getCredentialMetadata().getClaims().stream()
                .map(Claim::getPath)
                .toList();
        assertTrue(actualPaths.stream().anyMatch(List.of("org.iso.18013.5.1", "address", "street")::equals),
                "Expected nested mDoc path in " + actualPaths);

        OID4VCTestContext ctx = new OID4VCTestContext(client, scope);
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(ctx.getCredentialConfigurationId());
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .authorizationDetails(authDetail)
                .send(TEST_USER, TEST_PASSWORD);
        assertNotNull(authResponse.getCode());

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authResponse.getCode())
                .authorizationDetails(List.of(authDetail))
                .send();
        assertEquals(200, tokenResponse.getStatusCode());

        String credentialIdentifier = tokenResponse.getOID4VCAuthorizationDetails().get(0).getCredentialIdentifiers().get(0);
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .proofs(wallet.generateJwtProof(ctx))
                .bearerToken(tokenResponse.getAccessToken())
                .send();
        assertEquals(200, credentialResponse.getStatusCode());

        Object credential = credentialResponse.getCredentialResponse().getCredentials().get(0).getCredential();
        String encodedIssuerSigned = assertInstanceOf(String.class, credential);
        Map<String, Object> nameSpaces = getMdocNamespaces(encodedIssuerSigned);
        Map<?, ?> namespaceClaims = assertInstanceOf(Map.class, nameSpaces.get("org.iso.18013.5.1"));
        Map<?, ?> addressClaim = assertInstanceOf(Map.class, namespaceClaims.get("address"));
        assertEquals("221B Baker Street", addressClaim.get("street"));
    }

    @Test
    public void testVcLevelMappersAreIgnoredForMdoc() {
        ensureEcSigningKeyProvider("mdoc-vc-level-mapper-issuer-key", "P-256", "ES256", 200);

        ProtocolMapperRepresentation nameMapper =
                ProtocolMapperUtils.getUserAttributeMapper("given_name", "firstName", "org.iso.18013.5.1");
        nameMapper.getConfig().put(CredentialScopeModel.VC_INCLUDE_IN_METADATA, "true");

        ProtocolMapperRepresentation issuedAtMapper =
                ProtocolMapperUtils.getIssuedAtTimeMapper("iat", null, "COMPUTE");
        issuedAtMapper.setConfig(new HashMap<>(issuedAtMapper.getConfig()));
        issuedAtMapper.getConfig().put(CredentialScopeModel.VC_INCLUDE_IN_METADATA, "true");
        issuedAtMapper.getConfig().put(OID4VCMapper.MDOC_NAMESPACE, "org.iso.18013.5.1");

        ProtocolMapperRepresentation contextMapper = ProtocolMapperUtils.getProtocolMapper(
                "context-mapper", "oid4vc-context-mapper", new HashMap<>(Map.of(
                        OID4VCContextMapper.TYPE_KEY, "https://www.w3.org/2018/credentials/v1",
                        CredentialScopeModel.VC_INCLUDE_IN_METADATA, "true",
                        OID4VCMapper.MDOC_NAMESPACE, "org.iso.18013.5.1"
                )));

        ProtocolMapperRepresentation typeMapper = ProtocolMapperUtils.getProtocolMapper(
                "type-mapper", "oid4vc-vc-type-mapper", new HashMap<>(Map.of(
                        OID4VCTypeMapper.TYPE_KEY, "VerifiableCredential",
                        CredentialScopeModel.VC_INCLUDE_IN_METADATA, "true",
                        OID4VCMapper.MDOC_NAMESPACE, "org.iso.18013.5.1"
                )));

        CredentialScopeRepresentation scope = createCustomMdocCredentialScope(
                "mdoc-vc-level-mapper-scope",
                "mdoc-vc-level-mapper-config",
                List.of(nameMapper, issuedAtMapper, contextMapper, typeMapper)
        );

        CredentialIssuer credentialIssuer = oauth.oid4vc().doIssuerMetadataRequest().getMetadata();
        SupportedCredentialConfiguration supportedConfig =
                credentialIssuer.getCredentialsSupported().get(scope.getCredentialConfigurationId());
        assertNotNull(supportedConfig.getCredentialMetadata());
        List<List<String>> claimPaths = supportedConfig.getCredentialMetadata().getClaims().stream()
                .map(Claim::getPath)
                .toList();
        assertTrue(claimPaths.contains(List.of("org.iso.18013.5.1", "given_name")));
        assertFalse(claimPaths.contains(List.of("org.iso.18013.5.1", "iat")));
        assertFalse(claimPaths.contains(List.of("context")));
        assertFalse(claimPaths.contains(List.of("type")));

        OID4VCTestContext ctx = new OID4VCTestContext(client, scope);
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(ctx.getCredentialConfigurationId());
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .authorizationDetails(authDetail)
                .send(TEST_USER, TEST_PASSWORD);
        assertNotNull(authResponse.getCode());

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authResponse.getCode())
                .authorizationDetails(List.of(authDetail))
                .send();
        assertEquals(200, tokenResponse.getStatusCode());

        String credentialIdentifier = tokenResponse.getOID4VCAuthorizationDetails().get(0).getCredentialIdentifiers().get(0);
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .proofs(wallet.generateJwtProof(ctx))
                .bearerToken(tokenResponse.getAccessToken())
                .send();
        assertEquals(200, credentialResponse.getStatusCode());

        Object credential = credentialResponse.getCredentialResponse().getCredentials().get(0).getCredential();
        String encodedIssuerSigned = assertInstanceOf(String.class, credential);
        Map<String, Object> nameSpaces = getMdocNamespaces(encodedIssuerSigned);
        assertTrue(nameSpaces.containsKey("org.iso.18013.5.1"));
        assertFalse(nameSpaces.containsKey("iat"), "VC-level iat mapper must not become an mDoc namespace");
        Map<?, ?> namespaceClaims = assertInstanceOf(Map.class, nameSpaces.get("org.iso.18013.5.1"));
        assertFalse(namespaceClaims.containsKey("iat"));
        assertFalse(namespaceClaims.containsKey("context"));
        assertFalse(namespaceClaims.containsKey("type"));
    }
}
