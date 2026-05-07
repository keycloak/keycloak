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

package org.keycloak.tests.oid4vc.verifier;

import java.util.List;

import org.keycloak.protocol.oid4vc.model.CredentialResponse.Credential;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.VCTestServerConfig;
import org.keycloak.tests.oid4vc.OID4VCTestContext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@KeycloakIntegrationTest(config = VCTestServerConfig.class)
public class OID4VCPQueryRoundtripTest extends OID4VCIssuerTestBase {

    /**
     * The flow utilizes simple redirects to pass Authorization Request and Response between the Verifier and the Wallet.
     * The Presentations are returned to the Verifier in the fragment part of the redirect URI, when Response Mode is fragment.
     */
    @Test
    public void testSameDeviceFlow() throws Exception {

        var ctx = new OID4VCTestContext(pubClient);

        // Preload the execution context with some known Credentials
        //
        wallet.fetchCredentialByScope(ctx, sdJwtTypeNaturalPersonScopeName);
        assertEquals("oid4vc_natural_person", ctx.getCredentialType());

        // Verifier constructs an Authorization Request which contains a DCQL query
        //
        var dcqlQuery = verifier.createCredentialQuery(ctx, "oid4vc_natural_person");
        assertNotNull(dcqlQuery, "No DCQL query");

        // Verifier sends an Authorization Request to the Wallet
        //
        var authResponse = verifier.authorizationRequest()
                .dcql(dcqlQuery)
                .send();

        // The Wallet returns a VPToken
        //
        String vpToken = authResponse.getVpToken();
        assertNotNull(vpToken, "No VPToken");

        // The Verifier decodes the VPToken
        List<Credential> vpRes = verifier.decodeVPToken(ctx, vpToken, () ->
            wallet.matchCredentialPresentationRequest(ctx, vpToken)
        );
        assertNotNull(vpRes, "No matched Credentials");

        // Verifier validates the Credential Presentation Response
        //
        boolean outcome = verifier.validateCredentialPresentationResponse(ctx, vpRes);
        assertTrue(outcome, "Not verified");
    }

    /**
     * The Verifier prepares an Authorization Request and renders it as a QR Code.
     * The End-User then uses the Wallet to scan the QR Code.
     * The Presentations are sent to the Verifier in a direct HTTP POST request to a URL controlled by the Verifier.
     * The flow uses the Response Type vp_token in conjunction with the Response Mode direct_post.
     */
    @Test
    public void testCrossDeviceFlow() throws Exception {

        var ctx = new OID4VCTestContext(pubClient);

        // Preload the execution context with some known Credentials
        //
        wallet.fetchCredentialByScope(ctx, sdJwtTypeNaturalPersonScopeName);
        assertEquals("oid4vc_natural_person", ctx.getCredentialType());

        // Verifier creates a Presentation Request
        //
        var vpReqUri = verifier.createCredentialPresentationRequestUri(ctx, "oid4vc_natural_person");
        assertNotNull(vpReqUri, "No CredentialPresentationRequestUri");

        // Wallet fetches the Credential Presentation Request
        //
        var vpReqObj = wallet.fetchCredentialPresentationRequest(ctx, vpReqUri);
        assertNotNull(vpReqObj, "No CredentialPresentationRequest");

        // Wallet selects the Credentials that match the Credential Presentation Request
        //
        List<Credential> vpRes = wallet.matchCredentialPresentationRequest(ctx, vpReqObj);
        assertNotNull(vpRes, "No matched Credentials");

        // Verifier validates the Credential Presentation Response
        //
        boolean outcome = verifier.validateCredentialPresentationResponse(ctx, vpRes);
        assertTrue(outcome, "Not verified");
    }
}
