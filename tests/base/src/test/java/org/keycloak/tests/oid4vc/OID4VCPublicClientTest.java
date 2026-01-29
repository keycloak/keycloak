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

import java.net.URI;
import java.util.List;

import org.keycloak.TokenVerifier;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.VCTestServerConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * An external wallet would generally not be trusted to keep the client_secret
 * oid4vci clients should be configured as public with pkce enabled
 */
@KeycloakIntegrationTest(config = VCTestServerConfig.class)
public class OID4VCPublicClientTest extends OID4VCIssuerTestBase {

    OID4VCBasicWallet wallet;

    @BeforeEach
    void beforeEach() {
        wallet = new OID4VCBasicWallet(keycloak, oauth);

        // Reconfigure OAuthClient
        oauth.client(pubClient.getClientId(), null);
    }

    @AfterEach
    void afterEach() {
        wallet.logout();
    }

    @Test
    public void testCredentialFromPublicClient() throws Exception {

        var ctx = new OID4VCTestContext(pubClient, jwtTypeCredentialScope);

        PkceGenerator pkce = PkceGenerator.s256();

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
                .codeChallenge(pkce)
                .send(ctx.holder, "password");
        String authCode = authResponse.getCode();

        // Build and send AccessTokenRequest
        //
        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode)
                .codeVerifier(pkce)
                .send();
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
    public void testCredentialFromPreAuthCode() throws Exception {

        var ctx = new OID4VCTestContext(pubClient, jwtTypeCredentialScope);

        // Create Pre-Authorized CredentialOffer
        //
        CredentialsOffer credOffer = wallet.createPreAuthCredentialOffer(ctx, ctx.holder);
        String preAuthCode = credOffer.getPreAuthorizedCode();

        // Redeem Pre-Authorized Code for AccessToken
        //
        AccessTokenResponse tokenResponse = wallet.preAuthAccessTokenRequest(ctx, preAuthCode).send();
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
    public void testAuthorizationRequestNoPkce() throws Exception {

        var ctx = new OID4VCTestContext(pubClient, jwtTypeCredentialScope);

        // Send AuthorizationRequest
        //
        AuthorizationEndpointResponse authResponse = wallet
                .authorizationRequest()
                .scope(ctx.credScopeName)
                .send(ctx.holder, "password");

        assertEquals("invalid_request", authResponse.getError());
        assertEquals("Missing parameter: code_challenge_method", authResponse.getErrorDescription());
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
