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

package org.keycloak.testsuite.oid4vc.issuance.signing;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.keycloak.TokenVerifier;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.protocol.oid4vc.model.AuthorizationRequest;
import org.keycloak.protocol.oid4vc.model.AuthorizationRequestBuilder;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oidc.utils.PkceGenerator;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationRequestRequest;
import org.keycloak.testsuite.util.oauth.AuthorizationRequestResponse;
import org.keycloak.util.JsonSerialization;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;

import static org.keycloak.OAuth2Constants.OPENID_CREDENTIAL;
import static org.keycloak.OAuth2Constants.SCOPE_OPENID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * An external wallet would in most cases not be trusted to keep the client_secret
 * oid4vci clients should be configured as public with pkce enabled
 */
public class OID4VCPublicClientTest extends OID4VCIssuerEndpointTest {

    String clientId = "test-app-pub";

    class TestContext {
        String username;
        CredentialIssuer issuerMetadata;
        SupportedCredentialConfiguration credentialConfig;

        TestContext(String credConfigId, String username) {
            this.username = username;
            this.issuerMetadata = getCredentialIssuerMetadata();
            this.credentialConfig = issuerMetadata.getCredentialsSupported().get(credConfigId);
        }
    }

    @Before
    public void setup() {
        client = testRealm().clients().findByClientId(clientId).get(0);
        assertTrue("Is public client", client.isPublicClient());

        String redirectUri = client.getRedirectUris().get(0);
        oauth.client(client.getClientId(), null);
        oauth.redirectUri(redirectUri);
    }

    @Test
    public void testCredentialFromPublicClient() throws Exception {

        TestContext ctx = new TestContext(jwtTypeNaturalPersonScopeName, "alice");
        String credScope = ctx.credentialConfig.getScope();
        String credConfigId = ctx.credentialConfig.getId();

        String redirectUri = verifiedRedirectUri(clientId, oauth.getRedirectUri());
        PkceGenerator pkce = PkceGenerator.s256();

        // Build an AuthorizationRequest with AuthorizationDetails
        //
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(ctx.issuerMetadata.getCredentialIssuer()));

        AuthorizationRequest authRequest = new AuthorizationRequestBuilder()
                .withClientId(clientId)
                // [TODO #44320] Requires Credential scope in AuthorizationRequest although already given in AuthorizationDetails
                .withScope(SCOPE_OPENID, credScope)
                .withAuthorizationDetail(authDetail)
                .withRedirectUri(redirectUri)
                .withCodeChallenge(pkce)
                .build();

        AuthorizationRequestResponse authResponse = new AuthorizationRequestRequest(oauth, authRequest)
                .credentials(ctx.username, "password")
                .send();

