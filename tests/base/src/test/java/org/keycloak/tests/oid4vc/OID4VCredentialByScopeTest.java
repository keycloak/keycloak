package org.keycloak.tests.oid4vc;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

import org.keycloak.TokenVerifier;
import org.keycloak.protocol.oid4vc.clientpolicy.PredicateCredentialClientPolicy;
import org.keycloak.protocol.oid4vc.model.CredentialDefinition;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCBasicWallet.AuthorizationEndpointRequest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.protocol.oid4vc.clientpolicy.CredentialClientPolicies.VC_POLICY_CREDENTIAL_OFFER_REQUIRED;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerWithRestCredentialOfferEnabled.class)
public class OID4VCredentialByScopeTest extends OID4VCIssuerTestBase {

    @Test
    public void testRealmSetup() {
        String expProfile = "oid4vci-offer-required-profile";
        ClientProfilesRepresentation clientProfiles = testRealm.admin().clientPoliciesProfilesResource().getProfiles(true);
        List<String> profileNames = clientProfiles.getProfiles().stream().map(ClientProfileRepresentation::getName).toList();
        assertTrue(profileNames.contains(expProfile), "Expected profile not in: " + profileNames);

        String expPolicy = "oid4vci-offer-required";
        ClientPoliciesRepresentation clientPolicies = testRealm.admin().clientPoliciesPoliciesResource().getPolicies();
        List<String> policyNames = clientPolicies.getPolicies().stream().map(ClientPolicyRepresentation::getName).toList();
        assertTrue(policyNames.contains(expPolicy), "Expected policy not in: " + policyNames);
    }

    @Test
    public void testCredentialByScope() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        CredentialResponse credResponse = wallet.fetchCredentialByScope(ctx, ctx.getScope())
                .getCredentialResponse();

        verifyCredentialResponse(ctx, ctx.getHolder(), credResponse);
    }

    @Test
    public void testCredentialByScope_AuthDetails() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        CredentialIssuer issuerMetadata = wallet.getIssuerMetadata(ctx);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(ctx.getCredentialConfigurationId());
        authDetail.setLocations(List.of(issuerMetadata.getCredentialIssuer()));

        // Send AuthorizationRequest
        //
        AuthorizationEndpointResponse authResponse = wallet
                .authorizationRequest()
                .scope(ctx.getScope())
                .authorizationDetails(authDetail)
                .send(ctx.getHolder(), TEST_PASSWORD);
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
                .proofs(wallet.generateJwtProof(ctx))
                .send().getCredentialResponse();

        verifyCredentialResponse(ctx, ctx.getHolder(), credResponse);
    }

    @Test
    public void testCredentialByScope_ClientPolicy() {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        PredicateCredentialClientPolicy offerRequiredPolicy = VC_POLICY_CREDENTIAL_OFFER_REQUIRED;

        Function<List<Boolean>, Boolean> runner = (params) -> {
            Boolean createOffer = params.get(0);
            Boolean policyEnabled = params.get(1);
            Boolean scopeEnabled = params.get(2);

            // Set client policy 'oid4vci-offer-required'
            //
            Boolean wasPolicyEnabled = getClientPolicy(offerRequiredPolicy.getName()).isEnabled();
            setClientPolicyEnabled(offerRequiredPolicy.getName(), policyEnabled);

            // Set client scope attribute 'vc.policy.offer.required'
            //
            var credScope = ctx.getCredentialScope();
            Boolean wasScopeEnabled = credScope.getCredentialPolicyValue(offerRequiredPolicy);
            credScope.setCredentialPolicyValue(offerRequiredPolicy, scopeEnabled);
            updateCredentialScope(credScope);

            try {
                // Build AuthorizationRequest
                //
                AuthorizationEndpointRequest authRequest = wallet
                        .authorizationRequest()
                        .scope(ctx.getScope());

                if (createOffer) {
                    CredentialsOffer credOffer = wallet.createCredentialOffer(ctx, req -> {
                        req.credentialConfigurationId(ctx.getCredentialConfigurationId());
                        req.preAuthorized(false);
                    });
                    assertNotNull(credOffer, "No credOffer");

                    String issuerState = credOffer.getIssuerState();
                    assertNotNull(issuerState, "No issuerState");
                    authRequest.issuerState(issuerState);
                }

                // Send AuthorizationRequest
                //
                if (authRequest.openLoginForm()) {
                    authRequest.send(ctx.getHolder(), TEST_PASSWORD);
                } else {
                    AuthorizationEndpointResponse authResponse = authRequest.parseLoginResponse();
                    String errorDescription = authResponse.getErrorDescription();
                    assertTrue(errorDescription.contains("rejected by policy oid4vci-offer-required"), errorDescription);
                    return false;
                }

                AuthorizationEndpointResponse authResponse = authRequest.parseLoginResponse();
                String authCode = authResponse.getCode();
                assertNotNull(authCode, "No authCode");

                // Build and send AccessTokenRequest
                //
                AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode).send();

                String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
                assertNotNull(accessToken, "No accessToken");

                String authorizedIdentifier = ctx.getAuthorizedCredentialIdentifier();
                assertNotNull(authorizedIdentifier,"No authorized credential identifier");

                CredentialResponse credentialResponse = wallet.credentialRequest(ctx, accessToken)
                        .credentialIdentifier(authorizedIdentifier)
                        .proofs(wallet.generateJwtProof(ctx))
                        .send()
                        .getCredentialResponse();
                assertFalse(credentialResponse.getCredentials().isEmpty(), "Credentials expected");
                return true;
            } finally {
                setClientPolicyEnabled(offerRequiredPolicy.getName(), wasPolicyEnabled);

                credScope.setCredentialPolicyValue(offerRequiredPolicy, wasScopeEnabled);
                updateCredentialScope(credScope);

                wallet.logout(ctx.getHolder());
            }
        };

        // Verification matrix (createOffer, policyEnabled, scopeEnabled)
        //
        assertTrue(runner.apply(List.of(false, false, false)), "Offer not required");
        assertFalse(runner.apply(List.of(false, false, true)), "Offer required");
        assertFalse(runner.apply(List.of(false, true, false)), "Offer required");
        assertFalse(runner.apply(List.of(false, true, true)), "Offer required");
        assertTrue(runner.apply(List.of(true, false, false)), "Offer not required");
        assertTrue(runner.apply(List.of(true, false, true)), "Offer required");
        assertTrue(runner.apply(List.of(true, true, false)), "Offer required");
        assertTrue(runner.apply(List.of(true, true, true)), "Offer required");
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
