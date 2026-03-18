package org.keycloak.tests.oid4vc;

import java.net.URI;
import java.util.List;

import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.VCTestServerConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_VCT;
import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint.DEFAULT_CODE_LIFESPAN_S;
import static org.keycloak.protocol.oidc.OIDCLoginProtocol.PROMPT_VALUE_LOGIN;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Credential Offer Validity Matrix
 * <p>
 * +----------+----------+---------+------------------------------------------------------+
 * | Pre-Auth | Username | Valid   | Notes                                                |
 * +----------+----------+---------+------------------------------------------------------+
 * | no       | no       | yes     | Anonymous offer; any logged-in user may redeem.      |
 * | no       | yes      | yes     | Offer restricted to a specific user.                 |
 * +----------+----------+---------+------------------------------------------------------+
 * | yes      | no       | no      | Pre-auth requires a target user.                     |
 * | yes      | yes      | yes     | Pre-auth for a specific target user.                 |
 * +----------+----------+---------+------------------------------------------------------+
 */
@KeycloakIntegrationTest(config = VCTestServerConfig.class)
public class OID4VCCredentialOfferMatrixTest extends OID4VCIssuerTestBase {

    OID4VCBasicWallet wallet;

    @BeforeEach
    void beforeEach() {
        wallet = new OID4VCBasicWallet(keycloak, oauth);
    }

    @AfterEach
    void afterEach() {
        wallet.logout();
    }

    @Test
    public void testRealmSetup() {
        RealmRepresentation realmRep = testRealm.admin().toRepresentation();
        assertEquals(shouldEnableOid4vci(realmRep), realmRep.isVerifiableCredentialsEnabled());
        assertEquals(shouldEnableOid4vci(client), isOid4vciEnabled(client));
    }

    @Test
    public void testWithoutOffer_Scope() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Send AuthorizationRequest
        //
        AuthorizationEndpointResponse authResponse = wallet
                .authorizationRequest()
                .scope(ctx.credScopeName)
                .send(ctx.holder, "password");
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No authCode");

