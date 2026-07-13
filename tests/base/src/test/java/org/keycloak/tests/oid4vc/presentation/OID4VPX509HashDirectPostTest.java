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
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_SIGNING_ALG;

/**
 * Drives the OID4VP wallet login for the profile of an SD-JWT VC presented with an {@code x509_hash}
 * client identifier, a signed request object fetched by {@code request_uri}, and an unencrypted
 * {@code direct_post} response, same device.
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCDefaultServerConfig.class)
public class OID4VPX509HashDirectPostTest extends OID4VPVerifierTestBase {

    @BeforeEach
    void registerVerifier() {
        createVerifierIdp(Map.of(
                OID4VPIdentityProviderConfig.TRUSTED_ISSUER_JWKS, realmSigningJwks(),
                OID4VPIdentityProviderConfig.DCQL_QUERY, dcqlQuery(),
                OID4VPIdentityProviderConfig.PRINCIPAL_ATTRIBUTE, "email"));
    }

    private static String dcqlQuery() {
        return """
                {
                  "credentials": [
                    {
                      "id": "identity",
                      "format": "dc+sd-jwt",
                      "meta": { "vct_values": ["%s"] },
                      "claims": [{ "path": ["email"] }, { "path": ["lastName"] }]
                    }
                  ]
                }""".formatted(sdJwtTypeCredentialVct);
    }

    @Test
    public void requestObjectIsSignedByRealmKeyAndWellFormed() throws Exception {
        JWSInput signedRequest = new JWSInput(fetchRequestObject(requestUri(openWalletPage())));

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
        IssuedCredential credential = issueCredential();
        JsonNode request = requestObject();
        String completeAuth = directPost(request, present(credential, request), 200).path("redirect_uri").asText();

        driver.open(completeAuth);
        assertLoginContinued();
    }

    @Test
    public void completeAuthRejectsUnknownResponseCode() throws Exception {
        IssuedCredential credential = issueCredential();
        JsonNode request = requestObject();
        String completeAuth = directPost(request, present(credential, request), 200).path("redirect_uri").asText();

        driver.open(completeAuth.replaceAll("response_code=[^&]+", "response_code=" + UUID.randomUUID()));
        assertLoginRejected();
    }

    @Test
    public void completeAuthRejectsMismatchedBrowserSession() throws Exception {
        IssuedCredential credential = issueCredential();
        JsonNode request = requestObject();
        String completeAuth = directPost(request, present(credential, request), 200).path("redirect_uri").asText();

        driver.cookies().deleteAll();
        driver.open(completeAuth);
        assertLoginRejected();
    }

    @Test
    public void completeAuthResponseCodeIsSingleUse() throws Exception {
        IssuedCredential credential = issueCredential();
        JsonNode request = requestObject();
        String completeAuth = directPost(request, present(credential, request), 200).path("redirect_uri").asText();

        driver.open(completeAuth);
        assertLoginContinued();
        driver.open(completeAuth);
        assertLoginRejected();
    }

    @Test
    public void directPostRejectsPresentationWithWrongNonce() throws Exception {
        IssuedCredential credential = issueCredential();
        JsonNode request = requestObject();

        JsonNode body = directPost(request, present(credential, request, "not-the-issued-nonce"), 400);
        Assertions.assertEquals("access_denied", body.path("error").asText());
    }

    @Test
    public void requestObjectRejectsUnknownState() throws Exception {
        String unknownStateUri = requestUri(openWalletPage()).replaceAll("[^/]+$", UUID.randomUUID().toString());

        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(unknownStateUri))) {
            Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            JsonNode body = JsonSerialization.readValue(EntityUtils.toByteArray(response.getEntity()), JsonNode.class);
            Assertions.assertEquals("invalid_request", body.path("error").asText());
        }
    }

    @Test
    public void directPostRejectsMissingParameters() throws Exception {
        JsonNode request = requestObject();

        HttpPost post = new HttpPost(request.path("response_uri").asText());
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            JsonNode body = JsonSerialization.readValue(EntityUtils.toByteArray(response.getEntity()), JsonNode.class);
            Assertions.assertEquals("invalid_request", body.path("error").asText());
        }
    }

    @Test
    public void directPostRejectsUnsupportedSignatureAlgorithm() throws Exception {
        setCredentialScopeAttributes(sdJwtTypeCredentialScope, Map.of(VC_SIGNING_ALG, Algorithm.RS256));
        IssuedCredential credential = issueCredential();
        JsonNode request = requestObject();

        JsonNode body = directPost(request, present(credential, request), 400);
        Assertions.assertEquals("access_denied", body.path("error").asText());
        Assertions.assertTrue(body.path("error_description").asText().contains("algorithm"),
                "Expected an unsupported algorithm error, was: " + body);
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
        IssuedCredential credential = issueCredential();
        updateIdpConfig(OID4VPIdentityProviderConfig.SIGNING_KEY_ID, verifierSigningKeyKid());
        disableVerifierSigningKey();
        addActiveEs256KeyWithoutCertificate();

        JWSInput signedRequest = new JWSInput(fetchRequestObject(requestUri(openWalletPage())));
        X509Certificate leaf = PemUtils.decodeCertificate(signedRequest.getHeader().getX5c().get(0));
        Assertions.assertEquals(verifierSigningKeyPublicKeyPem(), PemUtils.encodeKey(leaf.getPublicKey()),
                "Request object is not signed with the pinned signing key");
        Assertions.assertTrue(verifyEs256(signedRequest, leaf),
                "Request object signature does not match the certificate in x5c");

        JsonNode request = JsonSerialization.readValue(signedRequest.getContent(), JsonNode.class);
        String completeAuth = directPost(request, present(credential, request), 200).path("redirect_uri").asText();

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
    public void directPostRejectsMultipleCredentials() throws Exception {
        IssuedCredential credential = issueCredential();
        JsonNode request = requestObject();
        String vpToken = present(credential, request);

        JsonNode body = directPost(request, "[\"" + vpToken + "\",\"" + vpToken + "\"]", 400);
        Assertions.assertEquals("access_denied", body.path("error").asText());
    }

    @Test
    public void directPostRejectsNonScalarPrincipal() throws Exception {
        // address is an object claim, so configuring it as the principal must be rejected rather than
        // mapped onto an empty user id.
        updatePrincipalAttribute("address");
        IssuedCredential credential = issueCredential();
        JsonNode request = requestObject();

        JsonNode body = directPost(request, present(credential, request), 400);
        Assertions.assertEquals("access_denied", body.path("error").asText());
        Assertions.assertTrue(body.path("error_description").asText().contains("principal attribute"),
                "Expected a principal attribute error, was: " + body);
    }

    private void assertLoginContinued() {
        // TODO assert the presented claims (email, given and family name) were mapped to the user
        Assertions.assertTrue(driver.driver().getCurrentUrl().contains("login-actions"),
                "Expected to continue into the login flow, was: " + driver.driver().getCurrentUrl());
    }

    private void assertLoginRejected() {
        String page = driver.driver().getPageSource();
        Assertions.assertTrue(page.contains("kc-error-message"),
                "Expected the login error page, was: " + driver.driver().getCurrentUrl());
        Assertions.assertTrue(page.contains("Session not active"),
                "Expected the session not active error, was: " + page);
    }
}
