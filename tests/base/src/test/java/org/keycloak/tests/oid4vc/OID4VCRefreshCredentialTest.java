package org.keycloak.tests.oid4vc;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserVerifiableCredentialResource;
import org.keycloak.common.util.Time;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.oid4vc.IssuedVerifiableCredentialRepresentation;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.ClientScopeBuilder;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OAuthErrorException.INVALID_GRANT;
import static org.keycloak.OAuthErrorException.INVALID_REQUEST;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_VCT;
import static org.keycloak.events.Details.CREDENTIAL_TYPE;
import static org.keycloak.events.Details.REASON;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testing scenarios related to refresh credentials and refresh requests
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-14.5">OID4VCI specification section about credential refresh</a>
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCRefreshCredentialTest extends OID4VCIssuerTestBase {

    @InjectUser(config = OID4VCActionTest.OID4VCTestUserConfig.class)
    ManagedUser user;

    OID4VCTestContext ctx;

    // See OpenID Conformance - VCIWarnOnAuthorizationDetailsInTokenEndpointResponseConventions
    private static final Set<String> KNOWN_AUTHORIZATION_DETAILS_FIELDS = Set.of(
            // Defined for openid_credential by OID4VCI 1.0 Final §5.1.1 / §6.2
            "type", "credential_configuration_id", "credential_identifiers", "claims",
            // RFC 9396 §2.2 common authorization-details fields — any type MAY include these
            "locations", "actions", "datatypes", "identifier", "privileges");

    @TestSetup
    public void configureTestRealm() {
        super.configureTestRealm();
        RealmResource realmResource = testRealm.admin();

        // Increase expiration of VC to the real value
        CredentialScopeRepresentation vcClientScope = realmResource.clientScopes().findAll()
                .stream()
                .filter(clientScope -> minimalJwtTypeCredentialScopeName.equals(clientScope.getName()))
                .map(CredentialScopeRepresentation::new)
                .findFirst().get();
        vcClientScope.setExpiryInSeconds(CredentialScopeModel.VC_EXPIRY_IN_SECONDS_DEFAULT);
        testRealm.admin().clientScopes().get(vcClientScope.getId()).update(vcClientScope);

        // Persist just refresh events
        RealmRepresentation realmRep = realmResource.toRepresentation();
        realmRep.setEnabledEventTypes(List.of(EventType.REFRESH_TOKEN.toString(), EventType.REFRESH_TOKEN_ERROR.toString()));
        realmResource.update(realmRep);
    }

    @BeforeEach
    void beforeEach() {
        ctx = new OID4VCTestContext(client, minimalJwtTypeCredentialScope);
        user.admin().logout();
        user.admin().verifiableCredentials().getIssuedCredentials()
                .forEach(issuedCred -> user.admin().verifiableCredentials().revokeIssuedCredential(issuedCred.getId()));
    }

    @AfterEach
    void resetTestState() {
        timeOffSet.set(0);
    }

    /**
     * Obtain authorization-code flow and obtain VC with access token.
     *
     * Then refresh token. After refreshing the token the new access-token must still be usable for a credential request
     * and there should be single issued-verifiable-credential instance
     *
     **/
    @Test
    public void testRefreshTokenSingleCredentialIssued() throws Exception {
        // Login
        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        AccessTokenResponse tokenResponse = authzCodeFlow(issuer);
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

        // Obtain credential
        String accessToken1 = tokenResponse.getAccessToken();
        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();

        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken1)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();
        assertSuccessfulCredentialResponse(credResponse);

        // Single issued-credential should be present
        List<IssuedVerifiableCredentialRepresentation> issuedCreds1 = testRealm.admin().users().get(user.getId()).verifiableCredentials().getIssuedCredentials();
        assertEquals(1, issuedCreds1.size(), "Single issued credential should be stored");
        IssuedVerifiableCredentialRepresentation issuedCred1 = issuedCreds1.get(0);

        // Assert issued-credential ID matches
        OID4VCAuthorizationDetail authzDetailResponse1 = ctx.getAuthorizationDetailFromAccessToken();
        assertEquals(issuedCred1.getId(), authzDetailResponse1.getIssuedCredentialId());

        // Verify that authorization_details on the token endpoint response have only known properties
        //
        for (OID4VCAuthorizationDetail authDetail : ctx.getAuthorizationDetails()) {
            String json = JsonSerialization.valueAsString(authDetail);
            Map<?, ?> authDetailMap = JsonSerialization.valueFromString(json, Map.class);
            for (Map.Entry<?, ?> entry : authDetailMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                assertTrue(KNOWN_AUTHORIZATION_DETAILS_FIELDS.contains(key), "Unknown authorization detail: " + key);
            }
        }

        // Move time a bit before refresh token
        timeOffSet.set(10);

        // Refresh token
        AccessTokenResponse refreshResponse = wallet.refreshRequest(ctx).send();
        assertTrue(refreshResponse.isSuccess(), "Refresh token exchange should succeed");
        String accessToken2 = refreshResponse.getAccessToken();
        EventAssertion.assertSuccess(events.poll())
                .details(CREDENTIAL_TYPE, ctx.getCredentialConfigurationId())
                .type(EventType.REFRESH_TOKEN);

        // Assert issued-credential ID matches and has not changed
        OID4VCAuthorizationDetail authzDetailResponse2 = ctx.getAuthorizationDetailFromAccessToken();
        assertEquals(issuedCred1.getId(), authzDetailResponse2.getIssuedCredentialId());

        // Verify that authorization_details on the token endpoint response have only known properties
        //
        for (OID4VCAuthorizationDetail authDetail : ctx.getAuthorizationDetails()) {
            String json = JsonSerialization.valueAsString(authDetail);
            Map<?, ?> authDetailMap = JsonSerialization.valueFromString(json, Map.class);
            for (Map.Entry<?, ?> entry : authDetailMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                assertTrue(KNOWN_AUTHORIZATION_DETAILS_FIELDS.contains(key), "Unknown authorization detail: " + key);
            }
        }

        // Obtain another VC
        credResponse = wallet.credentialRequest(ctx, accessToken2)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();
        assertSuccessfulCredentialResponse(credResponse);

        List<IssuedVerifiableCredentialRepresentation> issuedCreds2 = testRealm.admin().users().get(user.getId()).verifiableCredentials().getIssuedCredentials();
        assertEquals(1, issuedCreds2.size(), "Single issued credential should be stored");
        IssuedVerifiableCredentialRepresentation issuedCred2 = issuedCreds2.get(0);

        // Verify issuedAt and expiresAt did not changed for the stored issued-credential after refresh-token and obtain another VC
        assertEquals(issuedCred1.getId(), issuedCred2.getId());
        assertEquals(issuedCred1.getIssuedAt(), issuedCred2.getIssuedAt());
        assertEquals(issuedCred1.getExpiresAt(), issuedCred2.getExpiresAt());
        assertEquals(issuedCred1.getRevision(), issuedCred2.getRevision());
    }


    /**
     * Obtain authorization-code flow and obtain VC with access token.
     * Then make sure that refresh token is successful even after user session is expired (EG. after 14 days)
     **/
    @Test
    public void testRefreshSuccessAfterSessionExpired() throws Exception {
        // Login
        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        AccessTokenResponse tokenResponse = authzCodeFlow(issuer);
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

        // Obtain credential
        String accessToken1 = tokenResponse.getAccessToken();
        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();

        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken1)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();
        assertSuccessfulCredentialResponse(credResponse);

        // Move time a 14 days forward (user session is expired already at this point)
        timeOffSet.set(1209600);

        // Refresh token
        AccessTokenResponse refreshResponse = wallet.refreshRequest(ctx).send();
        assertTrue(refreshResponse.isSuccess(), "Refresh token exchange should succeed");
        String accessTokenRefreshed = refreshResponse.getAccessToken();

        // Obtain another VC
        credResponse = wallet.credentialRequest(ctx, accessTokenRefreshed)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();
        assertSuccessfulCredentialResponse(credResponse);

        // Move time 28 days forward and try 3rd refresh
        timeOffSet.set(2419200);

        // Refresh token
        refreshResponse = wallet.refreshRequest(ctx).send();
        assertTrue(refreshResponse.isSuccess(), "Refresh token exchange should succeed");
        accessTokenRefreshed = refreshResponse.getAccessToken();

        // Obtain another VC
        credResponse = wallet.credentialRequest(ctx, accessTokenRefreshed)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();
        assertSuccessfulCredentialResponse(credResponse);
    }

    /**
     * Obtain authorization-code flow and obtain VC with access token.
     * Then remove issued verifiable credential and try to refresh token. Refresh should fail due the issued VC revoked
     **/
    @Test
    public void testRefreshFailsWhenIssuedCredentialRemoved() throws Exception {
        // Login
        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        AccessTokenResponse tokenResponse = authzCodeFlow(issuer);
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

        // Obtain credential
        String accessToken1 = tokenResponse.getAccessToken();
        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();

        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken1)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();
        assertSuccessfulCredentialResponse(credResponse);

        // Single issued-credential should be present
        UserVerifiableCredentialResource credResource = testRealm.admin().users().get(user.getId()).verifiableCredentials();
        List<IssuedVerifiableCredentialRepresentation> issuedCreds1 = credResource.getIssuedCredentials();
        assertEquals(1, issuedCreds1.size(), "Single issued credential should be stored");
        IssuedVerifiableCredentialRepresentation issuedCred1 = issuedCreds1.get(0);

        // Remove issued credential
        credResource.revokeIssuedCredential(issuedCred1.getId());

        // Try to refresh
        AccessTokenResponse refreshResponse = wallet.refreshRequest(ctx).send();
        assertFalse(refreshResponse.isSuccess(), "Refresh token exchange should fail");
        assertNull(refreshResponse.getAccessToken());
        EventAssertion.assertError(events.poll())
                .type(EventType.REFRESH_TOKEN_ERROR)
                .details(CREDENTIAL_TYPE, ctx.getCredentialConfigurationId())
                .details(REASON, "Verifiable credential not found")
                .error(Errors.INVALID_TOKEN);

        assertEquals(INVALID_REQUEST, refreshResponse.getError());
        assertEquals("Verifiable credential not found", refreshResponse.getErrorDescription());
    }

    /**
     * Obtain authorization-code flow and obtain VC with access token.
     * Then make sure that issued verifiable credential is expired and try to refresh token. Refresh should fail due the issued VC expired
     **/
    @Test
    public void testRefreshFailsWhenIssuedCredentialExpired() throws Exception {
        // Login
        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        AccessTokenResponse tokenResponse = authzCodeFlow(issuer);
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

        // Obtain credential
        String accessToken1 = tokenResponse.getAccessToken();
        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();

        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken1)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();
        assertSuccessfulCredentialResponse(credResponse);

        // Single issued-credential should be present
        UserVerifiableCredentialResource credResource = testRealm.admin().users().get(user.getId()).verifiableCredentials();
        List<IssuedVerifiableCredentialRepresentation> issuedCreds1 = credResource.getIssuedCredentials();
        assertEquals(1, issuedCreds1.size(), "Single issued credential should be stored");
        IssuedVerifiableCredentialRepresentation issuedCred1 = issuedCreds1.get(0);
        long expiresAt = issuedCred1.getExpiresAt() / 1000;

        // Move time forward to the point when issued-credential is expired
        long timeOffset = expiresAt - Time.currentTime() + 10;
        timeOffSet.set(Duration.ofSeconds(timeOffset));

        // Try to refresh
        AccessTokenResponse refreshResponse = wallet.refreshRequest(ctx).send();
        assertFalse(refreshResponse.isSuccess(), "Refresh token exchange should fail");
        assertNull(refreshResponse.getAccessToken());
        assertEquals(INVALID_GRANT, refreshResponse.getError());
        assertEquals("Token is not active", refreshResponse.getErrorDescription());
    }

    /**
     * Obtain authorization-code flow and obtain VC with access token.
     * Then send refresh token requests with "scope" parameter. In case that OID4VCI scope is included, refresh should be OK. In case that oid4vci scope is missing in the "scope"
     * parameter, the refresh should fail as it is OID4VCI refresh token, but without the OID4VCI scope requested inside "scope" parameter
     **/
    @Test
    public void testRefreshWithScopeParameter() throws Exception {
        // Login
        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        AccessTokenResponse tokenResponse = authzCodeFlow(issuer);
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

        // Obtain credential
        String accessToken1 = tokenResponse.getAccessToken();
        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();

        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken1)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();
        assertSuccessfulCredentialResponse(credResponse);

        timeOffSet.set(10);

        // Refresh token with custom "scope" parameter, which contains the requested OID4VCI scope. Should be successful
        String origScope = oauth.config().getScope(false);

        try {
            oauth.config().openid(false).scope(ctx.getScope());
            AccessTokenResponse refreshResponse = wallet.refreshRequest(ctx).send();
            assertTrue(refreshResponse.isSuccess(), "Refresh token exchange should succeed");
            String accessTokenRefreshed = refreshResponse.getAccessToken();

            // Obtain another VC
            credResponse = wallet.credentialRequest(ctx, accessTokenRefreshed)
                    .credentialIdentifier(credentialIdentifier)
                    .send().getCredentialResponse();
            assertSuccessfulCredentialResponse(credResponse);

            // Refresh token with custom "scope" parameter, which does not contain the requested OID4VCI scope
            oauth.config().openid(false).scope(OAuth2Constants.SCOPE_PROFILE);
            refreshResponse = wallet.refreshRequest(ctx).send();
            assertFalse(refreshResponse.isSuccess(), "Refresh token exchange should fail");
            assertNull(refreshResponse.getAccessToken());
            assertEquals(INVALID_REQUEST, refreshResponse.getError());
            assertTrue(refreshResponse.getErrorDescription().startsWith("Not found credential scope model"));
        } finally {
            oauth.config().openid(true).scope(origScope);
        }
    }

    /**
     * Test that the VC expiration (exp claim) uses the refresh interval,
     * while the issued credential and refresh token use the credential lifetime.
     * To verify the core feature: separating VC expiration from refresh token expiration.
     */
    @Test
    public void testVCExpirationUsesRefreshInterval() throws Exception {
        //Using different values to configure credential scope
        int credentialLifetime = 31536000; // 365 days
        int refreshInterval = 604800; // 7 days

       String scopeId = getCredentialScopeId(minimalJwtTypeCredentialScopeName);

       testRealm.updateClientScope(scopeId, clientScope -> {
            CredentialScopeRepresentation credScopeRep = new CredentialScopeRepresentation(clientScope.build());
            credScopeRep.setExpiryInSeconds(credentialLifetime);
            credScopeRep.setRefreshIntervalInSeconds(refreshInterval);
            return ClientScopeBuilder.update(credScopeRep);
        });

        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        AccessTokenResponse tokenResponse = authzCodeFlow(issuer);
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

        String accessToken = tokenResponse.getAccessToken();
        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();

        CredentialResponse response = wallet.credentialRequest(ctx,accessToken)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();
        assertSuccessfulCredentialResponse(response);

        CredentialResponse.Credential credentialObj = response.getCredentials().get(0);
        assertNotNull(credentialObj);
        IssuerSignedJWT issuerSignedJWT = SdJwtVP.of(credentialObj.getCredential().toString()).getIssuerSignedJWT();

        long iat = issuerSignedJWT.getPayload().get("iat").asLong();
        long exp = issuerSignedJWT.getPayload().get("exp").asLong();
        long vcLifetimeSeconds = exp - iat;

        long tolerance = 60; // 1 minute tolerance for time normalization
        assertTrue(Math.abs(vcLifetimeSeconds - refreshInterval) <= tolerance,
                String.format("VC lifetime should be ~%d seconds (refresh interval), but was %d seconds",
                        refreshInterval, vcLifetimeSeconds));

        // 2. Verify issued credential expiration is based on credential lifetime (365 days)
        List<IssuedVerifiableCredentialRepresentation> issuedCreds =
                testRealm.admin().users().get(user.getId()).verifiableCredentials().getIssuedCredentials();
        assertEquals(1, issuedCreds.size());
        IssuedVerifiableCredentialRepresentation issuedCred = issuedCreds.get(0);

        long issuedCredLifetimeSeconds = (issuedCred.getExpiresAt() - issuedCred.getIssuedAt()) / 1000;
        assertTrue(Math.abs(issuedCredLifetimeSeconds - credentialLifetime) <= tolerance,
                String.format("Issued credential lifetime should be ~%d seconds (credential lifetime), but was %d seconds",
                        credentialLifetime, issuedCredLifetimeSeconds));

        // 3. Verify refresh token expiration matches credential lifetime (NOT refresh interval)
        String refreshToken = tokenResponse.getRefreshToken();
        assertNotNull(refreshToken);

        // Decode the refresh token JWT
        String[] parts = refreshToken.split("\\.");
        assertEquals(3, parts.length, "Expected refresh token to be a JWT with 3 parts");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        JsonNode refreshTokenPayload = JsonSerialization.readValue(payload, JsonNode.class);

        long refreshTokenIat = refreshTokenPayload.get("iat").asLong();
        long refreshTokenExp = refreshTokenPayload.get("exp").asLong();
        long refreshTokenLifetimeSeconds = refreshTokenExp - refreshTokenIat;

        // Refresh token lifetime should match credential lifetime (365 days), NOT refresh interval (7 days)
        assertTrue(Math.abs(refreshTokenLifetimeSeconds - credentialLifetime) <= tolerance,
                String.format("Refresh token lifetime should be ~%d seconds (credential lifetime), but was %d seconds",
                        credentialLifetime, refreshTokenLifetimeSeconds));
    }

    /**
     * Test that a custom refresh interval value is correctly applied to the VC.
     */
    @Test
    public void testCustomRefreshInterval() throws Exception {
        // Set custom refresh interval (1 hour)
        int customRefreshInterval = 3600;

        String scopeId = getCredentialScopeId(minimalJwtTypeCredentialScopeName);
        testRealm.updateClientScope(scopeId, clientScope -> {
            CredentialScopeRepresentation credScopeRep = new CredentialScopeRepresentation(clientScope.build());
            credScopeRep.setRefreshIntervalInSeconds(customRefreshInterval);
            return ClientScopeBuilder.update(credScopeRep);
        });

        // Obtain VC
        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        AccessTokenResponse tokenResponse = authzCodeFlow(issuer);
        assertTrue(tokenResponse.isSuccess());

        CredentialResponse credResponse = wallet.credentialRequest(ctx, tokenResponse.getAccessToken())
                .credentialIdentifier(ctx.getAuthorizedCredentialIdentifier())
                .send().getCredentialResponse();
        assertSuccessfulCredentialResponse(credResponse);

        // Parse and verify VC expiration
        CredentialResponse.Credential credentialObj = credResponse.getCredentials().get(0);
        IssuerSignedJWT issuerSignedJWT = SdJwtVP.of(credentialObj.getCredential().toString()).getIssuerSignedJWT();

        long iat = issuerSignedJWT.getPayload().get("iat").asLong();
        long exp = issuerSignedJWT.getPayload().get("exp").asLong();
        long vcLifetimeSeconds = exp - iat;

        long tolerance = 60;
        assertTrue(Math.abs(vcLifetimeSeconds - customRefreshInterval) <= tolerance,
                                    String.format("VC lifetime should be ~%d seconds, but was %d seconds",
                                    customRefreshInterval, vcLifetimeSeconds));
    }

    /**
     * Test that you can successfully refresh a credential even after the VC itself has expired,
     * as long as the refresh token is still valid.
     *
     */
    @Test
    public void testRefreshSucceedsAfterVCExpired() throws Exception {
        // Configure with short refresh interval for testing
        int credentialLifetime = 86400; // 1 day (for easier testing)
        int refreshInterval = 3600; // 1 hour
        String scopeId = getCredentialScopeId(minimalJwtTypeCredentialScopeName);

        testRealm.updateClientScope(scopeId, clientScope -> {
            CredentialScopeRepresentation credScopeRep = new CredentialScopeRepresentation(clientScope.build());
            credScopeRep.setExpiryInSeconds(credentialLifetime);
            credScopeRep.setRefreshIntervalInSeconds(refreshInterval);
            return ClientScopeBuilder.update(credScopeRep);
        });

        // Obtain VC
        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        AccessTokenResponse tokenResponse = authzCodeFlow(issuer);
        assertTrue(tokenResponse.isSuccess());

        CredentialResponse credResponse_1 = wallet.credentialRequest(ctx, tokenResponse.getAccessToken())
                .credentialIdentifier(ctx.getAuthorizedCredentialIdentifier())
                .send().getCredentialResponse();
        assertSuccessfulCredentialResponse(credResponse_1);

        // Parse first VC
        CredentialResponse.Credential credential_1 = credResponse_1.getCredentials().get(0);
        IssuerSignedJWT jwt_1 = SdJwtVP.of(credential_1.getCredential().toString()).getIssuerSignedJWT();
        long exp_1 = jwt_1.getPayload().get("exp").asLong();

        // Fast-forward time past VC expiration (2 hours)
        timeOffSet.set(7200); // 2 hours

        // Verify VC is expired
        long currentTime = Time.currentTime();
        assertTrue(currentTime > exp_1, "VC should be expired");

        // Refresh token should still work
        AccessTokenResponse refreshResponse = wallet.refreshRequest(ctx).send();
        assertTrue(refreshResponse.isSuccess(), "Refresh should succeed even though VC is expired");


        // Obtain new VC with the refreshed access token
        CredentialResponse credResponse_2 = wallet.credentialRequest(ctx, refreshResponse.getAccessToken())
                .credentialIdentifier(ctx.getAuthorizedCredentialIdentifier())
                .send().getCredentialResponse();
        assertSuccessfulCredentialResponse(credResponse_2);

        // Parse new VC
        CredentialResponse.Credential credential_2 = credResponse_2.getCredentials().get(0);
        IssuerSignedJWT jwt_2 = SdJwtVP.of(credential_2.getCredential().toString()).getIssuerSignedJWT();

        long iat_2 = jwt_2.getPayload().get("iat").asLong();
        long exp_2 = jwt_2.getPayload().get("exp").asLong();

        // New VC should have fresh expiration based on refresh interval
        long newVcLifetime = exp_2 - iat_2;
        long tolerance = 60;
        assertTrue(Math.abs(newVcLifetime - refreshInterval) <= tolerance, "New VC should have fresh expiration based on refresh interval");

        // New VC should not be expired yet
        assertTrue(currentTime < exp_2, "New VC should not be expired");

        // Both VCs should point to the same issued credential (same ID)
        List<IssuedVerifiableCredentialRepresentation> issuedCreds = testRealm.admin().users().get(user.getId())
                                                                              .verifiableCredentials().getIssuedCredentials();
        assertEquals(1, issuedCreds.size(), "Should still be only one issued credential");
    }

    protected AccessTokenResponse authzCodeFlow(CredentialIssuer issuer) throws Exception {
        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .send(user.getUsername(), TEST_PASSWORD);
        String code = authResponse.getCode();
        assertNotNull(code, "Authorization code should not be null");

        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, code).send();
        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken, "No accessToken");
        return tokenResponse;
    }

    protected void assertSuccessfulCredentialResponse(CredentialResponse credentialResponse) {
        CredentialResponse.Credential credentialObj = credentialResponse.getCredentials().get(0);
        assertNotNull(credentialObj, "The first credential in the array should not be null");
        IssuerSignedJWT issuerSignedJWT = SdJwtVP.of(credentialObj.getCredential().toString()).getIssuerSignedJWT();
        assertEquals(minimalJwtTypeCredentialScopeName, issuerSignedJWT.getPayload().get(CLAIM_NAME_VCT).asText());
    }

    private String getCredentialScopeId(String credentialScopeName) {
        return testRealm.admin()
                .clientScopes().findAll()
                .stream()
                .filter(cs -> credentialScopeName.equals(cs.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Credential scope not found:" + credentialScopeName))
                .getId();
    }
}
