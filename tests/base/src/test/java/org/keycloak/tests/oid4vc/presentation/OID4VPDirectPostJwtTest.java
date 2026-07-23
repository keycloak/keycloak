/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.oid4vc.presentation;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.OID4VCConstants;
import org.keycloak.broker.oid4vp.OID4VPIdentityProviderConfig;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.tests.oid4vc.OID4VCBasicWallet;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vpDirectPostResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Drives the OID4VP wallet login for the encrypted {@code direct_post.jwt} response mode, where the
 * wallet encrypts a JWE carrying the {@code vp_token} and {@code state} to the ephemeral key the
 * verifier publishes in the request object client metadata, same device.
 */
@KeycloakIntegrationTest(config = PresentationServerConfig.class)
public class OID4VPDirectPostJwtTest extends OID4VPVerifierTestBase {

    @Override
    @TestSetup
    public void configureTestRealm() {
        super.configureTestRealm();
        createVerifierIdp(Map.of(
                OID4VPIdentityProviderConfig.TRUSTED_ISSUER_JWKS, realmSigningJwks(),
                OID4VPIdentityProviderConfig.DCQL_QUERY, dcqlQuery(),
                OID4VPIdentityProviderConfig.RESPONSE_MODE, OID4VCConstants.RESPONSE_MODE_DIRECT_POST_JWT,
                OID4VPIdentityProviderConfig.PRINCIPAL_ATTRIBUTE, "email"));
    }

    @Test
    public void requestObjectAdvertisesEncryptedResponseMode() throws Exception {
        JsonNode request = requestObject();

        Assertions.assertEquals("direct_post.jwt", request.path("response_mode").asText());
        JsonNode metadata = request.path("client_metadata");
        // HAIP requires advertising both A128GCM and A256GCM.
        Assertions.assertEquals(List.of("A128GCM", "A256GCM"), JsonSerialization.mapper.convertValue(
                metadata.path("encrypted_response_enc_values_supported"), List.class));

        JsonNode jwk = OID4VCBasicWallet.encryptionJwk(request);
        Assertions.assertEquals("EC", jwk.path("kty").asText());
        Assertions.assertEquals("P-256", jwk.path("crv").asText());
        Assertions.assertEquals("enc", jwk.path("use").asText());
        Assertions.assertTrue(jwk.hasNonNull("kid"), "Ephemeral encryption key must carry a kid");
        Assertions.assertTrue(jwk.hasNonNull("x") && jwk.hasNonNull("y"), "EC public key must expose x and y");
    }

    @Test
    public void happyFlowDecryptsPresentationAndContinuesLogin() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();
        Oid4vpDirectPostResponse response =
                wallet.directPostJwt(request, wallet.encryptedResponse(request, wallet.present(credential, request)));
        assertNotCacheable(response);

        driver.open(response.getRedirectUri());
        assertLoginContinued();
    }

    @Test
    public void directPostRejectsUnencryptedPresentation() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();

        // A plain post for a flow that promised encryption would be a downgrade.
        Oid4vpDirectPostResponse response = wallet.directPost(request, wallet.present(credential, request));
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("invalid_request", response.getError());
    }

    @Test
    public void directPostRejectsMalformedEncryptedResponse() throws Exception {
        JsonNode request = requestObject();

        Oid4vpDirectPostResponse response = wallet.directPostJwt(request, "not-a-jwe");
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("invalid_request", response.getError());
    }

    @Test
    public void directPostRejectsEncryptedResponseWithoutEncAlgorithm() throws Exception {
        JsonNode request = requestObject();
        String kid = OID4VCBasicWallet.encryptionJwk(request).path("kid").asText();

        // A structurally valid JWE that names the issued key but omits the enc header.
        String header = Base64Url.encode(("{\"alg\":\"ECDH-ES\",\"kid\":\"" + kid + "\"}")
                .getBytes(StandardCharsets.UTF_8));
        String jwe = header + ".." + Base64Url.encode(new byte[12]) + "." + Base64Url.encode(new byte[16])
                + "." + Base64Url.encode(new byte[16]);

        Oid4vpDirectPostResponse response = wallet.directPostJwt(request, jwe);
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("invalid_request", response.getError());
    }

    @Test
    public void directPostRejectsBlankVpTokenInEncryptedResponse() throws Exception {
        JsonNode request = requestObject();

        String encrypted = wallet.encryptResponse(request, "", request.path("state").asText(),
                OID4VCBasicWallet.encryptionJwk(request), OID4VCBasicWallet.encryptionJwk(request).path("kid").asText());
        Oid4vpDirectPostResponse response = wallet.directPostJwt(request, encrypted);
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("invalid_request", response.getError());
    }

    @Test
    public void directPostRejectsResponseEncryptedToWrongKey() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();
        String kid = OID4VCBasicWallet.encryptionJwk(request).path("kid").asText();

        // Encrypted to a different key than advertised while claiming the advertised kid, so the
        // derived content key mismatches and decryption fails.
        JWK wrongKey = JWKBuilder.create().kid(kid).algorithm("ECDH-ES")
                .ec(KeyUtils.generateEcKeyPair("secp256r1").getPublic(), KeyUse.ENC);
        String encrypted = wallet.encryptResponse(request, wallet.present(credential, request),
                request.path("state").asText(), JsonSerialization.mapper.valueToTree(wrongKey), kid);
        Oid4vpDirectPostResponse response = wallet.directPostJwt(request, encrypted);
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("invalid_request", response.getError());
    }

    @Test
    public void directPostRejectsContentEncryptionOutsideHaipSet() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();
        JsonNode jwk = OID4VCBasicWallet.encryptionJwk(request);

        // A192GCM is a valid JWE algorithm but outside the advertised HAIP pair.
        String encrypted = wallet.encryptResponse(request, wallet.present(credential, request),
                request.path("state").asText(), jwk, jwk.path("kid").asText(), "A192GCM");
        Oid4vpDirectPostResponse response = wallet.directPostJwt(request, encrypted);
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("invalid_request", response.getError());
    }

    @Test
    public void directPostRejectsPresentationWithWrongNonce() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();

        // The response decrypts, but the presentation then fails verification like a plain direct_post.
        String encrypted = wallet.encryptedResponse(request, wallet.present(credential, request, "not-the-issued-nonce"));
        Oid4vpDirectPostResponse response = wallet.directPostJwt(request, encrypted);
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("access_denied", response.getError());
    }

    @Test
    public void directPostRejectsUnknownEncryptionKey() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();

        // Encrypted to a key id the verifier never issued.
        String encrypted = wallet.encryptResponse(request, wallet.present(credential, request),
                request.path("state").asText(), OID4VCBasicWallet.encryptionJwk(request), UUID.randomUUID().toString());
        Oid4vpDirectPostResponse response = wallet.directPostJwt(request, encrypted);
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("invalid_request", response.getError());
    }

    @Test
    public void invalidResponseModeIsRejectedOnUpdate() {
        Assertions.assertThrows(BadRequestException.class,
                () -> updateIdpConfig(OID4VPIdentityProviderConfig.RESPONSE_MODE, "fragment"));
    }

}
