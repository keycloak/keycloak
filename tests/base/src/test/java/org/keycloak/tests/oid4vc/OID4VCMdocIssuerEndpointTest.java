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

import java.util.List;
import java.util.Map;

import org.keycloak.crypto.Algorithm;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCMdocTestBase.VCTestServerWithMdocEnabled.class)
public class OID4VCMdocIssuerEndpointTest extends OID4VCMdocTestBase {

    private CredentialScopeRepresentation mdocScope;

    @BeforeEach
    void setUpMdocScope() {
        ensureEcSigningKeyProvider("mdoc-endpoint-issuer-key", "P-256", "ES256", 200);
        mdocScope = createMdocCredentialScope("mdoc-endpoint-scope", "mdoc-endpoint-config");
    }

    @Test
    void testCredentialRequestByIdentifierReturnsMdocCredential() {
        OID4VCTestContext ctx = new OID4VCTestContext(client, mdocScope);
        CredentialIssuer issuerMetadata = wallet.getIssuerMetadata(ctx);
        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(ctx, issuerMetadata);

        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .authorizationDetails(authDetail)
                .send(TEST_USER, TEST_PASSWORD);
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "Authorization code should not be null");

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authCode)
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
        assertValidMdocCredential(credentialResponse.getCredentialResponse(), true);
    }

    @Test
    void testCredentialRequestRejectsUnknownMdocIdentifier() {
        OID4VCTestContext ctx = new OID4VCTestContext(client, mdocScope);
        CredentialIssuer issuerMetadata = wallet.getIssuerMetadata(ctx);
        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(ctx, issuerMetadata);

        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .authorizationDetails(authDetail)
                .send(TEST_USER, TEST_PASSWORD);
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "Authorization code should not be null");

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authCode)
                .authorizationDetails(List.of(authDetail))
                .send();
        assertEquals(200, tokenResponse.getStatusCode());

        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier("invalid-credential-identifier")
                .proofs(wallet.generateJwtProof(ctx))
                .bearerToken(tokenResponse.getAccessToken())
                .send();

        assertEquals(400, credentialResponse.getStatusCode());
        assertEquals("unknown_credential_identifier", credentialResponse.getError());
        assertTrue(credentialResponse.getErrorDescription().contains("credential_identifier"),
                credentialResponse.getErrorDescription());
    }

    @Test
    void testCredentialRequestWithoutProofSucceedsWhenMdocBindingIsOptional() {
        CredentialScopeRepresentation optionalBindingScope = createCustomMdocCredentialScope(
                "mdoc-optional-binding-endpoint-scope",
                "mdoc-optional-binding-endpoint-config",
                List.of(
                        ProtocolMapperUtils.getUserAttributeMapper("given_name", "firstName", "org.iso.18013.5.1"),
                        ProtocolMapperUtils.getSubjectIdMapper("document_number", UserModel.DID, "org.iso.18013.5.1")
                ),
                null,
                false
        );

        OID4VCTestContext ctx = new OID4VCTestContext(client, optionalBindingScope);
        CredentialIssuer issuerMetadata = wallet.getIssuerMetadata(ctx);
        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(ctx, issuerMetadata);

        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .authorizationDetails(authDetail)
                .send(TEST_USER, TEST_PASSWORD);
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "Authorization code should not be null");

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authCode)
                .authorizationDetails(List.of(authDetail))
                .send();
        assertEquals(200, tokenResponse.getStatusCode());

        String credentialIdentifier = tokenResponse.getOID4VCAuthorizationDetails().get(0).getCredentialIdentifiers().get(0);
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .bearerToken(tokenResponse.getAccessToken())
                .send();

        assertEquals(200, credentialResponse.getStatusCode());
        assertValidMdocCredential(credentialResponse.getCredentialResponse(), false);
    }

    @Test
    void testCredentialRequestWithoutProofFailsWhenMdocBindingIsRequired() {
        OID4VCTestContext ctx = new OID4VCTestContext(client, mdocScope);
        CredentialIssuer issuerMetadata = wallet.getIssuerMetadata(ctx);
        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(ctx, issuerMetadata);

        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .authorizationDetails(authDetail)
                .send(TEST_USER, TEST_PASSWORD);
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "Authorization code should not be null");

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authCode)
                .authorizationDetails(List.of(authDetail))
                .send();
        assertEquals(200, tokenResponse.getStatusCode());

        String credentialIdentifier = tokenResponse.getOID4VCAuthorizationDetails().get(0).getCredentialIdentifiers().get(0);
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .bearerToken(tokenResponse.getAccessToken())
                .send();

        assertEquals(400, credentialResponse.getStatusCode());
        assertEquals("invalid_proof", credentialResponse.getError());
    }

    @Test
    void testCredentialRequestSupportsRsaIssuerSigning() {
        CredentialScopeRepresentation rsaScope = createCustomMdocCredentialScope(
                "mdoc-rsa-endpoint-scope",
                "mdoc-rsa-endpoint-config",
                List.of(
                        ProtocolMapperUtils.getUserAttributeMapper("given_name", "firstName", "org.iso.18013.5.1"),
                        ProtocolMapperUtils.getSubjectIdMapper("document_number", UserModel.DID, "org.iso.18013.5.1")
                ),
                Algorithm.RS256,
                true
        );

        OID4VCTestContext ctx = new OID4VCTestContext(client, rsaScope);
        CredentialIssuer issuerMetadata = wallet.getIssuerMetadata(ctx);
        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(ctx, issuerMetadata);

        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .authorizationDetails(authDetail)
                .send(TEST_USER, TEST_PASSWORD);
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "Authorization code should not be null");

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authCode)
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
        assertValidMdocCredential(credentialResponse.getCredentialResponse(), true);
    }

    private OID4VCAuthorizationDetail createAuthorizationDetail(OID4VCTestContext ctx, CredentialIssuer issuerMetadata) {
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(ctx.getCredentialConfigurationId());
        authDetail.setLocations(List.of(issuerMetadata.getCredentialIssuer()));
        return authDetail;
    }

    private void assertValidMdocCredential(CredentialResponse credentialResponse, boolean expectDeviceKeyInfo) {
        assertNotNull(credentialResponse);
        assertNotNull(credentialResponse.getCredentials());
        assertEquals(1, credentialResponse.getCredentials().size());

        Object credential = credentialResponse.getCredentials().get(0).getCredential();
        assertInstanceOf(String.class, credential, "mDoc credential should be a string");

        assertDoesNotThrow(() -> {
            Map<String, Object> nameSpaces = getMdocNamespaces((String) credential);
            assertTrue(nameSpaces.containsKey("org.iso.18013.5.1"));

            Map<String, Object> mobileSecurityObject = getMdocMobileSecurityObject((String) credential);
            assertEquals(mdocTypeCredentialDocType, mobileSecurityObject.get("docType"));
            if (expectDeviceKeyInfo) {
                assertNotNull(mobileSecurityObject.get("deviceKeyInfo"));
            } else {
                assertNull(mobileSecurityObject.get("deviceKeyInfo"));
            }
        });
    }
}
