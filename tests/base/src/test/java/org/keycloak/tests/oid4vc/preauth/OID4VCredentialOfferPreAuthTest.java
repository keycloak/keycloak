package org.keycloak.tests.oid4vc.preauth;

import java.net.URI;
import java.util.List;
import java.util.function.BiFunction;

import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.ClientPoliciesPoliciesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.protocol.oid4vc.clientpolicy.PredicateCredentialClientPolicy;
import org.keycloak.protocol.oid4vc.model.CredentialDefinition;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Test;

import static org.keycloak.protocol.oid4vc.clientpolicy.CredentialClientPolicies.VC_POLICY_CREDENTIAL_OFFER_REQUIRED;
import static org.keycloak.protocol.oid4vc.clientpolicy.CredentialClientPolicies.VC_POLICY_CREDENTIAL_PREAUTH_ALLOWED;

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

    @Test
    public void testRealmSetup() {
        List<String> expectedProfiles = List.of("oid4vci-offer-required-profile", "oid4vci-preauth-allowed-profile");
        ClientProfilesRepresentation clientProfiles = testRealm.admin().clientPoliciesProfilesResource().getProfiles(true);
        List<String> profileNames = clientProfiles.getProfiles().stream().map(ClientProfileRepresentation::getName).toList();
        assertTrue(profileNames.containsAll(expectedProfiles), "Expected profiles not in: " + profileNames);

        List<String> expectedPolicies = List.of(VC_POLICY_CREDENTIAL_OFFER_REQUIRED.getName(), VC_POLICY_CREDENTIAL_PREAUTH_ALLOWED.getName());
        ClientPoliciesRepresentation clientPolicies = testRealm.admin().clientPoliciesPoliciesResource().getPolicies();
        List<String> policyNames = clientPolicies.getPolicies().stream().map(ClientPolicyRepresentation::getName).toList();
        assertTrue(policyNames.containsAll(expectedPolicies), "Expected policies not in: " + policyNames);
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
    public void testPreAuthOffer_TargetUser() throws Exception {
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

        // Attempt to fetch the credential offer again after it has been consumed
        CredentialOfferResponse res = wallet.credentialsOfferRequest(ctx, offerURI).send();
        assertEquals("invalid_credential_offer_request", res.getError());
        assertEquals("Credential offer not found or already consumed", res.getErrorDescription());
    }

    @Test
    public void testPreAuthOffer_ClientPolicy() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        PredicateCredentialClientPolicy preAuthPolicy = VC_POLICY_CREDENTIAL_PREAUTH_ALLOWED;

        BiFunction<Boolean, Boolean, Boolean> runner = (policyEnabled, scopeEnabled) -> {

            // Set client policy 'oid4vci-preauth-allowed'
            //
            ClientPoliciesPoliciesResource clientPoliciesResource = testRealm.admin().clientPoliciesPoliciesResource();
            ClientPoliciesRepresentation policies = clientPoliciesResource.getPolicies();
            ClientPolicyRepresentation clientPolicy = policies.getPolicies().stream()
                    .filter(cp -> cp.getName().equals(preAuthPolicy.getName()))
                    .findFirst().orElseThrow();
            Boolean wasPolicyEnabled = clientPolicy.isEnabled();
            clientPolicy.setEnabled(policyEnabled);
            clientPoliciesResource.updatePolicies(policies);

            // Set client scope attribute 'vc.policy.preauth.allowed'
            //
            var credScope = ctx.getCredentialScope();
            Boolean wasScopeEnabled = credScope.getCredentialPolicyValue(preAuthPolicy);
            credScope.setCredentialPolicyValue(preAuthPolicy, scopeEnabled);
            updateCredentialScope(credScope);

            try {
                if (policyEnabled && scopeEnabled) {
                    CredentialsOffer credOffer = wallet.createCredentialOffer(ctx, req -> {
                        req.targetUser(ctx.getHolder());
                        req.preAuthorized(true);
                    });
                    assertNotNull(credOffer.getPreAuthorizedCode(), "Expected pre-auth offer");
                    return true;
                } else {
                    IllegalStateException error = assertThrows(IllegalStateException.class,
                            () -> wallet.createCredentialOffer(ctx, req -> {
                                req.targetUser(ctx.getHolder());
                                req.preAuthorized(true);
                            }));
                    assertTrue(error.getMessage().contains("Pre-Authorized code grant rejected by policy oid4vci-preauth-allowed"), error.getMessage());
                    return false;
                }
            } finally {
                clientPolicy.setEnabled(wasPolicyEnabled);
                clientPoliciesResource.updatePolicies(policies);

                credScope.setCredentialPolicyValue(preAuthPolicy, wasScopeEnabled);
                updateCredentialScope(credScope);

                wallet.logout(ctx.getHolder());
            }
        };

        // Verification matrix (policyEnabled, scopeEnabled) - We use an AND condition
        //
        assertFalse(runner.apply(false, false), "Pre-Auth offer denied");
        assertFalse(runner.apply(false, true), "Pre-Auth offer denied");
        assertFalse(runner.apply(true, false), "Pre-Auth offer denied");
        assertTrue(runner.apply(true, true), "Pre-Auth offer allowed");
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
        assertEquals(List.of(CredentialDefinition.VERIFIABLE_CREDENTIAL_TYPE, scope), credential.getType());
        assertEquals(URI.create("did:web:test.org"), credential.getIssuer());
        assertEquals(expUser + "@email.cz", credential.getCredentialSubject().getClaims().get("email"));
    }
}
