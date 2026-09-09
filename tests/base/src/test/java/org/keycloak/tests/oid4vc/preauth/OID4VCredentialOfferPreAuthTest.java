package org.keycloak.tests.oid4vc.preauth;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;
import org.keycloak.protocol.oid4vc.model.CredentialDefinition;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Credential Offer Validity Matrix
 * <p>
 * +----------+----------+-----------------+---------+------------------------------------------------------+
 * | Pre-Auth | Username | Client          | Valid   | Notes                                                |
 * +----------+----------+-----------------+---------+------------------------------------------------------+
 * | yes      | no       | explicit/unique | yes     | Defaults to the user from the current login session  |
 * | yes      | yes      | explicit        | yes     | Pre-auth for a specific target user and client.      |
 * | yes      | yes      | omitted unique  | yes     | Discovered from credential_configuration_id.         |
 * | yes      | yes      | omitted none    | no      | Discovery fails: no OID4VCI client has that scope.   |
 * | yes      | yes      | omitted multi   | no      | Discovery fails: multiple clients match.             |
 * +----------+----------+-----------------+---------+------------------------------------------------------+
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerWithPreAuthCodeEnabled.class)
public class OID4VCredentialOfferPreAuthTest extends OID4VCIssuerTestBase {

    @Test
    public void testPreAuthOffer_RejectsExpirationBeyondConfiguredLifespan() {
        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        CredentialOfferUriResponse response = wallet.createCredentialOfferUri(ctx, req -> {
            req.targetClient(ctx.getClientId());
            req.targetUser(ctx.getHolder());
            req.preAuthorized(true);
            req.expireAt(Time.currentTimeSeconds() + DEFAULT_CREDENTIAL_OFFER_LIFESPAN_S + 60);
        });
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        assertEquals("invalid_credential_offer_request", response.getError());
        assertTrue(response.getErrorDescription()
                .contains("Credential offer expiration must be after the current time and no later than"));
    }

    @Test
    public void testPreAuthOffer_RejectsLongMaximumExpiration() {
        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        CredentialOfferUriResponse response = wallet.createCredentialOfferUri(ctx, req -> {
            req.targetClient(ctx.getClientId());
            req.targetUser(ctx.getHolder());
            req.preAuthorized(true);
            req.expireAt(Long.MAX_VALUE);
        });
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        assertEquals("invalid_credential_offer_request", response.getError());
    }

