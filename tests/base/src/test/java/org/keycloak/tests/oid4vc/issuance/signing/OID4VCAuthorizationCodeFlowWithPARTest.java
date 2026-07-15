package org.keycloak.tests.oid4vc.issuance.signing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.RegisterPage;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCProofTestUtils;
import org.keycloak.tests.oid4vc.issuance.OID4VCAuthorizationCodeFlowTestBase;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.ParResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for Authorization Code Flow with PAR (Pushed Authorization Request) containing authorization_details.
 * This test specifically verifies that when authorization_details is used in the PAR request,
 * it MUST be returned in the token response according to OID4VC specification.
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCAuthorizationCodeFlowWithPARTest extends OID4VCAuthorizationCodeFlowTestBase {

    @Override
    protected String getCredentialFormat() {
        return "jwt_vc";
    }

    @Override
    protected CredentialScopeRepresentation getCredentialScope() {
        return jwtTypeCredentialScope;
    }

    @Override
    protected String getExpectedClaimPath() {
        return "family_name";
    }

    @Override
    protected String getFirstNameProtocolMapperName() {
        return "givenName";
    }

    @InjectPage
    RegisterPage registerPage;
    @Override
    protected void verifyCredentialStructure(Object credentialObj) {
        assertNotNull(credentialObj, "JWT credential object should not be null");
        assertInstanceOf(String.class, credentialObj, "JWT credential should be a string");
        String jwtString = (String) credentialObj;
        assertFalse(jwtString.isEmpty(), "JWT credential should not be empty");
        assertTrue(jwtString.contains("."), "JWT credential should contain dots (header.payload.signature)");
    }

    private Proofs newJwtProofs() {
        String cNonce = oauth.oid4vc().nonceRequest().send().getNonce();
        String issuer = oauth.oid4vc().issuerMetadataRequest().send().getMetadata().getCredentialIssuer();
        return OID4VCProofTestUtils.jwtProofs(issuer, cNonce);
    }

    @Test
    public void testAuthorizationCodeFlowWithPARAndAuthorizationDetails() throws Exception {
        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        OIDCConfigurationRepresentation openidConfig = wallet.getAuthorizationServerMetadata(ctx);

        // Step 1: Create PAR request with authorization_details
        String credentialConfigurationId = ctx.getCredentialConfigurationId();

        // Create authorization details with claims
        ClaimsDescription claim = new ClaimsDescription();
        List<Object> claimPath = Arrays.asList("credentialSubject", getExpectedClaimPath());
        claim.setPath(claimPath);
        claim.setMandatory(true);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credentialConfigurationId);
        authDetail.setClaims(List.of(claim));
        authDetail.setLocations(Collections.singletonList(issuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        // Create PAR request
        ParResponse parResponse = oauth.pushedAuthorizationRequest()
                .endpoint(openidConfig.getPushedAuthorizationRequestEndpoint())
                .client(client.getClientId(), client.getSecret())
                .scopeParam(ctx.getScope())
                .authorizationDetails(authDetails)
                .state("test-state")
                .nonce("test-nonce")
                .send();
        assertEquals(HttpStatus.SC_CREATED, parResponse.getStatusCode());
        String requestUri = parResponse.getRequestUri();
        assertNotNull(requestUri, "Request URI should not be null");

        // Step 2: Perform authorization with PAR
        oauth.client(client.getClientId(), client.getSecret());
        oauth.scope(ctx.getScope());
        oauth.loginForm().requestUri(requestUri).doLogin(TEST_USER, TEST_PASSWORD);

        String code = oauth.parseLoginResponse().getCode();
        assertNotNull(code, "Authorization code should not be null");

        // Step 3: Exchange authorization code for tokens (WITHOUT authorization_details in token request)
        // This tests that authorization_details from PAR request is processed and returned
        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(code)
                .endpoint(openidConfig.getTokenEndpoint())
                .client(client.getClientId(), client.getSecret())
                .send();
        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());

        // Step 4: Verify authorization_details is present in token response
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        assertNotNull(authDetailsResponse, "authorization_details should be present in the response");
        assertEquals(1, authDetailsResponse.size(), "Should have exactly one authorization detail");

        OID4VCAuthorizationDetail authDetailResponse = authDetailsResponse.get(0);
        assertEquals(OPENID_CREDENTIAL, authDetailResponse.getType(), "Type should be openid_credential");
        assertEquals(credentialConfigurationId, authDetailResponse.getCredentialConfigurationId(), "Credential configuration ID should match");

        // Verify claims are preserved
        assertNotNull(authDetailResponse.getClaims(), "Claims should be present");
        assertEquals(1, authDetailResponse.getClaims().size(), "Should have exactly one claim");
        ClaimsDescription responseClaim = authDetailResponse.getClaims().get(0);
        assertEquals(claimPath, responseClaim.getPath(), "Claim path should match");
        assertTrue(responseClaim.isMandatory(), "Claim should be mandatory");

        // Verify credential identifiers are present
        assertNotNull(authDetailResponse.getCredentialIdentifiers(), "Credential identifiers should be present");
        assertEquals(1, authDetailResponse.getCredentialIdentifiers().size(), "Should have exactly one credential identifier");

        String credentialIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
        assertNotNull(credentialIdentifier, "Credential identifier should not be null");
        assertFalse(credentialIdentifier.isEmpty(), "Credential identifier should not be empty");

        // Step 5: Request the actual credential using the identifier
        // When authorization_details are present in the token, credential_identifier must be used
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .proofs(newJwtProofs())
                .bearerToken(tokenResponse.getAccessToken())
                .send();

        assertSuccessfulCredentialResponse(credentialResponse);
    }

    @Test
    public void testAuthorizationCodeFlowWithPARAndAuthorizationDetailsFailure() throws Exception {
        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        OIDCConfigurationRepresentation openidConfig = wallet.getAuthorizationServerMetadata(ctx);

        // Step 1: Create PAR request with INVALID authorization_details
        // Create authorization details with INVALID credential configuration ID
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId("INVALID_CONFIG_ID"); // This should cause failure
        authDetail.setLocations(Collections.singletonList(issuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        // Create PAR request
        ParResponse parResponse = oauth.pushedAuthorizationRequest()
                .endpoint(openidConfig.getPushedAuthorizationRequestEndpoint())
                .client(client.getClientId(), client.getSecret())
                .scopeParam(ctx.getScope())
                .authorizationDetails(authDetails)
                .state("test-state")
                .nonce("test-nonce")
                .send();
        assertEquals(HttpStatus.SC_CREATED, parResponse.getStatusCode());
        String requestUri = parResponse.getRequestUri();
        assertNotNull(requestUri, "Request URI should not be null");

        // Step 2: Perform authorization with PAR
        oauth.client(client.getClientId(), client.getSecret());
        oauth.scope(ctx.getScope());
        oauth.loginForm().requestUri(requestUri).open();
        AuthorizationEndpointResponse authResponse = oauth.parseLoginResponse();

        // Should fail because authorization_details from PAR request cannot be processed
        String errorDescription = authResponse.getErrorDescription();
        assertTrue(errorDescription != null && errorDescription.contains("Invalid authorization_details"), "Error message should indicate authorization_details processing failure");
    }

    @Test
    public void testAuthorizationCodeFlowWithPARPromptCreateOpensTheRegistrationPage() throws Exception {
        testRealm.updateWithCleanup(r -> r.registrationAllowed(true));
        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        OIDCConfigurationRepresentation openidConfig = wallet.getAuthorizationServerMetadata(ctx);

        // Step 1: Create PAR request with authorization_details
        String credentialConfigurationId = ctx.getCredentialConfigurationId();

        // Create authorization details with claims
        ClaimsDescription claim = new ClaimsDescription();
        List<Object> claimPath = Arrays.asList("credentialSubject", getExpectedClaimPath());
        claim.setPath(claimPath);
        claim.setMandatory(true);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credentialConfigurationId);
        authDetail.setClaims(List.of(claim));
        authDetail.setLocations(Collections.singletonList(issuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        // Create PAR request
        ParResponse parResponse = oauth.pushedAuthorizationRequest()
                .endpoint(openidConfig.getPushedAuthorizationRequestEndpoint())
                .client(client.getClientId(), client.getSecret())
                .scopeParam(ctx.getScope())
                .authorizationDetails(authDetails)
                .state("test-state")
                .nonce("test-nonce")
                .prompt("create")
                .send();
        assertEquals(HttpStatus.SC_CREATED, parResponse.getStatusCode());
        String requestUri = parResponse.getRequestUri();
        assertNotNull(requestUri, "Request URI should not be null");

        // Step 2: Perform authorization with PAR
        oauth.client(client.getClientId(), client.getSecret());
        oauth.scope(ctx.getScope());
        oauth.loginForm().requestUri(requestUri).open();

        registerPage.assertCurrent();
        registerPage.register("Vilmos", "Szabó-Nagy", "vilmos@email", "vnagy", "AppleTree123");
        testRealm.cleanup().add(r -> r.users().search("vnagy").stream().findFirst().ifPresent(u -> r.users().delete(u.getId())));
        AuthorizationEndpointResponse authResponse = oauth.parseLoginResponse();
        assertTrue(authResponse.isSuccess());
    }

    @Test
    public void testAuthorizationCodeFlowWithPARButNoAuthorizationDetailsInTokenRequest() throws Exception {
        wallet.getIssuerMetadata(ctx);
        OIDCConfigurationRepresentation openidConfig = wallet.getAuthorizationServerMetadata(ctx);

        // Step 1: Create PAR request WITHOUT authorization_details
        ParResponse parResponse = oauth.pushedAuthorizationRequest()
                .endpoint(openidConfig.getPushedAuthorizationRequestEndpoint())
                .client(client.getClientId(), client.getSecret())
                .scopeParam(ctx.getScope())
                .state("test-state")
                .nonce("test-nonce")
                .send();
        assertEquals(HttpStatus.SC_CREATED, parResponse.getStatusCode());
        String requestUri = parResponse.getRequestUri();
        assertNotNull(requestUri, "Request URI should not be null");

        // Step 2: Perform authorization with PAR
        oauth.client(client.getClientId(), client.getSecret());
        oauth.scope(ctx.getScope());
        oauth.loginForm().requestUri(requestUri).doLogin(TEST_USER, TEST_PASSWORD);

        String code = oauth.parseLoginResponse().getCode();
        assertNotNull(code, "Authorization code should not be null");

        // Step 3: Exchange authorization code for tokens
        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(code)
                .endpoint(openidConfig.getTokenEndpoint())
                .client(client.getClientId(), client.getSecret())
                .send();
        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());

        // Step 4: Verify authorization_details are derived from requested OID4VC scope
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        assertNotNull(authDetailsResponse, "authorization_details should be present in the token response");
        assertFalse(authDetailsResponse.isEmpty(), "authorization_details should not be empty");
        OID4VCAuthorizationDetail firstAuthorizationDetail = authDetailsResponse.get(0);
        assertEquals(ctx.getCredentialConfigurationId(),
                firstAuthorizationDetail.getCredentialConfigurationId(),
                "credential_configuration_id should match requested scope");
        assertNotNull(firstAuthorizationDetail.getCredentialIdentifiers(), "credential_identifiers should be present");
        assertFalse(firstAuthorizationDetail.getCredentialIdentifiers().isEmpty(), "credential_identifiers should not be empty");
    }
}
