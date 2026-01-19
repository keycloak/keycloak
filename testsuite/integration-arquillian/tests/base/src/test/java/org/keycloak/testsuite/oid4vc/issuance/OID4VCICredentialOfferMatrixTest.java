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
package org.keycloak.testsuite.oid4vc.issuance;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.protocol.oid4vc.model.AuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCode;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCIssuerEndpointTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.commons.io.IOUtils;
import org.apache.directory.api.util.Strings;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import static org.keycloak.OAuth2Constants.CREDENTIAL_IDENTIFIERS;
import static org.keycloak.OAuth2Constants.OPENID_CREDENTIAL;
import static org.keycloak.constants.OID4VCIConstants.CREDENTIAL_OFFER_CREATE;
import static org.keycloak.protocol.oid4vc.model.ErrorType.INVALID_CREDENTIAL_OFFER_REQUEST;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsernameId;
import static org.keycloak.testsuite.forms.PassThroughClientAuthenticator.clientId;
import static org.keycloak.testsuite.forms.PassThroughClientAuthenticator.namedClientId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Credential Offer Validity Matrix
 * <p>
 * +----------+-----------+----------+---------+------------------------------------------------------+
 * | pre-auth | clientId  | username | Valid   | Notes                                                |
 * +----------+-----------+----------+---------+------------------------------------------------------+
 * | no       | no        | no       | yes     | Generic offer; any logged-in user may redeem.        |
 * | no       | no        | yes      | yes     | Offer restricted to a specific user.                 |
 * | no       | yes       | no       | yes     | Bound to client; user determined at login.           |
 * | no       | yes       | yes      | yes     | Bound to both client and user.                       |
 * +----------+-----------+----------+---------+------------------------------------------------------+
 * | yes      | no        | no       | no      | Pre-auth requires a user subject; missing username.  |
 * | yes      | no        | yes      | yes     | Pre-auth for a specific user; client issuer defined. |
 * | yes      | yes       | no       | no      | Same as above; username required.                    |
 * | yes      | yes       | yes      | yes     | Fully constrained: user + client.                    |
 * +----------+-----------+----------+---------+------------------------------------------------------+
 */
public class OID4VCICredentialOfferMatrixTest extends OID4VCIssuerEndpointTest {

    String issUsername = "john";
    String issClientId = clientId;

    String appUsername = "alice";

    String credScopeName = jwtTypeCredentialScopeName;
    String credConfigId = jwtTypeCredentialConfigurationIdName;

    static class OfferTestContext {
        boolean preAuthorized;
        String issUser;
        String issClient;
        String appUser;
        String appClient;
        CredentialIssuer issuerMetadata;
        OIDCConfigurationRepresentation authorizationMetadata;
        SupportedCredentialConfiguration supportedCredentialConfiguration;
    }

    OfferTestContext newTestContext(boolean preAuth, String appClient, String appUser) {
        var ctx = new OfferTestContext();
        ctx.preAuthorized = preAuth;
        ctx.issUser = issUsername;
        ctx.issClient = issClientId;
        ctx.appUser = appUser;
        ctx.appClient = appClient;
        ctx.issuerMetadata = getCredentialIssuerMetadata();
        ctx.authorizationMetadata = getAuthorizationMetadata(ctx.issuerMetadata.getAuthorizationServers().get(0));
        ctx.supportedCredentialConfiguration = ctx.issuerMetadata.getCredentialsSupported().get(credConfigId);
        return ctx;
    }

    @Test
    public void testVariousLogins() {
        assertNotNull(getBearerTokenAndLogout(issClientId, issUsername, "openid"));
        assertNotNull(getBearerTokenAndLogout(issClientId, appUsername, "openid"));
        assertNotNull(getBearerTokenAndLogout(namedClientId, issUsername, "openid"));
        assertNotNull(getBearerTokenAndLogout(namedClientId, appUsername, "openid"));
    }

