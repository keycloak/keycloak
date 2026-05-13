package org.keycloak.tests.oid4vc;

import java.net.URI;
import java.util.List;

import org.keycloak.TokenVerifier;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OfferResponseType;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferUriResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_VCT;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint.DEFAULT_CREDENTIAL_OFFER_LIFESPAN_S;
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
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfigRestCredentialOffer.class)
public class OID4VCredentialOfferAuthCodeTest extends OID4VCIssuerTestBase {

    @Test
    public void testAuthCodeOffer_Anonymous() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Create Authorization Code CredentialOffer
        //
        CredentialsOffer credOffer = wallet.createCredentialOffer(ctx, req -> {
            req.targetUser(null);
        });

        String issuerState = credOffer.getIssuerState();
        assertNotNull(issuerState, "No IssuerState");

        CredentialOfferURI offerURI = ctx.getCredentialsOfferUri();
        assertNotNull(offerURI, "No CredentialOfferURI");

        // Fetch credential offer again
        // https://github.com/keycloak/keycloak/issues/48014
        credOffer = wallet.credentialsOfferRequest(ctx, offerURI).send().getCredentialsOffer();
        issuerState = credOffer.getIssuerState();
        assertNotNull(issuerState, "No IssuerState");

        // Send AuthorizationRequest
        //
        AuthorizationEndpointResponse authResponse = wallet
                .authorizationRequest()
                .scope(ctx.getScope())
                .issuerState(issuerState)
                .send(ctx.getHolder(), TEST_PASSWORD);
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No authCode");

