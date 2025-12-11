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
 *
 */

package org.keycloak.testsuite.oid4vc.issuance.signing;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.models.KeycloakContext;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.keybinding.CNonceHandler;
import org.keycloak.protocol.oid4vc.issuance.keybinding.JwtCNonceHandler;
import org.keycloak.protocol.oid4vc.model.NonceResponse;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.JsonSerialization;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pascal Knüppel
 */
public class NonceEndpointTest extends OID4VCIssuerEndpointTest {

    @Test
    public void testGetCNonce() throws Exception {
        URI baseUri = RealmsResource.realmBaseUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build(
                AbstractTestRealmKeycloakTest.TEST_REALM_NAME,
                OID4VCLoginProtocolFactory.PROTOCOL_ID);
        String cNonce = getCNonce();

        URI oid4vcUri;
        UriBuilder builder = UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT);
        oid4vcUri = RealmsResource.protocolUrl(builder).build(AbstractTestRealmKeycloakTest.TEST_REALM_NAME,
                                                              OID4VCLoginProtocolFactory.PROTOCOL_ID);
        String nonceUrl = String.format("%s/%s", oid4vcUri, OID4VCIssuerEndpoint.NONCE_PATH);

        Assert.assertNotNull(cNonce);
        // verify nonce content
        {
            TokenVerifier<JsonWebToken> verifier = TokenVerifier.create(cNonce, JsonWebToken.class);

            JWSHeader jwsHeader = verifier.getHeader();
            Assert.assertEquals(Algorithm.ES256, jwsHeader.getAlgorithm().name());
            Assert.assertNotNull(jwsHeader.getKeyId());

            JsonWebToken nonce = verifier.getToken();
            String credentialsUrl = String.format("%s/%s", oid4vcUri, OID4VCIssuerEndpoint.CREDENTIAL_PATH);
            Assert.assertEquals(List.of(credentialsUrl), Arrays.asList(nonce.getAudience()));
            Assert.assertEquals(baseUri.toString(), nonce.getIssuer());
            Assert.assertEquals(nonceUrl, nonce.getOtherClaims().get(JwtCNonceHandler.SOURCE_ENDPOINT));
            Assert.assertNotNull(nonce.getOtherClaims().get("salt"));
        }

        // do internal nonce verification by using cNonceHandler
        testingClient.server(TEST_REALM_NAME).run(session -> {
            CNonceHandler cNonceHandler = session.getProvider(CNonceHandler.class);
            KeycloakContext keycloakContext = session.getContext();
            cNonceHandler.verifyCNonce(cNonce,
                                       List.of(OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(
                                               keycloakContext)),
                                       Map.of(JwtCNonceHandler.SOURCE_ENDPOINT,
                                              OID4VCIssuerWellKnownProvider.getNonceEndpoint(keycloakContext)));
        });
    }

    @Test
    public void testDPoPNonceHeaderPresent() throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT);
        URI oid4vcBaseUri = RealmsResource.protocolUrl(uriBuilder).build(AbstractTestRealmKeycloakTest.TEST_REALM_NAME,
                OID4VCLoginProtocolFactory.PROTOCOL_ID);
        String nonceEndpointUrl = String.format("%s/%s", oid4vcBaseUri, OID4VCIssuerEndpoint.NONCE_PATH);

        // Create HTTP client and make request to nonce endpoint
        try (Client httpClient = AdminClientUtil.createResteasyClient()) {
            WebTarget target = httpClient.target(nonceEndpointUrl);
            Invocation.Builder requestBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);

            try (Response response = requestBuilder.post(null)) {
                // Verify successful response
                Assert.assertEquals("Nonce endpoint should return 200 OK",
                        Response.Status.OK.getStatusCode(), response.getStatus());

                // Verify DPoP-Nonce header is present and non-empty
                String dpopNonceHeader = response.getHeaderString(OAuth2Constants.DPOP_NONCE_HEADER);
                Assert.assertNotNull("DPoP-Nonce header must be present", dpopNonceHeader);
                Assert.assertFalse("DPoP-Nonce header must not be empty", dpopNonceHeader.trim().isEmpty());

                // Verify that DPoP nonce is different from body nonce (separate generation)
                String responseBody = response.readEntity(String.class);
                NonceResponse nonceResponse = JsonSerialization.readValue(responseBody, NonceResponse.class);
                Assert.assertNotNull("Response body should contain valid nonce response", nonceResponse);
                Assert.assertNotNull("c_nonce in response body should not be null", nonceResponse.getNonce());

                Assert.assertNotEquals("DPoP-Nonce header should be different from c_nonce in body",
                        nonceResponse.getNonce(), dpopNonceHeader);
            }
        }
    }
}
