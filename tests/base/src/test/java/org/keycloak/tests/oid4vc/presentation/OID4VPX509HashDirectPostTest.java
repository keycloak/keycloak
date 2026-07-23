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

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.keycloak.OID4VCConstants;
import org.keycloak.broker.oid4vp.OID4VPIdentityProviderConfig;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vpDirectPostResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vpRequestObjectResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_SIGNING_ALG;

/**
 * Drives the OID4VP wallet login for the profile of an SD-JWT VC presented with an {@code x509_hash}
 * client identifier, a signed request object fetched by {@code request_uri}, and an unencrypted
 * {@code direct_post} response, same device.
 */
@KeycloakIntegrationTest(config = PresentationServerConfig.class)
public class OID4VPX509HashDirectPostTest extends OID4VPVerifierTestBase {

    @Override
    @TestSetup
    public void configureTestRealm() {
        super.configureTestRealm();
        createVerifierIdp(Map.of(
                OID4VPIdentityProviderConfig.TRUSTED_ISSUER_JWKS, realmSigningJwks(),
                OID4VPIdentityProviderConfig.DCQL_QUERY, dcqlQuery(),
                OID4VPIdentityProviderConfig.PRINCIPAL_ATTRIBUTE, "email"));
    }

    @Test
    public void requestObjectIsSignedByRealmKeyAndWellFormed() throws Exception {
        JWSInput signedRequest = new JWSInput(wallet.fetchRequestObject(wallet.requestUri(openWalletPage())).getRequestObject());

        JWSHeader header = signedRequest.getHeader();
        Assertions.assertEquals(OID4VCConstants.REQUEST_OBJECT_TYPE, header.getType());
        List<String> x5c = header.getX5c();
        Assertions.assertNotNull(x5c, "Request object must publish the verifier certificate in x5c");
        Assertions.assertFalse(x5c.isEmpty());
        X509Certificate leaf = PemUtils.decodeCertificate(x5c.get(0));
        Assertions.assertTrue(verifyEs256(signedRequest, leaf),
                "Request object signature does not match the certificate in x5c");

        JsonNode request = JsonSerialization.readValue(signedRequest.getContent(), JsonNode.class);
        Assertions.assertTrue(request.path("client_id").asText().startsWith("x509_hash:"));
        Assertions.assertEquals("vp_token", request.path("response_type").asText());
        Assertions.assertEquals("direct_post", request.path("response_mode").asText());
        Assertions.assertTrue(request.hasNonNull("nonce"));
        Assertions.assertEquals(JsonSerialization.readValue(dcqlQuery(), JsonNode.class), request.path("dcql_query"));
    }

    @Test
    public void happyFlowVerifiesPresentationAndContinuesLogin() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();
        Oid4vpDirectPostResponse response = wallet.directPost(request, wallet.present(credential, request));
        assertNotCacheable(response);