        String authCode = authResponse.assertCode();
        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authCode)
                .authorizationDetails(authDetail)
                .codeVerifier(pkce)
                .send();

        // Exclude scope: <credScope>
        // Include role: credential-offer-create
        String accessToken = verifyAccessToken(accessTokenResponse,
                List.of(SCOPE_OPENID, credScope), List.of(),
                List.of(), List.of());

        CredentialRequest credRequest = new CredentialRequest().setCredentialConfigurationId(credConfigId);
        CredentialResponse credResponse = oauth.oid4vc().credentialRequest(credRequest)
                .bearerToken(accessToken)
                .send().getCredentialResponse();

        verifyCredentialResponse(ctx, credResponse);
    }

    @Test
    public void testCredentialFromPreAuthCode() throws Exception {

        TestContext ctx = new TestContext(jwtTypeNaturalPersonScopeName, "alice");
        String credConfigId = ctx.credentialConfig.getId();

        String issuerToken = getIssuerAccessToken();

        // Create the CredentialOffer
        //
        CredentialOfferURI credOfferUri = oauth.oid4vc().credentialOfferUriRequest(credConfigId)
                .preAuthorized(true)
                .username(ctx.username)
                .bearerToken(issuerToken)
                .send().getCredentialOfferURI();

        // Get the CredentialOffer
        //
        CredentialsOffer credOffer = oauth.oid4vc().doCredentialOfferRequest(credOfferUri).getCredentialsOffer();
        assertEquals(List.of(credConfigId), credOffer.getCredentialConfigurationIds());

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(ctx.issuerMetadata.getCredentialIssuer()));

        // [TODO >>>>] Pre-Auth TokenRequest does not natively support OID4VCAuthorizationDetail
        String authDetailsJson = JsonSerialization.writeValueAsString(List.of(authDetail));

        // Send the Pre-Auth TokenRequest
        //
        String authCode = credOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode();
        AccessTokenResponse accessTokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(authCode)
                .addParameter("authorization_details", authDetailsJson)
                .send();

        // Exclude scope: <credScope>
        // Include role: credential-offer-create
        // [TODO >>>>] Pre-Auth AccessToken does not have expected scopes [openid, oid4vc_natural_person_jwt] (still works)
        String accessToken = verifyAccessToken(accessTokenResponse,
                List.of(), List.of(),
                List.of(), List.of());

        CredentialRequest credRequest = new CredentialRequest().setCredentialConfigurationId(credConfigId);
        CredentialResponse credResponse = oauth.oid4vc().credentialRequest(credRequest)
                .bearerToken(accessToken)
                .send().getCredentialResponse();

        verifyCredentialResponse(ctx, credResponse);
    }

    @Test
    public void testAuthorizationRequestNoPkce() throws Exception {

        TestContext ctx = new TestContext(jwtTypeNaturalPersonScopeName, "alice");
        String credScope = ctx.credentialConfig.getScope();

        String redirectUri = verifiedRedirectUri(clientId, oauth.getRedirectUri());

        // Build an AuthorizationRequest with AuthorizationDetails
        //
        AuthorizationRequest authRequest = new AuthorizationRequestBuilder()
                .withClientId(clientId)
                // [TODO #44320] Requires Credential scope in AuthorizationRequest although already given in AuthorizationDetails
                .withScope(SCOPE_OPENID, credScope)
                .withRedirectUri(redirectUri)
                .build();

        AuthorizationRequestResponse authResponse = new AuthorizationRequestRequest(oauth, authRequest)
                .credentials(ctx.username, "password")
                .send();

        assertEquals("invalid_request", authResponse.getError());
        assertEquals("Missing parameter: code_challenge_method", authResponse.getErrorDescription());
        assertNull("Null code", authResponse.getCode());
    }

    @Test
    public void testAuthorizationRequestNoRedirectUri() throws Exception {

        TestContext ctx = new TestContext(jwtTypeNaturalPersonScopeName, "alice");
        String credScope = ctx.credentialConfig.getScope();

        PkceGenerator pkce = PkceGenerator.s256();

        // Build an AuthorizationRequest with AuthorizationDetails
        //
        AuthorizationRequest authRequest = new AuthorizationRequestBuilder()
                .withClientId(clientId)
                // [TODO #44320] Requires Credential scope in AuthorizationRequest although already given in AuthorizationDetails
                .withScope(SCOPE_OPENID, credScope)
                .withCodeChallenge(pkce)
                .build();

        AuthorizationRequestResponse authResponse = new AuthorizationRequestRequest(oauth, authRequest)
                .credentials(ctx.username, "password")
                .send();

        assertEquals("invalid_request", authResponse.getError());
        assertEquals("Invalid parameter: redirect_uri", authResponse.getErrorDescription());
        assertNull("Null code", authResponse.getCode());
    }

    @Test
    public void testOAuthLoginPkce() throws Exception {
        String accessToken = getIssuerAccessToken();
        assertNotNull("AccessToken not null", accessToken);
    }

    @Test
    public void testOAuthLoginNoPkce() throws Exception {

        // [TODO >>>>] OAuthClient.login cannot handle error page/redirect
        try {
            oauth.doLogin("john", "password");
            fail("TimeoutException expected");
        } catch (TimeoutException e) {
            // expected
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private String getIssuerAccessToken() throws Exception {

        PkceGenerator pkce = PkceGenerator.s256();
        AuthorizationEndpointResponse authResponse = oauth.loginForm()
                .codeChallenge(pkce)
                .doLogin("john", "password");

        String authCode = authResponse.getCode();
        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authCode)
                .codeVerifier(pkce)
                .send();

        // Exclude scope: <credScope>
        // Include role: credential-offer-create
        // [TODO >>>>] Pre-Auth AccessToken does not have expected scopes [openid, oid4vc_natural_person_jwt] (still works)
        verifyAccessToken(accessTokenResponse,
                List.of(), List.of(),
                List.of(), List.of());

        return accessTokenResponse.getAccessToken();
    }

    private String verifiedRedirectUri(String clientId, String redirectUri) {
        ClientRepresentation client = testRealm().clients().findByClientId(clientId).get(0);
        String registeredUri = client.getRedirectUris().stream()
                .filter(uri -> uri.startsWith(redirectUri))
                .findFirst()
                .orElse(null);
        if (registeredUri == null) {
            throw new IllegalArgumentException("Invalid redirect_uri: " + redirectUri);
        }
        return redirectUri;
    }

    private void verifyCredentialResponse(TestContext ctx, CredentialResponse credResponse) throws Exception {

        String credScope = ctx.credentialConfig.getScope();
        String issuer = ctx.issuerMetadata.getCredentialIssuer();
        CredentialResponse.Credential credentialObj = credResponse.getCredentials().get(0);
        assertNotNull("The first credential in the array should not be null", credentialObj);

        JsonWebToken jsonWebToken = TokenVerifier.create((String) credentialObj.getCredential(), JsonWebToken.class).getToken();
        assertEquals(issuer, jsonWebToken.getIssuer());
        Object vc = jsonWebToken.getOtherClaims().get("vc");
        VerifiableCredential credential = JsonSerialization.mapper.convertValue(vc, VerifiableCredential.class);
        assertEquals(List.of(credScope), credential.getType());
        assertEquals(URI.create(issuer), credential.getIssuer());
        assertEquals(ctx.username + "@email.cz", credential.getCredentialSubject().getClaims().get("email"));
    }

    private String verifyAccessToken(
            AccessTokenResponse accessTokenResponse,
            List<String> includeScopes,
            List<String> excludeScopes,
            List<String> includeRoles,
            List<String> excludeRoles
    ) throws Exception {
        String accessToken = accessTokenResponse.getAccessToken();
        JsonWebToken jwt = JsonSerialization.readValue(new JWSInput(accessToken).getContent(), JsonWebToken.class);
        List<String> wasScopes = Arrays.stream(((String) jwt.getOtherClaims().get("scope")).split("\\s")).toList();
        includeScopes.forEach(it -> assertTrue("Missing scope: " + it, wasScopes.contains(it)));
        excludeScopes.forEach(it -> assertFalse("Invalid scope: " + it, wasScopes.contains(it)));

        List<String> allRoles = new ArrayList<>();
        Object realmAccess = jwt.getOtherClaims().get("realm_access");
        if (realmAccess != null) {
            @SuppressWarnings("unchecked")
            var realmRoles = ((Map<String, List<String>>) realmAccess).get("roles");
            allRoles.addAll(realmRoles);
        }
        Object resourceAccess = jwt.getOtherClaims().get("resource_access");
        if (resourceAccess != null) {
            @SuppressWarnings("unchecked")
            var resourceAccessMapping = (Map<String, Map<String, List<String>>>) resourceAccess;
            resourceAccessMapping.forEach((k, v) -> allRoles.addAll(v.get("roles")));
        }
        includeRoles.forEach(it -> assertTrue("Missing role: " + it, allRoles.contains(it)));
        excludeRoles.forEach(it -> assertFalse("Invalid role: " + it, allRoles.contains(it)));
        return accessToken;
    }
}