    @Test
    public void testPreAuthOffer_RejectsExpirationInThePast() {
        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        CredentialOfferUriResponse response = wallet.createCredentialOfferUri(ctx, req -> {
            req.targetClient(ctx.getClientId());
            req.targetUser(ctx.getHolder());
            req.preAuthorized(true);
            req.expireAt(Time.currentTimeSeconds() - 1L);
        });
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

            CredentialOfferUriResponse response = wallet.createCredentialOfferUri(ctx, req -> {
                req.targetClient(ctx.getClientId());
                req.targetUser(ctx.getHolder());
                req.preAuthorized(true);
            });
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
            req.targetClient(ctx.getClientId());
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
            req.targetClient(ctx.getClientId());
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
                    .targetClient(ctx.getClientId())
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
    public void testPreAuthOfferMatrix() {

        record MatrixParams(
                ClientRepresentation issuerClient,
                ClientRepresentation targetClient,
                String targetUser
        ) {
            String targetClientId() {
                return Optional.ofNullable(targetClient)
                        .map(ClientRepresentation::getClientId)
                        .orElse(null);
            }
        }

        Function<MatrixParams, Boolean> runMatrixParams = (p) -> {
            String expTargetUser = p.targetUser() != null ? p.targetUser() : "john";
            String expTargetClient = p.targetClientId() != null ? p.targetClientId() : client.getClientId();

            var otherClients = List.of(abcaClient, pubClient);
            var ctx = createOID4VCTestContext().withHolder(expTargetUser);

            // Create CredentialOfferURI
            //
            CredentialOfferUriResponse uriResponse;
            try {

                // Remove the credential scope from the two other OID4VCI clients so only oid4vci-client has it
                if (p.targetClient == null) {
                    removeOptionalClientScope(otherClients, ctx.getCredentialScope());
                }

                oauth.client(p.issuerClient.getClientId(), p.issuerClient.getSecret());
                uriResponse = wallet.createCredentialOfferUri(ctx, req -> {
                    req.targetClient(p.targetClientId());
                    req.targetUser(p.targetUser());
                    req.preAuthorized(true);
                });
            } finally {
                // Restore the credential scope to the two other OID4VCI clients
                if (p.targetClient == null) {
                    restoreOptionalClientScope(otherClients, ctx.getCredentialScope());
                }
                if (p.targetClient != null) {
                    oauth.client(p.targetClient.getClientId(), p.targetClient.getSecret());
                } else {
                    oauth.client(client.getClientId(), client.getSecret());
                }
            }
            if (p.targetClient != null && !p.targetClient.getOptionalClientScopes().contains(ctx.getScope())) {
                assertFalse(uriResponse.isSuccess());
                assertEquals("invalid_credential_offer_request", uriResponse.getError());
                String expErrorDescription = String.format("Client '%s' does not support '%s'", p.targetClientId(), ctx.getScope());
                assertEquals(expErrorDescription, uriResponse.getErrorDescription());
                wallet.logout();
                return false;
            }
            CredentialOfferURI offerURI = uriResponse.getCredentialOfferURI();

            // Get Credentials Offer
            //
            CredentialOfferResponse offerResponse = wallet.credentialsOfferRequest(ctx, offerURI).send();
            CredentialsOffer credOffer = offerResponse.getCredentialsOffer();

            String preAuthCode = credOffer.getPreAuthorizedCode();
            assertNotNull(preAuthCode, "No PreAuthorizedCode");

            // Verify internal offer state target user and client
            //
            CredentialOfferStateRecord offerState = getCredentialOfferStateRecord(runOnServer, offerURI.getNonce());
            assertEquals(expTargetUser, offerState.targetUsername());
            assertEquals(expTargetClient, offerState.targetClientId());

            // Send the CredentialRequest
            //
            CredentialResponse credResponse = wallet.fetchCredentialByOffer(ctx, credOffer)
                    .getCredentialResponse();

            verifyCredentialResponse(ctx, expTargetUser, credResponse);

            wallet.logout();

            return true;
        };

        assertTrue(runMatrixParams.apply(new MatrixParams(client2, null, null)));
        assertTrue(runMatrixParams.apply(new MatrixParams(client2, client, null)));
        assertFalse(runMatrixParams.apply(new MatrixParams(client2, client2, null)));
        assertTrue(runMatrixParams.apply(new MatrixParams(client2, null, "alice")));
        assertTrue(runMatrixParams.apply(new MatrixParams(client2, client, "alice")));
        assertFalse(runMatrixParams.apply(new MatrixParams(client2, client2, "alice")));
    }

    @Test
    public void testPreAuthOffer_DisabledUser() {

        var ctx = createOID4VCTestContext();

        String accessToken = wallet.getIssuerAccessToken(ctx);

        // Disable user
        UserRepresentation userRep = testRealm.admin().users().search(ctx.getHolder()).get(0);
        UserResource userResource = testRealm.admin().users().get(userRep.getId());
        userRep.setEnabled(false);
        userResource.update(userRep);

        try {
            String credConfigId = ctx.getCredentialConfigurationId();
            CredentialOfferUriResponse uriResponse = wallet.credentialOfferUriRequest(ctx, credConfigId)
                    .bearerToken(accessToken)
                    .targetClient(ctx.getClientId())
                    .targetUser(ctx.getHolder())
                    .preAuthorized(true)
                    .send();
            String errorDescription = uriResponse.getErrorDescription();
            assertFalse(uriResponse.isSuccess(), "Expected to fail");
            assertEquals("invalid_credential_offer_request", uriResponse.getError());
            assertEquals("User 'alice' disabled", errorDescription);
        } finally {
            userRep.setEnabled(true);
            userResource.update(userRep);
        }
    }

    @Test
    public void testPreAuthOffer_DisabledClient() {

        var ctx = createOID4VCTestContext();

        String accessToken = wallet.getIssuerAccessToken(ctx);

        // Disable the client
        ClientRepresentation clientRep = testRealm.admin().clients().get(ctx.getHolderClient().getId()).toRepresentation();
        clientRep.setEnabled(false);
        testRealm.admin().clients().get(ctx.getHolderClient().getId()).update(clientRep);

        try {
            String credConfigId = ctx.getCredentialConfigurationId();
            CredentialOfferUriResponse uriResponse = wallet.credentialOfferUriRequest(ctx, credConfigId)
                    .bearerToken(accessToken)
                    .targetUser(ctx.getHolder())
                    .targetClient(ctx.getClientId())
                    .preAuthorized(true)
                    .send();
            String errorDescription = uriResponse.getErrorDescription();
            assertFalse(uriResponse.isSuccess(), "Expected to fail");
            assertEquals("invalid_credential_offer_request", uriResponse.getError());
            assertEquals("Client 'oid4vci-client' disabled", errorDescription);
        } finally {
            clientRep.setEnabled(true);
            testRealm.admin().clients().get(ctx.getHolderClient().getId()).update(clientRep);
        }
    }

    /**
     * Explicit targetClient not found: passing an unknown client_id must fail at offer-creation time.
     */
    @Test
    public void testPreAuthOffer_ExplicitTargetClient_NotFound() {

        var ctx = createOID4VCTestContext();

        CredentialOfferUriResponse uriResponse = wallet.credentialOfferUriRequest(ctx, ctx.getCredentialConfigurationId())
                .bearerToken(wallet.getIssuerAccessToken(ctx))
                .targetUser(ctx.getHolder())
                .targetClient("non-existent-client")
                .preAuthorized(true)
                .send();

        assertFalse(uriResponse.isSuccess(), "Offer creation should have failed for unknown target client");
        assertEquals("invalid_credential_offer_request", uriResponse.getError());
        assertTrue(uriResponse.getErrorDescription().contains("non-existent-client"),
                "Error should mention the unknown client: " + uriResponse.getErrorDescription());
    }

    /**
     * Explicit targetClient that is not OID4VCI-enabled: must fail at offer-creation time.
     */
    @Test
    public void testPreAuthOffer_ExplicitTargetClient_NotOid4vciEnabled() {

        var ctx = createOID4VCTestContext();

        // Use a well-known Keycloak built-in client that is not OID4VCI-enabled
        String nonOid4vciClientId = "account";

        CredentialOfferUriResponse uriResponse = wallet.credentialOfferUriRequest(ctx, ctx.getCredentialConfigurationId())
                .bearerToken(wallet.getIssuerAccessToken(ctx))
                .targetUser(ctx.getHolder())
                .targetClient(nonOid4vciClientId)
                .preAuthorized(true)
                .send();

        assertFalse(uriResponse.isSuccess(), "Offer creation should have failed for non-OID4VCI-enabled target client");
        assertEquals("invalid_credential_offer_request", uriResponse.getError());
        assertTrue(uriResponse.getErrorDescription().contains(nonOid4vciClientId),
                "Error should mention the client id: " + uriResponse.getErrorDescription());
    }

    /**
     * Discovery failure — no match: the credential_configuration_id is not assigned to any
     * OID4VCI-enabled client, so discovery must fail at offer-creation time.
     */
    @Test
    public void testPreAuthOffer_DiscoverTargetClient_NoMatch() {

        var ctx = createOID4VCTestContext();
        String credConfigId = ctx.getCredentialConfigurationId();

        var otherClients = List.of(client, abcaClient, pubClient);
        try {
            removeOptionalClientScope(otherClients, ctx.getCredentialScope());

            CredentialOfferUriResponse uriResponse = wallet.credentialOfferUriRequest(ctx, credConfigId)
                    .bearerToken(wallet.getIssuerAccessToken(ctx))
                    .targetUser(ctx.getHolder())
                    // no targetClient → discovery attempted, should fail
                    .preAuthorized(true)
                    .send();

            assertFalse(uriResponse.isSuccess(), "Offer creation should fail when no client matches");
            assertEquals("invalid_credential_offer_request", uriResponse.getError());
            String expError = "No OID4VCI client found for credential configuration ids: [jwt-credential-config-id]";
            assertEquals(expError, uriResponse.getErrorDescription());

        } finally {
            restoreOptionalClientScope(otherClients, ctx.getCredentialScope());
        }
    }

    /**
     * Discovery failure — ambiguous: the credential_configuration_id maps to more than one
     * OID4VCI-enabled client, so discovery must fail with a descriptive error.
     * <p>
     * The default realm config assigns all credential scopes as optional to all three OID4VCI
     * clients, so omitting target_client on jwtTypeCredentialScope always hits this path.
     */
    @Test
    public void testPreAuthOffer_DiscoverTargetClient_Ambiguous() {

        var ctx = createOID4VCTestContext();

        CredentialOfferUriResponse uriResponse = wallet.credentialOfferUriRequest(ctx, ctx.getCredentialConfigurationId())
                .bearerToken(wallet.getIssuerAccessToken(ctx))
                .targetUser(ctx.getHolder())
                .preAuthorized(true)
                // no targetClient → multiple clients match → should fail
                .send();

        assertFalse(uriResponse.isSuccess(), "Offer creation should fail when multiple clients match");
        assertEquals("invalid_credential_offer_request", uriResponse.getError());
        assertEquals("Multiple OID4VCI clients for credential configuration ids: [jwt-credential-config-id]", uriResponse.getErrorDescription());
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private CredentialRepresentation passwordCredential(String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setValue(password);
        credential.setTemporary(false);
        return credential;
    }

    private OID4VCTestContext createOID4VCTestContext() {
        return new OID4VCTestContext(client, jwtTypeCredentialScope)
                .withIssuerClient(client2);
    }

    private void removeOptionalClientScope(List<ClientRepresentation> clients, CredentialScopeRepresentation credScope) {
        String scopeId = credScope.getId();
        for (ClientRepresentation client : clients) {
            testRealm.admin().clients().get(client.getId()).removeOptionalClientScope(scopeId);
        }
    }

    private void restoreOptionalClientScope(List<ClientRepresentation> clients, CredentialScopeRepresentation credScope) {
        String scopeId = credScope.getId();
        for (ClientRepresentation client : clients) {
            testRealm.admin().clients().get(client.getId()).addOptionalClientScope(scopeId);
        }
    }

    private void verifyCredentialResponse(OID4VCTestContext ctx, String expUser, CredentialResponse credResponse) {

        String scope = ctx.getCredentialScope().getName();
        CredentialResponse.Credential credentialObj = credResponse.getCredentials().get(0);
        assertNotNull(credentialObj, "The first credential in the array should not be null");

        JsonWebToken jsonWebToken;
        try {
            jsonWebToken = TokenVerifier.create((String) credentialObj.getCredential(), JsonWebToken.class).getToken();
        } catch (VerificationException e) {
            throw new RuntimeException(e);
        }

        assertEquals("did:web:test.org", jsonWebToken.getIssuer());
        Object vc = jsonWebToken.getOtherClaims().get("vc");
        VerifiableCredential credential = JsonSerialization.mapper.convertValue(vc, VerifiableCredential.class);
        assertEquals(List.of(CredentialDefinition.VERIFIABLE_CREDENTIAL_TYPE, scope), credential.getType());
        assertEquals(URI.create("did:web:test.org"), credential.getIssuer());
        assertEquals(expUser + "@email.cz", credential.getCredentialSubject().getClaims().get("email"));
    }
}