        driver.open(response.getRedirectUri());
        assertLoginContinued();
    }

    @Test
    public void completeAuthRejectsUnknownResponseCode() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();
        String completeAuth = wallet.directPost(request, wallet.present(credential, request)).getRedirectUri();

        driver.open(completeAuth.replaceAll("response_code=[^&]+", "response_code=" + UUID.randomUUID()));
        assertLoginRejected();
    }

    @Test
    public void completeAuthRejectsMismatchedBrowserSession() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();
        String completeAuth = wallet.directPost(request, wallet.present(credential, request)).getRedirectUri();

        driver.cookies().deleteAll();
        driver.open(completeAuth);
        assertLoginRejected();
    }

    @Test
    public void completeAuthResponseCodeIsSingleUse() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();
        String completeAuth = wallet.directPost(request, wallet.present(credential, request)).getRedirectUri();

        driver.open(completeAuth);
        assertLoginContinued();
        driver.open(completeAuth);
        assertLoginRejected();
    }

    @Test
    public void directPostRejectsPresentationWithWrongNonce() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();

        Oid4vpDirectPostResponse response = wallet.directPost(request, wallet.present(credential, request, "not-the-issued-nonce"));
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("access_denied", response.getError());
    }

    @Test
    public void requestObjectRejectsUnknownState() throws Exception {
        String unknownStateUri = wallet.requestUri(openWalletPage()).replaceAll("[^/]+$", UUID.randomUUID().toString());

        Oid4vpRequestObjectResponse response = wallet.fetchRequestObject(unknownStateUri);
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("invalid_request", response.getError());
    }

    @Test
    public void directPostRejectsMissingParameters() throws Exception {
        JsonNode request = requestObject();

        Oid4vpDirectPostResponse response = oauth.oid4vc()
                .oid4vpDirectPostRequest(request.path("response_uri").asText()).send();
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("invalid_request", response.getError());
    }

    @Test
    public void directPostRejectsUnsupportedSignatureAlgorithm() throws Exception {
        testRealm.updateClientScope(sdJwtTypeCredentialScope.getId(),
                scope -> scope.attribute(VC_SIGNING_ALG, Algorithm.RS256));
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();

        Oid4vpDirectPostResponse response = wallet.directPost(request, wallet.present(credential, request));
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("access_denied", response.getError());
        Assertions.assertTrue(response.getErrorDescription().contains("algorithm"),
                "Expected an unsupported algorithm error, was: " + response.getErrorDescription());
    }

    @Test
    public void loginFailsWhenActiveSigningKeyHasNoCertificate() {
        addActiveEs256KeyWithoutCertificate();

        driver.open(authUrl());
        Assertions.assertFalse(driver.driver().getPageSource().contains("openid4vp://"),
                "Login must not start when the active ES256 key has no certificate");
    }

    @Test
    public void configuredSigningKeyIdPinsTheRequestSigningKey() throws Exception {
        OID4VCTestContext credential = issueCredential();
        updateIdpConfig(OID4VPIdentityProviderConfig.SIGNING_KEY_ID, verifierSigningKeyKid());
        disableVerifierSigningKey();
        addActiveEs256KeyWithoutCertificate();

        JWSInput signedRequest = new JWSInput(wallet.fetchRequestObject(wallet.requestUri(openWalletPage())).getRequestObject());
        X509Certificate leaf = PemUtils.decodeCertificate(signedRequest.getHeader().getX5c().get(0));
        Assertions.assertEquals(verifierSigningKeyPublicKeyPem(), PemUtils.encodeKey(leaf.getPublicKey()),
                "Request object is not signed with the pinned signing key");
        Assertions.assertTrue(verifyEs256(signedRequest, leaf),
                "Request object signature does not match the certificate in x5c");

        JsonNode request = JsonSerialization.readValue(signedRequest.getContent(), JsonNode.class);
        String completeAuth = wallet.directPost(request, wallet.present(credential, request)).getRedirectUri();

        driver.open(completeAuth);
        assertLoginContinued();
    }

    @Test
    public void loginFailsForUnknownSigningKeyId() {
        updateIdpConfig(OID4VPIdentityProviderConfig.SIGNING_KEY_ID, "no-such-kid");

        driver.open(authUrl());
        Assertions.assertFalse(driver.driver().getPageSource().contains("openid4vp://"),
                "Login must not start when the configured signing key id matches no realm key");
    }

    @Test
    public void directPostRejectsEncryptedResponseForPlainFlow() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();
        String state = request.path("state").asText();

        // The flow never advertised an encryption key, so a JWE naming its state as kid must not
        // resolve one.
        JWK attackerKey = JWKBuilder.create().kid(state).algorithm("ECDH-ES")
                .ec(KeyUtils.generateEcKeyPair("secp256r1").getPublic(), KeyUse.ENC);
        String encrypted = wallet.encryptResponse(request, wallet.present(credential, request), state,
                JsonSerialization.mapper.valueToTree(attackerKey), state);
        Oid4vpDirectPostResponse response = wallet.directPostJwt(request, encrypted);
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("invalid_request", response.getError());
    }

    @Test
    public void directPostRejectsMultipleCredentials() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();
        String vpToken = wallet.present(credential, request);

        Oid4vpDirectPostResponse response = wallet.directPost(request, "[\"" + vpToken + "\",\"" + vpToken + "\"]");
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("access_denied", response.getError());
    }

    @Test
    public void directPostRejectsNonScalarPrincipal() throws Exception {
        // address is an object claim, so configuring it as the principal must be rejected rather than
        // mapped onto an empty user id.
        updatePrincipalAttribute("address");
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();

        Oid4vpDirectPostResponse response = wallet.directPost(request, wallet.present(credential, request));
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("access_denied", response.getError());
        Assertions.assertTrue(response.getErrorDescription().contains("principal attribute"),
                "Expected a principal attribute error, was: " + response.getErrorDescription());
    }

    private void assertLoginRejected() {
        String page = driver.driver().getPageSource();
        Assertions.assertTrue(page.contains("kc-error-message"),
                "Expected the login error page, was: " + driver.driver().getCurrentUrl());
        Assertions.assertTrue(page.contains("Session not active"),
                "Expected the session not active error, was: " + page);
    }
}
