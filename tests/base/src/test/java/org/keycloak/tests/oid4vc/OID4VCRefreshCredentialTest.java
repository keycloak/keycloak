package org.keycloak.tests.oid4vc;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_VCT;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
