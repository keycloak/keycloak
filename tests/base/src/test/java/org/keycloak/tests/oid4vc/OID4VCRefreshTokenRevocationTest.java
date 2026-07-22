package org.keycloak.tests.oid4vc;

import java.util.List;

import org.keycloak.OAuthErrorException;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.representations.idm.oid4vc.IssuedVerifiableCredentialRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.TokenRevocationResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialIssuerMetadataResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCDefaultServerConfig.class)
public class OID4VCRefreshTokenRevocationTest extends OID4VCIssuerTestBase {

    @InjectUser(config = OID4VCActionTest.OID4VCTestUserConfig.class)
    ManagedUser user;

    OID4VCTestContext ctx;

    @TestSetup
    public void configureTestRealm() {
        super.configureTestRealm();
        var realmRep = testRealm.admin().toRepresentation();
        realmRep.setEnabledEventTypes(List.of(
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

    /**
     *    Test for revoking an OID4VCI refresh token, which MUST delete the linked IssuedVerifiableCredentialEntity row
     */
    @Test
    public void testRevokeRefreshTokenRemovesIssuedCredential() {
        AccessTokenResponse tokenResponse = authzCodeFlow();
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

        // Obtain credential
        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();
        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();

        wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();

        List<IssuedVerifiableCredentialRepresentation> before = testRealm.admin().users().get(user.getId()).verifiableCredentials().getIssuedCredentials();
        assertEquals(1, before.size(), "Single issued credential should be stored");

        TokenRevocationResponse response = oauth.doTokenRevoke(refreshToken);
        assertEquals(200, response.getStatusCode(), "Expected 200 OK response");

        List<IssuedVerifiableCredentialRepresentation> after = testRealm.admin().users().get(user.getId()).verifiableCredentials().getIssuedCredentials();
        assertEquals(0, after.size(), "IssuedVerifiableCredential row must be deleted when OID4VCI refresh token is revoked");
    }

    /**
     *   Revoking an OID4VCI access token MUST block subsequent credential endpoint requests
     *   that carry that same access token.
     */
    @Test
    public void testRevokeAccessTokenBlocksCredentialEndpoint() {
        AccessTokenResponse tokenResponse = authzCodeFlow();
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

        // Obtain credential
        String accessToken = tokenResponse.getAccessToken();
        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();

        wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();

        //Revoke the access token
        TokenRevocationResponse response = oauth.doTokenRevoke(accessToken);
        assertEquals(200, response.getStatusCode(), "Expected 200 OK response");

        //Second credential request with revoked access token must fail
        String cNonce = oauth.oid4vc().nonceRequest().send().getNonce();
        Proofs proofs = OID4VCProofTestUtils.jwtProofs(
                getCredentialIssuerMetadata().getCredentialIssuer(), cNonce);
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc()
                .credentialRequest()
                .bearerToken(accessToken)
                .credentialIdentifier(credentialIdentifier)
                .proofs(proofs)
                .send();

        assertEquals(400, credentialResponse.getStatusCode(), "Credential request with revoked access token must be rejected");
        assertEquals(ErrorType.INVALID_TOKEN.getValue(), credentialResponse.getError(), "Error must be invalid_token");
    }

    /**
     *  Test checks idempotency of token revocation according to RFC 7009 (OAuth 2.0 Token Revocation)
     */
    @Test
    public void testRevokeRefreshTokenIsIdempotent() {
        AccessTokenResponse tokenResponse = authzCodeFlow();
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

        // Obtain credential
        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();
        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();

        wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();

        //first invocation
        TokenRevocationResponse revocationResponse = oauth.doTokenRevoke(refreshToken);
        assertEquals(200, revocationResponse.getStatusCode(), "Expected 200 OK response");

        //second invocation
        TokenRevocationResponse response = oauth.doTokenRevoke(refreshToken);
        assertEquals(200, response.getStatusCode(), "Expected 200 OK response");
    }

    /**
     *   OIDC Regression, Revoking a standard OIDC refresh token MUST
     *   still invalidate the user session
     */
    @Test
    public void testRevokeStandardOIDCRefreshTokenRevokesSession() {
        AccessTokenResponse response = oauth.doPasswordGrantRequest(user.getUsername(), user.getPassword());
        assertTrue(response.isSuccess(), "Access token exchange should succeed");
        String oidcRefreshToken = response.getRefreshToken();

        TokenRevocationResponse revocationResponse = oauth.doTokenRevoke(oidcRefreshToken);
        assertEquals(200, revocationResponse.getStatusCode(), "Expected 200 OK response");

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(oidcRefreshToken);

        assertTrue(OAuthErrorException.INVALID_GRANT.equals(refreshResponse.getError())
                        || OAuthErrorException.INVALID_TOKEN.equals(refreshResponse.getError()),
                "Refresh with revoked token must fail: " + refreshResponse.getError());
    }

    /**
     *  Test for revoking OID4VCI refresh token, which MUST perform BOTH effects:
     *      (a) OID4VCI-specific: the Issued Verifiable Credential row is deleted
     *      (b) OIDC baseline: the refresh token is no longer usable
     */
    @Test
    public void testAdditiveRevocationSemantics() {
        // Test for Part a
        AccessTokenResponse tokenResponse = authzCodeFlow();
        assertTrue(tokenResponse.isSuccess(), "Access token exchange should succeed");

        // Obtain credential
        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();
        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();

        wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();

        List<IssuedVerifiableCredentialRepresentation> before = testRealm.admin().users().get(user.getId()).verifiableCredentials().getIssuedCredentials();
        assertEquals(1, before.size(), "Single issued credential should be stored");

        TokenRevocationResponse response = oauth.doTokenRevoke(refreshToken);
        assertEquals(200, response.getStatusCode(), "Expected 200 OK response");

        List<IssuedVerifiableCredentialRepresentation> after = testRealm.admin().users().get(user.getId()).verifiableCredentials().getIssuedCredentials();
        assertEquals(0, after.size(), "IssuedVerifiableCredential row must be deleted when OID4VCI refresh token is revoked");

        // Test for part B
        AccessTokenResponse refreshTokenResponse = oauth.doRefreshTokenRequest(refreshToken);
        assertTrue(OAuthErrorException.INVALID_GRANT.equals(refreshTokenResponse.getError())
                        || OAuthErrorException.INVALID_TOKEN.equals(refreshTokenResponse.getError())
                        || OAuthErrorException.INVALID_REQUEST.equals(refreshTokenResponse.getError()),
                "Refresh with revoked token must fail: " + refreshTokenResponse.getError());
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

    protected CredentialIssuer getCredentialIssuerMetadata() {
        CredentialIssuerMetadataResponse metadataResponse = oauth.oid4vc().doIssuerMetadataRequest();
        return metadataResponse.getMetadata();
    }
}
