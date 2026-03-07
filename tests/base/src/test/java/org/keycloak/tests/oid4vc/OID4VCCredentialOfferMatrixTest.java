package org.keycloak.tests.oid4vc;

import java.net.URI;
import java.util.List;
import java.util.function.BiFunction;

import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.ClientPoliciesPoliciesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.protocol.oid4vc.clientpolicy.PredicateCredentialClientPolicy;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
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
import static org.keycloak.protocol.oid4vc.clientpolicy.CredentialClientPolicies.VC_POLICY_CREDENTIAL_OFFER_PREAUTH_ALLOWED;
import static org.keycloak.protocol.oid4vc.clientpolicy.CredentialClientPolicies.VC_POLICY_CREDENTIAL_OFFER_REQUIRED;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.CREDENTIAL_OFFER_URI_ATTACHMENT_KEY;

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

        ClientProfilesRepresentation clientProfiles = testRealm.admin().clientPoliciesProfilesResource().getProfiles(true);
        assertEquals(1, clientProfiles.getProfiles().size());
        assertEquals("oid4vci-client-profile", clientProfiles.getProfiles().get(0).getName());

        ClientPoliciesRepresentation clientPolicies = testRealm.admin().clientPoliciesPoliciesResource().getPolicies();
        assertEquals(2, clientPolicies.getPolicies().size());
        assertEquals("oid4vci-offer-required", clientPolicies.getPolicies().get(0).getName());
        assertEquals("oid4vci-offer-preauth-allowed", clientPolicies.getPolicies().get(1).getName());
    }

    @Test
    public void testNoOffer_Scope() throws Exception {

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
    public void testNoOffer_Scope_AuthDetails() throws Exception {

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
    public void testNoOffer_Scope_NotAllowed() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        PredicateCredentialClientPolicy offerRequiredPolicy = VC_POLICY_CREDENTIAL_OFFER_REQUIRED;

        BiFunction<Boolean, Boolean, Boolean> runner = (policyEnabled, scopeEnabled) -> {

            // Set client policy 'oid4vci-offer-required'
            //
            ClientPoliciesPoliciesResource clientPoliciesResource = testRealm.admin().clientPoliciesPoliciesResource();
            ClientPoliciesRepresentation policies = clientPoliciesResource.getPolicies();
            ClientPolicyRepresentation clientPolicy = policies.getPolicies().stream()
                    .filter(cp -> cp.getName().equals(offerRequiredPolicy.getName()))
                    .findFirst().orElseThrow();
            Boolean wasPolicyEnabled = clientPolicy.isEnabled();
            clientPolicy.setEnabled(policyEnabled);
            clientPoliciesResource.updatePolicies(policies);

            // Set client scope attribute 'vc.policy.offer.required'
            //
            Boolean wasScopeEnabled = offerRequiredPolicy.getCurrentValue(ctx.credentialScope);
            ctx.credentialScope.setAttribute(offerRequiredPolicy.getAttrName(), String.valueOf(scopeEnabled));
            updateCredentialScope(ctx.credentialScope);

            try {
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

                if (policyEnabled || scopeEnabled) {
                    AssertionError error = assertThrows(AssertionError.class,
                            () -> wallet.validateHolderAccessToken(ctx, tokenResponse));
                    assertTrue(error.getMessage().contains("Credential request rejected by policy oid4vci-offer-required for client scope jwt-credential"), error.getMessage());
                    return false;
                } else {
                    String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
                    assertNotNull(accessToken, "No accessToken");

                    String authorizedIdentifier = ctx.getAuthorizedCredentialIdentifier();
                    assertNotNull(authorizedIdentifier,"No authorized credential identifier");

                    CredentialResponse credentialResponse = wallet.credentialRequest(ctx, accessToken).credentialIdentifier(authorizedIdentifier).send().getCredentialResponse();
                    assertFalse(credentialResponse.getCredentials().isEmpty(), "Credentials expected");
                    return true;
                }
            } finally {
                clientPolicy.setEnabled(wasPolicyEnabled);
                clientPoliciesResource.updatePolicies(policies);

                ctx.credentialScope.setAttribute(offerRequiredPolicy.getAttrName(), String.valueOf(wasScopeEnabled));
                updateCredentialScope(ctx.credentialScope);

                wallet.logout(ctx.holder);
            }
        };

        assertTrue(runner.apply(false, false), "Offer not required");
        assertFalse(runner.apply(false, true), "Offer required");
        assertFalse(runner.apply(true, false), "Offer required");
        assertFalse(runner.apply(true, true), "Offer required");
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
                    () -> wallet.createPreAuthCredentialOffer(ctx, ctx.holder, false));
            assertTrue(error.getMessage().contains("User 'alice' disabled"), error.getMessage());
        } finally {
            userRep.setEnabled(true);
            userResource.update(userRep);
        }
    }

    @Test
    public void testPreAuthOffer_NotAllowed() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        PredicateCredentialClientPolicy preAuthPolicy = VC_POLICY_CREDENTIAL_OFFER_PREAUTH_ALLOWED;

        BiFunction<Boolean, Boolean, Boolean> runner = (policyEnabled, scopeEnabled) -> {

            // Set client policy 'oid4vci-offer-preauth-allowed'
            //
            ClientPoliciesPoliciesResource clientPoliciesResource = testRealm.admin().clientPoliciesPoliciesResource();
            ClientPoliciesRepresentation policies = clientPoliciesResource.getPolicies();
            ClientPolicyRepresentation clientPolicy = policies.getPolicies().stream()
                    .filter(cp -> cp.getName().equals(preAuthPolicy.getName()))
                    .findFirst().orElseThrow();
            Boolean wasPolicyEnabled = clientPolicy.isEnabled();
            clientPolicy.setEnabled(policyEnabled);
            clientPoliciesResource.updatePolicies(policies);

            // Set client scope attribute 'vc.policy.offer.preauth.allowed'
            //
            Boolean wasScopeEnabled = preAuthPolicy.getCurrentValue(ctx.credentialScope);
            ctx.credentialScope.setAttribute(preAuthPolicy.getAttrName(), String.valueOf(scopeEnabled));
            updateCredentialScope(ctx.credentialScope);

            try {
                if (policyEnabled && scopeEnabled) {
                    CredentialsOffer credOffer = wallet.createPreAuthCredentialOffer(ctx, ctx.holder, false);
                    assertNotNull(credOffer.getPreAuthorizedCode(), "Expected pre-auth offer");
                    return true;
                } else {
                    IllegalStateException error = assertThrows(IllegalStateException.class,
                            () -> wallet.createPreAuthCredentialOffer(ctx, ctx.holder, false));
                    if (!policyEnabled) {
                        assertTrue(error.getMessage().contains("Credential offer creation rejected for client scope jwt-credential"), error.getMessage());
                    } else {
                        assertTrue(error.getMessage().contains("Pre-Authorized code grant rejected by policy oid4vci-offer-preauth-allowed for client scope jwt-credential"), error.getMessage());
                    }
                    return false;
                }
            } finally {
                clientPolicy.setEnabled(wasPolicyEnabled);
                clientPoliciesResource.updatePolicies(policies);

                ctx.credentialScope.setAttribute(preAuthPolicy.getAttrName(), String.valueOf(wasScopeEnabled));
                updateCredentialScope(ctx.credentialScope);

                wallet.logout(ctx.holder);
            }
        };

        assertFalse(runner.apply(false, false), "Pre-Auth offer denied");
        assertFalse(runner.apply(false, true), "Pre-Auth offer denied");
        assertFalse(runner.apply(true, false), "Pre-Auth offer denied");
        assertTrue(runner.apply(true, true), "Pre-Auth offer allowed");
    }

    @Test
    public void testPreAuthOffer_SelfIssued() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Create Pre-Authorized CredentialOffer
        //
        CredentialsOffer credOffer = wallet.createPreAuthCredentialOffer(ctx, null, false);
        String preAuthCode = credOffer.getPreAuthorizedCode();

        // Redeem Pre-Authorized Code for AccessToken
        //
        AccessTokenResponse tokenResponse = wallet.preAuthAccessTokenRequest(ctx, preAuthCode, null).send();
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
        CredentialsOffer credOffer = wallet.createPreAuthCredentialOffer(ctx, ctx.holder, true);
        String preAuthCode = credOffer.getPreAuthorizedCode();

        // Redeem Pre-Authorized Code for AccessToken
        //
        String txCode = ctx.getAttachment(CREDENTIAL_OFFER_URI_ATTACHMENT_KEY).getTxCode();
        AccessTokenResponse tokenResponse = wallet.preAuthAccessTokenRequest(ctx, preAuthCode, txCode).send();
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

    @Test
    public void testPreAuthOffer_Targeted_Invalid_TxCode() throws Exception {
        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        // Create Pre-Authorized CredentialOffer
        //
        CredentialsOffer credOffer = wallet.createPreAuthCredentialOffer(ctx, ctx.holder, true);
        String preAuthCode = credOffer.getPreAuthorizedCode();

        // Test missing TxCode
        {
            AccessTokenResponse tokenResponse = wallet.preAuthAccessTokenRequest(ctx, preAuthCode, null).send();
            AssertionError error = assertThrows(AssertionError.class, () -> wallet.validateHolderAccessToken(ctx, tokenResponse));
            assertTrue(error.getMessage().contains("Missing TxCode"), error.getMessage());
        }

        // Test wrong TxCode
        {
            AccessTokenResponse tokenResponse = wallet.preAuthAccessTokenRequest(ctx, preAuthCode, "wrong-code").send();
            AssertionError error = assertThrows(AssertionError.class, () -> wallet.validateHolderAccessToken(ctx, tokenResponse));
            assertTrue(error.getMessage().contains("Invalid TxCode"), error.getMessage());
        }
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
