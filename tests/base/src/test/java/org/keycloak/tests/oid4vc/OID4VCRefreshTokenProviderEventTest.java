package org.keycloak.tests.oid4vc;

import java.util.List;

import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oid4vc.refresh.OID4VCIRefreshTokenProviderFactory;
import org.keycloak.protocol.oidc.refresh.DefaultRefreshTokenProviderFactory;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCRefreshTokenProviderEventTest extends OID4VCIssuerTestBase {

    @InjectUser(config = OID4VCActionTest.OID4VCTestUserConfig.class)
    ManagedUser user;

    OID4VCTestContext ctx;

    @TestSetup
    public void configureTestRealm() {
        super.configureTestRealm();
        var realmRep = testRealm.admin().toRepresentation();
        realmRep.setEnabledEventTypes(List.of(
                EventType.CODE_TO_TOKEN.toString(),
                EventType.REVOKE_GRANT.toString(),
                EventType.REFRESH_TOKEN.toString()));
        testRealm.admin().update(realmRep);
    }

    @BeforeEach
    void beforeEach() {
        ctx = new OID4VCTestContext(client, minimalJwtTypeCredentialScope);
        user.admin().logout();
        user.admin().verifiableCredentials().getIssuedCredentials()
                .forEach(issuedCred -> user.admin().verifiableCredentials().revokeIssuedCredential(issuedCred.getId()));
    }

    @AfterEach
    public void afterEach() {
        timeOffSet.set(0);
    }

    /**
     *   Verify that Refresh token must carry Details.REFRESH_TOKEN_PROVIDER_ID with value "oid4vci"
     *   when the token was issued by OID4VCIRefreshTokenProvider.
     */
    @Test
    public void testRefreshTokenEventCarriesOid4vciProviderId() {
        AccessTokenResponse tokenResponse = authzCodeFlow();
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

        String accessToken = tokenResponse.getAccessToken();
        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();

        wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();

        timeOffSet.set(5);

        events.clear();

        AccessTokenResponse refreshResponse = wallet.refreshRequest(ctx).send();
        assertTrue(refreshResponse.isSuccess(), "Refresh must succeed");

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.REFRESH_TOKEN)
                .details(Details.REFRESH_TOKEN_PROVIDER_ID,
                        OID4VCIRefreshTokenProviderFactory.PROVIDER_ID);
    }

    /**
     *   A standard token must carry provider id "default"
     *   when the token was issued by DefaultRefreshTokenProvider.
     */
    @Test
    public void testStandardRefreshTokenEventCarriesDefaultProviderId() {
        // Perform a plain OIDC auth-code flow (no OID4VCI scope)
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), user.getPassword());
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

        String refreshToken = tokenResponse.getRefreshToken();
        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshToken);
        assertTrue(refreshResponse.isSuccess(), "Standard refresh must succeed");

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.REFRESH_TOKEN)
                .details(Details.REFRESH_TOKEN_PROVIDER_ID,
                        DefaultRefreshTokenProviderFactory.PROVIDER_ID);
    }

    /**
     *  Verify that Revoke grant event must carry
     *  Details.REFRESH_TOKEN_PROVIDER_ID with value "oid4vci"
     */
    @Test
    public void testRevokeGrantEventCarriesOid4vciProviderId() {
        ctx = new OID4VCTestContext(client, minimalJwtTypeCredentialScope);
        user.admin().logout();

        AccessTokenResponse tokenResponse = authzCodeFlow();
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

        String accessToken = tokenResponse.getAccessToken();
        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();

        wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();

        events.clear();

        String refreshToken = tokenResponse.getRefreshToken();
        oauth.doTokenRevoke(refreshToken);

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.REVOKE_GRANT)
                .details(Details.REFRESH_TOKEN_PROVIDER_ID,
                        OID4VCIRefreshTokenProviderFactory.PROVIDER_ID);
    }

    /**
     *  CODE exchange event must carry refresh_token_provider_id = "oid4vci"
     */
    @Test
    public void testAuthzCodeExchangeEventCarriesOid4vciProviderId() {
        ctx = new OID4VCTestContext(client, minimalJwtTypeCredentialScope);
        user.admin().logout();

        AccessTokenResponse tokenResponse = authzCodeFlow();
        assertTrue(tokenResponse.isSuccess());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.CODE_TO_TOKEN)
                .details(Details.REFRESH_TOKEN_PROVIDER_ID,
                        OID4VCIRefreshTokenProviderFactory.PROVIDER_ID);
    }

    private AccessTokenResponse authzCodeFlow() {
        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .send(user.getUsername(), TEST_PASSWORD);
        String code = authResponse.getCode();
        assertNotNull(code, "Authorization code should not be null");

        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, code).send();
        assertNotNull(tokenResponse, "Token response should not be null");

        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken, "No accessToken");
        return tokenResponse;
    }
}
