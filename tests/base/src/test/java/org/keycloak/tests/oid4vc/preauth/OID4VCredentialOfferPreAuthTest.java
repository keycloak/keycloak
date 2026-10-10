package org.keycloak.tests.oid4vc.preauth;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.Time;
import org.keycloak.protocol.oid4vc.model.CredentialDefinition;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.CredentialOfferStateUtils.CredentialOfferStateRecord;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferUriResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.keycloak.constants.OID4VCIConstants.CREDENTIAL_OFFER_CREATE;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint.CREDENTIAL_OFFER_LIFESPAN_REALM_ATTRIBUTE_KEY;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint.DEFAULT_CREDENTIAL_OFFER_LIFESPAN_S;
import static org.keycloak.tests.oid4vc.CredentialOfferStateUtils.getCredentialOfferStateRecord;
import static org.keycloak.tests.oid4vc.CredentialOfferStateUtils.injectTxCode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    /**
     * Verify that a client role with the same name as the realm role
     * {@code credential-offer-create} does NOT authorize targeted credential offers.
     * This prevents role namespace confusion attacks.
     *
     * @see <a href="https://github.com/keycloak/keycloak/issues/52665">#52665</a>
     */
    @Test
    public void testClientRoleNameCollisionCannotAuthorizeTargetedOffer() {
        UserRepresentation issuer = testRealm.admin().users().search(TEST_USER).get(0);
        UserResource issuerResource = testRealm.admin().users().get(issuer.getId());
        RoleRepresentation realmOfferRole = testRealm.admin().roles()
                .get(CREDENTIAL_OFFER_CREATE.getName()).toRepresentation();

        // Create a client role with the same name as the realm role
        RoleRepresentation clientOfferRole = new RoleRepresentation();
        clientOfferRole.setName(CREDENTIAL_OFFER_CREATE.getName());
        clientOfferRole.setDescription("Unrelated application role with a colliding name");

        // Remove realm role from issuer, add same-named client role
        issuerResource.roles().realmLevel().remove(List.of(realmOfferRole));
        testRealm.admin().clients().get(client.getId()).roles().create(clientOfferRole);
        clientOfferRole = testRealm.admin().clients().get(client.getId()).roles()
                .get(CREDENTIAL_OFFER_CREATE.getName()).toRepresentation();
        issuerResource.roles().clientLevel(client.getId()).add(List.of(clientOfferRole));

        try {
            String token = getBearerToken(oauth, client);
            assertThrows(IllegalStateException.class, () -> oauth.oid4vc()
                            .credentialOfferUriRequest(jwtTypeCredentialScope.getCredentialConfigurationId())
                            .preAuthorized(true)
                            .targetUser("alice")
                            .bearerToken(token)
                            .send()
                            .getCredentialOfferURI(),
                    "A same-named client role must not satisfy the realm credential-offer privilege");
        } finally {
            wallet.logout(TEST_USER);
            issuerResource.roles().clientLevel(client.getId()).remove(List.of(clientOfferRole));
            testRealm.admin().clients().get(client.getId()).roles()
                    .deleteRole(CREDENTIAL_OFFER_CREATE.getName());
            issuerResource.roles().realmLevel().add(List.of(realmOfferRole));
        }
    }

    /**
     * Verify that a composite realm role containing {@code credential-offer-create}
     * correctly authorizes targeted credential offers via {@code hasRole()} traversal.
     *
     * @see <a href="https://github.com/keycloak/keycloak/issues/52665">#52665</a>
     */
    @Test
    public void testCompositeRealmRoleAuthorizesTargetedOffer() throws Exception {
        UserRepresentation issuer = testRealm.admin().users().search(TEST_USER).get(0);
        UserResource issuerResource = testRealm.admin().users().get(issuer.getId());
        RoleRepresentation realmOfferRole = testRealm.admin().roles()
                .get(CREDENTIAL_OFFER_CREATE.getName()).toRepresentation();

        // Create a composite realm role containing the credential-offer-create role
        RoleRepresentation delegatedOfferRole = new RoleRepresentation();
        delegatedOfferRole.setName("credential-offer-delegator");
        testRealm.admin().roles().create(delegatedOfferRole);
        delegatedOfferRole = testRealm.admin().roles().get(delegatedOfferRole.getName()).toRepresentation();
        testRealm.admin().roles().get(delegatedOfferRole.getName()).addComposites(List.of(realmOfferRole));

        // Replace direct role with composite role
        issuerResource.roles().realmLevel().remove(List.of(realmOfferRole));
        issuerResource.roles().realmLevel().add(List.of(delegatedOfferRole));

        try {
            var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);
            CredentialsOffer credOffer = wallet.createCredentialOffer(ctx, req -> {
                req.targetUser(ctx.getHolder());
                req.preAuthorized(true);
            });
            assertNotNull(credOffer.getPreAuthorizedCode(),
                    "Composite realm role should authorize targeted credential offer creation");
        } finally {
            issuerResource.roles().realmLevel().remove(List.of(delegatedOfferRole));
            testRealm.admin().roles().deleteRole(delegatedOfferRole.getName());
            issuerResource.roles().realmLevel().add(List.of(realmOfferRole));
        }
    }

    @Test
    public void testPreAuthOffer_RejectsExpirationBeyondConfiguredLifespan() {
        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> wallet.createCredentialOfferUri(ctx, req -> {
                    req.targetUser(ctx.getHolder());
                    req.preAuthorized(true);
                    req.expireAt(Time.currentTimeSeconds() + DEFAULT_CREDENTIAL_OFFER_LIFESPAN_S + 60);
                }));

        CredentialOfferUriResponse response = ctx.getCredentialsOfferUriResponse();
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        assertEquals("invalid_credential_offer_request", response.getError());
        assertTrue(response.getErrorDescription()
                .contains("Credential offer expiration must be after the current time and no later than"));
        assertEquals(String.format("[%s] %s", response.getError(), response.getErrorDescription()), error.getMessage());
    }

    @Test
    public void testPreAuthOffer_RejectsLongMaximumExpiration() {
        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        assertThrows(IllegalStateException.class,
                () -> wallet.createCredentialOfferUri(ctx, req -> {
                    req.targetUser(ctx.getHolder());
                    req.preAuthorized(true);
                    req.expireAt(Long.MAX_VALUE);
                }));

        CredentialOfferUriResponse response = ctx.getCredentialsOfferUriResponse();
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        assertEquals("invalid_credential_offer_request", response.getError());
    }

    @Test
    public void testPreAuthOffer_RejectsExpirationInThePast() {
        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        assertThrows(IllegalStateException.class,
                () -> wallet.createCredentialOfferUri(ctx, req -> {
                    req.targetUser(ctx.getHolder());
                    req.preAuthorized(true);
                    req.expireAt(Time.currentTimeSeconds() - 1L);
                }));

        CredentialOfferUriResponse response = ctx.getCredentialsOfferUriResponse();
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        assertEquals("invalid_credential_offer_request", response.getError());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    public void testPreAuthOffer_RejectsDefaultExpirationForNonPositiveConfiguredLifespan(int invalidLifespan) {
        String originalLifespan = getRealmAttribute(CREDENTIAL_OFFER_LIFESPAN_REALM_ATTRIBUTE_KEY);
        try {
            setRealmAttributes(Map.of(CREDENTIAL_OFFER_LIFESPAN_REALM_ATTRIBUTE_KEY,
                    Integer.toString(invalidLifespan)));
            var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

            assertThrows(IllegalStateException.class,
                    () -> wallet.createCredentialOfferUri(ctx, req -> {
                        req.targetUser(ctx.getHolder());
                        req.preAuthorized(true);
                    }));

            CredentialOfferUriResponse response = ctx.getCredentialsOfferUriResponse();
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            assertEquals("invalid_credential_offer_request", response.getError());
        } finally {
            var realm = testRealm.admin().toRepresentation();
            Map<String, String> attributes = new HashMap<>(realm.getAttributesOrEmpty());
            if (originalLifespan == null) {
                attributes.remove(CREDENTIAL_OFFER_LIFESPAN_REALM_ATTRIBUTE_KEY);
            } else {
                attributes.put(CREDENTIAL_OFFER_LIFESPAN_REALM_ATTRIBUTE_KEY, originalLifespan);
            }
            realm.setAttributes(attributes);
            testRealm.admin().update(realm);
        }
    }

    @Test
    public void testPreAuthOffer_OriginatingSessionLogoutRevokesCode() {
        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        CredentialsOffer credOffer = wallet.createCredentialOffer(ctx, req -> {
            req.targetUser(ctx.getHolder());
            req.preAuthorized(true);
        });
        String preAuthCode = credOffer.getPreAuthorizedCode();
        assertNotNull(preAuthCode, "preAuthCode");

        wallet.logout(ctx.getIssuer());

        AccessTokenResponse tokenResponse = wallet.accessTokenRequestPreAuth(ctx, preAuthCode).send();
        assertFalse(tokenResponse.isSuccess(), "Token request should have failed");
        assertEquals("invalid_grant", tokenResponse.getError());
        assertEquals("Session that authorized the pre-authorized code is not active", tokenResponse.getErrorDescription());
    }

    @Test
    public void testPreAuthOffer_OriginatingUserPasswordChangeRevokesCode() {
        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        CredentialsOffer credOffer = wallet.createCredentialOffer(ctx, req -> {
            req.targetUser(ctx.getHolder());
            req.preAuthorized(true);
        });
        String preAuthCode = credOffer.getPreAuthorizedCode();
        assertNotNull(preAuthCode, "preAuthCode");

        UserRepresentation issuer = testRealm.admin().users().search(ctx.getIssuer(), true).get(0);
        UserResource issuerResource = testRealm.admin().users().get(issuer.getId());
        try {
            issuerResource.resetPassword(passwordCredential("updated-password"));

            AccessTokenResponse tokenResponse = wallet.accessTokenRequestPreAuth(ctx, preAuthCode).send();
            assertFalse(tokenResponse.isSuccess(), "Token request should have failed");
            assertEquals("invalid_grant", tokenResponse.getError());
            assertEquals("Credentials of the user that authorized the pre-authorized code have changed",
                    tokenResponse.getErrorDescription());
        } finally {
            issuerResource.resetPassword(passwordCredential(TEST_PASSWORD));
        }
    }

    @Test
    public void testPreAuthOffer_ServiceAccountCanRedeemCode() {
        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);
        UserRepresentation serviceAccount = testRealm.admin().clients().get(client.getId()).getServiceAccountUser();
        UserResource serviceAccountResource = testRealm.admin().users().get(serviceAccount.getId());
        RoleRepresentation credentialOfferRole = testRealm.admin().roles()
                .get(CREDENTIAL_OFFER_CREATE.getName()).toRepresentation();
        serviceAccountResource.roles().realmLevel().add(List.of(credentialOfferRole));

        try {
            AccessTokenResponse clientCredentialsResponse = oauth
                    .client(client.getClientId(), client.getSecret())
                    .clientCredentialsGrantRequest()
                    .send();
            assertTrue(clientCredentialsResponse.isSuccess(), clientCredentialsResponse.getErrorDescription());

            CredentialOfferURI offerUri = oauth.oid4vc()
                    .credentialOfferUriRequest(ctx.getCredentialConfigurationId())
                    .targetUser(ctx.getHolder())
                    .preAuthorized(true)
                    .bearerToken(clientCredentialsResponse.getAccessToken())
                    .send()
                    .getCredentialOfferURI();
            CredentialsOffer credentialOffer = oauth.oid4vc().doCredentialOfferRequest(offerUri).getCredentialsOffer();

            AccessTokenResponse tokenResponse = wallet
                    .accessTokenRequestPreAuth(ctx, credentialOffer.getPreAuthorizedCode())
                    .send();
            assertTrue(tokenResponse.isSuccess(), tokenResponse.getErrorDescription());
        } finally {
            serviceAccountResource.roles().realmLevel().remove(List.of(credentialOfferRole));
        }
    }

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
    public void testPreAuthOffer_DisabledClient() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Create Pre-Authorized CredentialOffer
        //
        CredentialsOffer credOffer = wallet.createCredentialOffer(ctx, req -> {
            req.targetUser(ctx.getHolder());
            req.preAuthorized(true);
        });

        String preAuthCode = credOffer.getPreAuthorizedCode();
        assertNotNull(preAuthCode, "preAuthCode");

        // Disable the client
        ClientRepresentation clientRep = testRealm.admin().clients().get(ctx.getClient().getId()).toRepresentation();
        clientRep.setEnabled(false);
        testRealm.admin().clients().get(ctx.getClient().getId()).update(clientRep);

        try {
            // Attempt to redeem Pre-Authorized Code for AccessToken should fail
            //
            AccessTokenResponse tokenResponse = wallet.accessTokenRequestPreAuth(ctx, preAuthCode).send();
            assertFalse(tokenResponse.isSuccess(), "Token request should have failed for disabled client");
            assertEquals("invalid_request", tokenResponse.getError());
            assertTrue(tokenResponse.getErrorDescription().contains("disabled"),
                    "Error description should mention disabled: " + tokenResponse.getErrorDescription());
        } finally {
            // Re-enable client
            clientRep.setEnabled(true);
            testRealm.admin().clients().get(ctx.getClient().getId()).update(clientRep);
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

        CredentialOfferURI offerURI = ctx.getCredentialsOfferUri();
        assertNotNull(offerURI, "No CredentialOfferURI");

        // Verify internal offer state target user and client
        //
        CredentialOfferStateRecord runtimeOfferState = getCredentialOfferStateRecord(runOnServer, offerURI.getNonce());
        assertEquals("john", runtimeOfferState.targetUsername());
        assertEquals(client.getClientId(), runtimeOfferState.targetClientId());

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

        CredentialOfferURI offerURI = ctx.getCredentialsOfferUri();
        assertNotNull(offerURI, "No CredentialOfferURI");

        // Verify internal offer state target user and client
        //
        CredentialOfferStateRecord runtimeOfferState = getCredentialOfferStateRecord(runOnServer, offerURI.getNonce());
        assertEquals("alice", runtimeOfferState.targetUsername());
        assertEquals(client.getClientId(), runtimeOfferState.targetClientId());

        String preAuthCode = credOffer.getPreAuthorizedCode();
        assertNotNull(preAuthCode, "preAuthCode");

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

        // The offer is retained so the pre-authorized-code handler can report a replay.
        CredentialOfferResponse offerResponse = wallet.credentialsOfferRequest(ctx, offerURI).send();
        assertEquals(HttpStatus.SC_OK, offerResponse.getStatusCode());

        AccessTokenResponse replayResponse = wallet.accessTokenRequestPreAuth(ctx, preAuthCode).send();
        assertFalse(replayResponse.isSuccess(), "Reusing a pre-authorized code must fail");
        assertEquals("invalid_grant", replayResponse.getError());
        assertEquals("Pre-authorized code has already been used", replayResponse.getErrorDescription());
    }

    @Test
    public void testPreAuthOffer_WrongTxCodeDoesNotConsumePreAuthCode() throws Exception {
        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Create a pre-authorized credential offer
        CredentialsOffer credOffer = wallet.createCredentialOffer(ctx, req -> {
            req.targetUser(ctx.getHolder());
            req.preAuthorized(true);
        });

        String preAuthCode = credOffer.getPreAuthorizedCode();
        assertNotNull(preAuthCode, "preAuthCode");

        CredentialOfferURI offerURI = ctx.getCredentialsOfferUri();
        assertNotNull(offerURI, "No CredentialOfferURI");

        // Inject a tx_code into the stored offer state
        String correctTxCode = "123456";
        injectTxCode(runOnServer, offerURI.getNonce(), correctTxCode);

        // Verify the tx_code was injected
        CredentialOfferStateRecord offerState = getCredentialOfferStateRecord(runOnServer, offerURI.getNonce());
        assertEquals(correctTxCode, offerState.txCode());

        // Attempt to redeem with a wrong tx_code — should fail but NOT consume the pre-authorized code
        AccessTokenResponse wrongTxResponse = wallet.accessTokenRequestPreAuth(ctx, preAuthCode)
                .txCode("000000")
                .send();
        assertFalse(wrongTxResponse.isSuccess(), "Token request with wrong tx_code should fail");
        assertEquals("invalid_grant", wrongTxResponse.getError());
        assertEquals("Invalid TxCode", wrongTxResponse.getErrorDescription());

        // Attempt to redeem with the correct tx_code — should succeed because the code was not consumed
        AccessTokenResponse correctTxResponse = wallet.accessTokenRequestPreAuth(ctx, preAuthCode)
                .txCode(correctTxCode)
                .send();
        assertTrue(correctTxResponse.isSuccess(),
                "Token request with correct tx_code should succeed after a failed attempt: " + correctTxResponse.getErrorDescription());
    }

    @Test
    public void testPreAuthOffer_MissingTxCodeDoesNotConsumePreAuthCode() {
        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Create a pre-authorized credential offer
        CredentialsOffer credOffer = wallet.createCredentialOffer(ctx, req -> {
            req.targetUser(ctx.getHolder());
            req.preAuthorized(true);
        });

        String preAuthCode = credOffer.getPreAuthorizedCode();
        assertNotNull(preAuthCode, "preAuthCode");

        CredentialOfferURI offerURI = ctx.getCredentialsOfferUri();
        assertNotNull(offerURI, "No CredentialOfferURI");

        // Inject a tx_code into the stored offer state
        String correctTxCode = "789012";
        injectTxCode(runOnServer, offerURI.getNonce(), correctTxCode);

        // Attempt to redeem without providing tx_code — should fail but NOT consume the code
        AccessTokenResponse missingTxResponse = wallet.accessTokenRequestPreAuth(ctx, preAuthCode).send();
        assertFalse(missingTxResponse.isSuccess(), "Token request without tx_code should fail when one is required");
        assertEquals("invalid_grant", missingTxResponse.getError());
        assertEquals("Missing TxCode", missingTxResponse.getErrorDescription());

        // Attempt to redeem with the correct tx_code — should succeed
        AccessTokenResponse correctTxResponse = wallet.accessTokenRequestPreAuth(ctx, preAuthCode)
                .txCode(correctTxCode)
                .send();
        assertTrue(correctTxResponse.isSuccess(),
                "Token request with correct tx_code should succeed after missing tx_code attempt: " + correctTxResponse.getErrorDescription());
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private CredentialRepresentation passwordCredential(String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setValue(password);
        credential.setTemporary(false);
        return credential;
    }

    private void verifyCredentialResponse(OID4VCTestContext ctx, String expUser, CredentialResponse credResponse) throws Exception {

        String scope = ctx.getCredentialScope().getName();
        CredentialResponse.Credential credentialObj = credResponse.getCredentials().get(0);
        assertNotNull(credentialObj, "The first credential in the array should not be null");

        JsonWebToken jsonWebToken = TokenVerifier.create((String) credentialObj.getCredential(), JsonWebToken.class).getToken();
        assertEquals("did:web:test.org", jsonWebToken.getIssuer());
        Object vc = jsonWebToken.getOtherClaims().get("vc");
        VerifiableCredential credential = JsonSerialization.mapper.convertValue(vc, VerifiableCredential.class);
        assertEquals(List.of(CredentialDefinition.VERIFIABLE_CREDENTIAL_TYPE, scope), credential.getType());
        assertEquals(URI.create("did:web:test.org"), credential.getIssuer());
        assertEquals(expUser + "@email.cz", credential.getCredentialSubject().getClaims().get("email"));
    }
}
