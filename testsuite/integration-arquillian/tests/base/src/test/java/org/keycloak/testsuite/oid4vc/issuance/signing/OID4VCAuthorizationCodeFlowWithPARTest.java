/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.oid4vc.issuance.signing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailResponse;
import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OpenIDProviderConfigurationResponse;
import org.keycloak.testsuite.util.oauth.ParResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialIssuerMetadataResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static org.keycloak.OAuth2Constants.OPENID_CREDENTIAL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class for Authorization Code Flow with PAR (Pushed Authorization Request) containing authorization_details.
 * This test specifically verifies that when authorization_details is used in the PAR request,
 * it MUST be returned in the token response according to OID4VC specification.
 *
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public class OID4VCAuthorizationCodeFlowWithPARTest extends OID4VCIssuerEndpointTest {

    /**
     * Test context for OID4VC tests
     */
    protected static class Oid4vcTestContext {
        public CredentialIssuer credentialIssuer;
        public OIDCConfigurationRepresentation openidConfig;
    }

    /**
     * Get the credential client scope
     */
    protected ClientScopeRepresentation getCredentialClientScope() {
        return jwtTypeCredentialClientScope;
    }

    /**
     * Get the expected claim path for the credential format
     */
    protected String getExpectedClaimPath() {
        return "given_name";
    }

    /**
     * Prepare OID4VC test context by fetching issuer metadata
     */
    protected Oid4vcTestContext prepareOid4vcTestContext() throws Exception {
        Oid4vcTestContext ctx = new Oid4vcTestContext();

        // Get credential issuer metadata
        CredentialIssuerMetadataResponse metadataResponse = oauth.oid4vc()
                .issuerMetadataRequest()
                .endpoint(getRealmMetadataPath(TEST_REALM_NAME))
                .send();
        assertEquals(HttpStatus.SC_OK, metadataResponse.getStatusCode());
        ctx.credentialIssuer = metadataResponse.getMetadata();

        // Get OpenID configuration
        OpenIDProviderConfigurationResponse openIDProviderConfigurationResponse = oauth.wellknownRequest()
                .url(ctx.credentialIssuer.getAuthorizationServers().get(0))
                .send();
        assertEquals(HttpStatus.SC_OK, openIDProviderConfigurationResponse.getStatusCode());
        ctx.openidConfig = openIDProviderConfigurationResponse.getOidcConfiguration();

        return ctx;
    }

    @Test
    public void testAuthorizationCodeFlowWithPARAndAuthorizationDetails() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Step 1: Create PAR request with authorization_details
        String credentialConfigurationId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

        // Create authorization details with claims
        ClaimsDescription claim = new ClaimsDescription();
        List<Object> claimPath = Arrays.asList("credentialSubject", getExpectedClaimPath());
        claim.setPath(claimPath);
        claim.setMandatory(true);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credentialConfigurationId);
        authDetail.setClaims(List.of(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        // Create PAR request
        ParResponse parResponse = oauth.pushedAuthorizationRequest()
                .endpoint(ctx.openidConfig.getPushedAuthorizationRequestEndpoint())
                .client(oauth.getClientId(), "password")
                .scopeParam(getCredentialClientScope().getName())
                .authorizationDetails(authDetails)
                .state("test-state")
                .nonce("test-nonce")
                .send();
        assertEquals(HttpStatus.SC_CREATED, parResponse.getStatusCode());
        String requestUri = parResponse.getRequestUri();
        assertNotNull("Request URI should not be null", requestUri);

        // Step 2: Perform authorization with PAR
        oauth.client(client.getClientId());
        oauth.scope(getCredentialClientScope().getName());
        oauth.loginForm().requestUri(requestUri).doLogin("john", "password");

        String code = oauth.parseLoginResponse().getCode();
        assertNotNull("Authorization code should not be null", code);

        // Step 3: Exchange authorization code for tokens (WITHOUT authorization_details in token request)
        // This tests that authorization_details from PAR request is processed and returned
        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(code)
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .client(oauth.getClientId(), "password")
                .send();
        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());

        // Step 4: Verify authorization_details is present in token response
        List<OID4VCAuthorizationDetailResponse> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);
        assertEquals("Should have exactly one authorization detail", 1, authDetailsResponse.size());

        OID4VCAuthorizationDetailResponse authDetailResponse = authDetailsResponse.get(0);
        assertEquals("Type should be openid_credential", OPENID_CREDENTIAL, authDetailResponse.getType());
        assertEquals("Credential configuration ID should match", credentialConfigurationId, authDetailResponse.getCredentialConfigurationId());

        // Verify claims are preserved
        assertNotNull("Claims should be present", authDetailResponse.getClaims());
        assertEquals("Should have exactly one claim", 1, authDetailResponse.getClaims().size());
        ClaimsDescription responseClaim = authDetailResponse.getClaims().get(0);
        assertEquals("Claim path should match", claimPath, responseClaim.getPath());
        assertTrue("Claim should be mandatory", responseClaim.isMandatory());

        // Verify credential identifiers are present
        assertNotNull("Credential identifiers should be present", authDetailResponse.getCredentialIdentifiers());
        assertEquals("Should have exactly one credential identifier", 1, authDetailResponse.getCredentialIdentifiers().size());

        String credentialIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
        assertNotNull("Credential identifier should not be null", credentialIdentifier);
        assertFalse("Credential identifier should not be empty", credentialIdentifier.isEmpty());

        // Verify it's a valid UUID
        try {
            UUID.fromString(credentialIdentifier);
        } catch (IllegalArgumentException e) {
            fail("Credential identifier should be a valid UUID, but was: " + credentialIdentifier);
        }

        // Step 5: Request the actual credential using the identifier
        // When authorization_details are present in the token, credential_identifier must be used
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .bearerToken(tokenResponse.getAccessToken())
                .send();

        assertEquals(HttpStatus.SC_OK, credentialResponse.getStatusCode());

        // Parse the credential response
        CredentialResponse parsedResponse = credentialResponse.getCredentialResponse();
        assertNotNull("Credential response should not be null", parsedResponse);
        assertNotNull("Credentials should be present", parsedResponse.getCredentials());
        assertEquals("Should have exactly one credential", 1, parsedResponse.getCredentials().size());

        // Verify that the issued credential contains the requested claims
        CredentialResponse.Credential credentialWrapper = parsedResponse.getCredentials().get(0);
        assertNotNull("Credential wrapper should not be null", credentialWrapper);

        Object credentialObj = credentialWrapper.getCredential();
        assertNotNull("Credential object should not be null", credentialObj);

        // Verify the credential structure
        verifyCredentialStructure(credentialObj);
    }

    @Test
    public void testAuthorizationCodeFlowWithPARAndAuthorizationDetailsFailure() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Step 1: Create PAR request with INVALID authorization_details
        // Create authorization details with INVALID credential configuration ID
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId("INVALID_CONFIG_ID"); // This should cause failure
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        // Create PAR request
        ParResponse parResponse = oauth.pushedAuthorizationRequest()
                .endpoint(ctx.openidConfig.getPushedAuthorizationRequestEndpoint())
                .client(oauth.getClientId(), "password")
                .scopeParam(getCredentialClientScope().getName())
                .authorizationDetails(authDetails)
                .state("test-state")
                .nonce("test-nonce")
                .send();
        assertEquals(HttpStatus.SC_CREATED, parResponse.getStatusCode());
        String requestUri = parResponse.getRequestUri();
        assertNotNull("Request URI should not be null", requestUri);

        // Step 2: Perform authorization with PAR
        oauth.client(client.getClientId());
        oauth.scope(getCredentialClientScope().getName());
        oauth.loginForm().requestUri(requestUri).doLogin("john", "password");

        String code = oauth.parseLoginResponse().getCode();
        assertNotNull("Authorization code should not be null", code);

        // Step 3: Exchange authorization code for tokens (should fail because of invalid authorization_details)
        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(code)
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .client(oauth.getClientId(), "password")
                .send();

        // Should fail because authorization_details from PAR request cannot be processed
        assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusCode());
        String errorDescription = tokenResponse.getErrorDescription();
        assertTrue("Error message should indicate authorization_details processing failure",
                errorDescription != null && errorDescription.contains("authorization_details was used in authorization request but cannot be processed for token response"));
    }

    @Test
    public void testAuthorizationCodeFlowWithPARButNoAuthorizationDetailsInTokenRequest() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Step 1: Create PAR request WITHOUT authorization_details
        ParResponse parResponse = oauth.pushedAuthorizationRequest()
                .endpoint(ctx.openidConfig.getPushedAuthorizationRequestEndpoint())
                .client(oauth.getClientId(), "password")
                .scopeParam(getCredentialClientScope().getName())
                .state("test-state")
                .nonce("test-nonce")
                .send();
        assertEquals(HttpStatus.SC_CREATED, parResponse.getStatusCode());
        String requestUri = parResponse.getRequestUri();
        assertNotNull("Request URI should not be null", requestUri);

        // Step 2: Perform authorization with PAR
        oauth.client(client.getClientId());
        oauth.scope(getCredentialClientScope().getName());
        oauth.loginForm().requestUri(requestUri).doLogin("john", "password");

        String code = oauth.parseLoginResponse().getCode();
        assertNotNull("Authorization code should not be null", code);

        // Step 3: Exchange authorization code for tokens
        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(code)
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .client(oauth.getClientId(), "password")
                .send();
        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());

        // Step 4: Verify NO authorization_details in token response (since none was in PAR request)
        List<OID4VCAuthorizationDetailResponse> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        assertTrue("authorization_details should NOT be present in the response when not used in PAR request",
                authDetailsResponse == null || authDetailsResponse.isEmpty());
    }

    /**
     * Verify the credential structure based on the format.
     * Subclasses can override this to provide format-specific verification.
     */
    protected void verifyCredentialStructure(Object credentialObj) {
        // Default implementation - subclasses should override
        assertNotNull("Credential object should not be null", credentialObj);
    }
}
