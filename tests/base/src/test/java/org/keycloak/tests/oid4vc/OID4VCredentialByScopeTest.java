package org.keycloak.tests.oid4vc;

import java.net.URI;
import java.util.List;
import java.util.function.BiFunction;

import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.ClientPoliciesPoliciesResource;
import org.keycloak.protocol.oid4vc.clientpolicy.PredicateCredentialClientPolicy;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.VCTestServerConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.protocol.oid4vc.clientpolicy.CredentialClientPolicies.VC_POLICY_CREDENTIAL_OFFER_REQUIRED;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = VCTestServerConfig.class)
public class OID4VCredentialByScopeTest extends OID4VCIssuerTestBase {

    @Test
    public void testNoOffer_Scope() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        CredentialResponse credResponse = wallet.fetchCredentialByScope(ctx, ctx.getScope())
                .getCredentialResponse();

        verifyCredentialResponse(ctx, ctx.getHolder(), credResponse);
    }

    @Test
    public void testNoOffer_Scope_AuthDetails() throws Exception {

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
    public void testNoOffer_Scope_RequireOfferPolicy() {

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
            var credScope = ctx.getCredentialScope();
            Boolean wasScopeEnabled = credScope.getCredentialPolicyValue(offerRequiredPolicy);
            credScope.setCredentialPolicyValue(offerRequiredPolicy, scopeEnabled);
            updateCredentialScope(credScope);

            try {
                // Send AuthorizationRequest
                //
                AuthorizationEndpointResponse authResponse = wallet
                        .authorizationRequest()
                        .scope(ctx.getScope())
                        .send(ctx.getHolder(), TEST_PASSWORD);
                String authCode = authResponse.getCode();
                assertNotNull(authCode, "No authCode");

                // Build and send AccessTokenRequest
                //
                AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode).send();

                if (policyEnabled || scopeEnabled) {
                    AssertionError error = assertThrows(AssertionError.class,
                            () -> wallet.validateHolderAccessToken(ctx, tokenResponse));
                    assertTrue(error.getMessage().contains("Authorization request rejected by policy oid4vci-offer-required for scope jwt-credential"), error.getMessage());
                    return false;
                } else {
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
                }
            } finally {
                clientPolicy.setEnabled(wasPolicyEnabled);
                clientPoliciesResource.updatePolicies(policies);

                credScope.setCredentialPolicyValue(offerRequiredPolicy, wasScopeEnabled);
                updateCredentialScope(credScope);

                wallet.logout(ctx.getHolder());
            }
        };

        // Verification matrix (policyEnabled, scopeEnabled) - We use an OR condition
        //
        assertTrue(runner.apply(false, false), "Offer not required");
        assertFalse(runner.apply(false, true), "Offer required");
        assertFalse(runner.apply(true, false), "Offer required");
        assertFalse(runner.apply(true, true), "Offer required");
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
