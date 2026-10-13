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

import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.broker.oid4vp.OID4VPIdentityProviderConfig;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vpDirectPostResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the OID4VP wallet login for the cross device flow: the login page shows a QR code whose
 * request_uri carries the {@code flow=cross_device} signal, the wallet on another device posts the
 * presentation and receives an empty JSON object, and the browser learns about the completion by
 * polling the status endpoint.
 */
@KeycloakIntegrationTest(config = PresentationServerConfig.class)
public class OID4VPCrossDeviceTest extends OID4VPVerifierTestBase {

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
    public void loginPageShowsQrCodeSharingTheStateWithTheSameDeviceLink() {
        String sameDeviceUrl = openWalletPage();
        String crossDeviceUrl = currentCrossDeviceWalletUrl();

        assertEquals(stateOf(sameDeviceUrl), stateOf(crossDeviceUrl),
                "Both flows must share the state of the rendered login page");
        assertFalse(wallet.requestUri(sameDeviceUrl).contains("flow="),
                "The same device request_uri must not carry the cross device signal");
        assertTrue(wallet.requestUri(crossDeviceUrl).contains("flow=cross_device"),
                "The cross device request_uri must carry the cross device signal");

        Matcher qrImage = Pattern.compile("src=\"data:image/png;base64, ?([^\"]+)\"")
                .matcher(driver.driver().getPageSource());
        assertTrue(qrImage.find(), "Wallet login page did not contain the QR code image");
        byte[] png = Base64.getDecoder().decode(qrImage.group(1).trim());
        assertTrue(png.length > 8 && png[1] == 'P' && png[2] == 'N' && png[3] == 'G',
                "The QR code image is not a PNG");
    }

    @Test
    public void requestObjectAdvertisesTheFlaggedResponseUriOnlyForCrossDevice() throws Exception {
        JsonNode sameDevice = requestObject();
        assertFalse(sameDevice.path("response_uri").asText().contains("flow="),
                "The same device response_uri must not carry the cross device signal");

        JsonNode crossDevice = crossDeviceRequestObject();
        assertTrue(crossDevice.path("response_uri").asText().endsWith("?flow=cross_device"),
                "The cross device response_uri must carry the cross device signal, was: "
                        + crossDevice.path("response_uri").asText());
    }

    @Test
    public void happyFlowCompletesViaStatusPolling() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = crossDeviceRequestObject();
        String state = request.path("state").asText();

        assertEquals("pending", pollStatus(state).path("status").asText());

        Oid4vpDirectPostResponse response = wallet.directPost(request, wallet.present(credential, request));
        assertNotCacheable(response);
        assertEquals(200, response.getStatusCode());
        assertFalse(response.hasRedirectUri(),
                "A cross device direct_post must not hand the redirect to the remote wallet");
        assertTrue(response.getBody().isEmpty(),
                "A cross device direct_post must be answered with an empty JSON object");

        JsonNode status = pollStatus(state);
        assertEquals("complete", status.path("status").asText());
        driver.open(status.path("redirectUri").asText());
        assertLoginContinued();
    }

    @Test
    public void statusRevealsNothingWithoutTheBrowserSessionCookie() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = crossDeviceRequestObject();
        String state = request.path("state").asText();
        wallet.directPost(request, wallet.present(credential, request));

        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(statusUrl(state)))) {
            JsonNode status = JsonSerialization.readValue(
                    EntityUtils.toString(response.getEntity()), JsonNode.class);
            assertEquals("error", status.path("status").asText());
            assertFalse(status.has("redirectUri"),
                    "The completion must not be revealed without the browser session cookie");
        }
    }

    @Test
    public void statusIsTerminalAfterCompleteAuthConsumedTheCompletion() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = crossDeviceRequestObject();
        String state = request.path("state").asText();
        wallet.directPost(request, wallet.present(credential, request));

        driver.open(pollStatus(state).path("redirectUri").asText());
        assertLoginContinued();

        assertEquals("expired", pollStatus(state).path("status").asText());
    }

    @Test
    public void sameDeviceCompletionNeverSignalsTheStatusPoll() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();
        String state = request.path("state").asText();

        assertEquals("pending", pollStatus(state).path("status").asText());
        assertTrue(wallet.directPost(request, wallet.present(credential, request)).hasRedirectUri());

        // The completion marker is error because it is not cross device
        assertEquals("error", pollStatus(state).path("status").asText());
    }

    @Test
    public void directPostConsumesTheStateExactlyOnce() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = crossDeviceRequestObject();
        String vpToken = wallet.present(credential, request);

        assertEquals(200, wallet.directPost(request, vpToken).getStatusCode());

        Oid4vpDirectPostResponse replay = wallet.directPost(request, vpToken);
        assertEquals(400, replay.getStatusCode());
        assertEquals("invalid_request", replay.getError());
    }

    @Test
    public void statusPollIsNotCacheableAndRejectsMissingState() throws Exception {
        openWalletPage();

        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(statusUrl("")))) {
            assertEquals(400, response.getStatusLine().getStatusCode());
        }
        try (CloseableHttpResponse response = httpClient.execute(
                new HttpGet(statusUrl(UUID.randomUUID().toString())))) {
            String cacheControl = response.getFirstHeader("Cache-Control").getValue();
            assertTrue(cacheControl.contains("no-store"),
                    "Status responses must not be cacheable, was: " + cacheControl);
        }
    }
}
