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
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.VCTestServerConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
