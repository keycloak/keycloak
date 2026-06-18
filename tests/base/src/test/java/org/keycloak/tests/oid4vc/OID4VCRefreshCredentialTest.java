package org.keycloak.tests.oid4vc;

import java.util.List;

import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.representations.idm.oid4vc.IssuedVerifiableCredentialRepresentation;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_VCT;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @BeforeEach
    void beforeEach() {
        ctx = new OID4VCTestContext(client, minimalJwtTypeCredentialScope);
        user.admin().logout();
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
        OID4VCAuthorizationDetail authzDetailResponse1 = ctx.getAuthorizationDetail();
        assertEquals(issuedCred1.getId(), authzDetailResponse1.getIssuedCredentialId());

        // Move time a bit before refresh token
        timeOffSet.set(10);

        // Refresh token
        AccessTokenResponse refreshed = wallet.refreshRequest(ctx).send();
        assertEquals(200, refreshed.getStatusCode(), "Refresh token exchange should succeed");
        String accessToken2 = refreshed.getAccessToken();

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

        // Assert issued-credential ID matches and did not changed
        OID4VCAuthorizationDetail authzDetailResponse2 = ctx.getAuthorizationDetail();
        assertEquals(issuedCred2.getId(), authzDetailResponse2.getIssuedCredentialId());
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

}
