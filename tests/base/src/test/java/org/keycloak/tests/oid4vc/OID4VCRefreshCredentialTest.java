package org.keycloak.tests.oid4vc;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.VCFormat;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserVerifiableCredentialResource;
import org.keycloak.common.util.Time;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.utils.CredentialScopeUtils;
import org.keycloak.protocol.oid4vc.utils.OID4VCUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.oid4vc.IssuedVerifiableCredentialRepresentation;
import org.keycloak.representations.idm.oid4vc.UserVerifiableCredentialRepresentation;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.ClientScopeBuilder;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.OID4VCCredentialOfferPage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OAuthErrorException.INVALID_GRANT;
import static org.keycloak.OAuthErrorException.INVALID_REQUEST;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_EXP;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_VCT;
import static org.keycloak.events.Details.CREDENTIAL_TYPE;
import static org.keycloak.events.Details.REASON;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testing scenarios related to refresh credentials and refresh requests
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-14.5">OID4VCI specification section about credential refresh</a>
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCRefreshCredentialTest extends OID4VCIssuerTestBase {

    @InjectPage
    OID4VCCredentialOfferPage credentialOfferPage;

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
        resetUserNotBefore();
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
    public void testRefreshTokenSingleCredentialIssued() {
        AccessTokenResponse tokenResponse = authzCodeFlow();
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
        assertNull(authzDetailResponse2.getCredentialsOfferId(),
                "credentials_offer_id must not be present in refresh-token-derived access token");

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
    public void testRefreshSuccessAfterSessionExpired() {
        AccessTokenResponse tokenResponse = authzCodeFlow();
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
    public void testRefreshFailsWhenIssuedCredentialRemoved() {
        AccessTokenResponse tokenResponse = authzCodeFlow();
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
    public void testRefreshFailsWhenIssuedCredentialExpired() {
        AccessTokenResponse tokenResponse = authzCodeFlow();
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

    @Test
    public void testRefreshFailsWhenUserLoggedOut() {
        issueCredential();

        timeOffSet.set(10);
        user.admin().logout();

        assertRefreshFailsForStaleIssuedCredential();
    }

    @Test
    public void testIssuedCredentialInvalidatedByRealmLogoutAll() {
        IssuedVerifiableCredentialRepresentation issuedCredential = issueCredential();
        RealmRepresentation realmRep = testRealm.admin().toRepresentation();
        int originalRealmNotBefore = realmRep.getNotBefore() != null ? realmRep.getNotBefore() : 0;

        try {
            timeOffSet.set(10);
            testRealm.admin().logoutAll();

            assertIssuedCredentialRejectedAsStale(issuedCredential.getId());
            assertRefreshFailsForStaleToken();
        } finally {
            realmRep = testRealm.admin().toRepresentation();
            realmRep.setNotBefore(originalRealmNotBefore);
            testRealm.admin().update(realmRep);
        }
    }

    @Test
    public void testIssuedCredentialInvalidatedByRealmNotBefore() {
        IssuedVerifiableCredentialRepresentation issuedCredential = issueCredential();
        int staleNotBefore = Time.currentTime() + 10;

        testRealm.updateWithCleanup(realm -> realm.notBefore(staleNotBefore));

        assertIssuedCredentialRejectedAsStale(issuedCredential.getId());
    }

    @Test
    public void testIssuedCredentialInvalidatedByClientNotBefore() {
        IssuedVerifiableCredentialRepresentation issuedCredential = issueCredential();
        ClientRepresentation clientRep = managedClient.admin().toRepresentation();
        int originalClientNotBefore = clientRep.getNotBefore() != null ? clientRep.getNotBefore() : 0;

        try {
            clientRep.setNotBefore(Time.currentTime() + 10);
            managedClient.admin().update(clientRep);

            assertIssuedCredentialRejectedAsStale(issuedCredential.getId());
        } finally {
            clientRep = managedClient.admin().toRepresentation();
            clientRep.setNotBefore(originalClientNotBefore);
            managedClient.admin().update(clientRep);
        }
    }

    @Test
    public void testIssuedCredentialInvalidatedByUserNotBefore() {
        IssuedVerifiableCredentialRepresentation issuedCredential = issueCredential();

        timeOffSet.set(10);
        user.admin().logout();

        assertIssuedCredentialRejectedAsStale(issuedCredential.getId());
    }

    /**
     * Obtain authorization-code flow and obtain VC with access token.
     * Then send refresh token requests with "scope" parameter. In case that OID4VCI scope is included, refresh should be OK. In case that oid4vci scope is missing in the "scope"
     * parameter, the refresh should fail as it is OID4VCI refresh token, but without the OID4VCI scope requested inside "scope" parameter
     **/
    @Test
    public void testRefreshWithScopeParameter() {
        AccessTokenResponse tokenResponse = authzCodeFlow();
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
     * Verify credential re-issuance succeeds with refresh token after the original
     * credential offer has been removed.
     */
    @Test
    public void testRefreshSucceedsAfterCredentialOfferRemoved() {
        // Create credential offer via AIA
        oauth.loginForm()
                .kcAction(OID4VCActionTest.getKcActionParameter(client.getClientId(), minimalJwtTypeCredentialConfigurationIdName, false))
                .open();
        oauth.fillLoginForm(user.getUsername(), TEST_PASSWORD);

        credentialOfferPage.assertCurrent();
        String credentialOfferUri = credentialOfferPage.getCredentialOfferUri();
        String credentialOfferNonce = OID4VCActionTest.getNonceFromCredentialOfferUri(credentialOfferUri);

        CredentialOfferResponse credentialOfferResponse = oauth.oid4vc().credentialOfferRequest(credentialOfferNonce).send();
        CredentialsOffer credOffer = credentialOfferResponse.getCredentialsOffer();
        assertNotNull(credOffer, "Credential offer should be created");

        String issuerState = credOffer.getIssuerState();
        assertNotNull(issuerState);

        AccessTokenResponse tokenResponse = authzCodeFlow(issuerState);
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

        String accessToken1 = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken1, "access token must be present");

        OID4VCAuthorizationDetail authzDetail1 = ctx.getAuthorizationDetailFromAccessToken();
        assertNotNull(authzDetail1, "Authorization detail should be present in the first access token");

        // Verify credentialsOfferId is present in first authorization detail/access token
        assertNotNull(authzDetail1.getCredentialsOfferId(), "credentials_offer_id must be present in the first access token");

        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();
        CredentialResponse credResponse1 = wallet.credentialRequest(ctx, accessToken1)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();

        assertSuccessfulCredentialResponse(credResponse1);

        // Verify the issued credential was created and stored
        List<IssuedVerifiableCredentialRepresentation> issuedCreds1 = testRealm.admin().users().get(user.getId()).verifiableCredentials().getIssuedCredentials();
        assertEquals(1, issuedCreds1.size(), "One issued credential should exist after first issuance");
        String issuedCredId = issuedCreds1.get(0).getId();

        // Verify issuedCredentialId from authorization_details matches the stored issued credential
        assertEquals(issuedCredId, authzDetail1.getIssuedCredentialId(), "issued_credential_id in authorization_details should match the stored issued credential");

        //Time Delay
        timeOffSet.set(10);

        // Refresh the access token using the refresh token
        AccessTokenResponse refreshTokenResponse = wallet.refreshRequest(ctx).send();
        assertTrue(refreshTokenResponse.isSuccess(), "Refresh token exchange should succeed");
        String accessToken2 = refreshTokenResponse.getAccessToken();

        EventAssertion.assertSuccess(events.poll())
                .details(CREDENTIAL_TYPE, ctx.getCredentialConfigurationId())
                .type(EventType.REFRESH_TOKEN);

        OID4VCAuthorizationDetail authzDetail2 = ctx.getAuthorizationDetailFromAccessToken();
        assertNull(authzDetail2.getCredentialsOfferId(), "Refreshed access token should NOT contain credentialsOfferId");

        assertEquals(issuedCredId, authzDetail2.getIssuedCredentialId(), "Refreshed access token should contain the correct issuedCredentialId");

        CredentialResponse credResponse2 = wallet.credentialRequest(ctx, accessToken2)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();

        assertSuccessfulCredentialResponse(credResponse2);

        List<IssuedVerifiableCredentialRepresentation> issuedCreds2 = testRealm.admin().users().get(user.getId()).verifiableCredentials().getIssuedCredentials();
        assertEquals(1, issuedCreds2.size(), "Should still have one issued credential (re-issuance, not duplication)");
        assertEquals(issuedCredId, issuedCreds2.get(0).getId(), "Issued credential ID should remain the same after refresh and re-issuance");
    }

    /**
     * Verify that attempting to request a credential after the credential offer has expired results in an exception being thrown.
     */
    @Test
    public void testThrowsExceptionWhenCredentialOfferExpiredBeforeIssuance() {
        // Configure a short credential offer lifespan (3 seconds)
        RealmRepresentation realmRep = testRealm.admin().toRepresentation();
        realmRep.getAttributes().put(OID4VCIssuerEndpoint.CREDENTIAL_OFFER_LIFESPAN_REALM_ATTRIBUTE_KEY, "3");
        testRealm.admin().update(realmRep);

        try {
            // Create credential offer via AIA
            oauth.loginForm()
                    .kcAction(OID4VCActionTest.getKcActionParameter(client.getClientId(), minimalJwtTypeCredentialConfigurationIdName, false))
                    .open();
            oauth.fillLoginForm(user.getUsername(), TEST_PASSWORD);

            credentialOfferPage.assertCurrent();
            String credentialOfferUri = credentialOfferPage.getCredentialOfferUri();
            String credentialOfferNonce = OID4VCActionTest.getNonceFromCredentialOfferUri(credentialOfferUri);

            CredentialOfferResponse credentialOfferResponse = oauth.oid4vc().credentialOfferRequest(credentialOfferNonce).send();
            CredentialsOffer credOffer = credentialOfferResponse.getCredentialsOffer();
            assertNotNull(credOffer, "Credential offer should be created");

            String issuerState = credOffer.getIssuerState();
            assertNotNull(issuerState);

            AccessTokenResponse tokenResponse = authzCodeFlow(issuerState);
            assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

            String accessToken1 = wallet.validateHolderAccessToken(ctx, tokenResponse);
            assertNotNull(accessToken1, "access token must be present");

            OID4VCAuthorizationDetail authzDetail1 = ctx.getAuthorizationDetailFromAccessToken();
            assertNotNull(authzDetail1, "Authorization detail should be present in the first access token");

            // Verify credentialsOfferId is present in first authorization detail/access token
            assertNotNull(authzDetail1.getCredentialsOfferId(), "credentials_offer_id must be present in the first access token");

            String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();

            timeOffSet.set(8);

            Exception exception = assertThrows(IllegalStateException.class, () -> wallet.credentialRequest(ctx, accessToken1)
                    .credentialIdentifier(credentialIdentifier)
                    .send().getCredentialResponse());

            String expectedExceptionMessage = "Credential offer has already expired";
            assertTrue(exception.getMessage().contains(expectedExceptionMessage), "Expected exception message for expired credential offer");
        } finally {
            // Restore default credential offer lifespan
            realmRep = testRealm.admin().toRepresentation();
            realmRep.getAttributes().remove(OID4VCIssuerEndpoint.CREDENTIAL_OFFER_LIFESPAN_REALM_ATTRIBUTE_KEY);
            testRealm.admin().update(realmRep);
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

        AccessTokenResponse tokenResponse = authzCodeFlow();
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
     * Verifies that a mapper mapping user-controlled data to the reserved 'exp' claim causes the SD-JWT issuance
     * to be rejected. A mapper persisted via the scope-create path (which, like imports, does not run
     * config-time validation) could otherwise let a user-controlled value extend the refresh expiration time, so
     * it must fail the request at issuance.
     */
    @Test
    public void testUserControlledExpAttributeDoesNotExtendRefreshExpiration() throws Exception {
        long farFuture = Time.currentTimeSeconds() + 10L * 365 * 24 * 3600; // +10 years

        // Create an SD-JWT credential scope whose reserved 'exp' claim is mapped from user data
        String scopeName = "reserved-exp-scope-" + UUID.randomUUID();
        CredentialScopeRepresentation scope = new CredentialScopeRepresentation(scopeName)
                .setIncludeInTokenScope(true)
                .setCredentialConfigurationId(scopeName + "-config-id")
                .setCredentialIdentifier(scopeName)
                .setVct(scopeName)
                .setFormat(VCFormat.SD_JWT_VC);
        scope.setProtocolMappers(List.of(ProtocolMapperUtils.getUserAttributeMapper(CLAIM_NAME_EXP, "some-user-attribute")));

        String scopeId;
        try (Response response = testRealm.admin().clientScopes().create(scope)) {
            scopeId = ApiUtil.getCreatedId(response);
        }
        testRealm.cleanup().add(r -> r.clientScopes().get(scopeId).remove());
        testRealm.admin().clients().get(client.getId()).addOptionalClientScope(scopeId);

        // Grant the user a verifiable credential for the new scope so the access token exchange accepts it.
        UserVerifiableCredentialRepresentation granted = new UserVerifiableCredentialRepresentation();
        granted.setCredentialScopeName(scopeName);
        testRealm.admin().users().get(user.getId()).verifiableCredentials().createCredential(granted);

        // Set a user-controlled attribute to a far-future timestamp to attempt to extend the refresh expiration.
        user.updateWithCleanup(u -> u.attribute("some-user-attribute", String.valueOf(farFuture)));

        ctx = new OID4VCTestContext(client, scope);

        // The access token exchange succeeds, but issuance must be rejected: the reserved 'exp' mapping is a
        // misconfiguration that cannot be safely issued, so it fails the request instead of silently extending
        // the refresh expiration.
        AccessTokenResponse tokenResponse = authzCodeFlow();
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

        Oid4vcCredentialResponse credResponse = wallet.credentialRequest(ctx, tokenResponse.getAccessToken())
                .credentialIdentifier(ctx.getAuthorizedCredentialIdentifier())
                .send();
        assertEquals(HttpStatus.SC_BAD_REQUEST, credResponse.getStatusCode());
        assertEquals(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue(), credResponse.getError());
        assertTrue(credResponse.getErrorDescription().contains("Claim name 'exp' is reserved and must not be used by this OID4VC mapper"),
                "Issuance rejection should report the reserved claim, but was: " + credResponse.getErrorDescription());
    }

    /**
     * Test that both initial and refreshed access tokens have their audience limited to the credential endpoint.
     * This verifies that the OID4VCITokenPostProcessor correctly sets the 'aud' claim.
     */
    @Test
    public void testAccessTokenAudienceLimitedToCredentialEndpoint() throws Exception {
        // Login
        CredentialIssuer issuer = wallet.getIssuerMetadata(ctx);
        AccessTokenResponse tokenResponse = authzCodeFlow();
        assertTrue(tokenResponse.isSuccess());

        // Get the expected credential endpoint URL from issuer metadata
        String expectedAudience = issuer.getCredentialEndpoint();
        assertNotNull(expectedAudience);

        // Verify initial access token has correct audience
        String accessToken1 = tokenResponse.getAccessToken();
        wallet.assertAccessTokenAudience(accessToken1, expectedAudience);

        // Obtain credential to ensure the token works
        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();
        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken1)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();
        assertSuccessfulCredentialResponse(credResponse);

        // Move time forward a bit
        timeOffSet.set(10);

        // Refresh token
        AccessTokenResponse refreshResponse = wallet.refreshRequest(ctx).send();
        assertTrue(refreshResponse.isSuccess(), "Refresh token exchange should succeed");

        // Verify refreshed access token also has correct audience
        String accessToken2 = refreshResponse.getAccessToken();
        wallet.assertAccessTokenAudience(accessToken2, expectedAudience);

        // Verify the refreshed token still works for credential requests
        credResponse = wallet.credentialRequest(ctx, accessToken2)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();
        assertSuccessfulCredentialResponse(credResponse);
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
        AccessTokenResponse tokenResponse = authzCodeFlow();
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
        AccessTokenResponse tokenResponse = authzCodeFlow();
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

    /**
     * Verify that OID4VCI refresh token rotation correctly rejects a replayed token
     * when revokeRefreshToken is enabled on the realm.
     *
     * This test targets a potential vulnerability where OID4VCIRefreshTokenProvider
     * creates a new transient session on each refresh exchange, causing the rotation
     * state (consumed token ID and reuse count) to be lost between requests.
     */
    @Test
    public void testRefreshTokenRotationRejectsReplayedToken() {
        testRealm.updateWithCleanup(r -> r.revokeRefreshToken(true).refreshTokenMaxReuse(0));

        AccessTokenResponse tokenResponse = authzCodeFlow();
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

        String accessToken = tokenResponse.getAccessToken();
        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();

        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();
        assertSuccessfulCredentialResponse(credResponse);

        // Save Token-A (the initial refresh token)
        String tokenA = tokenResponse.getRefreshToken();
        assertNotNull(tokenA, "Token-A (initial refresh token) should not be null");

        timeOffSet.set(10);

        // Step 2: Exchange Token-A → Token-B (first refresh — should succeed)
        AccessTokenResponse refreshResponse1 = wallet.refreshRequest(ctx).send();
        assertTrue(refreshResponse1.isSuccess(), "First refresh (Token-A → Token-B) should succeed");
        String tokenB = refreshResponse1.getRefreshToken();
        assertNotNull(tokenB, "Token-B (new refresh token) should not be null");

        timeOffSet.set(20);

        // Step 3: Replay Token-A — should be REJECTED because it was already consumed
        AccessTokenResponse replayResponse = oauth.doRefreshTokenRequest(tokenA);
        assertFalse(replayResponse.isSuccess(), "Replaying Token-A after rotation should fail — transient session must not lose rotation state");
        assertEquals(INVALID_GRANT, replayResponse.getError(), "Expected invalid_grant error for replayed refresh token");
    }

    /**
     * Verify that two refreshes of the same refresh token family cannot both be accepted when they run concurrently.
     *
     * The rotation state is a single record shared by the whole family, which every exchange reads and writes back.
     * The initial token keeps the session id of the authorization-code session, while the tokens rotated out of it
     * are minted off a transient session and carry none. Unless the refresh lock is keyed on the family rather than
     * on the session, the two take different locks, read the same record and overwrite each other's update —
     * forking one family into two live branches.
     */
    @Test
    public void testConcurrentRefreshWithinFamilyKeepsRotationStateConsistent() throws Exception {
        // A max reuse of one keeps the parent usable after it has been rotated, so parent and child are valid at once
        testRealm.updateWithCleanup(r -> r.revokeRefreshToken(true).refreshTokenMaxReuse(1));

        AccessTokenResponse tokenResponse = authzCodeFlow();
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");
        String parent = tokenResponse.getRefreshToken();

        // Rotate the family one step. The clock is advanced so the two tokens have distinct issued-at times
        timeOffSet.set(10);
        AccessTokenResponse rotation = oauth.doRefreshTokenRequest(parent);
        assertTrue(rotation.isSuccess(), "Rotating the parent token should succeed, but failed with " + rotation.getError());
        String child = rotation.getRefreshToken();

        timeOffSet.set(20);
        List<AccessTokenResponse> responses = refreshConcurrently(parent, child);

        int accepted = 0;
        for (AccessTokenResponse response : responses) {
            if (response.isSuccess()) {
                accepted++;
            } else {
                // Anything other than invalid_grant means the exchange broke rather than being refused, for
                // instance because the serialization lock could not be acquired
                assertEquals(INVALID_GRANT, response.getError(), "A refused concurrent refresh must fail with invalid_grant");
            }
        }

        // Accepting the parent mints a new child and moves the family's latest-generated marker onto it, which
        // leaves the child stale; accepting the child does the same to the parent. Serialized, exactly one of the
        // two can therefore be accepted, whichever order they arrive in. Two acceptances mean both read the same
        // rotation record and overwrote each other's update
        assertEquals(1, accepted, "Exactly one of two concurrent refreshes of the same family must be accepted");
    }

    /**
     * Sends one refresh request per token, all released together, and returns the responses in the order the tokens
     * were given.
     */
    private List<AccessTokenResponse> refreshConcurrently(String... refreshTokens) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(refreshTokens.length);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<AccessTokenResponse>> futures = new ArrayList<>(refreshTokens.length);
            for (String refreshToken : refreshTokens) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return oauth.doRefreshTokenRequest(refreshToken);
                }));
            }
            start.countDown();

            List<AccessTokenResponse> responses = new ArrayList<>(futures.size());
            for (Future<AccessTokenResponse> future : futures) {
                responses.add(future.get(30, TimeUnit.SECONDS));
            }
            return responses;
        } finally {
            executor.shutdownNow();
        }
    }

    protected AccessTokenResponse authzCodeFlow() {
        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .send(user.getUsername(), TEST_PASSWORD);
        return assertAndGetAccessTokenResponse(authResponse);
    }

    protected AccessTokenResponse authzCodeFlow(String issuerState) {
        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .issuerState(issuerState)
                .send(user.getUsername(), TEST_PASSWORD);
        return assertAndGetAccessTokenResponse(authResponse);
    }

    private AccessTokenResponse assertAndGetAccessTokenResponse(AuthorizationEndpointResponse authResponse) {
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

    private IssuedVerifiableCredentialRepresentation issueCredential() {
        AccessTokenResponse tokenResponse = authzCodeFlow();
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

        CredentialResponse credResponse = wallet.credentialRequest(ctx, tokenResponse.getAccessToken())
                .credentialIdentifier(ctx.getAuthorizedCredentialIdentifier())
                .send().getCredentialResponse();
        assertSuccessfulCredentialResponse(credResponse);

        List<IssuedVerifiableCredentialRepresentation> issuedCredentials = user.admin().verifiableCredentials().getIssuedCredentials();
        assertEquals(1, issuedCredentials.size(), "Single issued credential should be stored");
        return issuedCredentials.get(0);
    }

    private void assertRefreshFailsForStaleIssuedCredential() {
        AccessTokenResponse refreshResponse = wallet.refreshRequest(ctx).send();
        assertFalse(refreshResponse.isSuccess(), "Refresh token exchange should fail");
        assertNull(refreshResponse.getAccessToken());
        assertEquals(INVALID_REQUEST, refreshResponse.getError());
        assertEquals("Issued credential is stale", refreshResponse.getErrorDescription());
    }

    private void assertRefreshFailsForStaleToken() {
        AccessTokenResponse refreshResponse = wallet.refreshRequest(ctx).send();
        assertFalse(refreshResponse.isSuccess(), "Refresh token exchange should fail");
        assertNull(refreshResponse.getAccessToken());
        assertEquals(INVALID_GRANT, refreshResponse.getError());
        assertEquals("Stale token", refreshResponse.getErrorDescription());
    }

    private void assertIssuedCredentialRejectedAsStale(String issuedCredentialId) {
        String realmName = testRealm.getName();
        String userId = user.getId();
        String clientId = managedClient.getId();
        String credentialConfigurationId = ctx.getCredentialConfigurationId();

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel userModel = session.users().getUserById(realm, userId);
            ClientModel clientModel = realm.getClientById(clientId);
            CredentialScopeModel credentialScopeModel = CredentialScopeUtils.findCredentialScopeModelByConfigurationId(
                    realm, () -> clientModel.getClientScopes(false).values().stream(), credentialConfigurationId);

            IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                    OID4VCUtil.checkIssuedVerifiableCredential(session, userModel, issuedCredentialId, credentialScopeModel, clientModel));
            assertEquals("Issued credential is stale", exception.getMessage());
        });
    }

    private void resetUserNotBefore() {
        String realmName = testRealm.getName();
        String userId = user.getId();

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel userModel = session.users().getUserById(realm, userId);
            session.users().setNotBeforeForUser(realm, userModel, 0);
        });
    }
}