        // Build and send AccessTokenRequest
        //
        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode).send();
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

        // Attempt to fetch the credential offer again after it has been consumed

        CredentialOfferResponse res = wallet.credentialsOfferRequest(ctx, offerURI).send();
        assertEquals("invalid_credential_offer_request", res.getError());
        assertEquals("Credential offer not found or already consumed", res.getErrorDescription());
    }

    @Test
    public void testAuthCodeOffer_Anonymous_multipleOffers() throws Exception {

        var ctx1 = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Create Authorization Code CredentialOffer
        //
        CredentialsOffer credOffer1 = wallet.createCredentialOffer(ctx1, req -> {
            req.targetUser(null);
        });

        String issuerState = credOffer1.getIssuerState();
        assertNotNull(issuerState, "No IssuerState");

        // Send AuthorizationRequest
        //
        AuthorizationEndpointResponse authResponse1 = wallet
                .authorizationRequest()
                .scope(ctx1.getScope())
                .issuerState(issuerState)
                .send(ctx1.getHolder(), TEST_PASSWORD);
        String authCode = authResponse1.getCode();
        assertNotNull(authCode, "No authCode");

        // Build and send AccessTokenRequest
        //
        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx1, authCode).send();
        String accessToken1 = wallet.validateHolderAccessToken(ctx1, tokenResponse);
        assertNotNull(accessToken1, "No accessToken");

        String authorizedIdentifier1 = ctx1.getAuthorizedCredentialIdentifier();
        assertNotNull(authorizedIdentifier1, "Has authorized credential identifier");

        // Delete cookies to avoid automatic SSO login
        driver.cookies().deleteAll();
        oauth.loginForm().prompt(PROMPT_VALUE_LOGIN).open();
        driver.cookies().deleteAll();

        var ctx2 = new OID4VCTestContext(client, minimalJwtTypeCredentialScope);

        // Create Authorization Code CredentialOffer and obtain 2nd access-token for different VC
        //
        CredentialsOffer credOffer2 = wallet.createCredentialOffer(ctx2, req -> {
            req.targetUser(null);
        });

        issuerState = credOffer2.getIssuerState();
        assertNotNull(issuerState, "No IssuerState");

        // Send AuthorizationRequest
        //
        AuthorizationEndpointResponse authResponse2 = wallet
                .authorizationRequest()
                .scope(ctx2.getScope())
                .issuerState(issuerState)
                .send(ctx2.getHolder(), TEST_PASSWORD);
        authCode = authResponse2.getCode();
        assertNotNull(authCode, "No authCode");

        // Build and send AccessTokenRequest
        //
        AccessTokenResponse tokenResponse2 = wallet.accessTokenRequest(ctx2, authCode).send();
        String accessToken2 = wallet.validateHolderAccessToken(ctx2, tokenResponse2);
        assertNotNull(accessToken2, "No accessToken");

        String authorizedIdentifier2 = ctx2.getAuthorizedCredentialIdentifier();
        assertNotNull(authorizedIdentifier2, "Has authorized credential identifier");

        // Send the CredentialRequest1 with first access-token. Ensure credential successfully obtained
        //
        CredentialResponse credResponse = wallet.credentialRequest(ctx1, accessToken1)
                .credentialIdentifier(authorizedIdentifier1)
                .proofs(wallet.generateJwtProof(ctx1))
                .send().getCredentialResponse();

        verifyCredentialResponse(ctx1, ctx1.getHolder(), credResponse);

        // Send the CredentialRequest2 with 2nd access-token. Ensure credential successfully obtained for the correct VC type
        //
        CredentialResponse credResponse2 = wallet.credentialRequest(ctx2, accessToken2)
                .credentialIdentifier(authorizedIdentifier2)
                .send().getCredentialResponse();

        CredentialResponse.Credential credentialObj = credResponse2.getCredentials().get(0);
        assertNotNull(credentialObj, "The first credential in the array should not be null");
        IssuerSignedJWT issuerSignedJWT = SdJwtVP.of(credentialObj.getCredential().toString()).getIssuerSignedJWT();
        assertEquals(ctx2.getCredentialScope().getName(), issuerSignedJWT.getPayload().get(CLAIM_NAME_VCT).asText());

        assertNotEquals(ctx1.getCredentialScope().getName(), ctx2.getCredentialScope().getName());
    }

    @Test
    public void testAuthCodeOffer_Anonymous_expiredOffer() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Create Authorization Code CredentialOffer
        //
        CredentialsOffer credOffer = wallet.createCredentialOffer(ctx, req -> {
            req.targetUser(null);
        });

        String issuerState = credOffer.getIssuerState();
        assertNotNull(issuerState, "No IssuerState");

        // Send AuthorizationRequest
        //
        AuthorizationEndpointResponse authResponse = wallet
                .authorizationRequest()
                .scope(ctx.getScope())
                .issuerState(issuerState)
                .send(ctx.getHolder(), TEST_PASSWORD);
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No authCode");

        // Build and send AccessTokenRequest
        //
        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode).send();
        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken, "No accessToken");

        String authorizedIdentifier = ctx.getAuthorizedCredentialIdentifier();
        assertNotNull(authorizedIdentifier, "Has authorized credential identifier");

        // Move time forward to make sure offer is expired
        timeOffSet.set(DEFAULT_CREDENTIAL_OFFER_LIFESPAN_S + 10);

        // Send the CredentialRequest
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> wallet.credentialRequest(ctx, accessToken)
                        .credentialIdentifier(authorizedIdentifier)
                        .proofs(wallet.generateJwtProof(ctx))
                        .send().getCredentialResponse());
        assertTrue(error.getMessage().contains("Credential offer has already expired"), error.getMessage());
        timeOffSet.set(0);
    }

    @Test
    public void testAuthCodeOffer_Targeted() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Create Authorization Code CredentialOffer
        //
        CredentialsOffer credOffer = wallet.createCredentialOffer(ctx, req -> {
            req.targetUser(ctx.getHolder());
        });

        String issuerState = credOffer.getIssuerState();
        assertNotNull(issuerState, "No IssuerState");

        // Send AuthorizationRequest
        //
        AuthorizationEndpointResponse authResponse = wallet
                .authorizationRequest()
                .scope(ctx.getScope())
                .issuerState(issuerState)
                .send(ctx.getHolder(), TEST_PASSWORD);
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No authCode");

        // Build and send AccessTokenRequest
        //
        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode).send();
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
    }

    @Test
    public void testAuthCodeOffer_QRCode() {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        CredentialsOffer credOffer = wallet.createCredentialOffer(ctx, req -> {
            req.responseType(OfferResponseType.URI_QR);
        });

        String issuerState = credOffer.getIssuerState();
        assertNotNull(issuerState, "No IssuerState");

        CredentialOfferURI credOfferURI = ctx.getCredentialsOfferUriResponse().getCredentialOfferURI();
        assertNotNull(credOfferURI, "No CredentialOfferURI");
        assertNotNull(credOfferURI.getQrCode(), "No QR Code");
    }

    @Test
    public void testAuthCodeOffer_QRCode_InvalidDimensions() {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        var ex = assertThrows(IllegalStateException.class, () -> wallet.createCredentialOfferUri(ctx, req -> {
            req.responseType(OfferResponseType.URI_QR);
            req.width(1000).height(1000);
        }));
        CredentialOfferUriResponse res = ctx.getCredentialsOfferUriResponse();

        String error = res.getError();
        assertNotNull(error, "No Error");

        String errorDescription = res.getErrorDescription();
        assertNotNull(errorDescription, "No ErrorDescription");

        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        assertEquals("invalid_credential_offer_request", error);
        assertEquals("Requested QR Code too large, allowed maximum is 800x800", errorDescription);
        assertEquals(String.format("[%s] %s", error, errorDescription), ex.getMessage());
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
