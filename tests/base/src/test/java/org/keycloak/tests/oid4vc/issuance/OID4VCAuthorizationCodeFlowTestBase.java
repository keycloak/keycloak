package org.keycloak.tests.oid4vc.issuance;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.oid4vci.Oid4vcProtocolMapperModel;
import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCProofTestUtils;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.InvalidTokenRequest;
import org.keycloak.testsuite.util.oauth.oid4vc.InvalidCredentialRequest;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base class for OID4VCI Authorization Code Flow integration tests.
 *
 * <p>This base class provides a common test structure for verifying credential issuance
 * using the authorization code flow with {@code authorization_details}. It handles
 * login orchestration, token exchange, and robust session cleanup.
 *
 * <p>Test classes for specific formats (e.g., {@code jwt_vc}, {@code sd_jwt_vc}) should
 * extend this class and implement the abstract methods to define format-specific expectations.
 */
public abstract class OID4VCAuthorizationCodeFlowTestBase extends OID4VCIssuerTestBase {

    private static final Logger logger = Logger.getLogger(OID4VCAuthorizationCodeFlowTestBase.class);

    protected OID4VCTestContext ctx;

    /** Returns the credential format supported by this test (e.g. {@code "jwt_vc"} or {@code "sd_jwt_vc"}). */
    protected abstract String getCredentialFormat();

    /** Returns the credential scope configured for this test's format. */
    protected abstract CredentialScopeRepresentation getCredentialScope();

    /**
     * Returns the claim name (last segment of the path) used when building mandatory-claim
     * authorization details for this format.
     */
    protected abstract String getExpectedClaimPath();

    /** Returns the name of the firstName protocol mapper in the credential scope. */
    protected abstract String getFirstNameProtocolMapperName();

    @BeforeEach
    void setUp() {
        ctx = new OID4VCTestContext(client, getCredentialScope());
        // Clean up before starting
        cleanupState();
    }

    @AfterEach
    void tearDown() {
        cleanupState();
    }

    private void cleanupState() {
        // 1. Server-side session cleanup
        try {
            wallet.logout(TEST_USER);
        } catch (Exception e) {
            logger.warn("Failed to logout user during cleanup", e);
        }

        // 2. User profile restoration
        try {
            resetTestUser();
        } catch (Exception e) {
            logger.warn("Failed to reset test user", e);
        }

        // 3. Consent cleanup
        try {
            clearUserConsents();
        } catch (Exception e) {
            logger.warn("Failed to clear user consents", e);
        }

        // 4. Event cleanup
        try {
            events.clear();
        } catch (Exception e) {
            logger.warn("Failed to clear events", e);
        }

        // 5. Browser-side cookie cleanup (VERY IMPORTANT to prevent NoSuchElementException: username)
        try {
            if (driver.driver() != null) {
                driver.driver().manage().deleteAllCookies();
                driver.driver().navigate().to("about:blank");
            }
        } catch (Exception e) {
            logger.warn("Failed to cleanup browser state", e);
        }
    }

    private void resetTestUser() {
        testRealm.admin().users().search(TEST_USER).stream()
            .findFirst()
            .ifPresent(u -> {
                UserResource userResource = testRealm.admin().users().get(u.getId());
                UserRepresentation rep = userResource.toRepresentation();
                rep.setFirstName("John");
                rep.setLastName("Doe");
                // Important: Clear all required actions to prevent login blocking
                rep.setRequiredActions(Collections.emptyList());
                // Clear all attributes except 'did' which is needed for OID4VCI
                Map<String, List<String>> attrs = rep.getAttributes();
                if (attrs != null) {
                    List<String> did = attrs.get("did");
                    attrs.clear();
                    if (did != null) attrs.put("did", did);
                }
                userResource.update(rep);
            });
    }

    private void clearUserConsents() {
        testRealm.admin().users().search(TEST_USER).stream()
            .findFirst()
            .ifPresent(u -> {
                UserResource userResource = testRealm.admin().users().get(u.getId());
                userResource.getConsents().forEach(c -> userResource.revokeConsent((String) c.get("clientId")));
            });
    }