        // Build and send AccessTokenRequest
        //
        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode).send();
        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken, "No accessToken");

        String authorizedIdentifier = ctx.getAuthorizedCredentialIdentifier();
        assertNotNull(authorizedIdentifier,"No authorized credential identifier");

        // Send the CredentialRequest
        //
        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(authorizedIdentifier)
                .send().getCredentialResponse();

        verifyCredentialResponse(ctx, ctx.holder, credResponse);
    }

    @Test
    public void testWithoutOffer_Scope_AuthDetails() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        CredentialIssuer issuerMetadata = wallet.getIssuerMetadata(ctx);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(ctx.credConfigId);
        authDetail.setLocations(List.of(issuerMetadata.getCredentialIssuer()));

        // Send AuthorizationRequest
        //
        AuthorizationEndpointResponse authResponse = wallet
                .authorizationRequest()
                .scope(ctx.credScopeName)
                .authorizationDetails(authDetail)
                .send(ctx.holder, "password");
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No authCode");

        // Build and send AccessTokenRequest
        //
        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode).send();
        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken, "No accessToken");

        String authorizedIdentifier = ctx.getAuthorizedCredentialIdentifier();
        assertNotNull(authorizedIdentifier,"No authorized credential identifier");

        // Send the CredentialRequest
        //
        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(authorizedIdentifier)
                .send().getCredentialResponse();

        verifyCredentialResponse(ctx, ctx.holder, credResponse);
    }

    @Test
    public void testAuthCodeOffer_Anonymous() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Create Authorization Code CredentialOffer
        //
        CredentialsOffer credOffer = wallet.createAuthCodeCredentialOffer(ctx, null);
        String issuerState = credOffer.getIssuerState();
        assertNotNull(issuerState, "No IssuerState");

        // Send AuthorizationRequest
        //
        AuthorizationEndpointResponse authResponse = wallet
                .authorizationRequest()
                .scope(ctx.credScopeName)
                .issuerState(issuerState)
                .send(ctx.holder, "password");
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No authCode");

        // Build and send AccessTokenRequest
        //
        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode).send();
        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken, "No accessToken");

        String authorizedIdentifier = ctx.getAuthorizedCredentialIdentifier();
        assertNotNull(authorizedIdentifier, "No authorized credential identifier");

        // Send the CredentialRequest
        //
        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(authorizedIdentifier)
                .send().getCredentialResponse();

        verifyCredentialResponse(ctx, ctx.holder, credResponse);
    }

    @Test
    public void testAuthCodeOffer_Anonymous_multipleOffers() throws Exception {

        var ctx1 = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Create Authorization Code CredentialOffer
        //
        CredentialsOffer credOffer = wallet.createAuthCodeCredentialOffer(ctx1, null);
        String issuerState = credOffer.getIssuerState();
        assertNotNull(issuerState, "No IssuerState");

        // Send AuthorizationRequest
        //
        AuthorizationEndpointResponse authResponse1 = wallet
                .authorizationRequest()
                .scope(ctx1.credScopeName)
                .issuerState(issuerState)
                .send(ctx1.holder, "password");
        String authCode = authResponse1.getCode();
        assertNotNull(authCode, "No authCode");

        // Build and send AccessTokenRequest
        //
        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx1, authCode).send();
        String accessToken1 = wallet.validateHolderAccessToken(ctx1, tokenResponse);
        assertNotNull(accessToken1, "No accessToken");

        String authorizedIdentifier1 = ctx1.getAuthorizedCredentialIdentifier();
        assertNotNull(authorizedIdentifier1, "No authorized credential identifier");

        // Delete cookies to avoid automatic SSO login
        driver.cookies().deleteAll();
        oauth.loginForm().prompt(PROMPT_VALUE_LOGIN).open();
        driver.cookies().deleteAll();

        var ctx2 = new OID4VCTestContext(client, minimalJwtTypeCredentialScope);

        // Create Authorization Code CredentialOffer and obtain 2nd access-token for different VC
        //
        CredentialsOffer credOffer2 = wallet.createAuthCodeCredentialOffer(ctx2, null);
        issuerState = credOffer2.getIssuerState();
        assertNotNull(issuerState, "No IssuerState");

        // Send AuthorizationRequest
        //
        AuthorizationEndpointResponse authResponse2 = wallet
                .authorizationRequest()
                .scope(ctx2.credScopeName)
                .issuerState(issuerState)
                .send(ctx2.holder, "password");
        authCode = authResponse2.getCode();
        assertNotNull(authCode, "No authCode");

        // Build and send AccessTokenRequest
        //
        AccessTokenResponse tokenResponse2 = wallet.accessTokenRequest(ctx2, authCode).send();
        String accessToken2 = wallet.validateHolderAccessToken(ctx2, tokenResponse2);
        assertNotNull(accessToken2, "No accessToken");

        String authorizedCredConfigId2 = ctx2.getAuthorizedCredentialConfigurationId();
        assertNotNull(authorizedCredConfigId2, "No authorized credential identifier");

        // Send the CredentialRequest1 with first access-token. Ensure credential successfully obtained
        //
        CredentialResponse credResponse = wallet.credentialRequest(ctx1, accessToken1)
                .credentialIdentifier(authorizedIdentifier1)
                .send().getCredentialResponse();

        verifyCredentialResponse(ctx1, ctx1.holder, credResponse);

        // Send the CredentialRequest2 with 2nd access-token. Ensure credential successfully obtained for the correct VC type
        //
        CredentialResponse credResponse2 = wallet.credentialRequest(ctx2, accessToken2)
                .credentialConfigurationId(authorizedCredConfigId2)
                .send().getCredentialResponse();

        CredentialResponse.Credential credentialObj = credResponse2.getCredentials().get(0);
        assertNotNull(credentialObj, "The first credential in the array should not be null");
        IssuerSignedJWT issuerSignedJWT = SdJwtVP.of(credentialObj.getCredential().toString()).getIssuerSignedJWT();
        assertEquals(ctx2.credentialScope.getName(), issuerSignedJWT.getPayload().get(CLAIM_NAME_VCT).asText());

        assertNotEquals(ctx1.credentialScope.getName(), ctx2.credentialScope.getName());
    }

    @Test
    public void testAuthCodeOffer_Anonymous_expiredOffer() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Create Authorization Code CredentialOffer
        //
        CredentialsOffer credOffer = wallet.createAuthCodeCredentialOffer(ctx, null);
        String issuerState = credOffer.getIssuerState();
        assertNotNull(issuerState, "No IssuerState");

        // Send AuthorizationRequest
        //
        AuthorizationEndpointResponse authResponse = wallet
                .authorizationRequest()
                .scope(ctx.credScopeName)
                .issuerState(issuerState)
                .send(ctx.holder, "password");
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No authCode");

        // Build and send AccessTokenRequest
        //
        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode).send();
        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken, "No accessToken");

        String authorizedIdentifier = ctx.getAuthorizedCredentialIdentifier();
        assertNotNull(authorizedIdentifier, "No authorized credential identifier");

        // Move time forward to make sure offer is expired
        timeOffSet.set(DEFAULT_CODE_LIFESPAN_S + 10);

        // Send the CredentialRequest
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> wallet.credentialRequest(ctx, accessToken)
                        .credentialIdentifier(authorizedIdentifier)
                        .send().getCredentialResponse());
        assertTrue(error.getMessage().contains("Credential offer has already expired"), error.getMessage());
        timeOffSet.set(0);
    }

    @Test
    public void testAuthCodeOffer_Targeted() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Create Authorization Code CredentialOffer
        //
        CredentialsOffer credOffer = wallet.createAuthCodeCredentialOffer(ctx, ctx.holder);
        String issuerState = credOffer.getIssuerState();
        assertNotNull(issuerState, "No IssuerState");

        // Send AuthorizationRequest
        //
        AuthorizationEndpointResponse authResponse = wallet
                .authorizationRequest()
                .scope(ctx.credScopeName)
                .issuerState(issuerState)
                .send(ctx.holder, "password");
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No authCode");

        // Build and send AccessTokenRequest
        //
        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode).send();
        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken, "No accessToken");

        String authorizedIdentifier = ctx.getAuthorizedCredentialIdentifier();
        assertNotNull(authorizedIdentifier, "No authorized credential identifier");

        // Send the CredentialRequest
        //
        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(authorizedIdentifier)
                .send().getCredentialResponse();

        verifyCredentialResponse(ctx, ctx.holder, credResponse);
    }

    @Test
    public void testPreAuthOffer_DisabledUser() {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Disable user
        UserRepresentation userRep = testRealm.admin().users().search(ctx.holder).get(0);
        UserResource userResource = testRealm.admin().users().get(userRep.getId());
        userRep.setEnabled(false);
        userResource.update(userRep);

        try {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> wallet.createPreAuthCredentialOffer(ctx, ctx.holder));
            assertTrue(error.getMessage().contains("User 'alice' disabled"), error.getMessage());
        } finally {
            userRep.setEnabled(true);
            userResource.update(userRep);
        }
    }

    @Test
    public void testPreAuthOffer_SelfIssued() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Create Pre-Authorized CredentialOffer
        //
        CredentialsOffer credOffer = wallet.createPreAuthCredentialOffer(ctx, null);
        String preAuthCode = credOffer.getPreAuthorizedCode();

        // Redeem Pre-Authorized Code for AccessToken
        //
        AccessTokenResponse tokenResponse = wallet.preAuthAccessTokenRequest(ctx, preAuthCode).send();
        assertTrue(tokenResponse.isSuccess(), tokenResponse.getErrorDescription());

        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken,"No accessToken");

        String authorizedIdentifier = ctx.getAuthorizedCredentialIdentifier();
        assertNotNull(authorizedIdentifier,"No authorized credential identifier");

        // Send the CredentialRequest
        //
        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(authorizedIdentifier)
                .send().getCredentialResponse();

        verifyCredentialResponse(ctx, ctx.issuer, credResponse);
    }

    @Test
    public void testPreAuthOffer_Targeted() throws Exception {
        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Create Pre-Authorized CredentialOffer
        //
        CredentialsOffer credOffer = wallet.createPreAuthCredentialOffer(ctx, ctx.holder);
        String preAuthCode = credOffer.getPreAuthorizedCode();

        // Redeem Pre-Authorized Code for AccessToken
        //
        AccessTokenResponse tokenResponse = wallet.preAuthAccessTokenRequest(ctx, preAuthCode).send();
        assertTrue(tokenResponse.isSuccess(), tokenResponse.getErrorDescription());

        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken, "No accessToken");

        String authorizedIdentifier = ctx.getAuthorizedCredentialIdentifier();
        assertNotNull(authorizedIdentifier, "No authorized credential identifier");

        // Send the CredentialRequest
        //
        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(authorizedIdentifier)
                .send().getCredentialResponse();

        verifyCredentialResponse(ctx, ctx.holder, credResponse);
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private void verifyCredentialResponse(OID4VCTestContext ctx, String expUser, CredentialResponse credResponse) throws Exception {

        String scope = ctx.credentialScope.getName();
        CredentialResponse.Credential credentialObj = credResponse.getCredentials().get(0);
        assertNotNull(credentialObj, "The first credential in the array should not be null");

        JsonWebToken jsonWebToken = TokenVerifier.create((String) credentialObj.getCredential(), JsonWebToken.class).getToken();
        assertEquals("did:web:test.org", jsonWebToken.getIssuer());
        Object vc = jsonWebToken.getOtherClaims().get("vc");
        VerifiableCredential credential = JsonSerialization.mapper.convertValue(vc, VerifiableCredential.class);
        assertEquals(List.of(scope), credential.getType());
        assertEquals(URI.create("did:web:test.org"), credential.getIssuer());
        assertEquals(expUser + "@email.cz", credential.getCredentialSubject().getClaims().get("email"));
    }
}
