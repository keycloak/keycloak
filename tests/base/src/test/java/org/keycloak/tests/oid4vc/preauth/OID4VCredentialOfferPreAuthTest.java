package org.keycloak.tests.oid4vc.preauth;

import java.net.URI;
import java.util.List;

import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Test;

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
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerWithPreAuthCodeEnabled.class)
public class OID4VCredentialOfferPreAuthTest extends OID4VCIssuerTestBase {

    @Test
    public void testPreAuthOffer_DisabledUser() {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Disable user
        UserRepresentation userRep = testRealm.admin().users().search(ctx.getHolder()).get(0);
        UserResource userResource = testRealm.admin().users().get(userRep.getId());
        userRep.setEnabled(false);
        userResource.update(userRep);

        try {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> wallet.createCredentialOffer(ctx, req -> {
                        req.targetUser(ctx.getHolder());
                        req.preAuthorized(true);
                    }));
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
        CredentialsOffer credOffer = wallet.createCredentialOffer(ctx, req -> {
            req.preAuthorized(true);
            req.targetUser(null);
        });

        String preAuthCode = credOffer.getPreAuthorizedCode();

        // Redeem Pre-Authorized Code for AccessToken
        //
        AccessTokenResponse tokenResponse = wallet.accessTokenRequestPreAuth(ctx, preAuthCode).send();
        assertTrue(tokenResponse.isSuccess(), tokenResponse.getErrorDescription());

        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken, "No accessToken");

        String authorizedIdentifier = ctx.getAuthorizedCredentialIdentifier();
        assertNotNull(authorizedIdentifier, "No authorized credential identifier");

        // Send the CredentialRequest
        //
        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(authorizedIdentifier)
                .proofs(wallet.generateJwtProof(ctx))
                .send().getCredentialResponse();

        verifyCredentialResponse(ctx, ctx.getIssuer(), credResponse);
    }

    @Test
    public void testPreAuthOffer_Targeted() throws Exception {
        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Create Pre-Authorized CredentialOffer
        //
        CredentialsOffer credOffer = wallet.createCredentialOffer(ctx, req -> {
            req.targetUser(ctx.getHolder());
            req.preAuthorized(true);
        });

        String preAuthCode = credOffer.getPreAuthorizedCode();
        assertNotNull(preAuthCode, "preAuthCode");

        CredentialOfferURI offerURI = ctx.getCredentialsOfferUri();
        assertNotNull(offerURI, "No CredentialOfferURI");

        // Fetch credential offer again
        // https://github.com/keycloak/keycloak/issues/48014
        credOffer = wallet.credentialsOfferRequest(ctx, offerURI).send().getCredentialsOffer();
        preAuthCode = credOffer.getPreAuthorizedCode();
        assertNotNull(preAuthCode, "preAuthCode");

        // Redeem Pre-Authorized Code for AccessToken
        //
        AccessTokenResponse tokenResponse = wallet.accessTokenRequestPreAuth(ctx, preAuthCode).send();
        assertTrue(tokenResponse.isSuccess(), tokenResponse.getErrorDescription());

        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken, "No accessToken");

        String authorizedIdentifier = ctx.getAuthorizedCredentialIdentifier();
        assertNotNull(authorizedIdentifier, "Has authorized credential identifier");

        // Send the CredentialRequest
        //
        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(authorizedIdentifier)
                .proofs(wallet.generateJwtProof(ctx))
                .send().getCredentialResponse();

        verifyCredentialResponse(ctx, ctx.getHolder(), credResponse);

        // Offer URI can be fetched again in the current implementation as long as offer state is still valid.
        CredentialsOffer replayedOffer = wallet.credentialsOfferRequest(ctx, offerURI).send().getCredentialsOffer();
        assertNotNull(replayedOffer, "Credential offer should still be available");
        assertNotNull(replayedOffer.getPreAuthorizedCode(), "Pre-authorized code should still be present");
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private void verifyCredentialResponse(OID4VCTestContext ctx, String expUser, CredentialResponse credResponse) throws Exception {

        String scope = ctx.getCredentialScope().getName();
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