    /**
     * A second regular SSO login must NOT carry over authorization_details from a previous OID4VCI login.
     */
    @Test
    public void testSecondSSOLoginDoesNotReturnAuthorizationDetails() throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);

        // Step 1: First login with OID4VCI (should return authorization_details)
        AccessTokenResponse firstTokenResponse = authzCodeFlow(issuer);
        String credentialIdentifier = assertTokenResponse(firstTokenResponse);
        assertNotNull(credentialIdentifier, "Credential identifier should be present in first token");

        // Step 2: Logout and clear cookies to isolate sessions strictly
        wallet.logout();
        if (driver.driver() != null) {
            driver.driver().manage().deleteAllCookies();
        }
        clearUserConsents();

        // Step 3: Second login - Regular SSO (should NOT return authorization_details)
        AuthorizationEndpointResponse secondAuthResponse = wallet.authorizationRequest()
                .scope(OAuth2Constants.SCOPE_OPENID)
                .send(TEST_USER, TEST_PASSWORD);

        String secondCode = secondAuthResponse.getCode();
        assertNotNull(secondCode, "Second authorization code should not be null");

        AccessTokenResponse secondTokenResponse = oauth.accessTokenRequest(secondCode).send();
        assertEquals(200, secondTokenResponse.getStatusCode(),
                "Second token exchange should succeed");

        // Step 4: Second token must NOT have authorization_details
        assertNull(secondTokenResponse.getAuthorizationDetails(),
                "Second token (regular SSO) should NOT have authorization_details. Details found: " + secondTokenResponse.getAuthorizationDetails());

        // Step 5: Credential request with second token must fail
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .proofs(newJwtProofs())
                .bearerToken(secondTokenResponse.getAccessToken())
                .send();

        assertEquals(400, credentialResponse.getStatusCode(),
                "Credential request with token without OID4VCI scope should fail");
        assertEquals(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION.getValue(), credentialResponse.getError());
        assertEquals("Invalid AccessToken for credential request. No authorization_details", credentialResponse.getErrorDescription());
    }

    /**
     * Using credential_configuration_id (instead of credential_identifier) must fail when
     * authorization_details are present in the token.
     */
    @Test
    public void testCompleteFlowWithClaimsValidationAuthorizationCode_credentialRequestWithConfigurationId()
            throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        AccessTokenResponse tokenResponse = authzCodeFlow(issuer);

        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setCredentialConfigurationId(ctx.getCredentialConfigurationId());
        credentialRequest.setProofs(newJwtProofs());

        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc()
                .credentialRequest(credentialRequest)
                .bearerToken(tokenResponse.getAccessToken())
                .send();

        assertEquals(400, credentialResponse.getStatusCode(),
                "Using credential_configuration_id with token that has authorization_details should fail");
        assertTrue(
                credentialResponse.getErrorDescription() != null &&
                (credentialResponse.getErrorDescription().contains("credential_identifier") ||
                 credentialResponse.getErrorDescription().contains("authorization_details")),
                "Error should indicate that credential_identifier must be used. Actual: "
                        + credentialResponse.getErrorDescription());
    }

    /**
     * Verifies that a credential request using a {@code credential_identifier} succeeds.
     */
    @Test
    public void testCompleteFlowWithClaimsValidationAuthorizationCode_credentialRequestWithCredentialIdentifier()
            throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        AccessTokenResponse tokenResponse = authzCodeFlow(issuer);
        String credentialIdentifier = assertTokenResponse(tokenResponse);

        Oid4vcCredentialResponse credResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .proofs(newJwtProofs())
                .bearerToken(tokenResponse.getAccessToken())
                .send();
        assertSuccessfulCredentialResponse(credResponse);
    }

    /** After refreshing the token the new access-token must still be usable for a credential request. */
    @Test
    public void testCompleteFlowWithClaimsValidationAuthorizationCode_refreshToken() throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        AccessTokenResponse tokenResponse = authzCodeFlow(issuer);
        AccessTokenResponse refreshed = oauth.refreshRequest(tokenResponse.getRefreshToken()).send();

        assertEquals(200, refreshed.getStatusCode(), "Refresh token exchange should succeed");

        String credentialIdentifier = null;
        List<OID4VCAuthorizationDetail> authDetails = refreshed.getOID4VCAuthorizationDetails();
        if (authDetails != null && !authDetails.isEmpty()) {
            List<String> ids = authDetails.get(0).getCredentialIdentifiers();
            if (ids != null && !ids.isEmpty()) {
                credentialIdentifier = ids.get(0);
            }
        }
        assertNotNull(credentialIdentifier, "Refreshed token should have credential identifier");

        Oid4vcCredentialResponse credResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .proofs(newJwtProofs())
                .bearerToken(refreshed.getAccessToken())
                .send();
        assertSuccessfulCredentialResponse(credResponse);
    }

    /**
     * Verifies that mandatory claims requested via {@code authorization_details} are enforced.
     *
     * <p>The test ensures that the request fails if a mandatory claim is missing from the user profile
     * and succeeds once the profile is updated.
     */
    @Test
    public void testCompleteFlow_mandatoryClaimsInAuthzDetailsParameter() throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);

        // Login while profile is complete (avoids VERIFY_PROFILE)
        AccessTokenResponse tokenResponse = authzCodeFlow(issuer);
        String credentialIdentifier = assertTokenResponse(tokenResponse);

        UserState userState = storeUserState();
        try {
            // Now remove lastName — credential request should fail
            userState.userRep.setLastName(null);
            userState.user.update(userState.userRep);

            Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                    .credentialIdentifier(credentialIdentifier)
                    .proofs(newJwtProofs())
                    .bearerToken(tokenResponse.getAccessToken())
                    .send();
            assertErrorCredentialResponse(credentialResponse);

            // Restore lastName and verify success
            userState.userRep.setLastName("Doe");
            userState.user.update(userState.userRep);

            credentialResponse = oauth.oid4vc().credentialRequest()
                    .credentialIdentifier(credentialIdentifier)
                    .proofs(newJwtProofs())
                    .bearerToken(tokenResponse.getAccessToken())
                    .send();
            assertSuccessfulCredentialResponse(credentialResponse);
        } finally {
            restoreUserState(userState);
        }
    }

    /**
     * Keycloak must use the authorization_details from the access-token that was actually supplied
     * when processing mandatory claim requirements.
     *
     * <p>WE DO NOT logout between flows to keep BOTH tokens valid. We use SSO for the second flow.
     */
    @Test
    public void testCorrectAccessTokenUsed() throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);

        // Flow 1: Token WITHOUT mandatory lastName requirement
        AccessTokenResponse tokenWithoutMandatory = authzCodeFlow(issuer, Collections.emptyList());

        // Flow 2: Token WITH mandatory lastName requirement
        // We USE SSO here instead of wallet.logout() because logout would invalidate tokenWithoutMandatory's session.
        OID4VCAuthorizationDetail authDetailWithMandatory = new OID4VCAuthorizationDetail();
        authDetailWithMandatory.setType(OPENID_CREDENTIAL);
        authDetailWithMandatory.setCredentialConfigurationId(ctx.getCredentialConfigurationId());
        authDetailWithMandatory.setClaims(mandatoryLastNameClaims());
        authDetailWithMandatory.setLocations(Collections.singletonList(issuer.getCredentialIssuer()));

        // Manually navigate to the authorization URL (exploiting SSO session from Flow 1)
        wallet.authorizationRequest()
                .scope(ctx.getScope())
                .authorizationDetails(authDetailWithMandatory)
                .openLoginForm();
        
        AuthorizationEndpointResponse secondAuthResponse = oauth.parseLoginResponse();
        String secondCode = secondAuthResponse.getCode();
        assertNotNull(secondCode, "Second authorization code should not be null (SSO)");

        AccessTokenResponse tokenWithMandatory = oauth.accessTokenRequest(secondCode)
                .authorizationDetails(List.of(authDetailWithMandatory))
                .send();
        assertEquals(200, tokenWithMandatory.getStatusCode(), "Second token exchange (SSO) should succeed");

        UserState userState = storeUserState();
        try {
            // Now remove lastName — credential requests will test against current profile
            userState.userRep.setLastName(null);
            userState.user.update(userState.userRep);

            // 1. Request with mandatory lastName token → must fail (user has no lastName)
            String credIdWithMandatory = assertTokenResponse(tokenWithMandatory);
            Oid4vcCredentialResponse respWithMandatory = oauth.oid4vc().credentialRequest()
                    .credentialIdentifier(credIdWithMandatory)
                    .proofs(newJwtProofs())
                    .bearerToken(tokenWithMandatory.getAccessToken())
                    .send();
            assertErrorCredentialResponse(respWithMandatory);

            // 2. Request with optional lastName token → must succeed (even though user has no lastName)
            String credIdWithoutMandatory = assertTokenResponse(tokenWithoutMandatory);
            Oid4vcCredentialResponse respWithoutMandatory = oauth.oid4vc().credentialRequest()
                    .credentialIdentifier(credIdWithoutMandatory)
                    .proofs(newJwtProofs())
                    .bearerToken(tokenWithoutMandatory.getAccessToken())
                    .send();
            assertSuccessfulCredentialResponse(respWithoutMandatory);
        } finally {
            restoreUserState(userState);
        }
    }

    /**
     * Credential request must fail when BOTH the authorization_details parameter AND a protocol mapper
     * mark claims as mandatory and those claims are missing from the user profile.
     */
    @Test
    public void testCompleteFlow_mandatoryClaimsInAuthzDetailsParameterAndProtocolMappersConfig() throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);

        // Make firstName mapper mandatory
        ClientScopeResource clientScopeResource = testRealm.admin().clientScopes().findAll().stream()
                .filter(s -> getCredentialScope().getName().equals(s.getName()))
                .map(s -> testRealm.admin().clientScopes().get(s.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Credential scope not found: " + getCredentialScope().getName()));

        ProtocolMapperRepresentation protocolMapper = clientScopeResource.getProtocolMappers().getMappers()
                .stream()
                .filter(pm -> getFirstNameProtocolMapperName().equals(pm.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Protocol mapper not found: " + getFirstNameProtocolMapperName()));

        String originalMandatoryValue = protocolMapper.getConfig().get(Oid4vcProtocolMapperModel.MANDATORY);
        protocolMapper.getConfig().put(Oid4vcProtocolMapperModel.MANDATORY, "true");
        clientScopeResource.getProtocolMappers().update(protocolMapper.getId(), protocolMapper);

        // Clear events and obtain token while profile is complete
        events.clear();
        AccessTokenResponse tokenResponse = authzCodeFlow(issuer);
        String credentialIdentifier = assertTokenResponse(tokenResponse);

        UserState userState = storeUserState();
        try {
            // Now remove lastName — credential request should fail (mandatory via auth details)
            userState.userRep.setLastName(null);
            userState.user.update(userState.userRep);

            Oid4vcCredentialResponse resp = oauth.oid4vc().credentialRequest()
                    .credentialIdentifier(credentialIdentifier)
                    .proofs(newJwtProofs())
                    .bearerToken(tokenResponse.getAccessToken())
                    .send();
            assertErrorCredentialResponse(resp);

            // Add lastName, remove firstName → still error (firstName mandatory via mapper)
            userState.userRep.setLastName("Doe");
            userState.userRep.setFirstName(null);
            userState.user.update(userState.userRep);

            resp = oauth.oid4vc().credentialRequest()
                    .credentialIdentifier(credentialIdentifier)
                    .proofs(newJwtProofs())
                    .bearerToken(tokenResponse.getAccessToken())
                    .send();
            assertErrorCredentialResponse(resp);

            // Both present → success
            userState.userRep.setLastName("Doe");
            userState.userRep.setFirstName("John");
            userState.user.update(userState.userRep);

            resp = oauth.oid4vc().credentialRequest()
                    .credentialIdentifier(credentialIdentifier)
                    .proofs(newJwtProofs())
                    .bearerToken(tokenResponse.getAccessToken())
                    .send();
            assertSuccessfulCredentialResponse(resp);
        } finally {
            restoreUserState(userState);
            protocolMapper.getConfig().put(Oid4vcProtocolMapperModel.MANDATORY,
                    originalMandatoryValue != null ? originalMandatoryValue : "false");
            clientScopeResource.getProtocolMappers().update(protocolMapper.getId(), protocolMapper);
        }
    }

    /** Reusing an authorization code must fail with an {@code invalid_grant} error. */
    @Test
    public void testAuthorizationCodeReuse() throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);

        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(issuer);
        String code = performAuthorizationCodeLoginWithAuthorizationDetails(authDetail);

        // First exchange — must succeed
        AccessTokenResponse firstResponse = oauth.accessTokenRequest(code).send();
        assertEquals(200, firstResponse.getStatusCode());

        // Drain any events from the successful flow before the error assertion
        events.clear();

        // Second exchange with same code — must fail
        AccessTokenResponse errorResponse = oauth.accessTokenRequest(code).send();

        assertTrue(errorResponse.getStatusCode() == 400 || errorResponse.getStatusCode() == 401,
                "Expected 400 or 401 but got " + errorResponse.getStatusCode());
        assertTrue(
                "invalid_grant".equals(errorResponse.getError()) ||
                (errorResponse.getErrorDescription() != null &&
                 errorResponse.getErrorDescription().contains("Code not valid")),
                "Error response should indicate invalid_grant. Got error: " + errorResponse.getError());

        EventAssertion.assertError(events.poll())
                .type(EventType.CODE_TO_TOKEN_ERROR)
                .error(Errors.INVALID_CODE);
    }

    /** An invalid/malformed authorization code must be rejected. */
    @Test
    public void testInvalidAuthorizationCode() throws Exception {

        // Drain any prior events
        events.clear();

        AccessTokenResponse errorResponse = oauth.accessTokenRequest("invalid-code-12345").send();

        assertTrue(errorResponse.getStatusCode() == 400 || errorResponse.getStatusCode() == 401,
                "Expected 400 or 401 but got " + errorResponse.getStatusCode());
        assertTrue(
                "invalid_grant".equals(errorResponse.getError()) ||
                (errorResponse.getErrorDescription() != null &&
                 errorResponse.getErrorDescription().contains("Code not valid")),
                "Error response should indicate invalid_grant. Got error: " + errorResponse.getError());

        EventAssertion.assertError(events.poll())
                .type(EventType.CODE_TO_TOKEN_ERROR)
                .error(Errors.INVALID_CODE);
    }

    /**
     * A token exchange without explicit authorization_details must still succeed; the server
     * derives authorization_details from the requested scope.
     */
    @Test
    public void testTokenExchangeWithoutAuthorizationDetails() throws Exception {

        // Login with scope only, no explicit authorization_details
        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .send(TEST_USER, TEST_PASSWORD);
        String code = authResponse.getCode();
        assertNotNull(code, "Authorization code should not be null");

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(code).send();

        assertEquals(200, tokenResponse.getStatusCode(),
                "Token exchange should succeed without authorization_details");
        assertNotNull(tokenResponse.getAccessToken(), "Access token should be present");

        List<OID4VCAuthorizationDetail> authDetails = tokenResponse.getOID4VCAuthorizationDetails();
        assertNotNull(authDetails, "authorization_details should be derived from requested OID4VC scope");
        assertFalse(authDetails.isEmpty(), "authorization_details should not be empty");
        assertEquals(ctx.getCredentialConfigurationId(),
                authDetails.get(0).getCredentialConfigurationId(),
                "credential_configuration_id should match requested scope");
        assertNotNull(authDetails.get(0).getCredentialIdentifiers(), "credential_identifiers should be present");
        assertFalse(authDetails.get(0).getCredentialIdentifiers().isEmpty(),
                "credential_identifiers should not be empty");
    }

    /** Authorization details referencing an unknown credential configuration ID must be rejected. */
    @Test
    public void testMismatchedCredentialConfigurationId() throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);

        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(issuer, "unknown-credential-config-id");
        String code = performAuthorizationCodeLoginWithAuthorizationDetails(authDetail);

        AccessTokenResponse errorResponse = oauth.accessTokenRequest(code).send();

        assertEquals(400, errorResponse.getStatusCode());
        assertTrue(
                ErrorType.INVALID_CREDENTIAL_REQUEST.getValue().equals(errorResponse.getError()) ||
                ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION.getValue().equals(errorResponse.getError()) ||
                Errors.INVALID_AUTHORIZATION_DETAILS.equals(errorResponse.getError()) ||
                (errorResponse.getErrorDescription() != null &&
                 errorResponse.getErrorDescription().contains("authorization_details")),
                "Error response should indicate authorization_details processing error. Actual: "
                        + errorResponse.getError() + " / " + errorResponse.getErrorDescription());
    }

    /** Token exchange without redirect_uri must fail. */
    @Test
    public void testTokenExchangeWithoutRedirectUri() throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);

        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(issuer);
        String code = performAuthorizationCodeLoginWithAuthorizationDetails(authDetail);

        AccessTokenResponse errorResponse = new InvalidTokenRequest(code, oauth)
                .withClientId(clientId)
                .withClientSecret("password")
                .send();

        // Keycloak may return 400 or 401 depending on validation order
        assertTrue(errorResponse.getStatusCode() == 400 || errorResponse.getStatusCode() == 401,
                "Expected 400 or 401 but got " + errorResponse.getStatusCode());
    }

    /** A redirect_uri mismatch between authorization and token requests must fail. */
    @Test
    public void testTokenExchangeWithMismatchedRedirectUri() throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);

        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(issuer);
        String code = performAuthorizationCodeLoginWithAuthorizationDetails(authDetail);

        AccessTokenResponse errorResponse = new InvalidTokenRequest(code, oauth)
                .withClientId(clientId)
                .withClientSecret("password")
                .withRedirectUri("http://invalid-redirect-uri")
                .send();

        // Keycloak may return 400 or 401 depending on validation order
        assertTrue(errorResponse.getStatusCode() == 400 || errorResponse.getStatusCode() == 401,
                "Expected 400 or 401 but got " + errorResponse.getStatusCode());
    }

    /** Malformed JSON in a credential request must return 400. */
    @Test
    public void testCredentialRequestWithMalformedJson() throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        AccessTokenResponse tokenResponse = authzCodeFlow(issuer);
        String credentialIdentifier = assertTokenResponse(tokenResponse);

        // Drain events from the successful flow
        events.clear();

        String malformedJson = "{\"credential_identifier\":\"" + credentialIdentifier + "\", invalid json}";

        Oid4vcCredentialResponse credentialResponse = new InvalidCredentialRequest(oauth, malformedJson)
                .endpoint(issuer.getCredentialEndpoint())
                .bearerToken(tokenResponse.getAccessToken())
                .send();

        assertEquals(400, credentialResponse.getStatusCode());

        String error = credentialResponse.getError();
        String errorDescription = credentialResponse.getErrorDescription();
        assertTrue(
                error != null ||
                (errorDescription != null &&
                 (errorDescription.contains("invalid_credential_request") ||
                  errorDescription.contains("Failed to parse JSON") ||
                  errorDescription.contains("JSON") ||
                  errorDescription.contains("parse"))),
                "Error response should indicate JSON parsing failure or invalid request");

        EventAssertion.assertError(events.poll())
                .type(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                .error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue());
    }

    /** An invalid client_secret in a token exchange must fail with 401 unauthorized_client. */
    @Test
    public void testTokenExchangeWithInvalidClientSecret() throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);

        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(issuer);
        String code = performAuthorizationCodeLoginWithAuthorizationDetails(authDetail);

        AccessTokenResponse errorResponse = oauth.accessTokenRequest(code)
                .client(clientId, "wrong-secret")
                .send();

        assertEquals(401, errorResponse.getStatusCode());
        assertEquals("unauthorized_client", errorResponse.getError());
    }

    /** A token exchange without client_id must return 400 or 401. */
    @Test
    public void testTokenExchangeWithoutClientId() throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);

        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(issuer);
        String code = performAuthorizationCodeLoginWithAuthorizationDetails(authDetail);

        AccessTokenResponse errorResponse = new InvalidTokenRequest(code, oauth)
                .withClientSecret("password")
                .send();

        int status = errorResponse.getStatusCode();
        assertTrue(status == 400 || status == 401,
                "Should return 400 or 401 for missing client_id");
        assertTrue(
                ErrorType.INVALID_REQUEST.getValue().equals(errorResponse.getError()) ||
                "invalid_client".equals(errorResponse.getError()),
                "Error should be invalid_request or invalid_client. Got error: " + errorResponse.getError());
    }

    /** Malformed authorization_details JSON supplied in the authorization request must fail at the token endpoint. */
    @Test
    public void testTokenExchangeWithMalformedAuthorizationDetails() throws Exception {

        // Use loginForm directly to inject the malformed JSON as a raw parameter
        AuthorizationEndpointResponse authResponse = oauth.loginForm()
                .scope(ctx.getScope())
                .param(OAuth2Constants.AUTHORIZATION_DETAILS, "invalid-json")
                .doLogin(TEST_USER, TEST_PASSWORD);
        String code = authResponse.getCode();
        assertNotNull(code, "Authorization code should not be null");

        AccessTokenResponse errorResponse = oauth.accessTokenRequest(code).send();

        assertEquals(400, errorResponse.getStatusCode());
        assertEquals(Errors.INVALID_AUTHORIZATION_DETAILS, errorResponse.getError());
        assertTrue(
                errorResponse.getErrorDescription() != null &&
                errorResponse.getErrorDescription().contains("authorization_details"),
                "Error description should indicate authorization_details processing error");
    }

    /**
     * Token request authorization_details that exceed what was granted in the authorization request
     * must be rejected.
     */
    @Test
    public void testTokenExchangeRejectsAuthorizationDetailsNotGranted() throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);

        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(issuer);
        String code = performAuthorizationCodeLoginWithAuthorizationDetails(authDetail);

        OID4VCAuthorizationDetail differentDetail = createAuthorizationDetail(issuer, "different-credential-config-id");

        AccessTokenResponse errorResponse = oauth.accessTokenRequest(code)
                .authorizationDetails(List.of(differentDetail))
                .send();

        assertEquals(400, errorResponse.getStatusCode());
        assertEquals(Errors.INVALID_AUTHORIZATION_DETAILS, errorResponse.getError());
        assertTrue(
                errorResponse.getErrorDescription() != null &&
                errorResponse.getErrorDescription().contains("authorization_details"),
                "Error description should indicate authorization_details mismatch");
    }

    /**
     * A credential request using an unknown {@code credential_configuration_id} (instead of a
     * credential_identifier) must fail.
     */
    @Test
    public void testCredentialRequestWithUnknownCredentialConfigurationId() throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        AccessTokenResponse tokenResponse = authzCodeFlow(issuer);

        // Drain events from the successful flow
        events.clear();
        Proofs proofs = newJwtProofs();
        events.clear();

        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialConfigurationId("unknown-credential-config-id")
                .proofs(proofs)
                .bearerToken(tokenResponse.getAccessToken())
                .send();

        assertEquals(400, credentialResponse.getStatusCode());
        assertEquals(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION.getValue(), credentialResponse.getError());

        EventAssertion.assertError(events.poll())
                .type(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                .error(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION.getValue())
                .details(Details.REASON, "Credential configuration id 'unknown-credential-config-id' not found in authorization_details. The credential_configuration_id must match the one from the authorization_details in the access token.");
    }

    /** A credential request using a credential_identifier from a different flow must fail. */
    @Test
    public void testCredentialRequestWithMismatchedCredentialIdentifier() throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        // Important: obtaining issuer metadata and authz flow consumes events, clear them before continuing
        AccessTokenResponse tokenResponse = authzCodeFlow(issuer);
        assertTokenResponse(tokenResponse);

        // Drain events from the successful flow
        events.clear();
        Proofs proofs = newJwtProofs();
        events.clear();

        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier("00000000-0000-0000-0000-000000000000")
                .proofs(proofs)
                .bearerToken(tokenResponse.getAccessToken())
                .send();

        assertEquals(400, credentialResponse.getStatusCode());
        assertEquals(ErrorType.UNKNOWN_CREDENTIAL_IDENTIFIER.getValue(), credentialResponse.getError());

        EventAssertion.assertError(events.poll())
                .type(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                .error(ErrorType.UNKNOWN_CREDENTIAL_IDENTIFIER.getValue())
                .details(Details.REASON, "Credential identifier '00000000-0000-0000-0000-000000000000' not found in authorization_details. The credential_identifier must match one from the authorization_details in the access token.");
    }

    /**
     * A credential request without either {@code credential_configuration_id} or
     * {@code credential_identifier} must fail.
     */
    @Test
    public void testCredentialRequestWithoutIdentifier() throws Exception {

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        AccessTokenResponse tokenResponse = authzCodeFlow(issuer);
        assertTokenResponse(tokenResponse);

        // Drain events from the successful flow
        events.clear();

        Oid4vcCredentialResponse credentialResponse = new InvalidCredentialRequest(oauth, "{}")
                .endpoint(issuer.getCredentialEndpoint())
                .bearerToken(tokenResponse.getAccessToken())
                .send();

        assertEquals(400, credentialResponse.getStatusCode());
        assertEquals(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue(), credentialResponse.getError());

        EventAssertion.assertError(events.poll())
                .type(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                .error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue());
    }

    /**
     * Hook for format-specific credential structure verification.
     *
     * <p>Subclasses should override this to perform deep assertions on the issued credential
     * (e.g. checking specific claims, headers, or signatures).
     *
     * @param credentialObj the credential object returned by the server.
     */
    protected void verifyCredentialStructure(Object credentialObj) {
        assertNotNull(credentialObj, "Credential object should not be null");
    }

    /** Runs the authorization code flow with default mandatory-lastName claims. */
    protected AccessTokenResponse authzCodeFlow(CredentialIssuer issuer) throws Exception {
        return authzCodeFlow(issuer, mandatoryLastNameClaims());
    }

    protected AccessTokenResponse authzCodeFlow(CredentialIssuer issuer,
            List<ClaimsDescription> claimsForAuthorizationDetailsParameter) throws Exception {

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(ctx.getCredentialConfigurationId());
        authDetail.setClaims(claimsForAuthorizationDetailsParameter);
        authDetail.setLocations(Collections.singletonList(issuer.getCredentialIssuer()));

        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .authorizationDetails(authDetail)
                .send(TEST_USER, TEST_PASSWORD);
        String code = authResponse.getCode();
        assertNotNull(code, "Authorization code should not be null");

        return oauth.accessTokenRequest(code)
                .authorizationDetails(List.of(authDetail))
                .send();
    }

    /**
     * Constructs a list containing a mandatory {@code lastName} claim.
     *
     * @return a singleton list with the mandatory claim segment.
     */
    protected List<ClaimsDescription> mandatoryLastNameClaims() {
        ClaimsDescription claim = new ClaimsDescription();
        List<Object> claimPath;
        if ("sd_jwt_vc".equals(getCredentialFormat())) {
            claimPath = Collections.singletonList(getExpectedClaimPath());
        } else {
            claimPath = Arrays.asList("credentialSubject", getExpectedClaimPath());
        }
        claim.setPath(claimPath);
        claim.setMandatory(true);
        return List.of(claim);
    }

    /**
     * Asserts that the token response contains valid OID4VCI authorization details.
     *
     * @param tokenResponse the response to validate.
     * @return the first credential identifier found in the response.
     */
    protected String assertTokenResponse(AccessTokenResponse tokenResponse) {
        List<OID4VCAuthorizationDetail> authDetails = tokenResponse.getOID4VCAuthorizationDetails();
        assertNotNull(authDetails, "authorization_details should be present in the response");
        assertEquals(1, authDetails.size());

        OID4VCAuthorizationDetail detail = authDetails.get(0);
        assertNotNull(detail.getCredentialIdentifiers(), "Credential identifiers should be present");
        assertEquals(1, detail.getCredentialIdentifiers().size());
        assertNotNull(detail.getCredentialConfigurationId(), "Credential configuration id should not be null");

        List<String> ids = detail.getCredentialIdentifiers();
        assertNotNull(ids, "Credential identifiers should not be null");
        assertEquals(1, ids.size(),
                "Credential identifiers expected to have 1 item. Had " + ids.size() + " with value " + ids);
        return ids.get(0);
    }

    /**
     * Asserts that the credential response is successful (HTTP 200) and contains exactly one credential.
     *
     * @param credentialResponse the response to validate.
     */
    protected void assertSuccessfulCredentialResponse(Oid4vcCredentialResponse credentialResponse) {
        assertEquals(200, credentialResponse.getStatusCode(), "Credential response status should be 200. Got: " + credentialResponse.getStatusCode() + " / error: " + credentialResponse.getError());

        CredentialResponse parsedResponse = credentialResponse.getCredentialResponse();
        assertNotNull(parsedResponse, "Credential response should not be null");
        assertNotNull(parsedResponse.getCredentials(), "Credentials should be present");
        assertEquals(1, parsedResponse.getCredentials().size(), "Should have exactly one credential");

        CredentialResponse.Credential wrapper = parsedResponse.getCredentials().get(0);
        assertNotNull(wrapper, "Credential wrapper should not be null");

        Object credentialObj = wrapper.getCredential();
        assertNotNull(credentialObj, "Credential object should not be null");

        verifyCredentialStructure(credentialObj);
    }

    protected void assertErrorCredentialResponse(Oid4vcCredentialResponse credentialResponse) {
        assertEquals(400, credentialResponse.getStatusCode());
        assertEquals(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue(), credentialResponse.getError());
        assertEquals(
                "Credential issuance failed: No elements selected after processing claims path pointer. " +
                "The requested claims are not available in the user profile.",
                credentialResponse.getErrorDescription());
    }

    /** Creates a standard {@link OID4VCAuthorizationDetail} using the default credential configuration. */
    protected OID4VCAuthorizationDetail createAuthorizationDetail(CredentialIssuer issuer) {
        return createAuthorizationDetail(issuer, null);
    }

    /** Creates a standard {@link OID4VCAuthorizationDetail}, allowing override of the credential configuration ID. */
    protected OID4VCAuthorizationDetail createAuthorizationDetail(CredentialIssuer issuer,
            String credentialConfigurationId) {
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credentialConfigurationId != null
                ? credentialConfigurationId
                : ctx.getCredentialConfigurationId());
        authDetail.setLocations(Collections.singletonList(issuer.getCredentialIssuer()));
        return authDetail;
    }

    /** Performs an authorization code login with the given authorization detail. */
    protected String performAuthorizationCodeLoginWithAuthorizationDetails(OID4VCAuthorizationDetail authDetail) {
        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .authorizationDetails(authDetail)
                .send(TEST_USER, TEST_PASSWORD);
        String code = authResponse.getCode();
        assertNotNull(code, "Authorization code should not be null");
        return code;
    }

    /** Captures user state before a test that modifies the user profile. */
    protected UserState storeUserState() {
        UserRepresentation userRep = testRealm.admin().users().search(TEST_USER).get(0);
        UserResource userResource = testRealm.admin().users().get(userRep.getId());
        userRep = userResource.toRepresentation();
        return new UserState(userResource, userRep,
                userRep.getFirstName(),
                userRep.getLastName(),
                userRep.getAttributes() != null ? new HashMap<>(userRep.getAttributes()) : null);
    }

    /** Restores user state captured by {@link #storeUserState()}. */
    protected void restoreUserState(UserState userState) {
        UserRepresentation userRep = userState.user.toRepresentation();
        userRep.setFirstName(userState.originalFirstName);
        userRep.setLastName(userState.originalLastName);
        userRep.setAttributes(Objects.requireNonNullElse(userState.originalAttributes, Collections.emptyMap()));
        userState.user.update(userRep);
    }

    private Proofs newJwtProofs() {
        String cNonce = oauth.oid4vc().nonceRequest().send().getNonce();
        String issuer = oauth.oid4vc().issuerMetadataRequest().send().getMetadata().getCredentialIssuer();
        return OID4VCProofTestUtils.jwtProofs(issuer, cNonce);
    }

    protected static class UserState {
        final UserResource user;
        final UserRepresentation userRep;
        final String originalFirstName;
        final String originalLastName;
        final Map<String, List<String>> originalAttributes;

        UserState(UserResource user, UserRepresentation userRep,
                  String originalFirstName, String originalLastName,
                  Map<String, List<String>> originalAttributes) {
            this.user = user;
            this.userRep = userRep;
            this.originalFirstName = originalFirstName;
            this.originalLastName = originalLastName;
            this.originalAttributes = originalAttributes;
        }
    }
}
