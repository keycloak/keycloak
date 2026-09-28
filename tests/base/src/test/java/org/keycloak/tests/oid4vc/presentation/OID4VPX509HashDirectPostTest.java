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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.keycloak.OID4VCConstants;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oid4vp.OID4VPIdentityProviderConfig;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.oid4vc.UserVerifiableCredentialRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vpDirectPostResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vpRequestObjectResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_SIGNING_ALG;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        createCompletionHolder("bob");
        createCompletionHolder("carol");
        createCompletionHolder("dave");
    }

    @Test
    public void requestObjectIsSignedByRealmKeyAndWellFormed() throws Exception {
        JWSInput signedRequest = new JWSInput(wallet.fetchRequestObject(wallet.requestUri(openWalletPage())).getRequestObject());

        JWSHeader header = signedRequest.getHeader();
        assertEquals(OID4VCConstants.REQUEST_OBJECT_TYPE, header.getType());
        List<String> x5c = header.getX5c();
        assertNotNull(x5c, "Request object must publish the verifier certificate in x5c");
        assertFalse(x5c.isEmpty());
        X509Certificate leaf = PemUtils.decodeCertificate(x5c.get(0));
        assertTrue(verifyEs256(signedRequest, leaf),
                "Request object signature does not match the certificate in x5c");

        JsonNode request = JsonSerialization.readValue(signedRequest.getContent(), JsonNode.class);
        assertTrue(request.path("client_id").asText().startsWith("x509_hash:"));
        assertEquals("vp_token", request.path("response_type").asText());
        assertEquals("direct_post", request.path("response_mode").asText());
        assertTrue(request.hasNonNull("nonce"));
        assertEquals(JsonSerialization.readValue(dcqlQuery(), JsonNode.class), request.path("dcql_query"));
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
        assertEquals(400, response.getStatusCode());
        assertEquals("access_denied", response.getError());
    }

    @Test
    public void requestObjectRejectsUnknownState() throws Exception {
        String unknownStateUri = wallet.requestUri(openWalletPage()).replaceAll("[^/]+$", UUID.randomUUID().toString());

        Oid4vpRequestObjectResponse response = wallet.fetchRequestObject(unknownStateUri);
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_request", response.getError());
    }

    @Test
    public void directPostRejectsMissingParameters() throws Exception {
        JsonNode request = requestObject();

        Oid4vpDirectPostResponse response = oauth.oid4vc()
                .oid4vpDirectPostRequest(request.path("response_uri").asText()).send();
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_request", response.getError());
    }

    @Test
    public void directPostRejectsUnsupportedSignatureAlgorithm() throws Exception {
        testRealm.updateClientScope(sdJwtTypeCredentialScope.getId(),
                scope -> scope.attribute(VC_SIGNING_ALG, Algorithm.RS256));
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();

        Oid4vpDirectPostResponse response = wallet.directPost(request, wallet.present(credential, request));
        assertEquals(400, response.getStatusCode());
        assertEquals("access_denied", response.getError());
        assertTrue(response.getErrorDescription().contains("algorithm"),
                "Expected an unsupported algorithm error, was: " + response.getErrorDescription());
    }

    @Test
    public void loginFailsWhenActiveSigningKeyHasNoCertificate() {
        addActiveEs256KeyWithoutCertificate();

        driver.open(authUrl());
        assertFalse(driver.driver().getPageSource().contains("openid4vp://"),
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
        assertEquals(verifierSigningKeyPublicKeyPem(), PemUtils.encodeKey(leaf.getPublicKey()),
                "Request object is not signed with the pinned signing key");
        assertTrue(verifyEs256(signedRequest, leaf),
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
        assertFalse(driver.driver().getPageSource().contains("openid4vp://"),
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
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_request", response.getError());
    }

    @Test
    public void directPostRejectsMultipleCredentials() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();
        String vpToken = wallet.present(credential, request);

        Oid4vpDirectPostResponse response = wallet.directPost(request, "[\"" + vpToken + "\",\"" + vpToken + "\"]");
        assertEquals(400, response.getStatusCode());
        assertEquals("access_denied", response.getError());
    }

    @Test
    public void completeAuthRejectsNonScalarPrincipal() throws Exception {
        // address is an object claim, so configuring it as the principal must fail the login rather
        // than map onto an empty user id. The principal is resolved when the browser completes the
        // login, so the wallet post succeeds and the browser sees the error page.
        updatePrincipalAttribute("address");
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();

        Oid4vpDirectPostResponse response = wallet.directPost(request, wallet.present(credential, request));
        assertEquals(200, response.getStatusCode());

        driver.open(response.getRedirectUri());
        assertTrue(driver.driver().getPageSource().contains("kc-error-message"),
                "Expected the login error page, was: " + driver.driver().getCurrentUrl());
    }

    @Test
    public void credentialClaimsAreMappedOnCompletedLogin() throws Exception {
        OID4VCTestContext credential = issueCredential("bob");
        UserRepresentation holder = deleteHolder("bob");
        String street = holder.getAttributes().get("address_street_address").get(0);
        String locality = holder.getAttributes().get("address_locality").get(0);

        UserRepresentation user = completeFirstLogin(credential, holder);

        assertEquals(holder.getEmail(), user.getEmail());
        assertEquals(holder.getFirstName(), user.getFirstName());
        assertEquals(holder.getLastName(), user.getLastName());
        assertEquals(List.of(locality), user.getAttributes().get("locality"));
        JsonNode address = JsonSerialization.readValue(user.getAttributes().get("addressJson").get(0), JsonNode.class);
        assertEquals(street, address.path("street_address").asText());
        assertEquals(locality, address.path("locality").asText());
        assertEquals(sdJwtTypeCredentialVct, accessTokenClaim(VCT_TOKEN_CLAIM));

        endBrowserSession(user.getId());
    }

    @Test
    public void forceSyncModeHealsTamperedClaimsOnRelogin() throws Exception {
        OID4VCTestContext credential = issueCredential("carol");
        UserRepresentation holder = deleteHolder("carol");
        String street = holder.getAttributes().get("address_street_address").get(0);
        String locality = holder.getAttributes().get("address_locality").get(0);
        UserRepresentation user = completeFirstLogin(credential, holder);

        tamperMappedValues(user, holder.getEmail());
        relogin(credential, user);

        UserRepresentation healed = federatedUser(holder.getEmail());
        assertEquals(holder.getFirstName(), healed.getFirstName());
        assertEquals(List.of(locality), healed.getAttributes().get("locality"));
        JsonNode healedAddress = JsonSerialization.readValue(healed.getAttributes().get("addressJson").get(0), JsonNode.class);
        assertEquals(street, healedAddress.path("street_address").asText());

        endBrowserSession(healed.getId());
    }

    // The session note is per login state, so it must survive an existing user login whose IMPORT
    // sync mode skips the user updates.
    @Test
    public void importSyncModeKeepsSessionNoteButSkipsUserUpdatesOnRelogin() throws Exception {
        OID4VCTestContext credential = issueCredential("dave");
        UserRepresentation holder = deleteHolder("dave");
        UserRepresentation user = completeFirstLogin(credential, holder);

        updateIdpSyncMode(IdentityProviderSyncMode.IMPORT);
        try {
            tamperMappedValues(user, holder.getEmail());
            relogin(credential, user);

            assertEquals(sdJwtTypeCredentialVct, accessTokenClaim(VCT_TOKEN_CLAIM));
            UserRepresentation untouched = federatedUser(holder.getEmail());
            assertEquals("Tampered", untouched.getFirstName());
            assertNull(untouched.getAttributes().get("addressJson"));
            endBrowserSession(untouched.getId());
        } finally {
            updateIdpSyncMode(IdentityProviderSyncMode.FORCE);
        }
    }

    // The mapped claims originate from the holder user, so its representation is kept as the
    // expectation. The holder is deleted because the first broker login would otherwise stop at
    // the duplicate user check instead of creating the account.
    private UserRepresentation deleteHolder(String username) {
        UserRepresentation holder = testRealm.admin().users()
                .get(requireExistingUser(username).getId()).toRepresentation();
        deleteRealmUser(holder);
        return holder;
    }

    private UserRepresentation completeFirstLogin(OID4VCTestContext credential, UserRepresentation holder) throws Exception {
        JsonNode request = requestObject();
        driver.open(wallet.directPost(request, wallet.present(credential, request)).getRedirectUri());
        assertLoginCompleted();
        return federatedUser(holder.getEmail());
    }

    private void updateIdpSyncMode(IdentityProviderSyncMode syncMode) {
        IdentityProviderResource idp = testRealm.admin().identityProviders().get(IDP_ALIAS);
        IdentityProviderRepresentation rep = idp.toRepresentation();
        rep.getConfig().put(IdentityProviderModel.SYNC_MODE, syncMode.name());
        idp.update(rep);
    }

    // Dedicated credential holder per completed login test: completing a broker login for a
    // holder changes how every later wallet login of that holder behaves, so the shared users stay
    // untouched and the tests are order independent. Same profile shape as the shared users.
    private void createCompletionHolder(String username) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);
        user.setEmail(username + "@email.cz");
        user.setEmailVerified(true);
        user.setFirstName("Bob");
        user.setLastName("Baumeister");
        user.setAttributes(Map.of(
                "address_street_address", List.of("221B Baker Street"),
                "address_locality", List.of("London")));
        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(TEST_PASSWORD);
        password.setTemporary(false);
        user.setCredentials(List.of(password));
        String id;
        try (Response response = testRealm.admin().users().create(user)) {
            assertEquals(201, response.getStatus(), "Failed to create the completion holder");
            String location = response.getHeaderString("Location");
            id = location.substring(location.lastIndexOf('/') + 1);
        }
        UserVerifiableCredentialRepresentation grant = new UserVerifiableCredentialRepresentation();
        grant.setCredentialScopeName(sdJwtTypeCredentialScopeName);
        testRealm.admin().users().get(id).verifiableCredentials().createCredential(grant);
    }

    private void tamperMappedValues(UserRepresentation user, String username) {
        user.setFirstName("Tampered");
        Map<String, List<String>> attributes = new HashMap<>(user.getAttributes());
        attributes.remove("addressJson");
        attributes.put("locality", List.of("Nowhere"));
        user.setAttributes(attributes);
        testRealm.admin().users().get(user.getId()).update(user);

        UserRepresentation tampered = federatedUser(username);
        assertEquals("Tampered", tampered.getFirstName());
        assertNull(tampered.getAttributes().get("addressJson"));
    }

    private void assertLoginRejected() {
        String page = driver.driver().getPageSource();
        assertTrue(page.contains("kc-error-message"),
                "Expected the login error page, was: " + driver.driver().getCurrentUrl());
        assertTrue(page.contains("Session not active"),
                "Expected the session not active error, was: " + page);
    }
}
