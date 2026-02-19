/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.tests.oid4vc;

import java.util.concurrent.Callable;

import org.keycloak.protocol.oid4vc.policy.CredentialPolicy;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.VCTestServerConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.protocol.oid4vc.policy.CredentialPolicies.VC_POLICY_CREDENTIAL_OFFER_REQUIRED;
import static org.keycloak.protocol.oid4vc.policy.CredentialPolicies.VC_POLICY_CREDENTIAL_OFFER_TXCODE_REQUIRED;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test CredentialPolicies
 */
@KeycloakIntegrationTest(config = VCTestServerConfig.class)
public class OID4VCCredentialPolicyTest extends OID4VCIssuerTestBase {

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
    public void testCredentialPolicy_OfferRequired() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        runWithPolicyValue(ctx, VC_POLICY_CREDENTIAL_OFFER_REQUIRED, true, () -> {

            // No Authorization Details
            AuthorizationEndpointResponse authResponse = wallet
                    .authorizationRequest()
                    .scope(ctx.credScopeName)
                    .send(ctx.holder, "password");

            String authCode = authResponse.getCode();
            assertNotNull(authCode, "No authCode");

            // Build and send AccessTokenRequest
            //
            AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode).send();
            AssertionError error = assertThrows(AssertionError.class, () -> wallet.validateHolderAccessToken(ctx, tokenResponse));
            assertTrue(error.getMessage().contains("Credential offer is required by policy: oid4vci-offer-required"), error.getMessage());
            return null;
        });
    }

    @Test
    public void testCredentialPolicy_TxCodeRequired() throws Exception {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        runWithPolicyValue(ctx, VC_POLICY_CREDENTIAL_OFFER_TXCODE_REQUIRED, true, () -> {

            IllegalStateException error = assertThrows(IllegalStateException.class, () -> wallet.createPreAuthCredentialOffer(ctx, null, false));
            assertTrue(error.getMessage().contains("TxCode required policy: oid4vci-offer-txcode-required"), error.getMessage());

            return null;
        });
    }

    <T> void runWithPolicyValue(OID4VCTestContext ctx, CredentialPolicy<T> policy, T value, Callable<Void> worker) throws Exception {
        T wasValue = wallet.getCredentialPolicyValue(ctx.credentialScope, policy);
        try {
            wallet.setCredentialPolicyValue(ctx.credentialScope, policy, value);
            worker.call();
        } finally {
            wallet.setCredentialPolicyValue(ctx.credentialScope, policy, wasValue);
        }
    }
}