    @Test
    public void testCredentialWithoutOffer() throws Exception {

        var ctx = newTestContext(false, null, appUsername);

        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(ctx.issuerMetadata.getCredentialIssuer()));

        // [TODO #44320] Requires Credential scope in AuthorizationRequest although already given in AuthorizationDetails
        // https://github.com/keycloak/keycloak/issues/44320
        String accessToken = getBearerToken(issClientId, ctx.appUser, credScopeName, authDetail);

        CredentialResponse credResponse = getCredentialByAuthDetail(ctx, accessToken, authDetail);
        verifyCredentialResponse(ctx, credResponse);
    }

    @Test
    public void testCredentialOffer_noPreAuth_noClientId_noUserId() throws Exception {
        runCredentialOfferTest(newTestContext(false, null, null));
    }

    @Test
    public void testCredentialOffer_noPreAuth_noClientId_UserId() throws Exception {
        runCredentialOfferTest(newTestContext(false, null, appUsername));
    }

    @Test
    public void testCredentialOffer_noPreAuth_ClientId_noUserId() throws Exception {
        runCredentialOfferTest(newTestContext(false, namedClientId, null));
    }

    @Test
    public void testCredentialOffer_noPreAuth_ClientId_UserId() throws Exception {
        runCredentialOfferTest(newTestContext(false, namedClientId, appUsername));
    }

    // Pre Authorized --------------------------------------------------------------------------------------------------

    @Test
    public void testCredentialOffer_PreAuth_noClientId_noUserId() throws Exception {
        try {
            runCredentialOfferTest(newTestContext(true, null, null));
            fail("Expected " + INVALID_CREDENTIAL_OFFER_REQUEST.name());
        } catch (RuntimeException ex) {
            List.of(INVALID_CREDENTIAL_OFFER_REQUEST.name(), "Pre-Authorized credential offer requires a target user")
                    .forEach(it -> assertTrue(ex.getMessage() + " does not contain " + it, ex.getMessage().contains(it)));
        }
    }

    @Test
    public void testCredentialOffer_PreAuth_noClientId_UserId() throws Exception {
        runCredentialOfferTest(newTestContext(true, null, appUsername));
    }

    @Test
    public void testCredentialOffer_PreAuth_ClientId_noUserId() throws Exception {
        try {
            runCredentialOfferTest(newTestContext(true, namedClientId, null));
            fail("Expected " + INVALID_CREDENTIAL_OFFER_REQUEST.name());
        } catch (RuntimeException ex) {
            List.of(INVALID_CREDENTIAL_OFFER_REQUEST.name(), "Pre-Authorized credential offer requires a target user")
                    .forEach(it -> assertTrue(ex.getMessage() + " does not contain " + it, ex.getMessage().contains(it)));
        }
    }

    @Test
    public void testCredentialOffer_PreAuth_ClientId_Username() throws Exception {
        runCredentialOfferTest(newTestContext(true, namedClientId, appUsername));
    }

    @Test
    public void testCredentialOffer_PreAuth_ClientId_Username_disabledUser() throws Exception {
        // Disable user
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), appUsername);
        UserRepresentation userRep = user.toRepresentation();
        userRep.setEnabled(false);
        user.update(userRep);

        try {
            runCredentialOfferTest(newTestContext(true, namedClientId, appUsername));
            fail("Expected " + INVALID_CREDENTIAL_OFFER_REQUEST.name());
        } catch (RuntimeException ex) {
            List.of(INVALID_CREDENTIAL_OFFER_REQUEST.name(), "User '" + appUsername + "' disabled")
                    .forEach(it -> assertTrue(ex.getMessage() + " does not contain " + it, ex.getMessage().contains(it)));
        } finally {
            // Re-enable user
            userRep.setEnabled(true);
            user.update(userRep);
        }
    }

    void runCredentialOfferTest(OfferTestContext ctx) throws Exception {

        // Issuer login
        //
        String issToken = getBearerToken(ctx.issClient, ctx.issUser, "openid");

        // Exclude scope: <credScope>
        // Require role: credential-offer-create
        verifyTokenJwt(ctx, issToken,
                List.of(), List.of(ctx.supportedCredentialConfiguration.getScope()),
                List.of(CREDENTIAL_OFFER_CREATE.getName()), List.of());

        // Retrieving the credential-offer-uri
        //
        String offerUri = getCredentialOfferUriUrl(ctx, issToken);

        // Issuer logout in order to remove unwanted session state
        //
        logout(ctx.issUser);

        try {

            // Using the uri to get the actual credential offer
            //
            CredentialsOffer credOffer = getCredentialsOffer(ctx, offerUri);

            if (credOffer.getCredentialConfigurationIds().size() > 1)
                throw new IllegalStateException("Multiple credential configuration ids not supported in: " + JsonSerialization.valueAsString(credOffer));

            if (ctx.preAuthorized) {

                // Get an access token for the pre-authorized code (PAC)
                //
                // For a PAC access token, we treat all scopes and all roles as non-meaningful.
                // The access token:
                //  1. has no authenticated user, and therefore cannot carry any user roles
                //  2. does not perform authorization-based scope filtering
                //  3. does not derive scopes from the client configuration
                //  4. does not reflect anything from the credential offer
                //
                AccessTokenResponse accessToken = getPreAuthorizedAccessTokenResponse(ctx, credOffer);
                List<AuthorizationDetail> authDetails = accessToken.getAuthorizationDetails();
                if (authDetails == null)
                    throw new IllegalStateException("No authorization_details in token response");
                if (authDetails.size() > 1)
                    throw new IllegalStateException("Multiple authorization_details in token response");

                // Get the credential and verify
                //
                CredentialResponse credResponse = getCredentialByAuthDetail(ctx, accessToken.getAccessToken(), authDetails.get(0));
                verifyCredentialResponse(ctx, credResponse);

            } else {

                String clientId = ctx.appClient != null ? ctx.appClient : namedClientId;
                String userId = ctx.appUser != null ? ctx.appUser : appUsername;
                String credConfigId = credOffer.getCredentialConfigurationIds().get(0);

                SupportedCredentialConfiguration credConfig = ctx.issuerMetadata.getCredentialsSupported().get(credConfigId);
                String scope = credConfig.getScope();

                String accessToken = getBearerToken(clientId, userId, scope);

                // Get the credential and verify
                //
                CredentialResponse credResponse = getCredentialByOffer(ctx, accessToken, credOffer);
                verifyCredentialResponse(ctx, credResponse);
            }
        } finally {
            if (ctx.appUser != null) {
                logout(ctx.appUser);
            }
        }
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private String getBearerToken(String clientId, String username, String scope) {
        ClientRepresentation client = testRealm().clients().findByClientId(clientId).get(0);
        if (client.isDirectAccessGrantsEnabled()) {
            return getBearerTokenDirectAccess(oauth, client, username, scope).getAccessToken();
        } else {
            return getBearerTokenCodeFlow(oauth, client, username, scope).getAccessToken();
        }
    }

    private String getBearerToken(String clientId, String username, String scope, AuthorizationDetail... authDetail) {
        ClientRepresentation client = testRealm().clients().findByClientId(clientId).get(0);
        String authCode = getAuthorizationCode(oauth, client, username, scope);
        return getBearerToken(oauth, authCode, authDetail).getAccessToken();
    }

    private String getBearerTokenAndLogout(String clientId, String userId, String scope) {
        String token = getBearerToken(clientId, userId, scope);
        logout(userId);
        return token;
    }

    private void logout(String userId) {
        findUserByUsernameId(testRealm(), userId).logout();
    }

    private String getCredentialOfferUriUrl(OfferTestContext ctx, String token) throws Exception {
        CredentialOfferURI offerURI = getCredentialOfferUri(ctx, token);
        return offerURI.getIssuer() + offerURI.getNonce();
    }

    private CredentialOfferURI getCredentialOfferUri(OfferTestContext ctx, String token) throws Exception {
        String credConfigId = ctx.supportedCredentialConfiguration.getId();
        String credOfferUriUrl = getCredentialOfferUriUrl(credConfigId, ctx.preAuthorized, ctx.appUser, ctx.appClient);
        HttpGet getCredentialOfferURI = new HttpGet(credOfferUriUrl);
        getCredentialOfferURI.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        CloseableHttpResponse credentialOfferURIResponse = httpClient.execute(getCredentialOfferURI);
        int statusCode = credentialOfferURIResponse.getStatusLine().getStatusCode();
        if (HttpStatus.SC_OK != statusCode) {
            HttpEntity entity = credentialOfferURIResponse.getEntity();
            throw new IllegalStateException(EntityUtils.toString(entity));
        }
        String s = IOUtils.toString(credentialOfferURIResponse.getEntity().getContent(), StandardCharsets.UTF_8);
        CredentialOfferURI credentialOfferURI = JsonSerialization.valueFromString(s, CredentialOfferURI.class);
        assertTrue(credentialOfferURI.getIssuer().startsWith(ctx.issuerMetadata.getCredentialIssuer()));
        assertTrue(Strings.isNotEmpty(credentialOfferURI.getNonce()));
        return credentialOfferURI;
    }

    private CredentialsOffer getCredentialsOffer(OfferTestContext ctx, String offerUri) throws Exception {
        HttpGet getCredentialOffer = new HttpGet(offerUri);
        CloseableHttpResponse credentialOfferResponse = httpClient.execute(getCredentialOffer);
        int statusCode = credentialOfferResponse.getStatusLine().getStatusCode();
        if (HttpStatus.SC_OK != statusCode) {
            HttpEntity entity = credentialOfferResponse.getEntity();
            throw new IllegalStateException(EntityUtils.toString(entity));
        }
        String s = IOUtils.toString(credentialOfferResponse.getEntity().getContent(), StandardCharsets.UTF_8);
        CredentialsOffer credOffer = JsonSerialization.valueFromString(s, CredentialsOffer.class);
        assertEquals(List.of(ctx.supportedCredentialConfiguration.getId()), credOffer.getCredentialConfigurationIds());
        return credOffer;
    }

    private AccessTokenResponse getPreAuthorizedAccessTokenResponse(OID4VCICredentialOfferMatrixTest.OfferTestContext ctx, CredentialsOffer credOffer) throws Exception {
        PreAuthorizedCode preAuthorizedCode = credOffer.getGrants().getPreAuthorizedCode();
        HttpPost postPreAuthorizedCode = new HttpPost(ctx.authorizationMetadata.getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, preAuthorizedCode.getPreAuthorizedCode()));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        postPreAuthorizedCode.setEntity(formEntity);
        CloseableHttpResponse accessTokenResponse = httpClient.execute(postPreAuthorizedCode);
        int statusCode = accessTokenResponse.getStatusLine().getStatusCode();
        if (HttpStatus.SC_OK != statusCode) {
            HttpEntity entity = accessTokenResponse.getEntity();
            throw new IllegalStateException(EntityUtils.toString(entity));
        }
        return new AccessTokenResponse(accessTokenResponse);
    }

    private CredentialResponse getCredentialByAuthDetail(OfferTestContext ctx, String accessToken, AuthorizationDetail authDetail) throws Exception {
        @SuppressWarnings("unchecked")
        List<String> credIdentifiers = (List<String>) authDetail.getAdditionalFields().get(CREDENTIAL_IDENTIFIERS);
        var credentialRequest = new CredentialRequest();
        if (credIdentifiers != null) {
            if (credIdentifiers.size() > 1)
                throw new IllegalStateException("Multiple credential ids not supported");
            credentialRequest.setCredentialIdentifier(credIdentifiers.get(0));
        } else {
            if (authDetail.getCredentialConfigurationId() == null)
                throw new IllegalStateException("No credential_configuration_id in: " + JsonSerialization.valueAsString(authDetail));
            credentialRequest.setCredentialConfigurationId(authDetail.getCredentialConfigurationId());
        }
        return sendCredentialRequest(ctx, accessToken, credentialRequest);
    }

    private CredentialResponse getCredentialByOffer(OfferTestContext ctx, String accessToken, CredentialsOffer credOffer) throws Exception {
        List<String> credConfigIds = credOffer.getCredentialConfigurationIds();
        if (credConfigIds.size() > 1)
            throw new IllegalStateException("Multiple credential configuration ids not supported in: " + JsonSerialization.valueAsString(credOffer));
        var credentialRequest = new CredentialRequest();
        credentialRequest.setCredentialConfigurationId(credConfigIds.get(0));
        return sendCredentialRequest(ctx, accessToken, credentialRequest);
    }

    private CredentialResponse sendCredentialRequest(OfferTestContext ctx, String accessToken, CredentialRequest credentialRequest) throws Exception {
        HttpPost postCredential = new HttpPost(ctx.issuerMetadata.getCredentialEndpoint());
        postCredential.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        StringEntity stringEntity = new StringEntity(JsonSerialization.valueAsString(credentialRequest), ContentType.APPLICATION_JSON);
        postCredential.setEntity(stringEntity);

        CloseableHttpResponse credentialRequestResponse = httpClient.execute(postCredential);
        int statusCode = credentialRequestResponse.getStatusLine().getStatusCode();
        if (HttpStatus.SC_OK != statusCode) {
            HttpEntity entity = credentialRequestResponse.getEntity();
            throw new IllegalStateException(EntityUtils.toString(entity));
        }

        String s = IOUtils.toString(credentialRequestResponse.getEntity().getContent(), StandardCharsets.UTF_8);
        CredentialResponse credentialResponse = JsonSerialization.valueFromString(s, CredentialResponse.class);

        assertNotNull("The credentials array should be present in the response", credentialResponse.getCredentials());
        assertFalse("The credentials array should not be empty", credentialResponse.getCredentials().isEmpty());
        return credentialResponse;
    }

    private void verifyCredentialResponse(OfferTestContext ctx, CredentialResponse credResponse) throws Exception {

        String scope = ctx.supportedCredentialConfiguration.getScope();
        CredentialResponse.Credential credentialObj = credResponse.getCredentials().get(0);
        assertNotNull("The first credential in the array should not be null", credentialObj);

        String expUsername = ctx.appUser != null ? ctx.appUser : appUsername;

        JsonWebToken jsonWebToken = TokenVerifier.create((String) credentialObj.getCredential(), JsonWebToken.class).getToken();
        assertEquals("did:web:test.org", jsonWebToken.getIssuer());
        Object vc = jsonWebToken.getOtherClaims().get("vc");
        VerifiableCredential credential = JsonSerialization.mapper.convertValue(vc, VerifiableCredential.class);
        assertEquals(List.of(scope), credential.getType());
        assertEquals(URI.create("did:web:test.org"), credential.getIssuer());
        assertEquals(expUsername + "@email.cz", credential.getCredentialSubject().getClaims().get("email"));
    }

    private void verifyTokenJwt(
            OfferTestContext ctx,
            String token,
            List<String> includeScopes,
            List<String> excludeScopes,
            List<String> includeRoles,
            List<String> excludeRoles
    ) throws Exception {
        JsonWebToken jwt = JsonSerialization.readValue(new JWSInput(token).getContent(), JsonWebToken.class);
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
            resourceAccessMapping.forEach((k, v) -> {
                allRoles.addAll(v.get("roles"));
            });
        }
        includeRoles.forEach(it -> assertTrue("Missing role: " + it, allRoles.contains(it)));
        excludeRoles.forEach(it -> assertFalse("Invalid role: " + it, allRoles.contains(it)));
    }
}
